package org.walleth.data.addressbook

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import org.kethereum.model.Address

fun AddressBookDAO.resolveName(address: Address) = byAddress(address)?.name ?: address.hex
fun AddressBookDAO.resolveNameAsync(address: Address, callback: (name: String) -> Unit) = async(UI) {
    callback(async(CommonPool) { resolveName(address) }.await())
}

fun AddressBookDAO.getByAddressAsync(address: Address, callback: (name: AddressBookEntry?) -> Unit) = async(UI) {
    callback(async(CommonPool) { byAddress(address) }.await())
}

@Dao
interface AddressBookDAO {

    @Query("SELECT * FROM addressbook ORDER BY name COLLATE NOCASE")
    fun allLiveData(): LiveData<List<AddressBookEntry>>

    @Query("SELECT * FROM addressbook ORDER BY name COLLATE NOCASE")
    fun all(): List<AddressBookEntry>

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(entry: AddressBookEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(entries: List<AddressBookEntry>)

}