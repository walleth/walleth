package org.walleth.ui.valueview

import kotlinx.android.synthetic.main.value.view.*
import org.ligi.kaxt.doAfterEdit
import org.ligi.kaxt.setVisibility
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.data.config.Settings
import org.walleth.data.exchangerate.ExchangeRateProvider
import org.walleth.data.networks.all.NetworkDefinition1
import org.walleth.data.tokens.Token
import org.walleth.data.tokens.isRootToken
import org.walleth.functions.*
import java.math.BigDecimal
import java.math.BigInteger
import java.math.BigInteger.ZERO

open class ValueViewController(private val valueView: ValueView,
                               private val exchangeRateProvider: ExchangeRateProvider,
                               private val settings: Settings) {

    private var currentAmountString by ValueViewTextObserver(valueView.current_eth, this)
    private var currentAmount: BigInteger? = null

    private var currentFiatString by ValueViewTextObserver(valueView.current_fiat, this)
    private var currentFiatUncutString: String? = null

    var currentToken: Token? = null

    init {

        if (valueView.showsPrecise) {
            valueView.current_value_rounding_indicator.setOnClickListener {
                currentAmount?.let { currentAmount ->
                    currentToken?.let { currentToken ->
                        val message = currentAmount.toFullValueString(currentToken)
                        valueView.context.alert(message, valueView.context.getString(R.string.precise_amount_alert_title))
                    }
                }
            }

            valueView.current_fiat_rounding_indicator.setOnClickListener {
                valueView.context.alert(currentFiatUncutString ?: "?", valueView.context.getString(R.string.precise_amount_alert_title))
            }
        }

        valueView.current_eth.doAfterEdit { editable ->
            if (valueView.current_eth.isFocused) {
                currentAmountString = editable.toString()
                currentAmount = getValueFromString()
                adaptFiat()
            }
        }

        valueView.current_fiat.doAfterEdit { editable ->
            if (valueView.current_fiat.isFocused) {
                currentFiatString = editable.toString()
                currentFiatUncutString = editable.toString()
                adaptValueFromFiat()
            }
        }
    }


    open fun refreshNonValues() {

        valueView.current_value_rounding_indicator.setVisibility(currentAmount != getValueFromString())

        val shouldDisplayFiat =  currentToken?.chain == NetworkDefinition1().chain && currentToken?.isRootToken() == true

        valueView.current_fiat_rounding_indicator.setVisibility(shouldDisplayFiat && isFiatRounded())

        valueView.current_fiat_symbol.setVisibility(shouldDisplayFiat)
        valueView.current_fiat.setVisibility(shouldDisplayFiat)

        valueView.current_fiat_symbol.text = settings.currentFiat
        currentToken?.also { currentToken ->
            valueView.current_token_symbol.text = currentToken.symbol
        }

    }

    private fun isFiatRounded() = currentFiatString != "?" && currentFiatString?.toBigDecimalOrNull()?.let {
        it.compareTo(currentFiatUncutString?.toBigDecimalOrNull() ?: it)
    } != 0

    fun setValue(value: BigInteger?, token: Token) {
        valueView.current_fiat.clearFocus()
        currentToken = token
        currentAmountString = value?.toValueString(token) ?: "?"
        currentAmount = value
        adaptFiat()
    }

    private fun adaptFiat() {
        if (currentToken?.isRootToken() == true) {
            val inFiat = exchangeRateProvider.convertToFiat(currentAmount, settings.currentFiat)
            currentFiatString = inFiat?.toFiatValueString() ?: "?"
            currentFiatUncutString = (inFiat?.toString() ?: "?")
        }
        refreshNonValues()
    }

    private fun adaptValueFromFiat() {
        if (currentToken?.isRootToken() == true) {
            currentAmount = exchangeRateProvider.convertFromFiat(currentFiatString?.toBigDecimal(), settings.currentFiat)
            currentAmountString = currentAmount?.toValueString(currentToken!!)
        }
        refreshNonValues()
    }

    private fun getValueFromString(): BigInteger? = try {
        ((currentAmountString ?: "").asBigDecimal() * BigDecimal("1" + currentToken!!.decimalsInZeroes())).toBigInteger()
    } catch (e: java.lang.Exception) {
        null
    }

    fun getValueOrZero(): BigInteger = currentAmount ?: ZERO

    fun setEnabled(b: Boolean) {
        valueView.current_fiat.isEnabled = b
        valueView.current_eth.isEnabled = b
    }
}