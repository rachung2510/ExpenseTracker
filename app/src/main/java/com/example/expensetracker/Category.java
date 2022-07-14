package com.example.expensetracker;

import android.content.Context;

public class Category extends Section {

    /**
     * CONSTRUCTORS
     */
    // no id given - create basic Category
    public Category (Context context, String name, String icon, String color) {
        super(context, name, icon, color);
    }

    // specify id - recall Category from database
    public Category(Context context, int id, String name, String icon, String color) {
        super(context, id, name, icon, color);
    }

    // NULL category
    public Category(Context context) {
        super(context);
    }

    /**
     * FUNCTIONS
     */
    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;

        if (!(o instanceof Category))
            return false;

        Category cat = (Category) o;
        return this.getId() == cat.getId()
                && this.getName().equals(cat.getName())
                && this.getIconName().equals(cat.getIconName())
                && this.getColorName().equals(cat.getColorName());
    }
}
