package org.walleth.util

import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import org.walleth.base_activities.BaseSubActivity


fun BaseSubActivity.showInputAlert(@StringRes title:  Int, action : (input: String) -> Unit) {
    val editText = EditText(this)
    val container = FrameLayout(this)
    val params = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
    params.setMargins(16.toDp(), 0, 16.toDp(), 0)
    editText.layoutParams = params
    editText.isSingleLine = true
    container.addView(editText)

    AlertDialog.Builder(this)
            .setTitle(title)
            .setView(container)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.cancel()
                action(editText.text.toString())
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
}
