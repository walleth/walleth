package org.walleth.sign

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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
import org.kethereum.wallet.model.InvalidPasswordException
import org.koin.android.ext.android.inject
import org.komputing.khex.extensions.hexToByteArray
import org.komputing.khex.extensions.toHexString
import org.komputing.khex.model.HexString
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.base_activities.BaseSubActivity
import org.walleth.data.*
import org.walleth.data.addresses.CurrentAddressProvider
import org.walleth.data.addresses.getSpec
import org.walleth.nfc.getNFCSignTextIntent
import org.walleth.trezor.getTrezorSignTextIntent
import org.walleth.util.security.getPasswordForAccountType

class SignTextActivity : BaseSubActivity() {

    private val keyStore: KeyStore by inject()
    private val currentAddressProvider: CurrentAddressProvider by inject()

    private val currentAddress by lazy { currentAddressProvider.getCurrentNeverNull() }
    private val appDatabase: AppDatabase by inject()

    private val signWithNFCForResult: ActivityResultLauncher<Intent> = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val putExtra = Intent()
                .putExtra("SIGNATURE", it.data?.getStringExtra("HEX"))
                .putExtra("ADDRESS", currentAddress.cleanHex)
        setResult(Activity.RESULT_OK, putExtra)

        finish()
    }

    private val text by lazy {
        HexString(intent.getStringExtra(Intent.EXTRA_TEXT) ?: throw (IllegalStateException("no EXTRA_TEXT passed in SignTextActivity")))
                .hexToByteArray()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_sign_text)

        textToSign.text = String(text)

        startSignFlow()
    }

    private fun startSignFlow() {
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
                            signWithNFCForResult.launch(getNFCSignTextIntent(text.toHexString(), currentAddress.cleanHex))
                        }
                    }
                    ACCOUNT_TYPE_TREZOR -> {
                        signWithNFCForResult.launch(getTrezorSignTextIntent(text.toHexString(), currentAddress))
                    }
                    ACCOUNT_TYPE_WATCH_ONLY -> fab.setOnClickListener {
                        alert("You have no key to sign with this account")
                    }
                    ACCOUNT_TYPE_KEEPKEY -> {
                        alert("signing text not yet supported for KeepKey")
                    }
                }
            }
        }
    }

    private fun signTextWithPassword(currentAddress: Address, password: String) {
        val key = try {
            keyStore.getKeyForAddress(currentAddress, password)
        } catch (e: InvalidPasswordException) {
            alert("Invalid Password - try again") {
                startSignFlow()
            }
            return
        }

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

                try {
                    val signature = key.signWithEIP191PersonalSign(text)

                    val putExtra = Intent()
                        .putExtra("SIGNATURE", signature.toHex())
                        .putExtra("ADDRESS", currentAddress.cleanHex)
                    setResult(Activity.RESULT_OK, putExtra)
                    finish()
                } catch (e: Exception) {
                    alert("Could not sign. Reason: " + e.message)
                }
            }
        }
    }

}
