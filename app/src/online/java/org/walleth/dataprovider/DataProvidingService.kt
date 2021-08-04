package org.walleth.dataprovider

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.kethereum.extensions.toHexString
import org.kethereum.model.Address
import org.kethereum.rpc.EthereumRPCException
import org.koin.android.ext.android.inject
import org.komputing.kethereum.erc20.ERC20RPCConnector
import org.ligi.kaxt.getNotificationManager
import org.ligi.kaxt.livedata.nonNull
import org.ligi.kaxt.livedata.observe
import org.walleth.App
import org.walleth.chains.ChainInfoProvider
import org.walleth.data.AppDatabase
import org.walleth.data.KEY_TX_HASH
import org.walleth.data.addresses.CurrentAddressProvider
import org.walleth.data.balances.Balance
import org.walleth.data.balances.upsertIfNewerBlock
import org.walleth.data.chaininfo.ChainInfo
import org.walleth.data.config.Settings
import org.walleth.data.rpc.RPCProvider
import org.walleth.data.tokens.CurrentTokenProvider
import org.walleth.data.tokens.isRootToken
import org.walleth.data.transactions.TransactionEntity
import org.walleth.kethereum.etherscan.ALL_ETHERSCAN_SUPPORTED_NETWORKS
import org.walleth.notifications.NOTIFICATION_CHANNEL_ID_DATA_SERVICE
import org.walleth.notifications.NOTIFICATION_ID_DATA_SERVICE
import org.walleth.workers.RelayTransactionWorker
import timber.log.Timber
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit


private const val ACTION_STOP_SERVICE = "STOP"

class DataProvidingService : LifecycleService() {

    private val okHttpClient: OkHttpClient by inject()
    private val currentAddressProvider: CurrentAddressProvider by inject()
    private val tokenProvider: CurrentTokenProvider by inject()
    private val appDatabase: AppDatabase by inject()
    private val chainInfoProvider: ChainInfoProvider by inject()
    private val rpcProvider: RPCProvider by inject()
    private val settings: Settings by inject()
    private val blockScoutApi = EtherScanAPI(appDatabase, rpcProvider, okHttpClient)

    companion object {
        private var timing = 7_000 // in MilliSeconds
        private var last_run = 0L
        private var shortcut = false

    }

    class TimingModifyingLifecycleObserver : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
        fun connectListener() {
            timing = 7_000
            shortcut = true
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        fun disconnectListener() {
            timing = 70_000
        }
    }

    class ResettingObserver<T> : Observer<T> {
        override fun onChanged(p0: T?) {
            shortcut = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (ACTION_STOP_SERVICE == intent?.action) {
            Timber.d("DataProvider stopped via intent from notificaion");
            stopSelf();
        } else {

            if (Build.VERSION.SDK_INT > 25) {
                val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID_DATA_SERVICE, "Ethereum Data Provider", NotificationManager.IMPORTANCE_LOW)
                channel.description = "WalletConnectNotifications"
                getNotificationManager().createNotificationChannel(channel)
            }

            val stopSelf = Intent(this, DataProvidingService::class.java)
            stopSelf.action = ACTION_STOP_SERVICE

            val stopSelfPendingIntent = PendingIntent.getService(this, 0, stopSelf, PendingIntent.FLAG_CANCEL_CURRENT)

            val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID_DATA_SERVICE).apply {
                if (Build.VERSION.SDK_INT > 19) {
                    setSmallIcon(org.walleth.R.drawable.ic_ethereum_logo)
                }

                setContentTitle(null)
                setOngoing(true)
                setLocalOnly(true)
                setOnlyAlertOnce(true)
                addAction(org.walleth.R.drawable.ic_baseline_cancel_24, "stop", stopSelfPendingIntent)
                priority = NotificationCompat.PRIORITY_MIN
            }

            startForeground(NOTIFICATION_ID_DATA_SERVICE, notification.build())

            lifecycle.addObserver(TimingModifyingLifecycleObserver())

