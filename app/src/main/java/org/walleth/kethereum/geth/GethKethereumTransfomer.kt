package org.walleth.kethereum.geth

import org.ethereum.geth.BigInt
import org.ethereum.geth.Geth
import org.json.JSONObject
import org.kethereum.model.Address
import org.kethereum.model.SignatureData
import org.kethereum.model.Transaction
import org.walleth.data.networks.NetworkDefinition
import org.walleth.functions.toGethInteger
import org.walleth.khex.hexToByteArray
import java.math.BigInteger
import org.ethereum.geth.Address as GethAddress

fun BigInt.toBigInteger() = BigInteger(bytes)
fun Address.toGethAddr() = Geth.newAddressFromHex(hex)
fun GethAddress.toKethereumAddress() = Address(hex)

fun Transaction.toGethTransaction(): org.ethereum.geth.Transaction = Geth.newTransaction(nonce!!.toLong(),
        to!!.toGethAddr(),
        BigInt(value.toLong()),
        gasLimit.toGethInteger(),
        gasPrice.toGethInteger(),
        input.toByteArray()
)

fun org.ethereum.geth.Transaction.toKetherumTransaction(networkDefinitionProvider: NetworkDefinition) = Transaction(
        to = to.toKethereumAddress(),
        from = getFrom(Geth.newBigInt(networkDefinitionProvider.chainId)).toKethereumAddress(),
        value = value.toBigInteger(),
        nonce = BigInteger.valueOf(nonce),
        txHash = hash.hex,
        gasPrice = gasPrice.toBigInteger(),
        gasLimit = BigInteger.valueOf(gas),
        signatureData = extractSignatureData()
)


fun String.hexToBigInteger() = BigInteger(replace("0x", ""), 16)

fun org.ethereum.geth.Transaction.extractSignatureData(): SignatureData? {
    val jsonObject = JSONObject(encodeJSON())

    return if (jsonObject.getString("r") == "0x0" && jsonObject.getString("s") == "0x0")
        null
    else
        jsonObject.let {
            SignatureData(
                    r = it.getString("r").hexToBigInteger(),
                    s = it.getString("s").hexToBigInteger(),
                    v = it.getString("v").hexToByteArray().first()
            )
        }
}
