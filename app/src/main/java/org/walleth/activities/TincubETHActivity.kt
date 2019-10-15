package org.walleth.activities

import android.os.Bundle
import android.widget.SeekBar
import in3.IN3
import kotlinx.android.synthetic.main.activity_in3.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.kethereum.model.ChainId
import org.kethereum.rpc.BaseEthereumRPC
import org.kethereum.rpc.RPCTransport
import org.koin.android.ext.android.inject
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.base_activities.BaseSubActivity
import org.walleth.data.AppDatabase
import org.walleth.util.hasIN3Support


class IN3Transport : RPCTransport {
    private val in3 by lazy { IN3() }

    fun setChain(chainId: ChainId) {
        in3.chainId = chainId.value.toLong()
    }

    override fun call(payload: String): String = in3.send(payload).let { result ->
        when {
            result.startsWith("0x") -> """{"result":"$result"}"""
            else -> """{"result":$result}"""
        }
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

        scan_chains.setOnClickListener {
            GlobalScope.launch(Dispatchers.IO) {

                val in3 = IN3RPC()
                var res = ""
                appDatabase.chainInfo.getAll().forEach {
                    try {
                        in3.transport.setChain(ChainId(it.chainId))
                        if (!it.hasIN3Support()) {
                            appDatabase.chainInfo.upsert(it.copy(rpc = it.rpc + "faucet"))
                        }
                        res += " ${it.chainId}  --- " + in3.clientVersion()
                    } catch (e: Exception) {
                        // unfortunately it is that generic if a chain does not exist " java.lang.Exception: E"
                    }
                }

                GlobalScope.launch(Dispatchers.Main) {
                    alert(res)
                }
            }

        }
        security_seek.max = 29
        security_seek.setOnSeekBarChangeListener(
                object : SeekBar.OnSeekBarChangeListener {
                    override fun onStartTrackingTouch(p0: SeekBar?) {
                    }

                    override fun onStopTrackingTouch(p0: SeekBar?) {
                    }

                    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                        security_details_text.text = when (p1 / 10) {
                            0 -> "-> Not secure but cheap and fast"
                            1 -> "-> More secure but also more expensive and slower"
                            2 -> "-> Most secure but also most expensive and slow"
                            else -> TODO()
                        }
                    }

                })

        privacy_seek.max = 29
        privacy_seek.setOnSeekBarChangeListener(
                object : SeekBar.OnSeekBarChangeListener {
                    override fun onStartTrackingTouch(p0: SeekBar?) {
                    }

                    override fun onStopTrackingTouch(p0: SeekBar?) {
                    }

                    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                        privacy_details_text.text = when (p1 / 10) {
                            0 -> "-> Not private but cheap and fast"
                            1 -> "-> More private but also more expensive and slower"
                            2 -> "-> Most private but also most expensive and slow"
                            else -> TODO()
                        }
                    }

                })

    }

}
