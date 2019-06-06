package org.walleth.startup

sealed class StartupStatus {
    object NeedsAddress : StartupStatus()
    object HasChainAndAddress : StartupStatus()
    object Timeout : StartupStatus()
}