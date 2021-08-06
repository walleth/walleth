package org.walleth.security

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
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
            e.printStackTrace()
        }
    }

    val openVPNBlinkt = Intent("android.intent.action.MAIN");
    openVPNBlinkt.setPackage("de.blinkt.openvpn");
    openVPNBlinkt.setClassName("de.blinkt.openvpn", "de.blinkt.openvpn.api.ConnectVPN");
    openVPNBlinkt.putExtra("de.blinkt.openvpn.api.profileName", "$profileName");
    if (packageManager?.let { pm -> openVPNBlinkt.resolveActivity(pm) } != null) {
        try {
            startActivity(openVPNBlinkt)
            return true
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
        }
    }


    return false
}