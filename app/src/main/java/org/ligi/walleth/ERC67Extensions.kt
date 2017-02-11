package org.ligi.walleth

import org.ethereum.geth.Address

fun Address.toERC67String() = "ethereum:$hex"