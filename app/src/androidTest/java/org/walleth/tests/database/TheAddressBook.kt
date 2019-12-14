package org.walleth.tests.database

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.walleth.data.addresses.AddressBookEntry
import org.walleth.testdata.DEFAULT_TEST_ADDRESS
import org.walleth.testdata.DEFAULT_TEST_ADDRESS2
import org.walleth.testdata.DEFAULT_TEST_ADDRESS3

class TheAddressBook : AbstractDatabaseTest() {

    @Test
    fun isEmptyInitially() = runBlocking {
        assertThat(database.addressBook.all().size).isEqualTo(0)
    }

    @Test
    fun weCanInsertTwo() = runBlocking {
        database.addressBook.upsert(AddressBookEntry(DEFAULT_TEST_ADDRESS, "nameprobe"))
        database.addressBook.upsert(AddressBookEntry(DEFAULT_TEST_ADDRESS2, "2nameprobe2"))
        assertThat(database.addressBook.all().size).isEqualTo(2)
    }


    @Test
    fun queryWorks() = runBlocking {
        database.addressBook.upsert(AddressBookEntry(DEFAULT_TEST_ADDRESS, "nameprobe"))
        database.addressBook.upsert(AddressBookEntry(DEFAULT_TEST_ADDRESS2, "2nameprobe2"))
        assertThat(database.addressBook.byAddress(DEFAULT_TEST_ADDRESS2)?.name).isEqualTo("2nameprobe2")
    }

    @Test
    fun updateWorks() = runBlocking {
        database.addressBook.upsert(AddressBookEntry(DEFAULT_TEST_ADDRESS, "nameprobe"))
        database.addressBook.upsert(AddressBookEntry(DEFAULT_TEST_ADDRESS, "nameprobe_after_upsert"))
        assertThat(database.addressBook.byAddress(DEFAULT_TEST_ADDRESS)?.name).isEqualTo("nameprobe_after_upsert")
    }


    @Test
    fun findsWhereNotificationIsWanted() = runBlocking {
        database.addressBook.upsert(AddressBookEntry(DEFAULT_TEST_ADDRESS, "notificationNotWanted1", isNotificationWanted = false))
        val entry = AddressBookEntry(DEFAULT_TEST_ADDRESS2, "notificationWanted", isNotificationWanted = true)
        database.addressBook.upsert(entry)
        database.addressBook.upsert(AddressBookEntry(DEFAULT_TEST_ADDRESS3, "notificationNotWanted2", isNotificationWanted = false))

        assertThat(database.addressBook.allThatWantNotifications()).containsExactly(entry)
        return@runBlocking
    }


    @Test
    fun queryInEmptyReturnsNull() = runBlocking {
        assertThat(database.addressBook.byAddress(DEFAULT_TEST_ADDRESS)).isNull()
    }


}