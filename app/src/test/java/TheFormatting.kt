import com.google.common.truth.Truth.assertThat
import data.testToken
import org.junit.Test
import org.walleth.util.decimalsInZeroes
import org.walleth.util.toValueString
import java.math.BigDecimal

class TheFormatting {

    @Test
    fun testWeCanParseFaucetTransactionsWithEmptyNonce() {
        assertThat(testToken.copy(decimals = 0).decimalsInZeroes()).isEqualTo("")
        assertThat(testToken.copy(decimals = 2).decimalsInZeroes()).isEqualTo("00")
        assertThat(testToken.copy(decimals = 8).decimalsInZeroes()).isEqualTo("00000000")
    }

    @Test
    fun testWeDoNotRoundWithTrailingZeros() {
        assertThat(BigDecimal("1.0000000").toValueString(testToken.copy(decimals = 0))).isEqualTo("1")
        assertThat(BigDecimal("10.000000").toValueString(testToken.copy(decimals = 1))).isEqualTo("1")
    }

    @Test
    fun testWeRoundLongStuff() {
        assertThat(BigDecimal("1.01").toValueString(testToken.copy(decimals = 0))).isEqualTo("1.01")
        assertThat(BigDecimal("123.45678").toValueString(testToken.copy(decimals = 0))).isEqualTo("123.4567")
        assertThat(BigDecimal("1234567.8").toValueString(testToken.copy(decimals = 0))).isEqualTo("1234567")
        assertThat(BigDecimal("123456.78").toValueString(testToken.copy(decimals = 0))).isEqualTo("123456.7")
        assertThat(BigDecimal("1234567890.12").toValueString(testToken.copy(decimals = 0))).isEqualTo("1234567890")
    }


    @Test
    fun testWeCanRound() {
        assertThat(BigDecimal("2").toValueString(testToken.copy(decimals = 6))).isEqualTo("0.000002")
        assertThat(BigDecimal("2").toValueString(testToken.copy(decimals = 7))).isEqualTo("0")
    }

}
