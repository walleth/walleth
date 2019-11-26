package org.walleth.nfc

import android.annotation.SuppressLint
import android.nfc.NfcAdapter.getDefaultAdapter
import android.os.Bundle
import org.koin.android.ext.android.inject
import org.walleth.R
import org.walleth.base_activities.BaseSubActivity
import org.walleth.khartwarewallet.KHardwareManager
import org.walleth.khartwarewallet.enableKhardwareReader

@SuppressLint("Registered")
open class BaseNFCActivity : BaseSubActivity() {

    protected val nfcCredentialStore: NFCCredentialStore by inject()

    private val nfcAdapter by lazy {
        getDefaultAdapter(this)
    }

    protected  val cardManager by lazy { KHardwareManager() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_nfc)

    }

    public override fun onResume() {
        super.onResume()
        nfcAdapter?.enableKhardwareReader(this, cardManager)
    }

    @SuppressLint("NewApi")
    public override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
    }
}
