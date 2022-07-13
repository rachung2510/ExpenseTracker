package com.example.expensetracker;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.expensetracker.HelperClasses.MoneyValueFilter;
import com.example.expensetracker.RecyclerViewAdapters.AccountAdapter;
import com.example.expensetracker.RecyclerViewAdapters.CategoryAdapter;
import com.example.expensetracker.RecyclerViewAdapters.SectionAdapter;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class WidgetExpenseActivity extends AppCompatActivity {

    public DatabaseHelper db = new DatabaseHelper(this);

    // Dialog components
    private AlertDialog.Builder dialogBuilder;
    private EditText expAmt, expDesc;
    private TextView expAccName, expCatName, expDate;
    private ImageButton expAccIcon, expCatIcon;
    private LinearLayout expAcc, expCat, expDelBtn, expDateBtn, expSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.empty_view);
        addExpense();

        // Make status bar transparent but not navigation bar
        if (Build.VERSION.SDK_INT >= 19 && Build.VERSION.SDK_INT < 21)
            setWindowFlag(this, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, true);
        if (Build.VERSION.SDK_INT >= 19)
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        if (Build.VERSION.SDK_INT >= 21) {
            setWindowFlag(this, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, false);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
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
        expDate.setText(("Today, " + new SimpleDateFormat("dd MMMM yyyy", MainActivity.locale).format(cal.getTime())).toUpperCase());

        // actions
        expAcc.setOnClickListener(view -> {
            AccountAdapter accAdapter = getAccountData();
            accAdapter.setSelected(expAccName.getText().toString());
            expOptAccDialog(accAdapter, new Expense(Calendar.getInstance()));
        });
        expCat.setOnClickListener(view -> {
            CategoryAdapter catAdapter = getCategoryData();
            catAdapter.setSelected(expCatName.getText().toString());
            expOptCatDialog(catAdapter, new Expense(Calendar.getInstance()));
        });
        expDateBtn.setOnClickListener(view -> {
            AlertDialog.Builder changeDate = new AlertDialog.Builder(this);
            DatePicker datePicker = new DatePicker(this);
            changeDate.setView(datePicker);
            changeDate.setPositiveButton(android.R.string.yes, (dialogInterface, i) -> {
                cal.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth());
                int relativeDate = MainActivity.getRelativeDate(cal);
                String datePrefix = (relativeDate == Constants.TODAY) ? "Today" :
                        ((relativeDate == Constants.YESTERDAY) ? "Yesterday" : new SimpleDateFormat("EEE", MainActivity.locale).format(cal.getTime()));
                expDate.setText((datePrefix + ", " + new SimpleDateFormat("dd MMMM yyyy", MainActivity.locale).format(cal.getTime())).toUpperCase());
            });
            changeDate.setNeutralButton(android.R.string.no, (dialog, which) -> dialog.cancel());
            changeDate.show();
        });
        expSave.setOnClickListener(v -> {
            if (!expAmt.getText().toString().isEmpty()) {
                float amt = Float.parseFloat(expAmt.getText().toString());
                String desc = expDesc.getText().toString();
                Account acc = db.getAccount(expAccName.getText().toString());
                Category cat = db.getCategory(expCatName.getText().toString());
                String datetime = new SimpleDateFormat(Expense.DATETIME_FORMAT, MainActivity.locale).format(cal.getTime());
                Expense expense = new Expense(amt, desc, acc, cat, datetime);
                db.createExpense(expense);
            } else {
                Toast.makeText(this, "Amount cannot be 0. No expense created", Toast.LENGTH_SHORT).show();
            }
            InputMethodManager imm = (InputMethodManager) getSystemService(this.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(expAmt.getWindowToken(), 0);
            expDialog.dismiss();
        });
    }

    /**
     * DIALOGS
     */
    public AlertDialog expenseDialog() {
        // dialog
        dialogBuilder = new AlertDialog.Builder(this);
        final View expView = getLayoutInflater().inflate(R.layout.dialog_expense, null);
        dialogBuilder.setView(expView);
        dialogBuilder.setOnDismissListener(dialogInterface -> {
            InputMethodManager imm1 = (InputMethodManager) getSystemService(this.INPUT_METHOD_SERVICE);
            imm1.hideSoftInputFromWindow(expAmt.getWindowToken(), 0);
            finish();
        });
        AlertDialog expDialog = dialogBuilder.create();
        expDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // set transparent dialog bg
        expDialog.show();
        expDialog.getWindow().setGravity(Gravity.BOTTOM);
        expDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        // get components by id
        expAmt = expView.findViewById(R.id.newEntry_amt);
        expAmt.setFilters(new InputFilter[] { new MoneyValueFilter() });
        expAmt.requestFocus(); // focus on amt and open keyboard
        expAmt.postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(expAmt, 0);
        }, 270);
        expDesc = expView.findViewById(R.id.newEntry_desc);
        expAccName = expView.findViewById(R.id.newEntry_accName); // name
        expCatName = expView.findViewById(R.id.newEntry_catName);
        expAccIcon = expView.findViewById(R.id.newEntry_accIcon); // icon
        expCatIcon = expView.findViewById(R.id.newEntry_catIcon);
        expAcc = expView.findViewById(R.id.chooseAcc); // color
        expCat = expView.findViewById(R.id.chooseCat);
        expDate = expView.findViewById(R.id.expDate);
        expDelBtn = expView.findViewById(R.id.expDelBtn);
        expDateBtn = expView.findViewById(R.id.expDateBtn);
        expSave =  expView.findViewById(R.id.newEntry_save);
        expDesc.setOnFocusChangeListener((view, b) -> {
            if (b) expDesc.setBackground(MainActivity.getIconFromId(this, R.color.white));
            else expDesc.setBackground(new ColorDrawable(android.R.attr.selectableItemBackground));
        });

        return expDialog;
    }
    public <T extends SectionAdapter<? extends Section>> AlertDialog.Builder expOptSectionDialog(T adapter, View expOptSectionView) {
        dialogBuilder = new AlertDialog.Builder(this);
        RecyclerView sectionGrid = expOptSectionView.findViewById(R.id.sectionGrid);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 3, GridLayoutManager.VERTICAL, false);
        sectionGrid.setLayoutManager(gridLayoutManager);
        sectionGrid.setAdapter(adapter);
        dialogBuilder.setView(expOptSectionView);
        return dialogBuilder;
    }
    public void expOptAccDialog(AccountAdapter adapter, Expense exp) {
        final View expOptSectionView = getLayoutInflater().inflate(R.layout.dialog_expense_opt_section, null);
        AlertDialog dialog = expOptSectionDialog(adapter, expOptSectionView).create();
        adapter.setDialog(dialog);

        // set values
        TextView title = expOptSectionView.findViewById(R.id.expOptSectionTitle);
        title.setText(R.string.acc_caps);
        TextView expAccName = this.expAccName;
        ImageButton expAccIcon = this.expAccIcon;
        LinearLayout expAccItem = expAcc;
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
        });

        dialog.show();
    }
    public void expOptCatDialog(CategoryAdapter adapter, Expense exp) {
        final View expOptSectionView = getLayoutInflater().inflate(R.layout.dialog_expense_opt_section, null);
        AlertDialog dialog = expOptSectionDialog(adapter, expOptSectionView).create();
        adapter.setDialog(dialog);

        // set values
        TextView title = expOptSectionView.findViewById(R.id.expOptSectionTitle);
        title.setText(R.string.cat_caps);
        TextView expCatName = this.expCatName;
        ImageButton expCatIcon = this.expCatIcon;
        LinearLayout expCatItem = expCat;
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

    public AccountAdapter getAccountData() {
        return new AccountAdapter(this, db.getAllAccounts());
    }
    public CategoryAdapter getCategoryData() {
        return new CategoryAdapter(this, db.getAllCategories());
    }



}