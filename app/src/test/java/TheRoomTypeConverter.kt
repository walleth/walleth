
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.kethereum.model.Address
import org.walleth.data.RoomTypeConverters
import java.math.BigInteger
import java.time.Instant
import java.util.*

class TheRoomTypeConverter {

    @Test
    fun testStringList() {
        val probe = listOf("foo", "bar")
        val serialized = RoomTypeConverters().stringListToString(probe)
        assertThat(RoomTypeConverters().stringListFromString(serialized)).isEqualTo(probe)
    }

    @Test
    fun testEmptyStringList() {
        val probe = listOf<String>()
        val serialized = RoomTypeConverters().stringListToString(probe)
        assertThat(RoomTypeConverters().stringListFromString(serialized)).isEqualTo(probe)
    }

    @Test
    fun testBigInteger() {
        listOf(BigInteger.ZERO, BigInteger.ONE, BigInteger.TEN, BigInteger.valueOf(42L)).forEach { probe ->
            val serialized = RoomTypeConverters().bigIntegerToByteArray(probe)
            assertThat(RoomTypeConverters().bigintegerFromByteArray(serialized)).isEqualTo(probe)
        }
    }

    @Test
    fun testAddress() {
        val probe = Address("0x1234567890123456789012345678901234567890")
        val serialized = RoomTypeConverters().addressToString(probe)
        assertThat(RoomTypeConverters().addressFromString(serialized)).isEqualTo(probe)
    }


    @Test
    fun testDate() {
        val probe = Date.from(Instant.parse("1984-05-23T10:42:42.00Z"))
        val serialized = RoomTypeConverters().dateToLong(probe)
        assertThat(RoomTypeConverters().dateFromLong(serialized)).isEqualTo(probe)
    }

}
