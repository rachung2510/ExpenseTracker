package com.example.expensetracker;

import java.util.ArrayList;
import java.util.HashMap;
import kotlin.Triple;

public class Constants {

    // Defaults
    public static final String[] defaultCatNames = new String[] { "Others",
            "Food", "Drinks", "Snacks",
            "Home/Living", "Groceries", "Taxi",
            "Transport", "Health", "Electronics",
            "Social", "Shopping", "Crafts",
            "Gifts", "Entertainment"
    };
    public static final String[] defaultCatIcons = new String[] { "cat_others",
            "cat_food", "cat_drinks", "cat_snacks",
            "cat_home", "cat_groceries", "cat_taxi",
            "cat_transport", "cat_health", "cat_electronics",
            "cat_social", "cat_shopping", "cat_crafts",
            "cat_gift", "cat_entertainment"
    };
    public static final String[] defaultCatColors = new String[] { "cat_storm_petrel",
            "cat_pastel_red", "cat_megaman", "cat_casandora_yellow",
            "cat_joust_blue", "cat_aqua_velvet", "cat_caribbean_green",
            "cat_fuel_town", "cat_dragon_skin", "cat_blue_ballerina",
            "cat_cyanite", "cat_lotus_pink", "cat_june_bud",
            "cat_fiery_fuchsia", "cat_nasu_purple"
    };
    public static final String[] defaultAccNames = new String[] { "Cash", "VISA", "NETS" };
    public static final String[] defaultAccIcons = new String[] { "acc_cash", "acc_visa", "acc_nets" };
    public static final String[] defaultAccColors = new String[] { "cat_pure_apple", "cat_bluebell", "cat_amour" };
    public static final String[] allColors = new String[] {
            "cat_amour", "cat_pastel_red", "cat_dragon_skin", "cat_casandora_yellow",
            "cat_june_bud", "cat_pure_apple", "cat_caribbean_green", "cat_mountain_meadow",
            "cat_jade_dust", "cat_megaman", "cat_cyanite", "cat_aqua_velvet",
            "cat_joust_blue", "cat_bleu_de_france", "cat_nasu_purple", "cat_bluebell",
            "cat_magenta_purple", "cat_fiery_fuchsia", "cat_lotus_pink", "cat_jigglypuff",
            "cat_blue_ballerina", "cat_storm_petrel", "cat_fuel_town", "cat_imperial_primer"
    };
    public static final String[] allAccIcons = new String[] { "acc_cash", "acc_visa", "acc_nets", "acc_dbs", "acc_hsbc", "acc_youtrip" };
    public static final String[] allCatIcons = new String[] {
            "cat_food", "cat_drinks", "cat_snacks", "cat_home", "cat_office", "cat_groceries", "cat_taxi",
            "cat_transport", "cat_health", "cat_electronics", "cat_social", "cat_shopping", "cat_clothes",
            "cat_beauty", "cat_crafts", "cat_gift", "cat_entertainment", "cat_music", "cat_others"
    };

    public static ArrayList<Currency> currencies = new ArrayList<>();
    public static HashMap<String, Currency> currency_map = new HashMap<>();
    static {
        currencies.add(new Currency("SGD","S$",.702f,"Singaporean dollar"));
        currencies.add(new Currency("GBP","£",1.117f,"British pound"));
        currencies.add(new Currency("USD","$",1f,"United States dollar"));
        currencies.add(new Currency("EUR","€",.98f,"Euro"));
        currencies.add(new Currency("CNY","￥",.141f,"Chinese yuan"));
        currencies.add(new Currency("JPY","¥",.007f,"Japanese yen"));
        for (Currency c : currencies) currency_map.put(c.getName(), c);
    }

    // Page modes
    public static final int HOME = 0;
    public static final int CHARTS = 1;
    public static final int MANAGE = 2;

    // Relative dates
    public static final int TODAY = 0;
    public static final int YESTERDAY = 1;
    public static final int THIS_WEEK = 2;
    public static final int LAST_WEEK = 3;

    // Account/Category
    public static final int SECTION_ACCOUNT = 0;
    public static final int SECTION_CATEGORY = 1;
    public static String defaultAccount = "Cash";
    public static String defaultCategory = "Others";

    // Date navigation
    public static final int PREV = -1;
    public static final int NEXT = 1;
    public static final int ASCENDING = 1;
    public static final int DESCENDING = -1;

    public static final String SETTINGS = "settings";
    public static final String TMP = "tmp";
}
