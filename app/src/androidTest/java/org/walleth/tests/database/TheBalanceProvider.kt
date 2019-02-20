package org.walleth.tests.database

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.kethereum.model.Address
import org.kethereum.model.ChainId
import org.walleth.data.balances.Balance
import org.walleth.data.balances.upsertIfNewerBlock
import org.walleth.util.findChainDefinition
import java.math.BigInteger

class TheBalanceProvider : AbstractDatabaseTest() {

    val SOME_TOKEN_ADDRESS = Address("0x124")
    val TEST_CHAIN = ChainId(4L).findChainDefinition()

    @Test
    fun unknownAddressHasNullBalance() {
        assertThat(database.balances.getBalance(Address("0x123"), null, ChainId(0).findChainDefinition())).isNull()
    }

    @Test
    fun weCanSetABalance() {
        val tested = database.balances

        tested.upsertIfNewerBlock(Balance(Address("0x124"), SOME_TOKEN_ADDRESS, TEST_CHAIN, 100L, BigInteger("5")))

        val returned = tested.getBalance(Address("0x124"), SOME_TOKEN_ADDRESS,TEST_CHAIN)

        assertThat(returned).isNotNull()
        assertThat(returned!!.balance).isEqualTo(BigInteger("5"))
        assertThat(returned.block).isEqualTo(100L)

    }


    @Test
    fun weCanUpdateBalance() {
        val tested = database.balances

        tested.upsertIfNewerBlock(Balance(Address("0x124"), SOME_TOKEN_ADDRESS, TEST_CHAIN, 100L, BigInteger("5")))
        tested.upsertIfNewerBlock(Balance(Address("0x124"), SOME_TOKEN_ADDRESS, TEST_CHAIN, 101L, BigInteger("6")))

        val returned = tested.getBalance(Address("0x124"), SOME_TOKEN_ADDRESS,TEST_CHAIN)

        assertThat(returned).isNotNull()
        assertThat(returned!!.balance).isEqualTo(BigInteger("6"))
        assertThat(returned.block).isEqualTo(101L)

    }

    @Test
    fun oldInfoIsRejected() { // important when data is coming from different sources e.g. ligh-client vs etherscan

        val tested = database.balances

        tested.upsertIfNewerBlock(Balance(Address("0x124"), SOME_TOKEN_ADDRESS, TEST_CHAIN, 100L, BigInteger("5")))
        tested.upsertIfNewerBlock(Balance(Address("0x124"), SOME_TOKEN_ADDRESS, TEST_CHAIN, 99L, BigInteger("6")))


        val returned = tested.getBalance(Address("0x124"), SOME_TOKEN_ADDRESS,TEST_CHAIN)

        assertThat(returned).isNotNull()
        assertThat(returned!!.balance).isEqualTo(BigInteger("5"))
        assertThat(returned.block).isEqualTo(100L)

    }

}
