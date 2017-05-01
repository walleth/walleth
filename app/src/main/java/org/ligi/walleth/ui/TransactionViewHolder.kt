package org.ligi.walleth.ui

import android.content.Intent
import android.support.v7.widget.RecyclerView
import android.text.format.DateUtils
import android.view.View
import kotlinx.android.synthetic.main.transaction_item.view.*
import org.ligi.kaxt.setVisibility
import org.ligi.walleth.activities.TransactionActivity
import org.ligi.walleth.data.addressbook.AddressBook
import org.ligi.walleth.data.transactions.Transaction
import org.threeten.bp.ZoneOffset

class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {


    fun bind(transaction: Transaction, addressBook: AddressBook) {

        itemView.difference.setEtherValue(transaction.value)

        itemView.address.text = addressBook.getEntryForName(transaction.from).name

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
                intent.putExtra(TransactionActivity.HASH_KEY,it)
                itemView.context.startActivity(intent)
            }

        }
    }

}