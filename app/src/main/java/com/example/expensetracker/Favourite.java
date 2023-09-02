package com.example.expensetracker;

import androidx.annotation.NonNull;

public class Favourite {
    private final String accName;
    private final String catName;
    private final String amount;
    private final String currencyName;

    public Favourite(String accName, String catName, String amount, String currencyName) {
        this.accName = accName;
        this.catName = catName;
        this.amount = amount;
        this.currencyName = currencyName;
    }

    @NonNull
    @Override
    public final String toString() {
        return String.format("Favourite: %s, %s, %s", accName, catName, amount);
    }

    public String getAccName() {
        return accName;
    }
    public String getCatName() {
        return catName;
    }
    public String getAmount() {
        return String.format(MainActivity.locale, "%.2f", Float.parseFloat(amount));
    }
    public String getCurrencyName() {
        return currencyName;
    }
}
