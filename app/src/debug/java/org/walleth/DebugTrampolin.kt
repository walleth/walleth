package org.walleth

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.ligi.kaxt.startActivityFromClass
import org.walleth.startup.StartupActivity

class DebugTrampolin : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startActivityFromClass(StartupActivity::class.java)
        // w startActivity(Intent().setData(Uri.parse("ethereum:0xf88Feb65eBbe4c32589a4f2da9A1d10CF69594cc@5/store?uint256=2")))

        //startActivity(Intent().setData(Uri.parse("ethereum:0x77f833124e5b896FfF8674869A6eA3A8ce7a5012@5")))

     //   startActivity(Intent().setData(Uri.parse("ethereum:0x961FaFF29974DbF4191F993d4B68Ce1cB73208d8@5/mint?address=0x8e23ee67d1332ad560396262c48ffbb01f93d052&uint256=2")))
        finish()
/*
        val intent = Intent(this, CreateTransactionActivity::class.java).apply {
            this.data = Uri.parse("ethereum:0x5182ac9946680b77a34c92ec4bb2ceb9ed87729e?value=660800215509")
                putExtra("data", "0x4fce048c624f3bc1eb18d397222bddad361766fe")
        }

        startActivity(intent)
*/

    }
}
