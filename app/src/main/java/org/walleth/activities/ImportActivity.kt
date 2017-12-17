package org.walleth.activities

import android.annotation.TargetApi
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import kotlinx.android.synthetic.main.activity_import_json.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import org.ligi.kaxt.setVisibility
import org.ligi.kaxtui.alert
import org.threeten.bp.LocalDateTime
import org.walleth.R
import org.walleth.activities.qrscan.startScanActivityForResult
import org.walleth.data.AppDatabase
import org.walleth.data.DEFAULT_PASSWORD
import org.walleth.data.addressbook.AddressBookEntry
import org.walleth.data.addressbook.getByAddressAsync
import org.walleth.data.keystore.WallethKeyStore
import java.io.FileNotFoundException

enum class KeyType {
    ECDSA, JSON
}


private const val KEY_INTENT_EXTRA_TYPE = "TYPE"
private const val KEY_INTENT_EXTRA_KEYCONTENT = "KEY"

fun Context.getKeyImportIntent(key: String, type: KeyType) = Intent(this, ImportActivity::class.java).apply {
    putExtra(KEY_INTENT_EXTRA_TYPE, type.toString())
    putExtra(KEY_INTENT_EXTRA_KEYCONTENT, key)
}

private const val READ_REQUEST_CODE = 42

class ImportActivity : AppCompatActivity() {

    private val keyStore: WallethKeyStore by LazyKodein(appKodein).instance()
    private val appDatabase: AppDatabase by LazyKodein(appKodein).instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_import_json)

        intent.getStringExtra(KEY_INTENT_EXTRA_KEYCONTENT)?.let {
            key_content.setText(it)
        }

        val typeExtra = intent.getStringExtra(KEY_INTENT_EXTRA_TYPE)

        type_json_select.isChecked = typeExtra == null || KeyType.valueOf(typeExtra) == KeyType.JSON
        type_ecdsa_select.isChecked = !type_json_select.isChecked

        key_type_select.setOnCheckedChangeListener { _, _ ->
            password.setVisibility(type_json_select.isChecked)
        }

        supportActionBar?.subtitle = getString(R.string.import_json_subtitle)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        fab.setOnClickListener {
            val alertBuilder = AlertDialog.Builder(this)
            try {
                val importKey = if (type_json_select.isChecked)
                    keyStore.importJSONKey(key_content.text.toString(), importPassword = password.text.toString(), storePassword = DEFAULT_PASSWORD)
                else
                    keyStore.importECDSAKey(key_content.text.toString(), storePassword = DEFAULT_PASSWORD)

                alertBuilder
                        .setMessage(getString(R.string.imported_key_alert_message, importKey?.hex))
                        .setTitle(getString(R.string.dialog_title_success))

                if (importKey != null) {
                    appDatabase.addressBook.getByAddressAsync(importKey) { oldEntry ->
                        val accountName = if (account_name.text.isBlank()) {
                            oldEntry?.name ?: getString(R.string.imported_key_default_entry_name)
                        } else {
                            account_name.text
                        }
                        val note = oldEntry?.note ?: getString(R.string.imported_key_entry_note, LocalDateTime.now())


                        async(CommonPool) {
                            appDatabase.addressBook.upsert(AddressBookEntry(name = accountName.toString(), address = importKey, note = note, isNotificationWanted = false, trezorDerivationPath = null))
                        }
                    }

                }
            } catch (e: Exception) {
                alertBuilder
                        .setMessage(e.message)
                        .setTitle(getString(R.string.dialog_title_error))
                        .show()
            }
            alertBuilder.setPositiveButton(android.R.string.ok, null).show()
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_import, menu)
        menu.findItem(R.id.menu_open).isVisible = Build.VERSION.SDK_INT >= 19
        return super.onCreateOptionsMenu(menu)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int,
                                         resultData: Intent?) {


        resultData?.let {
            if (it.hasExtra("SCAN_RESULT")) {
                key_content.setText(it.getStringExtra("SCAN_RESULT"))
            }
            if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {

                key_content.setText(readTextFromUri(it.data))

            }
        }


    }

    private fun readTextFromUri(uri: Uri) = try {
        contentResolver.openInputStream(uri).reader().readText()
    } catch (fileNotFoundException: FileNotFoundException) {
        alert("Cannot read from $uri - if you think I should - please contact ligi@ligi.de with details of the device (Android version,Brand) and the beginning of the uri")
        null
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {

        R.id.menu_open -> true.also {
            tryOpen()
        }

        R.id.menu_scan -> true.also {
            startScanActivityForResult(this)
        }

        android.R.id.home -> true.also {
            finish()
        }

        else -> super.onOptionsItemSelected(item)
    }

    @TargetApi(19)
    private fun tryOpen() {
        try {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "*/*"

            startActivityForResult(intent, READ_REQUEST_CODE)
        } catch (e: ActivityNotFoundException) {
            alert(R.string.saf_activity_not_found_problem)
        }
    }
}
