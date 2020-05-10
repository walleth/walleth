package org.walleth.intents

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.kethereum.erc681.toERC681
import org.kethereum.model.EthereumURI

class TheIntentHandlerDefaults {


    @Test
    fun isFalseForPlainAddress() {
        assertThat(shouldStartTransactionActivity("ethereum:0x79b48dd0fdDd17F3f945b8507430a774b99aCC21")).isFalse()
    }

    @Test
    fun isTrueWhenHasFunction() {
        assertThat(shouldStartTransactionActivity("ethereum:0x79b48dd0fdDd17F3f945b8507430a774b99aCC21/fun")).isTrue()
    }

    @Test
    fun isTrueWhenHasValue() {
        assertThat(shouldStartTransactionActivity("ethereum:0x79b48dd0fdDd17F3f945b8507430a774b99aCC21?value=10")).isTrue()
    }

    @Test
    fun isTrueWhenHasChain() {
        assertThat(shouldStartTransactionActivity("ethereum:0x79b48dd0fdDd17F3f945b8507430a774b99aCC21@5")).isTrue()
    }

    @Test
    fun isTrueWhenNoAddress() {
        assertThat(shouldStartTransactionActivity("ethereum:")).isTrue()
    }

    private fun shouldStartTransactionActivity(url: String) = EthereumURI(url).toERC681().shouldStartTransactionActivity()

}