            val dateFormat = java.text.DateFormat.getTimeInstance()
            lifecycleScope.launch(Dispatchers.IO) {

                lifecycleScope.launch {
                    currentAddressProvider.flow.combine(chainInfoProvider.getFlow()) { _, _ -> }.collect { shortcut = true }
                }

                while (settings.isKeepETHSyncEnabledWanted() || last_run == 0L || App.visibleActivities.isNotEmpty()) {
                    last_run = System.currentTimeMillis()

                    chainInfoProvider.getCurrent().let { currentChain ->
                        val currentChainId = currentChain.chainId
                        currentAddressProvider.getCurrent()?.let { address ->

                            notification.setContentTitle("Last Ethereum data sync: " + dateFormat.format(Date()))
                            notification.setContentText("via " + rpcProvider.get()?.description)
                            getNotificationManager().notify(NOTIFICATION_ID_DATA_SERVICE, notification.build())

                            try {
                                if (ALL_ETHERSCAN_SUPPORTED_NETWORKS.contains(currentChainId)) {
                                    tryFetchFromBlockscout(address, currentChain)
                                }

                                queryRPCForBalance(address)
                                queryRPCForTransactions()
                            } catch (ioe: IOException) {
                                Timber.i(ioe, "problem fetching data - are we online? ")
                            }
                        }

                        while ((last_run + timing) > System.currentTimeMillis() && !shortcut) {
                            delay(100)
                        }
                        shortcut = false
                    }
                }

                stopSelf()
            }


            relayTransactionsIfNeeded()

        }
        return START_STICKY
    }

    private suspend fun queryRPCForTransactions() {

        appDatabase.transactions.getAllPending().forEach { localTx ->
            val rpc = rpcProvider.get()
            val tx = rpc?.getTransactionByHash(localTx.hash)
            if (tx?.transaction?.blockNumber != null) {
                localTx.transactionState.isPending = false
                localTx.transaction = tx.transaction
                appDatabase.transactions.upsert(localTx)
            }
            localTx.hash
        }
    }

    private fun relayTransactionsIfNeeded() {
        appDatabase.transactions.getAllToRelayLive().nonNull().observe(this) { transactionList ->
            val alltorelay = transactionList.filter { it.transactionState.error == null }
            alltorelay.forEach { sendTransaction(it) }
        }
    }

    private fun sendTransaction(transaction: TransactionEntity) {

        val uploadWorkRequest = OneTimeWorkRequestBuilder<RelayTransactionWorker>()
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .addTag("relay")
            .setInputData(workDataOf(KEY_TX_HASH to transaction.hash))
            .build()

        WorkManager.getInstance(this).enqueueUniqueWork(transaction.hash, ExistingWorkPolicy.REPLACE, uploadWorkRequest)
    }

    private suspend fun tryFetchFromBlockscout(address: Address, chain: ChainInfo) {
        blockScoutApi.queryTransactions(address.hex, chain)
    }

    private suspend fun queryRPCForBalance(address: Address) {

        val currentToken = tokenProvider.getCurrent()
        val currentChainId = chainInfoProvider.getCurrent()?.chainId
        val rpc = rpcProvider.get()
        if (rpc == null) {
            Timber.e("no RPC found")
        } else {
            try {
                val blockNumber = rpc.blockNumber()
                if (blockNumber != null) {
                    val blockNumberAsHex = blockNumber.toHexString()


                    val balance = if (currentToken.isRootToken()) {
                        rpc.getBalance(address, blockNumberAsHex)
                    } else {
                        ERC20RPCConnector(currentToken.address, rpc).balanceOf(address)
                    }
                    if (balance != null) {
                        appDatabase.balances.upsertIfNewerBlock(
                            Balance(
                                address = address,
                                block = blockNumber.toLong(),
                                balance = balance,
                                tokenAddress = currentToken.address,
                                chain = currentChainId
                            )
                        )
                    }
                }
            } catch (rpcException: EthereumRPCException) {
                Timber.e(rpcException, "error when getting balance")
            } catch (e: Exception) {
                Timber.e(e, "error when getting balance")
            }
        }
    }
}