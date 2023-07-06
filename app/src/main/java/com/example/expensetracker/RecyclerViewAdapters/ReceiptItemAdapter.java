package com.example.expensetracker.RecyclerViewAdapters;

import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensetracker.Category;
import com.example.expensetracker.DatabaseHelper;
import com.example.expensetracker.HelperClasses.MoneyValueFilter;
import com.example.expensetracker.MainActivity;
import com.example.expensetracker.R;
import com.example.expensetracker.ReceiptItem;
import com.example.expensetracker.Widget.WidgetDialogActivity;
import com.example.expensetracker.Widget.WidgetStaticActivity;

import java.util.ArrayList;
import java.util.HashMap;

public class ReceiptItemAdapter extends RecyclerView.Adapter<ReceiptItemAdapter.ViewHolder> {

    private static final String TAG = "ReceiptItemAdapter";

    private final Context context;
    private final ArrayList<ReceiptItem> receiptItems;
    private final String receiptCurr;
    private Category receiptCat;

    /**
     * Constructor
     */
    public ReceiptItemAdapter(Context context, ArrayList<ReceiptItem> receiptItems, String receiptCurr) {
        this.context = context;
        this.receiptItems = receiptItems;
        this.receiptCurr = receiptCurr;
        if (context instanceof MainActivity)
            this.receiptCat = ((MainActivity) context).db.getCategory(((MainActivity) context).getDefaultCatName());
        if (context instanceof WidgetStaticActivity)
            this.receiptCat = ((WidgetStaticActivity) context).db.getCategory(((WidgetStaticActivity) context).getDefaultCatName());
        if (context instanceof WidgetDialogActivity)
            this.receiptCat = ((WidgetDialogActivity) context).db.getCategory(((WidgetDialogActivity) context).getDefaultCatName());
    }

    /**
     * Initialise adapter
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(this.context);
        View view = inflater.inflate(R.layout.item_receipt, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        populateRecyclerView(holder, position);
    }

    @Override
    public int getItemCount() {
        return receiptItems.size();
    }

    /**
     * Viewholder class
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        RelativeLayout receiptItemLayout;
        TextView receiptItemCurr, receiptItemDesc;
        EditText receiptItemAmt;
        ImageButton receiptItemCat;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            receiptItemCurr = itemView.findViewById(R.id.receiptItemCurrency);
            receiptItemAmt = itemView.findViewById(R.id.receiptItemAmt);
            receiptItemDesc = itemView.findViewById(R.id.receiptItemDesc);
            receiptItemCat = itemView.findViewById(R.id.receiptItemCat);
            receiptItemLayout = itemView.findViewById(R.id.receiptItemLayout);
        }
    }

    public void populateRecyclerView(ReceiptItemAdapter.ViewHolder holder, int position) {
        ReceiptItem item = receiptItems.get(position);
        holder.receiptItemCurr.setText(receiptCurr);
        holder.receiptItemDesc.setText(item.getDescription());
        holder.receiptItemAmt.setText(String.format(MainActivity.locale, "%.2f", item.getAmount()));
        holder.receiptItemAmt.setFilters(new InputFilter[] { new MoneyValueFilter() });
        holder.receiptItemAmt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.toString().isEmpty()) return;
                item.setAmount(Float.parseFloat(editable.toString()));
            }
        });
        String catName = item.getCatName();
        if (!(catName.isEmpty())) {
            Category receiptItemCat =  getDb().getCategory(catName);
            holder.receiptItemCat.setForeground(receiptItemCat.getIcon());
            holder.receiptItemCat.setBackgroundTintList(MainActivity.getColorStateListFromHex(receiptItemCat.getColorHex()));
        }
        View.OnClickListener onClickListener = view -> {
            if (!(item.getCatName().isEmpty()) && item.getCatName().equals(receiptCat.getName())) {
                holder.receiptItemCat.setBackground(MainActivity.getIconFromId(context, R.drawable.shape_circle_border_transparent));
                holder.receiptItemCat.setBackgroundTintList(null);
                holder.receiptItemCat.setForeground(null);
                item.setCatName("");
            } else {
                holder.receiptItemCat.setBackgroundTintList(MainActivity.getColorStateListFromHex(receiptCat.getColorHex()));
                holder.receiptItemCat.setForeground(receiptCat.getIcon());
                item.setCatName(receiptCat.getName());
            }
        };
        holder.receiptItemCat.setOnClickListener(onClickListener);
        holder.itemView.setOnClickListener(onClickListener);
    }

    /**
     * Getters & Setters
     */
    public void setReceiptCat(Category cat) {
        this.receiptCat = cat;
    }
    public Category getReceiptCat() {
        return receiptCat;
    }
    public ArrayList<ReceiptItem> getReceiptItems() {
        return receiptItems;
    }
    public HashMap<String,Float> getReceiptCatAmts() {
        HashMap<String,Float> catAmtMap = new HashMap<>();
        for (ReceiptItem item : receiptItems) {
            String cat = item.getCatName();
            if (cat.isEmpty()) continue;
            float catAmt = 0f;
            if (catAmtMap.containsKey(cat)) catAmt = catAmtMap.get(cat);
            catAmt += item.getAmount();
            catAmtMap.put(cat, catAmt);
        }
        return catAmtMap;
    }
    public String getReceiptCurr() {
        return receiptCurr;
    }

    public float getTotalAmt() {
        float totalAmt = 0;
        for (ReceiptItem item : receiptItems) {
            if (!(item.getCatName().isEmpty())) totalAmt += item.getAmount();
        }
        return totalAmt;
    }
    public DatabaseHelper getDb() {
        if (context instanceof MainActivity) return ((MainActivity) context).db;
        if (context instanceof WidgetStaticActivity) return ((WidgetStaticActivity) context).db;
        if (context instanceof WidgetDialogActivity) return ((WidgetDialogActivity) context).db;
        return null;
    }
}
