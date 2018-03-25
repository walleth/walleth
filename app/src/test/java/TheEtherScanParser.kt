import com.google.common.truth.Truth.assertThat
import data.faucet_transactions
import org.json.JSONArray
import org.junit.Test
import org.kethereum.model.ChainDefinition
import org.walleth.contracts.FourByteDirectory
import org.walleth.core.parseEtherScanTransactions
import org.walleth.kethereum.model.ContractFunction

class TheEtherScanParser {

    @Test
    fun testWeCanParseFaucetTransactionsWithEmptyNonce() {
        // the "nonce":"" from the genesis transaction really got me by surprise ;-)
        val transactions = parseEtherScanTransactions(JSONArray(faucet_transactions), ChainDefinition(42), object : FourByteDirectory {
            override fun getSignaturesFor(hexHash: String): List<ContractFunction> {
                return listOf()
            }
        })

        assertThat(transactions.list.size).isEqualTo(23)

    }

}
