package com.example.expensetracker;

import android.content.Context;
import android.util.Log;

import com.example.expensetracker.Widget.WidgetDialogActivity;

public class Currency {
    private static final String TAG = "Currency";
    private final String name;
    private final String description;
    private final String symbol;
    private float xrate;

    public Currency(String name, String symbol, float xrate, String description) {
        this.name = name;
        this.description = description;
        this.symbol = symbol;
        this.xrate = xrate;
    }
    public Currency(Context context) {
        String defaultCurrency = "";
        if (context instanceof MainActivity)
            defaultCurrency = ((MainActivity) context).getDefaultCurrency();
        else if (context instanceof WidgetDialogActivity) {
            defaultCurrency = ((WidgetDialogActivity) context).getDefaultCurrency();
        }
        this.name = defaultCurrency;
        if (!defaultCurrency.isEmpty()) {
            this.description = Constants.currency_map.get(defaultCurrency).getDescription();
            this.symbol = Constants.currency_map.get(defaultCurrency).getSymbol();
            this.xrate = Constants.currency_map.get(defaultCurrency).getRate();
        } else {
            this.description = "";
            this.symbol = "";
            this.xrate = 1f;
        }
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
    public float getRate() {
        return xrate;
    }
    public void setRate(float xrate) {
        this.xrate = xrate;
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
