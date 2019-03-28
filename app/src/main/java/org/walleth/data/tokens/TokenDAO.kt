package org.walleth.data.tokens

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import org.kethereum.model.Address

@Dao
interface TokenDAO {

    @Query("SELECT * FROM tokens")
    fun all(): List<Token>

    @Query("SELECT * FROM tokens WHERE chain = :chain ORDER BY \"order\" DESC,\"symbol\"")
    fun allForChain(chain: Long): List<Token>

    @Query("SELECT * FROM tokens WHERE chain = :chain  ORDER BY \"order\" DESC,\"symbol\"")
    fun allForChainLive(chain: Long): LiveData<List<Token>>

    @Query("UPDATE tokens SET showInList=1")
    fun showAll()

    @Query("SELECT * FROM tokens WHERE address = :address COLLATE NOCASE")
    fun forAddress(address: Address): Token?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(entry: Token)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(entries: List<Token>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun addIfNotPresent(entries: List<Token>)


}