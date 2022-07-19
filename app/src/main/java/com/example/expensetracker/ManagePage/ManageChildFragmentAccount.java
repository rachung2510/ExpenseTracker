package com.example.expensetracker.ManagePage;

import android.content.Context;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.example.expensetracker.Constants;
import com.example.expensetracker.MainActivity;
import com.example.expensetracker.R;
import com.example.expensetracker.RecyclerViewAdapters.AccountAdapter;

public class ManageChildFragmentAccount extends ManageChildFragment<AccountAdapter> {

    public ManageChildFragmentAccount(Context context) {
        super(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manage_acc, container, false);
        adapter = ((MainActivity) getParentFragment().getActivity()).getAccountData(Constants.MANAGE);
        adapter.addNewAcc();
        sectionList = view.findViewById(R.id.accList);
        ((SimpleItemAnimator) sectionList.getItemAnimator()).setSupportsChangeAnimations(false);
        sectionList.setLayoutManager(new LinearLayoutManager(getParentFragment().getActivity()));
        setAdapter(adapter, ItemTouchHelper.UP | ItemTouchHelper.DOWN);
        setHasOptionsMenu(true);
        return view;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.select:
                adapter.setSelectionMode(true);
                getParentFragment().getActivity().startActionMode(actionModeCallback);
                return true;

            case R.id.resetDefault:
                MainActivity context = (MainActivity) getParentFragment().getActivity();
                AlertDialog.Builder confirmReset = new AlertDialog.Builder(context, R.style.ConfirmDelDialog);
                confirmReset.setTitle("Reset defaults");
                confirmReset.setMessage("Reset default accounts? Relevant expenses will be moved to " + ((MainActivity) context).getDefaultAccName() + ".");
                confirmReset.setPositiveButton("Reset", (dialogInterface, i) -> {
                    context.db.deleteAllAccounts();
                    context.initialiseDefaultAccs();
                    context.updateAccountData();
                    context.updateHomeData();
                    Toast.makeText(context, "Accounts reset to defaults", Toast.LENGTH_SHORT).show();
                });
                confirmReset.setNeutralButton(android.R.string.no, (dialog, which) -> {
                    dialog.cancel(); // close dialog
                });
                confirmReset.show();
                return true;

            case R.id.resetOrder:
                adapter.resetPositions();
                adapter.setList(((MainActivity) getActivity()).sortSections(adapter.getList()));
                adapter.notifyItemRangeChanged(0, adapter.getItemCount()-1);
                return true;

            default:
                return false;
        }
    }

    @Override
    public void bulkDelete(ActionMode mode) {
        if (adapter.getSelectedPos().isEmpty()) {
            Toast.makeText(context, "No account selected", Toast.LENGTH_SHORT).show();
            return;
        }
        AlertDialog.Builder confirmDel = new AlertDialog.Builder(context, R.style.ConfirmDelDialog);
        confirmDel.setTitle((adapter.getSelectedPos().size() == 1) ? "Delete account" : "Delete accounts");
        confirmDel.setMessage("Are you sure you want to delete? Relevant expenses will be moved to " + ((MainActivity) context).getDefaultAccName() + ".");
        confirmDel.setPositiveButton("Delete", (dialog, which) -> {
            for (int pos : adapter.getSelectedPos()) {
                ((MainActivity) context).db.deleteAccount(adapter.getList().get(pos), false);
            }
            ((MainActivity) context).updateAccountData();
            Toast.makeText(context, (adapter.getSelectedPos().size() == 1) ? "Account deleted" : "Accounts deleted", Toast.LENGTH_SHORT).show();
            mode.finish();
            ((MainActivity) context).updateHomeData(); // update summary & expense list
        });
        confirmDel.setNeutralButton(android.R.string.no, (dialog, which) -> dialog.cancel());
        confirmDel.show();
    }

    @Override
    public void updateData() {
        ((MainActivity) context).updateAccountData();
    }
}
