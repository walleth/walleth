package org.walleth.ui

import android.app.Activity
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import kotlinx.android.synthetic.main.token_list_item.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.kethereum.model.ChainId
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import org.ligi.kaxt.setVisibility
import org.ligi.kaxt.startActivityFromURL
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.data.AppDatabase
import org.walleth.data.networks.*
import org.walleth.data.tokens.CurrentTokenProvider
import org.walleth.data.tokens.Token
import org.walleth.data.tokens.isRootToken

class TokenViewHolder(itemView: View, val activity: Activity) : RecyclerView.ViewHolder(itemView), KoinComponent {

    private val currentTokenProvider: CurrentTokenProvider by inject()
    private val currentAddressProvider: CurrentAddressProvider by inject()
    private val networkDefinitionProvider: NetworkDefinitionProvider by inject()
    private val appDatabase: AppDatabase by inject()

    fun bind(tokenDescriptor: Token) {
        itemView.token_name.text = "${tokenDescriptor.name}(${tokenDescriptor.symbol})"
        val network = ChainId(tokenDescriptor.chain).findNetworkDefinition()
        itemView.token_chain.text = "Chain: " + network?.getNetworkName()

        itemView.token_starred.setImageResource(
                if (tokenDescriptor.starred) {
                    R.drawable.ic_star_24dp
                } else {
                    R.drawable.ic_star_border_24dp
                }
        )

        if (tokenDescriptor.isRootToken() && network?.faucets?.isNotEmpty() == true) {
            itemView.faucet.setImageResource(if (network.faucets.find { it.contains(FAUCET_ADDRESS_TOKEN) } != null) R.drawable.ic_flash_on_black_24dp else R.drawable.ic_redeem_black_24dp)
            itemView.faucet.setVisibility(true)
        } else {
            itemView.faucet.setVisibility(false)
        }
        itemView.faucet.setOnClickListener {
            ChainId(tokenDescriptor.chain).getFaucetURL(currentAddressProvider.getCurrentNeverNull())?.let { url ->
                activity.startActivityFromURL(url)
            }
        }
        itemView.setOnClickListener {

            if (network == null) {
                activity.alert("Chain for this token not found")
            } else {
                currentTokenProvider.setCurrent(tokenDescriptor)
                networkDefinitionProvider.setCurrent(network)
                activity.finish()
            }

        }

        itemView.token_starred.setOnClickListener {
            GlobalScope.launch {
                appDatabase.tokens.upsert(tokenDescriptor.copy(starred = !tokenDescriptor.starred))
            }
        }
    }
}