package org.walleth.activities


import android.os.Bundle
import org.walleth.R


class PreferenceActivity : BaseSubActivity() ,  ToolbarColorChangeDetector by DefaultToolbarChangeDetector() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_prefs)

        supportActionBar?.subtitle = getString(R.string.preferences_activity_subtitle)
    }

    override fun onResume() {
        super.onResume()

        if (didToolbarColorChange()) {
            recreate()
        }
    }
}