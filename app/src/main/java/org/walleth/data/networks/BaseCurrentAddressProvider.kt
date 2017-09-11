package org.walleth.data.networks

import android.arch.lifecycle.MutableLiveData
import org.kethereum.model.Address

open class BaseCurrentAddressProvider : MutableLiveData<Address>() {

    fun setCurrent(value: Address) {
        setValue(value)
    }

    fun getCurrent() = value!!
}