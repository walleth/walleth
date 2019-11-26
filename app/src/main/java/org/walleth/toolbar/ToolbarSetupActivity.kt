package org.walleth.toolbar


import android.graphics.PorterDuff.Mode.SRC_IN
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import com.madrapps.pikolo.listeners.SimpleColorSelectionListener
import kotlinx.android.synthetic.main.activity_setup_toolbar.*
import kotlinx.android.synthetic.main.toolbar.*
import org.walleth.R
import org.walleth.base_activities.BaseSubActivity
import org.walleth.toolbar.ColorPickMode.Background
import org.walleth.toolbar.ColorPickMode.Foreground

enum class ColorPickMode {
    Foreground, Background
}

class ToolbarSetupActivity : BaseSubActivity() {

    var currentMode = Foreground

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_setup_toolbar)

        supportActionBar?.subtitle = "Customize toolbar"

        color_picker.setColorSelectionListener(object : SimpleColorSelectionListener() {
            override fun onColorSelected(color: Int) = when (currentMode) {
                Foreground -> settings.toolbarForegroundColor = color
                Background -> settings.toolbarBackgroundColor = color
            }.also {
                toolbar.requestLayout()
                setModeButtonColor(color)
            }
        })

        color_picker_mode_button.setOnClickListener {
            currentMode = when (currentMode) {
                Foreground -> Background
                Background -> Foreground
            }
            setModeIcon()

        }

        setModeIcon()
    }


    private fun setModeButtonColor(color: Int) {
        color_picker_mode_button.colorFilter = PorterDuffColorFilter(color, SRC_IN)
    }

    private fun setModeIcon() {
        color_picker_mode_button.setImageResource(when (currentMode) {
            Foreground -> R.drawable.ic_title_black_24dp
            Background -> R.drawable.ic_texture_black_24dp
        })

        val currentModeColor = when (currentMode) {
            Foreground -> settings.toolbarForegroundColor
            Background -> settings.toolbarBackgroundColor
        }
        color_picker.setColor(currentModeColor)

        setModeButtonColor(currentModeColor)
    }

}