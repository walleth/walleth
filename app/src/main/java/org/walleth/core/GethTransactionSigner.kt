package org.walleth.core

import android.app.Service
import android.content.Intent
import android.os.Binder
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import org.walleth.data.config.Settings
import org.walleth.data.keystore.WallethKeyStore
import org.walleth.data.transactions.Transaction
import org.walleth.data.transactions.TransactionProvider
import org.walleth.data.transactions.TransactionSource
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
       // depends on https://github.com/ethereum/go-ethereum/issues/14443
    }

}
