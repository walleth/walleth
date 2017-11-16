import com.google.common.truth.Truth.assertThat
import data.testToken
import org.junit.Test
import org.walleth.functions.decimalsInZeroes

class TheFormatting {

    @Test
    fun testWeCanParseFaucetTransactionsWithEmptyNonce() {
        assertThat(testToken.copy(decimals = 0).decimalsInZeroes()).isEqualTo("")
        assertThat(testToken.copy(decimals = 2).decimalsInZeroes()).isEqualTo("00")
        assertThat(testToken.copy(decimals = 8).decimalsInZeroes()).isEqualTo("00000000")

    }

}
