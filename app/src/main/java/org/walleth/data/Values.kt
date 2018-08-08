package org.walleth.data

import okhttp3.MediaType
import java.math.BigInteger

val ETH_IN_WEI = BigInteger("1000000000000000000")

var DEFAULT_GAS_PRICE = BigInteger("20000000000")
var DEFAULT_GAS_LIMIT_ETH_TX = BigInteger("21000")
var DEFAULT_GAS_LIMIT_ERC_20_TX = BigInteger("73000")

const val DEFAULT_PASSWORD = "default"

val JSON_MEDIA_TYPE = MediaType.parse("application/json")

const val DEFAULT_ETHEREUM_BIP44_PATH = "m/44'/60'/0'/0/0"