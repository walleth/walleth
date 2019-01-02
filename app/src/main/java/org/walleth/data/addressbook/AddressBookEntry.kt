package org.walleth.data.addressbook

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.kethereum.model.Address

@Entity(tableName = "addressbook")
data class AddressBookEntry(

        @PrimaryKey
        var address: Address,

        var name: String,

        var note: String? = null,

        @ColumnInfo(name = "is_notification_wanted")
        var isNotificationWanted: Boolean = false,

        @ColumnInfo(name = "trezor_derivation_path")
        var trezorDerivationPath: String? = null,

        var starred: Boolean = false,

        var deleted: Boolean = false,

        var fromUser: Boolean = false,

        var order: Int = 0
)