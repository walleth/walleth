package org.walleth.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.WindowManager.LayoutParams.FLAG_SECURE
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance
import org.walleth.data.config.Settings

@SuppressLint("Registered")
open class WallethActivity : AppCompatActivity() , KodeinAware {

    override val kodein by closestKodein()
    private val settings: Settings by instance()

    public override fun onCreate(savedInstanceState: Bundle?) {
        if (settings.isScreenshotsDisabled()) {
            window.setFlags(FLAG_SECURE, FLAG_SECURE)
        }
        super.onCreate(savedInstanceState)
    }

}