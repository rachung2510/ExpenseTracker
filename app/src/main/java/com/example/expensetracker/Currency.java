package com.example.expensetracker;

import kotlin.Triple;

public class Currency {
    private static final String TAG = "Currency";
    private String name;
    private String description;
    private String symbol;

    public Currency(String name, String description, String symbol) {
        this.name = name;
        this.description = description;
        this.symbol = symbol;
    }
    public Currency() {
        Triple<String, String, String> triple = Constants.currencies.get(0);
        this.name = triple.getFirst();
        this.description = triple.getThird();
        this.symbol = triple.getSecond();
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
