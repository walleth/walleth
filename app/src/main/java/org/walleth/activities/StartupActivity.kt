package org.walleth.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.init_choice.*
import kotlinx.android.synthetic.main.toolbar.*
import org.kethereum.model.Address
import org.koin.android.ext.android.inject
import org.ligi.kaxt.livedata.nonNull
import org.ligi.kaxt.livedata.observe
import org.ligi.kaxt.startActivityFromClass
import org.walleth.R
import org.walleth.data.networks.CurrentAddressProvider

class StartupActivity : AppCompatActivity() {
    val currentAddressProvider: CurrentAddressProvider by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.init_choice)
        setSupportActionBar(toolbar)
        create_key_button.setOnClickListener {
          startActivityFromClass(CreateKeyActivity::class.java)
        }

        import_key_button.setOnClickListener {
            startActivityForResult(Intent(this, ImportActivity::class.java), TO_ADDRESS_REQUEST_CODE)
        }

        watch_button.setOnClickListener {
            startActivityFromClass(SwitchAccountActivity::class.java)
        }

        currentAddressProvider.nonNull().observe(this) {
            startActivityFromClass(MainActivity::class.java)
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            currentAddressProvider.setCurrent(Address(data.getStringExtra("ADDRESS")))
        }
    }
}