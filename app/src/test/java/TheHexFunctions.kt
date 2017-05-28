
import com.google.common.truth.Truth.assertThat
import org.junit.Test
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
}
