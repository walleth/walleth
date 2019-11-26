package org.walleth.accounts

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_account_type_select.*
import org.koin.android.ext.android.inject
import org.walleth.R
import org.walleth.base_activities.BaseSubActivity
import org.walleth.data.*
import org.walleth.data.addressbook.AccountKeySpec
import org.walleth.data.networks.CurrentAddressProvider
import org.walleth.qr.scan.QRScanActivityAndProcessActivity

open class NewAccountTypeSelectActivity : BaseSubActivity() {

    val currentAddressProvider: CurrentAddressProvider by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_account_type_select)

        supportActionBar?.subtitle = "New account"

        supportActionBar?.setDisplayHomeAsUpEnabled(currentAddressProvider.getCurrent() != null)

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = AccountTypeAdapter(ACCOUNT_TYPE_LIST, AccountKeySpec(ACCOUNT_TYPE_NONE))

        fab.setOnClickListener {
            startActivityForResult(Intent(this, QRScanActivityAndProcessActivity::class.java), REQUEST_CODE_SCAN_QR)
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