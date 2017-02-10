package org.ligi.walleth

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import org.ligi.kaxt.startActivityFromClass

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (App.accountManager.accounts.size() == 0L) {
            startActivityFromClass(CreateAccountActivity::class.java)
            finish()
        } else {
            setContentView(R.layout.activity_main)
        }
    }


}
