package com.example.expensetracker;

import android.content.Context;

public class Account extends Section {

    /**
     * CONSTRUCTORS
     */
    // no id given - create basic Category
    public Account(Context context, String name, String icon, String color) {
        super(context, name, icon, color);
    }

    // specify id - recall Category from database
    public Account(Context context, int id, String name, String icon, String color) {
        super(context, id, name, icon, color);
    }

    // NULL category
    public Account() {
        super();
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
}
