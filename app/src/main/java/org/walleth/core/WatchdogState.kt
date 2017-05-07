package org.walleth.core

object WatchdogState {
    var watchdog_round: Long = 0

    var geth_last_seen: Long = System.currentTimeMillis()
    var geth_service_running = false
}