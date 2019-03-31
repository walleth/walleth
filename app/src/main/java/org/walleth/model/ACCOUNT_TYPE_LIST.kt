package org.walleth.model

import android.app.Activity
import android.content.Intent
import org.walleth.R
import org.walleth.activities.AccountPickActivity
import org.walleth.activities.ImportKeyActivity
import org.walleth.activities.RequestPINActivity
import org.walleth.activities.RequestPasswordActivity
import org.walleth.activities.nfc.NFCGetAddressActivity
import org.walleth.activities.trezor.TrezorGetAddressActivity
import org.walleth.data.*

val ACCOUNT_TYPE_LIST = listOf(

        AccountType(ACCOUNT_TYPE_BURNER,
                "Burner",
                "Create Burner",
                "Easy to get you started but weak security.",
                R.drawable.ic_whatshot_black_24dp,
                R.drawable.ic_key,
                wrapsKey = true) { activity, inSpec ->
            activity.setResult(Activity.RESULT_OK,
                    Intent().putExtra(EXTRA_KEY_ACCOUNTSPEC, inSpec.copy(type = ACCOUNT_TYPE_BURNER))
            )
            activity.finish()
        },
        AccountType(
                ACCOUNT_TYPE_TREZOR,
                "TREZOR wallet",
                "Connect TREZOR",
                "Very reasonable security but you need to have a device that costs more than 50DAI and plug it in the phone",
                R.drawable.trezor_icon_black,
                R.drawable.trezor_icon_black
        ) { activity, _ ->
            activity.startActivityForResult(Intent(activity, TrezorGetAddressActivity::class.java), REQUEST_CODE_IMPORT)
        },
        AccountType(
                ACCOUNT_TYPE_IMPORT,
                "Imported Key",
                "Import Key",
                "Import a key - e.g. your backup",
                R.drawable.ic_import,
                R.drawable.ic_import) { activity, _ ->
            activity.startActivityForResult(Intent(activity, ImportKeyActivity::class.java), REQUEST_CODE_IMPORT)
        },
        AccountType(ACCOUNT_TYPE_WATCH_ONLY,
                "Watch only account",
                "Watch Only",
                "No transactions possible then - just monitor or interact with this account",
                R.drawable.ic_watch,
                R.drawable.ic_watch) { activity, _ ->
            activity.startActivityForResult(Intent(activity, AccountPickActivity::class.java), REQUEST_CODE_PICK_WATCH_ONLY)
        },
        AccountType(ACCOUNT_TYPE_NFC,
                "NFC account",
                "Connect via NFC",
                "Contact-less connection to e.g. the keycard (a java card)",
                R.drawable.ic_nfc_black,
                R.drawable.ic_nfc_black) { activity, _ ->
            activity.startActivityForResult(Intent(activity, NFCGetAddressActivity::class.java), REQUEST_CODE_PICK_NFC)
        },
        AccountType(ACCOUNT_TYPE_PIN_PROTECTED,
                "PIN protected",
                "Create Key with PIN",
                "More secure than a burner but less secure than a hardware wallet",
                R.drawable.ic_fiber_pin_black_24dp,
                R.drawable.ic_fiber_pin_black_24dp,
                wrapsKey = true) { activity, _ ->
            activity.startActivityForResult(Intent(activity, RequestPINActivity::class.java), REQUEST_CODE_ENTER_PIN)
        },
        AccountType(ACCOUNT_TYPE_PASSWORD_PROTECTED,
                "password protected",
                "Create Key with password",
                "Similar to PIN but the keyboard might weaken the security",
                R.drawable.ic_keyboard_black_24dp,
                R.drawable.ic_keyboard_black_24dp,
                wrapsKey = true) { activity, _ ->
            activity.startActivityForResult(Intent(activity, RequestPasswordActivity::class.java), REQUEST_CODE_ENTER_PASSWORD)
        }
)

val ACCOUNT_TYPE_MAP by lazy {
    mutableMapOf<String, AccountType>().apply {
        ACCOUNT_TYPE_LIST.forEach {
            it.accountType?.let { accountType -> put(accountType, it) }
        }
    }
}
