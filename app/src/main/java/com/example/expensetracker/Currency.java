package com.example.expensetracker;

import android.content.Context;

public class Currency {
    private static final String TAG = "Currency";
    private final String name;
    private final String description;
    private final String symbol;

    public Currency(String name, String description, String symbol) {
        this.name = name;
        this.description = description;
        this.symbol = symbol;
    }
    public Currency(Context context) {
        String defaultCurrency = ((MainActivity) context).getDefaultCurrency();
        this.name = defaultCurrency;
        this.description = "";
        this.symbol = Constants.currency_map.get(defaultCurrency);
    }

    public String getName() {
        return name;
    }
    public String getDescription() {
        return description;
    }
    public String getSymbol() {
        return symbol;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (o instanceof String)
            return this.getName().equals(o);
        else if (o instanceof Currency)
            return this.getName().equals(((Currency) o).getName());
        else
            return false;
    }
}
