package org.walleth.data

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.migration.Migration

var MIGRATION_1_TO_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE tokens " + " ADD COLUMN requiresBalance INTEGER NOT NULL DEFAULT 1")
    }
};