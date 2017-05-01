package org.ligi.walleth.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import kotlinx.android.synthetic.main.activity_import_json.*
import org.ligi.walleth.R
import org.ligi.walleth.data.keystore.WallethKeyStore
import org.ligi.walleth.iac.BarCodeIntentIntegrator
import org.ligi.walleth.iac.BarCodeIntentIntegrator.QR_CODE_TYPES


class ImportActivity : AppCompatActivity() {

    val READ_REQUEST_CODE = 42
    val keyStore: WallethKeyStore by LazyKodein(appKodein).instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_import_json)

        supportActionBar?.subtitle = getString(R.string.import_json_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        fab.setOnClickListener {
            val alertBuilder = AlertDialog.Builder(this)
            try {
                val importKey = keyStore.importKey(inport_json_text.text.toString(), importPassword = password.text.toString(), newPassword = "default")
                alertBuilder
                        .setMessage("Imported " + importKey?.hex)
                        .setTitle("Success")
            } catch(e: Exception) {
                alertBuilder
                        .setMessage(e.message)
                        .setTitle("Error")
            }
            alertBuilder.setPositiveButton("OK", null).show()
        }
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_import, menu)
        return super.onCreateOptionsMenu(menu)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int,
                                         resultData: Intent?) {


        resultData?.let {
            if (it.hasExtra("SCAN_RESULT")) {
                inport_json_text.setText(it.getStringExtra("SCAN_RESULT"))
            }
            if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {

                inport_json_text.setText(readTextFromUri(it.data))

            }
        }


    }

    private fun readTextFromUri(uri: Uri) = contentResolver.openInputStream(uri).reader().readText()


    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {

        R.id.menu_open -> {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "*/*"

            startActivityForResult(intent, READ_REQUEST_CODE)
            true
        }

        R.id.menu_scan -> {
            BarCodeIntentIntegrator(this).initiateScan(QR_CODE_TYPES)
            true
        }

        android.R.id.home -> {
            finish()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}
