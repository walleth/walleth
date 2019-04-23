package org.walleth.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import org.walleth.data.addressbook.AddressBookDAO;
import org.walleth.data.addressbook.AddressBookEntry;
import org.walleth.data.balances.Balance;
import org.walleth.data.balances.BalanceDAO;
import org.walleth.data.tokens.Token;
import org.walleth.data.tokens.TokenDAO;
import org.walleth.data.transactions.TransactionDAO;
import org.walleth.data.transactions.TransactionEntity;

@Database(entities = {AddressBookEntry.class, Token.class, Balance.class, TransactionEntity.class}, version = 4)
@TypeConverters({RoomTypeConverters.class})
public abstract class AppDatabase extends RoomDatabase {

    public abstract AddressBookDAO getAddressBook();

    public abstract TokenDAO getTokens();

    public abstract TransactionDAO getTransactions();

    public abstract BalanceDAO getBalances();
}
