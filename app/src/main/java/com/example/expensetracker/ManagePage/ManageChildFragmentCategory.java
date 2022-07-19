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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.example.expensetracker.Constants;
import com.example.expensetracker.MainActivity;
import com.example.expensetracker.R;
import com.example.expensetracker.RecyclerViewAdapters.CategoryAdapter;

public class ManageChildFragmentCategory extends ManageChildFragment<CategoryAdapter> {

    public ManageChildFragmentCategory(Context context) {
        super(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manage_cat, container, false);
        adapter = ((MainActivity) getParentFragment().getActivity()).getCategoryData(Constants.MANAGE);
        adapter.addNewCat();
        sectionList = view.findViewById(R.id.catGrid);
        ((SimpleItemAnimator) sectionList.getItemAnimator()).setSupportsChangeAnimations(false);
        sectionList.setLayoutManager(new GridLayoutManager(getParentFragment().getActivity(), 3, GridLayoutManager.VERTICAL, false));
        setAdapter(adapter, ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
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
                confirmReset.setMessage("Reset default categories? Relevant expenses will be moved to " + ((MainActivity) context).getImmutableCat() + ".");
                confirmReset.setPositiveButton("Reset", (dialogInterface, i) -> {
                    context.db.deleteAllCategories();
                    context.initialiseDefaultCats();
                    context.updateCategoryData();
                    Toast.makeText(context, "Categories reset to defaults", Toast.LENGTH_SHORT).show();
                });
                confirmReset.setNeutralButton(android.R.string.no, (dialog, which) -> {
                    dialog.cancel(); // close dialog
                });
                confirmReset.show();
                return true;

            case R.id.resetOrder:
                adapter.resetPositions();
                adapter.setList(((MainActivity) getActivity()).sortSections(adapter.getList()));
                adapter.notifyItemRangeChanged(0, adapter.getItemCount()-2);
                return true;

            default:
                return false;
        }
    }

    @Override
    public void bulkDelete(ActionMode mode) {
        if (adapter.getSelectedPos().isEmpty()) {
            Toast.makeText(context, "No category selected", Toast.LENGTH_SHORT).show();
            return;
        }
        AlertDialog.Builder confirmDel = new AlertDialog.Builder(context, R.style.ConfirmDelDialog);
        confirmDel.setTitle((adapter.getSelectedPos().size() == 1) ? "Delete category" : "Delete categories");
        confirmDel.setMessage("Are you sure you want to delete? Relevant expenses will be moved to " + ((MainActivity) context).getImmutableCat() + ".");
        confirmDel.setPositiveButton("Delete", (dialog, which) -> {
            adapter.printSelectedPos();
            for (int pos : adapter.getSelectedPos()) {
                ((MainActivity) context).db.deleteCategory(adapter.getList().get(pos), false);
            }
            ((MainActivity) context).updateCategoryData(); // update categories
            Toast.makeText(context, (adapter.getSelectedPos().size() == 1) ? "Category deleted" : "Categories deleted", Toast.LENGTH_SHORT).show();
            mode.finish();
            ((MainActivity) context).updateHomeData(); // update summary & expense list
        });
        confirmDel.setNeutralButton(android.R.string.no, (dialog, which) -> dialog.cancel());
        confirmDel.show();
    }

    @Override
    public void updateData() {
        ((MainActivity) context).updateCategoryData();
    }
}
