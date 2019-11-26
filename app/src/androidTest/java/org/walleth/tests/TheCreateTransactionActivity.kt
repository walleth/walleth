package org.walleth.tests

import android.app.Activity.RESULT_OK
import android.app.Instrumentation
import android.content.Intent
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.google.common.truth.Truth
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.kethereum.erc681.generateURL
import org.kethereum.functions.getTokenTransferTo
import org.kethereum.functions.getTokenTransferValue
import org.kethereum.functions.isTokenTransfer
import org.kethereum.model.Address
import org.ligi.trulesk.TruleskIntentRule
import org.walleth.R
import org.walleth.data.ACCOUNT_TYPE_BURNER
import org.walleth.data.addressbook.AccountKeySpec
import org.walleth.data.addressbook.AddressBookEntry
import org.walleth.data.addressbook.toJSON
import org.walleth.data.balances.Balance
import org.walleth.data.tokens.Token
import org.walleth.data.tokens.TokenTransfer
import org.walleth.data.tokens.getRootToken
import org.walleth.data.tokens.toERC681
import org.walleth.util.decimalsAsMultiplicator
import org.walleth.infrastructure.TestApp
import org.walleth.infrastructure.setCurrentToken
import org.walleth.qr.scan.QRScanActivity
import org.walleth.testdata.DEFAULT_TEST_ADDRESS2
import org.walleth.testdata.DEFAULT_TEST_ADDRESS3
import org.walleth.transactions.CreateTransactionActivity
import java.math.BigInteger

val testToken = Token(
        "Test",
        "TEST",
        Address("0x01"),
        15,
        TestApp.chainInfoProvider.getCurrent()!!.chainId,
        softDeleted = true,
        starred = false,
        fromUser = false,
        order = 1
)
val eth = TestApp.chainInfoProvider.getCurrent()!!.getRootToken()

class TheCreateTransactionActivity {

    @get:Rule
    var rule = TruleskIntentRule(CreateTransactionActivity::class.java, autoLaunch = false)

    private val testAddress = "0x1234567890123456789012345678901234567890"
    private val urlBase = "ethereum:$testAddress"

    @Before
    fun setup() {
        TestApp.testDatabase.transactions.deleteAll()
        val addressBookEntry = AddressBookEntry(TestApp.currentAddressProvider.getCurrent()!!, keySpec = AccountKeySpec(ACCOUNT_TYPE_BURNER).toJSON(), name = "testing")
        TestApp.testDatabase.addressBook.upsert(addressBookEntry)
    }

    @Test
    fun chainNameDisplayedInSubtitle() {
        val chainDefinition = TestApp.chainInfoProvider.getCurrent()
        rule.launchActivity()

        Espresso.onView(withText(rule.activity.getString(R.string.create_transaction_on_network_subtitle, chainDefinition?.name)))
                .check(matches(ViewMatchers.isDisplayed()))
        rule.screenShot("chain_name_in_subtitle")
        Truth.assertThat(rule.activity.isFinishing).isFalse()
    }

    @Test
    fun rejectsEmptyAddress() {
        rule.launchActivity()
        Espresso.onView(ViewMatchers.withId(R.id.fab)).perform(ViewActions.closeSoftKeyboard(), ViewActions.click())

        Espresso.onView(withText(R.string.create_tx_err)).check(matches(ViewMatchers.isDisplayed()))

        rule.screenShot("address_empty")
        Truth.assertThat(rule.activity.isFinishing).isFalse()
    }

    @Test
    fun rejectsUnknownChainId() {
        val chainIdForTransaction = 0
        rule.launchActivity(Intent.getIntentOld("$urlBase@" + chainIdForTransaction))

        Espresso.onView(withText(R.string.alert_network_unsupported_title)).check(matches(ViewMatchers.isDisplayed()))
        Espresso.onView(withText(rule.activity.getString(R.string.alert_network_unsupported_message, chainIdForTransaction)))
                .check(matches(ViewMatchers.isDisplayed()))

        rule.screenShot("chainId_not_valid")
        Truth.assertThat(rule.activity.isFinishing).isFalse()
    }

    @Test
    fun acceptsDifferentChainId() {
        val chainIdForTransaction = TestApp.chainInfoProvider.getCurrent()!!.chainId
        rule.launchActivity(Intent.getIntentOld("$urlBase@" + chainIdForTransaction))

        Espresso.onView(withText(R.string.alert_network_unsupported_title)).check(ViewAssertions.doesNotExist())
        Espresso.onView(withText(rule.activity.getString(R.string.alert_network_unsupported_message, chainIdForTransaction)))
                .check(ViewAssertions.doesNotExist())

        rule.screenShot("please_change_chain")
        Truth.assertThat(rule.activity.isFinishing).isFalse()
    }


