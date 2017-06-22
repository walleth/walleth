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
import org.walleth.data.toGethAddr
import org.walleth.data.transactions.TransactionProvider
import org.walleth.data.transactions.TransactionSource
import org.walleth.data.transactions.TransactionWithState
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
                    if (it.state.ref == TransactionSource.WALLETH) {
                        signTransaction(it)
                    }
                }
            }
        }

        transactionProvider.registerChangeObserver(changeObserver)

        return START_STICKY
    }

    private fun signTransaction(transaction: TransactionWithState) {
        if (transaction.state.needsSigningConfirmation || transaction.transaction.signedRLP != null) {
            return
        }

        val previousTxHash = transaction.transaction.txHash

        transaction.state.ref = TransactionSource.WALLETH_PROCESSED

        if (transaction.transaction.nonce == null) {
            transaction.transaction.nonce = transactionProvider.getLastNonceForAddress(transaction.transaction.from) + 1
        }

        val newTransaction = Geth.newTransaction(transaction.transaction.nonce!!,
                transaction.transaction.to!!.toGethAddr(),
                BigInt(transaction.transaction.value.toLong()),
                transaction.transaction.gasLimit.toGethInteger(),
                transaction.transaction.gasPrice.toGethInteger(),
                transaction.transaction.input.toByteArray()
        )
        val gethKeystore = (keyStore as GethBackedWallethKeyStore).keyStore
        val accounts = gethKeystore.accounts
        val index = (0..(accounts.size() - 1)).firstOrNull { accounts.get(it).address.hex.toUpperCase() == transaction.transaction.from.hex.toUpperCase() }

        if (index == null) {
            transaction.transaction.error = "No key for sending account"
            transaction.transaction.unSignedRLP = newTransaction.encodeRLP().asList()
            transaction.transaction.txHash = newTransaction.hash.hex
        } else {
            gethKeystore.unlock(accounts.get(index), DEFAULT_PASSWORD)

            val signHash = gethKeystore.signHash(transaction.transaction.from.toGethAddr(), newTransaction.sigHash.bytes)
            val transactionWithSignature = newTransaction.withSignature(signHash)

            transaction.transaction.txHash = transactionWithSignature.hash.hex
            transaction.transaction.signedRLP = transactionWithSignature.encodeRLP().asList()
            transaction.transaction.sigHash = newTransaction.sigHash.hex

        }

        if (previousTxHash != null) {
            transactionProvider.updateTransaction(previousTxHash, transaction)
        } else {
            transactionProvider.addTransaction(transaction)
        }

    }

}
