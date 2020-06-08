package org.walleth.activities

import android.content.Context
import android.os.Bundle
import android.widget.SeekBar
import kotlinx.android.synthetic.main.activity_in3.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.walleth.R
import org.walleth.base_activities.BaseSubActivity
import org.walleth.data.AppDatabase
import org.walleth.data.chaininfo.ChainInfo
import org.walleth.data.rpc.KEY_IN3_RPC
import org.walleth.util.hasTincubethSupport
import java.math.BigInteger

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