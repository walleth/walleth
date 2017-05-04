
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.walleth.data.BalanceProvider
import org.walleth.data.WallethAddress
import java.math.BigInteger

class TheBalanceProvider {

    @Test
    fun unknownAddressHasNullBalance() {
        assertThat(BalanceProvider().getBalanceForAddress(WallethAddress("0x123"))).isNull()
    }

    @Test
    fun weCanSetABalance() {
        val tested = BalanceProvider()
        tested.setBalance(WallethAddress("0x124"),100L, BigInteger("5"))

        val returned = tested.getBalanceForAddress(WallethAddress("0x124"))

        assertThat(returned).isNotNull()
        assertThat(returned!!.balance).isEqualTo(BigInteger("5"))
        assertThat(returned.block).isEqualTo(100L)
    }


    @Test
    fun weCanUpdateBalance() {
        val tested = BalanceProvider()
        tested.setBalance(WallethAddress("0x124"),100L, BigInteger("5"))
        tested.setBalance(WallethAddress("0x124"),101L, BigInteger("6"))

        val returned = tested.getBalanceForAddress(WallethAddress("0x124"))

        assertThat(returned).isNotNull()
        assertThat(returned!!.balance).isEqualTo(BigInteger("6"))
        assertThat(returned.block).isEqualTo(101L)
    }

    @Test
    fun oldInfoIsRejected() { // important when data is coming from different sources e.g. ligh-client vs etherscan
        val tested = BalanceProvider()
        tested.setBalance(WallethAddress("0x124"),100L, BigInteger("5"))
        tested.setBalance(WallethAddress("0x124"),99L, BigInteger("6"))

        val returned = tested.getBalanceForAddress(WallethAddress("0x124"))

        assertThat(returned).isNotNull()
        assertThat(returned!!.balance).isEqualTo(BigInteger("5"))
        assertThat(returned.block).isEqualTo(100L)
    }

}
