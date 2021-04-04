package org.walleth.enhancedlist

interface EnhancedListInterface<T> {
    suspend fun undeleteAll()
    suspend fun deleteAllSoftDeleted()

    suspend fun getAll(): List<T>
    fun compare(t1: T, t2: T): Boolean
    suspend fun upsert(item: T)
    suspend fun filter(item: T) : Boolean
}