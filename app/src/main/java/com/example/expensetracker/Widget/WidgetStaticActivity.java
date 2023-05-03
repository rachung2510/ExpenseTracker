package com.example.expensetracker.Widget;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputFilter;
import android.text.InputType;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensetracker.Account;
import com.example.expensetracker.BuildConfig;
import com.example.expensetracker.Category;
import com.example.expensetracker.Constants;
import com.example.expensetracker.DatabaseHelper;
import com.example.expensetracker.Expense;
import com.example.expensetracker.HelperClasses.FileUtils;
import com.example.expensetracker.HelperClasses.MoneyValueFilter;
import com.example.expensetracker.MainActivity;
import com.example.expensetracker.R;
import com.example.expensetracker.ReceiptItem;
import com.example.expensetracker.RecyclerViewAdapters.AccountAdapter;
import com.example.expensetracker.RecyclerViewAdapters.CategoryAdapter;
import com.example.expensetracker.RecyclerViewAdapters.ReceiptItemAdapter;
import com.example.expensetracker.RecyclerViewAdapters.SectionAdapter;
import com.example.expensetracker.Section;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class WidgetStaticActivity extends AppCompatActivity {
    private static final String TAG = "WidgetStaticActivity";
    public DatabaseHelper db = new DatabaseHelper(this);

    public static final String KEY_AMT = "expense_amount";
    public static final String KEY_DESC = "expense_description";
    public static final String KEY_ACC = "expense_account";
    public static final String KEY_CAT = "expense_category";
    public static final String KEY_DATE = "expense_date";
    public static final String KEY_ADAPTER_LIST = "adapter_list";
    public static final String KEY_ADAPTER_CURR = "adapter_curr";
    private static final Type adapterListType = new TypeToken<ArrayList<ReceiptItem>>(){}.getType();

    private AlertDialog progressDialog;
    private ReceiptItemAdapter receiptItemAdapter;
    private ImageButton receiptCatIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.empty_view);

        // Make status bar transparent but not navigation bar
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        setWindowFlag(this, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        int action = getIntent().getIntExtra(WidgetStaticProvider.KEY,-1);
        switch (action) {
            case WidgetStaticProvider.EDIT_AMOUNT:
                editAmount();
                break;

            case WidgetStaticProvider.EDIT_DESCRIPTION:
                editDescription();
                break;

            case WidgetStaticProvider.EDIT_ACCOUNT:
                editAccount();
                break;

            case WidgetStaticProvider.EDIT_CATEGORY:
                editCategory();
                break;

            case WidgetStaticProvider.EDIT_DATE:
                editDate();
                break;

            case WidgetStaticProvider.SAVE:
                saveExpense();
                break;

            case WidgetStaticProvider.SCAN_RECEIPT:
                scanReceipt();
                break;

            default:
                break;
        }
    }
    public static void setWindowFlag(Activity activity, final int bits, boolean on) {
        Window win = activity.getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        if (on) winParams.flags |= bits;
        else winParams.flags &= ~bits;
        win.setAttributes(winParams);
    }

    /**
     * Main Functions
     */
    public void editAmount() {
        View view = getLayoutInflater().inflate(R.layout.dialog_input, null);
        EditText input = view.findViewById(R.id.input);
        float storedValue = getFloatValue(KEY_AMT);
        if (storedValue > 0)
            input.setText(String.format(MainActivity.locale, "%.2f", storedValue));
        else
            input.setHint(getString(R.string.hint_amt));
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setFilters(new InputFilter[] { new MoneyValueFilter() });
        input.setSelection(input.getText().length()); // set cursor to end of text
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Amount")
                .setView(view)
                .setOnDismissListener(dialogInterface -> {
                    hideKeyboard(input);
                    if (input.getText().toString().isEmpty()) {
                        finish();
                        return;
                    }
                    float floatInput = Float.parseFloat(input.getText().toString());
                    RemoteViews views = getRemoteViews();
                    views.setTextViewText(R.id.newExpAmt, String.format(MainActivity.locale, "%.2f", floatInput));
                    updateView(views);
                    storeValue(KEY_AMT, floatInput);
                    finish();
                });
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            input.requestFocus();
            showKeyboard(input);
        });
        dialog.show();
    }
    public void editDescription() {
        View view = getLayoutInflater().inflate(R.layout.dialog_input, null);
        EditText input = view.findViewById(R.id.input);
        input.setHint(getString(R.string.hint_description));
        String storedValue = getStringValue(KEY_DESC);
        if (!storedValue.isEmpty())
            input.setText(storedValue);
        input.setSelection(input.getText().length()); // set cursor to end of text
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Description")
                .setView(view)
                .setOnDismissListener(dialogInterface -> {
                    hideKeyboard(input);
                    RemoteViews views = getRemoteViews();
                    views.setTextViewText(R.id.newExpDesc, input.getText().toString());
                    updateView(views);
                    storeValue(KEY_DESC, input.getText().toString());
                    finish();
                });
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            input.requestFocus();
            showKeyboard(input);
        });
        dialog.show();
    }
    public void editAccount() {
        AccountAdapter accAdapter = getAccountData();
        String storedValue = getStringValue(KEY_ACC);
        accAdapter.setSelected((storedValue.isEmpty()) ? getDefaultAccName() : storedValue);
        expenseAccDialog(accAdapter);
    }
    public void editCategory() {
        CategoryAdapter catAdapter = getCategoryData();
        String storedValue = getStringValue(KEY_CAT);
        catAdapter.setSelected((storedValue.isEmpty()) ? getDefaultCatName() : storedValue);
        expenseCatDialog(catAdapter, new Expense(Calendar.getInstance()));
    }
    public void editDate() {
        Calendar cal = Calendar.getInstance(MainActivity.locale);
        DatePicker datePicker = new DatePicker(this);
        if (getStringValue(KEY_DATE).isEmpty()) {
            cal = MainActivity.getCalFromString(Expense.DATETIME_FORMAT, getStringValue(KEY_DATE));
            datePicker.updateDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        }

        AlertDialog.Builder changeDate = new AlertDialog.Builder(this);
        Calendar finalCal = cal;
        changeDate.setView(datePicker)
                .setPositiveButton(android.R.string.yes, (dialogInterface, i) -> {
                    finalCal.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth());
                    RemoteViews views = getRemoteViews();
                    views.setTextViewText(R.id.expDate, (MainActivity.getRelativePrefix(finalCal) + ", " + MainActivity.getDatetimeStr(finalCal, "dd MMMM yyyy")).toUpperCase());
                    updateView(views);
                    storeValue(KEY_DATE, MainActivity.getDatetimeStr(finalCal, Expense.DATETIME_FORMAT));
                    finish();
                })
                .setNeutralButton(android.R.string.no, (dialog, which) -> {
                    dialog.cancel();
                    finish();
                })
                .show();
    }
    public void saveExpense() {
        float amt = getFloatValue(KEY_AMT);
        if (amt < 0f)
            Toast.makeText(this, "Amount cannot be 0. No expense created", Toast.LENGTH_SHORT).show();
        else {
            String desc = getStringValue(KEY_DESC);
            String accName = getStringValue(KEY_ACC);
            String dateValue = getStringValue(KEY_DATE);
            Account acc = (accName.isEmpty()) ? db.getAccount(db.getDefaultAccName()) : db.getAccount(accName);
            Calendar cal = Calendar.getInstance(MainActivity.locale);
            String datetime = (dateValue.isEmpty()) ? MainActivity.getDatetimeStr(cal, Expense.DATETIME_FORMAT) : dateValue;
            ReceiptItemAdapter adapter = getReceiptItemAdapter();
            if (adapter == null || adapter.getTotalAmt() == 0f ) {
                String catName = getStringValue(KEY_CAT);
                Category cat = (catName.isEmpty()) ? db.getCategory(db.getDefaultCatName()) : db.getCategory(catName);
                Expense expense = new Expense(amt, desc, acc, cat, datetime);
                db.createExpense(expense);
            } else {
                ArrayList<Expense> expenses = new ArrayList<>();
                HashMap<String,Float> receiptCatAmts = adapter.getReceiptCatAmts();
                for (Map.Entry<String,Float> set : receiptCatAmts.entrySet()) {
                    Category cat = db.getCategory(set.getKey());
                    expenses.add(new Expense(set.getValue(), desc, acc, cat, datetime));
                }
                db.createExpenses(expenses);
            }
            sendBroadcast(new Intent(this, WidgetStaticProvider.class));
        }
        finish();
    }
    public void scanReceipt() {
        ReceiptItemAdapter adapter = getReceiptItemAdapter();
        if (adapter == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_camera, null);
            LinearLayout cameraOpt, galleryOpt;
            cameraOpt = dialogView.findViewById(R.id.cameraOpt);
            galleryOpt = dialogView.findViewById(R.id.galleryOpt);
            builder.setView(dialogView)
                    .setTitle(R.string.photo_dialog_title)
                    .setPositiveButton(android.R.string.no, (dialogInterface, i) -> finish());
            AlertDialog dialog = builder.create();
            dialog.setOnCancelListener(dialogInterface -> finish());
            cameraOpt.setOnClickListener(view1 -> {
                dialog.dismiss();
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                File photoFile = FileUtils.createImageFile(this);
                if (photoFile == null) return;
                Uri uri = FileProvider.getUriForFile(this,
                        BuildConfig.APPLICATION_ID + ".provider",
                        photoFile);
                MainActivity.setImageUri(uri);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                MainActivity.accessPhoneFeatures(this, intent, cameraLauncher);
            });
            galleryOpt.setOnClickListener(view12 -> {
                dialog.dismiss();
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                MainActivity.accessPhoneFeatures(this, intent, galleryLauncher);
            });
            dialog.show();
        } else {
            chooseReceiptItems(adapter.getReceiptItems());
        }
    }

    /**
     * Dialogs
     */
    public <T extends SectionAdapter<? extends Section>> AlertDialog.Builder expenseSectionDialog(T adapter, View expOptSectionView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        RecyclerView sectionGrid = expOptSectionView.findViewById(R.id.sectionGrid);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 3, GridLayoutManager.VERTICAL, false);
        sectionGrid.setLayoutManager(gridLayoutManager);
        sectionGrid.setAdapter(adapter);
        builder.setView(expOptSectionView);
        return builder;
    }
    public void expenseAccDialog(AccountAdapter adapter) {
        @SuppressLint("InflateParams") final View view = getLayoutInflater().inflate(R.layout.dialog_expense_opt_section, null);
        AlertDialog dialog = expenseSectionDialog(adapter, view).create();
        adapter.setDialog(dialog);

        // set values
        TextView title = view.findViewById(R.id.expOptSectionTitle);
        title.setText(R.string.ACC);

        dialog.setOnCancelListener(dialog1 -> {
            Account acc = adapter.getSelected();
            RemoteViews views = getRemoteViews();
            views.setTextViewText(R.id.newExpAccName, acc.getName());
            views.setInt(R.id.newExpAccBox,"setColorFilter", acc.getColor());
            views.setTextViewText(R.id.newExpCurrency, acc.getCurrencySymbol());
            views.setImageViewBitmap(R.id.newExpAccIcon, MainActivity.drawableToBitmap(acc.getIcon()));
            views.setInt(R.id.newExpAccIcon,"setColorFilter", acc.getColor());
            updateView(views);
            storeValue(KEY_ACC, acc.getName());
            finish();
        });
        dialog.show();
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
            Category cat = adapter.getSelected();
            RemoteViews views = getRemoteViews();
            views.setTextViewText(R.id.newExpCatName, cat.getName());
            views.setInt(R.id.newExpCatBox,"setColorFilter", cat.getColor());
            views.setImageViewBitmap(R.id.newExpCatIcon, MainActivity.drawableToBitmap(cat.getIcon()));
            views.setInt(R.id.newExpCatIcon,"setColorFilter", cat.getColor());
            updateView(views);
            storeValue(KEY_CAT, cat.getName());
            finish();
        });

        dialog.show();
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
            receiptCatIcon.setBackgroundTintList(MainActivity.getColorStateListFromHex(selectedCat.getColorHex()));
            receiptItemAdapter.setReceiptCat(selectedCat);
        });

        dialog.show();
    }

    /**
     * Getters & Setters
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
     * Helper Functions
     */
    public void showKeyboard(EditText view) {
        MainActivity.showKeyboard(this, view);
    }
    public void hideKeyboard(EditText view) {
        MainActivity.hideKeyboard(this, view);
    }
    public RemoteViews getRemoteViews() {
        return new RemoteViews(this.getPackageName(), R.layout.widget_static);
    }
    public void updateView(RemoteViews views) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        ComponentName thisWidget = new ComponentName(this, WidgetStaticProvider.class);
        int[] appWidgetId = appWidgetManager.getAppWidgetIds(thisWidget);
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
    public float getFloatValue(String key) {
        SharedPreferences pref = getSharedPreferences(Constants.TMP, Context.MODE_PRIVATE);
        return pref.getFloat(key, -1f);
    }
    public void storeValue(String key, float value) {
        SharedPreferences pref = getSharedPreferences(Constants.TMP, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putFloat(key, value);
        editor.apply();
    }
    public String getStringValue(String key) {
        SharedPreferences pref = getSharedPreferences(Constants.TMP, Context.MODE_PRIVATE);
        return pref.getString(key, "");
    }
    public void storeValue(String key, String value) {
        SharedPreferences pref = getSharedPreferences(Constants.TMP, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(key, value);
        editor.apply();
    }
    public ReceiptItemAdapter getReceiptItemAdapter() {
        SharedPreferences pref = getSharedPreferences(Constants.TMP, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String listJson = pref.getString(KEY_ADAPTER_LIST, "");
        String receiptCurr = pref.getString(KEY_ADAPTER_CURR, "");
        if (listJson.isEmpty() || receiptCurr.isEmpty()) return null;
        ArrayList<ReceiptItem> receiptItems = gson.fromJson(listJson, adapterListType);
        return new ReceiptItemAdapter(this, receiptItems, receiptCurr);
    }
    public void storeReceiptItemAdapter(ReceiptItemAdapter adapter) {
        Gson gson = new Gson();
        String listJson = gson.toJson(adapter.getReceiptItems(), adapterListType);
        SharedPreferences pref = getSharedPreferences(Constants.TMP, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(KEY_ADAPTER_LIST, listJson);
        editor.putString(KEY_ADAPTER_CURR, adapter.getReceiptCurr());
        editor.apply();
    }

    /**
     * Scan receipt
     */
    public void chooseReceiptItems(ArrayList<ReceiptItem> receiptItems) {
        RemoteViews views = getRemoteViews();
        String accName = getStringValue(KEY_ACC);
        Account acc = (accName.isEmpty()) ? db.getAccount(db.getDefaultAccName()) : db.getAccount(accName);
        receiptItemAdapter = new ReceiptItemAdapter(this, receiptItems, acc.getCurrencySymbol());
        final View view = getLayoutInflater().inflate(R.layout.dialog_receipt, null);
        RecyclerView receiptItemList = view.findViewById(R.id.recyclerView);
        receiptItemList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        receiptItemList.setAdapter(receiptItemAdapter);

        receiptCatIcon = view.findViewById(R.id.selectCat);
        receiptCatIcon.setOnClickListener(view1 -> receiptCatDialog());

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Receipt items")
                .setView(view)
                .setPositiveButton(android.R.string.yes, (dialogInterface, i) -> {
                    float amt = receiptItemAdapter.getTotalAmt();
                    views.setTextViewText(R.id.newExpAmt, String.format(MainActivity.locale, "%.2f", amt));
                    views.setImageViewBitmap(R.id.scanReceiptBtn, MainActivity.drawableToBitmap(MainActivity.getIconFromId(this, R.drawable.ic_baseline_edit_24)));
                    views.setImageViewBitmap(R.id.newExpCatIcon, MainActivity.drawableToBitmap(MainActivity.getIconFromId(this, R.drawable.cat_multi)));
                    views.setInt(R.id.newExpAccIcon,"setColorFilter", acc.getColor());
                    views.setTextViewText(R.id.newExpCatName, getString(R.string.multiple));
                    storeValue(KEY_AMT, amt);
                    storeReceiptItemAdapter(receiptItemAdapter);
                    updateView(views);
                    finish();
                })
                .setNeutralButton(android.R.string.no, (dialogInterface, i) -> finish());
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        // solves problem with keyboard not showing up
        dialog.setOnShowListener(d -> dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM));
        dialog.show();
    }
    public final ActivityResultLauncher<Intent> cameraLauncher = MainActivity.createCameraLauncher(this);
    public final ActivityResultLauncher<Intent> galleryLauncher = MainActivity.createGalleryLauncher(this);
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
}
