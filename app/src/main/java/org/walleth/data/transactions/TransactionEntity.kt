package org.walleth.data.transactions

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.kethereum.model.Address
import org.kethereum.model.SignatureData
import org.kethereum.model.Transaction

fun Transaction.toEntity(signatureData: SignatureData?, transactionState: TransactionState) =
        TransactionEntity(txHash!!, null, this, signatureData, transactionState)

fun TransactionEntity.setHash(newHash: String) {
    hash = newHash
    transaction.txHash = newHash
}

@Entity(tableName = "transactions")
data class TransactionEntity(

        @PrimaryKey
        var hash: String,

        var extraIncomingAffectedAddress: Address?,

        @Embedded
        var transaction: Transaction,

        @Embedded
        var signatureData: SignatureData?,

        @Embedded
        var transactionState: TransactionState
)