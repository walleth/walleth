package org.walleth.ui

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.ViewGroup
import kotlinx.android.synthetic.main.activity_main_in_drawer_container.view.*
import kotlinx.android.synthetic.main.navigation_drawer_header.view.*
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance
import org.ligi.kaxt.startActivityFromClass
import org.walleth.R
import org.walleth.activities.*
import org.walleth.data.AppDatabase
import org.walleth.data.config.Settings
import org.walleth.data.keystore.WallethKeyStore
import org.walleth.data.networks.CurrentAddressProvider
import org.walleth.data.networks.NetworkDefinitionProvider

class WalletNavigationFragment : Fragment(), KodeinAware {

    override val kodein by closestKodein()

    val keyStore: WallethKeyStore by instance()
    val settings: Settings by instance()
    val networkDefinitionProvider: NetworkDefinitionProvider by instance()
    val currentAddressProvider: CurrentAddressProvider by instance()
    val appDatabase: AppDatabase by instance()

    private val navigationView by lazy {
        NavigationView(activity).apply {
            inflateMenu(R.menu.navigation_drawer)
            inflateHeaderView(R.layout.navigation_drawer_header)
            getHeaderView(0).edit_account_image.setOnClickListener {
                context.startActivityFromClass(EditAccountActivity::class.java)
            }

        }
    }

    override fun onResume() {
        super.onResume()
        (navigationView.getHeaderView(0) as ViewGroup).apply {
            setBackgroundColor(settings.toolbarBackgroundColor)
            colorize(settings.toolbarForegroundColor)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val idToClassMap = mapOf(
                R.id.menu_switch_network to SwitchNetworkActivity::class,
                R.id.menu_debug to DebugWallethActivity::class,
                R.id.menu_keys to KeysActivity::class,
                R.id.menu_accounts to SwitchAccountActivity::class,
                R.id.menu_offline_transaction to OfflineTransactionActivity::class,
                R.id.menu_settings to PreferenceActivity::class,
                R.id.menu_security to SecurityInfoActivity::class
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

        currentAddressProvider.observe(this, Observer { address ->
            appDatabase.addressBook.byAddressLiveData(address!!).observe(this@WalletNavigationFragment, Observer { currentAddress ->
                navigationView.getHeaderView(0).let { header ->
                    currentAddress?.let { entry ->
                        header.accountHash.text = entry.address.hex
                        header.accountName.text = entry.name
                    }
                }

            })
        })

        networkDefinitionProvider.observe(this, Observer {
            val networkName = networkDefinitionProvider.value!!.getNetworkName()
            navigationView.menu.findItem(R.id.menu_switch_network).title = "Network: $networkName (switch)"
        })

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = navigationView

}