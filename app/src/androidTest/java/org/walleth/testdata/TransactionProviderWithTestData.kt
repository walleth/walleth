package org.walleth.testdata

import org.kethereum.model.Address
import org.kethereum.model.ChainDefinition
import org.kethereum.model.createTransactionWithDefaults
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import org.walleth.data.transactions.TransactionDAO
import org.walleth.data.transactions.TransactionEntity
import org.walleth.data.transactions.TransactionState
import org.walleth.data.transactions.toEntity
import java.math.BigInteger


fun TransactionDAO.loadTestData(toChain: ChainDefinition) {
    deleteAll()
    upsert(kotlin.collections.listOf(
            create(value = BigInteger("420000000000000000"), from = DEFAULT_TEST_ADDRESS, to = Room77, creationEpochSecond = LocalDateTime.now().minusHours(3).toEpochSecond(), txHash = "0x0000001", chain = toChain),
            create(value = BigInteger("500000000000000000"), from = DEFAULT_TEST_ADDRESS, to = ΞBay, creationEpochSecond = LocalDateTime.now().minusHours(5).toEpochSecond(), txHash = "0x0000002", chain = toChain),
            create(value = BigInteger("5000000000000000000"), from = ShapeShift, to = DEFAULT_TEST_ADDRESS, creationEpochSecond = LocalDateTime.now().minusHours(23).toEpochSecond(), txHash = "0x0000003", chain = toChain),
            create(value = BigInteger("5000000000000000000"), from = DEFAULT_TEST_ADDRESS, to = Faundation, creationEpochSecond = LocalDateTime.now().minusHours(42).toEpochSecond(), txHash = "0x0000004", chain = toChain),
            create(value = BigInteger("1000000000000000000"), from = Faucet, to = DEFAULT_TEST_ADDRESS, creationEpochSecond = LocalDateTime.now().minusHours(64).toEpochSecond(), txHash = "0x0000005", chain = toChain),
            create(value = BigInteger("10000000000000000"), from = Ligi, to = DEFAULT_TEST_ADDRESS, creationEpochSecond = LocalDateTime.now().minusHours(96).toEpochSecond(), txHash = "0x0000006", chain = toChain),
            create(value = BigInteger("10000000000000000"), from = Ligi, to = DEFAULT_TEST_ADDRESS, creationEpochSecond = LocalDateTime.now().minusHours(128).toEpochSecond(), txHash = "0x0000007", chain = toChain),
            create(value = BigInteger("10000000000000000"), from = Faucet, to = DEFAULT_TEST_ADDRESS, creationEpochSecond = LocalDateTime.now().minusHours(150).toEpochSecond(), txHash = "0x0000008", chain = toChain),
            create(value = BigInteger("420000000000000000"), from = DEFAULT_TEST_ADDRESS, to = Room77, creationEpochSecond = LocalDateTime.now().minusHours(190).toEpochSecond(), txHash = "0x0000009", chain = toChain),
            create(value = BigInteger("230000000000000000"), from = ShapeShift, to = DEFAULT_TEST_ADDRESS, creationEpochSecond = LocalDateTime.now().minusHours(210).toEpochSecond(), txHash = "0x0000010", chain = toChain),
            create(value = BigInteger("500000000000000000"), from = Faundation, to = DEFAULT_TEST_ADDRESS, creationEpochSecond = LocalDateTime.now().minusHours(230).toEpochSecond(), txHash = "0x0000011", chain = toChain),
            create(value = BigInteger("100000000000000000"), from = DEFAULT_TEST_ADDRESS, to = Ligi, creationEpochSecond = LocalDateTime.now().minusHours(320).toEpochSecond(), txHash = "0x0000012", chain = toChain),
            create(value = BigInteger("125000000000000000"), from = DEFAULT_TEST_ADDRESS, to = Room77, creationEpochSecond = LocalDateTime.now().minusHours(360).toEpochSecond(), txHash = "0x0000013", chain = toChain)


    ))
}

private fun create(value: BigInteger, from: Address, to: Address, creationEpochSecond: Long, txHash: String, chain: ChainDefinition): TransactionEntity
        = createTransactionWithDefaults(value = value, from = from, to = to, creationEpochSecond = creationEpochSecond, txHash = txHash, chain = chain).toEntity(null, TransactionState())

private fun LocalDateTime.toEpochSecond() = atZone(ZoneId.systemDefault()).toEpochSecond()
