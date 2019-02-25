package org.walleth.dataprovider

import android.arch.lifecycle.*
import android.content.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.kethereum.functions.encodeRLP
import org.kethereum.model.Address
import org.kethereum.rpc.EthereumRPC
import org.koin.android.ext.android.inject
import org.ligi.kaxt.livedata.nonNull
import org.ligi.kaxt.livedata.observe
import org.ligi.tracedroid.logging.Log
import org.walleth.data.AppDatabase
import org.walleth.data.balances.Balance
import org.walleth.data.balances.upsertIfNewerBlock
import org.walleth.data.networks.CurrentAddressProvider
import org.walleth.data.networks.NetworkDefinitionProvider
import org.walleth.data.networks.findNetworkDefinition
import org.walleth.data.tokens.CurrentTokenProvider
import org.walleth.data.tokens.isRootToken
import org.walleth.data.transactions.TransactionEntity
import org.walleth.data.transactions.setHash
import org.walleth.kethereum.blockscout.ALL_BLOCKSCOUT_SUPPORTED_NETWORKS
import org.walleth.khex.toHexString
import java.math.BigInteger

class DataProvidingService : LifecycleService() {

    private val okHttpClient: OkHttpClient by inject()
    private val currentAddressProvider: CurrentAddressProvider by inject()
    private val tokenProvider: CurrentTokenProvider by inject()
    private val appDatabase: AppDatabase by inject()
    private val networkDefinitionProvider: NetworkDefinitionProvider by inject()

    private val blockScoutApi = BlockScoutAPI(networkDefinitionProvider, appDatabase, okHttpClient)

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

        currentAddressProvider.observe(this, ResettingObserver())
        networkDefinitionProvider.observe(this, ResettingObserver())

        lifecycle.addObserver(TimingModifyingLifecycleObserver())

        GlobalScope.launch {

            while (true) {
                last_run = System.currentTimeMillis()

                val currentChainId = networkDefinitionProvider.getCurrent().chain.id.value
                currentAddressProvider.value?.let { address ->
                    if (ALL_BLOCKSCOUT_SUPPORTED_NETWORKS.contains(currentChainId)) {
                        tryFetchFromBlockscout(address)
                    }

                    queryRPCForBalance(address)
                }

                while ((last_run + timing) > System.currentTimeMillis() && !shortcut) {
                    delay(100)
                }
                shortcut = false
            }
        }


        relayTransactionsIfNeeded()
        return START_STICKY
    }

    private fun relayTransactionsIfNeeded() {
        appDatabase.transactions.getAllToRelayLive().nonNull().observe(this) { transactionList ->
            transactionList.filter { it.signatureData != null && it.transactionState.relayed.isEmpty() }.forEach { sendTransaction(it) }
        }
    }

    private fun sendTransaction(transaction: TransactionEntity) {
        val chain = transaction.transaction.chain
        val baseURL = chain?.id?.findNetworkDefinition()?.rpcEndpoints?.firstOrNull()

        if (baseURL == null) {
            transaction.transactionState.error = "RPC url not found for chain $chain"
        } else {
            val rpc = EthereumRPC(baseURL, okHttpClient)

            GlobalScope.launch(Dispatchers.IO) {
                val result = rpc.sendRawTransaction(transaction.transaction.encodeRLP(transaction.signatureData).toHexString())

                if (result != null) {
                    if (result.error?.message != null) {
                        if (result.error?.message != "Transaction with the same hash was already imported.") {
                            // this error should not be surfaced to user
                            transaction.transactionState.error = result.toString()
                        }
                    } else {
                        val newHash = result.result
                        val oldHash = transaction.hash
                        transaction.setHash(if (!newHash.startsWith("0x")) "0x$newHash" else newHash)

                        transaction.transactionState.eventLog = transaction.transactionState.eventLog ?: "" + "relayed via ${rpc.baseURL}"
                        transaction.transactionState.relayed = rpc.baseURL

                        appDatabase.transactions.deleteByHash(oldHash)
                        appDatabase.transactions.upsert(transaction)
                    }
                }

            }
        }
    }

    private fun tryFetchFromBlockscout(address: Address) {
        blockScoutApi.queryTransactions(address.hex)
    }

    private fun queryRPCForBalance(address: Address) {

        networkDefinitionProvider.value?.let { currentNetwork ->
            val baseURL = networkDefinitionProvider.getCurrent().rpcEndpoints.firstOrNull()
            val currentToken = tokenProvider.getCurrent()

            if (baseURL == null) {
                Log.e("no RPC URL found for " + networkDefinitionProvider.getCurrent())
            } else {
                val rpc = EthereumRPC(baseURL, okHttpClient)

                val blockNumberString = rpc.blockNumber()?.result
                val blockNumber = blockNumberString?.replace("0x", "")?.toLongOrNull(16)
                if (blockNumber != null) {
                    val balance = if (currentToken.isRootToken()) {
                        rpc.getBalance(address, blockNumberString)
                    } else {
                        val json = "{\"to\":\"" + currentToken.address.hex + "\",\"data\":\"0x70a08231" + ("0".repeat(24)) + "${address.cleanHex}\"}"
                        rpc.call(json, blockNumberString)
                    }

                    if (balance?.error == null && balance?.result != null) {
                        try {
                            appDatabase.balances.upsertIfNewerBlock(
                                    Balance(address = address,
                                            block = blockNumber,
                                            balance = BigInteger(balance.result.replace("0x", ""), 16),
                                            tokenAddress = currentToken.address,
                                            chain = currentNetwork.chain
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