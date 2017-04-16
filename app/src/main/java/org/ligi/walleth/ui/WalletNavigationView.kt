package org.ligi.walleth.ui

import android.content.Context
import android.support.annotation.LayoutRes
import android.support.design.widget.NavigationView
import android.support.v7.app.AlertDialog
import android.util.AttributeSet
import kotlinx.android.synthetic.main.navigation_drawer_header.view.*
import org.ligi.kaxt.applyIf
import org.ligi.kaxt.startActivityFromClass
import org.ligi.walleth.App
import org.ligi.walleth.R
import org.ligi.walleth.activities.EditAccountActivity

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

    override fun inflateHeaderView(@LayoutRes res: Int) = super.inflateHeaderView(res)!!.applyIf(App.keyStore.accounts.size() > 0) {
        accountHash.text = App.keyStore.accounts[0].address.hex
        editAccountActivity.setOnClickListener {
            context.startActivityFromClass(EditAccountActivity::class.java)
        }
    }

}
