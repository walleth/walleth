package org.walleth.util

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.google.android.material.snackbar.Snackbar
import android.view.View
import org.kethereum.erc681.ERC681
import org.kethereum.erc681.generateURL
import org.kethereum.model.Address
import org.walleth.R


fun Activity.copyToClipboard(address: Address, view: View) {
    copyToClipboard(ERC681(address = address.hex).generateURL(), view)
}

fun Activity.copyToClipboard(ethereumString: String, view: View) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.primaryClip = ClipData.newPlainText(getString(R.string.clipboard_copy_name), ethereumString)
    Snackbar.make(view, R.string.copied_to_clipboard, Snackbar.LENGTH_LONG).show()
}
