package org.walleth.activities


import android.os.Bundle
import org.walleth.R


class PreferenceActivity : BaseSubActivity() {

    var firstResume = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_prefs)
        firstResume = true

        supportActionBar?.subtitle = getString(R.string.preferences_activity_subtitle)
    }

    override fun onResume() {
        super.onResume()

        if (!firstResume) {
            // to apply the new toolbar configuration
            recreate()
        }

        firstResume = false
    }
}