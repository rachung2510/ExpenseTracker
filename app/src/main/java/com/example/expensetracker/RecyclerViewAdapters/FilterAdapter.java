package com.example.expensetracker.RecyclerViewAdapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensetracker.Account;
import com.example.expensetracker.Category;
import com.example.expensetracker.ChartsPage.ChartsChildFragmentGraph;
import com.example.expensetracker.ChartsPage.ChartsFragment;
import com.example.expensetracker.Constants;
import com.example.expensetracker.MainActivity;
import com.example.expensetracker.R;

import java.util.ArrayList;

public class FilterAdapter extends RecyclerView.Adapter<FilterAdapter.ViewHolder> {

    private static final String TAG = "FilterAdapter";
    private final ArrayList<Account> filteredAccounts;
    private final ArrayList<Category> filteredCategories;
    private final Context context;
    private final LayoutInflater inflater;

    public final ColorStateList tagGray, tagOrange;

    /**
     * Constructor
     */
    public FilterAdapter(Context context, ArrayList<Account> filteredAccounts, ArrayList<Category> filteredCategories) {
        this.filteredAccounts = filteredAccounts;
        this.filteredCategories = filteredCategories;
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        tagGray = MainActivity.getColorStateListFromId(context, R.color.tag_text_gray);
        tagOrange = MainActivity.getColorStateListFromId(context, R.color.tag_icon_orange);
    }

    /**
     * Initialise adapter
     */
    @NonNull
    @Override
    public FilterAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_section_filter, parent, false);
        return new ViewHolder(view);
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public void onBindViewHolder(@NonNull FilterAdapter.ViewHolder holder, int position) {
        int sectionType;
        if (position < filteredAccounts.size()) {
            sectionType = Constants.SECTION_ACCOUNT; // Account
            Account acc = filteredAccounts.get(position);
            holder.filterItem.setBackgroundTintList(MainActivity.getColorStateListFromId(context, R.color.tag_bg_gray));
            holder.filterItemName.setText(acc.getName());
            holder.filterItemIcon.setVisibility(ImageView.GONE);

            holder.filterItemName.setTextColor(MainActivity.getColorFromId(context, R.color.tag_text_gray));
            holder.filterItemIcon.setForegroundTintList(tagGray);
            holder.delFilterItem.setForegroundTintList(tagGray);
        } else {
            sectionType = Constants.SECTION_CATEGORY; // Category
            position -= filteredAccounts.size();
            Category cat = filteredCategories.get(position);
            holder.filterItem.setBackgroundTintList(null);
            holder.filterItem.setBackgroundTintList(MainActivity.getColorStateListFromId(context, R.color.tag_bg_orange));
            holder.filterItemName.setText(cat.getName());
            holder.filterItemIcon.setForeground(cat.getIcon());
            holder.filterItemIcon.setVisibility(ImageView.VISIBLE);

            holder.filterItemName.setTextColor(MainActivity.getColorFromId(context, R.color.tag_text_orange));
            holder.filterItemIcon.setForegroundTintList(tagOrange);
            holder.delFilterItem.setForegroundTintList(tagOrange);
        }

//        MainActivity.logFilters(TAG, filteredCategories, "cats");
        int finalPos = position;
        holder.delFilterItem.setOnClickListener((l) -> {
            if (sectionType == Constants.SECTION_ACCOUNT) {
                filteredAccounts.remove(finalPos);
                ((MainActivity) context).updateAccFilters(filteredAccounts);
            } else if (sectionType == Constants.SECTION_CATEGORY) {
                filteredCategories.remove(finalPos);
                ((MainActivity) context).updateCatFilters(filteredCategories);
            }
            if (((MainActivity) context).getCurrentFragment() instanceof ChartsFragment) {
                ChartsChildFragmentGraph frag = ((ChartsFragment) ((MainActivity) context).getCurrentFragment()).getChildFragmentGraph();
                frag.applyFilters(true);
            }
        });
    }

    @Override
    public int getItemCount() {
        return filteredAccounts.size() + filteredCategories.size();
    }

    /**
     * Viewholder class
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout filterItem;
        ImageView filterItemIcon, delFilterItem;
        TextView filterItemName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            filterItem = itemView.findViewById(R.id.filterItem);
            filterItemIcon = itemView.findViewById(R.id.filterItemIcon);
            filterItemName = itemView.findViewById(R.id.filterItemName);
            delFilterItem = itemView.findViewById(R.id.delFilterItem);
        }
    }

}

