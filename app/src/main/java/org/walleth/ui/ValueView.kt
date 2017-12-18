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
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.data.config.Settings
import org.walleth.data.exchangerate.ExchangeRateProvider
import org.walleth.data.tokens.Token
import org.walleth.data.tokens.isETH
import org.walleth.functions.addPrefixOnCondition
import org.walleth.functions.toFullValueString
import org.walleth.functions.toValueString
import org.walleth.functions.twoDigitDecimalFormat
import java.math.BigDecimal
import java.math.BigInteger
import java.math.BigInteger.ZERO

open class ValueView(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {

    private val exchangeRateProvider: ExchangeRateProvider by LazyKodein(appKodein).instance()
    private val settings: Settings by LazyKodein(appKodein).instance()

    open val layoutRes = R.layout.value

    private var currentValue = ZERO
    private var currentExchangeValue: BigDecimal? = null
    private var currentToken: Token? = null

    override fun onFinishInflate() {
        super.onFinishInflate()
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(layoutRes, this, true)
        current_eth.setOnClickListener {
            currentToken?.let { tokenNotNull ->
                showPreciseAmountAlert(currentValue.toFullValueString(tokenNotNull) + current_token_symbol.text )
            }
        }

        current_fiat.setOnClickListener {
            currentExchangeValue?.let { currentExchangeValueNotNull ->
                showPreciseAmountAlert(String.format("%f", currentExchangeValueNotNull) + current_fiat_symbol.text)
            }
        }
    }

    private fun showPreciseAmountAlert(fullAmountString: String) =
            context.alert(fullAmountString,context.getString(R.string.precise_amount_alert_title))

    fun setValue(value: BigInteger, token: Token) {

        if (token.isETH()) {
            val exChangeRate = exchangeRateProvider.getConvertedValue(value, settings.currentFiat)

            current_fiat_symbol.text = settings.currentFiat
            current_fiat.text = if (exChangeRate != null) {
                twoDigitDecimalFormat.format(exChangeRate).addPrefixOnCondition(prefix = "~", condition = exChangeRate.scale() <= 2)
            } else {
                "?"
            }

            currentExchangeValue = exChangeRate
        }

        currentValue = value
        currentToken = token

        current_token_symbol.text = token.symbol

        current_fiat_symbol.setVisibility(token.isETH())
        current_fiat.setVisibility(token.isETH())

        current_eth.text = value.toValueString(token)
    }

}
