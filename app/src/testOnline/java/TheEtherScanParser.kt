import com.google.common.truth.Truth.assertThat
import data.faucet_transactions
import org.json.JSONArray
import org.junit.Test
import org.kethereum.model.ChainDefinition
import org.kethereum.model.ChainId
import org.walleth.etherscan.parseEtherScanTransactions

class TheEtherScanParser {

    @Test
    fun testWeCanParseFaucetTransactionsWithEmptyNonce() {
        // the "nonce":"" from the genesis transaction really got me by surprise ;-)
        val transactions = parseEtherScanTransactions(JSONArray(faucet_transactions), ChainDefinition(ChainId(42), "TST"))

        assertThat(transactions.list.size).isEqualTo(23)

    }

}
