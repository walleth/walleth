package org.walleth

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import org.ligi.kaxt.startActivityFromClass
import org.walleth.activities.StartupActivity

class DebugTrampolin : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startActivityFromClass(StartupActivity::class.java)
        finish()
    }
}
