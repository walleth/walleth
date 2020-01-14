package org.walleth.activities

import android.os.Bundle
import android.widget.SeekBar
import in3.IN3
import kotlinx.android.synthetic.main.activity_in3.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.kethereum.model.ChainId
import org.kethereum.rpc.BaseEthereumRPC
import org.kethereum.rpc.RPCTransport
import org.koin.android.ext.android.inject
import org.ligi.tracedroid.logging.Log
import org.walleth.R
import org.walleth.base_activities.BaseSubActivity
import org.walleth.data.AppDatabase
import org.walleth.data.chaininfo.ChainInfo
import org.walleth.data.rpc.KEY_IN3_RPC
import org.walleth.util.hasTincubethSupport


class IN3Transport : RPCTransport {
    private val in3 by lazy { IN3() }

    fun setChain(chainId: ChainId) {
        in3.chainId = chainId.value.toLong()
    }

    override fun call(payload: String): String? = try {
        in3.send(payload).let { result ->
            when {
                result.startsWith("0x") -> """{"result":"$result"}"""
                else -> """{"result":$result}"""
            }
        }
    } catch (e: Exception) {
        null
    }
}

class IN3RPC(chainId: ChainId? = null, val transport: IN3Transport = IN3Transport()) : BaseEthereumRPC(transport) {
    init {
        chainId?.let { transport.setChain(it) }
    }
}


class TincubETHActivity : BaseSubActivity() {

    val appDatabase: AppDatabase by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_in3)

        supportActionBar?.subtitle = "TinCubETH preferences"

        security_seek.max = 29
        val listener = object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(p0: SeekBar?) {}

            override fun onStopTrackingTouch(p0: SeekBar?) {}

            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                refresh()
            }
        }
        security_seek.setOnSeekBarChangeListener(listener)

        privacy_seek.max = 29
        privacy_seek.setOnSeekBarChangeListener(listener)
        refresh()
    }

    fun refresh() {
        security_details_text.text = when (security_seek.progress / 10) {
            0 -> "-> Weak security but cheaper and faster"
            1 -> "-> Better security but also more expensive and slower"
            2 -> "-> Maximum security but also most expensive and slow"
            else -> TODO()
        }
        privacy_details_text.text = when (privacy_seek.progress / 10) {
            0 -> "-> Weak privacy but faster and cheaper"
            1 -> "-> Better privacy but also more expensive and slower"
            2 -> "-> Maximum privacy but also most expensive and slow"
            else -> TODO()
        }
    }
}

suspend fun findChainsWithTincubethSupportAndStore(appDatabase: AppDatabase, in3: IN3RPC = IN3RPC()): List<ChainInfo> {
    val res = findTincubethChains(appDatabase, in3)
    res.forEach {
        if (!it.hasTincubethSupport()) {
            appDatabase.chainInfo.upsert(it.copy(rpc = it.rpc + KEY_IN3_RPC))
        }
    }
    return res
}

private suspend fun findTincubethChains(appDatabase: AppDatabase, in3: IN3RPC) = withContext(Dispatchers.IO) {
    val res = appDatabase.chainInfo.getAll().filter {
        try {

            in3.transport.setChain(ChainId(it.chainId))
            in3.clientVersion() != null
        } catch (e: Exception) {
            // unfortunately it is that generic if a chain does not exist " java.lang.Exception: E"
            e.printStackTrace()
            false
        }

    }
    Log.i("TincubethLog " + res.size + " / " + appDatabase.chainInfo.getAll().size)
    res
}
