package org.walleth.kethereum.android

import android.os.Parcel
import android.os.Parcelable
import org.kethereum.extensions.hexToBigInteger
import org.kethereum.extensions.toHexString
import org.kethereum.model.Address
import org.kethereum.model.ChainId
import org.kethereum.model.Transaction
import org.kethereum.model.createTransactionWithDefaults
import java.math.BigInteger

class TransactionParcel(val transaction: Transaction) : Parcelable {

    constructor(parcel: Parcel) : this(createTransactionWithDefaults(
            chain = ChainId(parcel.readString()!!.hexToBigInteger()),
            value = BigInteger(parcel.readString()),
            from = Address(parcel.readString()!!),
            txHash = parcel.readValue(null) as String?,
            to = (parcel.readValue(null) as String?)?.let { Address(it) },
            nonce = (parcel.readValue(null) as String?)?.let { BigInteger(it) },
            creationEpochSecond = parcel.readValue(null) as Long?,
            gasPrice = BigInteger(parcel.readString()),
            gasLimit = BigInteger(parcel.readString()),
            input = parcel.createByteArray()!!))

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(transaction.chain!!.toHexString())
        dest.writeString(transaction.value.toString())
        dest.writeString(transaction.from?.hex)
        dest.writeValue(transaction.txHash)
        dest.writeValue(transaction.to?.hex)
        dest.writeValue(transaction.nonce?.toString())
        dest.writeValue(transaction.creationEpochSecond)
        dest.writeString(transaction.gasPrice.toString())
        dest.writeString(transaction.gasLimit.toString())
        dest.writeByteArray(transaction.input)
    }

    override fun describeContents() = 0

    companion object CREATOR : Parcelable.Creator<TransactionParcel> {
        override fun createFromParcel(parcel: Parcel) = TransactionParcel(parcel)

        override fun newArray(size: Int): Array<TransactionParcel?> = arrayOfNulls(size)
    }

}