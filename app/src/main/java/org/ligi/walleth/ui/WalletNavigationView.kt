package org.ligi.walleth.ui

import android.content.Context
import android.support.design.widget.NavigationView
import android.util.AttributeSet
import android.view.View
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import kotlinx.android.synthetic.main.navigation_drawer_header.view.*
import org.ligi.kaxt.startActivityFromClass
import org.ligi.walleth.R
import org.ligi.walleth.activities.EditAccountActivity
import org.ligi.walleth.activities.ImportActivity
import org.ligi.walleth.activities.PreferenceActivity
import org.ligi.walleth.activities.ShowAccountBarCodeActivity
import org.ligi.walleth.data.addressbook.AddressBook
import org.ligi.walleth.data.keystore.WallethKeyStore

class WalletNavigationView(context: Context, attrs: AttributeSet) : NavigationView(context, attrs), ChangeObserver {

    var headerView: View? = null
    val addressBook: AddressBook by LazyKodein(appKodein).instance()
    val keyStore: WallethKeyStore by LazyKodein(appKodein).instance()

    override fun observeChange() {

        headerView?.let { header ->
            addressBook.getEntryForName(keyStore.getCurrentAddress()).let {
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

                    context.startActivityFromClass(ShowAccountBarCodeActivity::class.java)
                    /*
                    val keyJSON = keyStore.exportCurrentKey(unlockPassword = "default", exportPassword = "default")


*/
                    true
                }

                R.id.menu_load -> {
                    context.startActivityFromClass(ImportActivity::class.java)
                    true
                }

                R.id.menu_settings -> {
                    context.startActivityFromClass(PreferenceActivity::class.java)
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
