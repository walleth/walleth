
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.ligi.walleth.data.WallethAddress
import org.ligi.walleth.iac.ERC67
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

    @Test
    fun invalidERC67IsDetected() {
        assertThat(ERC67("etheruum:0x00AB42?value=1").isValid()).isEqualTo(false)
    }

    @Test
    fun ERC67ParsingWorks() {
        assertThat(ERC67("ethereum:0x00AB42?value=1").getHex()).isEqualTo("0x00AB42")

        assertThat(ERC67("ethereum:0x00AB42?value=1").isValid()).isEqualTo(true)
        assertThat(ERC67("ethereum:0x00AB42?value=1").getValue()).isEqualTo("1")

        (0..10).forEach {
            val probe = ERC67(WallethAddress("0xAABB").toERC67String(valueInWei = BigInteger.valueOf(it.toLong())))
            assertThat(probe.getValue()).isEqualTo(it.toString())
        }
    }
}
