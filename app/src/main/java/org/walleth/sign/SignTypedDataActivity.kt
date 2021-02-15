package org.walleth.sign

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.activity_sign_text.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kethereum.crypto.signMessageHash
import org.kethereum.crypto.toHex
import org.kethereum.eip712.MoshiAdapter
import org.kethereum.keystore.api.KeyStore
import org.kethereum.model.Address
import org.koin.android.ext.android.inject
import org.komputing.khex.extensions.toHexString
import org.ligi.compat.HtmlCompat
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.base_activities.BaseSubActivity
import org.walleth.data.*
import org.walleth.data.addresses.CurrentAddressProvider
import org.walleth.data.addresses.getSpec
import org.walleth.nfc.getNFCSignTextIntent
import org.walleth.util.security.getPasswordForAccountType
import pm.gnosis.eip712.*

private const val STRUCTURED_DATA_EXTRA = "StructuredData"

fun Context.getSignTypedDataIntent(text: String) = Intent(this, SignTypedDataActivity::class.java).apply {
    putExtra(STRUCTURED_DATA_EXTRA, text)
}

class SignTypedDataActivity : BaseSubActivity() {

    private val keyStore: KeyStore by inject()
    private val currentAddressProvider: CurrentAddressProvider by inject()

    private val currentAddress by lazy { currentAddressProvider.getCurrentNeverNull() }
    private val appDatabase: AppDatabase by inject()

    private val text by lazy {
        intent.getStringExtra(STRUCTURED_DATA_EXTRA) ?: throw (IllegalStateException("no EXTRA_TEXT passed in SignTextActivity"))
    }

    private val domainWithMessage by lazy {
        EIP712JsonParser(MoshiAdapter()).parseMessage(text)
    }

    private fun Literal712.toHumanString() = value.toKotlinType().let {
        if (it is ByteArray) it.toHexString()
        else it.toString()
    }
    private fun Struct712Parameter.toDisplayString() : String = when(type) {
            is Literal712 ->"<b>"+ name + ":</b> " + (type as Literal712).toHumanString()
            is Struct712 -> "<br/><b>" + name + ":</b><br/>" + (type as Struct712).parameters.joinToString("<br/>") { "\t" + it.toDisplayString() } + "<br/>"
    }

    private fun List<Struct712Parameter>.toDisplayString() = joinToString("<br/>") { it.toDisplayString() }.replace("<br/><br/><br/>","<br/><br/>")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_sign_typed_data)

        textToSign.text =  HtmlCompat.fromHtml(domainWithMessage.domain.parameters.toDisplayString() + "<br/>" + domainWithMessage.message.parameters.toDisplayString())

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
                            startActivityForResult(getNFCSignTextIntent(text, currentAddress.cleanHex), REQUEST_CODE_NFC)
                        }
                    }
                    ACCOUNT_TYPE_TREZOR -> alert("signing text not yet supported for TREZOR")
                    ACCOUNT_TYPE_WATCH_ONLY -> fab.setOnClickListener {
                        alert("You have no key to sign with this account")
                    }
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


                val payload = typedDataHash(domainWithMessage.message, domainWithMessage.domain)
                val signature = signMessageHash(payload, key, false)

                val putExtra = Intent()
                        .putExtra("SIGNATURE", signature.toHex())
                        .putExtra("ADDRESS", currentAddress.cleanHex)
                setResult(Activity.RESULT_OK, putExtra)
                finish()
            }
        }
    }
}
