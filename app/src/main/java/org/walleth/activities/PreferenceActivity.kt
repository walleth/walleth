package org.walleth.activities


import android.os.Bundle
import org.ligi.kaxt.recreateWhenPossible
import org.walleth.R


class PreferenceActivity : BaseSubActivity() {

    var firstResume = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_prefs)
        firstResume = true
    }

    override fun onResume() {
        super.onResume()

        if (!firstResume) {
            // to apply the new toolbar configuration
            recreateWhenPossible()
        }

        firstResume = false
    }
}