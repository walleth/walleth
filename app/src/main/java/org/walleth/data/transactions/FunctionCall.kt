package org.walleth.data.transactions

import org.kethereum.model.Address

data class FunctionCall(var relevantAddress1: Address?, var relevantAddress2: Address? = null)