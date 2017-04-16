package org.ligi.walleth.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_account_creation.*
import net.glxn.qrgen.android.QRCode
import org.ligi.kaxt.startActivityFromClass
import org.ligi.walleth.App
import org.ligi.walleth.R
import org.ligi.walleth.data.toWallethAddress
import org.ligi.walleth.iac.toERC67String

class CreateAccountActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_account_creation)
        supportActionBar?.subtitle = getString(R.string.account_creation_subtitle)

        App.currentAddress = App.keyStore.newAccount("default").address.toWallethAddress()

        fab.setOnClickListener {
            startActivityFromClass(MainActivity::class.java)
            finish()
        }

        new_account_qrcode.setImageBitmap(QRCode.from(App.keyStore.accounts[0].address.toERC67String()).bitmap())

    }
}
