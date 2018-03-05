package data

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.kethereum.model.Address
import org.walleth.data.transactions.Signatures
import org.walleth.data.transactions.parametersFrom
import org.walleth.kethereum.model.ContractFunction
import java.math.BigInteger

class TheContractFunction {
    @Test
    fun canConvertTokenTransfer() {
        val transfer = ContractFunction(Signatures.tokenTransfer, null, null, arguments = listOf("address", "uint256"))
        val input = "a9059cbb00000000000000000000000086fa049857e0209aa7d9e616f7eb3b3b78ecfdb00000000000000000000000000000000000000000000000000de0b6b3a7640000"
        var parameters = transfer.parametersFrom(input)
        assertThat(parameters[0]).isEqualTo(Address("86fa049857e0209aa7d9e616f7eb3b3b78ecfdb0"))
        assertThat(parameters[1]).isEqualTo(BigInteger("0000000000000000000000000000000000000000000000000de0b6b3a7640000", 16))
    }
}