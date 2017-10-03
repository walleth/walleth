package org.walleth.data.syncprogress

import android.arch.lifecycle.MutableLiveData

class SyncProgressProvider : MutableLiveData<WallethSyncProgress>() {

    init {
        value = WallethSyncProgress(false, 0L, 0L)
    }
}