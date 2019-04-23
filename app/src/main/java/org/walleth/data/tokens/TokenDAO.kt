package org.walleth.data.tokens

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.kethereum.model.Address

@Dao
interface TokenDAO {

    @Query("SELECT * FROM tokens")
    fun all(): List<Token>

    @Query("SELECT * FROM tokens ORDER BY \"order\" DESC ,\"chain\",\"symbol\"")
    fun allLive(): LiveData<List<Token>>

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