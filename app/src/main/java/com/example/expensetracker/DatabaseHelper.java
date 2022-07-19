package com.example.expensetracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import com.example.expensetracker.HelperClasses.FileUtils;
import com.example.expensetracker.RecyclerViewAdapters.DateGridAdapter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

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
                    + KEY_CURRENCY + " TEXT NOT NULL"
                    + ")";

    /**
     * CONSTRUCTOR
     */
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    /**
     * DEFAULT METHODS
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
    public void closeDB() {
        SQLiteDatabase db = this.getReadableDatabase();
        if (db != null && db.isOpen())
            db.close();
    }

    /**
     * CRUD OPERATIONS
     */
    /**
     * CREATE
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
    public void createCurrency(String name, String desc, String symbol) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_NAME, name);
        values.put(KEY_DESC, desc);
        values.put(KEY_CURRENCY, symbol);
        db.insert(TABLE_CURRENCY, null, values);
    }

    /**
     * READ - GETTERS & SETTERS
     */
    // Expenses
    public ArrayList<Expense> getAllExpenses() {
        ArrayList<Expense> expenses = new ArrayList<>();
        Cursor c = getCursorFromQueryOrNull(getQuerySelectAll(TABLE_EXPENSE), "");
        if (c == null)
            return expenses;
        while (c.moveToNext())
            expenses.add(getExpenseFromCursor(c));
        c.close();
        return expenses;
    }
    public Expense getExpense(int id) {
        Expense exp = new Expense();
        Cursor c = getCursorFromQuery(
                getQuerySelectId(TABLE_EXPENSE, KEY_ID, id),
                "Expense ID: " + id + " not found");
        if (c.moveToFirst()) exp = getExpenseFromCursor(c);
        c.close();
        return exp;
    }
    public ArrayList<Expense> getExpensesByDateRange(Calendar from, Calendar to) {
        ArrayList<Expense> expenses = new ArrayList<>();
        Cursor c = getCursorFromQueryOrNull(getQuerySelectAll(TABLE_EXPENSE), "");
        if (c == null)
            return expenses;
        while (c.moveToNext()) {
            String datetime = c.getString(c.getColumnIndexOrThrow(KEY_DATETIME));
            Calendar cal = Calendar.getInstance();
            try { cal.setTime(new SimpleDateFormat(Expense.DATETIME_FORMAT, MainActivity.locale).parse(datetime)); }
            catch (Exception e) { e.printStackTrace(); }
            if (cal.compareTo(from)>-1 && cal.compareTo(to)<1)
                expenses.add(getExpenseFromCursor(c));
        }
        c.close();
        return expenses;
    }
    public ArrayList<Expense> getExpensesByDateRangeAndCategory(Category cat, Calendar from, Calendar to) {
        ArrayList<Expense> expenses = new ArrayList<>();
        Cursor c = getCursorFromQueryOrNull(getQuerySelectId(TABLE_EXPENSE, KEY_CAT_ID, cat.getId()), "");
        if (c == null)
            return expenses;
        while (c.moveToNext()) {
            String datetime = c.getString(c.getColumnIndexOrThrow(KEY_DATETIME));
            Calendar cal = Calendar.getInstance();
            try { cal.setTime(new SimpleDateFormat(Expense.DATETIME_FORMAT, MainActivity.locale).parse(datetime)); }
            catch (Exception e) { e.printStackTrace(); }
            if (cal.compareTo(from)>-1 && cal.compareTo(to)<1)
                expenses.add(getExpenseFromCursor(c));
        }
        c.close();
        return expenses;
    }
    public ArrayList<Expense> getExpensesByFilters(ArrayList<Account> accs, ArrayList<Category> cats) {
        ArrayList<Expense> expenses = new ArrayList<>();
        String accListStr = "";
        String catListStr = "";
        for (int i = 0;i < accs.size();i++) {
            accListStr += accs.get(i).getId();
            if (i != accs.size()-1) accListStr += ",";
        }
        for (int i = 0;i < cats.size();i++) {
            catListStr += cats.get(i).getId();
            if (i != cats.size()-1) catListStr += ",";
        }
        String query;
        if (accs.isEmpty() && cats.isEmpty()) { // no filters
            return getAllExpenses();
        } else if (accs.isEmpty() && !cats.isEmpty()) { // cat filters
            query = "SELECT * FROM " + TABLE_EXPENSE + " WHERE " + KEY_CAT_ID + " IN (" + catListStr + ")";
        } else if (cats.isEmpty()) { // acc filters
            query = "SELECT * FROM " + TABLE_EXPENSE + " WHERE " + KEY_ACC_ID + " IN (" + accListStr + ")";
        } else { // both filters
            query = "SELECT * FROM " + TABLE_EXPENSE + " WHERE "
                    + KEY_ACC_ID + " IN (" + accListStr + ") AND "
                    + KEY_CAT_ID + " IN (" + catListStr + ")";
        }
        Cursor c = getCursorFromQueryOrNull(query, "");
        if (c == null)
            return expenses;
        while (c.moveToNext())
            expenses.add(getExpenseFromCursor(c));
        c.close();
        return expenses;
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
    public Account getAccount(String name) {
        Account account = new Account(context);
        Cursor c = getCursorFromQuery(
                getQuerySelectName(TABLE_ACCOUNT, KEY_NAME, name),
                "Account not found. Check name again");
        if (c.moveToFirst()) account = getAccountFromCursor(c);
        c.close();
        return account;
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
    public Category getCategory(String name) {
        Category category = new Category(context);
        Cursor c = getCursorFromQuery(
                getQuerySelectName(TABLE_CATEGORY, KEY_NAME, name),
                "Category not found. Check name again");
        if (c.moveToFirst()) category = getCategoryFromCursor(c);
        c.close();
        return category;
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
    public ArrayList<Currency> getAllCurrencies() {
        ArrayList<Currency> currencies = new ArrayList<>();
        Cursor c = getCursorFromQueryOrNull(getQuerySelectAll(TABLE_CURRENCY), "");
        if (c == null)
            return currencies;
        while (c.moveToNext()) {
            String name = c.getString(c.getColumnIndexOrThrow(KEY_NAME));
            String desc = c.getString(c.getColumnIndexOrThrow(KEY_DESC));
            String symbol = c.getString(c.getColumnIndexOrThrow(KEY_CURRENCY));
            currencies.add(new Currency(name, desc, symbol));
        }
        c.close();
        return currencies;
    }

    /**
     * UPDATE
     */
    public int updateExpense(Expense expense) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = createExpenseValues(expense);
        return db.update(TABLE_EXPENSE, values, KEY_ID + " = ?",
                new String[] { String.valueOf(expense.getId()) });
    }
    public int updateAccount(Account account) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = createSectionValues(account);
        values.put(KEY_CURRENCY, account.getCurrencyName());
        return db.update(TABLE_ACCOUNT, values, KEY_ID + " = ?",
                new String[] { String.valueOf(account.getId()) });
    }
    public int updateCategory(Category category) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = createSectionValues(category);
        return db.update(TABLE_CATEGORY, values, KEY_ID + " = ?",
                new String[] { String.valueOf(category.getId()) });
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
     * DELETE
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
        if (acc.getName().equals(Constants.defaultAccount)) {
            Toast.makeText(context, "Cannot delete account " + Constants.defaultAccount, Toast.LENGTH_SHORT).show();
            return;
        }
        int moveRes = moveExpensesToDefault(acc);
        if (moveRes == 0)
            return;
        int res = deleteSection(TABLE_ACCOUNT, acc);
        if (res == 0)
            Toast.makeText(context, "Error: Failed to delete account", Toast.LENGTH_SHORT).show();
        else if (notify)
            Toast.makeText(context, "Account deleted", Toast.LENGTH_SHORT).show();
    }
    public void deleteCategory(Category cat, boolean notify) {
        if (cat.getName().equals(Constants.defaultCategory)) {
            Toast.makeText(context, "Cannot delete category " + Constants.defaultCategory, Toast.LENGTH_SHORT).show();
            return;
        }
        int moveRes = moveExpensesToImmutable(cat);
        if (moveRes == 0)
            return;
        int res = deleteSection(TABLE_CATEGORY, cat);
        if (res == 0)
            Toast.makeText(context, "Error: Failed to delete category", Toast.LENGTH_SHORT).show();
        else if (notify)
            Toast.makeText(context, "Category deleted", Toast.LENGTH_SHORT).show();
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
     * HELPER FUNCTIONS
     */
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
        Currency currency = MainActivity.getCurrencyFromName(c.getString(c.getColumnIndexOrThrow(KEY_CURRENCY)));
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

    /**
     * COMPUTATION
     */
    public float getAverage(int period, ArrayList<Expense> expenses) {
        float totalAmt = 0;
        int count = 0;
        HashMap<String,Boolean> dates = new HashMap<>();
        for (Expense e : expenses) {
            totalAmt += e.getAmount();
            String date = (period == DateGridAdapter.MONTH) ? e.getDatetimeStr("MM-yyyy") :
                    ((period == DateGridAdapter.WEEK) ? e.getDatetime().get(Calendar.WEEK_OF_YEAR) + "-" + e.getDatetimeStr("yyyy") :
                            e.getDatetimeStr("dd-MM-yyyy"));
            if (!dates.containsKey(date)) {
                dates.put(date, true);
                count++;
            }
        }
        return (count > 0) ? totalAmt / count : totalAmt;
    }
    public float getDayAverage(ArrayList<Expense> expenses) {
        return getAverage(DateGridAdapter.DAY, expenses);
    }
    public float getWeekAverage(ArrayList<Expense> expenses) {
        return getAverage(DateGridAdapter.WEEK, expenses);
    }
    public float getMonthAverage(ArrayList<Expense> expenses) {
        return getAverage(DateGridAdapter.MONTH, expenses);
    }
    public Calendar[] getFirstLastDates() {
        Calendar firstDate = null;
        Calendar lastDate = Calendar.getInstance();
        String query = "SELECT " + KEY_DATETIME + " FROM " + TABLE_EXPENSE;
        Cursor c = getCursorFromQueryOrNull(query, "");
        if (c == null)
            return null;
        while (c.moveToNext()) {
            try {
                Date date = new SimpleDateFormat(Expense.DATETIME_FORMAT, MainActivity.locale).parse(c.getString(0));
                if (firstDate == null) {
                    firstDate = Calendar.getInstance();
                    firstDate.setTime(date);
                    lastDate.setTime(date);
                }
                if (firstDate.getTime().compareTo(date) > 0) // first > date
                    firstDate.setTime(date);
                if (lastDate.getTime().compareTo(date) < 0) // last < date
                    lastDate.setTime(date);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        c.close();
        return new Calendar[] { firstDate, lastDate };
    }
    public int getNumAccounts() {
        int count = 0;
        Cursor c = getCursorFromQuery("SELECT COUNT(*) FROM " + TABLE_ACCOUNT, "");
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }
    public int getNumCategories() {
        int count = 0;
        Cursor c = getCursorFromQuery("SELECT COUNT(*) FROM " + TABLE_CATEGORY, "");
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }
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
    public int getNumExpensesByCategoryInRange(Category cat, Calendar from, Calendar to) {
        ArrayList<Expense> expenses = getExpensesByDateRangeAndCategory(cat, from, to);
        return expenses.size();
    }
    public float getTotalAmt() {
        float totalAmt = 0;
        Cursor c = getCursorFromQuery("SELECT SUM(" + KEY_AMOUNT + ") FROM " + TABLE_EXPENSE, "");
        if (c.moveToFirst()) totalAmt = c.getFloat(0);
        c.close();
        return totalAmt;
    }
    public float getTotalAmtByAccount(Account acc) {
        float totalAmt = 0;
        Cursor c = getCursorFromQuery("SELECT SUM(" + KEY_AMOUNT + ") FROM " + TABLE_EXPENSE + " WHERE " + KEY_ACC_ID + " = " + acc.getId(), "");
        if (c.moveToFirst()) totalAmt = c.getFloat(0);
        c.close();
        return totalAmt;
    }
    public float getTotalAmtByCategory(Category cat) {
        float totalAmt = 0;
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT SUM(" + KEY_AMOUNT + ") FROM " + TABLE_EXPENSE + " WHERE " + KEY_CAT_ID + " = " + cat.getId();
        Cursor c = db.rawQuery(query, null);
        if (c.moveToFirst()) totalAmt = c.getFloat(0);
        c.close();
        return totalAmt;
    }
    public float getTotalAmtByCategoryInRange(Category cat, Calendar from, Calendar to) {
        float totalAmt = 0;
        ArrayList<Expense> expenses = getExpensesByDateRangeAndCategory(cat, from, to);
        for (Expense e : expenses) totalAmt += e.getAmount();
        return totalAmt;
    }

    /**
     * IMPORT/EXPORT
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
