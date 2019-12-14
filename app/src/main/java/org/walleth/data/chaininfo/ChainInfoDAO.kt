package org.walleth.data.chaininfo

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import java.math.BigInteger

@Dao
interface ChainInfoDAO {

    @Query("SELECT * FROM chains ORDER by `order` DESC")
    suspend fun getAll(): List<ChainInfo>

    @Query("UPDATE chains SET softDeleted=0")
    suspend fun undeleteAll()

    @Query("DELETE FROM chains where softDeleted = 1")
    suspend fun deleteAllSoftDeleted()

    @Query("SELECT * FROM chains WHERE chainId = :chain")
    suspend fun getByChainId(chain: BigInteger): ChainInfo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: ChainInfo)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: List<ChainInfo>)
}