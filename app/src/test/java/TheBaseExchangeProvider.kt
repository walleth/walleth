import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.walleth.data.ETH_IN_WEI
import org.walleth.data.exchangerate.BaseExchangeProvider
import org.walleth.functions.toFiatValueString
import java.math.BigDecimal
import java.math.BigInteger

class TheBaseExchangeProvider {

    @Test
    fun testExchangeStringWithTrailingZeros() {
        val baseExchangeProvider = object : BaseExchangeProvider() {
            override fun getExChangeRate(name: String) = BigDecimal("0.100")
        }

        assertThat(baseExchangeProvider.getConvertedValue(BigInteger("2").times(ETH_IN_WEI), "USD")?.toFiatValueString())
                .isEqualTo( "0.20")
        assertThat(baseExchangeProvider.getConvertedValue(BigInteger("2000").times(ETH_IN_WEI), "USD")?.toFiatValueString())
                .isEqualTo("200")
    }

    @Test
    fun testExchangeStringWithRounding() {
        val baseExchangeProvider = object : BaseExchangeProvider() {
            override fun getExChangeRate(name: String) = BigDecimal("1")
        }
        val value = BigInteger("201").times(ETH_IN_WEI).divide(BigInteger("1000")) // 0.201 ETH
        assertThat(baseExchangeProvider.getConvertedValue(value, "USD")?.toFiatValueString())
                .isEqualTo("~0.20")
    }
}
