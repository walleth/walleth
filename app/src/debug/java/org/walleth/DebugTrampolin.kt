package org.walleth

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import org.ligi.kaxt.startActivityFromClass
import org.walleth.activities.SelectTokenActivity

class DebugTrampolin : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startActivityFromClass(SelectTokenActivity::class.java)
        finish()
    }
}
