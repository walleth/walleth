package org.walleth.data.tokens

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.kethereum.model.Address

@Dao
interface TokenDAO {

    @Query("SELECT * FROM tokens ORDER BY \"order\" DESC ,\"chain\",\"symbol\"")
    suspend fun all(): List<Token>

    @Query("UPDATE tokens SET softDeleted=0")
    suspend fun unDeleteAll()

    @Query("DELETE FROM addressbook where deleted = 1")
    suspend fun deleteAllSoftDeleted()

    @Query("SELECT * FROM tokens WHERE address = :address COLLATE NOCASE")
    suspend fun forAddress(address: Address): Token?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: Token)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entries: List<Token>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addIfNotPresent(entries: List<Token>)


}