package org.walleth.activities

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Point
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import kotlinx.android.synthetic.main.activity_show_qr.*
import net.glxn.qrgen.android.QRCode
import org.ligi.kaxt.doAfterEdit
import org.ligi.kaxt.setVisibility
import org.walleth.R
import org.walleth.data.keystore.WallethKeyStore


class ShowAccountBarCodeActivity : AppCompatActivity() {

    val keyStore: WallethKeyStore by LazyKodein(appKodein).instance()
    lateinit var keyJSON: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_show_qr)

        supportActionBar?.subtitle = getString(R.string.export_account_subtitle)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        generate()
        qrcode_image.setVisibility(false)
        show_qr_switch.setOnCheckedChangeListener { _, isChecked ->
            qrcode_image.setVisibility(isChecked)
        }

        password_input.doAfterEdit {
            generate()
        }
    }

    private fun generate() {
        keyJSON = keyStore.exportCurrentKey(unlockPassword = "default", exportPassword = password_input.text.toString())

        val point = Point()
        windowManager.defaultDisplay.getSize(point)
        val bmpScaled = Bitmap.createScaledBitmap(QRCode.from(keyJSON).bitmap(), point.x, point.x, false)
        qrcode_image.setImageBitmap(bmpScaled)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_export, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> {
            finish()
            true
        }

        R.id.menu_share -> {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, keyJSON)
                type = "text/plain"
            }

            startActivity(sendIntent)
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}
