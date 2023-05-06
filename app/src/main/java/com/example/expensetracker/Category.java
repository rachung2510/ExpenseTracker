package com.example.expensetracker;

import android.content.Context;

import androidx.annotation.NonNull;

public class Category extends Section {

    private float amount = -1;
    private int numExpenses = -1;

    /**
     * Constructors
     */
    // no id given - create basic Category
    public Category(Context context, String name, String icon, String color, int pos) {
        super(context, name, icon, color, pos);
    }

    // specify id - recall Category from database
    public Category(Context context, int id, String name, String icon, String color, int pos) {
        super(context, id, name, icon, color, pos);
    }

    // NULL category
    public Category(Context context) {
        super(context);
    }

    /**
     * Functions
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
    @SuppressWarnings("unchecked")
    @Override
    public Category copy() {
        return new Category(context, id, name, icon, color, position);
    }
    @NonNull
    @Override
    public String toString() {
        return String.format(MainActivity.locale, "Category %d: %s (%s,%s)", id, name, icon, color);
    }

    /**
     * Getters/setters
     */
    public void setAmount(Float amount) {
        this.amount = amount;
    }
    public float getAmount() {
        return this.amount;
    }
    public int getNumExpenses() {
        return numExpenses;
    }
    public void setNumExpenses(int numExpenses) {
        this.numExpenses = numExpenses;
    }
}
