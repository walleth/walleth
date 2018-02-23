package org.walleth.data.transactions

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.arch.persistence.room.Relation
import org.kethereum.model.Address
import org.kethereum.model.SignatureData
import org.kethereum.model.Transaction
import java.math.BigInteger

data class FunctionCall(
        var hexSignature: String,
        var functionValue: BigInteger
)

class TransactionEntityWithAddresses(
        hash: String, transaction: Transaction, signatureData: SignatureData?, transactionState: TransactionState, functionCall: FunctionCall?,
        @Relation(parentColumn = "hash", entityColumn = "hash")
        var relevantAddresses:List<RelevantAddress>
): TransactionEntity(hash, transaction, signatureData, transactionState, functionCall)

@Entity(tableName = "transactionToAddresses")
data class RelevantAddress(
        @PrimaryKey
        var hash: String,
        @PrimaryKey
        var address: Address
)
