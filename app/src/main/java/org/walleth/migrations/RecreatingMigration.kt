package org.walleth.migrations

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.migration.Migration

class RecreatingMigration(startVersion: Int, endVersion: Int) : Migration(startVersion, endVersion) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DROP table `transactions`")

        database.execSQL("CREATE TABLE `transactions` (`hash` TEXT NOT NULL, `extraIncomingAffectedAddress` TEXT, `chain` INTEGER, `creationEpochSecond` INTEGER, `from` TEXT, `gasLimit` TEXT, `gasPrice` TEXT, `input` TEXT NOT NULL, `nonce` TEXT, `to` TEXT, `txHash` TEXT, `value` TEXT, `r` TEXT, `s` TEXT, `v` INTEGER, `needsSigningConfirmation` INTEGER NOT NULL, `relayed` TEXT NOT NULL, `eventLog` TEXT, `isPending` INTEGER NOT NULL, `error` TEXT, PRIMARY KEY(`hash`))")


        database.execSQL("DROP table `tokens`")

        database.execSQL("CREATE TABLE `tokens` (`name` TEXT NOT NULL, `symbol` TEXT NOT NULL, `address` TEXT NOT NULL, `decimals` INTEGER NOT NULL, `chain` INTEGER NOT NULL, `showInList` INTEGER NOT NULL, `starred` INTEGER NOT NULL, `fromUser` INTEGER NOT NULL, `order` INTEGER NOT NULL, PRIMARY KEY(`address`, `chain`))")


        database.execSQL("DROP table `balances`")

        database.execSQL("CREATE TABLE `balances` (`address` TEXT NOT NULL, `tokenAddress` TEXT NOT NULL, `chain` INTEGER NOT NULL, `block` INTEGER NOT NULL, `balance` TEXT NOT NULL, PRIMARY KEY(`address`, `chain`, `tokenAddress`))")

    }
}