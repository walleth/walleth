package org.walleth.data.balances

import androidx.room.Entity
import org.kethereum.model.Address
import org.kethereum.model.ChainDefinition
import java.math.BigInteger

@Entity(tableName = "balances", primaryKeys = arrayOf("address", "chain", "tokenAddress"))
data class Balance(
        val address: Address,
        val tokenAddress: Address,
        val chain: ChainDefinition,

        val block: Long,
        val balance: BigInteger
)