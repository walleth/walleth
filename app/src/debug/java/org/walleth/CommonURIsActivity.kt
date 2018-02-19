package org.walleth

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.debug.uri_tests.*
import org.ligi.kaxt.startActivityFromURL

class CommonURIsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.uri_tests)


        test_uri_main.setOnClickListener {
            startActivityFromURL("ethereum:0xABC")
        }


        test_uri_main.setOnClickListener {
            startActivityFromURL("ethereum:0xABC@1")
        }

        test_uri_rinkeby.setOnClickListener {
            startActivityFromURL("ethereum:0xABC@4")
        }

        test_uri_ŕopsten.setOnClickListener {
            startActivityFromURL("ethereum:0xABC@3")
        }

    }
}
