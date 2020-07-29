package org.walleth.security

import android.os.Build
import org.ligi.tracedroid.logging.Log
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.temporal.ChronoUnit
import java.util.*

private const val GETPROP_EXECUTABLE_PATH = "/system/bin/getprop"
private val PATCH_LEVEL_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH)

fun getDaysSincePatch() = getSecurityPatchDate()?.let {
    ChronoUnit.DAYS.between(it, LocalDate.now())
}

private fun getSecurityPatchDate() = readSecurityPatchDateString()?.let {
    try {
        LocalDate.parse(it, PATCH_LEVEL_DATE_FORMAT)
    } catch (e: Exception) {
        Log.w("Could not parse date $it")
        null
    }
}

private fun readSecurityPatchDateString(): String? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        return Build.VERSION.SECURITY_PATCH

    var process: Process? = null

    return try {
        process = ProcessBuilder()
                .command(GETPROP_EXECUTABLE_PATH, "ro.build.version.security_patch")
                .redirectErrorStream(true)
                .start()
        process.inputStream.bufferedReader().use { it.readText() }.replace("\n", "")
    } catch (e: Exception) {
        Log.e("Failed to read System Property ro.build.version.security_patch", e)
        null
    } finally {
        process?.destroy()
    }
}
