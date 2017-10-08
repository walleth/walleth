package org.walleth.data.transactions

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import org.kethereum.model.Address
import org.kethereum.model.ChainDefinition
import java.math.BigInteger

fun TransactionDAO.getTransactionToSignWithGethLive() = getBySourceFlagLive(TransactionSource.WALLETH, false)

@Dao
interface TransactionDAO {

    @Query("SELECT * FROM transactions")
    fun getTransactions(): List<TransactionEntity>

    @Query("SELECT * FROM transactions")
    fun getTransactionsLive(): LiveData<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE source = :source AND gethSignProcessed = :gethProcessed")
    fun getBySourceFlagLive(source: TransactionSource, gethProcessed: Boolean): LiveData<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE \"to\" = :address COLLATE NOCASE AND chain=:chain ORDER BY creationEpochSecond DESC")
    fun getIncomingTransactionsForAddress(address: Address, chain: ChainDefinition): LiveData<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE \"from\" = :address COLLATE NOCASE  AND chain=:chain ORDER BY creationEpochSecond DESC")
    fun getOutgoingTransactionsForAddress(address: Address, chain: ChainDefinition): LiveData<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(transactionEntity: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(transactionEntities: List<TransactionEntity>)

    @Query("SELECT nonce from transactions WHERE \"from\" = :address COLLATE NOCASE AND chain=:chain")
    fun getNonceForAddressLive(address: Address, chain: ChainDefinition): LiveData<List<BigInteger>>

    @Query("SELECT nonce from transactions WHERE \"from\" = :address COLLATE NOCASE AND chain=:chain")
    fun getNonceForAddress(address: Address, chain: ChainDefinition): List<BigInteger>

    @Query("SELECT * from transactions")
    fun getAllToRelayLive(): LiveData<List<TransactionEntity>>

    @Query("SELECT * from transactions WHERE hash = :hash COLLATE NOCASE")
    fun getByHash(hash: String): TransactionEntity?

    @Query("SELECT * from transactions WHERE hash = :hash COLLATE NOCASE")
    fun getByHashLive(hash: String): LiveData<TransactionEntity>

    @Query("DELETE FROM transactions WHERE hash = :hash COLLATE NOCASE")
    fun deleteByHash(hash: String)

    @Query("DELETE FROM transactions")
    fun deleteAll()
}