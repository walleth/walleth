package data

import org.kethereum.model.Address
import org.walleth.data.tokens.Token
import java.math.BigInteger.ONE

val testToken = Token(
        name = "test",
        symbol = "TST",
        address = Address("0x0"),
        decimals = 18,
        chain = ONE,
        deleted = false,
        starred = true,
        fromUser = true,
        order = 1
)