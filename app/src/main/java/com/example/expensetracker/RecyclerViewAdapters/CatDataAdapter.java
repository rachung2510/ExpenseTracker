package com.example.expensetracker.RecyclerViewAdapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensetracker.Category;
import com.example.expensetracker.ChartsPage.ChartsChildFragmentPie;
import com.example.expensetracker.ChartsPage.ChartsFragment;
import com.example.expensetracker.Constants;
import com.example.expensetracker.HomePage.HomeFragment;
import com.example.expensetracker.MainActivity;
import com.example.expensetracker.R;

import java.util.ArrayList;
import java.util.Collections;

public class CatDataAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "CatDataAdapter";
    private final ArrayList<Category> categories;
    private final Context context;
    private final LayoutInflater inflater;

    public CatDataAdapter(Context context, ArrayList<Category> categories) {
        this.context = context;
        inflater = LayoutInflater.from(context);
        this.categories = categories;
    }

    /**
     * Initialise adapter
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
        if (holder instanceof ViewHolder) {
            populateCategories((ViewHolder) holder, cat);
        }
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    /**
     * Viewholder class
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        View border;
        TextView catDataLabel, catDataNumExpenses, catDataAmt, catDataCurr;
        ImageButton catDataIconBg;
        ImageView catDataIcon;
        LinearLayout catDataExpenses;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            border = itemView.findViewById(R.id.border);
            catDataLabel = itemView.findViewById(R.id.catDataLabel);
            catDataNumExpenses = itemView.findViewById(R.id.catDataNumExpenses);
            catDataAmt = itemView.findViewById(R.id.catDataAmt);
            catDataCurr = itemView.findViewById(R.id.catDataCurr);
            catDataIconBg = itemView.findViewById(R.id.catDataIconBg);
            catDataIcon = itemView.findViewById(R.id.catDataIcon);
            catDataExpenses = itemView.findViewById(R.id.catDataExpenses);
        }
    }
    public void populateCategories(ViewHolder holder, Category cat) {
        holder.catDataLabel.setText(cat.getName());
        holder.catDataIcon.setForeground(cat.getIcon());
        String currencySymbol = ((MainActivity) context).getDefaultCurrencySymbol();
        holder.catDataCurr.setText(currencySymbol);
        holder.catDataAmt.setText(String.format(MainActivity.locale, "%.2f", cat.getAmount()));
        holder.catDataNumExpenses.setText(String.valueOf(cat.getNumExpenses()));

        holder.border.setBackgroundColor(cat.getColor());
        holder.catDataIconBg.setBackgroundTintList(MainActivity.getColorStateListFromHex(cat.getColorHex()));

        if (cat.getAmount() == 0f) {
            holder.itemView.setAlpha(0.3f);
            return;
        }

        holder.catDataExpenses.setOnClickListener(view -> {
            ArrayList<Category> catFilter = new ArrayList<>(Collections.singletonList(cat));
            HomeFragment homeFrag = (HomeFragment) ((MainActivity) context).getFragment(Constants.HOME);
            ChartsFragment chartsFrag = (ChartsFragment) ((MainActivity) context).getFragment(Constants.CHARTS);
            homeFrag.setSelCatFilters(MainActivity.clone(catFilter));
            homeFrag.setDateRange(chartsFrag.getDateRange(), chartsFrag.getSelDatePos(), chartsFrag.getSelDateState());
            ((MainActivity) context).updateHomeData();
            ((MainActivity) context).goToFragment(Constants.HOME);
        });
        holder.itemView.setOnClickListener(view -> {
            ChartsChildFragmentPie fragment = ((ChartsFragment) ((MainActivity) context).getCurrentFragment()).getChildFragmentPie();
            if (fragment.isPieHighlighted(cat.getName()))
                fragment.clearPieHighlights();
            else
                fragment.highlightPieValue(cat.getName());
        });

    }
}
