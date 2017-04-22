package org.ligi.walleth.ui

import android.support.v7.widget.RecyclerView
import android.text.format.DateUtils
import android.view.View
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import kotlinx.android.synthetic.main.transaction_item.view.*
import org.ligi.kaxt.startActivityFromURL
import org.ligi.walleth.functions.toEtherValueString
import org.ligi.walleth.data.ETH_IN_WEI
import org.ligi.walleth.data.ExchangeRateProvider
import org.ligi.walleth.data.Transaction
import org.threeten.bp.ZoneOffset
import java.math.BigDecimal

class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    fun bind(transaction: Transaction) {

        var differenceText = transaction.value.toEtherValueString() + "ETH"

        val exchangeRateProvider: ExchangeRateProvider by LazyKodein(itemView.context.appKodein).instance()

        exchangeRateProvider.getExChangeRate("EUR")?.let {
            val divided = BigDecimal(transaction.value).divide(BigDecimal(ETH_IN_WEI))
            val times = BigDecimal(it).times(divided)
            differenceText += String.format(" (%.2f EUR)", times)
        }

        itemView.difference.text = differenceText

        itemView.address.text = transaction.to.hex


        val localTime = transaction.localTime
        val epochMillis = localTime.toEpochSecond(ZoneOffset.systemDefault().rules.getOffset(localTime)) * 1000
        itemView.date.text = DateUtils.getRelativeDateTimeString(itemView.context, epochMillis,
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.WEEK_IN_MILLIS,
                0
        )

        itemView.isClickable = true
        itemView.setOnClickListener {
            itemView.context.startActivityFromURL("https://testnet.etherscan.io/tx/"+transaction.txHash)
        }
    }

}