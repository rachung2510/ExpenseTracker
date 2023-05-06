package com.example.expensetracker.ManagePage;

import static androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_DRAG;

import android.content.Context;
import android.graphics.Canvas;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensetracker.MainActivity;
import com.example.expensetracker.R;
import com.example.expensetracker.RecyclerViewAdapters.AccountAdapter;
import com.example.expensetracker.RecyclerViewAdapters.CategoryAdapter;
import com.example.expensetracker.RecyclerViewAdapters.SectionAdapter;
import com.example.expensetracker.Section;

import java.util.ArrayList;

public class ManageChildFragment<T extends SectionAdapter<? extends Section>> extends Fragment {
    private static final String TAG = "ManageChildFragment";
    protected Context context;
    protected T adapter;
    protected RecyclerView sectionList;
    protected int sectionType;

    public ManageChildFragment(Context context) { this.context = context; }
    public void invalidateMenu() {}

    public void updateView() {

    }
    public void setAdapter(T adapter, int args) {
        this.adapter = adapter;
        sectionList.setAdapter(adapter);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(getSimpleCallback(args));
        itemTouchHelper.attachToRecyclerView(sectionList);
        adapter.setItemTouchHelper(itemTouchHelper);
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
            if (getActivity() == null)
                return false;
            int id = menuItem.getItemId();
            if (id == R.id.delSection) {
                bulkDelete(mode);
                return true;
            }
            if (id == R.id.selectAll) {
                if (adapter.getAllSelected().size() == adapter.getItemCount()-2)
                    adapter.clearAllSelected();
                else {
                    for (int i = 0; i < adapter.getItemCount()-1; i++) {
                        if (adapter instanceof AccountAdapter && adapter.getSelected(i).getName().equals(((MainActivity) getActivity()).getDefaultAccName()))
                            continue;
                        if (adapter instanceof CategoryAdapter && adapter.getSelected(i).getName().equals(((MainActivity) getActivity()).getImmutableCat()))
                            continue;
                        if (!adapter.isSelected(i))
                            adapter.setSelected(i);
                    }
                }
                adapter.notifyItemRangeChanged(0, adapter.getItemCount()-1);
                return true;
            }
            return false;
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
    @SuppressWarnings("unchecked")
    public <E extends Section> ItemTouchHelper.SimpleCallback getSimpleCallback(int args) {
        return new ItemTouchHelper.SimpleCallback(args, 0) {
            int fromFinal = -1;
            int toFinal;
            @Override
            public boolean canDropOver(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder current, @NonNull RecyclerView.ViewHolder target) {
                SectionAdapter<E> adapter = (SectionAdapter<E>) recyclerView.getAdapter();
                if (adapter == null)
                    return false;
                if (adapter.isNew(target.getAdapterPosition()))
                    return false;
                if (adapter instanceof AccountAdapter)
                    return true;
                if(adapter.getSection(target.getAdapterPosition()).getName().equals("Others"))
                    return false;
                return super.canDropOver(recyclerView, current, target);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                if (recyclerView.getAdapter() == null)
                    return false;
                int fromPos = viewHolder.getAdapterPosition();
                int toPos = target.getAdapterPosition();
                fromFinal = (fromFinal < 0) ? fromPos : fromFinal;
                toFinal = toPos;
                recyclerView.getAdapter().notifyItemMoved(fromPos, toPos);
                return true;
            }

            @Override
            public boolean isLongPressDragEnabled() {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }

            @Override
            public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
                if (viewHolder == null)
                    return;
                if (actionState == ACTION_STATE_DRAG)
                    viewHolder.itemView.setAlpha(0.7f);
                else if (fromFinal == -1)
                    adapter.notifyItemRangeChanged(0, adapter.getItemCount()-1);
                super.onSelectedChanged(viewHolder, actionState);
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                if (fromFinal == -1) return;
                viewHolder.itemView.setAlpha(1f);
                SectionAdapter<E> adapter = (SectionAdapter <E>) recyclerView.getAdapter();
                if (adapter == null)
                    return;
                ArrayList<E> sections = adapter.getList();
                E section = sections.remove(fromFinal);
                sections.add(toFinal, section);
                adapter.setList(sections);
                adapter.updatePositions();
                adapter.notifyItemRangeChanged(0, adapter.getItemCount()-1);
                super.clearView(recyclerView, viewHolder);
                fromFinal = -1;
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                float clippedDx = clip(recyclerView.getWidth(), viewHolder.itemView.getLeft(), viewHolder.itemView.getRight(), dX);
                float clippedDy = clip(recyclerView.getHeight(), viewHolder.itemView.getTop(), viewHolder.itemView.getBottom(), dY);
                super.onChildDraw(c, recyclerView, viewHolder, clippedDx, clippedDy, actionState, isCurrentlyActive);
            }

            private float clip(int size, int start, int end, float delta) {
                float newStart = start + delta;
                float newEnd = end + delta;
                float oobStart = 0 - newStart;
                float oobEnd = newEnd - size;
                if (oobStart > 0)
                    return delta + oobStart;
                else if (oobEnd > 0)
                    return delta - oobEnd;
                else
                    return delta;
            }
        };
    }
}
