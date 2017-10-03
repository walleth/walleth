
import com.google.common.truth.Truth.assertThat
import data.faucet_transactions
import org.json.JSONArray
import org.junit.Test
import org.kethereum.model.ChainDefinition
import org.walleth.core.parseEtherScanTransactions

class TheEtherScanParser {

    @Test
    fun testWeCanParseFaucetTransactionsWithEmptyNonce() {
        // the "nonce":"" from the genesis transaction really got me by surprise ;-)
        val transactions = parseEtherScanTransactions(JSONArray(faucet_transactions), ChainDefinition(42))

        assertThat(transactions.size).isEqualTo(23)

    }

}
