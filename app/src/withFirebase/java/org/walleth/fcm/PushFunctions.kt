package org.walleth.fcm

import com.google.firebase.iid.FirebaseInstanceId
import com.squareup.moshi.Moshi
import okhttp3.*
import org.ligi.tracedroid.logging.Log
import java.io.IOException


fun registerPush(okHttp: OkHttpClient , addresses: List<String>) {
    val firebaseToken = FirebaseInstanceId.getInstance().token
    if (firebaseToken != null) {
        val adapter = Moshi.Builder().build().adapter(PushMapping::class.java)

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