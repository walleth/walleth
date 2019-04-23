package org.walleth.activities.trezor

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import android.widget.RadioButton
import com.google.protobuf.GeneratedMessageV3
import com.google.protobuf.Message
import kotlinx.android.synthetic.main.hd_derivation_select.view.*
import org.kethereum.bip44.BIP44
import org.kethereum.model.Address
import org.ligi.kaxt.doAfterEdit
import org.ligi.kaxt.inflate
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.data.ACCOUNT_TYPE_TREZOR
import org.walleth.data.DEFAULT_ETHEREUM_BIP44_PATH
import org.walleth.data.EXTRA_KEY_ACCOUNTSPEC
import org.walleth.data.EXTRA_KEY_ADDRESS
import org.walleth.data.addressbook.AccountKeySpec
import org.walleth.kethereum.android.TransactionParcel

fun Intent?.hasAddressResult() = this?.hasExtra(EXTRA_KEY_ADDRESS) == true
fun Intent?.getAddressResult() = this?.getStringExtra(EXTRA_KEY_ADDRESS)

class TrezorGetAddressActivity : BaseTrezorActivity() {

    val transaction by lazy { intent.getParcelableExtra<TransactionParcel>("TX").transaction }

    private var isDerivationDialogShown = false
    private val initialBIP44 = BIP44(DEFAULT_ETHEREUM_BIP44_PATH)
    private var currentAddress: Address? = null

    private val currentDerivationDialogView by lazy {
        inflate(R.layout.hd_derivation_select)
    }

    override fun handleAddress(address: Address) {
        currentAddress = address

        if (!isDerivationDialogShown) {
            showDerivationDialog()
            isDerivationDialogShown = true
        }
        currentDerivationDialogView.address.text = if (currentBIP44 != null) address.hex else "?"

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentBIP44 = initialBIP44
        supportActionBar?.subtitle = getString(R.string.activity_subtitle_get_trezor_address)
    }

    override fun onResume() {
        super.onResume()
        handler.post(mainRunnable)
    }

    override fun handleExtraMessage(res: Message?) = Unit // we ony care for addresses
    override fun getTaskSpecificMessage(): GeneratedMessageV3? = null // and have no specific task

    private fun showDerivationDialog() {

        var myBIP44 = initialBIP44
        val radioGroup = currentDerivationDialogView.derivation_radiogroup

        currentDerivationDialogView.derivation_text.doAfterEdit {
            currentBIP44 = try {
                BIP44(currentDerivationDialogView.derivation_text.text.toString())
            } catch (e: IllegalArgumentException) {
                initialBIP44
            }
            enterNewState(STATES.READ_ADDRESS)
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
                        resultIntent.putExtra(EXTRA_KEY_ACCOUNTSPEC, AccountKeySpec(ACCOUNT_TYPE_TREZOR, derivationPath = currentBIP44.toString()))
                        setResult(Activity.RESULT_OK, resultIntent)
                        finish()
                    }
                }
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                    finish()
                }
                .show()
    }


}
