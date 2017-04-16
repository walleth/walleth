package org.ligi.walleth.ui

import android.support.v7.widget.RecyclerView
import android.text.format.DateUtils
import android.view.View
import kotlinx.android.synthetic.main.transaction_item.view.*
import org.ligi.walleth.data.CachingExchangeProvider
import org.ligi.walleth.data.FixedValueExchangeProvider
import org.ligi.kaxt.startActivityFromURL
import org.ligi.walleth.data.ETH_IN_WEI
import org.ligi.walleth.data.Transaction
import org.threeten.bp.ZoneOffset

class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    val cryptoCompareExchangeProvider = CachingExchangeProvider(FixedValueExchangeProvider(), itemView.context)

    fun bind(transaction: Transaction) {
        val divided = transaction.value / ETH_IN_WEI

        var differenceText = "" + divided + "ETH"

        val exChangeRate = cryptoCompareExchangeProvider.getExChangeRate("EUR")
        if (exChangeRate != null) {
            val times = exChangeRate.times(divided.toDouble())
            differenceText += String.format(" ( %.2f EUR)", times)
        }

        itemView.difference.text = transaction.ref.toString() // differenceText

        itemView.address.text = transaction.to.hex

        itemView.date.text = DateUtils.getRelativeDateTimeString(itemView.context, transaction.localTime.toEpochSecond(ZoneOffset.UTC) * 1000,
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