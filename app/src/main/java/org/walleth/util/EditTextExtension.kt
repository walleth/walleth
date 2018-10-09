package org.walleth.util

import android.widget.EditText

fun EditText.hasText() = text?.isNotBlank() == true
