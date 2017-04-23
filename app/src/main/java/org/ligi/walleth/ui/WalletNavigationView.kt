package org.ligi.walleth.ui

import android.content.Context
import android.content.Intent
import android.support.design.widget.NavigationView
import android.support.v7.app.AlertDialog
import android.util.AttributeSet
import android.view.View
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import kotlinx.android.synthetic.main.navigation_drawer_header.view.*
import org.ligi.kaxt.startActivityFromClass
import org.ligi.walleth.App
import org.ligi.walleth.R
import org.ligi.walleth.activities.EditAccountActivity
import org.ligi.walleth.activities.ImportActivity
import org.ligi.walleth.data.addressbook.AddressBook

class WalletNavigationView(context: Context, attrs: AttributeSet) : NavigationView(context, attrs), ChangeObserver {

    var headerView: View? = null
    val addressBook: AddressBook by LazyKodein(appKodein).instance()

    override fun observeChange() {

        headerView?.let { header ->
            addressBook.getEntryForName(App.currentAddress!!)?.let {
                header.accountHash.text = it.address.hex
                header.accountName.text = it.name
            }
        }
    }


    override fun inflateHeaderView(res: Int): View {
        headerView = super.inflateHeaderView(res)

        return headerView!!
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.menu_edit -> {
                    context.startActivityFromClass(EditAccountActivity::class.java)
                    true
                }

                R.id.menu_save -> {

                    val keyJSON = String(App.keyStore.exportKey(App.keyStore.accounts[0], "default", "default"))

                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, keyJSON)
                        type = "text/plain"
                    }

                    context.startActivity(sendIntent)

                    true
                }

                R.id.menu_load -> {
                    context.startActivityFromClass(ImportActivity::class.java)
                    true
                }

                R.id.menu_settings -> {
                    AlertDialog.Builder(context).setMessage("TODO").show()
                    true
                }
                else -> false
            }
        }

        addressBook.registerChangeObserverWithInitialObservation(this)
    }


    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        addressBook.unRegisterChangeObserver(this)
    }

}
