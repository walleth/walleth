package data

import org.kethereum.model.Address
import org.kethereum.model.ChainDefinition
import org.walleth.data.tokens.Token

val testToken = Token(
        name = "test",
        symbol = "TST",
        address = Address("0x0"),
        decimals = 18,
        chain = ChainDefinition(1L, "TST"),
        showInList = true,
        starred = true,
        fromUser = true,
        order = 1
)