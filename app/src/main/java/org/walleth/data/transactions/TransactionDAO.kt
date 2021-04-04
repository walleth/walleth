package org.walleth.data.transactions

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.kethereum.model.Address
import java.math.BigInteger

@Dao
interface TransactionDAO {

    @Query("SELECT * FROM transactions")
    fun getTransactions(): List<TransactionEntity>

    @Query("SELECT * FROM transactions")
    fun getTransactionsLive(): LiveData<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE (\"to\" = :address COLLATE NOCASE OR \"extraIncomingAffectedAddress\" = :address COLLATE NOCASE ) AND chain=:chain ORDER BY creationEpochSecond DESC")
    fun getIncomingTransactionsForAddressOnChainOrdered(address: Address, chain: BigInteger): LiveData<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE (\"to\" = :address COLLATE NOCASE OR \"extraIncomingAffectedAddress\" = :address COLLATE NOCASE )AND chain=:chain ORDER BY creationEpochSecond DESC")
    fun getIncomingPaged(address: Address, chain: BigInteger): DataSource.Factory<Int, TransactionEntity>

    @Query("SELECT * FROM transactions WHERE \"from\" = :address COLLATE NOCASE AND chain=:chain ORDER BY nonce DESC")
    fun getOutgoingPaged(address: Address, chain: BigInteger): DataSource.Factory<Int, TransactionEntity>

    @Query("SELECT * FROM transactions WHERE \"from\" = :address COLLATE NOCASE  AND chain=:chain ORDER BY nonce DESC")
    fun getOutgoingTransactionsForAddressOnChainOrdered(address: Address, chain: BigInteger): LiveData<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE \"to\" COLLATE NOCASE IN(:addresses) OR  \"from\" COLLATE NOCASE IN(:addresses)")
    fun getAllTransactionsForAddressLive(addresses: List<Address>): LiveData<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE \"to\" COLLATE NOCASE IN(:addresses) OR  \"from\" COLLATE NOCASE IN(:addresses)")
    fun getAllTransactionsForAddress(addresses: List<Address>): List<TransactionEntity>

    @Query("SELECT EXISTS(SELECT * FROM transactions WHERE (\"to\" = :address COLLATE NOCASE OR \"extraIncomingAffectedAddress\" = :address COLLATE NOCASE )AND chain=:chain)")
    fun isTransactionForAddressOnChainExisting(address: Address, chain: BigInteger) : Boolean

    @Query("SELECT * FROM transactions")
    fun getAll(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(transactionEntity: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(transactionEntities: List<TransactionEntity>)

    @Query("SELECT nonce from transactions WHERE \"from\" = :address COLLATE NOCASE AND chain=:chain")
    fun getNonceForAddressLive(address: Address, chain: BigInteger): LiveData<List<BigInteger>>

    @Query("SELECT nonce from transactions WHERE \"from\" = :address COLLATE NOCASE AND chain=:chain")
    fun getNonceForAddress(address: Address, chain: BigInteger): List<BigInteger>

    @Query("SELECT * from transactions WHERE r IS NOT NULL AND relayed=\"\" AND isPending=1")
    fun getAllToRelayLive(): LiveData<List<TransactionEntity>>

    @Query("SELECT * from transactions WHERE isPending=1")
    suspend fun getAllPending(): List<TransactionEntity>

    @Query("SELECT * from transactions WHERE hash = :hash COLLATE NOCASE")
    fun getByHash(hash: String): TransactionEntity?

    @Query("SELECT * from transactions WHERE hash = :hash COLLATE NOCASE")
    fun getByHashLive(hash: String): LiveData<TransactionEntity>

    @Query("DELETE FROM transactions WHERE hash = :hash COLLATE NOCASE")
    fun deleteByHash(hash: String)

    @Query("DELETE FROM transactions")
    fun deleteAll()
}