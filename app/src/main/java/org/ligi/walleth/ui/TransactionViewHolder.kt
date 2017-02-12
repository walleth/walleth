package org.ligi.ewallet.ui

import android.support.v7.widget.RecyclerView
import android.text.format.DateUtils
import android.view.View
import kotlinx.android.synthetic.main.transaction_item.view.*
import org.ligi.ewallet.data.CachingExchangeProvider
import org.ligi.ewallet.data.FixedValueExchangeProvider
import org.ligi.walleth.data.Transaction
import org.threeten.bp.ZoneOffset

class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    val cryptoCompareExchangeProvider = CachingExchangeProvider(FixedValueExchangeProvider(), itemView.context)

    fun bind(transaction: Transaction) {
        val divided = transaction.value / 1000000000000000000

        var differenceText = "" + divided + "ETH"

        val exChangeRate = cryptoCompareExchangeProvider.getExChangeRate("EUR")
        if (exChangeRate != null) {
            val times = exChangeRate.times(divided.toDouble())
            differenceText += String.format(" ( %.2f EUR)", times)
        }

        itemView.difference.text = differenceText
        itemView.address.text = transaction.address

        itemView.date.text = DateUtils.getRelativeDateTimeString(itemView.context, transaction.localTime.toEpochSecond(ZoneOffset.UTC) * 1000,
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.WEEK_IN_MILLIS,
                0
        )
    }

}