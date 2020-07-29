package org.walleth.security

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.tincubeth_config.*
import org.koin.android.ext.android.inject
import org.walleth.R
import org.walleth.data.AppDatabase

class TincubETHFragment : Fragment() {

    val appDatabase: AppDatabase by inject()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.tincubeth_config, container, false)


    override fun onResume() {
        super.onResume()
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