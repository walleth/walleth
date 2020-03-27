package org.walleth.data.addresses

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Moshi
import kotlinx.android.parcel.Parcelize
import org.kethereum.model.Address
import org.walleth.enhancedlist.ListItem
import org.walleth.data.ACCOUNT_TYPE_TREZOR

@Parcelize
data class AccountKeySpec(
        val type: String,
        val derivationPath: String? = null,
        val source: String? = null,
        val pwd: String? = null,
        val initPayload: String? = null,
        val name: String? = null
) : Parcelable

private val specAdapter = Moshi.Builder().build().adapter(AccountKeySpec::class.java)

fun AccountKeySpec.toJSON() = specAdapter.toJson(this)

fun AddressBookEntry?.getSpec() = this?.keySpec?.let { specAdapter.fromJson(it) }

fun AddressBookEntry.getTrezorDerivationPath(): String? {
    if (keySpec?.startsWith("m/") == true) {
        keySpec = specAdapter.toJson(AccountKeySpec(type = ACCOUNT_TYPE_TREZOR, derivationPath = keySpec))
    }
    return getSpec()?.derivationPath
}

fun AddressBookEntry.getNFCDerivationPath() = getSpec()?.derivationPath
fun AddressBookEntry?.isAccountType(accountType: String) = getSpec()?.type == accountType

@Entity(tableName = "addressbook")
data class AddressBookEntry (

        @PrimaryKey
        var address: Address,

        override var name: String,

        var note: String? = null,

        @ColumnInfo(name = "is_notification_wanted")
        var isNotificationWanted: Boolean = false,

        @ColumnInfo(name = "trezor_derivation_path") // TODO with the next migration we should rename the column
        var keySpec: String? = null,

        var starred: Boolean = false,

        override var deleted: Boolean = false,

        var fromUser: Boolean = false,

        var order: Int = 0
) : ListItem