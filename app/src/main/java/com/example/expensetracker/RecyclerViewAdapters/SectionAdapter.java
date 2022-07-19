package com.example.expensetracker.RecyclerViewAdapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensetracker.Account;
import com.example.expensetracker.Category;
import com.example.expensetracker.Constants;
import com.example.expensetracker.MainActivity;
import com.example.expensetracker.R;
import com.example.expensetracker.Section;

import java.util.ArrayList;

public class SectionAdapter<T extends Section> extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "SectionAdapter";

    // Components
    protected final Context context;
    protected final LayoutInflater inflater;
    protected static AlertDialog dialog;
    protected ArrayList<T> sections;
    protected ArrayList<String> sectionNames;
    protected int page;

    // Constants
    public final Drawable iconCheck;
    public final ColorStateList iconGray;

    // Selection components
    protected boolean selectionMode = false;
    protected final ArrayList<Integer> selectedPos = new ArrayList<>();

    protected ItemTouchHelper itemTouchHelper;
    public void setItemTouchHelper(ItemTouchHelper itemTouchHelper) { this.itemTouchHelper = itemTouchHelper; }

    /**
     * CONSTRUCTOR
     */
    public SectionAdapter(Context context, ArrayList<T> sections) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.iconGray = MainActivity.getColorStateListFromId(context, R.color.generic_icon_gray);
        this.iconCheck = MainActivity.getIconFromId(context, R.drawable.action_check);

        this.sections = sections;
        this.sectionNames = getSectionNames(sections);
        this.page = Constants.HOME;
    }
    public SectionAdapter(Context context, ArrayList<T> sections, int mode) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.iconGray = MainActivity.getColorStateListFromId(context, R.color.generic_icon_gray);
        this.iconCheck = MainActivity.getIconFromId(context, R.drawable.action_check);

        this.sections = sections;
        this.sectionNames = getSectionNames(sections);
        this.page = mode;
    }

    /**
     * INITIALISE ADAPTER
     */
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.empty_view, parent, false);
        return new EmptyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return sections.size();
    }

    public static class EmptyViewHolder extends RecyclerView.ViewHolder {
        public EmptyViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    /**
     * VIEWHOLDER CLASS
     */
    // ViewHolder class for expense options section grid
    public class GridViewHolder extends RecyclerView.ViewHolder {
        TextView gridItemName;
        ImageButton gridItemIcon;

        public GridViewHolder(@NonNull View itemView) {
            super(itemView);
            gridItemName = itemView.findViewById(R.id.gridItemName);
            gridItemIcon = itemView.findViewById(R.id.gridItemIcon);
        }

        public void select() {
            gridItemIcon.setForeground(iconCheck);
            gridItemName.setAlpha(1f);
            gridItemIcon.setAlpha(1f);
        }

        public void deselect(Section section) {
            if (section.getId() != -1) {
                gridItemIcon.setForeground(section.getIcon());
                gridItemName.setAlpha(0.3f);
                gridItemIcon.setAlpha(0.3f);
            }
        }

        public void toggleSelect(int position) {
            if (selectedPos.contains(position)) selectedPos.remove((Integer) position);
            else selectedPos.add(position);

        }
    }
    public void populateSectionGrid(GridViewHolder holder, Section section, int position) {
        // set components
        if (section.getId() == -1 || isNew(position)) {
            holder.gridItemName.setText("");
            holder.gridItemIcon.setForeground(MainActivity.getIconFromId(context, R.drawable.add));
            holder.gridItemIcon.setForegroundTintList(MainActivity.getColorStateListFromId(context, R.color.text_mid_gray));
            holder.gridItemIcon.setBackground(MainActivity.getIconFromId(context, R.drawable.shape_circle_dotted_border));
            holder.gridItemIcon.setBackgroundTintList(null);
        } else {
            holder.gridItemName.setText(section.getName());
            holder.gridItemIcon.setForeground(section.getIcon());
            holder.gridItemIcon.setForegroundTintList(MainActivity.getColorStateListFromId(context, R.color.white));
            holder.gridItemIcon.setBackground(MainActivity.getIconFromId(context, R.drawable.shape_circle));
            holder.gridItemIcon.setBackgroundTintList(MainActivity.getColorStateListFromName(context, section.getColorName()));
        }

        // Multiple selection mode
        if (selectionMode) {
            if (isNew(position)) holder.gridItemIcon.setVisibility(View.GONE);
            if (selectedPos.contains(position)) holder.select();
            else holder.deselect(section);
            holder.gridItemIcon.setOnClickListener(view -> {
                holder.toggleSelect(position);
                notifyItemChanged(position);
            });
            return;
        }

        // HOME: Single selection mode for expense options
        if (page == Constants.HOME) {
            if (selectedPos.isEmpty() || position != selectedPos.get(0))
                holder.itemView.setAlpha(0.3f);
            else
                holder.itemView.setAlpha(1f);
            holder.gridItemIcon.setOnClickListener(view -> {
                selectedPos.add(0, position);
                dialog.cancel();
            });
            return;
        }

        // MANAGE: add/edit cat, drag to reorder
        holder.itemView.setAlpha(1f);
        holder.gridItemIcon.setOnClickListener(view -> {
            if (isNew(position))
                ((MainActivity) context).addCategory();
            else {
                Log.e(TAG, "pos=" + position);
                ((MainActivity) context).editCategory((Category) sections.get(position));
                notifyItemChanged(position);
            }
        });
        holder.gridItemIcon.setOnLongClickListener(view -> {
            if (section.getName().equals(((MainActivity) context).getImmutableCat()))
                return false;
            itemTouchHelper.startDrag(holder);
            return true;
        });
    }

    // ViewHolder class for manage section list (accounts)
    public class ListViewHolder extends RecyclerView.ViewHolder {
        TextView listItemName, listItemAmt, listItemCurr;
        ImageButton listItemIcon;
        CardView listItemRow;
        LinearLayout listItemTotal;
        View separator;
        int position;

        public ListViewHolder(@NonNull View itemView) {
            super(itemView);
            listItemName = itemView.findViewById(R.id.listItemName);
            listItemAmt = itemView.findViewById(R.id.listItemAmt);
            listItemIcon = itemView.findViewById(R.id.listItemIcon);
            listItemRow = itemView.findViewById(R.id.listItemRow);
            listItemTotal = itemView.findViewById(R.id.listItemTotal);
            listItemCurr = itemView.findViewById(R.id.listItemCurrency);
            separator = itemView.findViewById(R.id.separator);
        }

        public void setPosition(int position) { this.position = position; }

        public void select() {
            listItemIcon.setForeground(iconCheck);
            listItemRow.setBackgroundColor(ContextCompat.getColor(context, R.color.orange_200));
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) separator.getLayoutParams();
            p.setMargins(0,0,0,0);
            separator.requestLayout();
        }

        public void deselect(int position) {
            T section = sections.get(position);
            if (section.getId() != -1) {
                listItemRow.setBackgroundColor(ContextCompat.getColor(context, R.color.white));
                listItemIcon.setForeground(section.getIcon());
                if (position != getItemCount()-2) {
                    ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) separator.getLayoutParams();
                    int m = (int) MainActivity.convertDpToPx(context, 24);
                    p.setMargins(m, 0, m, 0);
                    separator.requestLayout();
                }
            }
        }

        public void toggleSelect(int position) {
            if (selectedPos.contains(position)) selectedPos.remove((Integer) position);
            else selectedPos.add(position);
        }

    }
    public void populateList(ListViewHolder holder, T section, int position) {
        // set components
        if (section.getId() == -1 || isNew(position)) {
            holder.listItemName.setText("Add account");
            holder.listItemName.setTextColor(ContextCompat.getColor(context, R.color.text_light_gray));
            holder.listItemIcon.setForeground(MainActivity.getIconFromId(context, R.drawable.add));
            holder.listItemIcon.setForegroundTintList(MainActivity.getColorStateListFromId(context, R.color.text_mid_gray));
            holder.listItemIcon.setBackground(MainActivity.getIconFromId(context, R.drawable.shape_rounded_square_dotted_border));
            holder.listItemIcon.setBackgroundTintList(null);
            holder.listItemTotal.setVisibility(LinearLayout.GONE);
            holder.separator.setVisibility(View.GONE);
        } else {
            holder.listItemName.setText(section.getName());
            holder.listItemCurr.setText(((Account) section).getCurrencySymbol());
            holder.listItemAmt.setText(String.format(MainActivity.locale, "%.2f", ((MainActivity) context).db.getTotalAmtByAccount((Account) section)));
            holder.listItemIcon.setForeground(section.getIcon());
            holder.listItemIcon.setForegroundTintList(MainActivity.getColorStateListFromId(context, R.color.white));
            holder.listItemIcon.setBackground(MainActivity.getIconFromId(context, R.drawable.shape_rounded_square));
            holder.listItemIcon.setBackgroundTintList(MainActivity.getColorStateListFromName(context, section.getColorName()));
            holder.listItemTotal.setVisibility(LinearLayout.VISIBLE);
            holder.separator.setVisibility(View.VISIBLE);
        }
        // extend separator before new account
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) holder.separator.getLayoutParams();
        if (position == getItemCount() - 2)
            params.setMargins(0, 0, 0, 0);
        else
            params.setMargins(24, 0, 24, 0);
        holder.separator.setLayoutParams(params);

        // Multiple selection mode
        if (selectionMode) {
            if (isNew(position)) holder.itemView.setVisibility(View.GONE);
            if (selectedPos.contains(position)) holder.select();
            else holder.deselect(position);
            holder.listItemRow.setOnClickListener(view -> {
                holder.toggleSelect(position);
                notifyItemChanged(position);
            });
            return;
        }

        holder.itemView.setAlpha(1f);
        holder.listItemRow.setOnClickListener(view -> {
            if (isNew(position))
                ((MainActivity) context).addAccount();
            else {
                ((MainActivity) context).editAccount((Account) sections.get(position));
                notifyItemChanged(position);
            }
        });
        holder.listItemRow.setOnLongClickListener(view -> {
            itemTouchHelper.startDrag(holder);
            return false;
        });
    }

    /**
     * FUNCTIONS
     */
    public void clearSelected() {
        selectedPos.clear();
    }
    public boolean isNew(int position) {
        return (page == Constants.MANAGE) && (position == getItemCount()-1);
    }
    public void setDialog(AlertDialog alertDialog) {
        dialog = alertDialog;
    }
    public void resetPositions() {}
    public void updatePositions() {

    }
    public void clearAllSelected() {
        selectedPos.clear();
    }
    public boolean isSelected(int pos) {
        return selectedPos.contains(pos);
    }

    /**
     * GETTERS & SETTERS
     */
    public ArrayList<T> getAllSelected() {
        ArrayList<T> selected_sections = new ArrayList<>();
        for (int pos : selectedPos) {
            selected_sections.add(sections.get(pos));
        }
        return selected_sections;
    }
    public ArrayList<T> getList() {
        return sections;
    }
    public T getSection(int pos) { return sections.get(pos); }
    public static ArrayList<String> getSectionNames(ArrayList<? extends Section> sections) {
        ArrayList<String> sectionNames = new ArrayList<>();
        for (Section section : sections) {
            sectionNames.add(section.getName());
        }
        return sectionNames;
    }
    public T getSelected(int pos) {
        if (pos >= 0 && pos < getItemCount())
            return sections.get(pos);
        else
            return null;
    }
    public T getSelected() {
        return getSelected(selectedPos.get(0));
    }
    public ArrayList<Integer> getSelectedPos() {
        return selectedPos;
    }

    public void setList(ArrayList<T> list) { this.sections = list; }
    public void setSelected(int position) {
        int oldPos = 0;
        if (!selectedPos.isEmpty())
            oldPos = selectedPos.get(0);
        selectedPos.add(0, position);
        notifyItemChanged(oldPos);
        notifyItemChanged(selectedPos.get(0));
    }
    public void setSelected(String name) {
        int oldPos = 0;
        if (!selectedPos.isEmpty()) oldPos = selectedPos.get(0);
        selectedPos.add(0, sectionNames.indexOf(name));
        notifyItemChanged(oldPos);
        notifyItemChanged(selectedPos.get(0));
    }
    public void setSelectionMode(boolean selectionMode) {
        this.selectionMode = selectionMode;
        notifyItemRangeChanged(0, getItemCount());
    }

    // debug
    public void printSelectedPos() {
        String msg = "";
        for (int pos : selectedPos) {
            msg += pos + ", ";
        }
        Log.e("selectedPos", "{ " + msg + " }");
    }
}
