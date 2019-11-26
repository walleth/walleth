package org.walleth.nfc

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_nfc_enter_credentials.*
import org.ligi.kaxt.setVisibility
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.base_activities.BaseSubActivity
import org.walleth.data.EXTRA_KEY_NFC_CREDENTIALS

class NFCEnterCredentialsActivity : BaseSubActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_nfc_enter_credentials)

        radio_new_card.setOnCheckedChangeListener { _, _ ->
            refresh()
        }

        fab.setOnClickListener {
            when {
                input_pin.text?.length != 6 -> alert("The PIN must have 6 digits")
                input_puk.text?.length != 12 && isNewCard() -> alert("The PUK must have 12 digits")
                input_pairingpwd.text.isNullOrBlank() -> alert("The pairing password cannot be blank")
                else -> {
                    val nfcCredentials = NFCCredentials(
                            isNewCard = radio_new_card.isChecked,
                            pin = input_pin.text.toString(),
                            puk = input_puk.text.toString(),
                            pairingPassword = input_pairingpwd.text.toString()
                    )
                    val resultIntent = Intent().putExtra(EXTRA_KEY_NFC_CREDENTIALS, nfcCredentials)
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                }
            }
        }

        refresh()
    }

    private fun refresh() {
        puk_input_layout.setVisibility(isNewCard())
    }

    private fun isNewCard() = radio_new_card.isChecked
}
