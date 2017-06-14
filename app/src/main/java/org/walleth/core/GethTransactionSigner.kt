package org.walleth.core

import android.app.Service
import android.content.Intent
import android.os.Binder
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import org.ethereum.geth.BigInt
import org.ethereum.geth.Geth
import org.walleth.data.DEFAULT_PASSWORD
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
                transactionProvider.popPendingTransaction()?.let {
                    signTransaction(it)
                }

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
        if (transaction.needsSigningConfirmation || transaction.signedRLP != null) {
            return
        }

        val previousTxHash = transaction.txHash

        transaction.ref = TransactionSource.WALLETH_PROCESSED

        if (transaction.nonce == null) {
            transaction.nonce = transactionProvider.getLastNonceForAddress(transaction.from) + 1
        }

        val newTransaction = Geth.newTransaction(transaction.nonce!!,
                transaction.to.toGethAddr(),
                BigInt(transaction.value.toLong()),
                transaction.gasLimit.toGethInteger(),
                transaction.gasPrice.toGethInteger(),
                transaction.input.toByteArray()
        )
        val gethKeystore = (keyStore as GethBackedWallethKeyStore).keyStore
        val accounts = gethKeystore.accounts
        val index = (0..(accounts.size() - 1)).firstOrNull { accounts.get(it).address.hex.toUpperCase() == transaction.from.hex.toUpperCase() }

        if (index == null) {
            transaction.error = "No key for sending account"
            transaction.txRLP = newTransaction.encodeRLP().asList()
            transaction.txHash = newTransaction.hash.hex
        } else {
            gethKeystore.unlock(accounts.get(index), DEFAULT_PASSWORD)

            val signHash = gethKeystore.signHash(transaction.from.toGethAddr(), newTransaction.sigHash.bytes)
            val transactionWithSignature = newTransaction.withSignature(signHash)

            transaction.txHash = transactionWithSignature.hash.hex
            transaction.signedRLP = transactionWithSignature.encodeRLP().asList()
            transaction.sigHash = newTransaction.sigHash.hex

        }

        if (previousTxHash != null) {
            transactionProvider.updateTransaction(previousTxHash, transaction)
        } else {
            transactionProvider.addTransaction(transaction)
        }

    }

}
