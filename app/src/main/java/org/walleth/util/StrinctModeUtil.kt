package org.walleth.util

import android.os.Build
import android.os.StrictMode
import androidx.annotation.RequiresApi
import timber.log.Timber
import java.util.concurrent.Executors

@RequiresApi(Build.VERSION_CODES.P)
fun enableStrictMode() {
    StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder()
            .detectAll().penaltyListener(Executors.newSingleThreadExecutor()) { violation ->
                if (violation.javaClass.name.equals("android.os.strictmode.untaggedsocketviolation", ignoreCase = true)) {
                    Timber.v(violation)
                }
            }
            .build()
    )
}
