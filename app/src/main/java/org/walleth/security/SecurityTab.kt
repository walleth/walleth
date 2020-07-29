package org.walleth.security

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment

data class SecurityTab(@StringRes val name: Int, @DrawableRes val Icon: Int, val fragment: Fragment)