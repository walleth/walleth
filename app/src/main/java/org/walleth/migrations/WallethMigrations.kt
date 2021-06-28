package org.walleth.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class EIP1559Migration : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE transactions ADD COLUMN maxPriorityFeePerGas BLOB")
        database.execSQL("ALTER TABLE transactions ADD COLUMN maxFeePerGas BLOB")
        database.execSQL("ALTER TABLE chains ADD COLUMN useEIP1559 INTEGER NOT NULL DEFAULT(0)")
    }
}

class TransactionExtendingMigration : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE transactions ADD COLUMN blockNumber BLOB")
        database.execSQL("ALTER TABLE transactions ADD COLUMN blockHash TEXT")
    }
}

class ChainAddingAndRecreatingMigration(startVersion: Int) : RecreatingMigration(startVersion) {
    override fun migrate(database: SupportSQLiteDatabase) {
        super.migrate(database)

        database.execSQL("CREATE TABLE IF NOT EXISTS `chains` (`name` TEXT NOT NULL, `chainId` BLOB NOT NULL, `networkId` INTEGER NOT NULL, `shortName` TEXT NOT NULL, `rpc` TEXT NOT NULL, `faucets` TEXT NOT NULL, `infoURL` TEXT NOT NULL, `order` INTEGER, `starred` INTEGER NOT NULL, `softDeleted` INTEGER NOT NULL, `token_decimals` INTEGER NOT NULL, `token_name` TEXT NOT NULL, `token_symbol` TEXT NOT NULL, PRIMARY KEY(`chainId`))")
    }
}

internal fun SupportSQLiteDatabase.recreateTable(name: String, query: String) {
    execSQL("DROP table `$name`")
    execSQL(query.replace("\${TABLE_NAME}", name))
}

open class RecreatingMigration(startVersion: Int) : Migration(startVersion, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.recreateTable("transactions", "CREATE TABLE IF NOT EXISTS `\${TABLE_NAME}` (`hash` TEXT NOT NULL, `extraIncomingAffectedAddress` TEXT, `chain` BLOB, `creationEpochSecond` INTEGER, `from` TEXT, `gasLimit` BLOB, `gasPrice` BLOB, `input` BLOB NOT NULL, `nonce` BLOB, `to` TEXT, `txHash` TEXT, `value` BLOB, `r` BLOB, `s` BLOB, `v` BLOB, `needsSigningConfirmation` INTEGER NOT NULL, `relayed` TEXT NOT NULL, `eventLog` TEXT, `isPending` INTEGER NOT NULL, `error` TEXT, PRIMARY KEY(`hash`))")

        database.recreateTable("tokens", "CREATE TABLE IF NOT EXISTS `\${TABLE_NAME}` (`name` TEXT NOT NULL, `symbol` TEXT NOT NULL, `address` TEXT NOT NULL, `decimals` INTEGER NOT NULL, `chain` BLOB NOT NULL, `softDeleted` INTEGER NOT NULL, `starred` INTEGER NOT NULL, `fromUser` INTEGER NOT NULL, `order` INTEGER NOT NULL, PRIMARY KEY(`address`, `chain`))")

        database.recreateTable("balances","CREATE TABLE IF NOT EXISTS `\${TABLE_NAME}` (`address` TEXT NOT NULL, `tokenAddress` TEXT NOT NULL, `chain` BLOB NOT NULL, `block` INTEGER NOT NULL, `balance` BLOB NOT NULL, PRIMARY KEY(`address`, `chain`, `tokenAddress`))")

    }
}