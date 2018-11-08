package org.walleth.ui

import android.app.Activity
import android.support.design.button.MaterialButton
import android.support.v7.app.AlertDialog
import android.widget.GridLayout
import kotlinx.android.synthetic.main.pinput.view.*
import org.ligi.kaxt.inflate
import org.ligi.kaxt.setVisibility
import org.walleth.R

val KEY_MAP_TOUCH = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9)
val KEY_MAP_NUM_PAD = listOf(7, 8, 9, 4, 5, 6, 1, 2, 3) // mainly for TREZOR
val KEY_MAP_TOUCH_WITH_ZERO = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, null, 0, null)

fun Activity.showPINDialog(
        onPIN: (pin: String) -> Unit,
        onCancel: () -> Unit,
        labelButtons: Boolean = false,
        pinPadMapping: List<Int?> = KEY_MAP_TOUCH,
        maxLength: Int = 10
) {
    val view = inflate(R.layout.pinput)
    var dialogPin = ""
    val displayPin = {
        view.pin_textview.text = "*".repeat(dialogPin.length)
        view.pin_back.setVisibility(!dialogPin.isEmpty())
    }
    displayPin.invoke()
    pinPadMapping.forEach { number ->
        view.pin_grid.addView(MaterialButton(this).apply {
            text = if (labelButtons) number.toString() else "*"
            setOnClickListener {
                if (dialogPin.length <= maxLength)
                    dialogPin += number
                displayPin.invoke()
            }


            layoutParams = GridLayout.LayoutParams().apply {
                setMargins(5, 5, 5, 5)
            }
            setVisibility(number != null)
        })

    }
    view.pin_back.setOnClickListener {
        if (dialogPin.isNotEmpty())
            dialogPin = dialogPin.substring(0, dialogPin.length - 1)
        displayPin.invoke()
    }
    if (!isFinishing) {
        AlertDialog.Builder(this)
                .setView(view)
                .setTitle(R.string.trezor_please_enter_your_pin)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    onPIN.invoke(dialogPin)
                }
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                    onCancel.invoke()
                }
                .show()
    }
}