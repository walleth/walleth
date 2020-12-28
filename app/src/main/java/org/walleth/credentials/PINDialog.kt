package org.walleth.credentials

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
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
        maxLength: Int = 10,
        title: Int = R.string.please_enter_your_pin
) {
    val view = inflate(R.layout.pinput)
    var dialogPin = ""
    val displayPin = {
        view.pin_textview.text = "*".repeat(dialogPin.length)
        view.pin_back.setVisibility(dialogPin.isNotEmpty())
    }
    displayPin.invoke()

    // GridView seems the better choice here - but has a problem I could not overcome:
    // https://github.com/walleth/walleth/issues/485
    // if anyone can make it work with a GridView (which is nicer code-wise) - PR welcome!

    var horizontalButtonContainer = LinearLayout(this)

    pinPadMapping.forEachIndexed { index, number ->
        if ((index % 3) == 0) {
            horizontalButtonContainer = LinearLayout(this)
            view.grid_container.addView(horizontalButtonContainer)
        }

        horizontalButtonContainer.addView(MaterialButton(this).apply {
            text = if (labelButtons) number.toString() else "*"
            setOnClickListener {
                if (dialogPin.length <= maxLength)
                    dialogPin += number
                displayPin.invoke()
            }

            setTextColor(ContextCompat.getColor(this@showPINDialog, R.color.onAccent))

            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(5, 5, 5, 5)
            }
            setVisibility(number != null, View.INVISIBLE)
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
                .setTitle(title)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    onPIN.invoke(dialogPin)
                }
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                    onCancel.invoke()
                }
                .setOnCancelListener {
                    onCancel.invoke()
                }
                .show()
    }
}