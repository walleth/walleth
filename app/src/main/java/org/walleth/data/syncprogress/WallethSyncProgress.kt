package org.walleth.data.syncprogress

data class WallethSyncProgress(val isSyncing: Boolean = false, val currentBlock: Long=0, val highestBlock: Long=0)