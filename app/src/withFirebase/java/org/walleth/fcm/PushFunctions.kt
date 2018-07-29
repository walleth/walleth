package org.walleth.fcm

import com.google.firebase.iid.FirebaseInstanceId
import com.squareup.moshi.Moshi
import okhttp3.*
import org.ligi.tracedroid.logging.Log
import org.walleth.data.JSON_MEDIA_TYPE
import org.walleth.walletconnect.WalletConnectDriver
import java.io.IOException


fun registerPush(walletConnectInteractor: WalletConnectDriver,
                 okHttp: OkHttpClient,
                 addresses: List<String>) {
    val firebaseToken = FirebaseInstanceId.getInstance().token
    firebaseToken?.let {
        walletConnectInteractor.fcmToken = it
    }

    if (firebaseToken != null) {
        val adapter = Moshi.Builder().build().adapter(PushMapping::class.java)

        val pushMapping = PushMapping(PushState.uuid, firebaseToken, addresses)

        okHttp.newCall(Request.Builder().url("https://li5.ddns.net")

                .post(RequestBody.create(JSON_MEDIA_TYPE, adapter.toJson(pushMapping)))
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