package org.walleth.security

import android.os.Bundle
import kotlinx.android.synthetic.main.activity_security.*
import org.koin.android.ext.android.inject
import org.walleth.R
import org.walleth.base_activities.BaseSubActivity
import org.walleth.data.AppDatabase

class SecurityActivity : BaseSubActivity() {

    val tabItems = listOf(
            SecurityTab(R.string.info, R.drawable.ic_action_info_outline, SecurityInfoFragment()),
            SecurityTab(R.string.rpc, R.drawable.rpc, RPCConfigFragment()),
            SecurityTab(R.string.sourcify, R.drawable.ic_sourcify_logo, SourcifyConfigFragment())
    )

    val appDatabase: AppDatabase by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_security)

        supportActionBar?.subtitle = "Security/Privacy"

        view_pager.adapter = SecurityTabFragmentActivityStateAdapter(tabItems, activity = this)

        getLayoutMediator(tabs, tabItems, view_pager).attach()
    }

}

