package org.walleth.startup

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.walleth.chains.ChainInfoProvider
import org.walleth.data.addresses.CurrentAddressProvider

class StartupViewModel(private val currentAddressProvider: CurrentAddressProvider,
                       private val currentChainInfoProvider: ChainInfoProvider) : ViewModel() {

    val status = MutableLiveData<StartupStatus>()

    init {
        viewModelScope.launch {

            //1000 * 10ms = 10s
            (0..1000).forEach { _ ->
                if (currentAddressProvider.getCurrent() == null) {
                    status.postValue(StartupStatus.NeedsAddress)
                    return@launch
                }

                if (currentChainInfoProvider.getCurrent() != null) {
                    status.postValue(StartupStatus.HasChainAndAddress)
                    return@launch
                }
                delay(10)

            }

            status.postValue(StartupStatus.Timeout)
        }
    }
}