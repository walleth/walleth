package org.walleth.data.syncprogress

import org.walleth.data.SimpleObserveable

class SyncProgressProvider : SimpleObserveable() {

    var currentSyncProgress = WallethSyncProgress(false, 0L, 0L)

    fun setSyncProgress(syncProgress: WallethSyncProgress) {
        if (currentSyncProgress != syncProgress) {
            currentSyncProgress = syncProgress
            promoteChange()
        }
    }
}