    @Test
    fun showsAlertWheInvalidParameterIsUsed() {
        rule.launchActivity(Intent.getIntentOld("$urlBase/foo?yo=lo"))

        Espresso.onView(withText(rule.activity.getString(R.string.warning_invalid_param_type, "yo", "lo"))).check(matches(ViewMatchers.isDisplayed()))

    }

    @Test
    fun showsCorrectFunction() {
        rule.launchActivity(Intent.getIntentOld("$urlBase/foo"))

        Espresso.onView(ViewMatchers.withId(R.id.function_text)).check(matches(withText("foo()")))
    }

    @Test
    fun showsCorrectFunctionParameters() {
        rule.launchActivity(Intent.getIntentOld("$urlBase/otherFunction?uint256=23&uint256=5"))

        Espresso.onView(ViewMatchers.withId(R.id.function_text)).check(matches(withText("otherFunction(23,5)")))
    }

    @Test
    fun showsCorrectFunctionParametersWithNegativeValues() {
        rule.launchActivity(Intent.getIntentOld("$urlBase/otherFunction?uint256=23&int256=-5"))

        Espresso.onView(ViewMatchers.withId(R.id.function_text)).check(matches(withText("otherFunction(23,-5)")))
    }


    @Test
    fun showsWarningWhenParameterTypeIsUnsignedButValueIsSigned() {
        rule.launchActivity(Intent.getIntentOld("$urlBase/otherFunction?uint8=-23"))

        Espresso.onView(withText(rule.activity.getString(R.string.warning_invalid_parameter_value, -23, "uint8")))
                .check(matches(ViewMatchers.isDisplayed()))

    }


    @Test
    fun acceptsSimpleAddress() {
        rule.launchActivity(Intent.getIntentOld(testAddress))
        Espresso.onView(ViewMatchers.withId(R.id.to_address)).check(matches(withText(testAddress)))
    }

    @Test
    fun usesCorrectValuesForETHTransaction1() {
        setCurrentToken(eth)
        TestApp.testDatabase.balances.upsert(Balance(TestApp.currentAddressProvider.getCurrentNeverNull(), eth.address, TestApp.chainInfoProvider.getCurrent()!!.chainId, 1L, BigInteger.TEN * BigInteger("1" + "0".repeat(18))))

        rule.launchActivity(Intent.getIntentOld("$urlBase?value=1"))

        Espresso.onView(ViewMatchers.withId(R.id.fab)).perform(ViewActions.closeSoftKeyboard(), ViewActions.click())

        val allTransactionsForAddress = TestApp.testDatabase.transactions.getAllTransactionsForAddress(listOf(Address(testAddress)))
        Truth.assertThat(allTransactionsForAddress).hasSize(1)
        Truth.assertThat(allTransactionsForAddress.get(0).transaction.to?.hex).isEqualTo(testAddress)
        Truth.assertThat(allTransactionsForAddress.get(0).transaction.value).isEqualTo(BigInteger("1"))

    }

    @Test
    fun usesCorrectValuesForETHTransaction2() {
        setCurrentToken(testToken)
        TestApp.testDatabase.balances.upsert(Balance(TestApp.currentAddressProvider.getCurrentNeverNull(), eth.address, TestApp.chainInfoProvider.getCurrent()!!.chainId, 1L, BigInteger.TEN * BigInteger("1" + "0".repeat(18))))
        rule.launchActivity(Intent.getIntentOld("$urlBase?value=1"))

        Espresso.onView(ViewMatchers.withId(R.id.fab)).perform(ViewActions.closeSoftKeyboard(), click())

        val allTransactionsForAddress = TestApp.testDatabase.transactions.getAllTransactionsForAddress(listOf(Address(testAddress)))
        Truth.assertThat(allTransactionsForAddress).hasSize(1)
        Truth.assertThat(allTransactionsForAddress.get(0).transaction.to?.hex).isEqualTo(testAddress)
        Truth.assertThat(allTransactionsForAddress.get(0).transaction.value).isEqualTo(BigInteger("1"))

    }

