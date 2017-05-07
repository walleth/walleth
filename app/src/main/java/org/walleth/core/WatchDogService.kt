package org.walleth.core

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.SystemClock
import com.jakewharton.processphoenix.ProcessPhoenix
import org.walleth.activities.DebugWallethActivity

class WatchDogService : Service() {

    val binder by lazy { Binder() }
    override fun onBind(intent: Intent) = binder
    var running = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        running = true
        Thread({

            while (running) {
                WatchdogState.watchdog_round++
                if (WatchdogState.geth_service_running) {
                    if (System.currentTimeMillis() - WatchdogState.geth_last_seen > 4000) {
                        ProcessPhoenix.triggerRebirth(baseContext, Intent(this, DebugWallethActivity::class.java))
                    }
                }
                SystemClock.sleep(1000)
            }
        }).start()


        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        running = false
    }
}
