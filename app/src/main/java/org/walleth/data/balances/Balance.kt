package org.walleth.data.balances

import androidx.room.Entity
import org.kethereum.model.Address
import java.math.BigInteger

@Entity(tableName = "balances", primaryKeys = ["address", "chain", "tokenAddress"])
data class Balance(
        val address: Address,
        val tokenAddress: Address,
        val chain: BigInteger,

        val block: Long,
        val balance: BigInteger
)