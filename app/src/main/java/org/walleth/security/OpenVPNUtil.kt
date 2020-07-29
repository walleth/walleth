package org.walleth.security

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
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

fun Context.startOpenVPN(settings: Settings) = startOpenVPN(settings.dappNodeVPNProfile)
fun Context.startOpenVPN(profileName: String): Boolean {
    val openVPN = Intent("android.intent.action.VIEW");
    openVPN.setPackage("net.openvpn.openvpn");
    openVPN.setClassName("net.openvpn.openvpn", "net.openvpn.unified.MainActivity");
    openVPN.putExtra("net.openvpn.openvpn.AUTOSTART_PROFILE_NAME", "PC $profileName");
    openVPN.putExtra("net.openvpn.openvpn.AUTOCONNECT", true);
    openVPN.putExtra("net.openvpn.openvpn.APP_SECTION", "PC");
    if (packageManager?.let { pm -> openVPN.resolveActivity(pm) } != null) {
        try {
            startActivity(openVPN)
            return true
        } catch (e: ActivityNotFoundException) {
        }
    }
    return false
}