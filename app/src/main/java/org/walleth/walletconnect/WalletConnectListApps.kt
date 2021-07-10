package org.walleth.walletconnect

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.lifecycleScope
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types.newParameterizedType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kethereum.erc1328.isERC1328
import org.kethereum.erc831.toERC831
import org.kethereum.model.EthereumURI
import org.koin.android.ext.android.inject
import org.walleth.R
import org.walleth.base_activities.BaseSubActivity
import org.walleth.data.AppDatabase
import org.walleth.data.config.KotprefSettings
import org.walleth.data.config.getComposeColors
import org.walleth.walletconnect.model.WalletConnectApp
import org.walleth.walletconnect.model.WalletConnectApps
import org.walleth.walletconnect.model.WalletConnectEnhancedApp

class WalletConnectListApps : BaseSubActivity() {

    val moshi: Moshi by inject()
    val appDatabase: AppDatabase by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val adapter: JsonAdapter<List<WalletConnectApp>> = moshi.adapter(newParameterizedType(List::class.java, WalletConnectApp::class.java))

        lifecycleScope.launch(Dispatchers.IO) {
            val list = adapter.fromJson(WalletConnectApps)!!.map {
                val networks = it.networks?.map { network ->
                    val chainId = network.toBigIntegerOrNull()
                    when {
                        chainId != null -> appDatabase.chainInfo.getByChainId(chainId)?.name ?: "chain $chainId"
                        network == "*" -> "All networks"
                        else -> "Unknown"
                    }

                }
                WalletConnectEnhancedApp(it.name, it.url, it.icon, networks?.joinToString(", "))
            }

            lifecycleScope.launch(Dispatchers.Main) {

                setContent {
                    val enterURLAlertOpen = remember { mutableStateOf(false) }
                    MaterialTheme(colors = getComposeColors(isSystemInDarkTheme())) {

                        Column {
                            TopAppBar(
                                title = { Text(text = "WalletConnect Apps", color = Color(KotprefSettings.toolbarForegroundColor)) },
                                backgroundColor = Color(KotprefSettings.toolbarBackgroundColor),
                                navigationIcon = {
                                    IconButton(
                                        onClick = {
                                            finish()
                                        }
                                    ) {
                                        Icon(
                                            Icons.Filled.ArrowBack,
                                            contentDescription = "back",
                                            tint = Color(KotprefSettings.toolbarForegroundColor)
                                        )
                                    }
                                },
                                actions = {
                                    IconButton(onClick = { enterURLAlertOpen.value = true }) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_baseline_text_fields_24),
                                            contentDescription = "enter URL",
                                            tint = Color(KotprefSettings.toolbarForegroundColor)
                                        )
                                    }
                                }
                            )
                            LazyColumn {
                                items(list) { app ->
                                    WalletConnectAppComposable(app)
                                }
                            }
                        }

                    }

                    if (enterURLAlertOpen.value) {
                        val url = remember { mutableStateOf("") }
                        val isValid = remember { mutableStateOf(false) }
                        val hasInitialValue = remember { mutableStateOf(false) }

                        val context = LocalContext.current
                        AlertDialog(
                            onDismissRequest = { enterURLAlertOpen.value = false },
                            confirmButton = {
                                if (isValid.value) {
                                    Button(onClick = {
                                        enterURLAlertOpen.value = false
                                        val wcIntent = Intent(context, WalletConnectConnectionActivity::class.java)
                                        wcIntent.data = Uri.parse(url.value)
                                        startActivity(wcIntent)
                                        finish()
                                    }) {
                                        Text("OK")
                                    }
                                }
                            },
                            dismissButton = {
                                Button(onClick = { enterURLAlertOpen.value = false }) {
                                    Text("Cancel")
                                }
                            },

                            text = {
                                Column {
                                    TextField(
                                        value = url.value,
                                        onValueChange = {
                                            hasInitialValue.value = true
                                            url.value = it
                                            val ethereumURI = EthereumURI(url.value).toERC831()
                                            isValid.value = ethereumURI.isERC1328()
                                        },
                                        label = { Text("Please enter the URL") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    if (!isValid.value && hasInitialValue.value) {
                                        Text("Not a valid WalletConnect URL ", color = Color.Red)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

    }
}
