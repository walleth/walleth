package org.walleth.data;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;
import org.walleth.data.addressbook.AddressBookDAO;
import org.walleth.data.addressbook.AddressBookEntry;
import org.walleth.data.balances.Balance;
import org.walleth.data.balances.BalanceDAO;
import org.walleth.data.tokens.Token;
import org.walleth.data.tokens.TokenDAO;

@Database(entities = {AddressBookEntry.class, Token.class, Balance.class}, version = 1)
@TypeConverters({RoomTypeConverters.class})
public abstract class AppDatabase extends RoomDatabase {

    public abstract AddressBookDAO getAddressBook();

    public abstract TokenDAO getTokens();

    public abstract BalanceDAO getBalances();

}