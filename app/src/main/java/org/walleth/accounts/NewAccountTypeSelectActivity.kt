package org.walleth.accounts

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_account_type_select.*
import org.koin.android.ext.android.inject
import org.walleth.R
import org.walleth.base_activities.BaseSubActivity
import org.walleth.data.*
import org.walleth.data.addresses.AccountKeySpec
import org.walleth.data.addresses.CurrentAddressProvider
import org.walleth.qr.scan.QRScanActivityAndProcessActivity

private const val NO_QR_KEY = "NO_QR"

fun Activity.getNewAccountTypeSelectActivityIntent() = Intent(this, NewAccountTypeSelectActivity::class.java)
fun Activity.getNewAccountTypeSelectActivityNoFABIntent() = getNewAccountTypeSelectActivityIntent().putExtra(NO_QR_KEY, true)

open class NewAccountTypeSelectActivity : BaseSubActivity() {

    val currentAddressProvider: CurrentAddressProvider by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_account_type_select)

        supportActionBar?.subtitle = "New account"

        supportActionBar?.setDisplayHomeAsUpEnabled(currentAddressProvider.getCurrent() != null)

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = AccountTypeAdapter(ACCOUNT_TYPE_LIST, AccountKeySpec(ACCOUNT_TYPE_NONE))

        if (intent.getBooleanExtra(NO_QR_KEY, false)) {
            fab.visibility = View.GONE
        } else {
            fab.setOnClickListener {
                startActivity(Intent(this, QRScanActivityAndProcessActivity::class.java))
                finish()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            when (requestCode) {
                REQUEST_CODE_ENTER_PASSWORD -> {
                    val pwdExtra = data.getStringExtra(EXTRA_KEY_PWD)
                    val spec = AccountKeySpec(ACCOUNT_TYPE_PASSWORD_PROTECTED, pwd = pwdExtra)
                    setResult(resultCode, data.putExtra(EXTRA_KEY_ACCOUNTSPEC, spec))
                }
                REQUEST_CODE_ENTER_PIN -> {
                    val pinExtra = data.getStringExtra(EXTRA_KEY_PIN)
                    val spec = AccountKeySpec(ACCOUNT_TYPE_PIN_PROTECTED, pwd = pinExtra)
                    setResult(resultCode, data.putExtra(EXTRA_KEY_ACCOUNTSPEC, spec))
                }
                else -> {
                    setResult(resultCode, data)
                }
            }
            finish()

        }
    }
}