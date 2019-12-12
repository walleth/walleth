package org.walleth.chains

import android.animation.ObjectAnimator
import android.view.View
import androidx.core.animation.doOnEnd
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_network_definition.view.*
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.walleth.chains.ChainInfoViewAction.*
import org.walleth.data.addresses.CurrentAddressProvider
import org.walleth.data.chaininfo.ChainInfo
import org.walleth.tokens.prepareFaucetButton

enum class ChainInfoViewAction {
    CLICK,
    EDIT,
    INFO,
    DELETE
}

class ChainInfoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), KoinComponent {

    fun bind(chainInfo: ChainInfo, onAction: (entry: ChainInfo, action: ChainInfoViewAction) -> Unit) {


        itemView.network_title.text = chainInfo.name
        itemView.info_indicator.visibility = if (chainInfo.infoURL.isNotEmpty()) View.VISIBLE else View.INVISIBLE

        val currentAddressProvider: CurrentAddressProvider by inject()
        itemView.faucet_indicator.prepareFaucetButton(chainInfo, currentAddressProvider, postAction = {
            onAction.invoke(chainInfo, CLICK)
        })

        mapOf(
                itemView to CLICK,
                itemView.info_indicator to INFO,
                itemView.edit_button to EDIT
        ).forEach {
            it.key.setOnClickListener { _ ->
                onAction.invoke(chainInfo, it.value)
            }
        }

        itemView.delete.setOnClickListener {
            ObjectAnimator.ofFloat(itemView, "translationX", itemView.width.toFloat()).apply {
                duration = 333
                start()
                doOnEnd {
                    onAction.invoke(chainInfo, DELETE)
                    itemView.handler.postDelayed({
                        itemView.translationX = 0f
                    }, 333)
                }
            }
        }

    }
}