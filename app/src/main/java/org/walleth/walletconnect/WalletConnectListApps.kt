package org.walleth.walletconnect

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types.newParameterizedType
import kotlinx.android.synthetic.main.activity_list_nofab.*
import kotlinx.android.synthetic.main.item_wc_app.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.ligi.kaxt.setVisibility
import org.walleth.R
import org.walleth.base_activities.BaseSubActivity
import org.walleth.data.AppDatabase

val list = """
    [
    { 
        "name": "Example dApp",
        "url": "https://example.walletconnect.org",
        "icon": "https://example.walletconnect.org/favicon.ico",
        "networks": ["1","4","5","100"]
    },    
    { 
        "name": "ENS",
        "url": "https://app.ens.domains",
        "icon": "https://app.ens.domains/favicon-32x32.png",
        "networks": ["1","4","5","3"]
     },
      { 
        "name": "Etherscan",
        "url": "https://etherscan.io",
        "icon": "https://etherscan.io/images/brandassets/etherscan-logo-circle.png",
        "networks" : ["1"]
     },   
      { 
        "name": "Etherscan",
        "url": "https://goerli.etherscan.io",
        "icon": "https://etherscan.io/images/brandassets/etherscan-logo-circle.png",
        "networks" : ["5"]
     },   
     {
         "name": "Gnosis safe",
         "url": "https://gnosis-safe.io/app",
         "networks": ["1"],
         "icon": "https://gnosis-safe.io/app/favicon.ico"
     },
     {
         "name": "Gnosis safe",
         "url": "https://rinkeby.gnosis-safe.io/app/",
         "networks": ["4"],
         "icon": "https://rinkeby.gnosis-safe.io/app/favicon.ico"
     },
     { 
        "name": "ReMix IDE",
        "networks": [ "*" ],
        "url": "http://remix.ethereum.org",
        "icon": "https://raw.githubusercontent.com/ethereum/remix-ide/master/favicon.ico"
     }, 
     { 
      "name": "uniswap",
       "url": "https://app.uniswap.org",
       "networks": ["1"],
       "icon": "https://app.uniswap.org/./favicon.png"
      },
     { 
      "name": "zkSync",
       "url": "https://rinkeby.zksync.io",       
       "networks": ["1"],
       "icon": "https://rinkeby.zksync.io/_nuxt/icons/icon_64x64.3fdd8f.png"
      },
        { 
      "name": "zkSync",
       "url": "https://wallet zksync.io",       
       "networks": ["4"],
       "icon": "https://rinkeby.zksync.io/_nuxt/icons/icon_64x64.3fdd8f.png"
      },
     { 
        "name": "Other Apps",
        "url": "https://walletconnect.org/apps",
        "icon": "https://example.walletconnect.org/favicon.ico"
    }
    ]
    
""".trimIndent()

data class WalletConnectApp(val name: String, val url: String, val icon: String?, val networks: List<String>?)
data class WalletConnectEnhancedApp(val name: String, val url: String, val icon: String?, val networks: String?)


class WalletConnectListApps : BaseSubActivity() {

    val moshi: Moshi by inject()
    val appDatabase: AppDatabase by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_nofab)

        supportActionBar?.subtitle = "WalletConnect Apps"

        val adapter: JsonAdapter<List<WalletConnectApp>> = moshi.adapter(newParameterizedType(List::class.java, WalletConnectApp::class.java))

        lifecycleScope.launch(Dispatchers.Default) {
            val list = adapter.fromJson(list)!!.map {
                val networks = it.networks?.map { network ->
                    val chainId = network.toBigIntegerOrNull()
                    when {
                        (chainId != null) -> appDatabase.chainInfo.getByChainId(chainId)?.name
                        (network == "*") -> "All networks"
                        else -> "Unknown"
                    }

                }?.joinToString(", ")
                WalletConnectEnhancedApp(it.name, it.url, it.icon, networks)
            }

            lifecycleScope.launch(Dispatchers.Main) {
                recycler_view.layoutManager = LinearLayoutManager(this@WalletConnectListApps)
                recycler_view.adapter = WalletConnectAdapter(list)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_manage_wc, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.menu_input_text -> true.also {
            showWalletConnectURLInputAlert()
        }
        else -> super.onOptionsItemSelected(item)
    }

}

class WalletConnectAppViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
    fun bind(app: WalletConnectEnhancedApp) {
        view.app_name.text = app.name

        view.app_networks.text = app.networks

        view.app_networks.setVisibility(app.networks != null)
        app.icon?.let {
            view.session_icon.load(it)
        }
        view.session_card.setOnClickListener {
            view.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(app.url)).apply {
                flags += Intent.FLAG_ACTIVITY_NEW_TASK
            })

        }
    }
}

class WalletConnectAdapter(private val allFunctions: List<WalletConnectEnhancedApp>) : RecyclerView.Adapter<WalletConnectAppViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = WalletConnectAppViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_wc_app, parent, false))

    override fun getItemCount() = allFunctions.size

    override fun onBindViewHolder(holder: WalletConnectAppViewHolder, position: Int) {
        holder.bind(allFunctions[position])
    }

}
