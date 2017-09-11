package org.walleth.data.tokens

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import org.kethereum.model.Address
import org.walleth.data.networks.ChainDefinition

@Dao
interface TokenDAO {

    @Query("SELECT * FROM tokens")
    fun all(): List<Token>

    @Query("SELECT * FROM tokens WHERE chain = :chain")
    fun allForChain(chain: ChainDefinition): List<Token>

    @Query("SELECT * FROM tokens WHERE address = :address")
    fun forAddress(address: Address): Token?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(entry: Token)
}