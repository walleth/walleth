package org.walleth.sign

import android.os.Bundle
import kotlinx.android.synthetic.main.activity_parity_signer_qr.*
import org.koin.android.ext.android.inject
import org.walleth.R
import org.walleth.base_activities.BaseSubActivity
import org.walleth.data.addresses.CurrentAddressProvider
import org.walleth.util.setQRCode


class ParitySignerQRActivity : BaseSubActivity() {

    private val currentAddressProvider: CurrentAddressProvider by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_parity_signer_qr)

        val signatureHex = intent.getStringExtra("signatureHex")
        if (signatureHex != null) {
            supportActionBar?.subtitle = "Parity signer QR code (Signed TX)"

            parity_address_qr_image.setQRCode(signatureHex)
        } else {
            supportActionBar?.subtitle = "Address QR Code"

            parity_address_qr_image.setQRCode("ethereum:" + currentAddressProvider.getCurrent())
        }

    }
}
