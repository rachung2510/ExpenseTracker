package com.example.expensetracker;

import android.content.Context;

public class Account extends Section {

    private final Currency currency;

    /**
     * CONSTRUCTORS
     */
    // no id given - create basic Account
    public Account(Context context, String name, String icon, String color, int pos) {
        super(context, name, icon, color, pos);
        this.currency = new Currency(context);
    }
    public Account(Context context, String name, String icon, String color, int pos, Currency currency) {
        super(context, name, icon, color, pos);
        this.currency = currency;
    }

    // specify id - recall Account from database
    public Account(Context context, int id, String name, String icon, String color, int pos) {
        super(context, id, name, icon, color, pos);
        this.currency = new Currency(context);
    }
    public Account(Context context, int id, String name, String icon, String color, int pos, Currency currency) {
        super(context, id, name, icon, color, pos);
        this.currency = currency;
    }

    // NULL category
    public Account(Context context) {
        super(context);
        this.currency = new Currency(context);
    }

    /**
     * FUNCTIONS
     */
    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;

        if (!(o instanceof Account))
            return false;

        Account acc = (Account) o;
        return this.getId() == acc.getId()
                && this.getName().equals(acc.getName())
                && this.getIconName().equals(acc.getIconName())
                && this.getColorName().equals(acc.getColorName());
    }

    /**
     * GETTERS/SETTERS
     */
    public String getCurrencyName() {
        return currency.getName();
    }
    public String getCurrencySymbol() {
        return currency.getSymbol();
    }
}
