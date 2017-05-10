package org.walleth.ui

import android.content.Intent
import android.support.v7.widget.RecyclerView
import android.text.format.DateUtils
import android.view.View
import kotlinx.android.synthetic.main.transaction_item.view.*
import org.ligi.kaxt.setVisibility
import org.threeten.bp.ZoneOffset
import org.walleth.activities.TransactionActivity
import org.walleth.data.addressbook.AddressBook
import org.walleth.data.transactions.Transaction

class TransactionViewHolder(itemView: View, val direction: TransactionAdapterDirection) : RecyclerView.ViewHolder(itemView) {


    fun bind(transaction: Transaction, addressBook: AddressBook) {

        itemView.difference.setEtherValue(transaction.value)

        itemView.address.text = if (direction == TransactionAdapterDirection.INCOMMING) {
            addressBook.getEntryForName(transaction.from)
        } else {
            addressBook.getEntryForName(transaction.to)
        }.name

        itemView.transaction_err.setVisibility(transaction.error != null)
        if (transaction.error != null) {
            itemView.transaction_err.text = transaction.error
        }

        val localTime = transaction.localTime
        val epochMillis = localTime.toEpochSecond(ZoneOffset.systemDefault().rules.getOffset(localTime)) * 1000
        itemView.date.text = DateUtils.getRelativeDateTimeString(itemView.context, epochMillis,
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.WEEK_IN_MILLIS,
                0
        )

        itemView.isClickable = true
        itemView.setOnClickListener {
            transaction.txHash?.let {
                val intent = Intent(itemView.context, TransactionActivity::class.java)
                intent.putExtra(TransactionActivity.HASH_KEY, it)
                itemView.context.startActivity(intent)
            }

        }
    }

}