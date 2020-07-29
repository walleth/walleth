package org.walleth.security


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.rpc_config.*
import org.koin.android.ext.android.inject
import org.walleth.R
import org.walleth.data.AppDatabase

class RPCConfigFragment : Fragment() {

    private val tabItems = listOf(
            SecurityTab(R.string.tincubeth, R.drawable.ic_tincubeth_logo_mono, TincubETHFragment()),
            SecurityTab(R.string.dappnode, R.drawable.ic_dappnode, DappNodeConfigFragment())
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.rpc_config, container, false)

    val appDatabase: AppDatabase by inject()

    override fun onStart() {
        super.onStart()

        view_pager.adapter = SecurityTabFragmentStateAdapter(tabItems, fragment = this)
        getLayoutMediator(tabs, tabItems, view_pager).attach()
    }

}
