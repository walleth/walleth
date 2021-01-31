
import com.google.common.truth.Truth.assertThat
import data.faucet_transactions
import org.json.JSONArray
import org.junit.Test
import org.walleth.dataprovider.parseEtherscanTransactionList

class TheBlockScoutParser {

    @Test
    fun testWeCanParseFaucetTransactionsWithEmptyNonce() {
        // the "nonce":"" from the genesis transaction really got me by surprise ;-)
        val transactions = parseEtherscanTransactionList(JSONArray(faucet_transactions))

        assertThat(transactions.list.size).isEqualTo(23)

    }

}
