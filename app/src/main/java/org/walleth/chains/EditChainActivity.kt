package org.walleth.chains

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
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
import org.walleth.databinding.ActivityCreateChainBinding
import java.math.BigInteger

fun Context.startEditChainActivity(chainInfo: ChainInfo) = startActivity(Intent(this, EditChainActivity::class.java).putExtra("chainId", chainInfo))

open class EditChainActivity : BaseSubActivity() {

    val binding by lazy { ActivityCreateChainBinding.inflate(layoutInflater) }

    val chainInfoProvider: ChainInfoProvider by inject()
    val appDatabase: AppDatabase by inject()
    val chainInfo: ChainInfo? by lazy { intent.getParcelableExtra<ChainInfo>("chainId") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_create_chain)

        chainInfo?.let {
            binding.newChainId.setText(it.chainId.toString())
            binding.newChainName.setText(it.name)

            it.rpc.firstOrNull()?.let { rpc ->
                binding.newChainRPC.setText(rpc)
            }

            binding.newChainNativeCurrencySymbol.setText(it.nativeCurrency.symbol)
            binding.newChainNativeCurrencyDecimals.setText(it.nativeCurrency.decimals.toString())
            binding.newChainNativeCurrencyName.setText(it.nativeCurrency.name)

            binding.newChainInfoURL.setText(it.infoURL)
            binding.newChainNetworkId.setText(it.networkId.toString())

            it.faucets.firstOrNull()?.let { faucet ->
                binding.newChainFaucet.setText(faucet)
            }

        }

        supportActionBar?.subtitle = if (chainInfo == null) "Create chain" else "Edit chain"

        binding.fab.setOnClickListener {
            tryToCreateChain()
        }

        if (binding.newChainId.text?.isBlank() != false) {
            binding.newChainRPC.doAfterEdit {
                if (binding.newChainId.toString().isNotBlank()) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        try {
                            val tempRPC = HttpEthereumRPC(it.toString())
                            val chainId = withContext(Dispatchers.IO) {
                                tempRPC.chainId()
                            }
                            if (chainId != null) {
                                binding.newChainId.setText(chainId.value.toString())
                            }
                        } catch (exception: Exception) {
                        }
                    }
                }
            }
        }
    }

    private fun tryToCreateChain() = lifecycleScope.launch {
        val name = binding.newChainName.text?.toString()
        val chainId = binding.newChainId.text?.toString()

        val rpc = binding.newChainRPC.text?.toString()
        val faucet = binding.newChainFaucet.text?.toString()
        val nativeCurrencyDecimals = binding.newChainNativeCurrencyDecimals.text?.toString()
        val nativeCurrencySymbol = binding.newChainNativeCurrencySymbol.text?.toString()

        when {
            name?.isBlank() != false -> binding.newChainName.error = "You need to enter a name"
            rpc?.isBlank() != false -> binding.newChainRPC.error = "You need to enter a RPC URL"
            !rpc.startsWith("http://") && !rpc.startsWith("https://") -> binding.newChainRPC.error = "Must start with http:// or https://"
            rpc.substringAfter("//").isBlank() -> binding.newChainRPC.error = "Host must not be empty"
            chainId?.isBlank() != false -> binding.newChainId.error = "You need to enter a chain ID"
            nativeCurrencySymbol?.isBlank() != false -> binding.newChainRPC.error = "You need to enter a native currency symbol"
            nativeCurrencyDecimals?.isBlank() != false -> binding.newChainRPC.error = "You need to enter decimals for the native currency"


            else -> {
                appDatabase.chainInfo.upsert(ChainInfo(
                        name = name,
                        chainId = BigInteger(chainId),
                        networkId = binding.newChainNetworkId.text?.toString()?.toLongOrNull()?:chainId.toLong(),
                        shortName = name,
                        rpc = listOf(rpc).plus(chainInfo?.rpc ?: emptyList()).distinct(),
                        faucets = mutableListOf<String>().plus(faucet?.let { listOf(it) } ?: emptyList()).plus(chainInfo?.rpc ?: emptyList()).distinct(),
                        infoURL = binding.newChainInfoURL.text.toString(),
                        nativeCurrency = NativeCurrency(nativeCurrencySymbol, binding.newChainNativeCurrencyName.text.toString(), nativeCurrencyDecimals.toInt())
                ))

                finish()
            }
        }
    }

}
