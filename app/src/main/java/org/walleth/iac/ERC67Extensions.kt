package org.walleth.iac

import org.kethereum.model.Address
import org.walleth.data.ETH_IN_WEI
import java.math.BigDecimal
import java.math.BigInteger

// https://github.com/ethereum/EIPs/issues/67

fun Address.toERC67String() = "ethereum:$hex"
fun Address.toERC67String(valueInWei: BigInteger) = "ethereum:$hex?value=$valueInWei"
fun Address.toERC67String(valueInEther: BigDecimal) = toERC67String((valueInEther * BigDecimal(ETH_IN_WEI)).toBigInteger())

fun String.isERC67String() = startsWith("ethereum:")


class ERC67(val url: String) {

    private val colonPos = url.indexOfFirst { it == ':' }

    val scheme = url.substring(0, colonPos)

    val addressString = if (url.contains("?")) {
        url.substring(colonPos + 1, url.indexOfFirst { it == '?' })
    } else {
        url.substring(colonPos + 1)
    }

    val address by lazy { Address(getHex()) }
    fun isValid() = scheme == "ethereum"
    fun getHex() = addressString
    fun getValue() = if (url.contains("value=")) {
        url.split("value=")[1]
    } else {
        null
    }
}