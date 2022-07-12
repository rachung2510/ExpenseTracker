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
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensetracker.HelperClasses.MoneyValueFilter;
import com.example.expensetracker.RecyclerViewAdapters.AccountAdapter;
import com.example.expensetracker.RecyclerViewAdapters.CategoryAdapter;
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

public class MainActivity extends AppCompatActivity {

    public static String TAG = "MainActivity";

    public DatabaseHelper db = new DatabaseHelper(this);
    public static Locale locale = Locale.getDefault();
    private HashMap<Integer, String> iconMap = new HashMap<>();
    private HashMap<Integer, String> colorMap = new HashMap<>();

    // Dialog components
    private AlertDialog.Builder dialogBuilder;
    private EditText expAmt, expDesc, catName;
    private TextView expAccName, expCatName, expDate, catTypeView;
    private ImageButton expAccIcon, expCatIcon, catIcon;
    private LinearLayout expAcc, expCat, expDelBtn, expDateBtn, expSave, catBanner, catDelBtn, catSaveBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        binding = ActivityMainBinding.inflate(getLayoutInflater());
//        setContentView(binding.getRoot());

        // Initialize default accounts/categories
        if (db.empty(DatabaseHelper.TABLE_ACCOUNT))
            initialiseDefaultAccs();
        if (db.empty(DatabaseHelper.TABLE_CATEGORY))
            initialiseDefaultCats();
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
        dialogBuilder = new AlertDialog.Builder(this);
        final View expView = getLayoutInflater().inflate(R.layout.dialog_expense, null);
        dialogBuilder.setView(expView);
        AlertDialog expDialog = dialogBuilder.create();
        expDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // set transparent dialog bg
        expDialog.show();
        expDialog.getWindow().setGravity(Gravity.BOTTOM);
        expDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        // get components by id
        expAmt = expView.findViewById(R.id.newEntry_amt);
        expAmt.setFilters(new InputFilter[] { new MoneyValueFilter() });
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
            if (b) expDesc.setBackground(getIconFromId(this, R.color.white));
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
            expCatIcon.setForegroundTintList(getColorStateListFromName(MainActivity.this, selectedCat.getColorName())); // set icon color
            expCatItem.setBackgroundColor(Color.parseColor("#" + selectedCat.getColorHex())); // set bg color
        });

        dialog.show();
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
            expAccIcon.setForegroundTintList(getColorStateListFromName(MainActivity.this, selectedAcc.getColorName())); // set icon color
            expAccItem.setBackgroundColor(Color.parseColor("#" + selectedAcc.getColorHex())); // set bg color
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
        catTypeView = editCatView.findViewById(R.id.catType);
        catName = editCatView.findViewById(R.id.catName);
        catIcon = editCatView.findViewById(R.id.catIcon);
        catBanner = editCatView.findViewById(R.id.catBanner);
        catDelBtn = editCatView.findViewById(R.id.catDelBtn);
        catSaveBtn = editCatView.findViewById(R.id.catSaveBtn);

        return sectionDialog;
    }

    /**
     * UPDATE/GETTERS/SETTERS
     */
    // Update
    public void updateHomeData() {
        ArrayList<Expense> expenses = getExpenseList();
        getExpenseData(expenses);
        getSummaryData(expenses);
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
    public void getExpenseData() {
        getExpenseData(getExpenseList());
    }
    public void getExpenseData(ArrayList<Expense> expenses) {
        expenses = insertExpDateHeaders(sortExpenses(expenses));
        ExpenseAdapter expAdapter = new ExpenseAdapter(this, expenses);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        if (getFragment() instanceof HomeFragment)
            ((HomeFragment) getFragment()).setExpenseData(linearLayoutManager, expAdapter);
    }
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
        }
        return expensesByDate;
    }
    public ArrayList<Expense> insertExpDateHeaders(ArrayList<Expense> expenses) {
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
    public ArrayList<Expense> sortExpenses(ArrayList<Expense> expenses) {
        expenses.sort((e1, e2) -> {
            return e2.getDatetime().compareTo(e1.getDatetime()); // descending order
        });
        return expenses;
    }

    // Summary
    public void getSummaryData(ArrayList<Expense> expenses) {
        if (getFragment() instanceof HomeFragment) {
            HomeFragment fragment = (HomeFragment) getFragment();
            Calendar from = fragment.getDateRange()[0];
            Calendar to = fragment.getDateRange()[1];
            int state = fragment.getSelDateState();
            String summaryDateText;
            float totalAmt = 0;

            if (state == DateGridAdapter.ALL) {
                summaryDateText = "All time";
            } else {
                // set date
                Calendar cal = Calendar.getInstance();
                cal.set(to.get(Calendar.YEAR), to.get(Calendar.MONTH), to.get(Calendar. DAY_OF_MONTH), 0, 0, 0);
                SimpleDateFormat sdf;
                switch (state) {
                    case DateGridAdapter.MONTH:
                        sdf = new SimpleDateFormat("MMM yyyy", locale);
                        break;
                    case DateGridAdapter.YEAR:
                        sdf = new SimpleDateFormat("yyyy", locale);
                        break;
                    default:
                        sdf = new SimpleDateFormat("dd MMM yyyy", locale);
                }
                if (sdf.format(from.getTime()).equals(sdf.format(cal.getTime()))) {
                    int relativeDate = getRelativeDate(from);
                    if (state == DateGridAdapter.DAY || state == DateGridAdapter.SELECT_SINGLE) {
                        summaryDateText = (relativeDate == Constants.TODAY) ? "Today" : (
                                (relativeDate == Constants.YESTERDAY) ? "Yesterday" : new SimpleDateFormat("EEE", locale).format(from.getTime()));
                        summaryDateText += new SimpleDateFormat(", dd MMM yyyy", locale).format(from.getTime());
                    } else if (state == DateGridAdapter.MONTH)
                        summaryDateText = new SimpleDateFormat("MMMM yyyy", locale).format(from.getTime());
                    else
                        summaryDateText = sdf.format(from.getTime());
                } else {
                    summaryDateText = sdf.format(from.getTime()) + " - " + sdf.format(to.getTime());
                }
            }
            for (Expense exp : expenses) totalAmt += exp.getAmount();
            fragment.setSummaryData(summaryDateText.toUpperCase(), totalAmt);
        }
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

    /**
     * ADD/EDIT DIALOGS
     */
    // Expenses
    public void addExpense() {
        AlertDialog expDialog = expenseDialog();
        expDelBtn.setVisibility(LinearLayout.INVISIBLE);

        Calendar cal = Calendar.getInstance(locale);
        expDate.setText(("Today, " + new SimpleDateFormat("dd MMMM yyyy", locale).format(cal.getTime())).toUpperCase());

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
            AlertDialog.Builder changeDate = new AlertDialog.Builder(MainActivity.this);
            DatePicker datePicker = new DatePicker(MainActivity.this);
            changeDate.setView(datePicker);
            changeDate.setPositiveButton(android.R.string.yes, (dialogInterface, i) -> {
                cal.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth());
                int relativeDate = getRelativeDate(cal);
                String datePrefix = (relativeDate == Constants.TODAY) ? "Today" :
                        ((relativeDate == Constants.YESTERDAY) ? "Yesterday" : new SimpleDateFormat("EEE", locale).format(cal.getTime()));
                expDate.setText((datePrefix + ", " + new SimpleDateFormat("dd MMMM yyyy", locale).format(cal.getTime())).toUpperCase());
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
                String datetime = new SimpleDateFormat(Expense.DATETIME_FORMAT, locale).format(cal.getTime());
                Expense expense = new Expense(amt, desc, acc, cat, datetime);
                db.createExpense(expense);
                updateHomeData(); // update summary & expense list
            } else {
                Toast.makeText(MainActivity.this, "Amount cannot be 0. No expense created", Toast.LENGTH_SHORT).show();
            }
            expDialog.dismiss();
        });
    }
    public void editExpense(int id) {
        AlertDialog expDialog = expenseDialog();
        Expense exp = db.getExpense(id);

        // set values
        expAmt.setText(String.format(locale, "%.2f", exp.getAmount()));
        expDesc.setText(exp.getDescription());
        Account acc = exp.getAccount();
        Category cat = exp.getCategory();
        expAccName.setText(acc.getName()); // set name
        expCatName.setText(cat.getName());
        expAccIcon.setForeground(acc.getIcon()); // set icon
        expCatIcon.setForeground(cat.getIcon());
        expAccIcon.setForegroundTintList(ColorStateList.valueOf(Color.parseColor("#" + acc.getColorHex()))); // set icon color
        expCatIcon.setForegroundTintList(ColorStateList.valueOf(Color.parseColor("#" + cat.getColorHex())));
        expAcc.setBackgroundColor(Color.parseColor("#" + acc.getColorHex())); // set bg color
        expCat.setBackgroundColor(Color.parseColor("#" + cat.getColorHex()));

        Calendar today = Calendar.getInstance(locale);
        String datePrefix = (exp.getRelativeDate() == Constants.TODAY) ? "Today" :
                ((exp.getRelativeDate() == Constants.YESTERDAY) ? "Yesterday" : exp.getDatetimeStr("EEE"));
        expDate.setText((datePrefix + ", " + exp.getDatetimeStr("dd MMMM yyyy")).toUpperCase());

        // actions
        expAcc.setOnClickListener(view -> {
            AccountAdapter accAdapter = getAccountData();
            accAdapter.setSelected(expAccName.getText().toString());
            expOptAccDialog(accAdapter, exp);
        });
        expCat.setOnClickListener(view -> {
            CategoryAdapter catAdapter = getCategoryData();
            catAdapter.setSelected(expCatName.getText().toString());
            expOptCatDialog(catAdapter, exp);
        });

        expSave.setOnClickListener(v -> {
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
            confirmDel.setTitle("Delete expense");
            confirmDel.setMessage("Are you sure you want to delete?");
            confirmDel.setPositiveButton("Delete", (dialogInterface, i) -> {
                db.deleteExpense(exp);
                Toast.makeText(MainActivity.this, "Expense deleted", Toast.LENGTH_SHORT).show();
                updateHomeData();
                expDialog.dismiss();
            });
            confirmDel.setNeutralButton(android.R.string.no, (dialog, which) -> {
                dialog.cancel(); // close dialog
            });
            confirmDel.show();
        });

        expDateBtn.setOnClickListener(view -> {
            AlertDialog.Builder changeDate = new AlertDialog.Builder(MainActivity.this);
            DatePicker datePicker = new DatePicker(MainActivity.this);
            Calendar expDatetime = exp.getDatetime();
            datePicker.updateDate(expDatetime.get(Calendar.YEAR), expDatetime.get(Calendar.MONTH), expDatetime.get(Calendar.DAY_OF_MONTH));
            changeDate.setView(datePicker);
            changeDate.setPositiveButton(android.R.string.yes, (dialogInterface, i) -> {
                expDatetime.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth());
                exp.setDatetime(expDatetime);
                String datePrefix1 = "";
                if (exp.getDatetime().get(Calendar.YEAR) == today.get(Calendar.YEAR)) {
                    int dateDiff = exp.getDatetime().get(Calendar.DAY_OF_YEAR) - today.get(Calendar.DAY_OF_YEAR);
                    datePrefix1 = (dateDiff == 0) ? "TODAY, " : ((dateDiff == -1) ? "YESTERDAY, " : "");
                }
                expDate.setText(datePrefix1 + exp.getDatetimeStr("dd MMMM yyyy").toUpperCase());
            });
            changeDate.setNeutralButton(android.R.string.no, (dialog, which) -> {
                dialog.cancel(); // close dialog
            });
            changeDate.show();
        });

        expDialog.setOnCancelListener(dialogInterface -> {
            AlertDialog.Builder confirmCancel = new AlertDialog.Builder(MainActivity.this, R.style.DiscardChangesDialog);
            confirmCancel.setMessage("Discard changes to this entry?");
            confirmCancel.setPositiveButton("keep editing", (dialog, which) -> {
                dialog.cancel(); // close dialog
                expDialog.show();
            });
            confirmCancel.setNegativeButton("Discard changes", (dialog, which) -> expDialog.dismiss());
//                confirmCancel.show();
        });
    }

    public void addAccount() {
        AlertDialog addAccDialog = sectionDialog();
        catDelBtn.setVisibility(LinearLayout.GONE);

        // set values of selected category
        catName.setText("");
        catTypeView.setText("New Account");
        setEditAccOptions(iconMap.get(R.drawable.acc_cash), colorMap.get(R.color.cat_bleu_de_france));

        // actions
        catIcon.setOnClickListener(view -> {
            int iconId = (Integer) catIcon.getTag();
            int colorId = (Integer) catBanner.getTag();
            SectionOptDialogFragment sectionOptDialog = new SectionOptDialogFragment(Constants.SECTION_ACCOUNT, iconMap.get(iconId), colorMap.get(colorId));
            sectionOptDialog.show(getSupportFragmentManager(), "sectionOptDialog");
        });
        catSaveBtn.setOnClickListener(view -> {
            if (!catName.getText().toString().isEmpty()) {
                String name = catName.getText().toString();
                int icon_id = (Integer) catIcon.getTag();
                int color_id = (Integer) catBanner.getTag();
                Account account = new Account(MainActivity.this, name, iconMap.get(icon_id), colorMap.get(color_id));
                db.createAccount(account, true);
                updateAccountData();
                addAccDialog.dismiss();
            } else {
                Toast.makeText(MainActivity.this, "Account name cannot be empty.", Toast.LENGTH_SHORT).show();
            }

        });

        addAccDialog.show();
    }
    public void addCategory() {
        AlertDialog addCatDialog = sectionDialog();
        catDelBtn.setVisibility(LinearLayout.GONE);

        // set values of selected category
        catName.setText("");
        catTypeView.setText("New Category");
        setEditCatOptions(iconMap.get(R.drawable.cat_others), colorMap.get(R.color.cat_fiery_fuchsia));

        // actions
        catIcon.setOnClickListener(view -> {
            int iconId = (Integer) catIcon.getTag();
            int colorId = (Integer) catBanner.getTag();
            SectionOptDialogFragment sectionOptDialog = new SectionOptDialogFragment(Constants.SECTION_CATEGORY, iconMap.get(iconId), colorMap.get(colorId));
            sectionOptDialog.show(getSupportFragmentManager(), "sectionOptDialog");
        });
        catSaveBtn.setOnClickListener(view -> {
            if (!catName.getText().toString().isEmpty()) {
                String name = catName.getText().toString();
                int icon_id = (Integer) catIcon.getTag();
                int color_id = (Integer) catBanner.getTag();
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

    public void editAccount(Account acc) {
        AlertDialog dialog = sectionDialog();

        // set values of selected category
        catName.setText(acc.getName());
        catTypeView.setText("Account");
        setEditAccOptions(acc.getIconName(), acc.getColorName());

        // actions
        catIcon.setOnClickListener(view -> {
            int iconId = (Integer) catIcon.getTag();
            int colorId = (Integer) catBanner.getTag();
            SectionOptDialogFragment sectionOptDialog = new SectionOptDialogFragment(Constants.SECTION_ACCOUNT, iconMap.get(iconId), colorMap.get(colorId));
            sectionOptDialog.show(getSupportFragmentManager(), "sectionOptDialog");
        });
        catDelBtn.setOnClickListener(view -> {
            AlertDialog.Builder confirmDel = new AlertDialog.Builder(MainActivity.this, R.style.ConfirmDelDialog);
            confirmDel.setTitle("Delete entry");
            confirmDel.setMessage("Are you sure you want to delete? Relevant expenses will be moved to Cash.");
            confirmDel.setPositiveButton("Delete", (dialogInterface, i) -> {
                db.deleteAccount(acc, true);
                updateAccountData();
                updateHomeData();
                dialog.dismiss();
            });
            confirmDel.setNeutralButton(android.R.string.no, (dialog1, which) -> dialog1.cancel());
            confirmDel.show();
        });
        catSaveBtn.setOnClickListener(view -> {
            if (!catName.getText().toString().isEmpty()) {
                int id = acc.getId();
                String name = catName.getText().toString();
                int icon_id = (Integer) catIcon.getTag();
                int color_id = (Integer) catBanner.getTag();
                db.updateAccount(new Account(MainActivity.this, id, name, iconMap.get(icon_id), colorMap.get(color_id)));
                updateAccountData();
                updateHomeData();
            }
            dialog.dismiss();
        });

        dialog.show();
    }
    public void editCategory(Category cat) {
        AlertDialog dialog = sectionDialog();

        // set values of selected category
        catName.setText(cat.getName());
        catTypeView.setText("Category");
        setEditCatOptions(cat.getIconName(), cat.getColorName());

        // actions
        catIcon.setOnClickListener(view -> {
            int iconId = (Integer) catIcon.getTag();
            int colorId = (Integer) catBanner.getTag();
            SectionOptDialogFragment sectionOptDialog = new SectionOptDialogFragment(Constants.SECTION_CATEGORY, iconMap.get(iconId), colorMap.get(colorId));
            sectionOptDialog.show(getSupportFragmentManager(), "sectionOptDialog");
        });
        catDelBtn.setOnClickListener(view -> {
            AlertDialog.Builder confirmDel = new AlertDialog.Builder(MainActivity.this, R.style.ConfirmDelDialog);
            confirmDel.setTitle("Delete entry");
            confirmDel.setMessage("Are you sure you want to delete? Relevant expenses will be moved to Others.");
            confirmDel.setPositiveButton("Delete", (dialogInterface, i) -> {
                db.deleteCategory(cat, true);
                updateCategoryData();
                updateHomeData();
                dialog.dismiss();
            });
            confirmDel.setNeutralButton(android.R.string.no, (dialog1, which) -> dialog1.cancel());
            confirmDel.show();
        });
        catSaveBtn.setOnClickListener(view -> {
            if (!catName.getText().toString().isEmpty()) {
                int id = cat.getId();
                String name = catName.getText().toString();
                int icon_id = (Integer) catIcon.getTag();
                int color_id = (Integer) catBanner.getTag();
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
        catIcon.setForeground(icon); // set icon
        catIcon.setForegroundTintList(ColorStateList.valueOf(Color.parseColor("#" + colorHex))); // set icon color
        catBanner.setBackgroundColor(Color.parseColor("#" + colorHex)); // set bg color
        catIcon.setTag(MainActivity.getIconIdFromName(this, iconName));
        catBanner.setTag(MainActivity.getColorIdFromName(this, colorName));
    }
    public void setEditAccOptions(String iconName, String colorName) {
        setEditSectionOptions(iconName, colorName);
        catIcon.setBackground(getIconFromId(this, R.drawable.shape_rounded_square));
        catIcon.setImageResource(R.drawable.selector_rounded_square);
    }
    public void setEditCatOptions(String iconName, String colorName) {
        setEditSectionOptions(iconName, colorName);
        catIcon.setBackground(getIconFromId(this, R.drawable.shape_circle));
        catIcon.setImageResource(R.drawable.selector_circle);
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
        return ResourcesCompat.getDrawable(context.getResources(), id, null);
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
    public static float convertDpToPx(Context context, float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                context.getResources().getDisplayMetrics());
    }

}