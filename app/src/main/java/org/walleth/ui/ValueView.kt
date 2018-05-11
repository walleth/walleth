package org.walleth.ui

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.value.view.*
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance
import org.ligi.kaxt.setVisibility
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.data.config.Settings
import org.walleth.data.exchangerate.ExchangeRateProvider
import org.walleth.data.tokens.Token
import org.walleth.data.tokens.isETH
import org.walleth.functions.*
import java.math.BigDecimal
import java.math.BigInteger
import java.math.BigInteger.ZERO

open class ValueView(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs), KodeinAware {

    override val kodein by closestKodein()
    private val exchangeRateProvider: ExchangeRateProvider by instance()
    private val settings: Settings by instance()

    open val layoutRes = R.layout.value
    private val showsPrecise: Boolean

    private var currentValue = ZERO
    private var currentExchangeValue: BigDecimal? = null
    private var currentToken: Token? = null

    init {
        // extract the showPrecise value
        val a: TypedArray = context.theme.obtainStyledAttributes(attrs,R.styleable.ValueView,
                0, 0)
        try {
            showsPrecise = a.getBoolean(R.styleable.ValueView_showPrecise, true)
        } finally {
            a.recycle()
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(layoutRes, this, true)

        // only intercept touch through click listener if view can show precise
        if (showsPrecise) {
            current_eth.setOnClickListener {
                currentToken?.let { tokenNotNull ->
                    if (current_eth.text.isValueImprecise()) {
                        showPreciseAmountAlert(currentValue.toFullValueString(tokenNotNull) + current_token_symbol.text)
                    }
                }
            }

            current_fiat.setOnClickListener {
                currentExchangeValue?.let { currentExchangeValueNotNull ->
                    if (current_fiat.text.isValueImprecise()) {
                        showPreciseAmountAlert(String.format("%f", currentExchangeValueNotNull) + current_fiat_symbol.text)
                    }
                }
            }
        }
    }

    private fun showPreciseAmountAlert(fullAmountString: String) =
            context.alert(fullAmountString, context.getString(R.string.precise_amount_alert_title))

    fun setValue(value: BigInteger, token: Token) {

        if (token.isETH()) {
            val exChangeRate = exchangeRateProvider.getConvertedValue(value, settings.currentFiat)

            current_fiat_symbol.text = settings.currentFiat
            current_fiat.text = if (exChangeRate != null) {
                twoDigitDecimalFormat.format(exChangeRate).addPrefixOnCondition(prefix = "~", condition = exChangeRate.scale() > 2)
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
