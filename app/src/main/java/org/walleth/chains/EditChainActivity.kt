package org.walleth.chains

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.activity_create_chain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kethereum.rpc.HttpEthereumRPC
import org.koin.android.ext.android.inject
import org.ligi.kaxt.doAfterEdit
import org.walleth.R
import org.walleth.base_activities.BaseSubActivity
import org.walleth.data.AppDatabase
import org.walleth.data.chaininfo.ChainInfo
import org.walleth.data.chaininfo.NativeCurrency
import java.math.BigInteger

fun Context.startEditChainActivity(chainInfo: ChainInfo) = startActivity(Intent(this, EditChainActivity::class.java).putExtra("chainId", chainInfo))

open class EditChainActivity : BaseSubActivity() {

    val chainInfoProvider: ChainInfoProvider by inject()
    val appDatabase: AppDatabase by inject()
    val chainInfo: ChainInfo? by lazy { intent.getParcelableExtra<ChainInfo>("chainId") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_create_chain)

        chainInfo?.let {
            newChainId.setText(it.chainId.toString())
            newChainName.setText(it.name)

            it.rpc.firstOrNull()?.let { rpc ->
                newChainRPC.setText(rpc)
            }

            newChainNativeCurrencySymbol.setText(it.nativeCurrency.symbol)
            newChainNativeCurrencyDecimals.setText(it.nativeCurrency.decimals.toString())
            newChainNativeCurrencyName.setText(it.nativeCurrency.name)

            newChainInfoURL.setText(it.infoURL)
            newChainNetworkId.setText(it.networkId.toString())

            it.faucets.firstOrNull()?.let { faucet ->
                newChainFaucet.setText(faucet)
            }

        }

        supportActionBar?.subtitle = if (chainInfo == null) "Create chain" else "Edit chain"

        fab.setOnClickListener {
            tryToCreateChain()
        }

        if (newChainId.text?.isBlank() != false) {
            newChainRPC.doAfterEdit {
                if (!newChainId.toString().isBlank()) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        try {
                            val tempRPC = HttpEthereumRPC(it.toString())
                            val chainId = withContext(Dispatchers.IO) {
                                tempRPC.chainId()
                            }
                            if (chainId != null) {
                                newChainId.setText(chainId.value.toString())
                            }
                        } catch (exception: Exception) {
                        }
                    }
                }
            }
        }
    }

    private fun tryToCreateChain() = lifecycleScope.launch {
        val name = newChainName.text?.toString()
        val chainId = newChainId.text?.toString()

        val rpc = newChainRPC.text?.toString()
        val faucet = newChainFaucet.text?.toString()
        val nativeCurrencyDecimals = newChainNativeCurrencyDecimals.text?.toString()
        val nativeCurrencySymbol = newChainNativeCurrencySymbol.text?.toString()

        when {
            name?.isBlank() != false -> newChainName.error = "You need to enter a name"
            rpc?.isBlank() != false -> newChainRPC.error = "You need to enter a RPC URL"
            !rpc.startsWith("http://") && !rpc.startsWith("https://") -> newChainRPC.error = "Must start with http:// or https://"
            rpc.substringAfter("//").isBlank() -> newChainRPC.error = "Host must not be empty"
            chainId?.isBlank() != false -> newChainId.error = "You need to enter a chain ID"
            nativeCurrencySymbol?.isBlank() != false -> newChainRPC.error = "You need to enter a native currency symbol"
            nativeCurrencyDecimals?.isBlank() != false -> newChainRPC.error = "You need to enter decimals for the native currency"


            else -> {
                appDatabase.chainInfo.upsert(ChainInfo(
                        name = name,
                        chainId = BigInteger(chainId),
                        networkId = newChainNetworkId.text?.toString()?.toLongOrNull()?:chainId.toLong(),
                        shortName = name,
                        rpc = listOf(rpc).plus(chainInfo?.rpc ?: emptyList()).distinct(),
                        faucets = mutableListOf<String>().plus(faucet?.let { listOf(it) } ?: emptyList()).plus(chainInfo?.rpc ?: emptyList()).distinct(),
                        infoURL = newChainInfoURL.text.toString(),
                        nativeCurrency = NativeCurrency(nativeCurrencySymbol, newChainNativeCurrencyName.text.toString(), nativeCurrencyDecimals.toInt())
                ))

                finish()
            }
        }
    }

}
