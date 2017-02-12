package org.ligi.ewallet.ui

import android.content.Context
import android.support.annotation.LayoutRes
import android.support.design.widget.NavigationView
import android.support.v7.app.AlertDialog
import android.util.AttributeSet
import android.view.View
import kotlinx.android.synthetic.main.navigation_drawer_header.view.*
import org.ligi.walleth.App
import org.ligi.walleth.R

class WalletNavigationView(context: Context, attrs: AttributeSet) : NavigationView(context, attrs) {

    init {
        setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.menu_settings -> {
                    AlertDialog.Builder(context).setMessage("TODO").show()
                    true
                }
                else -> false
            }
        }
    }

    override fun inflateHeaderView(@LayoutRes res: Int): View {
        val view = super.inflateHeaderView(res)

        if (App.accountManager.accounts.size() > 0) {
            view.accountHash.text = App.accountManager.accounts[0].address.hex
        }
        return view
    }


}
