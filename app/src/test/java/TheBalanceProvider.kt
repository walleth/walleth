
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.kethereum.model.Address
import org.walleth.data.BalanceProvider
import org.walleth.data.exchangerate.ETH_TOKEN
import java.math.BigInteger

class TheBalanceProvider {

    @Test
    fun unknownAddressHasNullBalance() {
        assertThat(BalanceProvider().getBalanceForAddress(Address("0x123"), ETH_TOKEN)).isNull()
    }

    @Test
    fun weCanSetABalance() {
        val tested = BalanceProvider()
        tested.setBalance(Address("0x124"),100L, BigInteger("5"), ETH_TOKEN)

        val returned = tested.getBalanceForAddress(Address("0x124"), ETH_TOKEN)

        assertThat(returned).isNotNull()
        assertThat(returned!!.balance).isEqualTo(BigInteger("5"))
        assertThat(returned.block).isEqualTo(100L)
    }


    @Test
    fun weCanUpdateBalance() {
        val tested = BalanceProvider()
        tested.setBalance(Address("0x124"),100L, BigInteger("5"), ETH_TOKEN)
        tested.setBalance(Address("0x124"),101L, BigInteger("6"), ETH_TOKEN)

        val returned = tested.getBalanceForAddress(Address("0x124"), ETH_TOKEN)

        assertThat(returned).isNotNull()
        assertThat(returned!!.balance).isEqualTo(BigInteger("6"))
        assertThat(returned.block).isEqualTo(101L)
    }

    @Test
    fun oldInfoIsRejected() { // important when data is coming from different sources e.g. ligh-client vs etherscan
        val tested = BalanceProvider()
        tested.setBalance(Address("0x124"),100L, BigInteger("5"), ETH_TOKEN)
        tested.setBalance(Address("0x124"),99L, BigInteger("6"), ETH_TOKEN)

        val returned = tested.getBalanceForAddress(Address("0x124"), ETH_TOKEN)

        assertThat(returned).isNotNull()
        assertThat(returned!!.balance).isEqualTo(BigInteger("5"))
        assertThat(returned.block).isEqualTo(100L)
    }

}
