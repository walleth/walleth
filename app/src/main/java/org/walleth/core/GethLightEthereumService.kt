package org.walleth.core

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.SystemClock
import android.support.v4.app.NotificationCompat
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import org.ethereum.geth.*
import org.walleth.R
import org.walleth.activities.MainActivity
import org.walleth.data.BalanceProvider
import org.walleth.data.WallethAddress
import org.walleth.data.config.Settings
import org.walleth.data.keystore.GethBackedWallethKeyStore
import org.walleth.data.keystore.WallethKeyStore
import org.walleth.data.networks.NetworkDefinitionProvider
import org.walleth.data.syncprogress.SyncProgressProvider
import org.walleth.data.syncprogress.WallethSyncProgress
import org.walleth.data.transactions.Transaction
import org.walleth.data.transactions.TransactionProvider
import org.walleth.data.transactions.TransactionSource
import org.walleth.ui.ChangeObserver
import java.io.File
import java.math.BigInteger


class GethLightEthereumService : Service() {

    companion object {
        val STOP_SERVICE_ACTION = "STOPSERVICE"

        fun android.content.Context.gethStopIntent() = Intent(this, GethLightEthereumService::class.java).apply {
            action = STOP_SERVICE_ACTION
        }
    }

    val NOTIFICATION_ID = 101

    val binder by lazy { Binder() }
    override fun onBind(intent: Intent) = binder

    val lazyKodein = LazyKodein(appKodein)

    val balanceProvider: BalanceProvider by lazyKodein.instance()
    val transactionProvider: TransactionProvider by lazyKodein.instance()
    val syncProgress: SyncProgressProvider by lazyKodein.instance()
    val keyStore: WallethKeyStore by lazyKodein.instance()
    val settings: Settings by lazyKodein.instance()
    val networkDefinitionProvider: NetworkDefinitionProvider by lazyKodein.instance()
    private val path by lazy { File(baseContext.filesDir, ".ethereum_rb").absolutePath }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        if (intent.action == STOP_SERVICE_ACTION) {
            WatchdogState.geth_service_running = false
            return START_NOT_STICKY
        }

        if (WatchdogState.geth_service_running) {
            return START_NOT_STICKY
        }

        WatchdogState.geth_last_seen = System.currentTimeMillis()
        WatchdogState.geth_service_running = true

        val pendingStopIntent = PendingIntent.getService(baseContext, 0, gethStopIntent(), 0)
        val contentIntent = PendingIntent.getActivity(baseContext, 0, Intent(baseContext, MainActivity::class.java), 0)

        val notification = NotificationCompat.Builder(this).apply {
            setContentTitle("WALLETH Geth")
            setContentText("light client running")
            setContentIntent(contentIntent)
            addAction(android.R.drawable.ic_menu_close_clear_cancel, "exit", pendingStopIntent)
            setSmallIcon(R.drawable.notification)
        }.build()

        startForeground(NOTIFICATION_ID, notification)

        Thread({

            val ethereumContext = Context()

            val ethereumNode = Geth.newNode(path, NodeConfig().apply {
                val bootNodes = Enodes()

                val network = networkDefinitionProvider.networkDefinition

                network.bootNodes.forEach {
                    bootNodes.append(Enode(it))
                }

                bootstrapNodes = bootNodes
                ethereumGenesis = network.genesis
                ethereumNetworkID = 4
                ethereumNetStats = settings.getStatsName() + ":Respect my authoritah!@stats.rinkeby.io"
            })

            try {
                ethereumNode.start()

                ethereumNode.ethereumClient.subscribeNewHead(ethereumContext, object : NewHeadHandler {
                    override fun onNewHead(p0: Header) {
                        val address = keyStore.getCurrentAddress().toGethAddr()
                        val balance = ethereumNode.ethereumClient.getBalanceAt(ethereumContext, address, p0.number)
                        balanceProvider.setBalance(WallethAddress(address.hex), p0.number, BigInteger(balance.string()))

                    }

                    override fun onError(p0: String?) {}

                }, 16)

                transactionProvider.registerChangeObserver(object : ChangeObserver {
                    override fun observeChange() {
                        transactionProvider.getAllTransactions().forEach {
                            if (it.ref == TransactionSource.WALLETH) {
                                executeTransaction(it, ethereumNode, ethereumContext)
                            }
                        }
                    }

                })
            } catch (e: Exception) {
                org.ligi.tracedroid.logging.Log.e("node error", e)
            }

            while (WatchdogState.geth_service_running) {

                WatchdogState.geth_last_seen = System.currentTimeMillis()
                try {
                    val ethereumSyncProgress = ethereumNode.ethereumClient.syncProgress(ethereumContext)

                    if (ethereumSyncProgress != null) {
                        val newSyncProgress = WallethSyncProgress(true, ethereumSyncProgress.currentBlock, ethereumSyncProgress.highestBlock)
                        syncProgress.setSyncProgress(newSyncProgress)
                    } else {
                        syncProgress.setSyncProgress(WallethSyncProgress())
                    }
                } catch(e: Exception) {
                }

                SystemClock.sleep(1000)
            }

            ethereumNode.stop()
            stopForeground(true)
            stopSelf()
        }).start()

        return START_NOT_STICKY
    }


    private fun executeTransaction(transaction: Transaction, ethereumNode: Node, ethereumContext: Context) {
        transaction.ref = TransactionSource.WALLETH_PROCESSED

        try {
            val client = ethereumNode.ethereumClient
            val nonceAt = client.getNonceAt(ethereumContext, transaction.from.toGethAddr(), -1)

            val gasPrice = client.suggestGasPrice(ethereumContext)

            val gasLimit = BigInt(21_000)

            val newTransaction = Geth.newTransaction(nonceAt, transaction.to.toGethAddr(), BigInt(transaction.value.toLong()), gasLimit, gasPrice, ByteArray(0))

            newTransaction.hashCode()

            val gethKeystore = (keyStore as GethBackedWallethKeyStore).keyStore
            val accounts = gethKeystore.accounts
            val index = (0..(accounts.size() - 1)).firstOrNull { accounts.get(it).address.hex.toUpperCase() == transaction.from.hex.toUpperCase() }

            if (index == null) {
                transaction.error = "No key for sending account"
                return
            }
            gethKeystore.unlock(accounts.get(index), "default")

            val signHash = gethKeystore.signHash(transaction.from.toGethAddr(), newTransaction.sigHash.bytes)
            val transactionWithSignature = newTransaction.withSignature(signHash)

            transaction.sigHash = newTransaction.sigHash.hex
            transaction.txHash = newTransaction.hash.hex

            client.sendTransaction(ethereumContext, transactionWithSignature)
        } catch (e: Exception) {
            transaction.error = e.message
        }
    }

}
