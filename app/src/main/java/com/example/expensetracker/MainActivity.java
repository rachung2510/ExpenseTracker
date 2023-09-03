package com.example.expensetracker;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
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
import android.provider.MediaStore;
import android.text.InputFilter;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
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
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.core.view.WindowCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.expensetracker.ChartsPage.ChartsChildFragmentGraph;
import com.example.expensetracker.ChartsPage.ChartsFragment;
import com.example.expensetracker.HelperClasses.FileUtils;
import com.example.expensetracker.HelperClasses.MoneyValueFilter;
import com.example.expensetracker.RecyclerViewAdapters.AccountAdapter;
import com.example.expensetracker.RecyclerViewAdapters.CategoryAdapter;
import com.example.expensetracker.RecyclerViewAdapters.CurrencyAdapter;
import com.example.expensetracker.RecyclerViewAdapters.DateGridAdapter;
import com.example.expensetracker.RecyclerViewAdapters.ReceiptItemAdapter;
import com.example.expensetracker.RecyclerViewAdapters.SectionAdapter;
import com.example.expensetracker.HomePage.HomeFragment;
import com.example.expensetracker.ManagePage.ManageChildFragment;
import com.example.expensetracker.ManagePage.ManageFragment;
import com.example.expensetracker.ManagePage.SectionOptDialogFragment;
import com.example.expensetracker.RecyclerViewAdapters.ViewPagerAdapter;
import com.example.expensetracker.Widget.WidgetDialogActivity;
import com.example.expensetracker.Widget.WidgetStaticActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements NavigationBarView.OnItemSelectedListener {

    public static String TAG = "MainActivity";

    // Components
    public DatabaseHelper db = new DatabaseHelper(this);
    public static Locale locale = Locale.getDefault();
    private HashMap<Integer, String> iconMap = new HashMap<>();
    private HashMap<Integer, String> colorMap = new HashMap<>();
    private static Uri imageUri;
    private ReceiptItemAdapter receiptItemAdapter = null;

    // Side menu
    private TextView sideMenuValueCurr, sideMenuValueFirst;
    private DrawerLayout navDrawer;

    // Dialog components
    private AlertDialog.Builder dialogBuilder;
    private AlertDialog progressDialog;
    private EditText expAmt, sectionName;
    private AutoCompleteTextView expDesc;
    private TextView expAccName, expCatName, expDate, sectionType, expCurr, sectionCurr;
    private ImageButton expAccIcon, expCatIcon, scanReceiptBtn, favouritesBtn, sectionIcon, receiptCatIcon;
    private LinearLayout expAccBox, expCatBox, expDelBtn, expDateBtn, expSaveBtn, sectionBanner, sectionCurrRow, sectionDelBtn, sectionSaveBtn;

    // Fragments
    private BottomNavigationView bottomNavView;
    private ViewPager2 viewPager;
    private ViewPagerAdapter viewPagerAdapter;
    private boolean updateFragments = false;

    // Others
    int bottomNavHeight = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        FileUtils.setWindowFlag(this, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, false);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

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
        bottomNavView.post(() -> {
            bottomNavHeight = (int) bottomNavView.getMeasuredHeight();
            ((HomeFragment) getFragment(Constants.HOME)).setFragmentHeight(bottomNavHeight);
            ((ChartsFragment) getFragment(Constants.CHARTS)).setFragmentHeight(bottomNavHeight);
            ((ManageFragment) getFragment(Constants.MANAGE)).setFragmentHeight(bottomNavHeight);
        });
        getTabs(savedInstanceState);

        // Initialise menu
        navDrawer = findViewById(R.id.drawer_layout);
        LinearLayout sideMenuItemCurr = findViewById(R.id.sideMenuItemCurrency);
        LinearLayout sideMenuItemFirst = findViewById(R.id.sideMenuItemFirst);
        LinearLayout sideMenuItemImport = findViewById(R.id.sideMenuItemImport);
        LinearLayout sideMenuItemExport = findViewById(R.id.sideMenuItemExport);
        LinearLayout sideMenuItemExportCsv = findViewById(R.id.sideMenuItemExportCsv);
        LinearLayout sideMenuItemXrate = findViewById(R.id.sideMenuItemXrate);
        sideMenuValueCurr = findViewById(R.id.sideMenuValueCurrency);
        sideMenuValueFirst = findViewById(R.id.sideMenuValueFirst);
        sideMenuValueCurr.setText(getDefaultCurrencyName());
        sideMenuValueFirst.setText((getDefaultFirstDayOfWeek() == Calendar.SUNDAY) ? "Sunday" : "Monday");
        sideMenuItemCurr.setOnClickListener(view2 -> {
            CurrencyAdapter adapter = new CurrencyAdapter(this, db.getAllCurrencies(), getDefaultCurrency());
            final View view1 = getLayoutInflater().inflate(R.layout.dialog_recyclerview, null);
            RecyclerView currencyList = view1.findViewById(R.id.recyclerView);
            currencyList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
            currencyList.setAdapter(adapter);

            dialogBuilder = new AlertDialog.Builder(this, R.style.NormalDialog);
            dialogBuilder.setTitle(getString(R.string.default_curr_title))
                    .setView(view1)
                    .setPositiveButton(android.R.string.yes, (dialogInterface, i) -> {
                        SharedPreferences pref = getSharedPreferences(Constants.SETTINGS, Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = pref.edit();
                        editor.putString(getString(R.string.key_default_currency), adapter.getSelected().getName());
                        editor.apply();
                        sideMenuValueCurr.setText(getDefaultCurrencyName());
                        db.updateAllCurrencyRates(getDefaultCurrency());
                        if (getCurrentFragment() instanceof HomeFragment) {
                            ((HomeFragment) getCurrentFragment()).setSummaryCurr(getDefaultCurrencySymbol());
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
                        ((HomeFragment) getCurrentFragment()).updateDateRangeFromState();
                        updateHomeData();
                    })
                    .show();
        });
        sideMenuItemImport.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            accessPhoneFeatures(intent, importLauncher);
        });
        sideMenuItemExport.setOnClickListener(view -> {
            if (permissionsNotGranted()) {
                Toast.makeText(this, "Permissions not granted", Toast.LENGTH_SHORT).show();
                requestPermissions();
                return;
            }
            dialogBuilder = new AlertDialog.Builder(this, R.style.NormalDialog);
            dialogBuilder.setTitle("Export database?")
                    .setMessage("Database will be exported to Downloads folder.")
                    .setPositiveButton(android.R.string.yes, (dialogInterface1, i1) -> db.exportDatabase())
                    .setNegativeButton(android.R.string.no, (dialogInterface1, i1) -> dialogInterface1.dismiss())
                    .show();
        });
        sideMenuItemExportCsv.setOnClickListener(view -> exportToCsv());
        sideMenuItemXrate.setOnClickListener(view2 -> {
            final View view1 = getLayoutInflater().inflate(R.layout.dialog_xrate, null);
            Spinner spinner = view1.findViewById(R.id.spinnerCurrencies);
            EditText editText = view1.findViewById(R.id.xrate);
            ArrayList<String> spinnerArray = new ArrayList<>();
            for (Currency c : db.getAllCurrencies())
                if (!c.equals(getDefaultCurrency())) spinnerArray.add(c.getName());
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this, android.R.layout.simple_spinner_item, spinnerArray);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    Float xrate = 1 / db.getCurrency(spinner.getSelectedItem().toString()).getRate();
                    editText.setText(String.format(MainActivity.locale, "%.3f", xrate));
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });

            dialogBuilder = new AlertDialog.Builder(this, R.style.NormalDialog);
            dialogBuilder.setTitle(getString(R.string.set_xrate_title, getDefaultCurrencyName()))
                    .setView(view1)
                    .setPositiveButton(android.R.string.yes, (dialogInterface, i) -> {
                        Currency currency = db.getCurrency(spinner.getSelectedItem().toString());
                        try {
                            float newRate = 1 / Float.parseFloat(editText.getText().toString());
                            currency.setRate(newRate);
                            db.updateCurrency(currency);
                            Toast.makeText(this, "Conversion rate for " + currency.getName() + " updated.", Toast.LENGTH_SHORT).show();
                            updateSummaryData(Constants.HOME);
                            setUpdateFragments(true);
                        } catch (Exception e) {
                            Toast.makeText(this, "Something went wrong. Conversion rate for " + currency.getName() + " not updated.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNeutralButton(android.R.string.no, (dialogInterface, i) -> {})
                    .show();
        });
    }
    @Override
    public void onBackPressed() {
        HomeFragment homeFragment = (HomeFragment) getFragment(Constants.HOME);
        if (homeFragment.isSearchOpen()) {
            homeFragment.closeSearch();
        } else {
            super.onBackPressed();
        }
    }
    public void enableBottomNavView(boolean show) {
        bottomNavView.setVisibility((show) ? View.VISIBLE : View.GONE);
        ((HomeFragment) getFragment(Constants.HOME)).expandExpenseListLayout(show);
    }

    /**
     * Fragments
     */
    public void getTabs(Bundle savedInstanceState) {
        viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager(), getLifecycle());

        HomeFragment homeFragment = new HomeFragment();
        ChartsFragment chartsFragment = new ChartsFragment();
        ManageFragment manageFragment = new ManageFragment();
        if (savedInstanceState != null) {
            homeFragment = (HomeFragment) getSupportFragmentManager().getFragments().get(Constants.HOME);
            chartsFragment = (ChartsFragment) getSupportFragmentManager().getFragments().get(Constants.CHARTS);
            manageFragment = (ManageFragment) getSupportFragmentManager().getFragments().get(Constants.MANAGE);
        }
        viewPagerAdapter.addFragment(homeFragment);
        viewPagerAdapter.addFragment(chartsFragment);
        viewPagerAdapter.addFragment(manageFragment);
        viewPager.setAdapter(viewPagerAdapter);
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
                    ((HomeFragment) getFragment(Constants.HOME)).updateData();
                    ((ChartsFragment) getFragment(Constants.CHARTS)).updateData();
                    ((ManageFragment) getFragment(Constants.MANAGE)).updateData();
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
        return viewPagerAdapter.createFragment(viewPager.getCurrentItem());
    }
    public Fragment getFragment(int page) {
        return viewPagerAdapter.createFragment(page);
    }
    public void goToFragment(int page) {
        viewPager.setCurrentItem(page);
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
    public AlertDialog expenseDialog(boolean focusKeyboard) {
        // dialog
        final View expView = getLayoutInflater().inflate(R.layout.dialog_expense, null);
        dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setView(expView)
                .setOnDismissListener(dialogInterface -> {
                    hideKeyboard(expAmt);
                    receiptItemAdapter = null;
                });
        AlertDialog expDialog = dialogBuilder.create();
        expDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // set transparent dialog bg
        expDialog.show();
        expDialog.getWindow().setGravity(Gravity.BOTTOM);
        expDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        // get components by id
        expAmt = expView.findViewById(R.id.newExpAmt);
        expAmt.setFilters(new InputFilter[] { new MoneyValueFilter() });
        expAmt.requestFocus(); // focus on amt
        if (focusKeyboard) showKeyboard(expAmt);
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
            if (b) expDesc.setBackgroundResource(getResourceIdFromAttr(this, android.R.attr.colorBackground));
            else expDesc.setBackgroundResource(getResourceIdFromAttr(this, android.R.attr.selectableItemBackground));
        });
        expCurr = expView.findViewById(R.id.newExpCurrency);
        scanReceiptBtn = expView.findViewById(R.id.scanReceiptBtn);
        favouritesBtn = expView.findViewById(R.id.favouritesBtn);

        return expDialog;
    }
    public <T extends SectionAdapter<? extends Section>> AlertDialog.Builder expenseSectionDialog(T adapter, View expOptSectionView) {
        dialogBuilder = new AlertDialog.Builder(this, R.style.NormalDialog);
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
            if (exp.getId() == -1)
                adapter.setSelected(0);
            else {
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
            expCurr.setText(selectedAcc.getCurrency().getSymbol());
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
    public void receiptCatDialog() {
        @SuppressLint("InflateParams") final View view = getLayoutInflater().inflate(R.layout.dialog_expense_opt_section, null);
        CategoryAdapter catAdapter = getCategoryData();
        AlertDialog dialog = expenseSectionDialog(catAdapter, view).create();
        catAdapter.setDialog(dialog);
        TextView title = view.findViewById(R.id.expOptSectionTitle);
        title.setText(R.string.CAT);
        catAdapter.setSelected(receiptItemAdapter.getReceiptCat().getName());

        dialog.setOnCancelListener(dialog1 -> {
            Category selectedCat = catAdapter.getSelected();
            receiptCatIcon.setForeground(selectedCat.getIcon());
            receiptCatIcon.setBackgroundTintList(getColorStateListFromHex(selectedCat.getColorHex()));
            receiptItemAdapter.setReceiptCat(selectedCat);
        });

        dialog.show();
    }

    /**
     * Update functions
     */
    public void updateHomeData() {
        ((HomeFragment) getFragment(Constants.HOME)).updateData(true);
    }
    public void updateSummaryData(int page) {
        Calendar from, to;
        Fragment fragment;
        ArrayList<Account> accFilters = null;
        ArrayList<Category> catFilters = null;
        int state;
        fragment = getFragment(page);
        if (page == Constants.HOME) {
            from = ((HomeFragment) fragment).getDateRange()[0];
            to = ((HomeFragment) fragment).getDateRange()[1];
            state = ((HomeFragment) fragment).getSelDateState();
            accFilters = ((HomeFragment) fragment).getAccFilters();
            catFilters = ((HomeFragment) fragment).getCatFilters();
        } else if (page == Constants.CHARTS) {
            from = ((ChartsFragment) fragment).getDateRange()[0];
            to = ((ChartsFragment) fragment).getDateRange()[1];
            state = ((ChartsFragment) fragment).getSelectedDateState();
        } else {
            return;
        }

        String summaryDateText;
        float totalAmt;
        if (state == DateGridAdapter.ALL) {
            summaryDateText = "All time";
            totalAmt = (page == Constants.HOME) ? db.getConvertedFilteredTotalAmt(accFilters, catFilters, ((HomeFragment) fragment).getSearchQuery()) : db.getConvertedTotalAmt("");
        } else {
            String dtf;
            switch (state) {
                case DateGridAdapter.MONTH: dtf = "MMM yyyy"; break;
                case DateGridAdapter.YEAR: dtf = "yyyy"; break;
                default: dtf = "dd MMM yyyy";
            }
            if (getDatetimeStr(from, dtf).equals(getDatetimeStr(to, dtf))) {
                switch (state) {
                    case DateGridAdapter.DAY:
                    case DateGridAdapter.SELECT_SINGLE:
                        summaryDateText = getRelativePrefix(from);
                        summaryDateText += getDatetimeStr(from, ", dd MMM yyyy");
                        break;
                    case DateGridAdapter.MONTH:
                        summaryDateText = getDatetimeStr(from, "MMMM yyyy");
                        break;
                    default:
                        summaryDateText = getDatetimeStr(from, dtf);
                }
            } else {
                summaryDateText = getDatetimeStr(from, dtf) + " - " + getDatetimeStr(to, dtf);
            }
            totalAmt = (page == Constants.HOME) ? db.getConvertedFilteredTotalAmtInDateRange(accFilters, catFilters, from, to, ((HomeFragment) fragment).getSearchQuery()) : db.getConvertedTotalAmtInDateRange(from, to, "");
        }

        // set summary data
        if (page == Constants.HOME)
            ((HomeFragment) fragment).setSummaryData(summaryDateText.toUpperCase(), totalAmt);
        else if (page == Constants.CHARTS)
            ((ChartsFragment) fragment).setSummaryData(summaryDateText.toUpperCase(), totalAmt, true);
    }
    public void updateDateRange(int page, Calendar from, Calendar to, int selectedDatePos, int selectedDateState) {
        if (page == Constants.HOME) {
            HomeFragment homeFragment = (HomeFragment) getFragment(page);
            homeFragment.setDateRange(from, to, selectedDatePos, selectedDateState);
            updateHomeData();
        } else if (page == Constants.CHARTS) {
            ChartsFragment chartsFragment = (ChartsFragment) getFragment(page);
            chartsFragment.setDateRange(from, to, selectedDatePos, selectedDateState);
            updateSummaryData(page);
        }
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
            fragment.setAccFilters(filters);
            return;
        }
        if (getCurrentFragment() instanceof ChartsFragment) {
            ChartsChildFragmentGraph graphFrag = ((ChartsFragment) getCurrentFragment()).getChildFragmentGraph();
            graphFrag.setAccFilters(filters, true);
        }
    }
    public void updateCatFilters(ArrayList<Category> filters) {
        if (getCurrentFragment() instanceof HomeFragment) {
            HomeFragment fragment = (HomeFragment) getCurrentFragment();
            fragment.setCatFilters(filters);
            return;
        }
        if (getCurrentFragment() instanceof ChartsFragment) {
            ChartsChildFragmentGraph graphFrag = ((ChartsFragment) getCurrentFragment()).getChildFragmentGraph();
            graphFrag.setCatFilters(filters, true);
        }
    }

    /**
     * Getters
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
        AlertDialog expDialog = expenseDialog(true);
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
        expCurr.setText(acc.getCurrency().getSymbol());
        String[] favourites = getAllFavourites(this);
        if (favourites.length > 0) {
            ArrayAdapter<String> favAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, favourites);
            expDesc.setAdapter(favAdapter);
            expDesc.setOnItemClickListener((parent, view, position, id) -> {
                String selected = (String) parent.getItemAtPosition(position);
                Favourite action = getFavourite(this, selected);
                if (action == null) return;
                setFavouriteViews(action);
            });
        }

        // actions
        favouritesBtn.setOnClickListener(favouritesListener);
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
            AlertDialog.Builder changeDate = new AlertDialog.Builder(MainActivity.this, R.style.NormalDialog);
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
            if (expAmt.getText().toString().isEmpty())
                Toast.makeText(MainActivity.this, "Amount cannot be 0. No expense created", Toast.LENGTH_SHORT).show();
            else {
                String desc = expDesc.getText().toString();
                Account acc1 = db.getAccount(expAccName.getText().toString());
                Currency currency = db.getCurrency(expCurr.getText().toString());
                String datetime = getDatetimeStr(cal, "");
                if (receiptItemAdapter == null || receiptItemAdapter.getTotalAmt() == 0f ) {
                    float amt = Float.parseFloat(expAmt.getText().toString());
                    Category cat1 = db.getCategory(expCatName.getText().toString());
                    Expense expense = new Expense(amt, desc, acc1, cat1, datetime, currency);
                    db.createExpense(expense);
                } else {
                    ArrayList<Expense> expenses = new ArrayList<>();
                    HashMap<String,Float> receiptCatAmts = receiptItemAdapter.getReceiptCatAmts();
                    for (Map.Entry<String,Float> set : receiptCatAmts.entrySet()) {
                        Category cat1 = db.getCategory(set.getKey());
                        expenses.add(new Expense(set.getValue(), desc, acc1, cat1, datetime, currency));
                    }
                    db.createExpenses(expenses);
                }
                updateHomeData(); // update summary & expense list
                setUpdateFragments(true);
            }
            hideKeyboard(expAmt);
            expDialog.dismiss();
            receiptItemAdapter = null;
        });
        scanReceiptBtn.setOnClickListener(view -> {
            if (receiptItemAdapter == null) {
                Intent intent = getCameraGalleryIntent(this);
                accessPhoneFeatures(this, intent, imageLauncher);
            } else {
                chooseReceiptItems(receiptItemAdapter.getReceiptItems());
            }
            hideKeyboard(expAmt);
        });
        expCurr.setOnClickListener(view -> {
            Currency selectedCurrency = db.getCurrency(expCurr.getText().toString());
            CurrencyAdapter adapter = new CurrencyAdapter(MainActivity.this, db.getAllCurrencies(), selectedCurrency);
            final View view1 = getLayoutInflater().inflate(R.layout.dialog_recyclerview, null);
            RecyclerView currencyList = view1.findViewById(R.id.recyclerView);
            currencyList.setLayoutManager(new LinearLayoutManager(MainActivity.this, LinearLayoutManager.VERTICAL, false));
            currencyList.setAdapter(adapter);

            dialogBuilder = new AlertDialog.Builder(MainActivity.this, R.style.NormalDialog);
            dialogBuilder.setTitle("Expense currency")
                    .setView(view1)
                    .setPositiveButton(android.R.string.yes, (dialogInterface, i) -> expCurr.setText(adapter.getSelected().getSymbol()))
                    .setNeutralButton(android.R.string.no, (dialogInterface, i) -> {});
            dialogBuilder.create().show();
        });
    }
    public void editExpense(int id) {
        AlertDialog expDialog = expenseDialog(false);
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
        expCurr.setText(exp.getCurrency().getSymbol());
        expDate.setText(getString(R.string.full_date,getRelativePrefix(exp.getDatetime()),exp.getDatetimeStr("dd MMMM yyyy")).toUpperCase());
        if (isFavourite()) toggleFavouritesBtn(true);

        // actions
        favouritesBtn.setOnClickListener(favouritesListener);
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
                Currency currency = db.getCurrency(expCurr.getText().toString());
                Expense expense = new Expense(id, amt, desc, acc1, cat1, exp.getDatetime(), currency);
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
            AlertDialog.Builder changeDate = new AlertDialog.Builder(MainActivity.this, R.style.NormalDialog);
            DatePicker datePicker = new DatePicker(MainActivity.this);
            Calendar expDatetime = exp.getDatetime();
            datePicker.setFirstDayOfWeek(getDefaultFirstDayOfWeek());
            datePicker.updateDate(expDatetime.get(Calendar.YEAR), expDatetime.get(Calendar.MONTH), expDatetime.get(Calendar.DAY_OF_MONTH));
            changeDate.setView(datePicker)
                    .setPositiveButton(android.R.string.yes, (dialogInterface, i) -> {
                expDatetime.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth());
                exp.setDatetime(expDatetime);
                String datePrefix1 = getRelativePrefix(expDatetime);
                expDate.setText(getString(R.string.full_date, datePrefix1, exp.getDatetimeStr("dd MMMM yyyy")).toUpperCase());
            })
                    .setNeutralButton(android.R.string.no, (dialog, which) -> {
                dialog.cancel(); // close dialog
            })
                    .show();
        });
        expCurr.setOnClickListener(view -> {
            Currency selectedCurrency = db.getCurrency(expCurr.getText().toString());
            CurrencyAdapter adapter = new CurrencyAdapter(MainActivity.this, db.getAllCurrencies(), selectedCurrency);
            final View view1 = getLayoutInflater().inflate(R.layout.dialog_recyclerview, null);
            RecyclerView currencyList = view1.findViewById(R.id.recyclerView);
            currencyList.setLayoutManager(new LinearLayoutManager(MainActivity.this, LinearLayoutManager.VERTICAL, false));
            currencyList.setAdapter(adapter);

            dialogBuilder = new AlertDialog.Builder(MainActivity.this, R.style.NormalDialog);
            dialogBuilder.setTitle("Expense currency")
                    .setView(view1)
                    .setPositiveButton(android.R.string.yes, (dialogInterface, i) -> expCurr.setText(adapter.getSelected().getSymbol()))
                    .setNeutralButton(android.R.string.no, (dialogInterface, i) -> {});
            dialogBuilder.create().show();
        });
    }
    public void addAccount() {
        AlertDialog addAccDialog = sectionDialog();
        sectionDelBtn.setVisibility(LinearLayout.GONE);

        // set values of new account
        sectionName.setText("");
        sectionType.setText(getString(R.string.new_acc_title));
        setEditAccOptions(iconMap.get(R.drawable.acc_cash), colorMap.get(R.color.cat_bleu_de_france));
        sectionCurr.setText(getDefaultCurrencyName());

        // actions
        sectionCurrRow.setOnClickListener(view1 -> {
            Currency selectedCurrency = db.getCurrency(sectionCurr.getText().toString());
            CurrencyAdapter adapter = new CurrencyAdapter(MainActivity.this, db.getAllCurrencies(), selectedCurrency);
            final View view = getLayoutInflater().inflate(R.layout.dialog_recyclerview, null);
            RecyclerView currencyList = view.findViewById(R.id.recyclerView);
            currencyList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
            currencyList.setAdapter(adapter);

            dialogBuilder = new AlertDialog.Builder(this, R.style.NormalDialog);
            dialogBuilder.setTitle("Account currency")
                    .setView(view)
                    .setPositiveButton(android.R.string.yes, (dialogInterface, i) -> sectionCurr.setText(adapter.getSelected().getName()))
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

        // set values of selected account
        sectionName.setText(acc.getName());
        sectionType.setText(getString(R.string.Acc));
        sectionCurr.setText(acc.getCurrency().getName());
        setEditAccOptions(acc.getIconName(), acc.getColorName());
        if(acc.getName().equals(getDefaultAccName()))
            sectionDelBtn.setVisibility(View.GONE);

        // actions
        sectionCurrRow.setOnClickListener(view1 -> {
            Currency selectedCurrency = db.getCurrency(sectionCurr.getText().toString());
            CurrencyAdapter adapter = new CurrencyAdapter(MainActivity.this, db.getAllCurrencies(), selectedCurrency);
            final View view = getLayoutInflater().inflate(R.layout.dialog_recyclerview, null);
            RecyclerView currencyList = view.findViewById(R.id.recyclerView);
            currencyList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
            currencyList.setAdapter(adapter);

            dialogBuilder = new AlertDialog.Builder(this, R.style.NormalDialog);
            dialogBuilder.setTitle("Account currency")
                    .setView(view)
                    .setPositiveButton(android.R.string.yes, (dialogInterface, i) -> sectionCurr.setText(adapter.getSelected().getName()))
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

        // set values of new category
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
//                updateHomeData();
                setUpdateFragments(true);
            }
            dialog.dismiss();
        });

        dialog.show();
    }
    private final View.OnClickListener favouritesListener = view -> {
        if (receiptItemAdapter != null) return;
        String desc = expDesc.getText().toString();
        if (desc.isEmpty()) {
            Toast.makeText(this, "Description cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isFavourite()) {
            removeFavourite(this, desc);
            toggleFavouritesBtn(false);
        } else {
            setFavourite(this, desc, new Favourite(
                    expAccName.getText().toString(),
                    expCatName.getText().toString(),
                    expAmt.getText().toString(),
                    db.getCurrency(expCurr.getText().toString()).getName()
            ));
            toggleFavouritesBtn(true);
        }
    };

    /**
     * Helper functions
     */
    public static <T extends Section> ArrayList<T> clone(ArrayList<T> arrayList) {
        ArrayList<T> newList = new ArrayList<>(arrayList.size());
        for (T t : arrayList)
            newList.add(t.copy());
        return newList;
    }
    public float convertExpenseAmt(Expense exp) {
        if (exp.getId() == -1) return 0f;
        Currency thisCurr = db.getCurrency(exp.getAccount().getCurrency().getName());
        if (thisCurr.equals(getDefaultCurrency()))
            return exp.getAmount();
        else
            return exp.getAmount() * thisCurr.getRate();
    }
    public float convertAmtToDefaultCurrency(Float amt, Currency currency) {
        return convertAmtToCurrency(amt, currency, getDefaultCurrency());
    }
    public float convertAmtToCurrency(Float amt, Currency fromCurrency, Currency toCurrency) {
        if (fromCurrency.equals(toCurrency))
            return amt;
        else
            return amt * fromCurrency.getRate() / toCurrency.getRate();
    }
    public static int convertDpToPx(Context context, float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                context.getResources().getDisplayMetrics());
    }
    public static float convertPxToDp(Context context, int px) {
        float dpToPx = convertDpToPx(context, 1f);
        return px / dpToPx;
    }
    public Currency getCurrencyFromName(String name) {
        return db.getCurrency(name);
    }
    public <T extends Section> ArrayList<Integer> getFilterIds(ArrayList<T> arr) {
        return arr
                .stream()
                .map(Section::getId).collect(Collectors.toCollection(ArrayList::new));
    }
    public ArrayList<Account> getFilterAccounts(ArrayList<Integer> arr) {
        return arr
                .stream()
                .map(v -> db.getAccount(v))
                .collect(Collectors.toCollection(ArrayList::new));
    }
    public ArrayList<Category> getFilterCategories(ArrayList<Integer> arr) {
        return arr
                .stream()
                .map(v -> db.getCategory(v))
                .collect(Collectors.toCollection(ArrayList::new));
    }
    public int getNewPosAcc() { return db.getNewPosAccount(); }
    public int getNewPosCat() {
        return db.getNewPosCategory();
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
        int colorId = context.getResources().getIdentifier(name, "color", context.getPackageName());
        if (colorId == 0) return R.color.cat_imperial_primer;
        else return colorId;
    }
    public static String getColorHexFromName(Context context, String name) {
        return getColorHexFromId(context, getColorIdFromName(context, name));
    }
    public static String getColorHexFromId(Context context, int id) {
        return Integer.toHexString(getColorFromId(context, id));
    }
    public static ColorStateList getColorStateListFromName(Context context, String name) {
        return getColorStateListFromHex(getColorHexFromName(context, name));
    }
    public static ColorStateList getColorStateListFromHex(String hex) {
        return ColorStateList.valueOf(Color.parseColor("#" + hex));
    }
    public static ColorStateList getColorStateListFromId(Context context, int id) {
        return ColorStateList.valueOf(getColorFromId(context, id));
    }
    public static ColorStateList getColorStateListFromAttr(Context context, int attr) {
        return ColorStateList.valueOf(getResourceFromAttr(context, attr));
    }
    public static int getColorFromHex(String hex) {
        return Color.parseColor("#" + hex);
    }
    public static int getColorFromId(Context context, int id) {
        return ContextCompat.getColor(context, id);
    }
    public static int getResourceFromAttr(Context context, int attr) {
        TypedValue a = new TypedValue();
        context.getTheme().resolveAttribute(attr, a, true);
        return a.data;
    }
    public static int getResourceIdFromAttr(Context context, int attr) {
        TypedValue a = new TypedValue();
        context.getTheme().resolveAttribute(attr, a, true);
        return a.resourceId;
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
    public Currency getDefaultCurrency() {
        return db.getCurrency(getDefaultCurrencyName());
    }
    public String getDefaultCurrencyName() {
        return getDefaultCurrencyName(this);
    }
    public String getDefaultCurrencySymbol() {
        return getDefaultCurrency().getSymbol();
    }
    public static String getDefaultCurrencyName(Context context) {
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
     * User favourites
     */
    public static void setFavourite(Context context, String description, Favourite action) {
        SharedPreferences pref = context.getSharedPreferences(Constants.FAVOURITES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        Gson gson = new Gson();
        String json = gson.toJson(action, Favourite.class);
        editor.putString(description, json);
        editor.apply();
    }
    public static void removeFavourite(Context context, String description) {
        SharedPreferences pref = context.getSharedPreferences(Constants.FAVOURITES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.remove(description);
        editor.apply();
    }
    public static String[] getAllFavourites(Context context) {
        SharedPreferences pref = context.getSharedPreferences(Constants.FAVOURITES, Context.MODE_PRIVATE);
        Set<String> set = pref.getAll().keySet();
        String[] favourites = new String[set.size()];
        return set.toArray(favourites);
    }
    public static Favourite getFavourite(Context context, String description) {
        SharedPreferences pref = context.getSharedPreferences(Constants.FAVOURITES, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = pref.getString(description, "");
        if (json.isEmpty()) return null;
        return gson.fromJson(json, Favourite.class);
    }
    public static boolean isFavourite(Context context, String desc) {
        return (getFavourite(context, desc) != null);
    }
    private boolean isFavourite() {
        return isFavourite(this, expDesc.getText().toString());
    }
    private void setFavouriteViews(Favourite favourite) {
        toggleFavouritesBtn(true);

        String accName = favourite.getAccName();
        String currencyName = "";
        if (!accName.isEmpty()) {
            Account acc = db.getAccount(accName);
            if (acc.getId() != -1) {
                expAccName.setText(acc.getName());
                expAccIcon.setForeground(acc.getIcon());
                expAccIcon.setForegroundTintList(getColorStateListFromName(this, acc.getColorName()));
                expAccBox.setBackgroundColor(Color.parseColor("#" + acc.getColorHex()));
                currencyName = acc.getCurrency().getName();
            }
        }

        if (receiptItemAdapter != null) return;

        String catName = favourite.getCatName();
        if (!catName.isEmpty()) {
            Category cat = db.getCategory(catName);
            if (cat.getId() != -1) {
                expCatName.setText(cat.getName());
                expCatIcon.setForeground(cat.getIcon());
                expCatIcon.setForegroundTintList(getColorStateListFromHex(cat.getColorHex()));
                expCatBox.setBackgroundColor(cat.getColor());
            }
        }

        String amount = favourite.getAmount();
        if (amount.isEmpty()) return;
        expAmt.setText(amount);

        if (!(favourite.getCurrencyName() == null))
            currencyName = favourite.getCurrencyName();
        Currency currency = db.getCurrency(currencyName);
        expCurr.setText(currency.getSymbol());
    }
    private void toggleFavouritesBtn(boolean show) {
        int id = (show) ? R.drawable.ic_baseline_star_24 : R.drawable.ic_baseline_star_outline_24;
        favouritesBtn.setImageResource(id);
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
     * Import/export helper functions
     */
    public static void accessPhoneFeatures(Context context, Intent intent, ActivityResultLauncher<Intent> launcher) {
        if (permissionsNotGranted(context)) {
            requestPermissions((Activity) context);
            return;
        }
        launcher.launch(intent);
    }
    public void accessPhoneFeatures(Intent intent, ActivityResultLauncher<Intent> launcher) {
        accessPhoneFeatures(this, intent, launcher);
    }
    public static boolean permissionsNotGranted(Context context) {
        return context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PERMISSION_GRANTED ||
                context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED;
    }
    public boolean permissionsNotGranted() {
        return permissionsNotGranted(this);
    }
    public static void requestPermissions(Activity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[] { Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE },
                0);
    }
    public void requestPermissions() {
        requestPermissions(this);
    }
    public void exportToCsv() {
        // content
        StringBuilder data = new StringBuilder("Date,Account,Category,Description,Amount,Currency,Amount (" + getDefaultCurrencyName() + ")\n");
        ArrayList<Expense> expenses = db.getSortedAllExpenses(Constants.DESCENDING, "");
        for (Expense e : expenses) {
            data.append(e.getDatetimeStr()).append(",");
            data.append(e.getAccount().getName()).append(",");
            data.append(e.getCategory().getName()).append(",");
            data.append(e.getDescription()).append(",");
            data.append(String.format(locale, "%.2f", e.getAmount())).append(",");
            data.append(e.getAccount().getCurrency().getName()).append(",");
            data.append(String.format(locale, "%.2f", convertExpenseAmt(e))).append("\n");
        }

        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, "expenses.csv");   // file name
        values.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv");
        Uri extVolumeUri = MediaStore.Files.getContentUri("external");
        Uri fileUri = getContentResolver().insert(extVolumeUri, values);

        try {
            OutputStream output = getContentResolver().openOutputStream(fileUri);
            output.write(data.toString().getBytes());
            output.flush();
            output.close();
            Toast.makeText(this, "Database exported to Downloads" , Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(this, "Export failed", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Intents & launchers
     */
    private final ActivityResultLauncher<Intent> importLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() != RESULT_OK)
                    return;
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
            });
    public static ActivityResultLauncher<Intent> createImageLauncher(Context context) {
        return ((AppCompatActivity) context).registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != RESULT_OK) {
                        if (context instanceof WidgetStaticActivity) ((WidgetStaticActivity) context).finish();
                        return;
                    }
                    Uri uri = (result.getData() == null) ? imageUri : result.getData().getData();
                    try {
                        ByteArrayInputStream input = FileUtils.getInputStreamFromUri(context, uri);
                        postRequestImage(context, input);
                    } catch (IOException e) {
                        Log.e(TAG, e.toString());
                        if (context instanceof WidgetStaticActivity) ((WidgetStaticActivity) context).finish();
                    }
                });
    }
    public static Intent getCameraGalleryIntent(Context context) {
        Intent photoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = FileUtils.createImageFile(context);
        if (photoFile == null) {
            if (context instanceof WidgetStaticActivity) ((WidgetStaticActivity) context).finish();
            return null;
        }
        Uri uri = FileProvider.getUriForFile(context,
                BuildConfig.APPLICATION_ID + ".provider",
                photoFile);
        MainActivity.setImageUri(uri);
        photoIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
        getIntent.setType("image/*");
        getIntent.putExtra("requestCode", "camera");
        Intent pickIntent = new Intent(Intent.ACTION_PICK);
        pickIntent.setDataAndType(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        pickIntent.putExtra("requestCode", "gallery");
        Intent chooserIntent = Intent.createChooser(getIntent, "Select image");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] {photoIntent});
        return chooserIntent;
    }

    /**
     * Scan receipt
     */
    public final ActivityResultLauncher<Intent> imageLauncher = createImageLauncher(this);
    public static void postRequestImage(Context context, InputStream inputStream) throws IOException {
        Consumer<Boolean> showOverlay = (show) -> {
            if (context instanceof MainActivity) {
                if (show) ((MainActivity) context).showProgressOverlay();
                else ((MainActivity) context).hideProgressOverlay();
            }
            if (context instanceof WidgetStaticActivity) {
                if (show) ((WidgetStaticActivity) context).showProgressOverlay();
                else ((WidgetStaticActivity) context).hideProgressOverlay();
            }
            if (context instanceof WidgetDialogActivity) {
                if (show) ((WidgetDialogActivity) context).showProgressOverlay();
                else ((WidgetDialogActivity) context).hideProgressOverlay();
            }
        };

        String url = "https://api.ocr.space/parse/image"; // OCR API Endpoints
//        String url = "http://139.162.49.140:5000/scan-receipt";
        String apiKey = "K85073676188957";

        // http request
        MediaType MEDIA_TYPE_JPEG = MediaType.parse("image/jpeg");
        RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("file", "receipt.jpg",
                        FileUtils.createRequestBody(MEDIA_TYPE_JPEG, inputStream)
                )
                .addFormDataPart("apikey", apiKey)
                .addFormDataPart("language", "eng")
                .addFormDataPart("isOverlayRequired", "false")
                .addFormDataPart("detectOrientation", "true")
                .addFormDataPart("scale", "true")
                .addFormDataPart("isTable", "true")
                .addFormDataPart("OCREngine", "2")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        showOverlay.accept(true);
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        client.newCall(request)
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        showOverlay.accept(false);
                        ((Activity) context).runOnUiThread(() -> Toast.makeText(context, "Error: " + e, Toast.LENGTH_SHORT).show());
                        if (context instanceof WidgetStaticActivity) ((WidgetStaticActivity) context).finish();
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull final Response response) throws IOException {
                        String res = response.body().string();
//                        Log.e(TAG, res);
                        showOverlay.accept(false);
                        JSONObject results = null;
                        try {
                            results = new JSONObject(res);
                            if (results.has("ErrorMessage")) {
                                String errorMessage = results.getJSONArray("ErrorMessage").getString(0);
                                ((Activity) context).runOnUiThread(() -> Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show());
//                                showOverlay.accept(false);
                                if (context instanceof WidgetStaticActivity) ((WidgetStaticActivity) context).finish();
                                return;
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        ArrayList<ReceiptItem> receiptItems = parseJSON(results);
//                        showOverlay.accept(false);
                        if (receiptItems.isEmpty()) {
                            ((Activity) context).runOnUiThread(() -> Toast.makeText(context, "OCR failed. Try taking another photo", Toast.LENGTH_LONG).show());
                            if (context instanceof WidgetStaticActivity) ((WidgetStaticActivity) context).finish();
                        } else {
                            ((Activity) context).runOnUiThread(() -> {
                                if (context instanceof MainActivity) ((MainActivity) context).chooseReceiptItems(receiptItems);
                                if (context instanceof WidgetStaticActivity) ((WidgetStaticActivity) context).chooseReceiptItems(receiptItems);
                                if (context instanceof WidgetDialogActivity) ((WidgetDialogActivity) context).chooseReceiptItems(receiptItems);
                            });
                        }
                    }
                });
    }
    public void chooseReceiptItems(ArrayList<ReceiptItem> receiptItems) {
        String accName = expAccName.getText().toString();
        String accCurr = db.getAccount(accName).getCurrency().getSymbol();
        receiptItemAdapter = new ReceiptItemAdapter(this, receiptItems, accCurr);
        final View view = getLayoutInflater().inflate(R.layout.dialog_receipt, null);
        RecyclerView receiptItemList = view.findViewById(R.id.recyclerView);
        receiptItemList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        receiptItemList.setAdapter(receiptItemAdapter);

        receiptCatIcon = view.findViewById(R.id.selectCat);
        receiptCatIcon.setOnClickListener(view1 -> receiptCatDialog());

        dialogBuilder = new AlertDialog.Builder(this, R.style.NormalDialog);
        dialogBuilder.setTitle("Receipt items")
                .setView(view)
                .setPositiveButton(android.R.string.yes, (dialogInterface, i) -> {
                    expAmt.setText(String.format(MainActivity.locale, "%.2f", receiptItemAdapter.getTotalAmt()));
                    expAmt.setSelection(expAmt.getText().length()); // set cursor to end of text
                    scanReceiptBtn.setImageResource(R.drawable.ic_baseline_edit_24);
                    expCatIcon.setForeground(getIconFromId(MainActivity.this, R.drawable.cat_multi));
                    expCatName.setText(R.string.multiple);
                })
                .setNeutralButton(android.R.string.no, (dialogInterface, i) -> {
                    if (expAmt.getText().toString().isEmpty()) // only nullify adapter if cancelled on first dialog opening
                        receiptItemAdapter = null;
                });
        AlertDialog dialog = dialogBuilder.create();
        dialog.setCancelable(false);
        // solves problem with keyboard not showing up
        dialog.setOnShowListener(d -> dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM));
        dialog.show();
    }
    public static ArrayList<ReceiptItem> parseJSON(JSONObject res) {
        ArrayList<ReceiptItem> receiptItems = new ArrayList<>();
        try {
            String[] lines = res.getJSONArray("ParsedResults")
                    .getJSONObject(0)
                    .getString("ParsedText")
                    .split("\r\n");
            Pattern pattern = Pattern.compile("([0-9]+[\\.,][0-9]+)");
            for (String line : lines) {
                String[] components = line.split("\t");
                if (components.length < 2) continue;
                Matcher matcher = pattern.matcher(components[1]);
                if (!matcher.find()) continue;
                String item = components[0];
                float amount = Float.parseFloat(matcher.group());
                if (components[1].charAt(0) == '-') amount = -amount;
                receiptItems.add(new ReceiptItem(item, amount));
            }
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
        return receiptItems;
    }
    public static void setImageUri(Uri uri) {
        imageUri = uri;
    }

    /**
     * Others
     */
    public static void showKeyboard(Context context, View view) {
        view.postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager) context.getSystemService(INPUT_METHOD_SERVICE);
            imm.showSoftInput(view, 0);
        }, 270);
    }
    public static void hideKeyboard(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
    public void showKeyboard(View view) {
        showKeyboard(this, view);
    }
    public void hideKeyboard(View view) {
        hideKeyboard(this, view);
    }
    public void showProgressOverlay() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.FullscreenAlertDialog);
        builder.setView(R.layout.progress_overlay)
                .setCancelable(false);
        progressDialog = builder.create();
        progressDialog.setCancelable(false);
        progressDialog.show();
    }
    public void hideProgressOverlay() {
        progressDialog.dismiss();
    }

    /**
     * Debug
     */
    public static void logFromTo(String pageTag, String tag, Calendar from, Calendar to) {
        Log.e(pageTag, String.format("from%s=", tag) + MainActivity.getDatetimeStr(from,"dd MMM yyyy") + String.format(", to%s=", tag) + MainActivity.getDatetimeStr(to,"dd MMM yyyy"));
    }
    public static String logExpenses(ArrayList<Expense> expenses) {
        StringBuilder msg = new StringBuilder();
        for (Expense e : expenses)
            msg.append(e.getDescription().isEmpty() ? "date" : e.getDescription()).append(", ");
        return msg.toString();
    }
    public static <T extends Section> void logFilters(String pageTag, ArrayList<T> arrayList, String arrayName) {
        StringBuilder msg = new StringBuilder();
        for (T t : arrayList) {
            if (msg.length() > 0) msg.append(", ");
            msg.append(t.getName());
        }
        Log.e(pageTag, arrayName + "={ " + msg + " }");
    }
    public static <T> void logArray(String tag, String arrayName, ArrayList<T> array) {
        StringBuilder s = new StringBuilder("[");
        for (int i = 0; i < array.size(); i++) {
            T t = array.get(i);
            s.append(t.toString());
            if (i < array.size() - 1)
                s.append(",");
        }
        s.append("]");
        Log.e(TAG, arrayName + "=" + s);
    }

}