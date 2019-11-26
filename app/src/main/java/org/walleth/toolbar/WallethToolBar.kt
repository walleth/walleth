package org.walleth.toolbar

import android.content.Context
import android.graphics.ColorFilter
import android.graphics.PorterDuff.Mode.SRC_IN
import android.graphics.PorterDuffColorFilter
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.view.menu.ActionMenuItemView
import androidx.appcompat.widget.Toolbar
import org.walleth.data.config.KotprefSettings

class WallethToolBar(context: Context, attrs: AttributeSet) : Toolbar(context, attrs) {

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        setBackgroundColor(KotprefSettings.toolbarBackgroundColor)
        colorize(KotprefSettings.toolbarForegroundColor)
    }

}

fun ViewGroup.colorize(toolbarIconsColor: Int,
                       colorFilter: ColorFilter = PorterDuffColorFilter(toolbarIconsColor, SRC_IN)) {

    for (i in 0 until childCount) {
        getChildAt(i).doColorizing(colorFilter, toolbarIconsColor)
    }

}

fun View.doColorizing(colorFilter: ColorFilter, toolbarIconsColor: Int) {
    when (this) {

        is ActionMenuItemView -> {
            compoundDrawables.filterNotNull().forEach {
                it.colorFilter = colorFilter
            }
        }

        is ImageView -> {
            drawable.alpha = 255
            drawable.colorFilter = colorFilter
        }
        is TextView -> setTextColor(toolbarIconsColor)

        is ViewGroup -> colorize(toolbarIconsColor, colorFilter)
    }

}
