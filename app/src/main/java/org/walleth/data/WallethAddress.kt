package org.walleth.data

import org.ethereum.geth.Geth
import org.kethereum.model.Address
import org.ethereum.geth.Address as GethAddress

fun Address.toGethAddr() = Geth.newAddressFromHex(hex)
fun GethAddress.toKethereumAddress() = Address(hex)

