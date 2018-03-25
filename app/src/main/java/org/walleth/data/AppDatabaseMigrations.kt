package org.walleth.data

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.migration.Migration

val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE `transactions` ADD COLUMN relevantAddress1 TEXT")
        database.execSQL("ALTER TABLE `transactions` ADD COLUMN relevantAddress2 TEXT")
    }
}
