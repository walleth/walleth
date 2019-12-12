package org.walleth.data.chaininfo

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import java.math.BigInteger

@Dao
interface ChainInfoDAO {

    @Query("SELECT * FROM chains ORDER by `order` DESC")
    fun getAll(): List<ChainInfo>

    @Query("SELECT * FROM chains WHERE chainId = :chain")
    suspend fun getByChainId(chain: BigInteger): ChainInfo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: ChainInfo)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: List<ChainInfo>)
}