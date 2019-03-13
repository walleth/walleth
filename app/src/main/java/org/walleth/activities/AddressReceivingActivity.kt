package org.walleth.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import org.kethereum.model.Address
import org.koin.android.ext.android.inject
import org.walleth.data.networks.CurrentAddressProvider


fun Context.startAddressReceivingActivity(address: Address, clazz: Class<out AddressReceivingActivity>) {

    startActivity(
            Intent(this, clazz).apply {
                putExtra(INTENT_KEY_ADDRESS, address.cleanHex)
            }
    )
}

@SuppressLint("Registered")
open class AddressReceivingActivity: BaseSubActivity() {

    protected val currentAddressProvider: CurrentAddressProvider by inject()

    protected val relevantAddress by lazy {
        if (intent?.hasExtra(INTENT_KEY_ADDRESS) == true) {
            Address(intent.getStringExtra(INTENT_KEY_ADDRESS))
        } else {
            currentAddressProvider.getCurrentNeverNull()
        }
    }

}