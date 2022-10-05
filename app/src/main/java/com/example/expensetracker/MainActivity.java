package com.example.expensetracker;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.expensetracker.ChartsPage.ChartsChildFragment;
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
import com.example.expensetracker.RecyclerViewAdapters.ViewPagerAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements NavigationBarView.OnItemSelectedListener {

    public static String TAG = "MainActivity";

    // Main components
    public DatabaseHelper db = new DatabaseHelper(this);
    public static Locale locale = Locale.getDefault();
    private HashMap<Integer, String> iconMap = new HashMap<>();
    private HashMap<Integer, String> colorMap = new HashMap<>();

    // Side menu
    private TextView sideMenuValueCurr, sideMenuValueFirst;
    private DrawerLayout navDrawer;

    // Dialog components
    private AlertDialog.Builder dialogBuilder;
    private EditText expAmt, expDesc, sectionName;
    private TextView expAccName, expCatName, expDate, sectionType, expCurr, sectionCurr;
    private ImageButton expAccIcon, expCatIcon, sectionIcon;
    private LinearLayout expAccBox, expCatBox, expDelBtn, expDateBtn, expSaveBtn, sectionBanner, sectionCurrRow, sectionDelBtn, sectionSaveBtn;

    // Fragments
    private BottomNavigationView bottomNavView;
    private ViewPager2 viewPager;
    private ViewPagerAdapter adapter;
    private boolean updateFragments = false;

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
        if (!getSharedPreferences(Constants.SETTINGS, Context.MODE_PRIVATE).contains(getString(R.string.key_default_currency)))
            initialiseDefaultSettings();

        // Initialize the bottom navigation view
        bottomNavView = findViewById(R.id.bottom_nav_view);
        viewPager = findViewById(R.id.viewPager);
        bottomNavView.setOnItemSelectedListener(this);
        getTabs();

        // Initialise menu
        navDrawer = findViewById(R.id.drawer_layout);
        LinearLayout sideMenuItemCurr = findViewById(R.id.sideMenuItemCurrency);
        LinearLayout sideMenuItemFirst = findViewById(R.id.sideMenuItemFirst);
        LinearLayout sideMenuItemImport = findViewById(R.id.sideMenuItemImport);
        LinearLayout sideMenuItemExport = findViewById(R.id.sideMenuItemExport);
        LinearLayout sideMenuItemXrate = findViewById(R.id.sideMenuItemXrate);
        sideMenuValueCurr = findViewById(R.id.sideMenuValueCurrency);
        sideMenuValueFirst = findViewById(R.id.sideMenuValueFirst);
        sideMenuValueCurr.setText(getDefaultCurrency());
        sideMenuValueFirst.setText((getDefaultFirstDayOfWeek() == Calendar.SUNDAY) ? "Sunday" : "Monday");
        sideMenuItemCurr.setOnClickListener(view2 -> {
            CurrencyAdapter adapter = new CurrencyAdapter(this, db.getAllCurrencies(), getDefaultCurrency());
            final View view1 = getLayoutInflater().inflate(R.layout.dialog_recyclerview, null);
            RecyclerView currencyList = view1.findViewById(R.id.recyclerView);
            currencyList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
            currencyList.setAdapter(adapter);

            dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle(getString(R.string.default_curr_title))
                    .setView(view1)
                    .setPositiveButton(android.R.string.yes, (dialogInterface, i) -> {
                        SharedPreferences pref = getSharedPreferences(Constants.SETTINGS, Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = pref.edit();
                        editor.putString(getString(R.string.key_default_currency), adapter.getSelected());
                        editor.apply();
                        sideMenuValueCurr.setText(getDefaultCurrency());
                        if (getCurrentFragment() instanceof HomeFragment) {
                            ((HomeFragment) getCurrentFragment()).setSummaryCurr(new Currency(this).getSymbol());
                            updateSummaryData(Constants.HOME);
                        }
                        setUpdateFragments(true);
                    })
                    .setNeutralButton(android.R.string.no, (dialogInterface, i) -> {})
                    .show();
        });
        sideMenuItemFirst.setOnClickListener(view -> {
            // Calendar.SUNDAY is 1, Calendar.MONDAY is 2
            int[] selectedPos = { getDefaultFirstDayOfWeek() };
            dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle(getString(R.string.default_fdow_title))
                    .setSingleChoiceItems(new String[]{ "Sunday", "Monday" },
                            selectedPos[0] - 1,
                            (dialogInterface, i) -> {
                                selectedPos[0] = i + 1;
                                dialogInterface.dismiss();
                            })
                    .setOnDismissListener(dialogInterface -> {
                        SharedPreferences pref = getSharedPreferences(Constants.SETTINGS, Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = pref.edit();
                        editor.putInt(getString(R.string.key_default_firstDayOfWeek), selectedPos[0]);
                        editor.apply();
                        sideMenuValueFirst.setText((getDefaultFirstDayOfWeek() == Calendar.SUNDAY) ? "Sunday" : "Monday");
                        if (!(getCurrentFragment() instanceof HomeFragment))
                            return;
                        ((HomeFragment) getCurrentFragment()).updateDate();
                        updateHomeData();
                    })
                    .show();
        });
        sideMenuItemImport.setOnClickListener(view -> {
            if (permissionsGranted()) showFileChooser();
            else requestPermissions();
        });
        sideMenuItemExport.setOnClickListener(view -> {
            if (!permissionsGranted()) {
                Toast.makeText(this, "Permissions not granted", Toast.LENGTH_SHORT).show();
                requestPermissions();
                return;
            }
            dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle("Export database?")
                    .setMessage("Database will be exported to Downloads folder.")
                    .setPositiveButton(android.R.string.yes, (dialogInterface1, i1) -> db.exportDatabase())
                    .setNegativeButton(android.R.string.no, (dialogInterface1, i1) -> dialogInterface1.dismiss())
                    .show();
        });
        sideMenuItemXrate.setOnClickListener(view2 -> {
            final View view1 = getLayoutInflater().inflate(R.layout.dialog_xrate, null);
            Spinner spinner = (Spinner) view1.findViewById(R.id.spinnerCurrencies);
            EditText editText = (EditText) view1.findViewById(R.id.xrate);
            ArrayList<String> spinnerArray = new ArrayList<>();
            for (Currency c : db.getAllCurrencies())
                if (!c.getName().equals("USD")) spinnerArray.add(c.getName());
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                    this, android.R.layout.simple_spinner_item, spinnerArray);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
            spinner.setSelection(Constants.currencies.indexOf(db.getCurrency(getDefaultCurrency())));
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    Float xrate = db.getCurrency(spinner.getSelectedItem().toString()).getRate();
                    editText.setText(String.format(MainActivity.locale, "%.3f", xrate));
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });

            dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle(getString(R.string.set_xrate_title))
                    .setView(view1)
                    .setPositiveButton(android.R.string.yes, (dialogInterface, i) -> {
                        Currency currency = db.getCurrency(spinner.getSelectedItem().toString());
                        try {
                            currency.setRate(Float.parseFloat(editText.getText().toString()));
                            db.updateCurrency(currency);
                            Toast.makeText(this, "Conversion rate for " + currency.getName() + " updated.", Toast.LENGTH_SHORT).show();
                            updateSummaryData(Constants.HOME);
                            setUpdateFragments(true);
                        } catch (Exception e) {
                            Log.e(TAG, e.toString());
                            Toast.makeText(this, "Something went wrong. Conversion rate for " + currency.getName() + " not updated.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNeutralButton(android.R.string.no, (dialogInterface, i) -> {})
                    .show();
        });
    }

    /**
     * Fragments
     */
    public void getTabs() {
        adapter = new ViewPagerAdapter(getSupportFragmentManager(), getLifecycle());
        adapter.addFragment(new HomeFragment());
        adapter.addFragment(new ChartsFragment());
        adapter.addFragment(new ManageFragment());
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(3);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                switch (position) {
                    case Constants.HOME:
                        bottomNavView.getMenu().findItem(R.id.navigation_home).setChecked(true);
                        setMenuEnabled(true);
                        break;
                    case Constants.CHARTS:
                        bottomNavView.getMenu().findItem(R.id.navigation_charts).setChecked(true);
                        setMenuEnabled(false);
                        break;
                    case Constants.MANAGE:
                        bottomNavView.getMenu().findItem(R.id.navigation_manage).setChecked(true);
                        setMenuEnabled(false);
                        break;
                }
                if (updateFragments) {
                    if (position == Constants.HOME) updateHomeData();
                    ((ChartsFragment) adapter.createFragment(Constants.CHARTS)).updateData();
                    ((ManageFragment) adapter.createFragment(Constants.MANAGE)).updateData();
                    setUpdateFragments(false);
                }
            }
        });
    }
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.navigation_home)
            viewPager.setCurrentItem(Constants.HOME, false);
        else if (id == R.id.navigation_charts)
            viewPager.setCurrentItem(Constants.CHARTS, false);
        else if (id == R.id.navigation_manage)
            viewPager.setCurrentItem(Constants.MANAGE, false);
        else
            return false;
        return true;
    }
    public Fragment getCurrentFragment() {
        return adapter.createFragment(viewPager.getCurrentItem());
    }
    public Fragment getFragment(int page) {
        return adapter.createFragment(page);
    }
    public void setUpdateFragments(boolean enable) {
        updateFragments = enable;
    }

    /**
     * Initialisation
     */
    public void initialiseDefaultAccs() {
        for (int i = 0;i < Constants.defaultAccNames.length;i++) {
            Account acc = new Account(this, Constants.defaultAccNames[i], Constants.defaultAccIcons[i], Constants.defaultAccColors[i], i);
            db.createAccount(acc, false);
        }
    }
    public void initialiseDefaultCats() {
        for (int i = 0;i < Constants.defaultCatNames.length;i++) {
            Category cat = new Category(this, Constants.defaultCatNames[i], Constants.defaultCatIcons[i], Constants.defaultCatColors[i], i);
            db.createCategory(cat, false);
        }
    }
    public void initialiseCurrencies() {
        for (Currency c : Constants.currencies) db.createCurrency(c);
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
    public void initialiseDefaultSettings() {
        SharedPreferences pref = getSharedPreferences(Constants.SETTINGS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(getString(R.string.key_default_currency), getString(R.string.default_currency));
        editor.putInt(getString(R.string.key_default_firstDayOfWeek), Calendar.SUNDAY);
        editor.apply();
    }

    /**
     * Dialog templates
     */
    public AlertDialog expenseDialog() {
        // dialog
        final View expView = getLayoutInflater().inflate(R.layout.dialog_expense, null);
        dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setView(expView)
                .setOnDismissListener(dialogInterface -> hideKeyboard(expAmt));
        AlertDialog expDialog = dialogBuilder.create();
        expDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // set transparent dialog bg
        expDialog.show();
        expDialog.getWindow().setGravity(Gravity.BOTTOM);
        expDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        // get components by id
        expAmt = expView.findViewById(R.id.newExpAmt);
        expAmt.setFilters(new InputFilter[] { new MoneyValueFilter() });
        expAmt.requestFocus(); // focus on amt and open keyboard
        showKeyboard(expAmt);
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
        @SuppressLint("InflateParams") final View view = getLayoutInflater().inflate(R.layout.dialog_expense_opt_section, null);
        AlertDialog dialog = expenseSectionDialog(adapter, view).create();
        adapter.setDialog(dialog);

        // set values
        TextView title = view.findViewById(R.id.expOptSectionTitle);
        title.setText(R.string.CAT);
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
            expCatBox.setBackgroundColor(Color.parseColor("#" + selectedCat.getColorHex())); // set bg color
        });

        dialog.show();
    }
    public void expenseAccDialog(AccountAdapter adapter, Expense exp) {
        @SuppressLint("InflateParams") final View view = getLayoutInflater().inflate(R.layout.dialog_expense_opt_section, null);
        AlertDialog dialog = expenseSectionDialog(adapter, view).create();
        adapter.setDialog(dialog);

        // set values
        TextView title = view.findViewById(R.id.expOptSectionTitle);
        title.setText(R.string.ACC);

        dialog.setOnCancelListener(dialog1 -> {
            Account selectedAcc = adapter.getSelected();
            expAccName.setText(selectedAcc.getName()); // set name
            expAccIcon.setForeground(selectedAcc.getIcon()); // set icon
            expAccIcon.setForegroundTintList(getColorStateListFromName(MainActivity.this, selectedAcc.getColorName())); // set icon color
            expAccBox.setBackgroundColor(Color.parseColor("#" + selectedAcc.getColorHex())); // set bg color
            expCurr.setText(selectedAcc.getCurrencySymbol());
        });
        dialog.show();

        if (!adapter.getSelectedPos().isEmpty())
            return;
        if (exp.getId() == -1) {
            adapter.setSelected(0);
        } else {
            Account acc = exp.getAccount();
            adapter.setSelected(adapter.getList().indexOf(acc));
        }
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
     * Update functions
     */
    public void updateHomeData() {
        ArrayList<Expense> expenses = getExpenseList();
        updateExpenseData(expenses);
        updateSummaryData(expenses, Constants.HOME);
    }
    public void updateAllExpenseData() {
        HomeFragment homeFrag = (HomeFragment) adapter.createFragment(Constants.HOME);
        ChartsFragment chartsFrag = (ChartsFragment) adapter.createFragment(Constants.CHARTS);
        for (int i = 0;i < 2;i++) {
            ArrayList<Expense> expenses = new ArrayList<>();
            if (i == Constants.HOME) {
                Calendar from = homeFrag.getDateRange()[0];
                Calendar to = homeFrag.getDateRange()[1];
                if (homeFrag.getSelDateState() == DateGridAdapter.ALL)
                    expenses =  db.getAllExpenses();
                else
                    expenses = db.getExpensesByDateRange(from, to);
                ArrayList<Expense> expensesByFilter = db.getExpensesByFilters(homeFrag.getSelAccFilters(), homeFrag.getSelCatFilters());
                expenses.retainAll(expensesByFilter);
            } else if (i == Constants.CHARTS) {
                Calendar from = chartsFrag.getDateRange()[0];
                Calendar to = chartsFrag.getDateRange()[1];
                if (chartsFrag.getSelDateState() == DateGridAdapter.ALL)
                    expenses =  db.getAllExpenses();
                else
                    expenses = db.getExpensesByDateRange(from, to);
            }
            expenses = insertExpDateHeaders(sortExpenses(expenses, Constants.DESCENDING));
            ExpenseAdapter expAdapter = new ExpenseAdapter(this, expenses);
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
            linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
            ((HomeFragment) adapter.createFragment(Constants.HOME)).setExpenseData(linearLayoutManager, expAdapter);
        }
    }
    public void updateExpenseData(ArrayList<Expense> expenses) {
        expenses = insertExpDateHeaders(sortExpenses(expenses, Constants.DESCENDING));
        ExpenseAdapter expAdapter = new ExpenseAdapter(this, expenses);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        if (getCurrentFragment() instanceof HomeFragment)
            ((HomeFragment) getCurrentFragment()).setExpenseData(linearLayoutManager, expAdapter);
    }
    public void updateSummaryData(ArrayList<Expense> expenses, int page) {
        Calendar from, to;
        Fragment fragment;
        int state;
        fragment = adapter.createFragment(page);
        if (page == Constants.HOME) {
            from = ((HomeFragment) fragment).getDateRange()[0];
            to = ((HomeFragment) fragment).getDateRange()[1];
            state = ((HomeFragment) fragment).getSelDateState();
        } else if (page == Constants.CHARTS) {
            from = ((ChartsFragment) fragment).getDateRange()[0];
            to = ((ChartsFragment) fragment).getDateRange()[1];
            state = ((ChartsFragment) fragment).getSelDateState();
        } else
            return;

        String summaryDateText;
        float totalAmt = 0;
        if (state == DateGridAdapter.ALL) {
            summaryDateText = "All time";
            if (((HomeFragment) fragment).hasNoFilters())
                totalAmt = db.getConvertedTotalAmt();
            else
                for (Expense exp : expenses)
                    totalAmt += convertAmt(exp);
        } else {
            Calendar cal = getCalendarCopy(to, DateGridAdapter.FROM);
            String dtf;
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
            for (Expense exp : expenses)
                totalAmt += convertAmt(exp);
        }
        if (page == Constants.HOME)
            ((HomeFragment) adapter.createFragment(Constants.HOME)).setSummaryData(summaryDateText.toUpperCase(), totalAmt);
        else if (page == Constants.CHARTS)
            ((ChartsFragment) adapter.createFragment(Constants.CHARTS)).setSummaryData(summaryDateText.toUpperCase(), totalAmt, true);
//        long toc = System.currentTimeMillis();
//        Log.e(TAG,"setSummaryData="+(toc-tic1));
    }
    public void updateSummaryData(int page) {
        updateSummaryData(getExpenseList(), page);
    }
    public Pair<String,Float> getSummaryData(ArrayList<Expense> expenses, int fragType) {
        if (fragType != Constants.CHARTS)
            return null;
        ChartsFragment fragment = (ChartsFragment) adapter.createFragment(Constants.CHARTS);
        Calendar from = fragment.getDateRange()[0];
        Calendar to = fragment.getDateRange()[1];
        int state = fragment.getSelDateState();
        String summaryDateText;
        float totalAmt = 0;
        if (state == DateGridAdapter.ALL) {
            summaryDateText = "All time";
            totalAmt = db.getConvertedTotalAmt();
        } else {
            Calendar cal = getCalendarCopy(to, DateGridAdapter.FROM);
            String dtf;
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
        return new Pair<>(summaryDateText.toUpperCase(), totalAmt);
    }
    @SuppressWarnings("unchecked")
    public void updateAccountData() {
        AccountAdapter adapter = getAccountData(Constants.MANAGE);
        adapter.addNewAcc();
        Fragment childFragment = null;
        if (getCurrentFragment() instanceof ManageFragment)
            childFragment = getCurrentFragment().getChildFragmentManager().getFragments().get(0);
        if (childFragment instanceof ManageChildFragment)
            ((ManageChildFragment<AccountAdapter>) childFragment).setAdapter(adapter, ItemTouchHelper.UP | ItemTouchHelper.DOWN);
    }
    @SuppressWarnings("unchecked")
    public void updateCategoryData() {
        CategoryAdapter adapter = getCategoryData(Constants.MANAGE);
        adapter.addNewCat();
        Fragment childFragment = null;
        if (getCurrentFragment() instanceof ManageFragment)
            childFragment = getCurrentFragment().getChildFragmentManager().getFragments().get(1);
        if (childFragment instanceof ManageChildFragment)
            ((ManageChildFragment<CategoryAdapter>) childFragment).setAdapter(adapter, ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
    }
    public void updateAccFilters(ArrayList<Account> filters) {
        if (getCurrentFragment() instanceof HomeFragment) {
            HomeFragment fragment = (HomeFragment) getCurrentFragment();
            fragment.setSelAccFilters(filters);
            fragment.applyFilters();
            fragment.updateClearFiltersItem();
            return;
        }
        if (getCurrentFragment() instanceof ChartsFragment) {
            ChartsChildFragment fragment = (ChartsChildFragment) getCurrentFragment().getChildFragmentManager().getFragments().get(1);
            fragment.setAccFilters(filters);
            fragment.applyFilters(true);
        }
    }
    public void updateCatFilters(ArrayList<Category> filters) {
        if (getCurrentFragment() instanceof HomeFragment) {
            HomeFragment fragment = (HomeFragment) getCurrentFragment();
            fragment.setSelCatFilters(filters);
            fragment.applyFilters();
            fragment.updateClearFiltersItem();
            return;
        }
        if (getCurrentFragment() instanceof ChartsFragment) {
            ChartsChildFragment fragment = (ChartsChildFragment) getCurrentFragment().getChildFragmentManager().getFragments().get(1);
            fragment.setCatFilters(filters);
            fragment.applyFilters(true);
        }
    }

    /**
     * Getters
     */
    // Expenses
    public ArrayList<Expense> getExpenseList() {
        ArrayList<Expense> expensesByDate = new ArrayList<>();
        if (getCurrentFragment() instanceof HomeFragment) {
            HomeFragment fragment = (HomeFragment) getCurrentFragment();
            Calendar from = fragment.getDateRange()[0];
            Calendar to = fragment.getDateRange()[1];
            if (fragment.getSelDateState() == DateGridAdapter.ALL) {
                expensesByDate =  db.getAllExpenses();
            } else {
                expensesByDate = db.getExpensesByDateRange(from, to);
            }
            ArrayList<Expense> expensesByFilter = db.getExpensesByFilters(fragment.getSelAccFilters(), fragment.getSelCatFilters());
            expensesByDate.retainAll(expensesByFilter);
        } else if (getCurrentFragment() instanceof ChartsFragment) {
            ChartsFragment fragment = (ChartsFragment) getCurrentFragment();
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
            expenses.sort(Comparator.comparing(Expense::getDatetime));
        else
            expenses.sort((e1, e2) -> e2.getDatetime().compareTo(e1.getDatetime()));
        return expenses;
    }

    // Sections
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
    public CategoryAdapter getCategoryData(int mode) {
        return new CategoryAdapter(this, sortSections(db.getAllCategories()), mode);
    }
    public AccountAdapter getAccountData(int mode) {
        return new AccountAdapter(this, sortSections(db.getAllAccounts()), mode);
    }


    /**
     * Add/edit functions
     */
    public void addExpense() {
        AlertDialog expDialog = expenseDialog();
        expDelBtn.setVisibility(LinearLayout.INVISIBLE);
        Calendar cal = Calendar.getInstance(locale);
        expDate.setText(getString(R.string.full_date,"Today",getDatetimeStr(cal,"dd MMMM yyyy")).toUpperCase());
        Account acc = db.getAccount(getDefaultAccName());
        Category cat = db.getCategory(getDefaultCatName());
        expAccName.setText(acc.getName()); // set name
        expCatName.setText(cat.getName());
        expAccIcon.setForeground(acc.getIcon()); // set icon
        expCatIcon.setForeground(cat.getIcon());
        expAccIcon.setForegroundTintList(getColorStateListFromHex(acc.getColorHex())); // set icon color
        expCatIcon.setForegroundTintList(getColorStateListFromHex(cat.getColorHex()));
        expAccBox.setBackgroundColor(acc.getColor()); // set bg color
        expCatBox.setBackgroundColor(cat.getColor());
        expCurr.setText(acc.getCurrencySymbol());

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
            datePicker.setFirstDayOfWeek(getDefaultFirstDayOfWeek());
            changeDate.setView(datePicker)
                    .setPositiveButton(android.R.string.yes, (dialogInterface, i) -> {
                cal.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth());
                expDate.setText(getString(R.string.full_date,getRelativePrefix(cal),getDatetimeStr(cal,"dd MMM yyyy")).toUpperCase());
            })
                    .setNeutralButton(android.R.string.no, (dialog, which) -> dialog.cancel())
                    .show();
        });
        expSaveBtn.setOnClickListener(v -> {
            if (!expAmt.getText().toString().isEmpty()) {
                float amt = Float.parseFloat(expAmt.getText().toString());
                String desc = expDesc.getText().toString();
                Account acc1 = db.getAccount(expAccName.getText().toString());
                Category cat1 = db.getCategory(expCatName.getText().toString());
                String datetime = getDatetimeStr(cal, "");
                Expense expense = new Expense(amt, desc, acc1, cat1, datetime);
                db.createExpense(expense);
                updateHomeData(); // update summary & expense list
                setUpdateFragments(true);
            } else {
                Toast.makeText(MainActivity.this, "Amount cannot be 0. No expense created", Toast.LENGTH_SHORT).show();
            }
            hideKeyboard(expAmt);
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
        expAccIcon.setForegroundTintList(getColorStateListFromHex(acc.getColorHex())); // set icon color
        expCatIcon.setForegroundTintList(getColorStateListFromHex(cat.getColorHex()));
        expAccBox.setBackgroundColor(acc.getColor()); // set bg color
        expCatBox.setBackgroundColor(cat.getColor());
        expCurr.setText(acc.getCurrencySymbol());

        Calendar today = Calendar.getInstance(locale);
        expDate.setText(getString(R.string.full_date,getRelativePrefix(exp.getDatetime()),exp.getDatetimeStr("dd MMMM yyyy")).toUpperCase());

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
                setUpdateFragments(true);
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
                setUpdateFragments(true);
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
            datePicker.setFirstDayOfWeek(getDefaultFirstDayOfWeek());
            datePicker.updateDate(expDatetime.get(Calendar.YEAR), expDatetime.get(Calendar.MONTH), expDatetime.get(Calendar.DAY_OF_MONTH));
            changeDate.setView(datePicker)
                    .setPositiveButton(android.R.string.yes, (dialogInterface, i) -> {
                expDatetime.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth());
                exp.setDatetime(expDatetime);
                String datePrefix1 = "";
                if (exp.getDatetime().get(Calendar.YEAR) == today.get(Calendar.YEAR)) {
                    int dateDiff = exp.getDatetime().get(Calendar.DAY_OF_YEAR) - today.get(Calendar.DAY_OF_YEAR);
                    datePrefix1 = (dateDiff == 0) ? "TODAY" : ((dateDiff == -1) ? "YESTERDAY" : "");
                }
                expDate.setText(getString(R.string.full_date,datePrefix1,exp.getDatetimeStr("dd MMMM yyyy").toUpperCase()));
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
        sectionType.setText(getString(R.string.new_acc_title));
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
                Account account = new Account(MainActivity.this, name, iconMap.get(icon_id), colorMap.get(color_id), getNewPosAcc(), currency);
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
        sectionType.setText(getString(R.string.Acc));
        sectionCurr.setText(acc.getCurrencyName());
        setEditAccOptions(acc.getIconName(), acc.getColorName());
        if(acc.getName().equals(getDefaultAccName()))
            sectionDelBtn.setVisibility(View.GONE);

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
            int numExpenses = db.getNumExpensesByAccount(acc);
            confirmDel.setTitle("Delete account")
                    .setMessage("Are you sure you want to delete?" + ((numExpenses == 0) ? "" :
                            " " + numExpenses + " expense(s) will be moved to " + getDefaultAccName() + "."))
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
                int pos = acc.getPosition();
                Currency currency = getCurrencyFromName(sectionCurr.getText().toString());
                db.updateAccount(new Account(MainActivity.this, id, name, iconMap.get(icon_id), colorMap.get(color_id), pos, currency));
                updateAccountData();
                updateHomeData();
                setUpdateFragments(true);
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
        sectionType.setText(getString(R.string.new_cat_title));
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
                Category cat = new Category(MainActivity.this, name, iconMap.get(icon_id), colorMap.get(color_id), getNewPosCat());
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
        sectionType.setText(R.string.Cat);
        setEditCatOptions(cat.getIconName(), cat.getColorName());
        if (cat.getName().equals(getImmutableCat())) sectionDelBtn.setVisibility(View.GONE);

        // actions
        sectionIcon.setOnClickListener(view -> {
            int iconId = (Integer) sectionIcon.getTag();
            int colorId = (Integer) sectionBanner.getTag();
            SectionOptDialogFragment sectionOptDialog = new SectionOptDialogFragment(Constants.SECTION_CATEGORY, iconMap.get(iconId), colorMap.get(colorId));
            sectionOptDialog.show(getSupportFragmentManager(), "sectionOptDialog");
        });
        sectionDelBtn.setOnClickListener(view -> {
            AlertDialog.Builder confirmDel = new AlertDialog.Builder(MainActivity.this, R.style.ConfirmDelDialog);
            int numExpenses = db.getNumExpensesByCategory(cat);
            confirmDel.setTitle("Delete category")
                    .setMessage("Are you sure you want to delete?" + ((numExpenses == 0) ? "" :
                            " " + numExpenses + " expense(s) will be moved to " + getImmutableCat() + "."))
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
                int pos = cat.getPosition();
                db.updateCategory(new Category(MainActivity.this, id, name, iconMap.get(icon_id), colorMap.get(color_id), pos));
                updateCategoryData();
                updateHomeData();
                setUpdateFragments(true);
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    /**
     * Helper functions
     */
    public float convertAmt(Expense exp) {
        Currency defaultCurr = db.getCurrency(getDefaultCurrency());
        Currency thisCurr = db.getCurrency(exp.getAccount().getCurrencyName());
        if (thisCurr.equals(defaultCurr))
            return exp.getAmount();
        else
            return exp.getAmount() * thisCurr.getRate() / defaultCurr.getRate();
    }
    public float convertAmt(Account acc) {
        Currency defaultCurr = db.getCurrency(getDefaultCurrency());
        Currency thisCurr = db.getCurrency(acc.getCurrencyName());
        if (thisCurr.equals(defaultCurr))
            return db.getTotalAmtByAccount(acc);
        else
            return db.getTotalAmtByAccount(acc) * thisCurr.getRate() / defaultCurr.getRate();
    }
    public static float convertDpToPx(Context context, float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                context.getResources().getDisplayMetrics());
    }
    public static Currency getCurrencyFromName(String name) {
        return Constants.currency_map.get(name);
    }
    public int getNewPosAcc() { return db.getNewPosAccount(); }
    public int getNewPosCat() {
        return db.getNewPosCategory();
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
     * Side menu helper functions
     */
    public void setupMenuBtn(ImageButton menuBtn) {
        menuBtn.setOnClickListener(view1 -> navDrawer.openDrawer(GravityCompat.START));
    }
    public void setMenuEnabled(boolean enable) {
        navDrawer.setDrawerLockMode(enable ? DrawerLayout.LOCK_MODE_UNLOCKED : DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
    }

    /**
     * Icon/color helper functions
     */
    public static Bitmap drawableToBitmap (Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if(bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }
        Bitmap bitmap;
        if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
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
    public static int getColorFromHex(String hex) {
        return Color.parseColor("#" + hex);
    }

    /**
     * User defaults helper functions
     */
    public String getDefaultAccName() {
        return db.getDefaultAccName();
    }
    public String getDefaultCatName() {
        return db.getDefaultCatName();
    }
    public String getDefaultCurrency() {
        return getDefaultCurrency(this);
    }
    public String getDefaultCurrencySymbol() {
        return db.getCurrency(getDefaultCurrency()).getSymbol();
    }
    public static String getDefaultCurrency(Context context) {
        SharedPreferences pref = context.getSharedPreferences(Constants.SETTINGS, Context.MODE_PRIVATE);
        return pref.getString(context.getString(R.string.key_default_currency), context.getString(R.string.default_currency));
    }
    public int getDefaultFirstDayOfWeek() {
        return getDefaultFirstDayOfWeek(this);
    }
    public static int getDefaultFirstDayOfWeek(Context context) {
        SharedPreferences pref = context.getSharedPreferences(Constants.SETTINGS, Context.MODE_PRIVATE);
        return pref.getInt(context.getString(R.string.key_default_firstDayOfWeek), Calendar.SUNDAY);
    }
    public String getImmutableCat() {
        return db.getCategory(1).getName();
    }
    public void resetDefaultAccs() {
        ArrayList<Account> accounts = db.getAllAccounts();
        HashMap<String,Integer> defaultAccNames = new HashMap<>();
        for (int i = 0;i < Constants.defaultAccNames.length;i++)
            defaultAccNames.put(Constants.defaultAccNames[i], i);
        // delete non-default accounts
        for (Account acc : accounts) {
            if (defaultAccNames.containsKey(acc.getName())) {
                defaultAccNames.remove(acc.getName());
                continue;
            }
            db.deleteAccount(acc,false);
        }
        // add back deleted accounts
        for (String name : defaultAccNames.keySet()) {
            int idx = defaultAccNames.get(name);
            String icon = Constants.defaultAccIcons[idx];
            String color = Constants.defaultAccColors[idx];
            db.createAccount(new Account(this,idx+1,name,icon,color,getNewPosAcc()),true);
        }
    }
    public void resetDefaultCats() {
        ArrayList<Category> categories = db.getAllCategories();
        HashMap<String,Integer> defaultCatNames = new HashMap<>();
        for (int i = 0;i < Constants.defaultCatNames.length;i++)
            defaultCatNames.put(Constants.defaultCatNames[i], i);
        // delete non-default categories
        for (Category cat : categories) {
            if (defaultCatNames.containsKey(cat.getName())) {
                defaultCatNames.remove(cat.getName());
                continue;
            }
            db.deleteCategory(cat,false);
        }
        // add back deleted categories
        for (String name : defaultCatNames.keySet()) {
            int idx = defaultCatNames.get(name);
            String icon = Constants.defaultCatIcons[idx];
            String color = Constants.defaultCatColors[idx];
            db.createCategory(new Category(this,idx+1,name,icon,color,getNewPosCat()),true);
        }
    }

    /**
     * Calendar helper functions
     */
    public static Calendar getCalendarCopy(Calendar cal, int range) {
        Calendar copy = Calendar.getInstance();
        if (range == DateGridAdapter.FROM)
            copy.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 0,0,0);
        else
            copy.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 23,59,59);
        return copy;
    }
    public static String getDatetimeStr(Calendar cal, String format) {
        format = (format.isEmpty()) ? Expense.DATETIME_FORMAT : format;
        return new SimpleDateFormat(format, locale).format(cal.getTime());
    }
    public Calendar getInitSelectedDates(int range, int state) {
        return DateGridAdapter.getInitSelectedDates(range, state, getDefaultFirstDayOfWeek());
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
    public static Calendar getCalFromString(String dtf, String date) {
        Calendar cal = Calendar.getInstance(locale);

        try { cal.setTime(Objects.requireNonNull(new SimpleDateFormat(dtf, locale).parse(date)));
        } catch (ParseException e) { e.printStackTrace();}
        return cal;
    }

    /**
     * Keyboard helper functions
     */
    public static void showKeyboard(Context context, EditText view) {
        view.postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager) context.getSystemService(INPUT_METHOD_SERVICE);
            imm.showSoftInput(view, 0);
        }, 270);
    }
    public static void hideKeyboard(Context context, EditText view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
    public void showKeyboard(EditText view) {
        showKeyboard(this, view);
    }
    public void hideKeyboard(EditText view) {
        hideKeyboard(this, view);
    }

    /**
     * Import/export helper functions
     */
    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        launcher.launch(intent);
    }
    ActivityResultLauncher<Intent> launcher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Intent data = result.getData();
                    if (data == null)
                        return;
                    Uri uri = data.getData();
                    dialogBuilder = new AlertDialog.Builder(this, R.style.ConfirmDelDialog);
                    dialogBuilder.setTitle("Import database")
                            .setMessage("Are you sure you want to import? This will overwrite all current data.")
                            .setPositiveButton("Overwrite", (dialogInterface, i) -> {
                                try {
                                    InputStream input = getContentResolver().openInputStream(uri);
                                    db.importDatabase(input);
                                    Toast.makeText(this, "Import successful", Toast.LENGTH_SHORT).show();
                                    updateHomeData();
                                    setUpdateFragments(true);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    Toast.makeText(this, "Import failed", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNeutralButton(android.R.string.no, (dialog, which) -> {
                                dialog.cancel(); // close dialog
                            })
                            .show();
                }
            });
    public boolean permissionsGranted() {
        return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PERMISSION_GRANTED;
    }
    public void requestPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[] { Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE },
                0);
    }
}