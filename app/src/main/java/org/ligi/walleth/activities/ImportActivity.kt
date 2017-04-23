package org.ligi.walleth.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_import_json.*
import org.ligi.walleth.App
import org.ligi.walleth.R


class ImportActivity : AppCompatActivity() {

    val READ_REQUEST_CODE = 42

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_import_json)

        supportActionBar?.subtitle = getString(R.string.import_json_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        fab.setOnClickListener {
            try {
                val importKey = App.keyStore.importKey(inport_json_text.text.toString().toByteArray(), "default", "default")
                AlertDialog.Builder(this)
                        .setMessage("Imported " + importKey.address.hex)
                        .setPositiveButton("OK",null)
                        .setTitle("Success").show()
            } catch(e:Exception) {
                AlertDialog.Builder(this)
                        .setMessage(e.message)
                        .setPositiveButton("OK",null)
                        .setTitle("Error").show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_import,menu)
        return super.onCreateOptionsMenu(menu)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int,
                                         resultData: Intent?) {


        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (resultData != null) {
                inport_json_text.setText(readTextFromUri(resultData.data))
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

        android.R.id.home -> {
            finish()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}
