package com.example.expensetracker;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensetracker.ChartsPage.ChartsFragment;
import com.example.expensetracker.HelperClasses.MoneyValueFilter;
import com.example.expensetracker.RecyclerViewAdapters.AccountAdapter;
import com.example.expensetracker.RecyclerViewAdapters.CategoryAdapter;
import com.example.expensetracker.RecyclerViewAdapters.CurrencyAdapter;
import com.example.expensetracker.RecyclerViewAdapters.DateGridAdapter;
import com.example.expensetracker.RecyclerViewAdapters.ExpenseAdapter;
import com.example.expensetracker.RecyclerViewAdapters.SectionAdapter;
import com.example.expensetracker.HomePage.HomeFragment;
import com.example.expensetracker.ManagePage.ManageChildFragment;
import com.example.expensetracker.ManagePage.ManageFragment;
import com.example.expensetracker.ManagePage.SectionOptDialogFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

import kotlin.Triple;

public class MainActivity extends AppCompatActivity {

    public static String TAG = "MainActivity";

    public DatabaseHelper db = new DatabaseHelper(this);
    public static Locale locale = Locale.getDefault();
    private HashMap<Integer, String> iconMap = new HashMap<>();
    private HashMap<Integer, String> colorMap = new HashMap<>();

    // Dialog components
    private AlertDialog.Builder dialogBuilder;
    private EditText expAmt, expDesc, sectionName;
    private TextView expAccName, expCatName, expDate, sectionType, expCurr, sectionCurr;
    private ImageButton expAccIcon, expCatIcon, sectionIcon;
    private LinearLayout expAccBox, expCatBox, expDelBtn, expDateBtn, expSaveBtn, sectionBanner, sectionCurrRow, sectionDelBtn, sectionSaveBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize default accounts/categories
        if (db.empty(DatabaseHelper.TABLE_ACCOUNT))
            initialiseDefaultAccs();
        if (db.empty(DatabaseHelper.TABLE_CATEGORY))
            initialiseDefaultCats();
        if (db.empty(DatabaseHelper.TABLE_CURRENCY))
            initialiseCurrencies();
        if (iconMap.isEmpty())
            getIconMap();
        if (colorMap.isEmpty())
            getColorMap();

