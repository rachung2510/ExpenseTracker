package com.example.expensetracker;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Expense {

    private final int id;
    private final float amount;
    private final String description;
    private Category category;
    private Account account;
    private Calendar datetime;

    public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public Expense(float amount, String description, Account account, Category category, String datetime) {
        this.id = -1;
        this.amount = amount;
        this.description = description.isEmpty() ? category.getName() : description;
        this.account = account;
        this.category = category;
        this.datetime = MainActivity.getCalFromString(DATETIME_FORMAT, datetime);
    }

    // specify id (create expense with id from database)
    // datetime as String
    public Expense(int id, float amount, String description, Account account, Category category, String datetime) {
        this.id = id;
        this.amount = amount;
        this.description = description;
        this.account = account;
        this.category = category;
        this.datetime = MainActivity.getCalFromString(DATETIME_FORMAT, datetime);
    }

    // datetime as Calendar
    public Expense(int id, float amount, String description, Account account, Category category, Calendar datetime) {
        this.id = id;
        this.amount = amount;
        this.description = description;
        this.account = account;
        this.category = category;
        this.datetime = datetime;
    }

    // create NULL Expense for date header
    public Expense(Calendar datetime) {
        this.id = -1;
        this.amount = 0;
        this.description = "";
        this.account = null;
        this.category = null;
        this.datetime = datetime;
    }

    // null expense
    public Expense() {
        this.id = -1;
        this.amount = 0;
        this.description = "";
        this.account = null;
        this.category = null;
        this.datetime = Calendar.getInstance(MainActivity.locale);
    }

    /**
     * GETTERS
     */
    public Account getAccount() {
        return account;
    }
    public float getAmount() {
        return amount;
    }
    public Category getCategory() {
        return category;
    }
    public Calendar getDatetime() {
        return datetime;
    }
    public String getDatetimeStr() {
        Date date = datetime.getTime();
        DateFormat dateFormat = new SimpleDateFormat(DATETIME_FORMAT, MainActivity.locale);
        return dateFormat.format(date);
    }
    public String getDatetimeStr(String dtf) {
        Date date = datetime.getTime();
        DateFormat dateFormat = new SimpleDateFormat(dtf, MainActivity.locale);
        return dateFormat.format(date);
    }
    public String getDescription() {
        return description;
    }
    public int getId() {
        return id;
    }

    /**
     * SETTERS
     */
    public void setAccount(Account account) {
        this.account = account;
    }
    public void setCategory(Category category) {
        this.category = category;
    }
    public void setDatetime(Calendar datetime) {
        this.datetime = datetime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Expense expense = (Expense) o;
        return id == expense.id;
    }
    @Override
    public String toString() {
        return String.format("Expense %s: %.2f at %s (%s,%s)", description, amount, getDatetimeStr(), account.getName(), category.getName());
    }


}
