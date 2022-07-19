package com.example.expensetracker.RecyclerViewAdapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensetracker.Category;
import com.example.expensetracker.Currency;
import com.example.expensetracker.DatabaseHelper;
import com.example.expensetracker.MainActivity;
import com.example.expensetracker.R;

import java.util.ArrayList;
import java.util.Calendar;

public class CatDataAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final ArrayList<Category> categories;
    private final Context context;
    private final LayoutInflater inflater;
    private final DatabaseHelper db;
    private final Calendar fromDate, toDate;

    public CatDataAdapter(Context context, Calendar fromDate, Calendar toDate) {
        this.context = context;
        inflater = LayoutInflater.from(context);
        this.db = ((MainActivity) context).db;
        this.fromDate = fromDate;
        this.toDate = toDate;
        categories = db.getAllCategories();
        if (fromDate == null)
            categories.sort((cat1, cat2) -> {
                return Float.compare(db.getTotalAmtByCategory(cat2), db.getTotalAmtByCategory(cat1)); // descending order
            });
        else
            categories.sort((cat1, cat2) -> {
                return Float.compare(db.getTotalAmtByCategoryInRange(cat2, fromDate, toDate), db.getTotalAmtByCategoryInRange(cat1, fromDate, toDate)); // descending order
            });

    }

    /**
     * INITIALISE ADAPTER
     */
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_cat_data, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Category cat = categories.get(position);
        if (holder instanceof  ViewHolder) {
            populateCategories((ViewHolder) holder, cat);
        }
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    /**
     * VIEWHOLDER CLASS
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        View border;
        TextView catDataLabel, catDataNumExpenses, catDataAmt, catDataCurr;
        ImageButton catDataIconBg;
        ImageView catDataIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            border = itemView.findViewById(R.id.border);
            catDataLabel = itemView.findViewById(R.id.catDataLabel);
            catDataNumExpenses = itemView.findViewById(R.id.catDataNumExpenses);
            catDataAmt = itemView.findViewById(R.id.catDataAmt);
            catDataCurr = itemView.findViewById(R.id.catDataCurr);
            catDataIconBg = itemView.findViewById(R.id.catDataIconBg);
            catDataIcon = itemView.findViewById(R.id.catDataIcon);
        }
    }
    public void populateCategories(ViewHolder holder, Category cat) {
        holder.catDataLabel.setText(cat.getName());
        holder.catDataIcon.setForeground(cat.getIcon());
        holder.catDataCurr.setText(new Currency(context).getSymbol());
        float totalAmt;
        if (fromDate == null) {
            totalAmt = ((MainActivity) context).db.getTotalAmtByCategory(cat);
            holder.catDataNumExpenses.setText(String.valueOf(((MainActivity) context).db.getNumExpensesByCategory(cat)));
        } else {
            totalAmt = ((MainActivity) context).db.getTotalAmtByCategoryInRange(cat, fromDate, toDate);
            holder.catDataNumExpenses.setText(String.valueOf(((MainActivity) context).db.getNumExpensesByCategoryInRange(cat, fromDate, toDate)));
        }
        holder.catDataAmt.setText(String.format(MainActivity.locale, "%.2f", totalAmt));

        holder.border.setBackgroundColor(ContextCompat.getColor(context, cat.getColorId()));
        holder.catDataIconBg.setBackgroundTintList(MainActivity.getColorStateListFromId(context, cat.getColorId()));

        if (totalAmt == 0f) {
            holder.itemView.setAlpha(0.3f);
        }
    }
}
