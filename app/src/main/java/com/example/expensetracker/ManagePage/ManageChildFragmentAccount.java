package com.example.expensetracker.ManagePage;

import android.content.Context;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.example.expensetracker.Constants;
import com.example.expensetracker.MainActivity;
import com.example.expensetracker.R;
import com.example.expensetracker.RecyclerViewAdapters.AccountAdapter;

import java.util.Objects;

public class ManageChildFragmentAccount extends ManageChildFragment<AccountAdapter> {

    public ManageChildFragmentAccount() {
        super();
    }
    public ManageChildFragmentAccount(Context context) {
        super(context);
        this.sectionType = Constants.SECTION_ACCOUNT;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (getActivity() == null)
            return null;
        View view = inflater.inflate(R.layout.fragment_manage_acc, container, false);
        sectionList = view.findViewById(R.id.accList);
        ((SimpleItemAnimator) Objects.requireNonNull(sectionList.getItemAnimator())).setSupportsChangeAnimations(false);
        sectionList.setLayoutManager(new LinearLayoutManager(getActivity()));
        updateView();
        // menu
        invalidateMenu();
        setHasOptionsMenu(true);
        return view;
    }
    @Override
    public void updateView() {
        if (getActivity() == null)
            return;
        adapter = ((MainActivity) getActivity()).getAccountData(Constants.MANAGE);
        adapter.addNewAcc();
        setAdapter(adapter, ItemTouchHelper.UP | ItemTouchHelper.DOWN);
    }
    @Override
    public void invalidateMenu() {
        if (getParentFragment() == null) return;
        ((ManageFragment) getParentFragment()).getToolbar().setOnMenuItemClickListener(menuItemClickListener);
    }

    public Toolbar.OnMenuItemClickListener menuItemClickListener = item -> {
        if (getActivity() == null)
            return false;
        int id = item.getItemId();
        if (id == R.id.select) {
            adapter.setSelectionMode(true);
            getActivity().startActionMode(actionModeCallback);
            return true;
        }
        if (id == R.id.resetDefault) {
            MainActivity context = (MainActivity) getActivity();
            int numExpenses = context.db.getNumExpensesNonDefaultAccount();
            AlertDialog.Builder confirmReset = new AlertDialog.Builder(context, R.style.ConfirmDelDialog);
            confirmReset.setTitle("Reset defaults")
                    .setMessage("Reset default accounts?" + ((numExpenses == 0) ? "" :
                            " " + numExpenses + " expense(s) from " + context.db.getNumAccountsNonDefault() +
                                    " account(s) will be moved to " + context.getDefaultAccName() + "."))
                    .setPositiveButton("Reset", (dialogInterface, i) -> {
                        context.resetDefaultAccs();
                        context.updateAccountData();
                        resetOrder();
                        context.setUpdateFragments(true);
                        Toast.makeText(context, "Accounts reset to defaults", Toast.LENGTH_SHORT).show();
                    })
                    .setNeutralButton(android.R.string.no, (dialog, which) -> dialog.cancel())
                    .show();
            return true;
        }
        if (id == R.id.resetOrder) {
            adapter.resetPositions();
            adapter.setList(((MainActivity) getActivity()).sortSections(adapter.getList()));
            adapter.notifyItemRangeChanged(0, adapter.getItemCount()-1);
            return true;
        }
        return false;
    };

    @Override
    public void bulkDelete(ActionMode mode) {
        if (adapter.getSelectedPos().isEmpty()) {
            Toast.makeText(context, "No account selected", Toast.LENGTH_SHORT).show();
            return;
        }
        if (getActivity() == null)
            return;
        MainActivity context = (MainActivity) getActivity();
        int numExpenses = context.db.getNumExpensesByAccounts(adapter.getAllSelected());
        AlertDialog.Builder confirmDel = new AlertDialog.Builder(context, R.style.ConfirmDelDialog);
        confirmDel.setTitle((adapter.getSelectedPos().size() == 1) ? "Delete account" : "Delete accounts")
                .setMessage("Are you sure you want to delete?" + ((numExpenses == 0) ? "" :
                " " + numExpenses + " expense(s) will be moved to " + context.getDefaultAccName() + "."))
                .setPositiveButton("Delete", (dialog, which) -> {
                    for (int pos : adapter.getSelectedPos())
                        context.db.deleteAccount(adapter.getList().get(pos), false);
                    context.updateAccountData();
                    Toast.makeText(context, (adapter.getSelectedPos().size() == 1) ? "Account deleted" : "Accounts deleted", Toast.LENGTH_SHORT).show();
                    mode.finish();
                    context.updateHomeData(); // update summary & expense list
                })
                .setNeutralButton(android.R.string.no, (dialog, which) -> dialog.cancel())
                .show();
    }

    @Override
    public void updateData() {
        ((MainActivity) context).updateAccountData();
    }

    public void resetOrder() {
        adapter.resetPositions();
        adapter.setList(((MainActivity) getActivity()).sortSections(adapter.getList()));
        adapter.notifyItemRangeChanged(0, adapter.getItemCount()-1);
    }
}
