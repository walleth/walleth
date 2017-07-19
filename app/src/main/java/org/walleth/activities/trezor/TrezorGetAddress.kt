package org.walleth.activities.trezor

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.widget.RadioButton
import com.google.protobuf.GeneratedMessageV3
import com.google.protobuf.Message
import kotlinx.android.synthetic.main.hd_derivation_select.view.*
import org.kethereum.bip44.BIP44
import org.kethereum.model.Address
import org.ligi.kaxt.doAfterEdit
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.kethereum.android.TransactionParcel

private val ADDRESS_HEX_KEY = "address_hex"
private val ADDRESS_PATH = "address_path"
fun Intent.hasAddressResult() = hasExtra(ADDRESS_HEX_KEY)
fun Intent.getAddressResult() = getStringExtra(ADDRESS_HEX_KEY)
fun Intent.getPATHResult() = getStringExtra(ADDRESS_PATH)

class TrezorGetAddress : BaseTrezorActivity() {

    var isDerivationDialogShown = false

    val transaction by lazy { intent.getParcelableExtra<TransactionParcel>("TX").transaction }

    val initialBIP44 = BIP44.fromPath("m/44'/60'/0'/0/0")
    var currentAddress: Address? = null

    val currentDerivationDialogView by lazy {
        inflater.inflate(R.layout.hd_derivation_select, null)!!
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

    override fun handleExtraMessage(res: Message?) = Unit // we ony care for addresses
    override fun getTaskSpecificMessage(): GeneratedMessageV3? = null // and have no specific task

    private fun showDerivationDialog() {

        var myBIP44 = initialBIP44
        val radioGroup = currentDerivationDialogView.derivation_radiogroup

        currentDerivationDialogView.derivation_text.doAfterEdit {
            currentBIP44 = try {
                BIP44.fromPath(currentDerivationDialogView.derivation_text.text.toString())
            } catch (e: IllegalArgumentException) {
                initialBIP44
            }
            enterNewState(STATES.READ_ADDRESS)
        }

        for (i in 1..5) {

            val radioButton = RadioButton(this).apply {
                text = "$i. Account"
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
            text = "Custom"
            setOnClickListener {
                currentDerivationDialogView.derivation_text.isEnabled = true
            }
        })
        radioGroup.check(radioGroup.getChildAt(0).id)

        AlertDialog.Builder(this)
                .setView(currentDerivationDialogView)
                .setTitle("Select Address")
                .setPositiveButton(android.R.string.ok, { _, _ ->
                    if (currentBIP44 == null || currentAddress == null) {
                        alert("No valid input")
                    } else {
                        val resultIntent = Intent()
                        resultIntent.putExtra(ADDRESS_HEX_KEY, currentAddress!!.hex)
                        resultIntent.putExtra(ADDRESS_PATH, currentBIP44.toString())
                        setResult(Activity.RESULT_OK, resultIntent)
                        finish()
                    }
                })
                .setNegativeButton(android.R.string.cancel, { _, _ ->
                    finish()
                })
                .show()
    }


}
