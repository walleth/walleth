package org.walleth.tests.database

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.db.framework.FrameworkSQLiteOpenHelperFactory
import android.arch.persistence.room.Room
import android.arch.persistence.room.testing.MigrationTestHelper
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.support.test.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.walleth.data.AppDatabase
import org.walleth.data.MIGRATION_1_2
import org.walleth.data.transactions.TransactionSource


const val TEST_DB_NAME = "migration-test";

class TheAppDatabaseMigration {

    @get:Rule
    var helper: MigrationTestHelper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(),
            AppDatabase::class.java.canonicalName,
            FrameworkSQLiteOpenHelperFactory())


    @Test
    fun canMigrateFrom1To2() {
        val db = helper.createDatabase(TEST_DB_NAME, 1)!!
        insertTransaction(db)
        db.close();

        helper.runMigrationsAndValidate(TEST_DB_NAME, 2, false, MIGRATION_1_2);

        val transactions = getMigratedRoomDatabase().transactions.getTransactions()
        assertThat(transactions.size).isEqualTo(1)
        assertThat(transactions.get(0).hash).isEqualTo("ahash")
    }

    private fun insertTransaction(db: SupportSQLiteDatabase) {
        val values = ContentValues()
        values.put("hash", "ahash")
        values.put("gaslimit", "1")
        values.put("gasprice", "1")
        values.put("input", "")
        values.put("value", "0")
        values.put("needsSigningConfirmation", 1)
        values.put("source", TransactionSource.WALLETH.name)
        values.put("relayedLightClient", 1)
        values.put("relayedEtherscan", 1)
        values.put("isPending", 0)
        values.put("gethSignProcessed", 0)

        values.put("chain", "ETH:1")
        db.insert("transactions", SQLiteDatabase.CONFLICT_REPLACE, values)
    }

    private fun getMigratedRoomDatabase(): AppDatabase {
        val database = Room.databaseBuilder(InstrumentationRegistry.getTargetContext(),
                AppDatabase::class.java!!, TEST_DB_NAME)
                .addMigrations(MIGRATION_1_2)
                .build()

        helper.closeWhenFinished(database)
        return database
    }
}