package org.walleth.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.method.LinkMovementMethod
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_parity_signer_qr.*
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance
import org.ligi.compat.HtmlCompat
import org.walleth.R
import org.walleth.data.networks.CurrentAddressProvider
import org.walleth.functions.setQRCode


class ParitySignerQRActivity : AppCompatActivity(), KodeinAware {

    override val kodein: Kodein by closestKodein()

    private val currentAddressProvider: CurrentAddressProvider by instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_parity_signer_qr)

        if (intent.hasExtra("signatureHex")) {
            supportActionBar?.subtitle = "Parity signer QR code (Signed TX)"
            parity_address_qr_image.setQRCode(intent.getStringExtra("signatureHex"))
        } else {
            supportActionBar?.subtitle = "Parity signer QR code (Address)"

            parity_address_qr_image.setQRCode(currentAddressProvider.getCurrent().hex.removePrefix("0x"))
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        textview_parity_explanation.text = HtmlCompat.fromHtml("Please upvote <a href='https://github.com/paritytech/parity-signer/issues/103'>this issue to the parity signer</a> so they use ERC-681 and we can have better UX afterwards.")
        textview_parity_explanation.movementMethod = LinkMovementMethod()
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {

        android.R.id.home -> true.also {
            finish()
        }

        else -> super.onOptionsItemSelected(item)
    }
}
