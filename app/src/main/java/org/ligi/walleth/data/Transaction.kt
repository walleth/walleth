package org.ligi.walleth.data

import org.threeten.bp.LocalDateTime


data class Transaction(val value: Long, val address: String, val localTime: LocalDateTime)
