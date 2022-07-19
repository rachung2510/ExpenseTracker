package com.example.expensetracker.RecyclerViewAdapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensetracker.Account;
import com.example.expensetracker.Constants;
import com.example.expensetracker.MainActivity;
import com.example.expensetracker.R;

import java.util.ArrayList;

public class AccountAdapter extends SectionAdapter<Account> {

    private static final String TAG = "AccountAdapter";

    /**
     * CONSTRUCTOR
     */
    public AccountAdapter(Context context, ArrayList<Account> accounts) {
        super(context, accounts);
    }
    public AccountAdapter(Context context, ArrayList<Account> accounts, int mode) {
        super(context, accounts, mode);
    }

    /**
     * INITIALISE ADAPTER
     */
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (page == Constants.MANAGE) {
            View view = inflater.inflate(R.layout.item_section_list, parent, false);
            return new ListViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_section_grid, parent, false);
            return new GridViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof SectionAdapter.GridViewHolder) {
            Account acc = sections.get(position);
            populateSectionGrid((GridViewHolder) holder, acc, position);
        } else if (holder instanceof SectionAdapter.ListViewHolder) {
            ((ListViewHolder) holder).setPosition(position);
            populateList((ListViewHolder) holder, sections.get(position), position);
        }
    }

    /**
     * FUNCTIONS
     */
    public void addNewAcc() {
        sections.add(new Account(context));
        notifyItemInserted(sections.size()-1);
    }
    @Override
    public void resetPositions() {
        for (Account acc : sections) {
            acc.setPosition(acc.getId() - 1);
            ((MainActivity) context).db.updateAccount(acc);
        }
    }
    public void updatePositions() {
        for (int i = 0;i < getItemCount()-1;i++) {
            Account acc = sections.get(i);
            acc.setPosition(i);
            ((MainActivity) context).db.updateAccount(acc);
        }
    }

}
