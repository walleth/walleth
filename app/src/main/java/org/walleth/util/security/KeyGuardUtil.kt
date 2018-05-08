package org.walleth.util.security

import android.annotation.TargetApi
import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.provider.Settings

fun isDeviceLockScreenProtected(context: Context) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
    isDevicePinOrPatternLocked(context)
} else {
    isPatternSet(context) || isKeyguardSecure(context)
}


/**
 * @return true if pattern set, false if not (or if an issue when checking)
 */
private fun isPatternSet(context: Context) = try {
    1 == Settings.Secure.getInt(context.contentResolver, Settings.Secure.LOCK_PATTERN_ENABLED)
} catch (e: Settings.SettingNotFoundException) {
    false
}


/**
 * @return whether the keyguard is secured by a PIN, pattern or password or a SIM card is currently locked.
 */
@TargetApi(16)
private fun isKeyguardSecure(context: Context) =
        (context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isKeyguardSecure

/**
 * Returns whether the device is secured with a PIN, pattern or password.
 */
@TargetApi(23)
private fun isDevicePinOrPatternLocked(context: Context) =
        (context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isDeviceSecure