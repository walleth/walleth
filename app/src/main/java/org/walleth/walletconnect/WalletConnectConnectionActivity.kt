package org.walleth.walletconnect

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts.*
import androidx.lifecycle.lifecycleScope
import coil.load
import kotlinx.android.synthetic.main.activity_wallet_connect.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.kethereum.erc681.ERC681
import org.kethereum.erc681.generateURL
import org.kethereum.extensions.hexToBigInteger
import org.kethereum.model.Address
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.komputing.khex.model.HexString
import org.ligi.kaxt.setVisibility
import org.ligi.kaxtui.alert
import org.walletconnect.Session
import org.walletconnect.Session.MethodCall.*
import org.walletconnect.Session.Status.Approved
import org.walletconnect.Session.Status.Closed
import org.walleth.R
import org.walleth.accounts.AccountPickActivity
import org.walleth.base_activities.BaseSubActivity
import org.walleth.chains.ChainInfoProvider
import org.walleth.chains.SwitchChainActivity
import org.walleth.data.AppDatabase
import org.walleth.data.EXTRA_KEY_ADDRESS
import org.walleth.data.addresses.CurrentAddressProvider
import org.walleth.sign.SignTextActivity
import org.walleth.sign.getSignTypedDataIntent
import org.walleth.transactions.CreateTransactionActivity

private const val EXTRA_FROM_SCAN = "fromScan"

fun Context.getWalletConnectIntent(data: Uri) = Intent(this, WalletConnectConnectionActivity::class.java).apply {
    setData(data)
    putExtra(EXTRA_FROM_SCAN, true)
}

class WalletConnectConnectionActivity : BaseSubActivity() {

    private val currentAddressProvider: CurrentAddressProvider by inject()
    private val currentNetworkProvider: ChainInfoProvider by inject()

    private val wcViewModel: WalletConnectViewModel by viewModel()
    private val appDatabase: AppDatabase by inject()

    private var currentRequestId: Long? = null

    private var accounts = listOf<String>()

    private var approved = false

    private var mService: WalletConnectService? = null
    private var mBound: Boolean = false

    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as WalletConnectService.LocalBinder
            mService = binder.getService()

            if (mService?.handler?.session == null) {
                startService(getServiceIntent())
            }

            processCallAndStatus()
            mService?.uiPendingCallback = {
                processCallAndStatus()
            }

            wcViewModel.peerMeta = mService?.handler?.session?.peerMeta()

            applyViewModel()

