package org.walleth.ui

import android.app.Activity
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.token_list_item.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.ligi.kaxt.setVisibility
import org.ligi.kaxt.startActivityFromURL
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.data.AppDatabase
import org.walleth.data.chaininfo.ChainInfo
import org.walleth.data.networks.ChainInfoProvider
import org.walleth.data.networks.CurrentAddressProvider
import org.walleth.data.networks.getFaucetURL
import org.walleth.data.networks.hasFaucetWithAddressSupport
import org.walleth.data.tokens.CurrentTokenProvider
import org.walleth.data.tokens.Token
import org.walleth.data.tokens.isRootToken

class TokenViewHolder(itemView: View, val activity: Activity) : RecyclerView.ViewHolder(itemView), KoinComponent {

    private val currentTokenProvider: CurrentTokenProvider by inject()
    private val currentAddressProvider: CurrentAddressProvider by inject()
    private val chainInfoProvider: ChainInfoProvider by inject()
    private val appDatabase: AppDatabase by inject()

    fun bind(tokenDescriptor: Token) {
        itemView.token_name.text = "${tokenDescriptor.name}(${tokenDescriptor.symbol})"

        GlobalScope.launch(Dispatchers.Default) {
            val chainInfo = appDatabase.chainInfo.getByChainId(tokenDescriptor.chain)

            GlobalScope.launch(Dispatchers.Main) {
                itemView.token_chain.text = "Chain: " + chainInfo?.name
                itemView.token_starred.setImageResource(
                        if (tokenDescriptor.starred) {
                            R.drawable.ic_star_24dp
                        } else {
                            R.drawable.ic_star_border_24dp
                        }
                )

                if (tokenDescriptor.isRootToken()) {
                    itemView.faucet.prepareFaucetButton(chainInfo, currentAddressProvider)
                }

                itemView.setOnClickListener {

                    if (chainInfo == null) {
                        activity.alert("Chain for this token not found")
                    } else {
                        currentTokenProvider.setCurrent(tokenDescriptor)
                        chainInfoProvider.setCurrent(chainInfo)
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

    }


}

fun AppCompatImageView.prepareFaucetButton(chainInfo: ChainInfo?,
                                           currentAddressProvider: CurrentAddressProvider,
                                           postAction: () -> Unit = {}) {
    if (chainInfo?.faucets?.isNotEmpty() == true) {
        setImageResource(if (chainInfo.hasFaucetWithAddressSupport()) R.drawable.ic_flash_on_black_24dp else R.drawable.ic_redeem_black_24dp)
        setVisibility(true)
    } else {
        setVisibility(false)
    }

    setOnClickListener {
        chainInfo?.getFaucetURL(currentAddressProvider.getCurrentNeverNull())?.let { url ->
            context.startActivityFromURL(url)
            postAction.invoke()
        }
    }
}