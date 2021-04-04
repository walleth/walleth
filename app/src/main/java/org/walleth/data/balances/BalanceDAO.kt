package org.walleth.data.balances

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.kethereum.model.Address
import java.math.BigInteger

fun BalanceDAO.upsertIfNewerBlock(entry: Balance) {
    val oldBalance = getBalance(entry.address, entry.tokenAddress, entry.chain)
    if (oldBalance == null || oldBalance.block < entry.block) {
        upsert(entry)
    }
}

@Dao
interface BalanceDAO {

    @Query("SELECT * FROM balances WHERE address = :address AND tokenAddress = :tokenAddress AND chain = :chain")
    fun getBalance(address: Address, tokenAddress: Address?, chain: BigInteger): Balance?

    @Query("SELECT * FROM balances WHERE address = :address AND tokenAddress = :tokenAddress AND chain = :chain")
    fun getBalanceLive(address: Address, tokenAddress: Address?, chain: BigInteger?): Flow<Balance>

    @Query("SELECT * FROM balances")
    fun getAllBalances(): List<Balance>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(entry: Balance)

    @Query("DELETE FROM balances")
    fun deleteAll()
}