    @Test
    fun usesCorrectValuesForCurrentTokenTransfer(): Unit = runBlocking {
        TestApp.testDatabase.tokens.addIfNotPresent(listOf(testToken))
        setCurrentToken(testToken)

        val toAddress = DEFAULT_TEST_ADDRESS2
        val uri = TokenTransfer(toAddress, testToken, BigInteger.TEN).toERC681().generateURL()

        TestApp.testDatabase.balances.upsert(Balance(TestApp.currentAddressProvider.getCurrentNeverNull(), eth.address, TestApp.chainInfoProvider.getCurrent()!!.chainId, 1L, BigInteger.TEN * BigInteger("1" + "0".repeat(18))))
        TestApp.testDatabase.balances.upsert(Balance(TestApp.currentAddressProvider.getCurrentNeverNull(), testToken.address, TestApp.chainInfoProvider.getCurrent()!!.chainId, 1L, BigInteger.TEN * BigInteger("1" + "0".repeat(18))))

        rule.launchActivity(Intent.getIntentOld(uri))
        Espresso.onView(ViewMatchers.withId(R.id.fab)).perform(ViewActions.closeSoftKeyboard(), ViewActions.click())

        val allTransactionsForAddress = TestApp.testDatabase.transactions.getAllTransactionsForAddress(listOf(toAddress))
        Truth.assertThat(allTransactionsForAddress).hasSize(0)

        val allTransactionsForToken = TestApp.testDatabase.transactions.getAllTransactionsForAddress(listOf(testToken.address))
        Truth.assertThat(allTransactionsForToken).hasSize(1)
        Truth.assertThat(allTransactionsForToken[0].transaction.isTokenTransfer()).isTrue()
        Truth.assertThat(allTransactionsForToken[0].transaction.getTokenTransferTo()).isEqualTo(toAddress)
        Truth.assertThat(allTransactionsForToken[0].transaction.getTokenTransferValue()).isEqualTo(BigInteger.TEN)
    }

    @Test
    fun usesCorrectValuesForNewTokenTransfer(): Unit = runBlocking {
        val eth = TestApp.chainInfoProvider.getCurrent()!!.getRootToken()
        setCurrentToken(eth)
        TestApp.testDatabase.tokens.addIfNotPresent(listOf(testToken))
        TestApp.testDatabase.balances.upsert(Balance(TestApp.currentAddressProvider.getCurrentNeverNull(), eth.address, TestApp.chainInfoProvider.getCurrent()!!.chainId, 1L, BigInteger.TEN * eth.decimalsAsMultiplicator().toBigInteger()))
        TestApp.testDatabase.balances.upsert(Balance(TestApp.currentAddressProvider.getCurrentNeverNull(), testToken.address, TestApp.chainInfoProvider.getCurrent()!!.chainId, 1L, BigInteger.TEN * testToken.decimalsAsMultiplicator().toBigInteger()))

        val toAddress = DEFAULT_TEST_ADDRESS2
        val uri = TokenTransfer(toAddress, testToken, BigInteger.TEN).toERC681().generateURL()


        rule.launchActivity(Intent.getIntentOld(uri))
        Espresso.onView(ViewMatchers.withId(R.id.fab)).perform(ViewActions.closeSoftKeyboard(), ViewActions.click())

        val allTransactionsForAddress = TestApp.testDatabase.transactions.getAllTransactionsForAddress(listOf(toAddress))
        Truth.assertThat(allTransactionsForAddress).hasSize(0)

        val allTransactionsForToken = TestApp.testDatabase.transactions.getAllTransactionsForAddress(listOf(testToken.address))
        Truth.assertThat(allTransactionsForToken).hasSize(1)
        Truth.assertThat(allTransactionsForToken.get(0).transaction.isTokenTransfer()).isTrue()
        Truth.assertThat(allTransactionsForToken.get(0).transaction.getTokenTransferTo()).isEqualTo(toAddress)
        Truth.assertThat(allTransactionsForToken.get(0).transaction.getTokenTransferValue()).isEqualTo(BigInteger.TEN)
    }

    @Test
    fun doesNotAcceptUnknownTokenTransfer() {
        setCurrentToken(TestApp.chainInfoProvider.getCurrent()!!.getRootToken())

        val toAddress = DEFAULT_TEST_ADDRESS2
        val uri = TokenTransfer(toAddress, testToken, BigInteger.TEN).toERC681().generateURL()

        rule.launchActivity(Intent.getIntentOld(uri))

        Espresso.onView(withText(R.string.unknown_token)).check(matches(ViewMatchers.isDisplayed()))

        rule.screenShot("unknown_token")
        Truth.assertThat(rule.activity.isFinishing).isFalse()
    }


    @Test
    fun doesNotChangeTokenOnToAddressScan() {
        runBlocking {
            setCurrentToken(testToken)
            TestApp.testDatabase.tokens.addIfNotPresent(listOf(testToken))

            val uri = TokenTransfer(DEFAULT_TEST_ADDRESS2, testToken, BigInteger.TEN).toERC681()
                    .generateURL()
            rule.launchActivity(Intent.getIntentOld(uri))

            val result = Instrumentation.ActivityResult(RESULT_OK, Intent().putExtra("SCAN_RESULT", DEFAULT_TEST_ADDRESS3.hex))
            intending(hasComponent(QRScanActivity::class.java.canonicalName)).respondWith(result)

            Espresso.onView(ViewMatchers.withId(R.id.menu_scan)).perform(click())

            Espresso.onView(withText(testToken.symbol)).check(matches(ViewMatchers.isDisplayed()))

        }
    }
}