        // Initialize the bottom navigation view
        BottomNavigationView navView = findViewById(R.id.nav_view);
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_manage, R.id.navigation_charts)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);
    }

    /**
     * INITIALISE DEFAULTS
     */
    public void initialiseDefaultAccs() {
        for (int i = 0;i < Constants.defaultAccNames.length;i++) {
            Account acc = new Account(this, Constants.defaultAccNames[i], Constants.defaultAccIcons[i], Constants.defaultAccColors[i]);
            db.createAccount(acc, false);
        }
    }
    public void initialiseDefaultCats() {
        for (int i = 0;i < Constants.defaultCatNames.length;i++) {
            Category cat = new Category(this, Constants.defaultCatNames[i], Constants.defaultCatIcons[i], Constants.defaultCatColors[i]);
            db.createCategory(cat, false);
        }
    }
    public void initialiseCurrencies() {
        for (int i = 0;i < Constants.currencies.size();i++) {
            Triple<String, String, String> triple = Constants.currencies.get(i);
            db.createCurrency(triple.getFirst(), triple.getThird(), triple.getSecond());
        }
    }
    public void getIconMap() {
        iconMap = new HashMap<>();
        for (String name : Constants.allAccIcons) {
            iconMap.put(getIconIdFromName(this, name), name);
        }
        for (String name : Constants.allCatIcons) {
            iconMap.put(getIconIdFromName(this, name), name);
        }
    }
    public void getColorMap() {
        colorMap = new HashMap<>();
        for (String name : Constants.allColors) {
            colorMap.put(getColorIdFromName(this, name), name);
        }
    }

    /**
     * DIALOG TEMPLATES
     */
    public AlertDialog expenseDialog() {
        // dialog
        final View expView = getLayoutInflater().inflate(R.layout.dialog_expense, null);
        dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setView(expView)
                .setOnDismissListener(dialogInterface -> {
            InputMethodManager imm1 = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm1.hideSoftInputFromWindow(expAmt.getWindowToken(), 0);
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
        expAmt.postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(expAmt, 0);
        }, 270);
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
        expSaveBtn =  expView.findViewById(R.id.newExpSave);
        expDesc.setOnFocusChangeListener((view, b) -> {
            if (b) expDesc.setBackground(getIconFromId(this, R.color.white));
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
    public void expenseCatDialog(CategoryAdapter adapter, Expense exp) {
        final View expOptSectionView = getLayoutInflater().inflate(R.layout.dialog_expense_opt_section, null);
        AlertDialog dialog = expenseSectionDialog(adapter, expOptSectionView).create();
        adapter.setDialog(dialog);

        // set values
        TextView title = expOptSectionView.findViewById(R.id.expOptSectionTitle);
        title.setText(R.string.cat_caps);
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
            expCatIcon.setForegroundTintList(getColorStateListFromName(MainActivity.this, selectedCat.getColorName())); // set icon color
            expCatItem.setBackgroundColor(Color.parseColor("#" + selectedCat.getColorHex())); // set bg color
        });

        dialog.show();
    }
    public void expenseAccDialog(AccountAdapter adapter, Expense exp) {
        final View expOptSectionView = getLayoutInflater().inflate(R.layout.dialog_expense_opt_section, null);
        AlertDialog dialog = expenseSectionDialog(adapter, expOptSectionView).create();
        adapter.setDialog(dialog);

        // set values
        TextView title = expOptSectionView.findViewById(R.id.expOptSectionTitle);
        title.setText(R.string.acc_caps);
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
            expAccIcon.setForegroundTintList(getColorStateListFromName(MainActivity.this, selectedAcc.getColorName())); // set icon color
            expAccItem.setBackgroundColor(Color.parseColor("#" + selectedAcc.getColorHex())); // set bg color
            expCurr.setText(selectedAcc.getCurrencySymbol());
        });

        dialog.show();
    }
    public AlertDialog sectionDialog() {
        // dialog
        dialogBuilder = new AlertDialog.Builder(this);
        final View editCatView = getLayoutInflater().inflate(R.layout.dialog_section, null);
        dialogBuilder.setView(editCatView);
        AlertDialog sectionDialog = dialogBuilder.create();
        sectionDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // set transparent dialog bg
        sectionDialog.getWindow().setGravity(Gravity.BOTTOM);
        sectionDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        // get components by id
        sectionType = editCatView.findViewById(R.id.catType);
        sectionName = editCatView.findViewById(R.id.sectionName);
        sectionIcon = editCatView.findViewById(R.id.sectionIcon);
        sectionBanner = editCatView.findViewById(R.id.sectionBanner);
        sectionCurr = editCatView.findViewById(R.id.sectionCurrency);
        sectionCurrRow = editCatView.findViewById(R.id.sectionCurrencyRow);
        sectionDelBtn = editCatView.findViewById(R.id.catDelBtn);
        sectionSaveBtn = editCatView.findViewById(R.id.catSaveBtn);

        return sectionDialog;
    }

    /**
     * UPDATE/GETTERS/SETTERS
     */
    // Update
    public void updateHomeData() {
        ArrayList<Expense> expenses = getExpenseList();
        updateExpenseData(expenses);
        updateSummaryData(expenses);
    }
    public void updateSummaryData(ArrayList<Expense> expenses) {
        Calendar from, to;
        int state;
        if (getFragment() instanceof HomeFragment) {
            HomeFragment fragment = (HomeFragment) getFragment();
            from = fragment.getDateRange()[0];
            to = fragment.getDateRange()[1];
            state = fragment.getSelDateState();
        } else if (getFragment() instanceof ChartsFragment) {
            ChartsFragment fragment = (ChartsFragment) getFragment();
            from = fragment.getDateRange()[0];
            to = fragment.getDateRange()[1];
            state = fragment.getSelDateState();
        } else {
            return;
        }

        String summaryDateText;
        float totalAmt = 0;
        if (state == DateGridAdapter.ALL) {
            summaryDateText = "All time";
            totalAmt = db.getTotalAmt();
        } else {
            Calendar cal = getCalendarCopy(to, DateGridAdapter.FROM);
            String dtf = "";
            switch (state) {
                case DateGridAdapter.MONTH:
                    dtf = "MMM yyyy";
                    break;
                case DateGridAdapter.YEAR:
                    dtf = "yyyy";
                    break;
                default:
                    dtf = "dd MMM yyyy";
            }
            if (getDatetimeStr(from, dtf).equals(getDatetimeStr(cal, dtf))) {
                if (state == DateGridAdapter.DAY || state == DateGridAdapter.SELECT_SINGLE) {
                    summaryDateText = getRelativePrefix(from);
                    summaryDateText += getDatetimeStr(from, ", dd MMM yyyy");
                } else if (state == DateGridAdapter.MONTH)
                    summaryDateText = getDatetimeStr(from, "MMMM yyyy");
                else
                    summaryDateText = getDatetimeStr(from, dtf);
            } else {
                summaryDateText = getDatetimeStr(from, dtf) + " - " + getDatetimeStr(to, dtf);
            }
            for (Expense exp : expenses) totalAmt += exp.getAmount();
        }
        if (getFragment() instanceof HomeFragment) {
            ((HomeFragment) getFragment()).setSummaryData(summaryDateText.toUpperCase(), totalAmt);
        } else if (getFragment() instanceof ChartsFragment) {
            ((ChartsFragment) getFragment()).setSummaryData(summaryDateText.toUpperCase(), totalAmt);
        }
    }
    public void updateExpenseData(ArrayList<Expense> expenses) {
        expenses = insertExpDateHeaders(sortExpenses(expenses, Constants.DESCENDING));
        ExpenseAdapter expAdapter = new ExpenseAdapter(this, expenses);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        if (getFragment() instanceof HomeFragment)
            ((HomeFragment) getFragment()).setExpenseData(linearLayoutManager, expAdapter);
    }
    public void updateAccountData() {
        AccountAdapter adapter = getAccountData(Constants.MANAGE);
        adapter.addNewAcc();
        Fragment childFragment = null;
        if (getFragment() instanceof ManageFragment)
            childFragment = getFragment().getChildFragmentManager().getFragments().get(0);
        if (childFragment instanceof ManageChildFragment)
            ((ManageChildFragment<AccountAdapter>) childFragment).setAdapter(adapter);
    }
    public void updateCategoryData() {
        CategoryAdapter adapter = getCategoryData(Constants.MANAGE);
        adapter.addNewCat();
        Fragment childFragment = null;
        if (getFragment() instanceof ManageFragment)
            childFragment = getFragment().getChildFragmentManager().getFragments().get(1);
        if (childFragment instanceof ManageChildFragment)
            ((ManageChildFragment<CategoryAdapter>) childFragment).setAdapter(adapter);
    }
    public void updateAccFilters(ArrayList<Account> filters) {
        if (getFragment() instanceof HomeFragment) {
            HomeFragment fragment = (HomeFragment) getFragment();
            fragment.setSelAccFilters(filters);
            fragment.applyFilters();
            fragment.updateClearFiltersItem();
        }
    }
    public void updateCatFilters(ArrayList<Category> filters) {
        if (getFragment() instanceof HomeFragment) {
            HomeFragment fragment = (HomeFragment) getFragment();
            fragment.setSelCatFilters(filters);
            fragment.applyFilters();
            fragment.updateClearFiltersItem();
        }
    }

    // Expenses
    public ArrayList<Expense> getExpenseList() {
        ArrayList<Expense> expensesByDate = new ArrayList<>();
        if (getFragment() instanceof HomeFragment) {
            HomeFragment fragment = (HomeFragment) getFragment();
            Calendar from = fragment.getDateRange()[0];
            Calendar to = fragment.getDateRange()[1];
            if (fragment.getSelDateState() == DateGridAdapter.ALL) {
                expensesByDate =  db.getAllExpenses();
            } else {
                expensesByDate = db.getExpensesByDateRange(from, to);
            }
            db.getExpensesByFilters(fragment.getSelAccFilters(), fragment.getSelCatFilters());
            ArrayList<Expense> expensesByFilter = db.getExpensesByFilters(fragment.getSelAccFilters(), fragment.getSelCatFilters());
            expensesByDate.retainAll(expensesByFilter);
        } else if (getFragment() instanceof ChartsFragment) {
            ChartsFragment fragment = (ChartsFragment) getFragment();
            Calendar from = fragment.getDateRange()[0];
            Calendar to = fragment.getDateRange()[1];
            if (fragment.getSelDateState() == DateGridAdapter.ALL) {
                expensesByDate =  db.getAllExpenses();
            } else {
                expensesByDate = db.getExpensesByDateRange(from, to);
            }
        }
        return expensesByDate;
    }
    public static ArrayList<Expense> insertExpDateHeaders(ArrayList<Expense> expenses) {
        int day = -1;
        ArrayList<Expense> newLst = new ArrayList<>();
        for (Expense exp : expenses) {
            if (exp.getDatetime().get(Calendar.DAY_OF_YEAR) != day) {
                newLst.add(new Expense(exp.getDatetime())); // add date header
                day = exp.getDatetime().get(Calendar.DAY_OF_YEAR);
            }
            newLst.add(exp); // add actual expense item
        }
        return newLst;
    }
    public static ArrayList<Expense> sortExpenses(ArrayList<Expense> expenses, int order) {
        if (order == Constants.ASCENDING)
            expenses.sort((e1, e2) -> {
                return e1.getDatetime().compareTo(e2.getDatetime()); // descending order
            });
        else
            expenses.sort((e1, e2) -> {
                return e2.getDatetime().compareTo(e1.getDatetime()); // descending order
            });
        return expenses;
    }

    // Sections
    public CategoryAdapter getCategoryData() {
        return new CategoryAdapter(this, db.getAllCategories());
    }
    public AccountAdapter getAccountData() {
        return new AccountAdapter(this, db.getAllAccounts());
    }
    public CategoryAdapter getCategoryData(int mode) {
        return new CategoryAdapter(this, db.getAllCategories(), mode);
    }
    public AccountAdapter getAccountData(int mode) {
        return new AccountAdapter(this, db.getAllAccounts(), mode);
    }

    /**
     * ADD/EDIT DIALOGS
     */
    public void addExpense() {
        AlertDialog expDialog = expenseDialog();
        expDelBtn.setVisibility(LinearLayout.INVISIBLE);
        Calendar cal = Calendar.getInstance(locale);
        expDate.setText(("Today, " + getDatetimeStr(cal, "dd MMMM yyyy")).toUpperCase());
        expCurr.setText(db.getAccount(1).getCurrencySymbol());

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
            AlertDialog.Builder changeDate = new AlertDialog.Builder(MainActivity.this);
            DatePicker datePicker = new DatePicker(MainActivity.this);
            changeDate.setView(datePicker)
                    .setPositiveButton(android.R.string.yes, (dialogInterface, i) -> {
                cal.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth());
                expDate.setText((getRelativePrefix(cal) + getDatetimeStr(cal, ", dd MMM yyyy")).toUpperCase());
            })
                    .setNeutralButton(android.R.string.no, (dialog, which) -> dialog.cancel())
                    .show();
        });
        expSaveBtn.setOnClickListener(v -> {
            if (!expAmt.getText().toString().isEmpty()) {
                float amt = Float.parseFloat(expAmt.getText().toString());
                String desc = expDesc.getText().toString();
                Account acc = db.getAccount(expAccName.getText().toString());
                Category cat = db.getCategory(expCatName.getText().toString());
                String datetime = getDatetimeStr(cal, "");
                Expense expense = new Expense(amt, desc, acc, cat, datetime);
                db.createExpense(expense);
                updateHomeData(); // update summary & expense list
            } else {
                Toast.makeText(MainActivity.this, "Amount cannot be 0. No expense created", Toast.LENGTH_SHORT).show();
            }
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(expAmt.getWindowToken(), 0);
            expDialog.dismiss();
        });
    }
    public void editExpense(int id) {
        AlertDialog expDialog = expenseDialog();
        Expense exp = db.getExpense(id);

        // set values
        expAmt.setText(String.format(locale, "%.2f", exp.getAmount()));
        expAmt.setSelection(expAmt.getText().length()); // set cursor to end of text
        expDesc.setText(exp.getDescription());
        Account acc = exp.getAccount();
        Category cat = exp.getCategory();
        expAccName.setText(acc.getName()); // set name
        expCatName.setText(cat.getName());
        expAccIcon.setForeground(acc.getIcon()); // set icon
        expCatIcon.setForeground(cat.getIcon());
        expAccIcon.setForegroundTintList(ColorStateList.valueOf(Color.parseColor("#" + acc.getColorHex()))); // set icon color
        expCatIcon.setForegroundTintList(ColorStateList.valueOf(Color.parseColor("#" + cat.getColorHex())));
        expAccBox.setBackgroundColor(Color.parseColor("#" + acc.getColorHex())); // set bg color
        expCatBox.setBackgroundColor(Color.parseColor("#" + cat.getColorHex()));
        expCurr.setText(acc.getCurrencySymbol());

        Calendar today = Calendar.getInstance(locale);
        String datePrefix = (exp.getRelativeDate() == Constants.TODAY) ? "Today" :
                ((exp.getRelativeDate() == Constants.YESTERDAY) ? "Yesterday" : exp.getDatetimeStr("EEE"));
        expDate.setText((datePrefix + ", " + exp.getDatetimeStr("dd MMMM yyyy")).toUpperCase());

        // actions
        expAccBox.setOnClickListener(view -> {
            AccountAdapter accAdapter = getAccountData();
            accAdapter.setSelected(expAccName.getText().toString());
            expenseAccDialog(accAdapter, exp);
        });
        expCatBox.setOnClickListener(view -> {
            CategoryAdapter catAdapter = getCategoryData();
            catAdapter.setSelected(expCatName.getText().toString());
            expenseCatDialog(catAdapter, exp);
        });

        expSaveBtn.setOnClickListener(v -> {
            // get values of inputs
            if (!expAmt.getText().toString().isEmpty()) {
                float amt = Float.parseFloat(expAmt.getText().toString());
                String desc = expDesc.getText().toString();
                Account acc1 = db.getAccount(expAccName.getText().toString());
                Category cat1 = db.getCategory(expCatName.getText().toString());
                desc = (desc.isEmpty()) ? cat1.getName() : desc;
                Expense expense = new Expense(id, amt, desc, acc1, cat1, exp.getDatetime());
                db.updateExpense(expense);

                updateHomeData(); // update data lists
            } else {
                Toast.makeText(MainActivity.this, "Amount cannot be 0. Expense not updated", Toast.LENGTH_SHORT).show();
            }
            expDialog.dismiss();
        });

        expDelBtn.setOnClickListener(view -> {
            AlertDialog.Builder confirmDel = new AlertDialog.Builder(MainActivity.this, R.style.ConfirmDelDialog);
            confirmDel.setTitle("Delete expense")
                    .setMessage("Are you sure you want to delete?")
                    .setPositiveButton("Delete", (dialogInterface, i) -> {
                db.deleteExpense(exp);
                Toast.makeText(MainActivity.this, "Expense deleted", Toast.LENGTH_SHORT).show();
                updateHomeData();
                expDialog.dismiss();
            })
                    .setNeutralButton(android.R.string.no, (dialog, which) -> {
                dialog.cancel(); // close dialog
            })
                    .show();
        });

        expDateBtn.setOnClickListener(view -> {
            AlertDialog.Builder changeDate = new AlertDialog.Builder(MainActivity.this);
            DatePicker datePicker = new DatePicker(MainActivity.this);
            Calendar expDatetime = exp.getDatetime();
            datePicker.updateDate(expDatetime.get(Calendar.YEAR), expDatetime.get(Calendar.MONTH), expDatetime.get(Calendar.DAY_OF_MONTH));
            changeDate.setView(datePicker)
                    .setPositiveButton(android.R.string.yes, (dialogInterface, i) -> {
                expDatetime.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth());
                exp.setDatetime(expDatetime);
                String datePrefix1 = "";
                if (exp.getDatetime().get(Calendar.YEAR) == today.get(Calendar.YEAR)) {
                    int dateDiff = exp.getDatetime().get(Calendar.DAY_OF_YEAR) - today.get(Calendar.DAY_OF_YEAR);
                    datePrefix1 = (dateDiff == 0) ? "TODAY, " : ((dateDiff == -1) ? "YESTERDAY, " : "");
                }
                expDate.setText(datePrefix1 + exp.getDatetimeStr("dd MMMM yyyy").toUpperCase());
            })
                    .setNeutralButton(android.R.string.no, (dialog, which) -> {
                dialog.cancel(); // close dialog
            })
                    .show();
        });
    }
    public void addAccount() {
        AlertDialog addAccDialog = sectionDialog();
        sectionDelBtn.setVisibility(LinearLayout.GONE);

        // set values of selected category
        sectionName.setText("");
        sectionType.setText("New account");
        setEditAccOptions(iconMap.get(R.drawable.acc_cash), colorMap.get(R.color.cat_bleu_de_france));

        // actions
        sectionCurrRow.setOnClickListener(view1 -> {
            CurrencyAdapter adapter = new CurrencyAdapter(this, db.getAllCurrencies(), sectionCurr.getText().toString());
            final View view = getLayoutInflater().inflate(R.layout.dialog_recyclerview, null);
            RecyclerView currencyList = view.findViewById(R.id.recyclerView);
            currencyList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
            currencyList.setAdapter(adapter);

            dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle("Account currency")
                    .setView(view)
                    .setPositiveButton(android.R.string.yes, (dialogInterface, i) -> sectionCurr.setText(adapter.getSelected()))
                    .setNeutralButton(android.R.string.no, (dialogInterface, i) -> {});
            dialogBuilder.create().show();

        });
        sectionIcon.setOnClickListener(view -> {
            int iconId = (Integer) sectionIcon.getTag();
            int colorId = (Integer) sectionBanner.getTag();
            SectionOptDialogFragment sectionOptDialog = new SectionOptDialogFragment(Constants.SECTION_ACCOUNT, iconMap.get(iconId), colorMap.get(colorId));
            sectionOptDialog.show(getSupportFragmentManager(), "sectionOptDialog");
        });
        sectionSaveBtn.setOnClickListener(view -> {
            if (!sectionName.getText().toString().isEmpty()) {
                String name = sectionName.getText().toString();
                int icon_id = (Integer) sectionIcon.getTag();
                int color_id = (Integer) sectionBanner.getTag();
                Currency currency = getCurrencyFromName(sectionCurr.getText().toString());
                Account account = new Account(MainActivity.this, name, iconMap.get(icon_id), colorMap.get(color_id), currency);
                db.createAccount(account, true);
                updateAccountData();
                addAccDialog.dismiss();
            } else {
                Toast.makeText(MainActivity.this, "Account name cannot be empty.", Toast.LENGTH_SHORT).show();
            }

        });

        addAccDialog.show();
    }
    public void editAccount(Account acc) {
        AlertDialog dialog = sectionDialog();

        // set values of selected category
        sectionName.setText(acc.getName());
        sectionType.setText("Account");
        sectionCurr.setText(acc.getCurrencyName());
        setEditAccOptions(acc.getIconName(), acc.getColorName());

        // actions
        sectionCurrRow.setOnClickListener(view1 -> {
            CurrencyAdapter adapter = new CurrencyAdapter(this, db.getAllCurrencies(), sectionCurr.getText().toString());
            final View view = getLayoutInflater().inflate(R.layout.dialog_recyclerview, null);
            RecyclerView currencyList = view.findViewById(R.id.recyclerView);
            currencyList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
            currencyList.setAdapter(adapter);

            dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle("Account currency")
                    .setView(view)
                    .setPositiveButton(android.R.string.yes, (dialogInterface, i) -> sectionCurr.setText(adapter.getSelected()))
                    .setNeutralButton(android.R.string.no, (dialogInterface, i) -> {});
            dialogBuilder.create().show();

        });
        sectionIcon.setOnClickListener(view -> {
            int iconId = (Integer) sectionIcon.getTag();
            int colorId = (Integer) sectionBanner.getTag();
            SectionOptDialogFragment sectionOptDialog = new SectionOptDialogFragment(Constants.SECTION_ACCOUNT, iconMap.get(iconId), colorMap.get(colorId));
            sectionOptDialog.show(getSupportFragmentManager(), "sectionOptDialog");
        });
        sectionDelBtn.setOnClickListener(view -> {
            AlertDialog.Builder confirmDel = new AlertDialog.Builder(MainActivity.this, R.style.ConfirmDelDialog);
            confirmDel.setTitle("Delete entry")
                    .setMessage("Are you sure you want to delete? Relevant expenses will be moved to Cash.")
                    .setPositiveButton("Delete", (dialogInterface, i) -> {
                db.deleteAccount(acc, true);
                updateAccountData();
                updateHomeData();
                dialog.dismiss();
            })
                    .setNeutralButton(android.R.string.no, (dialog1, which) -> dialog1.cancel())
                    .show();
        });
        sectionSaveBtn.setOnClickListener(view -> {
            if (!sectionName.getText().toString().isEmpty()) {
                int id = acc.getId();
                String name = sectionName.getText().toString();
                int icon_id = (Integer) sectionIcon.getTag();
                int color_id = (Integer) sectionBanner.getTag();
                Currency currency = getCurrencyFromName(sectionCurr.getText().toString());
                db.updateAccount(new Account(MainActivity.this, id, name, iconMap.get(icon_id), colorMap.get(color_id), currency));
                updateAccountData();
                updateHomeData();
            }
            dialog.dismiss();
        });

        dialog.show();
    }
    public void addCategory() {
        AlertDialog addCatDialog = sectionDialog();
        sectionCurrRow.setVisibility(LinearLayout.GONE);
        sectionDelBtn.setVisibility(LinearLayout.GONE);

        // set values of selected category
        sectionName.setText("");
        sectionType.setText("New category");
        setEditCatOptions(iconMap.get(R.drawable.cat_others), colorMap.get(R.color.cat_fiery_fuchsia));

        // actions
        sectionIcon.setOnClickListener(view -> {
            int iconId = (Integer) sectionIcon.getTag();
            int colorId = (Integer) sectionBanner.getTag();
            SectionOptDialogFragment sectionOptDialog = new SectionOptDialogFragment(Constants.SECTION_CATEGORY, iconMap.get(iconId), colorMap.get(colorId));
            sectionOptDialog.show(getSupportFragmentManager(), "sectionOptDialog");
        });
        sectionSaveBtn.setOnClickListener(view -> {
            if (!sectionName.getText().toString().isEmpty()) {
                String name = sectionName.getText().toString();
                int icon_id = (Integer) sectionIcon.getTag();
                int color_id = (Integer) sectionBanner.getTag();
                Category cat = new Category(MainActivity.this, name, iconMap.get(icon_id), colorMap.get(color_id));
                db.createCategory(cat, true);
                updateCategoryData();
                addCatDialog.dismiss();
            } else {
                Toast.makeText(MainActivity.this, "Category name cannot be empty.", Toast.LENGTH_SHORT).show();
            }

        });

        addCatDialog.show();
    }
    public void editCategory(Category cat) {
        AlertDialog dialog = sectionDialog();
        sectionCurrRow.setVisibility(LinearLayout.GONE);

        // set values of selected category
        sectionName.setText(cat.getName());
        sectionType.setText("Category");
        setEditCatOptions(cat.getIconName(), cat.getColorName());

        // actions
        sectionIcon.setOnClickListener(view -> {
            int iconId = (Integer) sectionIcon.getTag();
            int colorId = (Integer) sectionBanner.getTag();
            SectionOptDialogFragment sectionOptDialog = new SectionOptDialogFragment(Constants.SECTION_CATEGORY, iconMap.get(iconId), colorMap.get(colorId));
            sectionOptDialog.show(getSupportFragmentManager(), "sectionOptDialog");
        });
        sectionDelBtn.setOnClickListener(view -> {
            AlertDialog.Builder confirmDel = new AlertDialog.Builder(MainActivity.this, R.style.ConfirmDelDialog);
            confirmDel.setTitle("Delete entry")
                    .setMessage("Are you sure you want to delete? Relevant expenses will be moved to Others.")
                    .setPositiveButton("Delete", (dialogInterface, i) -> {
                db.deleteCategory(cat, true);
                updateCategoryData();
                updateHomeData();
                dialog.dismiss();
            })
                    .setNeutralButton(android.R.string.no, (dialog1, which) -> dialog1.cancel())
                    .show();
        });
        sectionSaveBtn.setOnClickListener(view -> {
            if (!sectionName.getText().toString().isEmpty()) {
                int id = cat.getId();
                String name = sectionName.getText().toString();
                int icon_id = (Integer) sectionIcon.getTag();
                int color_id = (Integer) sectionBanner.getTag();
                db.updateCategory(new Category(MainActivity.this, id, name, iconMap.get(icon_id), colorMap.get(color_id)));
                updateCategoryData();
                updateHomeData();
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    public void setEditSectionOptions(String iconName, String colorName) {
        Drawable icon = MainActivity.getIconFromName(this, iconName);
        String colorHex = MainActivity.getColorHexFromName(this, colorName);
        sectionIcon.setForeground(icon); // set icon
        sectionIcon.setForegroundTintList(ColorStateList.valueOf(Color.parseColor("#" + colorHex))); // set icon color
        sectionBanner.setBackgroundColor(Color.parseColor("#" + colorHex)); // set bg color
        sectionIcon.setTag(MainActivity.getIconIdFromName(this, iconName));
        sectionBanner.setTag(MainActivity.getColorIdFromName(this, colorName));
    }
    public void setEditAccOptions(String iconName, String colorName) {
        setEditSectionOptions(iconName, colorName);
        sectionIcon.setBackground(getIconFromId(this, R.drawable.shape_rounded_square));
        sectionIcon.setImageResource(R.drawable.selector_rounded_square);
    }
    public void setEditCatOptions(String iconName, String colorName) {
        setEditSectionOptions(iconName, colorName);
        sectionIcon.setBackground(getIconFromId(this, R.drawable.shape_circle));
        sectionIcon.setImageResource(R.drawable.selector_circle);
    }

    /**
     * FUNCTIONS
     */
    public Fragment getFragment() {
        Fragment navHostFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_activity_main);
        return (navHostFragment == null) ? null : navHostFragment.getChildFragmentManager().getFragments().get(0);
    }

    /**
     * STATIC METHODS
     */
    public static int getIconIdFromName(Context context, String name) {
        return context.getResources().getIdentifier(name, "drawable", context.getPackageName());
    }
    public static Drawable getIconFromName(Context context, String name) {
        return getIconFromId(context, getIconIdFromName(context, name));
    }
    public static Drawable getIconFromId(Context context, int id) {
        return ContextCompat.getDrawable(context, id);
    }
    public static int getColorIdFromName(Context context, String name) {
        return context.getResources().getIdentifier(name, "color", context.getPackageName());
    }
    public static String getColorHexFromName(Context context, String name) {
        return getColorHexFromId(context, getColorIdFromName(context, name));
    }
    public static String getColorHexFromId(Context context, int id) {
        return Integer.toHexString(ContextCompat.getColor(context.getApplicationContext(), id));
    }
    public static ColorStateList getColorStateListFromName(Context context, String name) {
        return getColorStateListFromHex(getColorHexFromName(context, name));
    }
    public static ColorStateList getColorStateListFromHex(String hex) {
        return ColorStateList.valueOf(Color.parseColor("#" + hex));
    }
    public static ColorStateList getColorStateListFromId(Context context, int id) {
        return ColorStateList.valueOf(ContextCompat.getColor(context, id));
    }
    public static int getRelativeDate(Calendar cal) {
        Calendar today = Calendar.getInstance(locale);
        if (cal.get(Calendar.YEAR) == today.get(Calendar.YEAR)) {
            int dateDiff = cal.get(Calendar.DAY_OF_YEAR) - today.get(Calendar.DAY_OF_YEAR);
            if (dateDiff == 0 || dateDiff == -1) {
                return (dateDiff == 0) ? Constants.TODAY : Constants.YESTERDAY;
            } else {
                today.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
                today.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
                if (cal.compareTo(today) > -1 && cal.compareTo(today) < 0)
                    return Constants.THIS_WEEK;
                today.set(Calendar.WEEK_OF_YEAR, today.get(Calendar.WEEK_OF_YEAR)-1);
                today.set(Calendar.WEEK_OF_YEAR, today.get(Calendar.WEEK_OF_YEAR)-1);
                if (cal.compareTo(today) > -1 && cal.compareTo(today) <= 0)
                    return Constants.LAST_WEEK;
            }
        }
        return -1;
    }
    public static String getRelativePrefix(Calendar cal) {
        int relativeDate = getRelativeDate(cal);
        return (relativeDate == Constants.TODAY) ? "Today" : (
                    (relativeDate == Constants.YESTERDAY) ? "Yesterday" : getDatetimeStr(cal, "EEE"));
    }
    public static String getDatetimeStr(Calendar cal, String format) {
        format = (format.isEmpty()) ? Expense.DATETIME_FORMAT : format;
        return new SimpleDateFormat(format, locale).format(cal.getTime());
    }
    public static float convertDpToPx(Context context, float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                context.getResources().getDisplayMetrics());
    }
    public static Currency getCurrencyFromName(String name) {
        return new Currency(name, "", Constants.currency_map.get(name));
    }
    public static String getString(Context context, int id) {
        return context.getResources().getString(id);
    }

    public static Calendar getCalendarCopy(Calendar cal, int range) {
        Calendar copy = Calendar.getInstance();
        if (range == DateGridAdapter.FROM)
            copy.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 0,0,0);
        else
            copy.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 23,59,59);
        return copy;
    }
}