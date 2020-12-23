package org.walleth.base_activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import org.kethereum.model.Address
import org.koin.android.ext.android.inject
import org.walleth.data.addresses.CurrentAddressProvider

private const val INTENT_KEY_ADDRESS = "ADDRESS"

fun Context.startAddressReceivingActivity(address: Address, clazz: Class<out AddressReceivingActivity>) {

    startActivity(
            Intent(this, clazz).apply {
                putExtra(INTENT_KEY_ADDRESS, address.cleanHex)
            }
    )
}

@SuppressLint("Registered")
open class AddressReceivingActivity : BaseSubActivity() {

    protected val currentAddressProvider: CurrentAddressProvider by inject()

    protected val relevantAddress: Address by lazy {
        intent.getStringExtra(INTENT_KEY_ADDRESS)?.let {
            Address(it)
        } ?: currentAddressProvider.getCurrentNeverNull()
    }

}