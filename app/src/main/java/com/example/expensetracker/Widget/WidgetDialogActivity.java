package com.example.expensetracker.Widget;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.expensetracker.Account;
import com.example.expensetracker.Category;
import com.example.expensetracker.Currency;
import com.example.expensetracker.DatabaseHelper;
import com.example.expensetracker.Expense;
import com.example.expensetracker.HelperClasses.MoneyValueFilter;
import com.example.expensetracker.MainActivity;
import com.example.expensetracker.R;
import com.example.expensetracker.RecyclerViewAdapters.AccountAdapter;
import com.example.expensetracker.RecyclerViewAdapters.CategoryAdapter;
import com.example.expensetracker.RecyclerViewAdapters.SectionAdapter;
import com.example.expensetracker.Section;

import java.util.ArrayList;
import java.util.Calendar;

public class WidgetDialogActivity extends AppCompatActivity {

    public DatabaseHelper db = new DatabaseHelper(this);

    // Dialog components
    private AlertDialog.Builder dialogBuilder;
    private EditText expAmt, expDesc;
    private TextView expAccName, expCatName, expDate, expCurr;
    private ImageButton expAccIcon, expCatIcon;
    private LinearLayout expAccBox, expCatBox, expDelBtn, expDateBtn, expSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.empty_view);
        addExpense();

        // Make status bar transparent but not navigation bar
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        setWindowFlag(this, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
    }
    public static void setWindowFlag(Activity activity, final int bits, boolean on) {
        Window win = activity.getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        if (on) winParams.flags |= bits;
        else winParams.flags &= ~bits;
        win.setAttributes(winParams);
    }
    public void addExpense() {
        AlertDialog expDialog = expenseDialog();
        expDelBtn.setVisibility(LinearLayout.INVISIBLE);
        Calendar cal = Calendar.getInstance(MainActivity.locale);
        expDate.setText(("Today, " + MainActivity.getDatetimeStr(cal, "dd MMMM yyyy")).toUpperCase());
        Account acc = db.getAccount(getDefaultAccName());
        Category cat = db.getCategory(getDefaultCatName());
        expAccName.setText(acc.getName()); // set name
        expCatName.setText(cat.getName());
        expAccIcon.setForeground(acc.getIcon()); // set icon
        expCatIcon.setForeground(cat.getIcon());
        expAccIcon.setForegroundTintList(ColorStateList.valueOf(Color.parseColor("#" + acc.getColorHex()))); // set icon color
        expCatIcon.setForegroundTintList(ColorStateList.valueOf(Color.parseColor("#" + cat.getColorHex())));
        expAccBox.setBackgroundColor(Color.parseColor("#" + acc.getColorHex())); // set bg color
        expCatBox.setBackgroundColor(Color.parseColor("#" + cat.getColorHex()));
        expCurr.setText(new Currency(this).getSymbol());

        // actions
        expAccBox.setOnClickListener(view -> {
            AccountAdapter accAdapter = getAccountData();
            accAdapter.setSelected(expAccName.getText().toString());
            expenseAccDialog(accAdapter, new Expense(Calendar.getInstance()));
        });
        expCatBox.setOnClickListener(view -> {
            CategoryAdapter catAdapter = getCategoryData();
            catAdapter.setSelected(expCatName.getText().toString());
            expenseCatDialog(catAdapter, new Expense(Calendar.getInstance()));
        });
        expDateBtn.setOnClickListener(view -> {
            AlertDialog.Builder changeDate = new AlertDialog.Builder(this);
            DatePicker datePicker = new DatePicker(this);
            changeDate.setView(datePicker)
                    .setPositiveButton(android.R.string.yes, (dialogInterface, i) -> {
                cal.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth());
                expDate.setText((MainActivity.getRelativePrefix(cal) + ", " + MainActivity.getDatetimeStr(cal, "dd MMMM yyyy")).toUpperCase());
            })
                    .setNeutralButton(android.R.string.no, (dialog, which) -> dialog.cancel())
                    .show();
        });
        expSave.setOnClickListener(v -> {
            if (expAmt.getText().toString().isEmpty())
                Toast.makeText(this, "Amount cannot be 0. No expense created", Toast.LENGTH_SHORT).show();
            else {
                float amt = Float.parseFloat(expAmt.getText().toString());
                String desc = expDesc.getText().toString();
                Account acc1 = db.getAccount(expAccName.getText().toString());
                Category cat1 = db.getCategory(expCatName.getText().toString());
                String datetime = MainActivity.getDatetimeStr(cal, Expense.DATETIME_FORMAT);
                Expense expense = new Expense(amt, desc, acc1, cat1, datetime);
                db.createExpense(expense);
            }
            hideKeyboard(expAmt);
            expDialog.dismiss();
        });
    }

    /**
     * DIALOGS
     */
    public AlertDialog expenseDialog() {
        // dialog
        final View expView = getLayoutInflater().inflate(R.layout.dialog_expense, null);
        dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setView(expView)
                .setOnDismissListener(dialogInterface -> {
            hideKeyboard(expAmt);
            finish();
        });
        AlertDialog expDialog = dialogBuilder.create();
        expDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // set transparent dialog bg
        expDialog.show();
        expDialog.getWindow().setGravity(Gravity.BOTTOM);
        expDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        // get components by id
        expAmt = expView.findViewById(R.id.newExpAmt);
        expAmt.setFilters(new InputFilter[] { new MoneyValueFilter() });
        expAmt.requestFocus(); // focus on amt and open keyboard
        expAmt.postDelayed(() -> showKeyboard(expAmt), 270);
        expDesc = expView.findViewById(R.id.newExpDesc);
        expAccName = expView.findViewById(R.id.newExpAccName); // name
        expCatName = expView.findViewById(R.id.newExpCatName);
        expAccIcon = expView.findViewById(R.id.newExpAccIcon); // icon
        expCatIcon = expView.findViewById(R.id.newExpCatIcon);
        expAccBox = expView.findViewById(R.id.newExpAccBox); // color
        expCatBox = expView.findViewById(R.id.newExpCatBox);
        expDate = expView.findViewById(R.id.expDate);
        expDelBtn = expView.findViewById(R.id.newExpDel);
        expDateBtn = expView.findViewById(R.id.newExpDate);
        expSave =  expView.findViewById(R.id.newExpSave);
        expDesc.setOnFocusChangeListener((view, b) -> {
            if (b) expDesc.setBackground(MainActivity.getIconFromId(this, R.color.white));
            else expDesc.setBackground(new ColorDrawable(android.R.attr.selectableItemBackground));
        });
        expCurr = expView.findViewById(R.id.newExpCurrency);

        return expDialog;
    }
    public <T extends SectionAdapter<? extends Section>> AlertDialog.Builder expenseSectionDialog(T adapter, View expOptSectionView) {
        dialogBuilder = new AlertDialog.Builder(this);
        RecyclerView sectionGrid = expOptSectionView.findViewById(R.id.sectionGrid);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 3, GridLayoutManager.VERTICAL, false);
        sectionGrid.setLayoutManager(gridLayoutManager);
        sectionGrid.setAdapter(adapter);
        dialogBuilder.setView(expOptSectionView);
        return dialogBuilder;
    }
    public void expenseAccDialog(AccountAdapter adapter, Expense exp) {
        final View expOptSectionView = getLayoutInflater().inflate(R.layout.dialog_expense_opt_section, null);
        AlertDialog dialog = expenseSectionDialog(adapter, expOptSectionView).create();
        adapter.setDialog(dialog);

        // set values
        TextView title = expOptSectionView.findViewById(R.id.expOptSectionTitle);
        title.setText(R.string.ACC);
        TextView expAccName = this.expAccName;
        ImageButton expAccIcon = this.expAccIcon;
        LinearLayout expAccItem = expAccBox;
        if (adapter.getSelectedPos().isEmpty()) {
            if (exp.getId() == -1) {
                adapter.setSelected(0);
            } else {
                Account acc = exp.getAccount();
                adapter.setSelected(adapter.getList().indexOf(acc));
            }
        }

        dialog.setOnCancelListener(dialog1 -> {
            Account selectedAcc = adapter.getSelected();
            expAccName.setText(selectedAcc.getName()); // set name
            expAccIcon.setForeground(selectedAcc.getIcon()); // set icon
            expAccIcon.setForegroundTintList(MainActivity.getColorStateListFromName(this, selectedAcc.getColorName())); // set icon color
            expAccItem.setBackgroundColor(Color.parseColor("#" + selectedAcc.getColorHex())); // set bg color
            expCurr.setText(selectedAcc.getCurrencySymbol());
        });

        dialog.show();
    }
    public void expenseCatDialog(CategoryAdapter adapter, Expense exp) {
        final View expOptSectionView = getLayoutInflater().inflate(R.layout.dialog_expense_opt_section, null);
        AlertDialog dialog = expenseSectionDialog(adapter, expOptSectionView).create();
        adapter.setDialog(dialog);

        // set values
        TextView title = expOptSectionView.findViewById(R.id.expOptSectionTitle);
        title.setText(R.string.CAT);
        TextView expCatName = this.expCatName;
        ImageButton expCatIcon = this.expCatIcon;
        LinearLayout expCatItem = expCatBox;
        if (adapter.getSelectedPos().isEmpty()) {
            if (exp.getId() == -1) {
                adapter.setSelected(0);
            } else {
                Category cat = exp.getCategory();
                adapter.setSelected(adapter.getList().indexOf(cat));
            }
        }

        dialog.setOnCancelListener(dialog1 -> {
            Category selectedCat = adapter.getSelected();
            expCatName.setText(selectedCat.getName()); // set name
            expCatIcon.setForeground(selectedCat.getIcon()); // set icon
            expCatIcon.setForegroundTintList(MainActivity.getColorStateListFromName(this, selectedCat.getColorName())); // set icon color
            expCatItem.setBackgroundColor(Color.parseColor("#" + selectedCat.getColorHex())); // set bg color
        });

        dialog.show();
    }

    /**
     * GETTERS & SETTERS
     */
    public <T extends Section> ArrayList<T> sortSections(ArrayList<T> sections) {
        sections.sort((s1, s2) -> {
            if (s1.getId() == -1) return 1; // new appears last
            if (s2.getId() == -1) return -1;
            if (s1 instanceof Category && s1.getName().equals(getImmutableCat())) return 1;
            if (s2 instanceof Category && s2.getName().equals(getImmutableCat())) return -1;
            return Integer.compare(s1.getPosition(), s2.getPosition());
        });
        return sections;
    }
    public CategoryAdapter getCategoryData() {
        return new CategoryAdapter(this, sortSections(db.getAllCategories()));
    }
    public AccountAdapter getAccountData() {
        return new AccountAdapter(this, sortSections(db.getAllAccounts()));
    }
    public String getDefaultCurrency() {
        return MainActivity.getDefaultCurrency(this);
    }
    public String getDefaultAccName() {
        return db.getDefaultAccName();
    }
    public String getDefaultCatName() {
        return db.getDefaultCatName();
    }
    public String getImmutableCat() {
        return db.getCategory(1).getName();
    }

    /**
     * HELPER FUNCTIONS
     */
    public void showKeyboard(EditText view) {
        MainActivity.showKeyboard(this, view);
    }
    public void hideKeyboard(EditText view) {
        MainActivity.hideKeyboard(this, view);
    }

}