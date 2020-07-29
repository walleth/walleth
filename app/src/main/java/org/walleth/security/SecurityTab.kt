package org.walleth.security

import androidx.annotation.DrawableRes
import androidx.fragment.app.Fragment

data class SecurityTab(val name: String, @DrawableRes val Icon: Int, val fragment: Fragment)