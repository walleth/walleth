import com.google.common.truth.Truth.assertThat
import data.testToken
import org.junit.Test
import org.walleth.functions.decimalsInZeroes
import org.walleth.functions.toValueString
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
    fun testWeCanRound() {
        assertThat(BigDecimal("2").toValueString(testToken.copy(decimals = 6))).isEqualTo("0.000002")
        assertThat(BigDecimal("2").toValueString(testToken.copy(decimals = 7))).isEqualTo("~0")
    }

}
