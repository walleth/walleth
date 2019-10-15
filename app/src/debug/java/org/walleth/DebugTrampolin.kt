package org.walleth

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.ligi.kaxt.startActivityFromClass
import org.walleth.chains.SwitchChainActivity

class DebugTrampolin : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startActivityFromClass(SwitchChainActivity
        ::class.java)
        finish()
    }
}
