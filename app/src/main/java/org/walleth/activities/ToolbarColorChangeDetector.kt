package org.walleth.activities

import org.koin.core.KoinComponent
import org.koin.core.inject
import org.walleth.data.config.Settings

interface ToolbarColorChangeDetector {
    val settings: Settings
    var lastToolbarColor: Long
    fun calcToolbarColorCombination() = settings.toolbarBackgroundColor.toLong() + settings.toolbarForegroundColor

    fun didToolbarColorChange() = (lastToolbarColor != calcToolbarColorCombination()).also {
        lastToolbarColor = calcToolbarColorCombination()
    }

}

class DefaultToolbarChangeDetector : ToolbarColorChangeDetector, KoinComponent {
    override val settings: Settings by inject()
    override var lastToolbarColor: Long = calcToolbarColorCombination()
}