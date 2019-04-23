package org.walleth.data.addressbook

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.kethereum.model.Address

fun AddressBookDAO.resolveName(address: Address) = byAddress(address)?.name ?: address.hex
fun AddressBookDAO.resolveNameAsync(address: Address, callback: (name: String) -> Unit) = GlobalScope.launch(Dispatchers.Main) {
    callback(async(Dispatchers.Default) { resolveName(address) }.await())
}

fun AddressBookDAO.getByAddressAsync(address: Address, callback: (name: AddressBookEntry?) -> Unit) = GlobalScope.launch(Dispatchers.Main) {
    callback(async(Dispatchers.Default) { byAddress(address) }.await())
}

@Dao
interface AddressBookDAO {

    @Query("SELECT * FROM addressbook ORDER BY name COLLATE NOCASE")
    fun allLiveData(): LiveData<List<AddressBookEntry>>

    @Query("SELECT * FROM addressbook ORDER BY name COLLATE NOCASE")
    fun all(): List<AddressBookEntry>

    @Query("SELECT * FROM addressbook WHERE deleted = 1")
    fun allDeleted(): List<AddressBookEntry>

    @Query("UPDATE addressbook SET deleted=0")
    fun undeleteAll()

    @Query("SELECT * FROM addressbook WHERE is_notification_wanted")
    fun allThatWantNotifications(): List<AddressBookEntry>

    @Query("SELECT * FROM addressbook WHERE is_notification_wanted")
    fun allThatWantNotificationsLive(): LiveData<List<AddressBookEntry>>

    @Query("SELECT * FROM addressbook where address = :address COLLATE NOCASE")
    fun byAddressLiveData(address: Address): LiveData<AddressBookEntry?>

    @Query("SELECT * FROM addressbook where address = :address COLLATE NOCASE")
    fun byAddress(address: Address): AddressBookEntry?

    @Query("DELETE FROM addressbook")
    fun deleteAll()

    @Query("DELETE FROM addressbook where deleted = 1")
    fun deleteAllSoftDeleted()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(entry: AddressBookEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(entries: List<AddressBookEntry>)

}