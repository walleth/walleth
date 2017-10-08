package org.walleth.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.arch.lifecycle.LifecycleService
import android.arch.lifecycle.Observer
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.SystemClock
import android.support.v4.app.NotificationCompat
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import org.ethereum.geth.*
import org.kethereum.functions.encodeRLP
import org.walleth.R
import org.walleth.activities.MainActivity
import org.walleth.data.AppDatabase
import org.walleth.data.balances.Balance
import org.walleth.data.config.Settings
import org.walleth.data.keystore.WallethKeyStore
import org.walleth.data.networks.CurrentAddressProvider
import org.walleth.data.networks.NetworkDefinitionProvider
import org.walleth.data.syncprogress.SyncProgressProvider
import org.walleth.data.syncprogress.WallethSyncProgress
import org.walleth.data.tokens.getEthTokenForChain
import org.walleth.data.transactions.TransactionEntity
import org.walleth.kethereum.geth.toGethAddr
import java.io.File
import java.math.BigInteger


class GethLightEthereumService : LifecycleService() {

    companion object {
        val STOP_SERVICE_ACTION = "STOPSERVICE"
        fun android.content.Context.gethStopIntent() = Intent(this, GethLightEthereumService::class.java).apply {
            action = STOP_SERVICE_ACTION
        }

        var isRunning = false
    }

    val NOTIFICATION_ID = 101

    val lazyKodein = LazyKodein(appKodein)

    val syncProgress: SyncProgressProvider by lazyKodein.instance()
    val appDatabase: AppDatabase by lazyKodein.instance()
    val keyStore: WallethKeyStore by lazyKodein.instance()
    val settings: Settings by lazyKodein.instance()
    val networkDefinitionProvider: NetworkDefinitionProvider by lazyKodein.instance()
    val currentAddressProvider: CurrentAddressProvider by lazyKodein.instance()
    private val path by lazy { File(baseContext.cacheDir, ".ethereum").absolutePath }
    val notificationManager by lazy { getSystemService(android.content.Context.NOTIFICATION_SERVICE) as NotificationManager }

    var isSyncing = false
    var finishedSyncing = false

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent.action == STOP_SERVICE_ACTION) {
            isRunning = false
            return START_NOT_STICKY
        }

        isRunning = true

        val pendingStopIntent = PendingIntent.getService(baseContext, 0, gethStopIntent(), 0)
        val contentIntent = PendingIntent.getActivity(baseContext, 0, Intent(baseContext, MainActivity::class.java), 0)


        if (Build.VERSION.SDK_INT > 25) {
            val channel = NotificationChannel("geth", "Channel name", NotificationManager.IMPORTANCE_HIGH)
            channel.description = "Channel description"
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, "geth")
                .setContentTitle("WALLETH Geth")
                .setContentText("light client running")
                .setContentIntent(contentIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "exit", pendingStopIntent)
                .setSmallIcon(R.drawable.notification)
                .build()

        startForeground(NOTIFICATION_ID, notification)
        val handler = Handler()
        Thread({

            val ethereumContext = Context()

            val network = networkDefinitionProvider.getCurrent()
            val subPath = File(path, "chain" + network.chain.id.toString())
            subPath.mkdirs()
            val ethereumNode = Geth.newNode(subPath.absolutePath, NodeConfig().apply {

                if (network.bootNodes.isEmpty()) {
                    val bootNodes = Enodes()

                    network.bootNodes.forEach {
                        bootNodes.append(Enode(it))
                    }

                    bootstrapNodes = bootNodes
                }

                ethereumNetworkID = network.chain.id

                if (!network.genesis.isEmpty()) {
                    ethereumGenesis = when (network.chain.id) {
                        1L -> Geth.mainnetGenesis()
                        3L -> Geth.testnetGenesis()
                        4L -> Geth.rinkebyGenesis()
                        else -> throw (IllegalStateException("NO genesis"))
                    }
                }

                if (!network.statsSuffix.isEmpty()) {
                    ethereumNetStats = settings.getStatsName() + network.statsSuffix
                }
            })

            ethereumNode.start()

            while (isRunning && !finishedSyncing) {
                SystemClock.sleep(1000)
                syncTick(ethereumNode, ethereumContext)
            }
            val transactionsLiveData = appDatabase.transactions.getAllToRelayLive()
            val transactionObserver = Observer<List<TransactionEntity>> {
                it?.forEach {
                    executeTransaction(it, ethereumNode.ethereumClient, ethereumContext)
                }
            }
            transactionsLiveData.observe(this, transactionObserver)
            try {
                ethereumNode.ethereumClient.subscribeNewHead(ethereumContext, object : NewHeadHandler {
                    override fun onNewHead(p0: Header) {
                        val address = currentAddressProvider.getCurrent()
                        val gethAddress = address.toGethAddr()
                        val balance = ethereumNode.ethereumClient.getBalanceAt(ethereumContext, gethAddress, p0.number)
                        appDatabase.balances.upsert(Balance(
                                address = address,
                                tokenAddress = getEthTokenForChain(networkDefinitionProvider.getCurrent()).address,
                                chain = networkDefinitionProvider.getCurrent().chain,
                                balance = BigInteger(balance.string()),
                                block = p0.number))
                    }

                    override fun onError(p0: String?) {}

                }, 16)

                //transactionProvider.registerChangeObserver(changeObserver)

            } catch (e: Exception) {
                org.ligi.tracedroid.logging.Log.e("node error", e)
            }

            org.ligi.tracedroid.logging.Log.i("FinishedSyncing")
            while (isRunning) {
                syncTick(ethereumNode, ethereumContext)
            }

            handler.post {
                transactionsLiveData.removeObserver(transactionObserver)
                ethereumNode.stop()
                stopForeground(true)
                stopSelf()
            }

        }).start()
        return START_NOT_STICKY
    }

    private fun syncTick(ethereumNode: Node, ethereumContext: Context) {
        try {
            val ethereumSyncProgress = ethereumNode.ethereumClient.syncProgress(ethereumContext)

            if (ethereumSyncProgress != null) {
                isSyncing = true
                val newSyncProgress = WallethSyncProgress(true, ethereumSyncProgress.currentBlock, ethereumSyncProgress.highestBlock)
                syncProgress.postValue(newSyncProgress)
            } else {
                syncProgress.postValue(WallethSyncProgress())
                if (isSyncing) {
                    finishedSyncing = true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        SystemClock.sleep(1000)
    }


    private fun executeTransaction(transaction: TransactionEntity, client: EthereumClient, ethereumContext: Context) {
        try {
            val rlp = transaction.transaction.encodeRLP()
            val transactionWithSignature = Geth.newTransactionFromRLP(rlp)
            client.sendTransaction(ethereumContext, transactionWithSignature)
            transaction.transactionState.relayedLightClient = true
        } catch (e: Exception) {
            transaction.transactionState.error = e.message
        }
    }

}