            mBound = true
        }

        private fun processCallAndStatus() {

            mService?.takeCall {
                sessionCallback.onMethodCall(it)
            }

            val uiPendingStatus = mService?.uiPendingStatus
            if (uiPendingStatus != null) {
                sessionCallback.onStatus(uiPendingStatus)
                mService?.uiPendingStatus = null
                applyViewModel()
            }
        }


        override fun onServiceDisconnected(arg0: ComponentName) {
            mBound = false
            mService = null
        }
    }


    private val signTxActionForResult = registerForActivityResult(StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            it.data?.getStringExtra("TXHASH")?.let { txHash ->
                mService?.handler?.session?.approveRequest(currentRequestId!!, txHash)
            }
        } else {
            mService?.handler?.session?.rejectRequest(currentRequestId!!, 1L, "user canceled")
        }

        if (close_after_interactions_checkbox.isChecked) {
            finish()
        }

    }

    private val signTextActionForResult = registerForActivityResult(StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK && it.data?.hasExtra("SIGNATURE") == true) {
            val result = it.data?.getStringExtra("SIGNATURE")?.removePrefix("0x")
            mService?.handler?.session?.approveRequest(currentRequestId!!, "0x$result")
        } else {
            mService?.handler?.session?.rejectRequest(currentRequestId!!, 1L, "user canceled")
        }

        if (close_after_interactions_checkbox.isChecked) {
            finish()
        }

    }

    private val switchNetActionForResult = registerForActivityResult(StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            lifecycleScope.launch(Dispatchers.Main) {
                mService?.handler?.session?.approve(accounts, currentNetworkProvider.getCurrent().chainId.toLong())
            }
        }
    }


    private val switchAccountActionForResult = registerForActivityResult(StartActivityForResult()) {
        lifecycleScope.launch(Dispatchers.Main) {
            if (it.resultCode == Activity.RESULT_OK) {
                it.data?.getStringExtra(EXTRA_KEY_ADDRESS)?.let { addressHex ->
                    currentAddressProvider.setCurrent(Address(addressHex))
                    accounts = listOf(addressHex)

                    if (approved) {
                        mService?.handler?.session?.approve(accounts, currentNetworkProvider.getCurrent().chainId.toLong())
                    }
                }
            }
        }
    }

    private val sessionCallback = object : Session.Callback {
        override fun onMethodCall(call: Session.MethodCall) {
            lifecycleScope.launch(Dispatchers.Main) {
                when (call) {
                    is SessionRequest -> {
                        wcViewModel.peerMeta = call.peer.meta
                        applyViewModel()

                        fab.show()

                    }

                    is SignMessage -> {
                        currentRequestId = call.id
                        signText(call.message)

                        signTextActionForResult.launch(intent)
                    }

                    is SendTransaction -> {
                        currentRequestId = call.id
                        lifecycleScope.launch(Dispatchers.Main) {
                            val url = ERC681(scheme = "ethereum",
                                    address = call.to,
                                    value = HexString(call.value).hexToBigInteger(),
                                    gasLimit = call.gasLimit?.let { HexString(it).hexToBigInteger() }
                            ).generateURL()


                            val intent = Intent(this@WalletConnectConnectionActivity, CreateTransactionActivity::class.java).apply {
                                this.data = Uri.parse(url)
                                if (call.data.isNotEmpty()) {
                                    putExtra("data", call.data)
                                }

                                putExtra("gasPrice", call.gasPrice)
                                putExtra("nonce", call.nonce)
                                putExtra("from", call.from)
                                putExtra("parityFlow", false)
                            }

                            signTxActionForResult.launch(intent)
                        }
                    }

                    is Custom -> {
                        currentRequestId = call.id
                        if (call.method == "personal_sign") {
                            if (call.params == null) {
                                alert("got personal_sign without parameters")
                            } else {
                                signText(call.params!!.first().toString())
                            }
                        } else if (call.method == "eth_signTypedData") {
                            val intent = getSignTypedDataIntent(call.params!!.last().toString())
                            signTextActionForResult.launch(intent)
                        } else {
                            alert("The method " + call.method + " is not yet supported. If you think it should - open an issue in on github or write a mail to walleth@walleth.org") {
                                if (close_after_interactions_checkbox.isChecked) {
                                    finish()
                                }
                            }
                            mService?.handler?.session?.rejectRequest(currentRequestId!!, 1L, "user canceled")
                        }
                    }
                }
            }

        }

        override fun onStatus(status: Session.Status) {
            lifecycleScope.launch(Dispatchers.Main) {
                when (status) {
                    is Error -> alert("Error:" + status.message)
                    is Approved -> {
                        wcViewModel.showSwitchAccountButton = true
                        wcViewModel.showSwitchNetworkButton = true
                        applyViewModel()
                    }
                    is Closed -> {
                        finish()
                    }
                    else -> Unit
                }

            }

        }


    }

    private fun signText(message: String) {
        val intent = Intent(this@WalletConnectConnectionActivity, SignTextActivity::class.java).apply {
            putExtra(Intent.EXTRA_TEXT, message)

        }
        signTextActionForResult.launch(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_wallet_connect)

        supportActionBar?.subtitle = getString(R.string.wallet_connect)

        lifecycleScope.launch(Dispatchers.Main) {
            currentNetworkProvider.getFlow().onEach {
                network_name.text = it.name
            }.launchIn(lifecycleScope)

            currentAddressProvider.flow.onEach { address ->
                address?.let {
                    lifecycleScope.launch {
                        val entry = appDatabase.addressBook.byAddress(address)
                        account_name.text = entry?.name
                    }
                }
            }.launchIn(lifecycleScope)
        }

        wc_change_account.setOnClickListener {
            selectAccount()
        }

        wc_change_network.setOnClickListener {
            switchNetActionForResult.launch(Intent(this@WalletConnectConnectionActivity, SwitchChainActivity::class.java))
        }

        fab.setOnClickListener {
            approved = true

            accounts = listOf(currentAddressProvider.getCurrentNeverNull().hex)
            lifecycleScope.launch(Dispatchers.Main) {
                mService?.handler?.session?.approve(accounts, currentNetworkProvider.getCurrent().chainId.toLong())
            }
            if (close_after_interactions_checkbox.isChecked) {
                finish()
            } else {
                fab.hide()
            }

        }

    }

    private fun getServiceIntent() = Intent(applicationContext, WalletConnectService::class.java).setData(intent.data)

    override fun onResume() {
        super.onResume()
        intent.data?.let { uri ->
            if (Session.Config.fromWCUri(uri.toString()).isFullyQualifiedConfig()) {
                stopService(getServiceIntent())
            }
        }

        close_after_interactions_checkbox.isChecked = !intent.getBooleanExtra(EXTRA_FROM_SCAN, false)
        bindService(getServiceIntent(), connection, Context.BIND_AUTO_CREATE)
        applyViewModel()
    }

    override fun onPause() {
        super.onPause()
        mService?.uiPendingCallback = null

    }

    override fun onBackPressed() {
        super.onBackPressed()

        if (!approved) {
            mService?.handler?.session?.reject()
        }
    }

    private fun applyViewModel() {

        val isLoading = wcViewModel.peerMeta == null || !mBound
        loading_indicator.setVisibility(isLoading)
        main_wc_scrollview.setVisibility(!isLoading)

        app_text.text = wcViewModel.peerMeta?.name ?: "Unknown app"
        wcViewModel.peerMeta?.icons?.firstOrNull()?.let { url ->
            app_icon.load(url)
            app_icon.visibility = View.VISIBLE
        }
    }


    private fun selectAccount() {
        val intent = Intent(this@WalletConnectConnectionActivity, AccountPickActivity::class.java)

        switchAccountActionForResult.launch(intent)
    }
}
