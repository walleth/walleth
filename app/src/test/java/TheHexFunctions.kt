import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.walleth.functions.fromHexToByteArray
import org.walleth.functions.toHexString

class TheHexFunctions {


    @Test
    fun weCanProduceSingleDigitHex() {
        assertThat(0.toByte().toHexString()).isEqualTo("00")
        assertThat(1.toByte().toHexString()).isEqualTo("01")
        assertThat(15.toByte().toHexString()).isEqualTo("0f")
    }

    @Test
    fun weCanProduceDoubleDigitHex() {
        assertThat(16.toByte().toHexString()).isEqualTo("10")
        assertThat(42.toByte().toHexString()).isEqualTo("2a")
        assertThat(255.toByte().toHexString()).isEqualTo("ff")
    }

    @Test
    fun prefixIsIgnored() {
        assertThat(fromHexToByteArray("0xab")).isEqualTo(fromHexToByteArray("ab"))
    }

    @Test
    fun sizesAreOk() {
        assertThat(fromHexToByteArray("ff").size).isEqualTo(1)
        assertThat(fromHexToByteArray("ffaa").size).isEqualTo(2)
        assertThat(fromHexToByteArray("ffaabb").size).isEqualTo(3)
        assertThat(fromHexToByteArray("ffaabb44").size).isEqualTo(4)
        assertThat(fromHexToByteArray("0xffaabb4455").size).isEqualTo(5)
        assertThat(fromHexToByteArray("0xffaabb445566").size).isEqualTo(6)
        assertThat(fromHexToByteArray("ffaabb44556677").size).isEqualTo(7)
    }

    @Test(expected = IllegalArgumentException::class)
    fun exceptionOnOddInput() {
        fromHexToByteArray("0xa")
    }

}
