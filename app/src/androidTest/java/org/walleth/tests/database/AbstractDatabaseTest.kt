package org.walleth.tests.database

import android.arch.persistence.room.Room
import android.support.test.InstrumentationRegistry
import org.junit.Before
import org.walleth.data.AppDatabase

abstract class AbstractDatabaseTest {

    lateinit var database : AppDatabase

    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getTargetContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

}