package org.walleth.security

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import org.koin.android.ext.android.inject
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.data.AppDatabase
import org.walleth.data.config.DappNodeMode
import org.walleth.data.config.Settings
import org.walleth.databinding.DappnodeConfigBinding

class DappNodeConfigFragment : Fragment() {

    val binding by lazy { DappnodeConfigBinding.inflate(layoutInflater)}

    val appDatabase: AppDatabase by inject()
    val settings: Settings by inject()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.dappnode_config, container, false)

    override fun onStart() {
        super.onStart()

        binding.startOpenvpnButton.setOnClickListener {
            if (!requireContext().startOpenVPN(settings)) {
                requireContext().alert("Cannot start OpenVPN - please install!")
            }
        }

        binding.dappnodeVpnProfileName.setText(settings.dappNodeVPNProfile)
        binding.dappnodeVpnProfileName.doOnTextChanged { text, _, _, _ ->
            settings.dappNodeVPNProfile = text.toString()
        }

        binding.dappnodeAutostartVpn.isChecked = settings.dappNodeAutostartVPN
        binding.dappnodeAutostartVpn.setOnCheckedChangeListener { _, isChecked ->
            settings.dappNodeAutostartVPN = isChecked
        }

        when (settings.dappNodeMode) {
            DappNodeMode.DONT_USE -> binding.radioDontUseDappnode
            DappNodeMode.USE_WHEN_POSSIBLE -> binding.radioUseDappnodeWhenPossible
            DappNodeMode.ONLY_USE_DAPPNODE -> binding.radioOnlyUseDappnode
        }.isChecked = true

        binding.radioGroupDappnodeMode.setOnCheckedChangeListener { _, _ ->
            settings.dappNodeMode = when {
                binding.radioDontUseDappnode.isChecked -> DappNodeMode.DONT_USE
                binding.radioUseDappnodeWhenPossible.isChecked -> DappNodeMode.USE_WHEN_POSSIBLE
                binding.radioOnlyUseDappnode.isChecked -> DappNodeMode.ONLY_USE_DAPPNODE
                else -> throw RuntimeException("radio_group_dappnode_mode must have selection")
            }
        }

    }


}
