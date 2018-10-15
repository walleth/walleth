package org.walleth.activities


import android.os.Bundle
import org.walleth.R


class PreferenceActivity : BaseSubActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_prefs)
    }

}