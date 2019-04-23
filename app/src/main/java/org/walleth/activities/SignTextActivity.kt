package org.walleth.activities

import android.app.Activity
import androidx.lifecycle.Observer
import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_sign_text.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kethereum.crypto.toHex
import org.kethereum.eip191.signWithEIP191PersonalSign
import org.kethereum.keystore.api.KeyStore
import org.koin.android.ext.android.inject
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.data.AppDatabase
import org.walleth.data.DEFAULT_PASSWORD
import org.walleth.data.networks.CurrentAddressProvider

class SignTextActivity : BaseSubActivity() {

    private val keyStore: KeyStore by inject()
    private val currentAddressProvider: CurrentAddressProvider by inject()
    private val appDatabase: AppDatabase by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_sign_text)

        val currentAddress = currentAddressProvider.getCurrentNeverNull()

        val key = keyStore.getKeyForAddress(currentAddress, DEFAULT_PASSWORD)

        if (key == null) {
            GlobalScope.launch(Dispatchers.Main) {
                val accountName = withContext(Dispatchers.Default) {
                    appDatabase.addressBook.byAddress(currentAddress)?.name ?: currentAddress.hex
                }
                alert("No key for $accountName") {
                    finish()
                }
            }
        } else {
            appDatabase.addressBook.byAddressLiveData(currentAddress).observe(this, Observer { entry ->
                supportActionBar?.subtitle = "Signing as " + (entry?.name ?: currentAddress.hex)
            })

            val text = intent.getStringExtra(Intent.EXTRA_TEXT)

            textToSign.text = text

            fab.setOnClickListener {

                val signature = key.signWithEIP191PersonalSign(text.toByteArray())

                val putExtra = Intent()
                        .putExtra("SIGNATURE", signature.toHex())
                        .putExtra("ADDRESS", currentAddress.cleanHex)
                setResult(Activity.RESULT_OK, putExtra)
                finish()
            }
        }
    }

}
