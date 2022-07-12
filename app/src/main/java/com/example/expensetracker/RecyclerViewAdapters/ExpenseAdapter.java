package com.example.expensetracker.RecyclerViewAdapters;

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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensetracker.Category;
import com.example.expensetracker.Constants;
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

    // actions and selections
    private ActionMode actionMode;
    private boolean selectionMode = false;
    private final ArrayList<Integer> selectedPos = new ArrayList<>();

    /**
     * CONSTRUCTOR
     */
    public ExpenseAdapter(Context context, ArrayList<Expense> expenses) {
        this.expenses = expenses;
        this.context = context;
        this.inflater = LayoutInflater.from(context);
    }

    /**
     * INITIALISE ADAPTER
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
     * VIEWHOLDER CLASS
     */
    // ViewHolder class for normal expense entry
    public class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView expAmt, expDesc, expAccName, expCatName;
        ImageButton expCatIcon;
        ConstraintLayout expRow;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            expAmt = itemView.findViewById(R.id.expAmt);
            expDesc = itemView.findViewById(R.id.expDesc);
            expAccName = itemView.findViewById(R.id.expAccName);
            expCatName = itemView.findViewById(R.id.expCatName);
            expCatIcon = itemView.findViewById(R.id.expCatIcon);
            expRow = itemView.findViewById(R.id.expenseRow);
        }

        public void select() {
            expRow.setBackgroundColor(ContextCompat.getColor(context, R.color.orange_200));
        }

        public void deselect() {
            expRow.setBackgroundColor(ContextCompat.getColor(context, R.color.white));
        }

        public void toggleSelect(int position) {
            if (selectedPos.contains(position)) selectedPos.remove((Integer) position);
            else selectedPos.add(position);
        }

    }
    public void populateItemRows(ItemViewHolder holder, int position) {
        Category cat = expenses.get(position).getCategory();
        holder.expAmt.setText(String.format(MainActivity.locale, "%.2f", expenses.get(position).getAmount()));
        holder.expDesc.setText(expenses.get(position).getDescription());
        holder.expAccName.setText(expenses.get(position).getAccount().getName());
        holder.expCatName.setText(cat.getName());
        holder.expCatIcon.setForeground(cat.getIcon());
        holder.expCatIcon.setBackgroundTintList(MainActivity.getColorStateListFromName(context, cat.getColorName()));

        // Selection behaviour
        if (selectedPos.contains(position)) holder.select();
        else holder.deselect();

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
            switch (menuItem.getItemId()) {
                case R.id.delExpense:
                    bulkDelete(mode);
                    return true;

                case R.id.selectAll:
                    if (selectedPos.size() != expenses.size()) {
                        for (int i = 0; i < expenses.size(); i++) {
                            if (!selectedPos.contains(i))
                                selectedPos.add(i);
                        }
                    } else {
                        selectedPos.clear();
                    }
                    notifyDataSetChanged();
                    return true;

                case R.id.changeAcc:
                    AccountAdapter accAdapter = ((MainActivity) context).getAccountData();
                    accAdapter.clearSelected();
                    bulkChangeAcc(mode, accAdapter);
                    return true;

                case R.id.changeCat:
                    CategoryAdapter catAdapter = ((MainActivity) context).getCategoryData();
                    catAdapter.clearSelected();
                    bulkChangeCat(mode, catAdapter);
                    return true;

                case R.id.changeDate:
                    bulkChangeDate(mode);
                    return true;

                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            selectedPos.clear();
            selectionMode = false;
            notifyDataSetChanged();
            actionMode = null;
        }
    };
    public void bulkDelete(ActionMode mode) {
        bulkNoAction();
        AlertDialog.Builder confirmDel = new AlertDialog.Builder(context, R.style.DiscardChangesDialog);
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
        });
        confirmDel.setNeutralButton(android.R.string.no, (dialog, which) -> {
            dialog.cancel(); // close dialog
        });
        confirmDel.show();
    }
    public void bulkChangeDate(ActionMode mode) {
        bulkNoAction();
        AlertDialog.Builder changeDate = new AlertDialog.Builder(context);
        DatePicker datePicker = new DatePicker(context);
        changeDate.setView(datePicker);
        changeDate.setPositiveButton(android.R.string.ok, (dialogInterface, which) -> {
            Calendar datetime = Calendar.getInstance();
            datetime.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth());

            AlertDialog.Builder confirmChangeDate = new AlertDialog.Builder(context, R.style.DiscardChangesDialog);
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
    public void bulkChangeCat(ActionMode mode, CategoryAdapter adapter) {
        bulkNoAction();
        final View expOptSectionView = inflater.inflate(R.layout.dialog_expense_opt_section, null);
        AlertDialog dialog = ((MainActivity) context).expOptSectionDialog(adapter, expOptSectionView).create();
        adapter.setDialog(dialog);
        TextView title = expOptSectionView.findViewById(R.id.expOptSectionTitle);
        title.setText(R.string.cat_caps);

        dialog.setOnCancelListener(dialogInterface -> {
            if (adapter.getSelected().getId() == -1) {
                Toast.makeText(context, "No category selected", Toast.LENGTH_SHORT).show();
                return;
            }
            AlertDialog.Builder confirmEdit = new AlertDialog.Builder(context, R.style.DiscardChangesDialog);
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
    public void bulkChangeAcc(ActionMode mode, AccountAdapter adapter) {
        bulkNoAction();
        final View expOptSectionView = inflater.inflate(R.layout.dialog_expense_opt_section, null);
        AlertDialog dialog = ((MainActivity) context).expOptSectionDialog(adapter, expOptSectionView).create();
        adapter.setDialog(dialog);
        TextView title = expOptSectionView.findViewById(R.id.expOptSectionTitle);
        title.setText(R.string.acc_caps);

        dialog.setOnCancelListener(dialogInterface -> {
            if (adapter.getSelected().getId() == -1) {
                Toast.makeText(context, "No account selected", Toast.LENGTH_SHORT).show();
                return;
            }
            AlertDialog.Builder confirmEdit = new AlertDialog.Builder(context, R.style.DiscardChangesDialog);
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
    public void bulkNoAction() {
        if (selectedPos.isEmpty()) {
            Toast.makeText(context, "No expense selected", Toast.LENGTH_SHORT).show();
        }
    }

    // ViewHolder class for expense date headers
    public static class DateViewHolder extends RecyclerView.ViewHolder {
        TextView date;

        public DateViewHolder(@NonNull View itemView) {
            super(itemView);
            date = itemView.findViewById(R.id.expDate);
        }
    }
    public void getDateHeaders(DateViewHolder holder, int position) {
        Expense exp = expenses.get(position);
        String dateSuffix = ", " +
                ((exp.getRelativeDate() == Constants.TODAY) ? "Today" :
                        ((exp.getRelativeDate() == Constants.YESTERDAY) ? "Yesterday" : exp.getDatetimeStr("EEE")));
        holder.date.setText((exp.getDatetimeStr("dd MMM") + dateSuffix).toUpperCase());
    }
}
