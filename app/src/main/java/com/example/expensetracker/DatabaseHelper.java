package com.example.expensetracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

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

    // Common column names
    private static final String KEY_ID = "id";

    // EXPENSES Table - column names
    private static final String KEY_AMOUNT = "amount";
    private static final String KEY_DESC = "description";
    private static final String KEY_CAT_ID = "category_id";
    private static final String KEY_ACC_ID = "account_id";
    private static final String KEY_DATETIME = "datetime";

    // CATEGORIES & ACCOUNTS Tables - column names
    private static final String KEY_NAME = "name";
    private static final String KEY_ICON = "icon";
    private static final String KEY_COLOR = "color";

    // Table Create Statements
    // EXPENSES table create statement
    private static final String CREATE_TABLE_EXPENSE =
            "CREATE TABLE " + TABLE_EXPENSE + "("
                    + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + KEY_AMOUNT + " REAL NOT NULL,"
                    + KEY_DESC + " TEXT NOT NULL,"
                    + KEY_ACC_ID + " INTEGER NOT NULL,"
                    + KEY_CAT_ID + " INTEGER NOT NULL,"
                    + KEY_DATETIME + " TEXT"
                    + ")";

    // CATEGORIES table create statement
    private static final String CREATE_TABLE_CATEGORY =
            "CREATE TABLE " + TABLE_CATEGORY + "("
                    + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + KEY_NAME + " TEXT UNIQUE,"
                    + KEY_ICON + " TEXT,"
                    + KEY_COLOR + " TEXT"
                    + ")";

    // ACCOUNTS table create statement
    private static final String CREATE_TABLE_ACCOUNT =
            "CREATE TABLE " + TABLE_ACCOUNT + "("
                    + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + KEY_NAME + " TEXT UNIQUE,"
                    + KEY_ICON + " TEXT,"
                    + KEY_COLOR + " TEXT"
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
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // on upgrade drop older tables
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EXPENSE);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CATEGORY);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ACCOUNT);

        // create new tables
        onCreate(db);
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
        ContentValues values = new ContentValues();
        values.put(KEY_AMOUNT, expense.getAmount());
        values.put(KEY_DESC, expense.getDescription());
        values.put(KEY_ACC_ID, expense.getAccount().getId());
        values.put(KEY_CAT_ID, expense.getCategory().getId());
        values.put(KEY_DATETIME, expense.getDatetimeStr());

        long res = db.insert(TABLE_EXPENSE, null, values);
        String toast = (res == -1) ? "Error: Failed to create expense" : "Expense created";
        Toast.makeText(context, toast, Toast.LENGTH_SHORT).show();
    }
    public long createSection(Section section, String table_name) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_NAME, section.getName());
        values.put(KEY_ICON, section.getIconName());
        values.put(KEY_COLOR, section.getColorName());
        return db.insert(table_name, null, values);
    }
    public void createAccount(Account account, boolean notify) {
        long res = createSection(account, TABLE_ACCOUNT);
        if (notify) {
            String toast = (res == -1) ? "Error: Failed to create account " : "Account created: ";
            Toast.makeText(context, toast + account.getName(), Toast.LENGTH_SHORT).show();
        }
    }
    public void createCategory(Category category, boolean notify) {
        long res = createSection(category, TABLE_CATEGORY);
        if (notify) {
            String toast = (res == -1) ? "Error: Failed to create category " : "Category created: ";
            Toast.makeText(context, toast + category.getName(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * READ - GETTERS & SETTERS
     */
    // Expenses
    public ArrayList<Expense> getAllExpenses() {
        ArrayList<Expense> expenses = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_EXPENSE;
        Cursor c = db.rawQuery(query, null);
        if (c.getCount() != 0) {
            while (c.moveToNext()) {
                int id = c.getInt(c.getColumnIndexOrThrow(KEY_ID));
                float amt = c.getFloat(c.getColumnIndexOrThrow(KEY_AMOUNT));
                String desc = c.getString(c.getColumnIndexOrThrow(KEY_DESC));
                Account acc = getAccount(c.getInt(c.getColumnIndexOrThrow(KEY_ACC_ID)));
                Category cat = getCategory(c.getInt(c.getColumnIndexOrThrow(KEY_CAT_ID)));
                String datetime = c.getString(c.getColumnIndexOrThrow(KEY_DATETIME));
                expenses.add(new Expense(id, amt, desc, acc, cat, datetime));
            }
        }
        c.close();
        return expenses;
    }
    public Expense getExpense(int id) {
        Expense exp = null;
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_EXPENSE + " WHERE "  + KEY_ID + " = " + id;
        Cursor c = db.rawQuery(query, null);
        if (c.getCount() == 0) {
            Toast.makeText(context, "Expense ID: " + id + " not found", Toast.LENGTH_SHORT).show();
        } else {
            c.moveToFirst();
            float amt = c.getFloat(c.getColumnIndexOrThrow(KEY_AMOUNT));
            String desc = c.getString(c.getColumnIndexOrThrow(KEY_DESC));
            Account acc = getAccount(c.getInt(c.getColumnIndexOrThrow(KEY_ACC_ID)));
            Category cat = getCategory(c.getInt(c.getColumnIndexOrThrow(KEY_CAT_ID)));
            String datetime = c.getString(c.getColumnIndexOrThrow(KEY_DATETIME));
            exp = new Expense(id, amt, desc, acc, cat, datetime);
        }
        c.close();
        return exp;
    }
    public ArrayList<Expense> getExpensesByDateRange(Calendar from, Calendar to) {
        ArrayList<Expense> expenses = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_EXPENSE;
        Cursor c = db.rawQuery(query, null);
        if (c.getCount() != 0) {
            while (c.moveToNext()) {
                String datetime = c.getString(c.getColumnIndexOrThrow(KEY_DATETIME));
                Calendar cal = Calendar.getInstance();
                try {
                    cal.setTime(new SimpleDateFormat(Expense.DATETIME_FORMAT, MainActivity.locale).parse(datetime));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (cal.compareTo(from)>-1 && cal.compareTo(to)<1) {
                    int id = c.getInt(c.getColumnIndexOrThrow(KEY_ID));
                    float amt = c.getFloat(c.getColumnIndexOrThrow(KEY_AMOUNT));
                    String desc = c.getString(c.getColumnIndexOrThrow(KEY_DESC));
                    Account acc = getAccount(c.getInt(c.getColumnIndexOrThrow(KEY_ACC_ID)));
                    Category cat = getCategory(c.getInt(c.getColumnIndexOrThrow(KEY_CAT_ID)));
                    expenses.add(new Expense(id, amt, desc, acc, cat, datetime));
                }
            }
        }
        c.close();
        return expenses;
    }
    public ArrayList<Expense> getExpensesByFilters(ArrayList<Account> accs, ArrayList<Category> cats) {
        ArrayList<Expense> expenses = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String accList = "";
        String catList = "";
        for (int i = 0;i < accs.size();i++) {
            accList += accs.get(i).getId();
            if (i != accs.size()-1) accList += ",";
        }
        for (int i = 0;i < cats.size();i++) {
            catList += cats.get(i).getId();
            if (i != cats.size()-1) catList += ",";
        }
        String query;
        if (accs.isEmpty() && cats.isEmpty()) { // no filters
            return getAllExpenses();
        } else if (accs.isEmpty() && !cats.isEmpty()) { // cat filters
            query = "SELECT * FROM " + TABLE_EXPENSE + " WHERE " + KEY_CAT_ID + " IN (" + catList + ")";
        } else if (cats.isEmpty()) { // acc filters
            query = "SELECT * FROM " + TABLE_EXPENSE + " WHERE " + KEY_ACC_ID + " IN (" + accList + ")";
        } else { // both filters
            query = "SELECT * FROM " + TABLE_EXPENSE + " WHERE "
                    + KEY_ACC_ID + " IN (" + accList + ") AND "
                    + KEY_CAT_ID + " IN (" + catList + ")";
        }
        Cursor c = db.rawQuery(query, null);
        if (c.getCount() != 0) {
            while (c.moveToNext()) {
                int id = c.getInt(c.getColumnIndexOrThrow(KEY_ID));
                float amt = c.getFloat(c.getColumnIndexOrThrow(KEY_AMOUNT));
                String desc = c.getString(c.getColumnIndexOrThrow(KEY_DESC));
                Account acc = getAccount(c.getInt(c.getColumnIndexOrThrow(KEY_ACC_ID)));
                Category cat = getCategory(c.getInt(c.getColumnIndexOrThrow(KEY_CAT_ID)));
                String datetime = c.getString(c.getColumnIndexOrThrow(KEY_DATETIME));
                expenses.add(new Expense(id, amt, desc, acc, cat, datetime));
            }
        }
        c.close();
        return expenses;
    }

    // Sections
    public ArrayList<Section> getAllSections(String table_name) {
        ArrayList<Section> sections = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + table_name;
        Cursor c = db.rawQuery(query, null);
        if (c.getCount() != 0) {
            while (c.moveToNext()) {
                int id = c.getInt(c.getColumnIndexOrThrow(KEY_ID));
                String name = c.getString(c.getColumnIndexOrThrow(KEY_NAME));
                String icon = c.getString(c.getColumnIndexOrThrow(KEY_ICON));
                String color = c.getString(c.getColumnIndexOrThrow(KEY_COLOR));
                sections.add(new Section(context, id, name, icon, color));
            }
        }
        c.close();
        return sections;
    }
    public ArrayList<Account> getAllAccounts() {
        ArrayList<Section> sections = getAllSections(TABLE_ACCOUNT);
        ArrayList<Account> accounts = new ArrayList<>();
        for (Section section : sections) {
            accounts.add(new Account(section.context, section.id, section.name, section.icon, section.color));
        }
        return accounts;
    }
    public ArrayList<Category> getAllCategories() {
        ArrayList<Section> sections = getAllSections(TABLE_CATEGORY);
        ArrayList<Category> categories = new ArrayList<>();
        for (Section section : sections) {
            categories.add(new Category(section.context, section.id, section.name, section.icon, section.color));
        }
        return categories;
    }

    public Section getSection(String name, String table_name) {
        Section section = null;
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + table_name + " WHERE "  + KEY_NAME + " = '" + name + "'";
        Cursor c = db.rawQuery(query, null);
        if (c.getCount() == 0) {
            String toast = (table_name.equals(TABLE_ACCOUNT)) ? "Account" : "Category";
            Toast.makeText(context, toast + " not found. Check name again", Toast.LENGTH_SHORT).show();
        } else {
            c.moveToFirst();
            int id = c.getInt(c.getColumnIndexOrThrow(KEY_ID));
            String icon = c.getString(c.getColumnIndexOrThrow(KEY_ICON));
            String color = c.getString(c.getColumnIndexOrThrow(KEY_COLOR));
            section = new Section(context, id, name, icon, color);
        }
        c.close();
        return section;
    }
    public Section getSection(int id, String table_name) {
        Section section = null;
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + table_name + " WHERE "  + KEY_ID + " = " + id;
        Cursor c = db.rawQuery(query, null);
        if (c.getCount() == 0) {
            Toast.makeText(context, "Category ID: " + id + " not found", Toast.LENGTH_SHORT).show();
        } else {
            c.moveToFirst();
            String name = c.getString(c.getColumnIndexOrThrow(KEY_NAME));
            String icon = c.getString(c.getColumnIndexOrThrow(KEY_ICON));
            String color = c.getString(c.getColumnIndexOrThrow(KEY_COLOR));
            section = new Section(context, id, name, icon, color);
        }
        c.close();
        return section;
    }
    public Account getAccount(String name) {
        Section section = getSection(name, TABLE_ACCOUNT);
        return new Account(context, section.id, section.name, section.icon, section.color);
    }
    public Account getAccount(int id) {
        Section section = getSection(id, TABLE_ACCOUNT);
        return new Account(context, section.id, section.name, section.icon, section.color);
    }
    public Category getCategory(String name) {
        Section section = getSection(name, TABLE_CATEGORY);
        return new Category(context, section.id, section.name, section.icon, section.color);
    }
    public Category getCategory(int id) {
        Section section = getSection(id, TABLE_CATEGORY);
        return new Category(context, section.id, section.name, section.icon, section.color);
    }

    /**
     * UPDATE
     */
    public int updateExpense(Expense expense) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_AMOUNT, expense.getAmount());
        values.put(KEY_DESC, expense.getDescription());
        values.put(KEY_ACC_ID, expense.getAccount().getId());
        values.put(KEY_CAT_ID, expense.getCategory().getId());
        values.put(KEY_DATETIME, expense.getDatetimeStr());
        return db.update(TABLE_EXPENSE, values, KEY_ID + " = ?",
                new String[] { String.valueOf(expense.getId()) });
    }
    public <T extends Section> int updateSection(T section, String table_name) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_NAME, section.getName());
        values.put(KEY_ICON, section.getIconName());
        values.put(KEY_COLOR, section.getColorName());

        // updating row
        return db.update(table_name, values, KEY_ID + " = ?",
                new String[] { String.valueOf(section.getId()) });
    }
    public int updateAccount(Account account) {
        return updateSection(account, TABLE_ACCOUNT);
    }
    public int updateCategory(Category category) {
        return updateSection(category, TABLE_CATEGORY);
    }

    public int moveExpenses(Section section, String key, String section_name) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT * FROM " + TABLE_EXPENSE + " WHERE " + key + " = " + section.getId();
        Cursor c = db.rawQuery(query, null);
        int res = 1;
        if (c.getCount() != 0) {
            while (c.moveToNext()) {
                int id = c.getInt(c.getColumnIndexOrThrow(KEY_ID));
                ContentValues values = new ContentValues();
                if (key.equals(KEY_ACC_ID)) {
                    values.put(key, getAccount(section_name).getId());
                } else if (key.equals(KEY_CAT_ID)) {
                    values.put(key, getCategory(section_name).getId());
                }
                res = db.update(TABLE_EXPENSE, values, KEY_ID + " = ?",
                        new String[] { String.valueOf(id) });
            }
        }
        c.close();
        return res;
    }
    public int moveExpensesToCash(Account acc) {
        return moveExpenses(acc, KEY_ACC_ID, "Cash");
    }
    public int moveExpensesToOthers(Category cat) {
        return moveExpenses(cat, KEY_CAT_ID, "Others");
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
        if (acc.getName().equals("Cash")) {
            Toast.makeText(context, "Cannot delete account Cash", Toast.LENGTH_SHORT).show();
            return;
        }
        int moveRes = moveExpensesToCash(acc);
        if (moveRes == 0) return;
        int res = deleteSection(TABLE_ACCOUNT, acc);
        if (res == 0)
            Toast.makeText(context, "Error: Failed to delete account", Toast.LENGTH_SHORT).show();
        else if (notify)
            Toast.makeText(context, "Account deleted", Toast.LENGTH_SHORT).show();
    }
    public void deleteCategory(Category cat, boolean notify) {
        if (cat.getName().equals("Others")) {
            Toast.makeText(context, "Cannot delete category Others", Toast.LENGTH_SHORT).show();
            return;
        }
        int moveRes = moveExpensesToOthers(cat);
        if (moveRes == 0) {
            return;
        }
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
            if (!Arrays.asList(Constants.defaultAccNames).contains(acc.getName())) {
                moveExpensesToCash(acc);
            }
        }
        deleteAllSections(TABLE_ACCOUNT);
    }
    public void deleteAllCategories() {
        ArrayList<Category> categories = getAllCategories();
        for (Category cat : categories) {
            if (!Arrays.asList(Constants.defaultCatNames).contains(cat.getName())) {
                moveExpensesToOthers(cat);
            }
        }
        deleteAllSections(TABLE_CATEGORY);
    }

    /**
     * END CRUD OPERATIONS
     */

    /**
     * CHECKS
     */
    // Check if not empty
    public boolean empty(String table_name) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + table_name;
        Cursor c = db.rawQuery(query, null);
        boolean res = (c.getCount() == 0);
        c.close();
        return res;
    }

    /**
     * COMPUTATION
     */
    public Calendar[] getFirstLastDates() {
        Calendar firstDate = null;
        Calendar lastDate = Calendar.getInstance();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " + KEY_DATETIME + " FROM " + TABLE_EXPENSE;
        Cursor c = db.rawQuery(query, null);
        if (c.getCount() == 0) {
            Toast.makeText(context, "No expenses added", Toast.LENGTH_SHORT).show();
        } else {
            while (c.moveToNext()) {
                try {
                    Date date = new SimpleDateFormat(Expense.DATETIME_FORMAT, MainActivity.locale).parse(c.getString(0));
                    if (firstDate == null) {
                        firstDate = Calendar.getInstance();
                        firstDate.setTime(date);
                        lastDate.setTime(date);
                    }
                    if (firstDate.getTime().compareTo(date) > 0)
                        firstDate.setTime(date);
                    if (lastDate.getTime().compareTo(date) < 0)
                        lastDate.setTime(date);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        c.close();
        return new Calendar[] { firstDate, lastDate };
    }
    public float getTotalAmtByAccount(Account acc) {
        float totalAmt = 0;
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT SUM(" + KEY_AMOUNT + ") FROM " + TABLE_EXPENSE + " WHERE " + KEY_ACC_ID + " = " + acc.getId();
        Cursor c = db.rawQuery(query, null);
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

}
