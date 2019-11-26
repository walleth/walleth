package org.walleth

import android.os.Bundle
import android.view.View
import org.ligi.kaxt.startActivityFromURL
import org.walleth.base_activities.WallethActivity

class CommonURIsActivity : WallethActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.uri_tests)

        findViewById<View>(R.id.test_uri_main).setOnClickListener {
            startActivityFromURL("ethereum:0xABC")
        }

        findViewById<View>(R.id.test_uri_main).setOnClickListener {
            startActivityFromURL("ethereum:0xABC@4")
        }

        findViewById<View>(R.id.test_uri_main).setOnClickListener {
            startActivityFromURL("ethereum:0xABC@3")
        }

    }
}
