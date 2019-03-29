package data

import org.kethereum.model.Address
import org.walleth.data.tokens.Token

val testToken = Token(
        name = "test",
        symbol = "TST",
        address = Address("0x0"),
        decimals = 18,
        chain = 1L,
        showInList = true,
        starred = true,
        fromUser = true,
        order = 1
)