package com.example.expensetracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.example.expensetracker.HelperClasses.FileUtils;
import com.example.expensetracker.RecyclerViewAdapters.DateGridAdapter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";
    private final Context context;

    // Database info
    private static final int DATABASE_VERSION = 1; // Database Version
    private static final String DATABASE_NAME = "ExpenseTracker.db"; // Database Name

    // Table names
    public static final String TABLE_EXPENSE = "expenses";
    public static final String TABLE_CATEGORY = "categories";
    public static final String TABLE_ACCOUNT = "accounts";
    public static final String TABLE_CURRENCY = "currencies";

    // Common column names
    private static final String KEY_ID = "id";

    // EXPENSES Table - column names
    private static final String KEY_AMOUNT = "amount";
    private static final String KEY_DESC = "description";
    private static final String KEY_CAT_ID = "category_id";
    private static final String KEY_ACC_ID = "account_id";
    private static final String KEY_DATETIME = "datetime";

    // CATEGORIES & ACCOUNTS tables - column names
    private static final String KEY_NAME = "name";
    private static final String KEY_ICON = "icon";
    private static final String KEY_COLOR = "color";
    private static final String KEY_POSITION = "position";
    private static final String KEY_CURRENCY = "currency";
    private static final String KEY_XRATE = "exchange_rate";

    // EXPENSES table create statement
    private static final String CREATE_TABLE_EXPENSE =
            "CREATE TABLE " + TABLE_EXPENSE + "("
                    + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + KEY_AMOUNT + " REAL NOT NULL,"
                    + KEY_DESC + " TEXT NOT NULL,"
                    + KEY_ACC_ID + " INTEGER NOT NULL,"
                    + KEY_CAT_ID + " INTEGER NOT NULL,"
                    + KEY_DATETIME + " TEXT NOT NULL"
                    + ")";

    // CATEGORIES table create statement
    private static final String CREATE_TABLE_CATEGORY =
            "CREATE TABLE " + TABLE_CATEGORY + "("
                    + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + KEY_NAME + " TEXT UNIQUE,"
                    + KEY_ICON + " TEXT NOT NULL,"
                    + KEY_COLOR + " TEXT NOT NULL,"
                    + KEY_POSITION + " INTEGER NOT NULL"
                    + ")";

    // ACCOUNTS table create statement
    private static final String CREATE_TABLE_ACCOUNT =
            "CREATE TABLE " + TABLE_ACCOUNT + "("
                    + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + KEY_NAME + " TEXT UNIQUE,"
                    + KEY_ICON + " TEXT NOT NULL,"
                    + KEY_COLOR + " TEXT NOT NULL,"
                    + KEY_POSITION + " INTEGER NOT NULL,"
                    + KEY_CURRENCY + " TEXT NOT NULL"
                    + ")";

    // CURRENCY table create statement
    private static final String CREATE_TABLE_CURRENCY =
            "CREATE TABLE " + TABLE_CURRENCY + "("
                    + KEY_NAME + " TEXT PRIMARY KEY,"
                    + KEY_DESC + " TEXT NOT NULL,"
                    + KEY_CURRENCY + " TEXT NOT NULL,"
                    + KEY_XRATE + " REAL NOT NULL"
                    + ")";

    /**
     * Constructor
     */
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    /**
     * Default methods
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        // creating required tables
        db.execSQL(CREATE_TABLE_EXPENSE);
        db.execSQL(CREATE_TABLE_CATEGORY);
        db.execSQL(CREATE_TABLE_ACCOUNT);
        db.execSQL(CREATE_TABLE_CURRENCY);
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CURRENCY);
        db.execSQL(CREATE_TABLE_CURRENCY);
    }
    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion > DATABASE_VERSION) {
            db.setVersion(DATABASE_VERSION);
        }
    }

    /**
     * CRUD Operations - Create
     */
    public void createExpense(Expense expense) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = createExpenseValues(expense);
        long res = db.insert(TABLE_EXPENSE, null, values);
        String toast = (res == -1) ? "Error: Failed to create expense" : "Expense created";
        Toast.makeText(context, toast, Toast.LENGTH_SHORT).show();
    }
    public void createAccount(Account account, boolean notify) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = createSectionValues(account);
        values.put(KEY_CURRENCY, account.getCurrencyName());
        long res = db.insert(TABLE_ACCOUNT, null, values);
        if (notify) {
            String toast = (res == -1) ? "Error: Failed to create account " : "Account created: ";
            Toast.makeText(context, toast + account.getName(), Toast.LENGTH_SHORT).show();
        }
    }
    public void createCategory(Category category, boolean notify) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = createSectionValues(category);
        long res = db.insert(TABLE_CATEGORY, null, values);
        if (notify) {
            String toast = (res == -1) ? "Error: Failed to create category " : "Category created: ";
            Toast.makeText(context, toast + category.getName(), Toast.LENGTH_SHORT).show();
        }
    }
    public void createCurrency(Currency currency) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_NAME, currency.getName());
        values.put(KEY_DESC, currency.getDescription());
        values.put(KEY_CURRENCY, currency.getSymbol());
        values.put(KEY_XRATE, currency.getRate());
        db.insert(TABLE_CURRENCY, null, values);
    }

    /**
     * Read - Getters & Setters
     */
    // Expenses
    public Expense getExpense(int id) {
        Expense exp = new Expense();
        Cursor c = getCursorFromQuery(
                getQuerySelectId(TABLE_EXPENSE, KEY_ID, id),
                "Expense ID: " + id + " not found");
        if (c.moveToFirst()) exp = getExpenseFromCursor(c);
        c.close();
        return exp;
    }
    public ArrayList<Expense> getSortedAllExpenses(int direction) {
        ArrayList<Expense> expenses = new ArrayList<>();
        String KEY_DIRECTION = (direction == Constants.ASCENDING) ? "ASC" : "DESC";
        Cursor c = getCursorFromQueryOrNull(
                "SELECT * FROM " + TABLE_EXPENSE + " ORDER BY " + KEY_DATETIME + " " + KEY_DIRECTION,
                "");
        if (c == null)
            return expenses;
        while (c.moveToNext())
            expenses.add(getExpenseFromCursor(c));
        c.close();
        return expenses;
    }
    public ArrayList<Expense> getSortedExpensesByDateRange(Calendar from, Calendar to, int direction) {
        ArrayList<Expense> expenses = new ArrayList<>();
        String fromStr = MainActivity.getDatetimeStr(from, Expense.DATETIME_FORMAT);
        String toStr = MainActivity.getDatetimeStr(to, Expense.DATETIME_FORMAT);
        String KEY_DIRECTION = (direction == Constants.ASCENDING) ? "ASC" : "DESC";
        Cursor c = getCursorFromQueryOrNull(
                "SELECT * FROM " + TABLE_EXPENSE +
                        " WHERE " + KEY_DATETIME + " BETWEEN '" + fromStr + "' AND '" + toStr +
                        "' ORDER BY " + KEY_DATETIME + " " + KEY_DIRECTION,
                "");
        if (c == null)
            return expenses;
        while (c.moveToNext())
            expenses.add(getExpenseFromCursor(c));
        c.close();
        return expenses;
    }
    public ArrayList<Expense> getSortedFilteredExpenses(ArrayList<Account> accs, ArrayList<Category> cats, int direction) {
        if (accs.isEmpty() && cats.isEmpty()) // no filters
            return getSortedAllExpenses(direction);

        ArrayList<Expense> expenses = new ArrayList<>();
        String KEY_DIRECTION = (direction == Constants.ASCENDING) ? "ASC" : "DESC";
        String query = getQueryFromFilters("*", accs, cats, KEY_DATETIME + " " + KEY_DIRECTION);
        Cursor c = getCursorFromQueryOrNull(query, "");
        if (c == null)
            return expenses;
        while (c.moveToNext())
            expenses.add(getExpenseFromCursor(c));
        c.close();
        return expenses;
    }
    public ArrayList<Expense> getSortedFilteredExpensesInDateRange(ArrayList<Account> accs, ArrayList<Category> cats, Calendar from, Calendar to, int direction) {
        if (accs.isEmpty() && cats.isEmpty()) // no filters
            return getSortedExpensesByDateRange(from, to, direction);

        ArrayList<Expense> expenses = new ArrayList<>();
        String KEY_DIRECTION = (direction == Constants.ASCENDING) ? "ASC" : "DESC";
        String query = getQueryFromFiltersInDateRange("*", accs, cats, from, to, KEY_DATETIME + " " + KEY_DIRECTION);
        Cursor c = getCursorFromQueryOrNull(query, "");
        if (c == null)
            return expenses;
        while (c.moveToNext())
            expenses.add(getExpenseFromCursor(c));
        c.close();
        return expenses;
    }
    public String getQueryFromFilters(String select, ArrayList<Account> accs, ArrayList<Category> cats, String order) {
        return getQueryFromFiltersInDateRange(select, accs, cats, null, null, order);
    }
    public String getQueryFromFiltersInDateRange(String select, ArrayList<Account> accs, ArrayList<Category> cats, Calendar from, Calendar to, String order) {

        // convert filter sections to string
        StringBuilder accListStr = new StringBuilder();
        StringBuilder catListStr = new StringBuilder();
        for (int i = 0;i < accs.size();i++) {
            if (accListStr.length() != 0)
                accListStr.append(",");
            accListStr.append(accs.get(i).getId());
        }
        for (int i = 0;i < cats.size();i++) {
            if (catListStr.length() != 0)
                catListStr.append(",");
            catListStr.append(cats.get(i).getId());
        }

        String query = "SELECT " + select + " FROM " + TABLE_EXPENSE;
        if (accs.isEmpty() && !cats.isEmpty()) // cat filters
            query += " WHERE " + KEY_CAT_ID + " IN (" + catListStr + ")";
        else if (cats.isEmpty()) // acc filters
            query += " WHERE " + KEY_ACC_ID + " IN (" + accListStr + ")";
        else // both filters
            query += " WHERE " + KEY_ACC_ID + " IN (" + accListStr + ") AND "
                    + KEY_CAT_ID + " IN (" + catListStr + ")";

        // get datetime strings for querying
        if (from != null) {
            String fromStr = MainActivity.getDatetimeStr(from, Expense.DATETIME_FORMAT);
            String toStr = MainActivity.getDatetimeStr(to, Expense.DATETIME_FORMAT);
            query += " AND " + KEY_DATETIME + " BETWEEN '" + fromStr + "' AND '" + toStr + "'";
        }

        // order by
        if (!order.isEmpty())
            query += " ORDER BY " + order;

        return query;
    }

    // Sections
    public ArrayList<Account> getAllAccounts() {
        ArrayList<Account> accounts = new ArrayList<>();
        Cursor c = getCursorFromQueryOrNull(getQuerySelectAll(TABLE_ACCOUNT), "");
        if (c == null)
            return accounts;
        while (c.moveToNext())
            accounts.add(getAccountFromCursor(c));
        c.close();
        return accounts;
    }
    public ArrayList<Category> getAllCategories() {
        ArrayList<Category> categories = new ArrayList<>();
        Cursor c = getCursorFromQueryOrNull(getQuerySelectAll(TABLE_CATEGORY), "");
        if (c == null)
            return categories;
        while (c.moveToNext())
            categories.add(getCategoryFromCursor(c));
        c.close();
        return categories;
    }
    public Account getAccount(String name, boolean notify) {
        Account account = new Account(context);
        Cursor c = getCursorFromQuery(
                getQuerySelectName(TABLE_ACCOUNT, KEY_NAME, name),
                (notify) ? "Account " + name + " not found. Check name again" : "");
        if (c.moveToFirst()) account = getAccountFromCursor(c);
        c.close();
        return account;
    }
    public Account getAccount(String name) {
        return getAccount(name,true);
    }
    public Account getAccount(int id) {
        Account account = new Account(context);
        Cursor c = getCursorFromQuery(getQuerySelectId(
                TABLE_ACCOUNT, KEY_ID, id),
                "Account ID: " + id + " not found");
        if (c.moveToFirst()) account = getAccountFromCursor(c);
        c.close();
        return account;
    }
    public Category getCategory(String name, boolean notify) {
        Category category = new Category(context);
        Cursor c = getCursorFromQuery(
                getQuerySelectName(TABLE_CATEGORY, KEY_NAME, name),
                (notify) ? "Category " + name + " not found. Check name again" : "");
        if (c.moveToFirst()) category = getCategoryFromCursor(c);
        c.close();
        return category;
    }
    public Category getCategory(String name) {
        return getCategory(name,true);
    }
    public Category getCategory(int id) {
        Category category = new Category(context);
        Cursor c = getCursorFromQuery(
                getQuerySelectId(TABLE_CATEGORY, KEY_ID, id),
                "Category ID: " + id + " not found");
        if (c.moveToFirst()) category = getCategoryFromCursor(c);
        c.close();
        return category;
    }

    // Currencies
    public Currency getCurrency(String name) {
        Currency currency = new Currency(context);
        Cursor c = getCursorFromQuery(
                getQuerySelectName(TABLE_CURRENCY, KEY_NAME, name),"");
        if (c.moveToFirst()) currency = getCurrencyFromCursor(c);
        c.close();
        return currency;
    }
    public ArrayList<Currency> getAllCurrencies() {
        ArrayList<Currency> currencies = new ArrayList<>();
        Cursor c = getCursorFromQueryOrNull(getQuerySelectAll(TABLE_CURRENCY), "");
        if (c == null)
            return currencies;
        while (c.moveToNext()) {
            String name = c.getString(c.getColumnIndexOrThrow(KEY_NAME));
            String desc = c.getString(c.getColumnIndexOrThrow(KEY_DESC));
            String symbol = c.getString(c.getColumnIndexOrThrow(KEY_CURRENCY));
            float xrate = c.getFloat(c.getColumnIndexOrThrow(KEY_XRATE));
            currencies.add(new Currency(name, desc, xrate, symbol));
        }
        c.close();
        return currencies;
    }

    /**
     * Update
     */
    public void updateExpense(Expense expense) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = createExpenseValues(expense);
        db.update(TABLE_EXPENSE, values, KEY_ID + " = ?",
                new String[] { String.valueOf(expense.getId()) });
    }
    public void updateAccount(Account account) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = createSectionValues(account);
        values.put(KEY_CURRENCY, account.getCurrencyName());
        db.update(TABLE_ACCOUNT, values, KEY_ID + " = ?",
                new String[] { String.valueOf(account.getId()) });
    }
    public void updateCategory(Category category) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = createSectionValues(category);
        db.update(TABLE_CATEGORY, values, KEY_ID + " = ?",
                new String[] { String.valueOf(category.getId()) });
    }
    public void updateCurrency(Currency currency) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = createCurrencyValues(currency);
        db.update(TABLE_CURRENCY, values, KEY_NAME + " = ?",
                new String[] { currency.getName() });
    }
    public int moveExpenses(Section section, String key_id, String section_name) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor c = getCursorFromQueryOrNull(getQuerySelectId(TABLE_EXPENSE, key_id, section.getId()), "");
        int res = 1;
        if (c == null)
            return res;
        while (c.moveToNext()) {
            int id = c.getInt(c.getColumnIndexOrThrow(KEY_ID));
            ContentValues values = new ContentValues();
            if (key_id.equals(KEY_ACC_ID))
                values.put(key_id, getAccount(section_name).getId());
            else if (key_id.equals(KEY_CAT_ID))
                values.put(key_id, getCategory(section_name).getId());
            res = db.update(TABLE_EXPENSE, values, KEY_ID + " = ?",
                    new String[] { String.valueOf(id) });
        }
        c.close();
        return res;
    }
    public int moveExpensesToDefault(Account acc) {
        return moveExpenses(acc, KEY_ACC_ID, ((MainActivity) context).getDefaultAccName());
    }
    public int moveExpensesToImmutable(Category cat) {
        return moveExpenses(cat, KEY_CAT_ID, ((MainActivity) context).getImmutableCat());
    }

    /**
     * Delete
     */
    public void deleteExpense(Expense expense) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_EXPENSE + " WHERE "  + KEY_ID + " = " + expense.getId());
    }
    public <T extends Section> int deleteSection(String table_name, T section) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(table_name, KEY_ID + " = ?",new String[] { String.valueOf(section.getId()) });
    }
    public void deleteAccount(Account acc, boolean notify) {
        if (acc.getName().equals(((MainActivity) context).getDefaultAccName())) {
            Toast.makeText(context, "Cannot delete account " + acc.getName(), Toast.LENGTH_SHORT).show();
            return;
        }
        int moveRes = moveExpensesToDefault(acc);
        if (moveRes == 0)
            return;
        int res = deleteSection(TABLE_ACCOUNT, acc);
        if (res == 0)
            Toast.makeText(context, "Error: Failed to delete account " + acc.getName(), Toast.LENGTH_SHORT).show();
        else if (notify)
            Toast.makeText(context, "Account deleted: " + acc.getName(), Toast.LENGTH_SHORT).show();
    }
    public void deleteCategory(Category cat, boolean notify) {
        if (cat.getName().equals(((MainActivity) context).getImmutableCat())) {
            Toast.makeText(context, "Cannot delete category " + cat.getName(), Toast.LENGTH_SHORT).show();
            return;
        }
        int moveRes = moveExpensesToImmutable(cat);
        if (moveRes == 0)
            return;
        int res = deleteSection(TABLE_CATEGORY, cat);
        if (res == 0)
            Toast.makeText(context, "Error: Failed to delete category " + cat.getName(), Toast.LENGTH_SHORT).show();
        else if (notify)
            Toast.makeText(context, "Category deleted: " + cat.getName(), Toast.LENGTH_SHORT).show();
    }
    public void deleteAllSections(String table_name) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + table_name);
        db.execSQL("DELETE FROM SQLITE_SEQUENCE WHERE name='" + table_name + "'");
    }
    public void deleteAllAccounts() {
        ArrayList<Account> accounts = getAllAccounts();
        for (Account acc : accounts) {
            if (!Arrays.asList(Constants.defaultAccNames).contains(acc.getName()))
                moveExpensesToDefault(acc);
        }
        deleteAllSections(TABLE_ACCOUNT);
    }
    public void deleteAllCategories() {
        ArrayList<Category> categories = getAllCategories();
        for (Category cat : categories) {
            if (!Arrays.asList(Constants.defaultCatNames).contains(cat.getName()))
                moveExpensesToImmutable(cat);
        }
        deleteAllSections(TABLE_CATEGORY);
    }

    /**
     * Helper functions
     */
    public ContentValues createCurrencyValues(Currency currency) {
        ContentValues values = new ContentValues();
        values.put(KEY_NAME, currency.getName());
        values.put(KEY_DESC, currency.getDescription());
        values.put(KEY_CURRENCY, currency.getSymbol());
        values.put(KEY_XRATE, currency.getRate());
        return values;
    }
    public ContentValues createExpenseValues(Expense expense) {
        ContentValues values = new ContentValues();
        values.put(KEY_AMOUNT, expense.getAmount());
        values.put(KEY_DESC, expense.getDescription());
        values.put(KEY_ACC_ID, expense.getAccount().getId());
        values.put(KEY_CAT_ID, expense.getCategory().getId());
        values.put(KEY_DATETIME, expense.getDatetimeStr());
        return values;
    }
    public ContentValues createSectionValues(Section section) {
        ContentValues values = new ContentValues();
        if (section.getId() != -1)
            values.put(KEY_ID, section.getId());
        values.put(KEY_NAME, section.getName());
        values.put(KEY_ICON, section.getIconName());
        values.put(KEY_COLOR, section.getColorName());
        values.put(KEY_POSITION, section.getPosition());
        return values;
    }
    public boolean empty(String table_name) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + table_name;
        Cursor c = db.rawQuery(query, null);
        boolean res = (c.getCount() == 0);
        c.close();
        return res;
    }
    public String getQuerySelectAll(String table) {
        return "SELECT * FROM " + table;
    }
    public String getQuerySelectName(String table, String key, String value) {
        return "SELECT * FROM " + table + " WHERE " + key + " = '" + value + "'";
    }
    public String getQuerySelectId(String table, String key, int value) {
        return "SELECT * FROM " + table + " WHERE " + key + " = " + value;
    }
    public Cursor getCursorFromQueryOrNull(String query, String toast) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(query, null);
        if (c.getCount() != 0)
            return c;
        if (!toast.isEmpty())
            Toast.makeText(context, toast, Toast.LENGTH_SHORT).show();
        return null;
    }
    public Cursor getCursorFromQuery(String query, String toast) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(query, null);
        if (c.getCount() ==0 && !toast.isEmpty())
            Toast.makeText(context, toast, Toast.LENGTH_SHORT).show();
        return c;
    }
    public String getDefaultSectionName(String table_name, int pos) {
        String name = "";
        Cursor c = getCursorFromQuery(
                "SELECT " + KEY_NAME + " FROM " + table_name + " WHERE " + KEY_POSITION + "=" + pos,
                "");
        if (c.moveToFirst()) name = c.getString(0);
        return name;
    }
    public String getDefaultAccName() {
        return getDefaultSectionName(TABLE_ACCOUNT, 0);
    }
    public String getDefaultCatName() {
        return getDefaultSectionName(TABLE_CATEGORY, 1);
    }
    public Expense getExpenseFromCursor(Cursor c) {
        if (c.getCount() == 0) return new Expense();
        int id = c.getInt(c.getColumnIndexOrThrow(KEY_ID));
        float amt = c.getFloat(c.getColumnIndexOrThrow(KEY_AMOUNT));
        String desc = c.getString(c.getColumnIndexOrThrow(KEY_DESC));
        Account acc = getAccount(c.getInt(c.getColumnIndexOrThrow(KEY_ACC_ID)));
        Category cat = getCategory(c.getInt(c.getColumnIndexOrThrow(KEY_CAT_ID)));
        String datetime = c.getString(c.getColumnIndexOrThrow(KEY_DATETIME));
        return new Expense(id, amt, desc, acc, cat, datetime);
    }
    public Account getAccountFromCursor(Cursor c) {
        if (c.getCount() == 0) return new Account(context);
        int id = c.getInt(c.getColumnIndexOrThrow(KEY_ID));
        String name = c.getString(c.getColumnIndexOrThrow(KEY_NAME));
        String icon = c.getString(c.getColumnIndexOrThrow(KEY_ICON));
        String color = c.getString(c.getColumnIndexOrThrow(KEY_COLOR));
        int pos = c.getInt(c.getColumnIndexOrThrow(KEY_POSITION));
        Currency currency = MainActivity.getCurrencyFromNameStatic(c.getString(c.getColumnIndexOrThrow(KEY_CURRENCY)));
        return new Account(context, id, name, icon, color, pos, currency);
    }
    public Category getCategoryFromCursor(Cursor c) {
        if (c.getCount() == 0) return null;
        int id = c.getInt(c.getColumnIndexOrThrow(KEY_ID));
        String name = c.getString(c.getColumnIndexOrThrow(KEY_NAME));
        String icon = c.getString(c.getColumnIndexOrThrow(KEY_ICON));
        String color = c.getString(c.getColumnIndexOrThrow(KEY_COLOR));
        int pos = c.getInt(c.getColumnIndexOrThrow(KEY_POSITION));
        return new Category(context, id, name, icon, color, pos);
    }
    public Currency getCurrencyFromCursor(Cursor c) {
        if (c.getCount() == 0) return new Currency(context);
        String name = c.getString(c.getColumnIndexOrThrow(KEY_NAME));
        String desc = c.getString(c.getColumnIndexOrThrow(KEY_DESC));
        String symbol = c.getString(c.getColumnIndexOrThrow(KEY_CURRENCY));
        float xrate = c.getFloat(c.getColumnIndexOrThrow(KEY_XRATE));
        return new Currency(name, symbol, xrate, desc);
    }

    /**
     * Computation
     */
    public Calendar[] getFirstLastDates() {
        Calendar[] firstLastDates = null;
        String dateFormat = Expense.DATETIME_FORMAT;
        Cursor c = getCursorFromQuery(
                "SELECT MIN(strftime('" + dateFormat + "', " + KEY_DATETIME + ")), " +
                        "MAX(strftime('" + dateFormat + "', " + KEY_DATETIME + ")) FROM " + TABLE_EXPENSE,
                "");
        Calendar firstDate, lastDate;
        if (c.moveToFirst()) {
            firstDate = MainActivity.getCalFromString(dateFormat, c.getString(0));
            lastDate = MainActivity.getCalFromString(dateFormat, c.getString(1));
            firstLastDates = new Calendar[] { firstDate, lastDate };
        }
        c.close();
        return firstLastDates;
    }
    public Calendar[] getFirstLastDatesInDateRange(int period, Calendar from, Calendar to) {
        String DATEFORMAT, dtf;
        switch (period) {
            case DateGridAdapter.MONTH:
                DATEFORMAT = "%Y-%m";
                dtf = "yyyy-MM";
                break;
            case DateGridAdapter.WEEK:
                DATEFORMAT = "%Y-%W";
                dtf = "yyyy-ww";
                break;
            default:
                DATEFORMAT = "%Y-%m-%d";
                dtf = "yyyy-MM-dd";
        }

        String fromStr = MainActivity.getDatetimeStr(from, Expense.DATETIME_FORMAT);
        String toStr = MainActivity.getDatetimeStr(to, Expense.DATETIME_FORMAT);
        Cursor c = getCursorFromQuery(
                "SELECT MIN(strftime('" + DATEFORMAT + "', " + KEY_DATETIME +
                        ")), MAX(strftime('" + DATEFORMAT + "', " + KEY_DATETIME + ")) FROM " + TABLE_EXPENSE +
                        " WHERE " + KEY_DATETIME + " BETWEEN '" + fromStr + "' AND '" + toStr + "'",
                "");
        Calendar firstDate, lastDate;
        if (c.moveToFirst()) {
            firstDate = MainActivity.getCalFromString(dtf, c.getString(0));
            lastDate = MainActivity.getCalFromString(dtf, c.getString(1));
        } else {
            firstDate = lastDate = null;
        }
        c.close();
        return new Calendar[] { firstDate, lastDate };
    }

    // average
    public int getNumUnits(int period, Calendar from, Calendar to) {
        int numUnits = 0;

        // get first and last dates of recorded expenses in given date range
        Calendar[] firstLastDates = getFirstLastDatesInDateRange(period, from, to);
        Calendar lastDate;
        if (firstLastDates == null) // no expenses in the range
            return numUnits;
        else // only return most recent expense
            lastDate = firstLastDates[1];

        // get number of days/months/weeks
        LocalDate fromDate = LocalDateTime.ofInstant(from.toInstant(), from.getTimeZone().toZoneId()).toLocalDate();
        LocalDate toDate = LocalDateTime.ofInstant(lastDate.toInstant(), lastDate.getTimeZone().toZoneId()).toLocalDate();
        switch (period) {
            case DateGridAdapter.MONTH: numUnits = (int) ChronoUnit.MONTHS.between(fromDate, toDate) + 1; break;
            case DateGridAdapter.WEEK: numUnits = (int) ChronoUnit.WEEKS.between(fromDate, toDate) + 1; break;
            default: numUnits = (int) (ChronoUnit.DAYS.between(fromDate, toDate) + 1);
        }
        return numUnits;
    }
    public float[] getAverages(Calendar from, Calendar to) {
        float totalAmt = getConvertedTotalAmtInDateRange(from, to);
        int numDays = getNumUnits(DateGridAdapter.DAY, from, to);
        int numWeeks = getNumUnits(DateGridAdapter.WEEK, from, to);
        int numMonths = getNumUnits(DateGridAdapter.MONTH, from, to);
        float avgDay = totalAmt / numDays;
        float avgWeek = totalAmt / numWeeks;
        float avgMonths = totalAmt / numMonths;
//        Log.e(TAG, "totalAmt=" + totalAmt);
//        Log.e(TAG, "numDays=" + numDays + ", numWeeks=" + numWeeks + ", numMonths=" + numMonths);
        return new float[] { avgDay, avgWeek, avgMonths };
    }

    // position of new section
    public int getNewPosSection(String table_name) {
        int maxPos = 0;
        Cursor c = getCursorFromQuery("SELECT MAX(" + KEY_POSITION + ") FROM " + table_name, "");
        if (c.moveToFirst()) maxPos = c.getInt(0);
        c.close();
        return maxPos + 1;
    }
    public int getNewPosAccount() {
        return getNewPosSection(TABLE_ACCOUNT);
    }
    public int getNewPosCategory() {
        return getNewPosSection(TABLE_CATEGORY);
    }

    // number of sections
    public int getNumAccountsNonDefault() {
        int count = 0;
        StringBuilder accListStr = new StringBuilder();
        ArrayList<Integer> defaultAccIds = new ArrayList<>();
        for (String name : Constants.defaultAccNames) {
            int id = getAccount(name,false).getId(); // account may have been deleted
            if (id != -1) defaultAccIds.add(id);
        }
        for (int i = 0;i < defaultAccIds.size();i++) {
            accListStr.append(defaultAccIds.get(i));
            if (i != defaultAccIds.size()-1) accListStr.append(",");
        }
        Cursor c = getCursorFromQuery("SELECT COUNT(*) FROM " + TABLE_ACCOUNT + " WHERE " + KEY_ID + " NOT IN (" + accListStr + ")", "");
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }
    public int getNumCategoriesNonDefault() {
        int count = 0;
        StringBuilder catListStr = new StringBuilder();
        ArrayList<Integer> defaultCatIds = new ArrayList<>();
        for (String name : Constants.defaultCatNames) {
            int id = getCategory(name,false).getId(); // category may have been deleted
            if (id != -1) defaultCatIds.add(id);
        }
        for (int i = 0;i < defaultCatIds.size();i++) {
            catListStr.append(defaultCatIds.get(i));
            if (i != defaultCatIds.size()-1) catListStr.append(",");
        }
        Cursor c = getCursorFromQuery("SELECT COUNT(*) FROM " + TABLE_CATEGORY + " WHERE " + KEY_ID + " NOT IN (" + catListStr + ")", "");
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }

    // number of expenses
    public int getNumExpensesByAccount(Account acc) {
        int count = 0;
        Cursor c = getCursorFromQuery("SELECT COUNT(*) FROM " + TABLE_EXPENSE + " WHERE " + KEY_ACC_ID + " = " + acc.getId(), "");
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }
    public int getNumExpensesByCategory(Category cat) {
        int count = 0;
        Cursor c = getCursorFromQuery("SELECT COUNT(*) FROM " + TABLE_EXPENSE + " WHERE " + KEY_CAT_ID + " = " + cat.getId(), "");
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }
    public int getNumExpensesNonDefaultAccount() {
        int count = 0;
        StringBuilder accListStr = new StringBuilder();
        ArrayList<Integer> defaultAccIds = new ArrayList<>();
        for (String name : Constants.defaultAccNames) {
            int id = getAccount(name,false).getId(); // account may have been deleted
            if (id != -1) defaultAccIds.add(id);
        }
        for (int i = 0;i < defaultAccIds.size();i++) {
            accListStr.append(defaultAccIds.get(i));
            if (i != defaultAccIds.size()-1) accListStr.append(",");
        }
        Cursor c = getCursorFromQuery("SELECT COUNT(*) FROM " + TABLE_EXPENSE + " WHERE " + KEY_ACC_ID + " NOT IN (" + accListStr + ")", "");
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }
    public int getNumExpensesNonDefaultCategory() {
        int count = 0;
        StringBuilder catListStr = new StringBuilder();
        ArrayList<Integer> defaultCatIds = new ArrayList<>();
        for (String name : Constants.defaultCatNames) {
            int id = getCategory(name, false).getId(); // category may have been deleted
            if (id != -1) defaultCatIds.add(id);
        }
        for (int i = 0;i < defaultCatIds.size();i++) {
            catListStr.append(defaultCatIds.get(i));
            if (i != defaultCatIds.size()-1) catListStr.append(",");
        }
        Cursor c = getCursorFromQuery("SELECT COUNT(*) FROM " + TABLE_EXPENSE + " WHERE " + KEY_CAT_ID + " NOT IN (" + catListStr + ")", "");
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }
    public int getNumExpensesByAccounts(ArrayList<Account> accs) {
        int count = 0;
        StringBuilder accListStr = new StringBuilder();
        for (int i = 0;i < accs.size();i++) {
            if (accListStr.length() != 0)
                accListStr.append(",");
            accListStr.append(accs.get(i).getId());
        }
        Cursor c = getCursorFromQuery("SELECT COUNT(*) FROM " + TABLE_EXPENSE + " WHERE " + KEY_ACC_ID + " IN (" + accListStr + ")", "");
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }
    public int getNumExpensesByCategories(ArrayList<Category> cats) {
        int count = 0;
        StringBuilder catListStr = new StringBuilder();
        for (int i = 0;i < cats.size();i++) {
            if (catListStr.length() != 0)
                catListStr.append(",");
            catListStr.append(cats.get(i).getId());
        }
        Cursor c = getCursorFromQuery("SELECT COUNT(*) FROM " + TABLE_EXPENSE + " WHERE " + KEY_CAT_ID + " IN (" + catListStr + ")", "");
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }

    // totals
    public float getTotalAmtByAccount(Account acc) {
        float totalAmt = 0;
        Cursor c = getCursorFromQuery(
                "SELECT SUM(" + KEY_AMOUNT + ") FROM " + TABLE_EXPENSE +
                " WHERE " + KEY_ACC_ID + "=" + acc.getId(),
                "");
        if (c.moveToFirst()) totalAmt = c.getFloat(0);
        c.close();
        return totalAmt;
    }
    public float getConvertedTotalAmt() {
        float totalAmt = 0;
        Cursor c = getCursorFromQueryOrNull(
                "SELECT " + KEY_ACC_ID + ", SUM(" + KEY_AMOUNT + ") FROM " + TABLE_EXPENSE +
                " GROUP BY " + KEY_ACC_ID,
                "");
        if (c == null)
            return totalAmt;
        while (c.moveToNext()) {
            Account acc = getAccount(c.getInt(0));
            totalAmt += ((MainActivity) context).convertAmt(c.getFloat(1), acc);
        }
        c.close();
        return totalAmt;
    }
    public float getConvertedTotalAmtInDateRange(Calendar from, Calendar to) {
        float totalAmt = 0;
        String fromStr = MainActivity.getDatetimeStr(from, Expense.DATETIME_FORMAT);
        String toStr = MainActivity.getDatetimeStr(to, Expense.DATETIME_FORMAT);
        Cursor c = getCursorFromQueryOrNull(
                "SELECT " + KEY_ACC_ID + ", SUM(" + KEY_AMOUNT + ") FROM " + TABLE_EXPENSE +
                        " WHERE " + KEY_DATETIME + " BETWEEN '" + fromStr + "' AND '" + toStr +
                        "' GROUP BY " + KEY_ACC_ID,
                "");
        if (c == null)
            return totalAmt;
        while (c.moveToNext()) {
            Account acc = getAccount(c.getInt(0));
            totalAmt += ((MainActivity) context).convertAmt(c.getFloat(1), acc);
        }
        c.close();
        return totalAmt;
    }
    public float getConvertedFilteredTotalAmt(ArrayList<Account> accs, ArrayList<Category> cats) {
        if (accs.isEmpty() && cats.isEmpty()) // no filters
            return getConvertedTotalAmt();
        float totalAmt = 0;
        String query = getQueryFromFiltersInDateRange(KEY_ACC_ID + ", SUM(" + KEY_AMOUNT + ")", accs, cats, null, null, KEY_ACC_ID);
        Cursor c = getCursorFromQueryOrNull(query, "");
        if (c == null)
            return totalAmt;
        while (c.moveToNext()) {
            Account acc = getAccount(c.getInt(0));
            totalAmt += ((MainActivity) context).convertAmt(c.getFloat(1), acc);
        }
        c.close();
        return totalAmt;
    }
    public float getConvertedFilteredTotalAmtInDateRange(ArrayList<Account> accs, ArrayList<Category> cats, Calendar from, Calendar to) {
        if (accs.isEmpty() && cats.isEmpty()) // no filters
            return getConvertedTotalAmtInDateRange(from, to);
        float totalAmt = 0;
        String query = getQueryFromFiltersInDateRange(KEY_ACC_ID + ", SUM(" + KEY_AMOUNT + ")", accs, cats, from, to, KEY_ACC_ID);
        Cursor c = getCursorFromQueryOrNull(query, "");
        if (c == null)
            return totalAmt;
        while (c.moveToNext()) {
            Account acc = getAccount(c.getInt(0));
            totalAmt += ((MainActivity) context).convertAmt(c.getFloat(1), acc);
        }
        c.close();
        return totalAmt;
    }
    public ArrayList<Category> getSortedCategoriesByConvertedTotalAmt() {
        ArrayList<Category> categories = new ArrayList<>();

        HashMap<Integer, Pair<Float,Integer>> catIdAmtMap = new HashMap<>();
        Cursor c = getCursorFromQueryOrNull(
                "SELECT " + KEY_CAT_ID + "," + KEY_ACC_ID + ", SUM(" + KEY_AMOUNT + "), COUNT(*) FROM " + TABLE_EXPENSE +
                        " GROUP BY " + KEY_CAT_ID + "," + KEY_ACC_ID,
                "");
        if (c == null)
            return categories;
        while (c.moveToNext()) {
            int catId = c.getInt(0);
            float catAmt = 0;
            int catNumExp = 0;
            if (catIdAmtMap.containsKey(catId)) {
                catAmt = catIdAmtMap.get(catId).first;
                catNumExp = catIdAmtMap.get(catId).second;
            }
            Account acc = getAccount(c.getInt(1));
            catAmt += ((MainActivity) context).convertAmt(c.getFloat(2), acc);
            catNumExp += c.getInt(3);
            catIdAmtMap.put(catId, new Pair<>(catAmt, catNumExp));
        }
        c.close();

        ArrayList<Map.Entry<Integer, Pair<Float,Integer>>> entries = new ArrayList<>(catIdAmtMap.entrySet());
        entries.sort((Comparator<Map.Entry<Integer, Pair<Float,Integer>>>) (o1, o2) -> Float.compare(o2.getValue().first, o1.getValue().first));

        for (Map.Entry<Integer, Pair<Float,Integer>> entry : entries) {
            Category cat = getCategory(entry.getKey());
            cat.setAmount(entry.getValue().first);
            cat.setNumExpenses(entry.getValue().second);
            categories.add(cat);
        }
        return categories;
    }
    public ArrayList<Category> getSortedCategoriesByConvertedTotalAmtInDateRange(Calendar from, Calendar to) {
        ArrayList<Category> categories = new ArrayList<>();

        HashMap<Integer, Pair<Float,Integer>> catIdAmtMap = new HashMap<>();
        String fromStr = MainActivity.getDatetimeStr(from, Expense.DATETIME_FORMAT);
        String toStr = MainActivity.getDatetimeStr(to, Expense.DATETIME_FORMAT);
        Cursor c = getCursorFromQueryOrNull(
                "SELECT " + KEY_CAT_ID + "," + KEY_ACC_ID + ", SUM(" + KEY_AMOUNT + "), COUNT(*) FROM " + TABLE_EXPENSE +
                        " WHERE " + KEY_DATETIME + " BETWEEN '" + fromStr + "' AND '" + toStr +
                        "' GROUP BY " + KEY_CAT_ID + "," + KEY_ACC_ID,
                "");
        if (c == null)
            return categories;
        while (c.moveToNext()) {
            int catId = c.getInt(0);
            float catAmt = 0;
            int catNumExp = 0;
            if (catIdAmtMap.containsKey(catId)) {
                catAmt = catIdAmtMap.get(catId).first;
                catNumExp = catIdAmtMap.get(catId).second;
            }
            Account acc = getAccount(c.getInt(1));
            catAmt += ((MainActivity) context).convertAmt(c.getFloat(2), acc);
            catNumExp += c.getInt(3);
            catIdAmtMap.put(catId, new Pair<>(catAmt, catNumExp));
        }
        c.close();

        ArrayList<Map.Entry<Integer, Pair<Float,Integer>>> entries = new ArrayList<>(catIdAmtMap.entrySet());
        entries.sort((Comparator<Map.Entry<Integer, Pair<Float,Integer>>>) (o1, o2) -> Float.compare(o2.getValue().first, o1.getValue().first));

        for (Map.Entry<Integer, Pair<Float,Integer>> entry : entries) {
            Category cat = getCategory(entry.getKey());
            cat.setAmount(entry.getValue().first);
            cat.setNumExpenses(entry.getValue().second);
            categories.add(cat);
        }
        return categories;
    }
    public HashMap<String, Float> getSortedAmountsByDateRange(Calendar from, Calendar to, String dateFormat) {
        HashMap<String, Float> dateAmtMap = new HashMap<>();
        String fromStr = MainActivity.getDatetimeStr(from, Expense.DATETIME_FORMAT);
        String toStr = MainActivity.getDatetimeStr(to, Expense.DATETIME_FORMAT);
        Cursor c = getCursorFromQueryOrNull(
                "SELECT " + KEY_ACC_ID + ", strftime('" + dateFormat + "', " + KEY_DATETIME + "), SUM(" + KEY_AMOUNT +
                        ") FROM " + TABLE_EXPENSE +
                        " WHERE " + KEY_DATETIME + " BETWEEN '" + fromStr + "' AND '" + toStr +
                        "' GROUP BY strftime('" + dateFormat + "', " + KEY_DATETIME + "), " + KEY_ACC_ID,
                "");
        if (c == null)
            return dateAmtMap;
        while (c.moveToNext()) {
            Account acc = getAccount(c.getInt(0));
            String date = c.getString(1);
            float dateAmt = 0;
            if (dateAmtMap.containsKey(date))
                dateAmt = dateAmtMap.get(date);
            dateAmt += ((MainActivity) context).convertAmt(c.getFloat(2), acc);
            dateAmtMap.put(date, dateAmt);
        }
        c.close();
        return dateAmtMap;
    }

    /**
     * Import/Export
     */
    public File getDatabaseFile() {
        return context.getDatabasePath(DATABASE_NAME);
    }
    public void importDatabase(InputStream in) throws IOException {
        close();
        File outputDir = context.getCacheDir(); // context being the Activity pointer
        File newDb = File.createTempFile("tmp", ".db", outputDir);
        File oldDb = getDatabaseFile();
        OutputStream out = null;
        try {
            out = new FileOutputStream(newDb);
            byte[] buf = new byte[1024];
            int len;
            while((len = in.read(buf)) >0) out.write(buf,0,len);
        } catch (Exception e) { e.printStackTrace();
        } finally {
            try {
                if ( out != null ) out.close();
                in.close();
            } catch ( IOException e ) { e.printStackTrace(); }
        }
        FileUtils.copyFile(new FileInputStream(newDb), new FileOutputStream(oldDb));
        getWritableDatabase().close();
    }
    public void exportDatabase() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, DATABASE_NAME);   // file name
        values.put(MediaStore.MediaColumns.MIME_TYPE, "application/x-sqlite3");
        Uri extVolumeUri = MediaStore.Files.getContentUri("external");
        Uri fileUri = context.getContentResolver().insert(extVolumeUri, values);

        try {
            FileInputStream stream = new FileInputStream(getDatabaseFile());
            OutputStream output = context.getContentResolver().openOutputStream(fileUri);
            byte[] buffer = new byte[1024]; // transfer bytes from the inputfile to the outputfile
            int length;
            while ((length = stream.read(buffer)) > 0) output.write(buffer, 0, length);
            output.flush();
            output.close();
            stream.close();
            Toast.makeText(context, "Database exported to Downloads" , Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Log.e(TAG, String.valueOf(e));
            Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show();
        }
    }
}
