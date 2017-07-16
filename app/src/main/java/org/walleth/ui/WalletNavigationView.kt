package org.walleth.ui

import android.content.Context
import android.support.design.widget.NavigationView
import android.util.AttributeSet
import android.view.View
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import kotlinx.android.synthetic.main.activity_main_in_drawer_container.view.*
import kotlinx.android.synthetic.main.navigation_drawer_header.view.*
import org.ligi.kaxt.startActivityFromClass
import org.walleth.R
import org.walleth.activities.*
import org.walleth.data.addressbook.AddressBook
import org.walleth.data.keystore.WallethKeyStore
import org.walleth.data.networks.NetworkDefinitionProvider

class WalletNavigationView(context: Context, attrs: AttributeSet) : NavigationView(context, attrs), ChangeObserver {

    var headerView: View? = null
    val addressBook: AddressBook by LazyKodein(appKodein).instance()
    val keyStore: WallethKeyStore by LazyKodein(appKodein).instance()
    val networkDefinitionProvider: NetworkDefinitionProvider by LazyKodein(appKodein).instance()

    override fun observeChange() {

        headerView?.let { header ->
            addressBook.getEntryForName(keyStore.getCurrentAddress())?.let {
                header.accountHash.text = it.address.hex
                header.accountName.text = it.name
            }

            val networkName = networkDefinitionProvider.currentDefinition.getNetworkName()
            menu.findItem(R.id.menu_switch_network).title = "Network: $networkName (switch)"
            menu.findItem(R.id.menu_save).setVisible(keyStore.hasKeyForForAddress(keyStore.getCurrentAddress()))
        }
    }


    override fun inflateHeaderView(res: Int): View = super.inflateHeaderView(res).apply {
        headerView = this
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        setNavigationItemSelectedListener {
            rootView.drawer_layout.closeDrawers()
            when (it.itemId) {
                R.id.menu_switch_network -> {
                    context.startActivityFromClass(SwitchNetworkActivity::class.java)
                    true
                }
                R.id.menu_debug -> {
                    context.startActivityFromClass(DebugWallethActivity::class.java)
                    true
                }
                R.id.menu_edit -> {
                    context.startActivityFromClass(EditAccountActivity::class.java)
                    true
                }

                R.id.menu_save -> {
                    context.startActivityFromClass(ExportKeyActivity::class.java)
                    true
                }

                R.id.menu_switch -> {
                    context.startActivityFromClass(SwitchAccountActivity::class.java)
                    true
                }

                R.id.menu_load -> {
                    context.startActivityFromClass(ImportActivity::class.java)
                    true
                }

                R.id.menu_offline_transaction -> {
                    context.startActivityFromClass(OfflineTransactionActivity::class.java)
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
        keyStore.registerChangeObserver(this)
        networkDefinitionProvider.registerChangeObserver(this)
    }


    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        addressBook.unRegisterChangeObserver(this)
        keyStore.unRegisterChangeObserver(this)
        networkDefinitionProvider.registerChangeObserver(this)
    }

}
