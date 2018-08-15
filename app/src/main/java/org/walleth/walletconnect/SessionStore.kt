package org.walleth.walletconnect

import com.squareup.moshi.Moshi
import org.ligi.tracedroid.logging.Log
import org.walleth.walletconnect.model.Session
import org.walleth.walletconnect.model.SessionList
import java.io.File

class SessionStore(private val file: File) {

    private val sessionListAdapter = Moshi.Builder().build().adapter(SessionList::class.java)

    private val sessionMap by lazy {
        mutableMapOf<String, Session>().apply {
            if (file.exists()) {
                sessionListAdapter.fromJson(file.readText())?.sessions?.forEach { session ->
                    put(session.sessionId, session)
                }
            }
        }
    }

    fun put(session: Session) {
        sessionMap[session.sessionId] = session

        val sessionJSON = sessionListAdapter.toJson(SessionList(sessionMap.map { it.value }))

        file.writeText(sessionJSON)
    }

    fun get(key: String): Session? {
        val session = sessionMap[key]

        if (session == null) {
            Log.w("Cannot find session for id $key in SessionStore")
        }

        return session
    }
}