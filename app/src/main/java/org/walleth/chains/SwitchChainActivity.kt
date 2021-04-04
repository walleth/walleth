package org.walleth.chains

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.lifecycleScope
import com.squareup.moshi.JsonEncodingException
import com.squareup.moshi.Moshi
import com.tingyik90.snackprogressbar.SnackProgressBar
import com.tingyik90.snackprogressbar.SnackProgressBarManager
import kotlinx.android.synthetic.main.activity_list.*
import kotlinx.android.synthetic.main.item_network_definition.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.android.ext.android.inject
import org.ligi.kaxt.setVisibility
import org.ligi.kaxt.startActivityFromClass
import org.ligi.kaxt.startActivityFromURL
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.data.addresses.CurrentAddressProvider
import org.walleth.data.chaininfo.ChainInfo
import org.walleth.enhancedlist.BaseEnhancedListActivity
import org.walleth.enhancedlist.EnhancedListAdapter
import org.walleth.enhancedlist.EnhancedListInterface
import org.walleth.util.hasTincubethSupport
import org.walleth.util.question
import timber.log.Timber
import java.io.IOException
import javax.net.ssl.SSLException

open class SwitchChainActivity : BaseEnhancedListActivity<ChainInfo>() {

    private val snackProgressBarManager by lazy { SnackProgressBarManager(coordinator, lifecycleOwner = this) }

    override val enhancedList by lazy {
        object : EnhancedListInterface<ChainInfo> {
            override suspend fun getAll() = appDatabase.chainInfo.getAll()
            override fun compare(t1: ChainInfo, t2: ChainInfo) = t1.chainId == t2.chainId
            override suspend fun upsert(item: ChainInfo) = appDatabase.chainInfo.upsert(item)

            override suspend fun undeleteAll() = appDatabase.chainInfo.undeleteAll()
            override suspend fun deleteAllSoftDeleted() = appDatabase.chainInfo.deleteAllSoftDeleted()
            override suspend fun filter(item: ChainInfo) = (!settings.filterFastFaucet || item.hasFaucetWithAddressSupport())
                    && (!settings.filterFaucet || (item.faucets.isNotEmpty() && !item.hasFaucetWithAddressSupport()))
                    && (!settings.filterTincubeth || (item.hasTincubethSupport()))
                    && checkForSearchTerm(item.name, item.nativeCurrency.symbol, item.nativeCurrency.name)
        }
    }


    val chainInfoProvider: ChainInfoProvider by inject()
    private val okHttpClient: OkHttpClient by inject()
    private val moshi: Moshi by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.subtitle = getString(R.string.chains_subtitle)

        fab.setOnClickListener {
            startActivityFromClass(EditChainActivity::class)
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.chain_list, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.menu_has_fast_faucet).isChecked = settings.filterFastFaucet
        menu.findItem(R.id.menu_has_faucet).isChecked = settings.filterFaucet
        menu.findItem(R.id.menu_has_tincubeth).isChecked = settings.filterTincubeth
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = super.onOptionsItemSelected(item).also {
        when (item.itemId) {
            R.id.menu_refresh -> downloadNewChains()
            R.id.menu_has_tincubeth -> item.filterToggle {
                settings.filterTincubeth = it
            }
            R.id.menu_has_fast_faucet -> item.filterToggle {
                settings.filterFastFaucet = it
                if (it) settings.filterFaucet = !it
            }
            R.id.menu_has_faucet -> item.filterToggle {
                settings.filterFaucet = it
                if (it) settings.filterFastFaucet = !it
            }
        }
    }

    private fun downloadNewChains() {
        val pb = SnackProgressBar(SnackProgressBar.TYPE_CIRCULAR, "Downloading chain definitions")
                .setIsIndeterminate(true)

        snackProgressBarManager.show(pb, SnackProgressBarManager.LENGTH_INDEFINITE)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val request = Request.Builder().url("https://chainid.network/chains_mini.json")
                val list = okHttpClient.newCall(request.build()).execute().body()?.string()?.let { json ->
                    moshi.deSerialize(json)
                }
                val newList = list?.filter { appDatabase.chainInfo.getByChainId(it.chainId) == null }
                lifecycleScope.launch(Dispatchers.Main) {

                    when {
                        newList == null -> handleRefreshError("cannot load chains")
                        newList.isEmpty() -> handleRefreshError("no new chains found")
                        else -> {
                            question(configurator = { setMessage("Really Import ${newList.size} Elements?") }, action = {
                                lifecycleScope.launch(Dispatchers.Main) {
                                    appDatabase.chainInfo.upsert(newList)
                                    refreshAdapter()
                                }
                            })

                        }
                    }

                }

            } catch (e: SSLException) {
                handleRefreshError("SSLError - cannot load chains. Setting your time can help in some cases. If it does not help please drop a mail to walleth@walleth.org")
            } catch (e: JsonEncodingException) {
                handleRefreshError("List was not valid JSON - this should not happen - if it happened to you - please drop a mail to walleth@walleth.org")
            } catch (e: IOException) {
                Timber.e(e, "Cannot load chains - there seems to be a problem with your internet")
                handleRefreshError("Cannot load chains - there seems to be a problem with your internet - please try again later. If you think it is not your internet - please drop a mail to walleth@walleth.org")
            } catch (e: Throwable) {
                Timber.e(e, "Cannot load chains - this should not happen - if it happened to you - please drop a mail to walleth@walleth.org")
                handleRefreshError("General Error - cannot load chains. $e")
            } finally {
                snackProgressBarManager.dismissAll()
            }
        }
    }

    private fun handleRefreshError(message: String) = lifecycleScope.launch(Dispatchers.Main) {
        alert(message)
    }

    override val adapter: EnhancedListAdapter<ChainInfo> by lazy {
        EnhancedListAdapter<ChainInfo>(
                layout = R.layout.item_network_definition,
                bind = { chainInfo, view ->
                    view.setOnClickListener {
                        lifecycleScope.launch( context = Dispatchers.IO) {
                            chainInfoProvider.setCurrent(chainInfo)
                        }
                        setResult(RESULT_OK)
                        finish()
                    }
                    view.in3_indicator.setVisibility(chainInfo.hasTincubethSupport())
                    val currentAddressProvider: CurrentAddressProvider by inject()
                    view.faucet_indicator.prepareFaucetButton(chainInfo, currentAddressProvider, postAction = {
                        view.performClick()
                    })
                    view.delete.setOnClickListener {
                        view.deleteWithAnimation(chainInfo)
                    }
                    view.edit_button.setOnClickListener {
                        startEditChainActivity(chainInfo)
                    }
                    view.info_button.setOnClickListener {
                        startActivityFromURL(chainInfo.infoURL)
                    }
                    view.chain_title.text = chainInfo.name
                })

    }
}
