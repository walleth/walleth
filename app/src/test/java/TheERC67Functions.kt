
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.ligi.walleth.data.WallethAddress
import org.ligi.walleth.iac.toERC67String
import java.math.BigDecimal
import java.math.BigInteger

class TheERC67Functions {

    @Test
    fun basicToERC67Works() {
        assertThat(WallethAddress("0x00AB42").toERC67String()).isEqualTo("ethereum:0x00AB42")
    }


    @Test
    fun ERC67WithValueWorks() {
        assertThat(WallethAddress("0x00AB42").toERC67String(valueInWei = BigInteger("1"))).isEqualTo("ethereum:0x00AB42?value=1")
        assertThat(WallethAddress("0x00AB42").toERC67String(valueInWei = BigInteger("2"))).isEqualTo("ethereum:0x00AB42?value=2")

        assertThat(WallethAddress("0x00AB42").toERC67String(valueInEther = BigDecimal("0.42"))).isEqualTo("ethereum:0x00AB42?value=420000000000000000")
    }
}
