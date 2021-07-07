package org.walleth.trezor

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.RadioButton
import androidx.appcompat.app.AlertDialog
import com.squareup.wire.Message
import kotlinx.android.synthetic.main.hd_derivation_select.view.*
import org.kethereum.model.Address
import org.komputing.kbip44.BIP44
import org.ligi.kaxt.doAfterEdit
import org.ligi.kaxt.inflate
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.data.*
import org.walleth.data.addresses.AccountKeySpec

fun Intent?.hasAddressResult() = this?.hasExtra(EXTRA_KEY_ADDRESS) == true
fun Intent?.getAddressResult() = this?.getStringExtra(EXTRA_KEY_ADDRESS)

class TrezorGetAddressActivity : BaseTrezorActivity() {

    private var isDerivationDialogShown = false
    private val initialBIP44 = BIP44(DEFAULT_ETHEREUM_BIP44_PATH)
    private var currentAddress: Address? = null

    private val currentDerivationDialogView by lazy {
        inflate(R.layout.hd_derivation_select)
    }

    override fun handleAddress(address: Address) = handleNullableAddress(address)

    private fun handleNullableAddress(address: Address?) {
        currentAddress = address

        if (!isDerivationDialogShown) {
            isDerivationDialogShown = true
            showDerivationDialog()
        }

        currentDerivationDialogView.address.text = if (currentBIP44 != null && address != null) address.hex else "Waiting for TREZOR to provide Address"

    }


    var pendingBIP44: BIP44? = null

    override fun enterState(newState: STATES, withConnect: Boolean) {
        super.enterState(newState, withConnect)

        maybeRequestNewAddress(newState)
    }

    private fun maybeRequestNewAddress(withState: STATES = state) {
        if (withState == STATES.IDLE && pendingBIP44 != null) {
            currentBIP44 = pendingBIP44
            enterState(STATES.READ_ADDRESS)
            pendingBIP44 = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentBIP44 = initialBIP44
        supportActionBar?.subtitle = getString(R.string.activity_subtitle_get_trezor_address)
        connectAndExecute()
    }

    override fun handleExtraMessage(res: Message<*, *>?) = Unit // we ony care for addresses
    override suspend fun getTaskSpecificMessage(): Message<*, *>? = null // and have no specific task

    private fun showDerivationDialog() {

        var myBIP44 = initialBIP44
        val radioGroup = currentDerivationDialogView.derivation_radiogroup

        currentDerivationDialogView.derivation_text.doAfterEdit {
            pendingBIP44 = try {
                BIP44(currentDerivationDialogView.derivation_text.text.toString())
            } catch (e: IllegalArgumentException) {
                initialBIP44
            }
            handleNullableAddress(null)
            maybeRequestNewAddress()
        }

        for (i in 1..5) {

            val radioButton = RadioButton(this).apply {
                text = getString(R.string.trezor_account, i)
                val relevantBIP44 = myBIP44.copy()
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        currentDerivationDialogView.derivation_text.setText(relevantBIP44.toString())
                        currentDerivationDialogView.derivation_text.isEnabled = false
                    }
                }
            }
            myBIP44 = myBIP44.increment()
            radioGroup.addView(radioButton)
        }

        radioGroup.addView(RadioButton(this).apply {
            text = getString(R.string.trezor_custom_derivation_path)
            setOnClickListener {
                currentDerivationDialogView.derivation_text.isEnabled = true
            }
        })
        radioGroup.check(radioGroup.getChildAt(0).id)

        AlertDialog.Builder(this)
            .setView(currentDerivationDialogView)
            .setTitle(R.string.trezor_select_address)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                if (currentBIP44 == null || currentAddress == null) {
                    alert(R.string.trezor_no_valid_input)
                } else {
                    val resultIntent = Intent()
                    resultIntent.putExtra(EXTRA_KEY_ADDRESS, currentAddress!!.hex)
                    resultIntent.putExtra(
                        EXTRA_KEY_ACCOUNTSPEC, AccountKeySpec(
                            type = getAccountType(),
                            derivationPath = currentBIP44.toString(),
                            name = currentDeviceName
                        )
                    )
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                }
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                finish()
            }
            .show()
    }

    private fun getAccountType() = if (isKeepKeyDevice) ACCOUNT_TYPE_KEEPKEY else ACCOUNT_TYPE_TREZOR

}
