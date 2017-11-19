package org.walleth.core

import android.arch.lifecycle.LifecycleService
import android.arch.lifecycle.Observer
import android.content.Intent
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import org.ethereum.geth.BigInt
import org.ethereum.geth.Geth
import org.kethereum.functions.encodeRLP
import org.walleth.R
import org.walleth.data.AppDatabase
import org.walleth.data.DEFAULT_PASSWORD
import org.walleth.data.keystore.GethBackedWallethKeyStore
import org.walleth.data.keystore.WallethKeyStore
import org.walleth.data.networks.NetworkDefinitionProvider
import org.walleth.data.transactions.TransactionEntity
import org.walleth.data.transactions.TransactionSource
import org.walleth.data.transactions.getTransactionToSignWithGethLive
import org.walleth.data.transactions.setHash
import org.walleth.kethereum.geth.extractSignatureData
import org.walleth.kethereum.geth.toGethTransaction
import java.math.BigInteger.ONE
import java.math.BigInteger.ZERO


class GethTransactionSigner : LifecycleService() {

    private val lazyKodein = LazyKodein(appKodein)

    private val keyStore: WallethKeyStore by lazyKodein.instance()
    private val appDatabase: AppDatabase by lazyKodein.instance()
    private val networkDefinitionProvider: NetworkDefinitionProvider by lazyKodein.instance()

    val observer = Observer<List<TransactionEntity>> {
        it?.forEach {
            signTransaction(it)
        }
    }

    private val liveData by lazy { appDatabase.transactions.getTransactionToSignWithGethLive() }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (!liveData.hasObservers()) {
            liveData.observe(this, observer)
        }

        return START_STICKY
    }

    private fun signTransaction(transaction: TransactionEntity) = async(CommonPool) {
        if (transaction.transactionState.needsSigningConfirmation) {
            return@async
        }

        val oldHash = transaction.hash
        transaction.transactionState.source = TransactionSource.WALLETH

        transaction.transaction.from?.let { notNullFrom ->
            if (transaction.transaction.nonce == null) {
                val lastNonce = appDatabase.transactions.getNonceForAddress(notNullFrom, networkDefinitionProvider.getCurrent().chain)
                transaction.transaction.nonce = lastNonce.max() ?: ZERO + ONE
            }

            val newTransaction = transaction.transaction.toGethTransaction()
            val gethKeystore = (keyStore as GethBackedWallethKeyStore).keyStore
            val accounts = gethKeystore.accounts

            val index = (0..(accounts.size() - 1)).firstOrNull {
                accounts.get(it).address.hex.toUpperCase() == notNullFrom.hex.toUpperCase()
            }

            when {
                transaction.signatureData != null -> { // coming from TREZOR
                    val newTransactionFromRLP = Geth.newTransactionFromRLP(transaction.transaction.encodeRLP())
                    transaction.setHash(newTransactionFromRLP.hash.hex)
                }
                index == null -> {
                    transaction.transactionState.error = getString(R.string.no_key_for_sending_account)
                    transaction.setHash(newTransaction.hash.hex)
                }
                else -> {
                    val relevantAddress = accounts.get(index)
                    gethKeystore.unlock(relevantAddress, DEFAULT_PASSWORD)

                    val transactionWithSignature = gethKeystore.signTx(relevantAddress, newTransaction, BigInt(transaction.transaction.chain!!.id))

                    gethKeystore.lock(relevantAddress.address)

                    transaction.setHash(transactionWithSignature.hash.hex)
                    transaction.signatureData = transactionWithSignature.extractSignatureData()

                }
            }

        }
        async(CommonPool) {
            appDatabase.transactions.deleteByHash(oldHash)
            transaction.transactionState.gethSignProcessed = true
            appDatabase.transactions.upsert(transaction)

        }
    }

}
