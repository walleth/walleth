package org.walleth.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import kotlinx.android.synthetic.main.value.view.*
import org.walleth.R
import org.walleth.data.config.Settings
import org.walleth.data.exchangerate.ExchangeRateProvider
import org.walleth.functions.toEtherValueString
import java.math.BigInteger

open class ValueView(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {

    val exchangeRateProvider: ExchangeRateProvider by LazyKodein(appKodein).instance()
    val settings: Settings by LazyKodein(appKodein).instance()

    override fun onFinishInflate() {
        super.onFinishInflate()
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.value, this, true)
    }

    fun setEtherValue(int: BigInteger) {

        val exChangeRate = exchangeRateProvider.getExchangeString(int, settings.currentFiat)
        current_fiat_symbol.text = settings.currentFiat
        if (exChangeRate != null) {
            current_fiat.text = exChangeRate
        } else {
            current_fiat.text = "?"
        }

        current_eth.text = int.toEtherValueString()

    }
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
    }

}
