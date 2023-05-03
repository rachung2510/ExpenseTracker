package com.example.expensetracker;

public class ReceiptItem {
    private final String description;
    private float amount;
    private String catName;

    public ReceiptItem(String description, float amount) {
        this.description = description;
        this.amount = amount;
        this.catName = "";
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }
    public void setCatName(String catName) {
        this.catName = catName;
    }

    public float getAmount() {
        return amount;
    }
    public String getCatName() {
        return catName;
    }
    public String getDescription() {
        return description;
    }

}
