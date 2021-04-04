package org.walleth.overview

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.paging.Pager
import androidx.paging.PagingConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import org.kethereum.model.AddressOnChain
import org.kethereum.model.ChainId
import org.walleth.chains.ChainInfoProvider
import org.walleth.data.AppDatabase
import org.walleth.data.addresses.CurrentAddressProvider

class TransactionListViewModel(app: Application,
                               val appDatabase: AppDatabase,
                               val currentAddressProvider: CurrentAddressProvider,
                               val chainInfoProvider: ChainInfoProvider) : AndroidViewModel(app) {

    val isOnboardingVisible = MutableLiveData<Boolean>().apply { value = false }

    private val pagingConfig by lazy { PagingConfig(50) }

    private suspend fun getAddressOnChainFlow() = currentAddressProvider.flow.filterNotNull().combine(chainInfoProvider.getFlow()) { address, chain ->
        AddressOnChain(address, ChainId(chain.chainId))
    }

    suspend fun isEmptyViewVisibleFlow() = getAddressOnChainFlow().map {
        !appDatabase.transactions.isTransactionForAddressOnChainExisting(it.address, it.chain.value)
    }

    suspend fun getIncomingTransactionsPager() = getAddressOnChainFlow().map {
        val dataSource = appDatabase.transactions.getIncomingPaged(it.address, it.chain.value)
        Pager(pagingConfig, pagingSourceFactory = dataSource.asPagingSourceFactory(Dispatchers.IO))
    }

    suspend fun getOutgoingTransactionsPager() = getAddressOnChainFlow().map {
        val dataSource = appDatabase.transactions.getOutgoingPaged(it.address, it.chain.value)
        Pager(pagingConfig, pagingSourceFactory = dataSource.asPagingSourceFactory(Dispatchers.IO))
    }
}