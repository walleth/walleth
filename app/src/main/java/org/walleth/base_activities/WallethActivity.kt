package org.walleth.base_activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.WindowManager.LayoutParams.FLAG_SECURE
import androidx.appcompat.app.AppCompatActivity
import org.koin.android.ext.android.inject
import org.walleth.App
import org.walleth.data.config.Settings

@SuppressLint("Registered")
open class WallethActivity : AppCompatActivity() {

    protected val settings: Settings by inject()

    public override fun onCreate(savedInstanceState: Bundle?) {
        App.activeActivities.add(this)

        if (settings.isScreenshotsDisabled()) {
            window.setFlags(FLAG_SECURE, FLAG_SECURE)
        }
        super.onCreate(savedInstanceState)
    }


    override fun onDestroy() {
        super.onDestroy()
        App.activeActivities.remove(this)
    }

    override fun onResume() {
        App.visibleActivities.add(this)

        App.onActivityToForegroundObserver.forEach {
            it.invoke()
        }

        super.onResume()
    }

    override fun onPause() {
        App.visibleActivities.remove(this)
        super.onPause()
    }
}