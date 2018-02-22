package org.walleth.util

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import org.walleth.R

fun Context.question(msg: Int, title: Int, onOkClickListener: DialogInterface.OnClickListener) {
    AlertDialog.Builder(this)
            .setMessage(msg)
            .setTitle(title)
            .setPositiveButton(R.string.ok, onOkClickListener)
            .setNegativeButton(R.string.no, { dialog, _ -> dialog.dismiss() })
            .show()
}