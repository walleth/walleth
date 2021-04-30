package org.walleth.data.addresses

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.kethereum.model.Address

suspend fun AddressBookDAO.resolveNameWithFallback(address: Address, fallback: String = address.hex) = byAddress(address)?.name ?: fallback

@Dao
interface AddressBookDAO {

    @Query("SELECT * FROM addressbook ORDER BY name COLLATE NOCASE")
    suspend fun all(): List<AddressBookEntry>

    @Query("SELECT * FROM addressbook WHERE deleted = 1")
    fun allDeleted(): List<AddressBookEntry>

    @Query("UPDATE addressbook SET deleted=0")
    suspend fun unDeleteAll()

    @Query("SELECT * FROM addressbook WHERE is_notification_wanted")
    fun allThatWantNotifications(): List<AddressBookEntry>

    @Query("SELECT * FROM addressbook WHERE is_notification_wanted")
    fun allThatWantNotificationsLive(): LiveData<List<AddressBookEntry>>

    @Query("SELECT * FROM addressbook where address = :address COLLATE NOCASE")
    fun byAddressLiveData(address: Address): LiveData<AddressBookEntry?>

    @Query("SELECT * FROM addressbook where address = :address COLLATE NOCASE")
    fun byAddressFlow(address: Address): Flow<AddressBookEntry?>

    @Query("SELECT * FROM addressbook where address = :address COLLATE NOCASE")
    suspend fun byAddress(address: Address): AddressBookEntry?

    @Query("DELETE FROM addressbook")
    fun deleteAll()

    @Query("DELETE FROM addressbook where deleted = 1")
    suspend fun deleteAllSoftDeleted()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: AddressBookEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(entries: List<AddressBookEntry>)

}