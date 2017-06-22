import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.walleth.data.WallethAddress
import org.walleth.data.transactions.*
import java.math.BigInteger

class TheTransactionFun {

    val someAddress = WallethAddress("0xfdf1210fc262c73d0436236a0e07be419babbbc4")

    @Test
    fun weCanCreateTokenTransfer() {

        assertThat(createTokenTransferTransactionInput(someAddress, BigInteger("10")).startsWith(tokenTransferSignature)).isTrue()
    }


    @Test
    fun weCanParseTokenTransferValue() {
        val createTokenTransferTransactionInput = Transaction(BigInteger.valueOf(103), someAddress, someAddress, input = createTokenTransferTransactionInput(someAddress, BigInteger("10")))
        assertThat(createTokenTransferTransactionInput.getTokenTransferValue()).isEqualTo(BigInteger("10"))
    }

    @Test
    fun weCanParseTokenTransferTo() {
        val createTokenTransferTransactionInput = Transaction(BigInteger.valueOf(103), someAddress, someAddress, input = createTokenTransferTransactionInput(someAddress, BigInteger("10")))
        assertThat(createTokenTransferTransactionInput.getTokenTransferTo()).isEqualTo(someAddress)
    }
}
