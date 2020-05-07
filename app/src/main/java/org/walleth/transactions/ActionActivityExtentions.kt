package org.walleth.transactions

import android.app.Activity
import android.content.Context
import android.content.Intent
import org.kethereum.erc681.ERC681
import org.kethereum.erc681.generateURL
import org.kethereum.erc681.parseERC681
import org.walleth.base_activities.BaseSubActivity
import org.walleth.data.EXTRA_KEY_ERC681
import kotlin.reflect.KClass

val BaseSubActivity.erc681StringExtra: String
    get() = intent.getStringExtra(EXTRA_KEY_ERC681) ?: throw IllegalArgumentException("Activity called without ERC681 in intent")
val BaseSubActivity.erc681 get() = parseERC681(erc681StringExtra)

fun Context.getERC681ActivityIntent(erC681: ERC681, clazz: KClass<out Activity>) = Intent(this, clazz.java).apply {
    putExtra(EXTRA_KEY_ERC681, erC681.generateURL())
}