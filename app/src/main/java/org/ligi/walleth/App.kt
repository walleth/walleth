package org.ligi.walleth

import android.app.Application
import android.content.Intent
import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.bind
import com.github.salomonbrys.kodein.singleton
import com.jakewharton.threetenabp.AndroidThreeTen
import org.ethereum.geth.*
import org.greenrobot.eventbus.EventBus
import org.ligi.tracedroid.TraceDroid
import org.ligi.walleth.core.EthereumService
import org.ligi.walleth.data.WallethAddress
import org.ligi.walleth.data.networks.NetworkDefinition
import org.ligi.walleth.data.networks.RinkebyNetworkDefinition
import org.ligi.walleth.data.toWallethAddress
import java.io.File

class App : Application() {

    private val keyStoreFile by lazy { File(filesDir, "keystore") }
    private val path by lazy { File(baseContext.filesDir, ".ethereum_rb").absolutePath }

    override fun onCreate() {
        super.onCreate()

        TraceDroid.init(this)
        AndroidThreeTen.init(this)

        kodein = Kodein {
            bind<EventBus>() with singleton { EventBus.getDefault() }
        }

        keyStore = KeyStore(keyStoreFile.absolutePath, Geth.LightScryptN, Geth.LightScryptP)

        ethereumNode = Geth.newNode(path, NodeConfig().apply {
            val bootnodes = Enodes()

            networḱ.bootNodes.forEach {
                bootnodes.append(Enode(it))
            }

            bootstrapNodes = bootnodes
            ethereumGenesis = networḱ.genesis
            ethereumNetworkID = 4
            ethereumNetStats = "ligi2:Respect my authoritah!@stats.rinkeby.io"
        })

        startService(Intent(this, EthereumService::class.java))
        if (keyStore.accounts.size() > 0) {
            currentAddress = keyStore.accounts[0].address.toWallethAddress()
        }
    }

    companion object {
        lateinit var keyStore: KeyStore
        lateinit var ethereumNode: Node

        lateinit var kodein: Kodein

        var currentAddress: WallethAddress? = null

        var networḱ: NetworkDefinition = RinkebyNetworkDefinition()
        var syncProgress: SyncProgress? = null
        var lastSeenBlock = 0L


    }
}

