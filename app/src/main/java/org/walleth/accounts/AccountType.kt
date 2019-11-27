package org.walleth.accounts

import android.app.Activity
import androidx.annotation.DrawableRes
import org.walleth.data.addresses.AccountKeySpec

data class AccountType(
        val accountType: String?,
        val name: String,
        val action: String,
        val description: String,
        @DrawableRes val drawable: Int,
        @DrawableRes val actionDrawable: Int,
        val wrapsKey: Boolean = false,
        val callback: (context: Activity, inSpec: AccountKeySpec) -> Unit = { _, _ -> }
)