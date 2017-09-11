package org.walleth.tests

import android.content.Intent
import android.support.test.InstrumentationRegistry
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.*
import org.junit.Test
import org.walleth.R
import org.walleth.activities.KeyType
import org.walleth.activities.getKeyImportIntent

class TheImportWithPayload {

    @Test
    fun startOfECDSAWorks() {
        val context = InstrumentationRegistry.getTargetContext()
        context.startActivity(context.getKeyImportIntent("keyProbe", KeyType.ECDSA).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })

        onView(withId(R.id.type_ecdsa_select)).check(matches(isChecked()))
        onView(withId(R.id.type_json_select)).check(matches(isNotChecked()))
        onView(withId(R.id.key_content)).check(matches(withText("keyProbe")))
    }

    @Test
    fun startOfJSONWorks() {
        val context = InstrumentationRegistry.getTargetContext()
        context.startActivity(context.getKeyImportIntent("{}", KeyType.JSON).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })

        onView(withId(R.id.type_json_select)).check(matches(isChecked()))
        onView(withId(R.id.type_ecdsa_select)).check(matches(isNotChecked()))
        onView(withId(R.id.key_content)).check(matches(withText("{}")))
    }
}
