package org.walleth.accounts

import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.util.Base64
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_show_qr.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.glxn.qrgen.android.QRCode
import org.kethereum.keystore.api.KeyStore
import org.kethereum.wallet.LIGHT_SCRYPT_CONFIG
import org.kethereum.wallet.generateWalletJSON
import org.kethereum.wallet.model.InvalidPasswordException
import org.koin.android.ext.android.inject
import org.ligi.kaxt.doAfterEdit
import org.ligi.kaxt.setVisibility
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.base_activities.AddressReceivingActivity
import org.walleth.data.ACCOUNT_TYPE_BURNER
import org.walleth.data.AppDatabase
import org.walleth.data.REQUEST_CODE_CREATE_DOCUMENT
import org.walleth.data.addresses.getSpec
import org.walleth.util.security.getInvalidStringResForAccountType
import org.walleth.util.security.getPasswordForAccountType
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream

class ExportKeyActivity : AddressReceivingActivity() {

    val keyStore: KeyStore by inject()
    private val appDatabase: AppDatabase by inject()

    private var keyJSON: String? = null
    var mWebView: WebView? = null
    private var currentPassword: String? = null
    private var currentAccountType: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            currentAccountType = appDatabase.addressBook.byAddress(relevantAddress)?.getSpec()?.type

            getPasswordForAccountType(currentAccountType ?: ACCOUNT_TYPE_BURNER) { password ->
                if (password == null) {
                    finish()
                } else {
                    currentPassword = password

                    setContentView(R.layout.activity_show_qr)

                    supportActionBar?.subtitle = getString(R.string.export_account_subtitle)

                    qrcode_image.setVisibility(false)
                    show_qr_switch.setOnCheckedChangeListener { _, isChecked ->
                        qrcode_image.setVisibility(isChecked)
                    }

                    password_input.doAfterEdit {
                        reGenerate()
                        checkConfirmation()
                    }

                    password_input_confirmation.doAfterEdit {
                        checkConfirmation()
                    }

                    checkConfirmation()

                    reGenerate()
                }
            }
        }
    }

    private fun checkConfirmation() {
        confirmation_warning.setVisibility(password_input.text.toString() != password_input_confirmation.text.toString())
    }

    private fun reGenerate() {
        keyJSON = null
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                val bmpScaled = withContext(Dispatchers.Default) {


                    val key = keyStore.getKeyForAddress(relevantAddress, currentPassword!!)

                    keyJSON = key?.generateWalletJSON(password_input.text.toString(), LIGHT_SCRYPT_CONFIG)
                            ?: throw (IllegalStateException("Could not create JSON from key"))

                    val point = Point()
                    windowManager.defaultDisplay.getSize(point)
                    Bitmap.createScaledBitmap(QRCode.from(keyJSON).bitmap(), point.x, point.x, false)


                }
                val bitmapDrawable = BitmapDrawable(resources, bmpScaled)
                bitmapDrawable.setAntiAlias(false)
                qrcode_image.setImageDrawable(bitmapDrawable)
            } catch (e: InvalidPasswordException) {
                alert(getInvalidStringResForAccountType(currentAccountType!!)) {
                    finish()
                }
            }

        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_export, menu)
        menu.findItem(R.id.menu_print).isVisible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {

        R.id.menu_copy_to_clipboard -> true.also {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("key", keyJSON)
            clipboard.setPrimaryClip(clip)
            Snackbar.make(activity_export_key, "Key copied to clipboard", Snackbar.LENGTH_LONG).show()
        }

        R.id.menu_share -> true.also {
            startAfterKeyIsReady {
                val sendIntent = Intent().apply {

                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, keyJSON)
                    type = "text/plain"
                }
                try {
                    startActivity(sendIntent)
                } catch (e: ActivityNotFoundException) {
                    alert("Did not find any app to share with.")
                }
            }
        }

        R.id.menu_save -> true.also {
            startAfterKeyIsReady {
                val sendIntent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {

                    putExtra(Intent.EXTRA_TITLE, relevantAddress.hex + ".key")
                    type = "application/json"
                }
                startActivityForResult(sendIntent, REQUEST_CODE_CREATE_DOCUMENT)
            }
        }


        R.id.menu_print -> true.also {
            doWebViewPrint()
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun startAfterKeyIsReady(action: () -> Unit) = lifecycleScope.launch(Dispatchers.Main) {
        key_progress.visibility = View.VISIBLE

        withContext(Dispatchers.Default) {
            while (keyJSON == null) {
                delay(10)
            }
        }

        key_progress.visibility = View.GONE
        action.invoke()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != Activity.RESULT_OK) {
            return
        }

        data?.data?.let { uri ->
            contentResolver.openFileDescriptor(uri, "w")
        }?.use { fileDescriptor ->
            FileOutputStream(fileDescriptor.fileDescriptor).use {
                it.writer().use { writer ->
                    writer.write(keyJSON)
                }
            }
        }
    }

    private fun doWebViewPrint() = startAfterKeyIsReady {
        val webView = WebView(this)
        webView.webViewClient = object : WebViewClient() {

            @SuppressLint("NewApi") // we hide the menu entry for SDK < 19 so they *should* never land here
            override fun onPageFinished(view: WebView, url: String) {
                createWebPrintJob(view)
                mWebView = null
            }
        }

        val bos = ByteArrayOutputStream()
        val qrCode = QRCode.from(keyJSON)
        qrCode.bitmap().compress(Bitmap.CompressFormat.PNG, 100, bos)
        bos.close()
        val encode = Base64.encodeToString(bos.toByteArray(), Base64.DEFAULT)

        // Generate an HTML document on the fly:
        val htmlDocument = "<html><body><h1>" + getString(R.string.paper_wallet_title) + "</h1>" +
                "<p><font size='21'>" + getString(R.string.paper_wallet_text) + "</font></p>" +
                "<center><img width=\"80%\" src=\"data:image/png;base64,$encode\"/></center></body></html>"
        webView.loadDataWithBaseURL(null, htmlDocument, "text/HTML", "UTF-8", null)

        mWebView = webView
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun createWebPrintJob(webView: WebView) {

        val printAdapter = webView.createPrintDocumentAdapter()

        val jobName = getString(R.string.app_name) + " Document"
        val printManager = getSystemService(Context.PRINT_SERVICE) as PrintManager
        printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())

    }
}
