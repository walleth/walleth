package org.walleth.ui

import androidx.lifecycle.Observer
import android.os.Bundle
import com.google.android.material.navigation.NavigationView
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.ViewGroup
import kotlinx.android.synthetic.main.activity_main_in_drawer_container.view.*
import kotlinx.android.synthetic.main.navigation_drawer_header.view.*
import org.koin.android.ext.android.inject
import org.ligi.kaxt.startActivityFromClass
import org.walleth.R
import org.walleth.activities.*
import org.walleth.data.AppDatabase
import org.walleth.data.config.Settings
import org.walleth.data.networks.CurrentAddressProvider
import org.walleth.data.networks.NetworkDefinitionProvider
import java.security.KeyStore

class WalletNavigationFragment : Fragment() {

    val keyStore: KeyStore by inject()
    val settings: Settings by inject()
    val networkDefinitionProvider: NetworkDefinitionProvider by inject()
    val currentAddressProvider: CurrentAddressProvider by inject()
    val appDatabase: AppDatabase by inject()

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

        navigationView.menu.findItem(R.id.menu_debug).isVisible = settings.showDebug
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val idToClassMap = mapOf(
                R.id.menu_switch_network to SwitchNetworkActivity::class,
                R.id.menu_debug to DebugWallethActivity::class,
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