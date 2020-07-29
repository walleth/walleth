package org.walleth.security

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.dappnode_config.*
import org.koin.android.ext.android.inject
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.data.AppDatabase
import org.walleth.data.config.DappNodeMode
import org.walleth.data.config.Settings

class DappNodeConfigFragment : Fragment() {

    val appDatabase: AppDatabase by inject()
    val settings: Settings by inject()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.dappnode_config, container, false)

    override fun onStart() {
        super.onStart()

        start_openvpn_button.setOnClickListener {
            if (!requireContext().startOpenVPN(settings)) {
                requireContext().alert("Cannot start OpenVPN - please install!")
            }
        }

        dappnode_vpn_profile_name.setText(settings.dappNodeVPNProfile)
        dappnode_vpn_profile_name.doOnTextChanged { text, _, _, _ ->
            settings.dappNodeVPNProfile = text.toString()
        }

        dappnode_autostart_vpn.isChecked = settings.dappNodeAutostartVPN
        dappnode_autostart_vpn.setOnCheckedChangeListener { _, isChecked ->
            settings.dappNodeAutostartVPN = isChecked
        }

        when (settings.dappNodeMode) {
            DappNodeMode.DONT_USE -> radio_dont_use_dappnode
            DappNodeMode.USE_WHEN_POSSIBLE -> radio_use_dappnode_when_possible
            DappNodeMode.ONLY_USE_DAPPNODE -> radio_only_use_dappnode
        }.isChecked = true

        radio_group_dappnode_mode.setOnCheckedChangeListener { _, _ ->
            settings.dappNodeMode = when {
                radio_dont_use_dappnode.isChecked -> DappNodeMode.DONT_USE
                radio_use_dappnode_when_possible.isChecked -> DappNodeMode.USE_WHEN_POSSIBLE
                radio_only_use_dappnode.isChecked -> DappNodeMode.ONLY_USE_DAPPNODE
                else -> throw RuntimeException("radio_group_dappnode_mode must have selection")
            }
        }

    }


}
