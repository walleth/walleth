package org.walleth.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import kotlinx.android.synthetic.main.value.view.*
import org.ligi.kaxt.setVisibility
import org.walleth.R
import org.walleth.data.config.Settings
import org.walleth.data.exchangerate.ExchangeRateProvider
import org.walleth.data.tokens.Token
import org.walleth.data.tokens.isETH
import org.walleth.functions.toValueString
import java.math.BigInteger

open class ValueView(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {

    private val exchangeRateProvider: ExchangeRateProvider by LazyKodein(appKodein).instance()
    private val settings: Settings by LazyKodein(appKodein).instance()

    open val layoutRes = R.layout.value

    override fun onFinishInflate() {
        super.onFinishInflate()
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(layoutRes, this, true)
    }

    fun setValue(int: BigInteger, token: Token) {

        if (token.isETH()) {
            val exChangeRate = exchangeRateProvider.getExchangeString(int, settings.currentFiat)

            current_fiat_symbol.text = settings.currentFiat
            if (exChangeRate != null) {
                current_fiat.text = exChangeRate
            } else {
                current_fiat.text = "?"
            }
        }

        current_token_symbol.text = token.symbol

        current_fiat_symbol.setVisibility(token.isETH())
        current_fiat.setVisibility(token.isETH())

        current_eth.text = int.toValueString(token)
    }

}
