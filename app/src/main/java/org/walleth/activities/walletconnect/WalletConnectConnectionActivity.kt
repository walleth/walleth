package org.walleth.activities.walletconnect

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import kotlinx.android.synthetic.main.activity_wallet_connect.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.kethereum.erc681.ERC681
import org.kethereum.erc681.generateURL
import org.kethereum.model.Address
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel
import org.ligi.kaxt.setVisibility
import org.walletconnect.Session
import org.walleth.R
import org.walleth.activities.*
import org.walleth.data.networks.CurrentAddressProvider
import org.walleth.data.networks.NetworkDefinitionProvider
import org.walleth.khex.clean0xPrefix
import org.walleth.viewmodels.WalletConnectViewModel
import java.math.BigInteger

fun Context.getWalletConnectIntent(data: Uri) = Intent(this, WalletConnectConnectionActivity::class.java).apply {
    setData(data)
}

private const val REQUEST_ID_SIGN_TEXT = 100
private const val REQUEST_ID_SIGN_TX = 101
private const val REQUEST_ID_SWITCH_NET = 102

class WalletConnectConnectionActivity : BaseSubActivity() {

    private val currentAddressProvider: CurrentAddressProvider by inject()
    private val currentNetworkProvider: NetworkDefinitionProvider by inject()

    private val wcViewModel: WalletConnectViewModel by viewModel()

    private var currentRequestId: Long? = null

    private var accounts = listOf<String>()

    private val sessionCallback = object : Session.Callback {
        override fun handleMethodCall(call: Session.MethodCall) {
            GlobalScope.launch(Dispatchers.Main) {
                when (call) {
                    is Session.MethodCall.SessionRequest -> {
                        wcViewModel.peerMeta = call.peer.meta
                        wcViewModel.statusText = "waiting for interactions with " + call.peer.meta?.name + " " + call.peer.meta?.icons
                        applyViewModel()

                        requestInitialAccount()
                    }

                    is Session.MethodCall.SignMessage -> {
                        currentRequestId = call.id
                        val intent = Intent(this@WalletConnectConnectionActivity, SignTextActivity::class.java).apply {
                            putExtra(Intent.EXTRA_TEXT, call.message)

                        }
                        startActivityForResult(intent, REQUEST_ID_SIGN_TEXT)
                    }

                    is Session.MethodCall.SendTransaction -> {
                        currentRequestId = call.id
                        GlobalScope.launch(Dispatchers.Main) {
                            val url = ERC681(scheme = "ethereum",
                                    address = call.to,
                                    value = BigInteger(call.value.clean0xPrefix(), 16),
                                    gas = call.gasLimit?.let { BigInteger(it.clean0xPrefix(), 16) }
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

                            startActivityForResult(intent, REQUEST_ID_SIGN_TX)
                        }
                    }
                }
            }
        }

        override fun sessionApproved() {
            wcViewModel.showSwitchAccountButton = true
            wcViewModel.showSwitchNetworkButton = true
            applyViewModel()
        }

        override fun sessionClosed() {
            finish()
        }

    }

    private fun requestInitialAccount(): AlertDialog? {
        return AlertDialog.Builder(this@WalletConnectConnectionActivity)
                .setTitle(getString(R.string.walletconnect_do_you_want_to_use, wcViewModel.peerMeta?.name))
                .setItems(R.array.walletconnect_options) { _, i ->
                    when (i) {
                        0 -> {
                            accounts = listOf(currentAddressProvider.getCurrentNeverNull().hex)
                            wcViewModel.session?.approve(accounts, currentNetworkProvider.getCurrent().chain.id.value)
                        }

                        1 -> selectAccount()

                        else -> {
                            wcViewModel.session?.reject()
                            finish()
                        }
                    }
                }
                .setOnCancelListener { finish() }
                .show()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_wallet_connect)
        supportActionBar?.subtitle = getString(R.string.wallet_connect)

        wc_change_account.setOnClickListener {
            selectAccount()
        }

        wc_change_network.setOnClickListener {
            startActivityForResult(Intent(this@WalletConnectConnectionActivity, SwitchNetworkActivity::class.java), REQUEST_ID_SWITCH_NET)
        }

        if (!wcViewModel.processURI(intent?.data.toString())) {
            requestInitialAccount()
        }
    }

    override fun onResume() {
        super.onResume()
        applyViewModel()

        wcViewModel.session?.addCallback(sessionCallback)
    }

    override fun onPause() {
        super.onPause()

        wcViewModel.session?.removeCallback(sessionCallback)
    }

    private fun applyViewModel() {

        wc_change_account.setVisibility(wcViewModel.showSwitchAccountButton)
        wc_change_network.setVisibility(wcViewModel.showSwitchNetworkButton)
        status_text.text = wcViewModel.statusText

    }

    private fun selectAccount() {
        val intent = Intent(this@WalletConnectConnectionActivity, AccountPickActivity::class.java)
        startActivityForResult(intent, TO_ADDRESS_REQUEST_CODE)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_ID_SWITCH_NET -> {
                wcViewModel.session?.approve(accounts, currentNetworkProvider.getCurrent().chain.id.value)
            }


            TO_ADDRESS_REQUEST_CODE -> {
                if (data?.hasExtra("HEX") == true) {
                    val addressHex = data.getStringExtra("HEX")
                    currentAddressProvider.setCurrent(Address(addressHex))
                    accounts = listOf(addressHex)
                    wcViewModel.session?.approve(accounts, currentNetworkProvider.getCurrent().chain.id.value)
                }
            }

            REQUEST_ID_SIGN_TEXT -> {
                if (data?.hasExtra("SIGNATURE") == true) {
                    val result = data.getStringExtra("SIGNATURE")
                    wcViewModel.session?.approveRequest(currentRequestId!!, "0x$result")
                } else {
                    wcViewModel.session?.rejectRequest(currentRequestId!!, 1L, "user canceled")
                }

            }

            REQUEST_ID_SIGN_TX -> {
                if (data?.hasExtra("TXHASH") == true) {
                    val result = data.getStringExtra("TXHASH")
                    wcViewModel.session?.approveRequest(currentRequestId!!, "0x$result")
                }

            }

        }

    }
}
