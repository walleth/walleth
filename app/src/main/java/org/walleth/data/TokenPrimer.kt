package org.walleth.data

import android.content.res.AssetManager
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import org.json.JSONArray
import org.json.JSONObject
import org.kethereum.model.Address
import org.ligi.tracedroid.logging.Log
import org.walleth.data.config.Settings
import org.walleth.data.networks.AllNetworkDefinitions
import org.walleth.data.tokens.Token
import org.walleth.data.tokens.getEthTokenForChain

private const val TOKEN_INIT_VERSION = 7
// yes this is opinionated - but it also cuts to the chase
// so much garbage in this token-list ..

fun mapToOrder(input: String) = when (input.toLowerCase()) {
    "0xd26114cd6EE289AccF82350c8d8487fedB8A0C07".toLowerCase() -> 230 // OMG
    "0x89205A3A3b2A69De6Dbf7f01ED13B2108B2c43e7".toLowerCase() -> 420 // Unicorn
    "0xE41d2489571d322189246DaFA5ebDe1F4699F498".toLowerCase() -> 50  // 0x
    "0x744d70FDBE2Ba4CF95131626614a1763DF805B9E".toLowerCase() -> 30  // SNT
    "0x6810e776880C02933D47DB1b9fc05908e5386b96".toLowerCase() -> 20 // Gnossis
    "0xBB9bc244D798123fDe783fCc1C72d3Bb8C189413".toLowerCase() -> 10 // DAO
    else -> 0
}

fun initTokens(settings: Settings, assets: AssetManager, appDatabase: AppDatabase) {
    if (settings.tokensInitVersion < TOKEN_INIT_VERSION) {
        settings.tokensInitVersion = TOKEN_INIT_VERSION

        async(CommonPool) {
            AllNetworkDefinitions.forEach {
                try {
                    val chain = it.chain
                    val open = assets.open("token_init/${chain.id}.json")
                    val jsonArray = JSONArray(open.use { it.reader().readText() })

                    val newTokens = (0 until jsonArray.length()).map { jsonArray.get(it) as JSONObject }.map {
                        val address = it.getString("address")
                        Token(
                                symbol = it.getString("symbol"),
                                name = it.getString("name"),
                                decimals = Integer.parseInt(it.getString("decimals")),
                                address = Address(address),
                                starred = false,
                                showInList = true,
                                fromUser = false,
                                chain = chain,
                                order = mapToOrder(address)
                        )


                    }
                    appDatabase.tokens.upsert(newTokens)
                    appDatabase.tokens.upsert(getEthTokenForChain(it).copy(order = 8888))
                } catch (ioe: Exception) {
                    Log.e("Could not load Token " + ioe)
                }

            }

        }
    }
}