package org.walleth.data.balances

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.kethereum.model.Address
import org.kethereum.model.ChainDefinition

fun BalanceDAO.upsertIfNewerBlock(entry: Balance) {
    val oldBalance = getBalance(entry.address, entry.tokenAddress, entry.chain)
    if (oldBalance == null || oldBalance.block < entry.block) {
        upsert(entry)
    }
}

@Dao
interface BalanceDAO {

    @Query("SELECT * FROM balances WHERE address = :address AND tokenAddress = :tokenAddress AND chain = :chain")
    fun getBalance(address: Address, tokenAddress: Address?, chain: ChainDefinition): Balance?

    @Query("SELECT * FROM balances WHERE address = :address AND tokenAddress = :tokenAddress AND chain = :chain")
    fun getBalanceLive(address: Address, tokenAddress: Address?, chain: ChainDefinition): LiveData<Balance>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(entry: Balance)

    @Query("DELETE FROM balances")
    fun deleteAll()
}