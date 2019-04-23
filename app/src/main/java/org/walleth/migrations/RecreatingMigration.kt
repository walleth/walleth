package org.walleth.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class RecreatingMigration(startVersion: Int, endVersion: Int) : Migration(startVersion, endVersion) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DROP table `transactions`")

        database.execSQL("CREATE TABLE IF NOT EXISTS `transactions` (`hash` TEXT NOT NULL, `extraIncomingAffectedAddress` TEXT, `chain` INTEGER, `creationEpochSecond` INTEGER, `from` TEXT, `gasLimit` BLOB, `gasPrice` BLOB, `input` BLOB NOT NULL, `nonce` BLOB, `to` TEXT, `txHash` TEXT, `value` BLOB, `r` BLOB, `s` BLOB, `v` INTEGER, `needsSigningConfirmation` INTEGER NOT NULL, `relayed` TEXT NOT NULL, `eventLog` TEXT, `isPending` INTEGER NOT NULL, `error` TEXT, PRIMARY KEY(`hash`))")


        database.execSQL("DROP table `tokens`")

        database.execSQL("CREATE TABLE IF NOT EXISTS `tokens` (`name` TEXT NOT NULL, `symbol` TEXT NOT NULL, `address` TEXT NOT NULL, `decimals` INTEGER NOT NULL, `chain` INTEGER NOT NULL, `showInList` INTEGER NOT NULL, `starred` INTEGER NOT NULL, `fromUser` INTEGER NOT NULL, `order` INTEGER NOT NULL, PRIMARY KEY(`address`, `chain`))")


        database.execSQL("DROP table `balances`")

        database.execSQL("CREATE TABLE IF NOT EXISTS `balances` (`address` TEXT NOT NULL, `tokenAddress` TEXT NOT NULL, `chain` INTEGER NOT NULL, `block` INTEGER NOT NULL, `balance` BLOB NOT NULL, PRIMARY KEY(`address`, `chain`, `tokenAddress`))")

    }
}