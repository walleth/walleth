package data

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.kethereum.model.Address
import org.walleth.contracts.FourByteDirectory
import org.walleth.functions.SignatureHash
import org.walleth.functions.toCleanHex
import org.walleth.functions.toFunctionCall
import org.walleth.kethereum.model.ContractFunction

class TheFunctionCall {

    companion object {
        val input = "0xa9059cbb000000000000000000000000fdf1210fc262c73d0436236a0e07be419babbbc400000000000000000000000000000000000000000000000005d423c655aa0000"
        val cleanInput = "a9059cbb000000000000000000000000fdf1210fc262c73d0436236a0e07be419babbbc400000000000000000000000000000000000000000000000005d423c655aa0000"

        val targetAddress = Address("fdf1210fc262c73d0436236a0e07be419babbbc4")

        val dir = object : FourByteDirectory {
            override fun getSignaturesFor(hex: String): List<ContractFunction> {
                if (hex == SignatureHash.tokenTransfer) {
                    return listOf(ContractFunction(SignatureHash.tokenTransfer, arguments = listOf("address", "uint256")))
                } else {
                    return emptyList()
                }
            }
        }
    }

    @Test
    fun canConvertCleanInputToFunctionCall() {
        val functionCall = cleanInput.toFunctionCall(dir)

        assertThat(functionCall).isNotNull()
        assertThat(functionCall!!.relevantAddress1).isEqualTo(targetAddress)
    }

    @Test
    fun canNotConvertInputToFunctionCall() {
        val functionCallWithoutCleaning = input.toFunctionCall(dir)

        assertThat(functionCallWithoutCleaning).isNull()
    }

    @Test
    fun canConvertInputToFunctionCallAfterCleaning() {
        val functionCall = input.toCleanHex().toFunctionCall(dir)

        assertThat(functionCall).isNotNull()
        assertThat(functionCall!!.relevantAddress1).isEqualTo(targetAddress)
    }
}