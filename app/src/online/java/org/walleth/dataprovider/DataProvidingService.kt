package org.walleth.dataprovider

import android.content.Intent
import androidx.lifecycle.*
import androidx.work.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.kethereum.model.Address
import org.kethereum.model.Transaction
import org.koin.android.ext.android.inject
import org.ligi.kaxt.livedata.nonNull
import org.ligi.kaxt.livedata.observe
import org.ligi.tracedroid.logging.Log
import org.walleth.data.AppDatabase
import org.walleth.data.KEY_TX_HASH
import org.walleth.data.balances.Balance
import org.walleth.data.balances.upsertIfNewerBlock
import org.walleth.data.chaininfo.ChainInfo
import org.walleth.data.networks.ChainInfoProvider
import org.walleth.data.networks.CurrentAddressProvider
import org.walleth.data.rpc.RPCProvider
import org.walleth.data.tokens.CurrentTokenProvider
import org.walleth.data.tokens.isRootToken
import org.walleth.data.transactions.TransactionEntity
import org.walleth.kethereum.blockscout.ALL_BLOCKSCOUT_SUPPORTED_NETWORKS
import org.walleth.khex.hexToByteArray
import org.walleth.workers.RelayTransactionWorker
import java.io.IOException
import java.math.BigInteger
import java.util.concurrent.TimeUnit

class DataProvidingService : LifecycleService() {

    private val okHttpClient: OkHttpClient by inject()
    private val currentAddressProvider: CurrentAddressProvider by inject()
    private val tokenProvider: CurrentTokenProvider by inject()
    private val appDatabase: AppDatabase by inject()
    private val chainInfoProvider: ChainInfoProvider by inject()
    private val rpcProvider: RPCProvider by inject()
    private val blockScoutApi = BlockScoutAPI(appDatabase, okHttpClient)

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

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        currentAddressProvider.observe(this, ResettingObserver())
        chainInfoProvider.observe(this, ResettingObserver())

        lifecycle.addObserver(TimingModifyingLifecycleObserver())

        GlobalScope.launch {

            while (true) {
                last_run = System.currentTimeMillis()

                chainInfoProvider.getCurrent()?.let { currentChain ->
                    val currentChainId = currentChain.chainId
                    currentAddressProvider.value?.let { address ->

                        try {
                            if (ALL_BLOCKSCOUT_SUPPORTED_NETWORKS.contains(currentChainId)) {
                                tryFetchFromBlockscout(address, currentChain)
                            }

                            queryRPCForBalance(address)
                        } catch (ioe: IOException) {
                            Log.i("problem fetching data - are we online? ", ioe)
                        }
                    }

                    while ((last_run + timing) > System.currentTimeMillis() && !shortcut) {
                        delay(100)
                    }
                    shortcut = false
                }
            }
        }


        relayTransactionsIfNeeded()
        return START_STICKY
    }

    private fun relayTransactionsIfNeeded() {
        appDatabase.transactions.getAllToRelayLive().nonNull().observe(this) { transactionList ->
            transactionList.filter { it.transactionState.isPending }.forEach { sendTransaction(it) }
        }
    }

    private fun sendTransaction(transaction: TransactionEntity) {

        val uploadWorkRequest = OneTimeWorkRequestBuilder<RelayTransactionWorker>()
                .setBackoffCriteria(
                        BackoffPolicy.LINEAR,
                        OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                        TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(KEY_TX_HASH to transaction.hash))
                .build()

        WorkManager.getInstance(this).enqueue(uploadWorkRequest)
    }

    private fun tryFetchFromBlockscout(address: Address, chain: ChainInfo) {
        blockScoutApi.queryTransactions(address.hex, chain)
    }

    private fun queryRPCForBalance(address: Address) {

        val currentToken = tokenProvider.getCurrent()
        val currentChainId = chainInfoProvider.getCurrent()?.chainId
        val rpc = rpcProvider.get()
        when {
            currentChainId == null -> Log.e("no current chain is null")
            rpc == null -> Log.e("no RPC found")
            else -> {

                val blockNumberString = rpc.blockNumber()?.result
                val blockNumber = blockNumberString?.replace("0x", "")?.toLongOrNull(16)
                if (blockNumber != null) {
                    val balance = if (currentToken.isRootToken()) {
                        rpc.getBalance(address, blockNumberString)
                    } else {
                        val input = ("0x70a08231" + "0".repeat(24) + address.cleanHex).hexToByteArray()
                        val tx = Transaction().copy(to = currentToken.address, input = input, gasLimit = null, gasPrice = null)
                        rpc.call(tx, blockNumberString)
                    }

                    if (balance?.error == null && balance?.result != null) {
                        try {

                            appDatabase.balances.upsertIfNewerBlock(
                                    Balance(address = address,
                                            block = blockNumber,
                                            balance = BigInteger(balance.result.replace("0x", ""), 16),
                                            tokenAddress = currentToken.address,
                                            chain = currentChainId
                                    )
                            )
                        } catch (e: NumberFormatException) {
                            Log.i("could not parse number ${balance.result}")
                        }
                    }
                }
            }
        }
    }

}