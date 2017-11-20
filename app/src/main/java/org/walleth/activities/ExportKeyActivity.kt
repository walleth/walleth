package org.walleth.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.support.annotation.RequiresApi
import android.support.v7.app.AppCompatActivity
import android.util.Base64
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebView
import android.webkit.WebViewClient
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import kotlinx.android.synthetic.main.activity_show_qr.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import net.glxn.qrgen.android.QRCode
import org.ligi.kaxt.doAfterEdit
import org.ligi.kaxt.setVisibility
import org.walleth.R
import org.walleth.data.DEFAULT_PASSWORD
import org.walleth.data.keystore.WallethKeyStore
import org.walleth.data.networks.CurrentAddressProvider
import java.io.ByteArrayOutputStream


class ExportKeyActivity : AppCompatActivity() {

    val keyStore: WallethKeyStore by LazyKodein(appKodein).instance()
    val currentAddressProvider: CurrentAddressProvider by LazyKodein(appKodein).instance()

    private lateinit var keyJSON: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_show_qr)

        supportActionBar?.subtitle = getString(R.string.export_account_subtitle)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        qrcode_image.setVisibility(false)
        show_qr_switch.setOnCheckedChangeListener { _, isChecked ->
            qrcode_image.setVisibility(isChecked)
        }

        password_input.doAfterEdit {
            generate()
            checkConfirmation()
        }

        password_input_confirmation.doAfterEdit {
            checkConfirmation()
        }

        checkConfirmation()
        generate()
    }

    private fun checkConfirmation() {
        confirmation_warning.setVisibility(password_input.text.toString() != password_input_confirmation.text.toString())
    }

    private fun generate() {
        async(UI) {
            val bmpScaled = async(CommonPool) {
                keyJSON = keyStore.exportKey(address = currentAddressProvider.getCurrent(), unlockPassword = DEFAULT_PASSWORD, exportPassword = password_input.text.toString())

                val point = Point()
                windowManager.defaultDisplay.getSize(point)
                Bitmap.createScaledBitmap(QRCode.from(keyJSON).bitmap(), point.x, point.x, false)
            }.await()
            val bitmapDrawable = BitmapDrawable(resources, bmpScaled)
            bitmapDrawable.setAntiAlias(false)
            qrcode_image.setImageDrawable(bitmapDrawable)

        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_export, menu)
        menu.findItem(R.id.menu_print).isVisible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
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

        R.id.menu_print -> {
            doWebViewPrint()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    var mWebView: WebView? = null
    private fun doWebViewPrint() {
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
