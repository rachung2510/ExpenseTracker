package com.example.expensetracker.RecyclerViewAdapters;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensetracker.Category;
import com.example.expensetracker.Expense;
import com.example.expensetracker.MainActivity;
import com.example.expensetracker.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class ExpenseAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>  {

    private static final String TAG = "ExpenseAdapter";

    private static final int VIEW_TYPE_DATE_HEADER = 0;
    private static final int VIEW_TYPE_ITEM = 1;

    private final ArrayList<Expense> expenses;
    private final Context context;
    private final LayoutInflater inflater;
    private boolean viewOnly = false;

    // actions and selections
    private ActionMode actionMode;
    private boolean selectionMode = false;
    private final ArrayList<Integer> selectedPos = new ArrayList<>();

    /**
     * Constructor
     */
    public ExpenseAdapter(Context context, ArrayList<Expense> expenses) {
        this.expenses = expenses;
        this.context = context;
        this.inflater = LayoutInflater.from(context);
    }
    public ExpenseAdapter(Context context, ArrayList<Expense> expenses, boolean viewOnly) {
        this.expenses = expenses;
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.viewOnly = viewOnly;
    }

    /**
     * Initialise adapter
     */
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_DATE_HEADER) {
            View view = inflater.inflate(R.layout.item_expense_date_header, parent, false);
            return new DateViewHolder(view);
        } else { // VIEW_TYPE_ITEM
            View view = inflater.inflate(R.layout.item_expense, parent, false);
            return new ItemViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ItemViewHolder) {
            populateItemRows((ItemViewHolder) holder, position);
        } else {
            getDateHeaders((DateViewHolder) holder, position);
        }
    }

    @Override
    public int getItemCount() {
        return expenses.size();
    }

    @Override
    public int getItemViewType(int position) {
        return (expenses.get(position).getId() == -1) ? VIEW_TYPE_DATE_HEADER : VIEW_TYPE_ITEM;
    }

    /**
     * Viewholder class
     */
    // ViewHolder class for normal expense entry
    private class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView expAmt, expCurr, expDesc, expAccName, expCatName;
        ImageButton expCatIcon;
        ConstraintLayout expRow;
        View separator;
        LinearLayout expLayout;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            expAmt = itemView.findViewById(R.id.expAmt);
            expCurr = itemView.findViewById(R.id.expCurrency);
            expDesc = itemView.findViewById(R.id.expDesc);
            expAccName = itemView.findViewById(R.id.expAccName);
            expCatName = itemView.findViewById(R.id.expCatName);
            expCatIcon = itemView.findViewById(R.id.expCatIcon);
            expRow = itemView.findViewById(R.id.expenseRow);
            separator = itemView.findViewById(R.id.separator);
            expLayout = itemView.findViewById(R.id.linearLayout);
        }

        public void select() {
            expRow.setBackgroundColor(MainActivity.getColorFromId(context, R.color.select_light_orange));
        }

        public void deselect() {
            expRow.setBackgroundColor(MainActivity.getColorFromId(context, android.R.attr.colorBackground));
        }

        public void toggleSelect(int position) {
            if (selectedPos.contains(position)) selectedPos.remove((Integer) position);
            else selectedPos.add(position);
        }

    }
    private void populateItemRows(ItemViewHolder holder, int position) {
        Expense exp = expenses.get(position);
        Category cat = exp.getCategory();
        holder.expAmt.setText(String.format(MainActivity.locale, "%.2f", exp.getAmount()));
        holder.expCurr.setText(exp.getAccount().getCurrencySymbol());
        holder.expDesc.setText(exp.getDescription());
        holder.expAccName.setText(exp.getAccount().getName());
        holder.expCatName.setText(cat.getName());
        holder.expCatIcon.setForeground(cat.getIcon());
        holder.expCatIcon.setBackgroundTintList(MainActivity.getColorStateListFromName(context, cat.getColorName()));
        holder.separator.setVisibility((viewOnly && expenses.get(position-1).getId() >= -1) ? View.VISIBLE : View.GONE);
        if (viewOnly) holder.expLayout.setForeground(null);
        if (expenses.get(position-1).getId() == -1) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) holder.separator.getLayoutParams();
            params.setMargins(0, 0, 0, 0);
            holder.separator.setLayoutParams(params);
        }

        // Selection behaviour
        if (selectedPos.contains(position)) holder.select();
        else holder.deselect();

        if (!viewOnly) {
            holder.itemView.setOnLongClickListener(view -> {
                if (actionMode != null)
                    return false;
                selectionMode = true;
                holder.toggleSelect(position);
                notifyItemChanged(position);
                actionMode = ((Activity) context).startActionMode(actionModeCallback);
                return true;
            });
            holder.itemView.setOnClickListener(view -> {
                if (selectionMode) {
                    holder.toggleSelect(position);
                } else {
                    ((MainActivity) context).editExpense(expenses.get(position).getId());
                }
                notifyItemChanged(position);
            });
        }
    }
    private final ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.expense_options, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem menuItem) {
            int id = menuItem.getItemId();
            if (id == R.id.delExpense) {
                bulkDelete(mode);
                return true;
            }
            if (id == R.id.selectAll) {
                if (selectedPos.size() == expenses.size())
                    selectedPos.clear();
                else {
                    for (int i = 0; i < expenses.size(); i++)
                        if (!selectedPos.contains(i)) selectedPos.add(i);
                }
                notifyItemRangeChanged(0,getItemCount());
                return true;
            }
            if (id == R.id.changeAcc) {
                AccountAdapter accAdapter = ((MainActivity) context).getAccountData();
                accAdapter.clearSelected();
                bulkChangeAcc(mode, accAdapter);
                return true;
            }
            if (id == R.id.changeCat) {
                CategoryAdapter catAdapter = ((MainActivity) context).getCategoryData();
                catAdapter.clearSelected();
                bulkChangeCat(mode, catAdapter);
                return true;
            }
            if (id == R.id.changeDate) {
                bulkChangeDate(mode);
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            selectedPos.clear();
            selectionMode = false;
            notifyItemRangeChanged(0,getItemCount());
            actionMode = null;
        }
    };
    private void bulkDelete(ActionMode mode) {
        bulkNoAction();
        AlertDialog.Builder confirmDel = new AlertDialog.Builder(context, R.style.ConfirmDelDialog);
        confirmDel.setTitle((selectedPos.size() == 1) ? "Delete entry" : "Delete entries");
        confirmDel.setMessage("Are you sure you want to delete?");
        confirmDel.setPositiveButton("Delete", (dialog, which) -> {
            for (int i = 0;i < selectedPos.size();i++) {
                int pos = selectedPos.get(i);
                ((MainActivity) context).db.deleteExpense(expenses.get(pos));
            }
            String toast = (selectedPos.size() == 1) ? "Expense deleted" : "Expenses deleted";
            Toast.makeText(context, toast, Toast.LENGTH_SHORT).show();
            mode.finish();
            ((MainActivity) context).updateHomeData(); // update summary & expense list
            ((MainActivity) context).setUpdateFragments(true);
        });
        confirmDel.setNeutralButton(android.R.string.no, (dialog, which) -> {
            dialog.cancel(); // close dialog
        });
        confirmDel.show();
    }
    private void bulkChangeDate(ActionMode mode) {
        bulkNoAction();
        AlertDialog.Builder changeDate = new AlertDialog.Builder(context, R.style.NormalDialog);
        DatePicker datePicker = new DatePicker(context);
        changeDate.setView(datePicker);
        changeDate.setPositiveButton(android.R.string.ok, (dialogInterface, which) -> {
            Calendar datetime = Calendar.getInstance();
            datetime.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth());

            AlertDialog.Builder confirmChangeDate = new AlertDialog.Builder(context, R.style.NormalDialog);
            DateFormat dateFormat = new SimpleDateFormat("d MMM, yyyy", MainActivity.locale);
            confirmChangeDate.setMessage("Change date to " + dateFormat.format(datetime.getTime()) +  "?");
            confirmChangeDate.setPositiveButton(android.R.string.yes, (dialog, which12) -> {
                for (int i = 0; i < selectedPos.size(); i++) {
                    Expense exp = expenses.get(selectedPos.get(i));
                    exp.setDatetime(datetime);
                    ((MainActivity) context).db.updateExpense(exp);
                }
                mode.finish();
                ((MainActivity) context).updateHomeData(); // update summary & expense list
            });
            confirmChangeDate.setNeutralButton(android.R.string.no, (dialog, which1) -> dialogInterface.dismiss());
            confirmChangeDate.show();
        });
        changeDate.setNeutralButton(android.R.string.cancel, (dialog, which) -> {
            dialog.cancel(); // close dialog
        });
        changeDate.show();
    }
    private void bulkChangeCat(ActionMode mode, CategoryAdapter adapter) {
        bulkNoAction();
        @SuppressLint("InflateParams") final View expOptSectionView = inflater.inflate(R.layout.dialog_expense_opt_section, null);
        AlertDialog dialog = ((MainActivity) context).expenseSectionDialog(adapter, expOptSectionView).create();
        adapter.setDialog(dialog);
        TextView title = expOptSectionView.findViewById(R.id.expOptSectionTitle);
        title.setText(R.string.CAT);

        dialog.setOnCancelListener(dialogInterface -> {
            if (adapter.getSelected() == null) return;
            AlertDialog.Builder confirmEdit = new AlertDialog.Builder(context, R.style.NormalDialog);
            confirmEdit.setMessage("Change category to " + adapter.getSelected().getName() + "?");
            confirmEdit.setPositiveButton(android.R.string.yes, (dialog1, which) -> {
                for (int i = 0; i < selectedPos.size(); i++) {
                    Expense exp = expenses.get(selectedPos.get(i));
                    exp.setCategory(adapter.getSelected());
                    ((MainActivity) context).db.updateExpense(exp);
                }
                ((MainActivity) context).updateHomeData(); // update summary & expense list
                dialog1.dismiss();
                mode.finish();
            });
            confirmEdit.setNeutralButton(android.R.string.no, (dialog1, which) -> dialog1.cancel());
            confirmEdit.show();
        });
        dialog.show();
    }
    private void bulkChangeAcc(ActionMode mode, AccountAdapter adapter) {
        bulkNoAction();
        @SuppressLint("InflateParams") final View expOptSectionView = inflater.inflate(R.layout.dialog_expense_opt_section, null);
        AlertDialog dialog = ((MainActivity) context).expenseSectionDialog(adapter, expOptSectionView).create();
        adapter.setDialog(dialog);
        TextView title = expOptSectionView.findViewById(R.id.expOptSectionTitle);
        title.setText(R.string.ACC);

        dialog.setOnCancelListener(dialogInterface -> {
            if (adapter.getSelected() == null) return;
            AlertDialog.Builder confirmEdit = new AlertDialog.Builder(context, R.style.NormalDialog);
            confirmEdit.setMessage("Change account to " + adapter.getSelected().getName() + "?");
            confirmEdit.setPositiveButton(android.R.string.yes, (dialog1, which) -> {
                for (int i = 0; i < selectedPos.size(); i++) {
                    Expense exp = expenses.get(selectedPos.get(i));
                    exp.setAccount(adapter.getSelected());
                    ((MainActivity) context).db.updateExpense(exp);
                }
                ((MainActivity) context).updateHomeData(); // update summary & expense list
                dialog1.dismiss();
                mode.finish();
            });
            confirmEdit.setNeutralButton(android.R.string.no, (dialog1, which) -> dialog1.cancel());
            confirmEdit.show();
        });
        dialog.show();
    }
    private void bulkNoAction() {
        if (selectedPos.isEmpty()) {
            Toast.makeText(context, "No expense selected", Toast.LENGTH_SHORT).show();
        }
    }

    // ViewHolder class for expense date headers
    private static class DateViewHolder extends RecyclerView.ViewHolder {
        TextView date;

        public DateViewHolder(@NonNull View itemView) {
            super(itemView);
            date = itemView.findViewById(R.id.expDate);
        }
    }
    private void getDateHeaders(DateViewHolder holder, int position) {
        Expense exp = expenses.get(position);
        holder.date.setText(context.getString(R.string.full_date,exp.getDatetimeStr("dd MMM"),MainActivity.getRelativePrefix(exp.getDatetime())).toUpperCase());
    }
}
