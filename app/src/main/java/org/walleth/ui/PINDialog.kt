package org.walleth.ui

import android.app.Activity
import android.support.v7.app.AlertDialog
import android.widget.Button
import kotlinx.android.synthetic.main.pinput.view.*
import org.ligi.kaxt.inflate
import org.ligi.kaxt.setVisibility
import org.walleth.R

fun Activity.showPINDialog(onPIN: (pin: String) -> Unit, onCancel: () -> Unit) {
    val view = inflate(R.layout.pinput)
    var dialogPin = ""
    val displayPin = {
        view.pin_textview.text = "*".repeat(dialogPin.length)
        view.pin_back.setVisibility(!dialogPin.isEmpty())
    }
    displayPin.invoke()
    val pinPadMapping = arrayOf(7, 8, 9, 4, 5, 6, 1, 2, 3)
    for (i in 0..8) {
        val button = Button(this)
        button.text = "*"
        button.setOnClickListener {
            if (dialogPin.length <= 10)
                dialogPin += pinPadMapping[i]
            displayPin.invoke()
        }
        view.pin_grid.addView(button)
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