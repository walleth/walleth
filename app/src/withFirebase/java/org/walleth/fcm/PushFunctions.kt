package org.walleth.fcm

import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.instance
import com.google.firebase.iid.FirebaseInstanceId
import com.squareup.moshi.Moshi
import okhttp3.*
import org.ligi.tracedroid.logging.Log
import org.walleth.data.addressbook.AddressBook
import java.io.IOException


fun registerPush(kodein: Kodein) {
    val firebaseToken = FirebaseInstanceId.getInstance().token
    if (firebaseToken != null) {
        val okHttp: OkHttpClient = kodein.instance()
        val addressBook: AddressBook = kodein.instance()
        val adapter = Moshi.Builder().build().adapter(PushMapping::class.java)

        val addresses = addressBook.getAllEntries().filter { it.isNotificationWanted }.map { it.address.hex }
        val pushMapping = PushMapping(PushState.uuid, firebaseToken, addresses)

        okHttp.newCall(Request.Builder().url("https://li5.ddns.net")

                .post(RequestBody.create(MediaType.parse("application/json"), adapter.toJson(pushMapping)))
                .build())
                .enqueue(object : Callback {
                    override fun onResponse(call: Call?, response: Response?) {
                        Log.i("Registered push")
                    }

                    override fun onFailure(call: Call?, exception: IOException?) {
                        Log.i("Could not register for push: " + exception?.message)
                    }
                })
    }
}