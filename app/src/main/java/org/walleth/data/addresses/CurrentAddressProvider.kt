package org.walleth.data.addresses

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.flow.MutableStateFlow
import org.kethereum.model.Address
import org.walleth.data.config.Settings

open class CurrentAddressProvider(val settings: Settings) {

    val flow = MutableStateFlow(settings.accountAddress?.let { Address(it) })

    suspend fun setCurrent(value: Address) {
        settings.accountAddress = value.hex
        flow.emit(value)
    }

    fun getCurrent() = flow.value
    fun getCurrentNeverNull() = flow.value!!
}