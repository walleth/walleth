package org.walleth.activities

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import kotlinx.android.synthetic.main.activity_addresses_transfer.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import org.json.JSONArray
import org.json.JSONObject
import org.kethereum.model.Address
import org.walleth.R
import org.walleth.data.AppDatabase
import org.walleth.data.addressbook.AddressBookEntry
import org.walleth.data.config.Settings
import java.lang.Exception

private const val REQUEST_CODE_EXPORT: Int = 1
private const val REQUEST_CODE_IMPORT: Int = 2
private const val ACTION_EXPORT: String = "ACTION_EXPORT"
private const val ACTION_IMPORT: String = "ACTION_IMPORT"

fun Context.getExportAccountsIntent()
        = Intent(this, TransferAccountsActivity::class.java).apply {
    action = ACTION_EXPORT
}

fun Context.getImportAccountsIntent()
        = Intent(this, TransferAccountsActivity::class.java).apply {
    action = ACTION_IMPORT
}


@TargetApi(Build.VERSION_CODES.KITKAT)
class TransferAccountsActivity : AppCompatActivity() {

    val appDatabase: AppDatabase by LazyKodein(appKodein).instance()
    val settings: Settings by LazyKodein(appKodein).instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_addresses_transfer)

        supportActionBar?.subtitle = getString(R.string.export_addresses_subtitle)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        export_button.setOnClickListener({ v -> pickExportLocation() })
        import_button.setOnClickListener({ v -> pickImportLocation() })

        if (intent.action == ACTION_IMPORT) {
            pickImportLocation()
        } else if (intent.action == ACTION_EXPORT) {
            pickExportLocation()
        }
    }

    fun pickExportLocation() {
        startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT)
                .setData(settings.defaultExportLocation?.let { Uri.parse(it) })
                .setType("text/*")
                .addCategory(Intent.CATEGORY_OPENABLE),
                REQUEST_CODE_EXPORT)
    }

    fun pickImportLocation() {
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT)
                .setData(settings.defaultExportLocation?.let { Uri.parse(it) })
                .setType("text/*")
                .addCategory(Intent.CATEGORY_OPENABLE),
                REQUEST_CODE_IMPORT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK)
            when (requestCode) {
                REQUEST_CODE_EXPORT -> data?.data?.let {
                    settings.defaultExportLocation = it.toString()
                    val takeFlags = data.getFlags() and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    contentResolver.takePersistableUriPermission(it, takeFlags)
                    export(it)
                }
                REQUEST_CODE_IMPORT -> data?.data?.let {
                    import(it)
                }
            }
    }

    private fun export(exportUri: Uri) {
        async(UI) {
            try {
                val addresses = JSONArray()
                async(CommonPool) {

                    appDatabase.addressBook.all().forEach {
                        if (!it.deleted) {
                            addresses.put(
                                    it.toJSONObject()
                            )
                        }
                    }
                    contentResolver.openOutputStream(exportUri).bufferedWriter().use { it.write(addresses.toString()) }
                }.await()

                Toast.makeText(this@TransferAccountsActivity,
                        resources.getQuantityString(R.plurals.addresses_exported, addresses.length(), addresses.length()),
                        Toast.LENGTH_SHORT)
                        .show()
            } catch (e: Exception) {
                Log.d("transfer", "export failed", e)
                Toast.makeText(this@TransferAccountsActivity,
                        R.string.export_failed,
                        Toast.LENGTH_SHORT)
                        .show()
            } finally {
                finish()
            }

        }


    }

    private fun import(importUri: Uri) {
        async(UI) {
            try {
                val count = async(CommonPool) {
                    val addresses = JSONArray(contentResolver.openInputStream(importUri).bufferedReader()
                            .use { it.readText() })
                    addresses.iterator().forEach {
                        appDatabase.addressBook.upsert(it.toAddressBookEntry())
                    }
                    addresses.length()
                }.await()
                Toast.makeText(this@TransferAccountsActivity,
                        resources.getQuantityString(R.plurals.addresses_imported, count, count),
                        Toast.LENGTH_SHORT)
                        .show()
            } catch (e: Exception) {
                Log.d("transfer", "import failed", e)
                Toast.makeText(this@TransferAccountsActivity,
                        R.string.import_failed,
                        Toast.LENGTH_SHORT)
                        .show()
            } finally {
                finish()
            }
        }
    }

    operator fun JSONArray.iterator(): Iterator<JSONObject>
            = (0 until length()).asSequence().map { get(it) as JSONObject }.iterator()

    private fun AddressBookEntry.toJSONObject(): JSONObject {
        return JSONObject().apply {
            put("address", address.hex)
            put("name", name)
            put("note", note)
            put("isNotificationWanted", isNotificationWanted)
            put("trezorDerivationPath", trezorDerivationPath)
            put("starred", starred)
            put("fromUser", fromUser)
            put("order", order)
        }
    }

    private fun JSONObject.toAddressBookEntry(): AddressBookEntry {
        return AddressBookEntry(
                Address(getString("address")),
                getString("name"),
                getStringOrNull("note"),
                getBoolean("isNotificationWanted"),
                getStringOrNull("trezorDerivationPath"),
                getBoolean("starred"),
                false,
                getBoolean("fromUser"),
                getInt("order"))
    }

}

private fun JSONObject.getStringOrNull(name: String) = if (has(name)) getString(name) else null
