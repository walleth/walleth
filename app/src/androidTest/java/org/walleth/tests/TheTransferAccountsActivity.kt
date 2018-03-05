package org.walleth.tests

import android.app.Activity.RESULT_OK
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import android.support.test.InstrumentationRegistry
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.intent.Intents
import android.support.test.espresso.intent.matcher.IntentMatchers.hasAction
import android.support.test.espresso.matcher.ViewMatchers.withId
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.kethereum.model.Address
import org.ligi.trulesk.TruleskIntentRule
import org.walleth.R
import org.walleth.activities.TransferAccountsActivity
import org.walleth.data.addressbook.AddressBookEntry
import org.walleth.infrastructure.TestApp
import java.io.File

class TheTransferAccountsActivity {

    @get:Rule
    var rule = TruleskIntentRule(TransferAccountsActivity::class.java)

    @Test
    fun importsTheExportCorrectly() {
        val accountName = "test account"
        val address = Address("0xdeadbeef")
        TestApp.testDatabase.addressBook.upsert(AddressBookEntry(address, accountName))
        val addressesJson = "addresses.txt"
        val addressesFile = File(InstrumentationRegistry.getTargetContext().filesDir, addressesJson)
        val uri = Uri.parse("file://" + addressesFile.absoluteFile)
        val addressCount = TestApp.testDatabase.addressBook.all().size

        Intents.intending(hasAction(Intent.ACTION_CREATE_DOCUMENT))
                .respondWith(Instrumentation.ActivityResult(RESULT_OK, Intent().setData(uri)))
        Intents.intending(hasAction(Intent.ACTION_OPEN_DOCUMENT))
                .respondWith(Instrumentation.ActivityResult(RESULT_OK, Intent().setData(uri)))

        // export
        onView(withId(R.id.export_button)).perform(click())

        assertThat(addressesFile.exists()).isTrue()

        // import
        TestApp.testDatabase.addressBook.deleteAll()
        InstrumentationRegistry.getTargetContext().startActivity(Intent(InstrumentationRegistry.getTargetContext(), TransferAccountsActivity::class.java))

        onView(withId(R.id.import_button)).perform(click())

        assertThat(TestApp.testDatabase.addressBook.all().size).isEqualTo(addressCount)
        assertThat(TestApp.testDatabase.addressBook.byAddress(address)!!.name).isEqualTo(accountName)
    }

}