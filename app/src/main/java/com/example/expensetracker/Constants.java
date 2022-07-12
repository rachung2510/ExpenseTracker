package com.example.expensetracker;

public class Constants {

    // Defaults
    public static final String[] defaultCatNames = new String[] { "Food", "Gift", "Entertainment", "Transport", "Office", "Electronics",
            "Social", "Online Shopping", "Crafts", "Beauty", "Others" };
    public static final String[] defaultCatIcons = new String[] { "cat_food", "cat_gift", "cat_entertainment", "cat_transport", "cat_office", "cat_electronics",
            "cat_social", "cat_online_shopping", "cat_crafts", "cat_beauty", "cat_others"
    };
    public static final String[] defaultAccNames = new String[] { "Cash", "VISA", "NETS" };
    public static final String[] defaultAccIcons = new String[] { "acc_cash", "acc_visa", "acc_nets" };
    public static final String[] defaultColors = new String[] {
            "cat_jigglypuff", "cat_lotus_pink", "cat_casandora_yellow",
            "cat_dragon_skin", "cat_pastel_red", "cat_amour", "cat_megaman", "cat_cyanite",
            "cat_caribbean_green", "cat_mountain_meadow", "cat_jade_dust", "cat_aqua_velvet",
            "cat_joust_blue", "cat_bleu_de_france", "cat_nasu_purple", "cat_bluebell",
            "cat_blue_ballerina", "cat_storm_petrel", "cat_fuel_town", "cat_imperial_primer",
            "cat_june_bud", "cat_pure_apple", "cat_fiery_fuchsia", "cat_magenta_purple"
    };

    // Page modes
    public static final int HOME = 0;
    public static final int MANAGE = 1;

    // Relative dates
    public static final int TODAY = 0;
    public static final int YESTERDAY = 1;
    public static final int THIS_WEEK = 2;
    public static final int LAST_WEEK = 3;

    // Account/Category
    public static final int SECTION_ACCOUNT = 0;
    public static final int SECTION_CATEGORY = 1;
}
