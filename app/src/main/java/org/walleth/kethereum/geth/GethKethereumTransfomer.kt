package org.walleth.kethereum.geth

import org.ethereum.geth.BigInt
import org.ethereum.geth.Geth
import org.kethereum.model.Address
import org.kethereum.model.Transaction
import org.walleth.data.networks.NetworkDefinition
import org.walleth.functions.toGethInteger
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
        sigHash = sigHash?.hex,
        gasPrice = gasPrice.toBigInteger(),
        gasLimit = BigInteger.valueOf(gas),
        signedRLP = if (sigHash.hex != null) encodeRLP().toList() else null,
        unSignedRLP = if (sigHash.hex == null) encodeRLP().toList() else null
)
