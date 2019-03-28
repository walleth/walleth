package org.walleth.data.balances

import android.arch.persistence.room.Entity
import org.kethereum.model.Address
import java.math.BigInteger

@Entity(tableName = "balances", primaryKeys = ["address", "chain", "tokenAddress"])
data class Balance(
        val address: Address,
        val tokenAddress: Address,
        val chain: Long,

        val block: Long,
        val balance: BigInteger
)