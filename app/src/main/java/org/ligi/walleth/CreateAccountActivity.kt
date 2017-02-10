package org.ligi.walleth

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_account_creation.*
import org.ligi.kaxt.startActivityFromClass

class CreateAccountActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_account_creation)
        supportActionBar?.subtitle = "Account Creation"

        App.accountManager.newAccount("default")

        fab.setOnClickListener {
            startActivityFromClass(MainActivity::class.java)
            finish()
        }
    }
}
