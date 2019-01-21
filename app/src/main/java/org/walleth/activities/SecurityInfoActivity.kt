package org.walleth.activities


import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import com.scottyab.rootbeer.RootBeer
import kotlinx.android.synthetic.main.activity_security_info.*
import kotlinx.android.synthetic.main.activity_security_item.view.*
import org.ligi.compat.HtmlCompat
import org.walleth.R
import org.walleth.activities.ProblemLevel.*
import org.walleth.util.security.getDaysSincePatch
import org.walleth.util.security.isDeviceLockScreenProtected

private enum class ProblemLevel {
    GREEN,
    ORANGE,
    RED
}

private data class SecurityInfoItem(val level: ProblemLevel, val message: String)

class SecurityInfoActivity : BaseSubActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_security_info)

        supportActionBar?.run {
            subtitle = getString(R.string.security_info)
        }
    }

    override fun onResume() {
        super.onResume()

        val inflater = LayoutInflater.from(this)

        val infoList = listOf(
                getPatchInfo(),
                getRootInfo(this),
                getLockInfo(this),
                getNoAuditWarning()
        )

        security_info_content.removeAllViews()

        infoList.forEach {
            val view = inflater.inflate(R.layout.activity_security_item, security_info_content, false)

            view.security_info_text.text = HtmlCompat.fromHtml(it.message)
            view.security_info_icon.setImageResource(when (it.level) {
                GREEN -> R.drawable.ic_check_box_black_24dp
                ORANGE -> R.drawable.ic_warning_orange_24dp
                RED -> R.drawable.ic_warning_red_24dp
            })
            security_info_content.addView(view)
        }

    }

}

private fun getNoAuditWarning() = SecurityInfoItem(ORANGE, "This software did not yet have an security audit. Please only use it with significant amounts when secured by e.g. a TREZOR.")

private fun getLockInfo(context: Context) = if (isDeviceLockScreenProtected(context)) {
    SecurityInfoItem(GREEN, "Your device has a secured lock screen - so people finding your device cannot simply access your funds. Be aware if you use very simple PINs, patterns or passwords: then you might still be at risk. For obvious reasons we cannot check the quality of your PINs, patterns or passwords. Also on some Android versions it was possible to brute force e.g. the PINs. And if you use fingerprint as protection - be aware they can easily be copied and should just be used for identification, but not authentification.")
} else {
    SecurityInfoItem(RED, "Your device has no (good) lock screen method set. This means if you loose your phone - your funds are at risk. Please consider setting ideally a password there to protect your funds!")
}

private fun getPatchInfo() = getDaysSincePatch().let {
    when {

        it == null -> SecurityInfoItem(RED, "Could not determine the date of your last Android security patch - this is a really bad sign")

        it < 33 -> SecurityInfoItem(GREEN, "Your Android system was patched $it days ago - which is considered good. ")

        it < 123 -> SecurityInfoItem(ORANGE, "Your Android got the last security updates $it days ago - which is reasonable - but could be better. Nag your phone provider to provide you with patches or install a custom ROM with recent security updates. You might also consider a hardware wallet like a TREZOR to mitigate the problem.")

        else -> SecurityInfoItem(RED, "Your Android system was patched $it days ago - which is considered bad as it is old and the chance is high that there are security flaws and your funds are at risk. Nag your phone provider to provide you with patches or install a custom ROM with recent security patches. You might also consider a hardware wallet like a TREZOR to mitigate the problem")
    }
}

private fun getRootInfo(context: Context) = if (RootBeer(context).isRooted) {
    SecurityInfoItem(RED, "There are indicators this phone is rooted. This breaks app-isolation and other apps might be able to access your keys. We will not forbid you to use this app as we hope you know what you are doing (e.g. only use this app for watch-only accounts, use a TREZOR or only have insignificant value in accounts used with WallETH.)")
} else {
    SecurityInfoItem(GREEN, "No indication of rooting found - which is good as this would break app isolation and other apps could potentially access your keys. Please be advised that we can only check for indicators - if you rooted your device and cloaked (e.g. to use banking apps that do not allow to run on rooted phones) it good we might not detect it. If you did so please be aware of the security implications. We will not forbid you to use this app as we hope you know what you are doing (e.g. only use this app for watch-only accounts, use a TREZOR or only have insignificant value in accounts used with WallETH.)")
}
