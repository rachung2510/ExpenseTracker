package com.example.expensetracker;

import android.content.Context;
import android.graphics.drawable.Drawable;

public class Section {
    protected Context context;
    protected int id;
    protected String name;
    protected String icon;
    protected String color;
    protected int position;

    /**
     * CONSTRUCTORS
     */
    // no id given - create basic Section
    public Section(Context context, String name, String icon, String color, int pos) {
        this.context = context;
        this.id = -1;
        this.name = name;
        this.icon = icon;
        this.color = color;
        this.position = pos;
    }

    // specify id - recall Section from database
    public Section(Context context, int id, String name, String icon, String color, int position) {
        this.context = context;
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.color = color;
        this.position = position;
    }

    // NULL Section
    public Section(Context context) {
        this.context = context;
        this.id = -1;
        this.name = "";
        this.icon = "";
        this.color = "";
        this.position = -1;
    }

    /**
     * GETTERS
     */
    public int getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public String getIconName() {
        return icon;
    }
    public String getColorName() {
        return color;
    }
    public int getPosition() { return position; }
    public int getIconId() {
        return context.getResources().getIdentifier(icon, "drawable", context.getPackageName());
    }
    public int getColorId() {
        return context.getResources().getIdentifier(color, "color", context.getPackageName());
    }
    public Drawable getIcon() {
        return MainActivity.getIconFromName(context, icon);
    }
    public String getColorHex() {
        return MainActivity.getColorHexFromName(context, color);
    }

    /**
     * SETTERS
     */
    public void setId(int id) {
        this.id = id;
    }
    public void setName(String name) {
        this.name = name;
    }
    public void setPosition(int pos) { this.position = pos; }
}
