package org.walleth.util

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.View
import com.google.android.material.snackbar.Snackbar
import org.kethereum.model.Address
import org.walleth.R


fun Activity.copyToClipboard(address: Address, view: View) = copyToClipboard(address.hex, view)

fun Activity.copyToClipboard(string: String, view: View) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.clipboard_copy_name), string))
    Snackbar.make(view, R.string.copied_to_clipboard, Snackbar.LENGTH_LONG).show()
}
