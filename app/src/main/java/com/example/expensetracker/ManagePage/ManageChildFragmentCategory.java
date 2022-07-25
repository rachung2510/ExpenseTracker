package com.example.expensetracker.ManagePage;

import android.content.Context;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.example.expensetracker.Constants;
import com.example.expensetracker.MainActivity;
import com.example.expensetracker.R;
import com.example.expensetracker.RecyclerViewAdapters.CategoryAdapter;

import java.util.Objects;

public class ManageChildFragmentCategory extends ManageChildFragment<CategoryAdapter> {

    public ManageChildFragmentCategory(Context context) {
        super(context);
        this.sectionType = Constants.SECTION_CATEGORY;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (getActivity() == null)
            return null;
        View view = inflater.inflate(R.layout.fragment_manage_cat, container, false);
        sectionList = view.findViewById(R.id.catGrid);
        ((SimpleItemAnimator) Objects.requireNonNull(sectionList.getItemAnimator())).setSupportsChangeAnimations(false);
        sectionList.setLayoutManager(new GridLayoutManager(getActivity(), 3, GridLayoutManager.VERTICAL, false));
        updateView();
        invalidateMenu();
        setHasOptionsMenu(true);
        return view;
    }
    @Override
    public void updateView() {
        if (getActivity() == null)
            return;
        adapter = ((MainActivity) getActivity()).getCategoryData(Constants.MANAGE);
        adapter.addNewCat();
        setAdapter(adapter, ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
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
            int numExpenses = context.db.getNumExpensesNonDefaultCategory();
            AlertDialog.Builder confirmReset = new AlertDialog.Builder(context, R.style.ConfirmDelDialog);
            confirmReset.setTitle("Reset defaults")
                    .setMessage("Reset default categories?" + ((numExpenses == 0) ? "" :
                            " " + numExpenses + " expense(s) from " + context.db.getNumCategoriesNonDefault() +
                                    " categorie(s) will be moved to " + context.getImmutableCat() + "."))
                    .setPositiveButton("Reset", (dialogInterface, i) -> {
                        context.resetDefaultCats();
                        context.updateCategoryData();
                        resetOrder();
                        context.updateAllExpenseData();
                        Toast.makeText(context, "Categories reset to defaults", Toast.LENGTH_SHORT).show();
                    })
                    .setNeutralButton(android.R.string.no, (dialog, which) -> dialog.cancel())
                    .show();
            return true;
        }
        if (id == R.id.resetOrder) {
            resetOrder();
            return true;
        }
        return false;
    };
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem menuItem) {
        if (getActivity() == null)
            return false;
        int id = menuItem.getItemId();
        if (id == R.id.select) {
            adapter.setSelectionMode(true);
            getActivity().startActionMode(actionModeCallback);
            return true;
        }
        if (id == R.id.resetDefault) {
            MainActivity context = (MainActivity) getActivity();
            int numExpenses = context.db.getNumExpensesNonDefaultCategory();
            AlertDialog.Builder confirmReset = new AlertDialog.Builder(context, R.style.ConfirmDelDialog);
            confirmReset.setTitle("Reset defaults")
                    .setMessage("Reset default categories?" + ((numExpenses == 0) ? "" :
                            " " + numExpenses + " expense(s) from " + context.db.getNumCategoriesNonDefault() +
                            " categorie(s) will be moved to " + context.getImmutableCat() + "."))
                    .setPositiveButton("Reset", (dialogInterface, i) -> {
                        context.resetDefaultCats();
                        context.updateCategoryData();
                        Toast.makeText(context, "Categories reset to defaults", Toast.LENGTH_SHORT).show();
                    })
                    .setNeutralButton(android.R.string.no, (dialog, which) -> dialog.cancel())
                            .show();
            return true;
        }
        if (id == R.id.resetOrder) {
            adapter.resetPositions();
            adapter.setList(((MainActivity) getActivity()).sortSections(adapter.getList()));
            adapter.notifyItemRangeChanged(0, adapter.getItemCount()-2);
            return true;
        }
        return false;
    }

    @Override
    public void bulkDelete(ActionMode mode) {
        if (adapter.getSelectedPos().isEmpty()) {
            Toast.makeText(context, "No category selected", Toast.LENGTH_SHORT).show();
            return;
        }
        if (getActivity() == null)
            return;
        MainActivity context = (MainActivity) getActivity();
        int numExpenses = context.db.getNumExpensesByCategories(adapter.getAllSelected());
        AlertDialog.Builder confirmDel = new AlertDialog.Builder(context, R.style.ConfirmDelDialog);
        confirmDel.setTitle((adapter.getSelectedPos().size() == 1) ? "Delete category" : "Delete categories")
                .setMessage("Are you sure you want to delete?" + ((numExpenses == 0) ? "" :
                        " " + numExpenses + " expense(s) will be moved to " + context.getImmutableCat() + "."))
                .setPositiveButton("Delete", (dialog, which) -> {
                    for (int pos : adapter.getSelectedPos())
                        context.db.deleteCategory(adapter.getList().get(pos), false);
                    context.updateCategoryData(); // update categories
                    Toast.makeText(context, (adapter.getSelectedPos().size() == 1) ? "Category deleted" : "Categories deleted", Toast.LENGTH_SHORT).show();
                    mode.finish();
                    context.updateHomeData(); // update summary & expense list
                })
                .setNeutralButton(android.R.string.no, (dialog, which) -> dialog.cancel())
                .show();
    }

    @Override
    public void updateData() {
        ((MainActivity) context).updateCategoryData();
    }

    public void resetOrder() {
        adapter.resetPositions();
        adapter.setList(((MainActivity) getActivity()).sortSections(adapter.getList()));
        adapter.notifyItemRangeChanged(0, adapter.getItemCount()-2);
    }
}
