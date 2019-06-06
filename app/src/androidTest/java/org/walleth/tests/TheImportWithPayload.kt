package org.walleth.tests

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import org.junit.Test
import org.walleth.R
import org.walleth.accounts.KeyType
import org.walleth.accounts.getCreateImportIntentFor
import org.walleth.infrastructure.TestApp

class TheImportWithPayload {

    @Test
    fun startOfECDSAWorks() {
        val context = ApplicationProvider.getApplicationContext<TestApp>()
        context.startActivity(context.getCreateImportIntentFor("keyProbe", KeyType.ECDSA).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })

        onView(withId(R.id.type_ecdsa_select)).check(matches(isChecked()))
        onView(withId(R.id.type_json_select)).check(matches(isNotChecked()))
        onView(withId(R.id.key_content)).check(matches(withText("keyProbe")))
    }

    @Test
    fun startOfJSONWorks() {
        val context = ApplicationProvider.getApplicationContext<TestApp>()
        context.startActivity(context.getCreateImportIntentFor("{}", KeyType.JSON).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })

        onView(withId(R.id.type_json_select)).check(matches(isChecked()))
        onView(withId(R.id.type_ecdsa_select)).check(matches(isNotChecked()))
        onView(withId(R.id.key_content)).check(matches(withText("{}")))
    }
}
