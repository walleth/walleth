package org.walleth.ui

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.ViewGroup
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import kotlinx.android.synthetic.main.activity_main_in_drawer_container.view.*
import kotlinx.android.synthetic.main.navigation_drawer_header.view.*
import org.ligi.kaxt.startActivityFromClass
import org.walleth.R
import org.walleth.activities.*
import org.walleth.data.AppDatabase
import org.walleth.data.keystore.WallethKeyStore
import org.walleth.data.networks.CurrentAddressProvider
import org.walleth.data.networks.NetworkDefinitionProvider

class WalletNavigationFragment : Fragment() {

    private val navigationView by lazy {
        NavigationView(activity).apply {
            inflateMenu(R.menu.navigation_drawer)
            inflateHeaderView(R.layout.navigation_drawer_header)
        }
    }

    val keyStore: WallethKeyStore by LazyKodein(appKodein).instance()
    val networkDefinitionProvider: NetworkDefinitionProvider by LazyKodein(appKodein).instance()
    val currentAddressProvider: CurrentAddressProvider by LazyKodein(appKodein).instance()
    val appDatabase: AppDatabase by LazyKodein(appKodein).instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val idToClassMap = mapOf(
                R.id.menu_switch_network to SwitchNetworkActivity::class,
                R.id.menu_debug to DebugWallethActivity::class,
                R.id.menu_edit to EditAccountActivity::class,
                R.id.menu_save to ExportKeyActivity::class,
                R.id.menu_switch to SwitchAccountActivity::class,
                R.id.menu_load to ImportActivity::class,
                R.id.menu_offline_transaction to OfflineTransactionActivity::class,
                R.id.menu_settings to PreferenceActivity::class
        )


        navigationView.setNavigationItemSelectedListener {
            view!!.rootView.drawer_layout.closeDrawers()
            val classToStart = idToClassMap[it.itemId]
            if (classToStart != null) {
                context?.startActivityFromClass(classToStart)
                true
            } else {
                false
            }
        }

        currentAddressProvider.observe(this, Observer {
            appDatabase.addressBook.byAddressLiveData(it!!).observe(this@WalletNavigationFragment, Observer { currentAddress ->
                navigationView.getHeaderView(0).let { header ->
                    currentAddress?.let {
                        header.accountHash.text = it.address.hex
                        header.accountName.text = it.name
                    }
                }

            })
            navigationView.menu.findItem(R.id.menu_save).isVisible = keyStore.hasKeyForForAddress(it)
        })

        networkDefinitionProvider.observe(this, Observer {
            val networkName = networkDefinitionProvider.value!!.getNetworkName()
            navigationView.menu.findItem(R.id.menu_switch_network).title = "Network: $networkName (switch)"
        })

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = navigationView

}