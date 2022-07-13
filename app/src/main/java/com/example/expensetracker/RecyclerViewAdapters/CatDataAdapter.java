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
import com.example.expensetracker.DatabaseHelper;
import com.example.expensetracker.MainActivity;
import com.example.expensetracker.R;

import java.util.ArrayList;

public class CatDataAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final ArrayList<Category> categories;
    private final Context context;
    private final LayoutInflater inflater;

    public CatDataAdapter(Context context) {
        this.context = context;
        inflater = LayoutInflater.from(context);
        DatabaseHelper db = ((MainActivity) context).db;
        categories = db.getAllCategories();
        categories.sort((cat1, cat2) -> {
            return Float.compare(db.getTotalAmtByCategory(cat2), db.getTotalAmtByCategory(cat1)); // descending order
        });
    }

    /**
     * OVERRIDEN METHODS
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
            populateCategories((ViewHolder) holder, cat, position);
        }
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    /**
     * VIEWHOLDER CLASS AND METHODS
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {

        View border;
        TextView catDataLabel, catDataNumExpenses, catDataAmt;
        ImageButton catDataIconBg;
        ImageView catDataIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            border = itemView.findViewById(R.id.border);
            catDataLabel = itemView.findViewById(R.id.catDataLabel);
            catDataNumExpenses = itemView.findViewById(R.id.catDataNumExpenses);
            catDataAmt = itemView.findViewById(R.id.catDataAmt);
            catDataIconBg = itemView.findViewById(R.id.catDataIconBg);
            catDataIcon = itemView.findViewById(R.id.catDataIcon);
        }
    }
    public void populateCategories(ViewHolder holder, Category cat, int position) {
        holder.catDataLabel.setText(cat.getName());
        holder.catDataIcon.setForeground(cat.getIcon());
        holder.catDataAmt.setText(String.format(MainActivity.locale,"%.2f", ((MainActivity) context).db.getTotalAmtByCategory(cat)));
        holder.catDataNumExpenses.setText(String.valueOf(((MainActivity) context).db.getNumExpensesByCategory(cat)));

        holder.border.setBackgroundColor(ContextCompat.getColor(context, cat.getColorId()));
        holder.catDataIconBg.setBackgroundTintList(MainActivity.getColorStateListFromId(context, cat.getColorId()));

        if (((MainActivity) context).db.getTotalAmtByCategory(cat) == 0f) {
            holder.itemView.setAlpha(0.3f);
        }
    }
}
