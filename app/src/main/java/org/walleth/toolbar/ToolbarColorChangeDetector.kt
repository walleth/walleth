package org.walleth.toolbar

import org.koin.core.KoinComponent
import org.koin.core.inject
import org.walleth.data.config.Settings

interface ToolbarColorChangeDetector {
    val wallethSettings: Settings
    var lastToolbarColor: Long
    fun calcToolbarColorCombination() = wallethSettings.toolbarBackgroundColor.toLong() + wallethSettings.toolbarForegroundColor

    fun didToolbarColorChange() = (lastToolbarColor != calcToolbarColorCombination()).also {
        lastToolbarColor = calcToolbarColorCombination()
    }

}

class DefaultToolbarChangeDetector : ToolbarColorChangeDetector, KoinComponent {
    override val wallethSettings: Settings by inject()
    override var lastToolbarColor: Long = calcToolbarColorCombination()
}