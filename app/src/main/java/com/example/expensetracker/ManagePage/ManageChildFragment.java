package com.example.expensetracker.ManagePage;

import android.content.Context;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensetracker.R;
import com.example.expensetracker.RecyclerViewAdapters.SectionAdapter;
import com.example.expensetracker.Section;

public class ManageChildFragment<T extends SectionAdapter<? extends Section>> extends Fragment {
    protected Context context;
    protected T adapter;
    protected RecyclerView sectionList;

    public ManageChildFragment(Context context) { this.context = context; }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.manage_options, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    public void setAdapter(T adapter) {
        this.adapter = adapter;
        sectionList.setAdapter(adapter);
    }

    // action bar when user enters selection mode in MANAGE
    protected ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.manage_options_selection, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.delCat:
                    bulkDelete(mode);
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            adapter.clearSelected();
            adapter.setSelectionMode(false);
            updateData();
        }
    };
    public void bulkDelete(ActionMode mode) {
    }
    public void updateData() {}

    // Define drag/drop moveable grid
    ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT |
                    ItemTouchHelper.START | ItemTouchHelper.END, 0) {
        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            int fromPos = viewHolder.getAdapterPosition();
            int toPos = target.getAdapterPosition();

//            Collections.swap(adapter.getCategories(), fromPos, toPos);
            recyclerView.getAdapter().notifyItemMoved(fromPos, toPos);
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

        }
    };
}
