package org.walleth.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.activity_sign_text.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kethereum.crypto.toHex
import org.kethereum.eip191.signWithEIP191PersonalSign
import org.kethereum.keystore.api.KeyStore
import org.kethereum.model.Address
import org.koin.android.ext.android.inject
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.activities.nfc.getNFCSignTextIntent
import org.walleth.data.*
import org.walleth.data.addressbook.getSpec
import org.walleth.data.networks.CurrentAddressProvider
import org.walleth.khex.hexToByteArray
import org.walleth.khex.toHexString
import org.walleth.util.security.getPasswordForAccountType

class SignTextActivity : BaseSubActivity() {

    private val keyStore: KeyStore by inject()
    private val currentAddressProvider: CurrentAddressProvider by inject()

    private val currentAddress by lazy { currentAddressProvider.getCurrentNeverNull() }
    private val appDatabase: AppDatabase by inject()

    private val text by lazy { intent.getStringExtra(Intent.EXTRA_TEXT).hexToByteArray() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_sign_text)

        textToSign.text = String(text)

        lifecycleScope.launch(Dispatchers.Default) {


            val account = appDatabase.addressBook.byAddress(currentAddress)

            lifecycleScope.launch(Dispatchers.Main) {
                when (val type = account.getSpec()?.type) {
                    ACCOUNT_TYPE_PIN_PROTECTED, ACCOUNT_TYPE_BURNER, ACCOUNT_TYPE_PASSWORD_PROTECTED -> getPasswordForAccountType(type) { pwd ->
                        if (pwd != null) {
                            signTextWithPassword(currentAddress, pwd)
                        }
                    }
                    ACCOUNT_TYPE_NFC -> {
                        fab.setImageResource(R.drawable.ic_nfc_black)
                        fab.setOnClickListener {
                            startActivityForResult(getNFCSignTextIntent(text.toHexString(), currentAddress.cleanHex), REQUEST_CODE_NFC)
                        }
                    }
                    ACCOUNT_TYPE_TREZOR -> alert("signing text not yet supported for TREZOR")
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val putExtra = Intent()
                .putExtra("SIGNATURE", data?.getStringExtra("HEX"))
                .putExtra("ADDRESS", currentAddress.cleanHex)
        setResult(Activity.RESULT_OK, putExtra)

        finish()
    }

    private fun signTextWithPassword(currentAddress: Address, password: String) {
        val key = keyStore.getKeyForAddress(currentAddress, password)

        if (key == null) {
            lifecycleScope.launch(Dispatchers.Main) {
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



            fab.setOnClickListener {

                val signature = key.signWithEIP191PersonalSign(text)

                val putExtra = Intent()
                        .putExtra("SIGNATURE", signature.toHex())
                        .putExtra("ADDRESS", currentAddress.cleanHex)
                setResult(Activity.RESULT_OK, putExtra)
                finish()
            }
        }
    }

}
