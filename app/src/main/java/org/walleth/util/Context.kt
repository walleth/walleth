package org.walleth.util

import android.content.Context
import androidx.appcompat.app.AlertDialog
import org.walleth.R

fun Context.question(configurator: AlertDialog.Builder.(builder: AlertDialog.Builder) -> Unit, action: () -> Unit) {
    AlertDialog.Builder(this)
            .apply { configurator(this) }
            .setPositiveButton(android.R.string.ok) { _, _ -> action.invoke() }
            .setNegativeButton(R.string.no) { dialog, _ -> dialog.dismiss() }
            .show()
}