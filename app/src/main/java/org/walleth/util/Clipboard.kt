package org.walleth.util

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.support.design.widget.Snackbar
import android.view.View
import org.kethereum.model.Address
import org.walleth.R


fun Activity.copyToClipboard(address: Address, view: View) {
    copyToClipboard("ethereum:${address.hex}", view)
}

fun Activity.copyToClipboard(ethereumString: String, view: View) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.primaryClip = ClipData.newPlainText(getString(R.string.clipboard_copy_name), ethereumString)
    Snackbar.make(view, R.string.copied_to_clipboard, Snackbar.LENGTH_LONG).show()
}
