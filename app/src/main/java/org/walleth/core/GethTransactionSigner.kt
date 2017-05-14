package org.walleth.core

import android.app.Service
import android.content.Intent
import android.os.Binder
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import org.ethereum.geth.BigInt
import org.ethereum.geth.Geth
import org.walleth.data.config.Settings
import org.walleth.data.keystore.GethBackedWallethKeyStore
import org.walleth.data.keystore.WallethKeyStore
import org.walleth.data.transactions.Transaction
import org.walleth.data.transactions.TransactionProvider
import org.walleth.data.transactions.TransactionSource
import org.walleth.functions.toGethInteger
import org.walleth.ui.ChangeObserver


class GethTransactionSigner : Service() {

    val binder by lazy { Binder() }
    override fun onBind(intent: Intent) = binder

    val lazyKodein = LazyKodein(appKodein)

    val transactionProvider: TransactionProvider by lazyKodein.instance()
    val keyStore: WallethKeyStore by lazyKodein.instance()
    val settings: Settings by lazyKodein.instance()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val changeObserver: ChangeObserver = object : ChangeObserver {
            override fun observeChange() {
                transactionProvider.getAllTransactions().forEach {
                    if (it.ref == TransactionSource.WALLETH) {
                        signTransaction(it)
                    }
                }
            }
        }

        transactionProvider.registerChangeObserver(changeObserver)

        return START_STICKY
    }

    private fun signTransaction(transaction: Transaction) {
        transaction.ref = TransactionSource.WALLETH_PROCESSED

        val nonce = transactionProvider.getLastNonceForAddress(transaction.from) + 1
        transaction.nonce = nonce
        val newTransaction = Geth.newTransaction(nonce,
                transaction.to.toGethAddr(),
                BigInt(transaction.value.toLong()),
                transaction.gasLimit.toGethInteger(),
                transaction.gasPrice.toGethInteger(),
                ByteArray(0)
        )
        val gethKeystore = (keyStore as GethBackedWallethKeyStore).keyStore
        val accounts = gethKeystore.accounts
        val index = (0..(accounts.size() - 1)).firstOrNull { accounts.get(it).address.hex.toUpperCase() == transaction.from.hex.toUpperCase() }

        if (index == null) {
            transaction.error = "No key for sending account"
        } else {
            gethKeystore.unlock(accounts.get(index), "default")

            val signHash = gethKeystore.signHash(transaction.from.toGethAddr(), newTransaction.sigHash.bytes)
            val transactionWithSignature = newTransaction.withSignature(signHash)

            transaction.signedRLP = transactionWithSignature.encodeRLP().asList()
            transaction.sigHash = newTransaction.sigHash.hex
            transaction.txHash = newTransaction.hash.hex

            transactionProvider.addTransaction(transaction)
        }
    }

}
