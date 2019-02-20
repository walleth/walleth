package data

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.kethereum.model.ChainId
import org.walleth.data.RoomTypeConverters
import org.walleth.data.transactions.TransactionSource

class TheRoomTypeConverters {

    val tested = RoomTypeConverters()
    @Test
    fun weCanConvertChainDefinition() {
        assertThat(tested.fromNetworkDefinition("ETH:1").id).isEqualTo(ChainId(1L))
        assertThat(tested.fromNetworkDefinition("ETH:42").id).isEqualTo(ChainId(42L))
    }

    @Test
    fun weCanConvertTransactionSources() {
        TransactionSource.values().forEach {
            assertThat(it).isEqualTo(tested.fromTransactionSourceString(tested.toTransactionSourceString(it)))
        }
    }

}