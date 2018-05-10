package org.walleth.ui.info.licenses


import android.support.v7.widget.RecyclerView
import android.view.View
import kotlinx.android.synthetic.main.item_license_entry.view.*
import org.ligi.kaxt.startActivityFromURL
import org.ligi.kaxtui.alert
import org.walleth.R

class LicenceInfoViewHolder(itemView: View?) : RecyclerView.ViewHolder(itemView) {
    fun bind(entry: LicenseInfoEntry, data: String) {
        val nameSplit = entry.name.split(":")
        val context = itemView.context
        itemView.license_text_title.text = nameSplit.last()
        itemView.license_text_detail1.text = nameSplit.first()

        val message = data.substring(entry.position, entry.position + entry.length)

        if (entry.length < 256) {
            itemView.license_text_detail2.text = message
        } else {
            itemView.license_text_detail2.text = context.getString(R.string.click_for_details)
        }
        itemView.setOnClickListener {


            if (message.startsWith("http")) {
                context.startActivityFromURL(message)
            } else {
                context.alert(message)
            }
        }
    }
}