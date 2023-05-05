package com.example.expensetracker.Widget;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputFilter;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.expensetracker.Account;
import com.example.expensetracker.BuildConfig;
import com.example.expensetracker.Category;
import com.example.expensetracker.DatabaseHelper;
import com.example.expensetracker.Expense;
import com.example.expensetracker.Favourite;
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

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class WidgetDialogActivity extends AppCompatActivity {

    public DatabaseHelper db = new DatabaseHelper(this);

    // Dialog components
    private AlertDialog.Builder dialogBuilder;
    private AlertDialog progressDialog;
    private EditText expAmt;
    private AutoCompleteTextView expDesc;
    private TextView expAccName, expCatName, expDate, expCurr;
    private ImageButton expAccIcon, expCatIcon, scanReceiptBtn, favouritesBtn, receiptCatIcon;
    private LinearLayout expAccBox, expCatBox, expDelBtn, expDateBtn, expSave;
    private ReceiptItemAdapter receiptItemAdapter = null;

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
        expDate.setText(getString(R.string.full_date,"Today", MainActivity.getDatetimeStr(cal,"dd MMMM yyyy")).toUpperCase());
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
        expCurr.setText(acc.getCurrencySymbol());
        String[] favourites = MainActivity.getAllFavourites(this);
        if (favourites.length > 0) {
            ArrayAdapter<String> favAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, favourites);
            expDesc.setAdapter(favAdapter);
            expDesc.setOnItemClickListener((parent, view, position, id) -> {
                String selected = (String) parent.getItemAtPosition(position);
                Favourite action = MainActivity.getFavourite(this, selected);
                if (action == null) return;
                setFavouriteViews(action);
            });
        }

        // actions
        favouritesBtn.setOnClickListener(view -> {
            if (receiptItemAdapter != null) return;
            String desc = expDesc.getText().toString();
            if (desc.isEmpty()) {
                Toast.makeText(this, "Description cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            if (isFavourite()) {
                MainActivity.removeFavourite(this, desc);
                toggleFavouritesBtn(false);
            } else {
                MainActivity.setFavourite(this, desc, new Favourite(
                        expAccName.getText().toString(),
                        expCatName.getText().toString(),
                        expAmt.getText().toString()));
                toggleFavouritesBtn(true);
            }
        });
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
                        expDate.setText(getString(R.string.full_date, MainActivity.getRelativePrefix(cal), MainActivity.getDatetimeStr(cal,"dd MMM yyyy")).toUpperCase());
            })
                    .setNeutralButton(android.R.string.no, (dialog, which) -> dialog.cancel())
                    .show();
        });
        expSave.setOnClickListener(v -> {
            if (expAmt.getText().toString().isEmpty())
                Toast.makeText(WidgetDialogActivity.this, "Amount cannot be 0. No expense created", Toast.LENGTH_SHORT).show();
            else {
                String desc = expDesc.getText().toString();
                Account acc1 = db.getAccount(expAccName.getText().toString());
                String datetime = MainActivity.getDatetimeStr(cal, "");
                if (receiptItemAdapter == null || receiptItemAdapter.getTotalAmt() == 0f ) {
                    float amt = Float.parseFloat(expAmt.getText().toString());
                    Category cat1 = db.getCategory(expCatName.getText().toString());
                    Expense expense = new Expense(amt, desc, acc1, cat1, datetime);
                    db.createExpense(expense);
                } else {
                    ArrayList<Expense> expenses = new ArrayList<>();
                    HashMap<String,Float> receiptCatAmts = receiptItemAdapter.getReceiptCatAmts();
                    for (Map.Entry<String,Float> set : receiptCatAmts.entrySet()) {
                        Category cat1 = db.getCategory(set.getKey());
                        expenses.add(new Expense(set.getValue(), desc, acc1, cat1, datetime));
                    }
                    db.createExpenses(expenses);
                }
            }
            hideKeyboard(expAmt);
            expDialog.dismiss();
            receiptItemAdapter = null;
        });
        scanReceiptBtn.setOnClickListener(view -> {
            if (receiptItemAdapter == null) {
                dialogBuilder = new AlertDialog.Builder(this);
                View dialogView = getLayoutInflater().inflate(R.layout.dialog_camera, null);
                LinearLayout cameraOpt, galleryOpt;
                cameraOpt = dialogView.findViewById(R.id.cameraOpt);
                galleryOpt = dialogView.findViewById(R.id.galleryOpt);
                dialogBuilder.setView(dialogView)
                        .setTitle(R.string.photo_dialog_title)
                        .setPositiveButton(android.R.string.no, (dialogInterface, i) -> {
                        });
                AlertDialog dialog = dialogBuilder.show();
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
                    MainActivity.accessPhoneFeatures(this, intent, MainActivity.createCameraLauncher(this));
                });
                galleryOpt.setOnClickListener(view12 -> {
                    dialog.dismiss();
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("image/*");
                    MainActivity.accessPhoneFeatures(this, intent, MainActivity.createGalleryLauncher(this));
                });
            } else {
                chooseReceiptItems(receiptItemAdapter.getReceiptItems());
            }
            hideKeyboard(expAmt);
        });
    }

    /**
     * Dialogs
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
        scanReceiptBtn = expView.findViewById(R.id.scanReceiptBtn);
        favouritesBtn = expView.findViewById(R.id.favouritesBtn);

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
        @SuppressLint("InflateParams") final View expOptSectionView = getLayoutInflater().inflate(R.layout.dialog_expense_opt_section, null);
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
        @SuppressLint("InflateParams") final View expOptSectionView = getLayoutInflater().inflate(R.layout.dialog_expense_opt_section, null);
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
     * Favourites
     */
    private boolean isFavourite() {
        return MainActivity.isFavourite(this, expDesc.getText().toString());
    }
    private void setFavouriteViews(Favourite favourite) {
        toggleFavouritesBtn(true);

        String accName = favourite.getAccName();
        if (!accName.isEmpty()) {
            Account acc = db.getAccount(accName);
            if (acc.getId() != -1) {
                expAccName.setText(acc.getName());
                expAccIcon.setForeground(acc.getIcon());
                expAccIcon.setForegroundTintList(MainActivity.getColorStateListFromName(this, acc.getColorName()));
                expAccBox.setBackgroundColor(Color.parseColor("#" + acc.getColorHex()));
                expCurr.setText(acc.getCurrencySymbol());
            }
        }

        if (receiptItemAdapter != null) return;

        String catName = favourite.getCatName();
        if (!catName.isEmpty() && receiptItemAdapter == null) {
            Category cat = db.getCategory(catName);
            if (cat.getId() != -1) {
                expCatName.setText(cat.getName());
                expCatIcon.setForeground(cat.getIcon());
                expCatIcon.setForegroundTintList(MainActivity.getColorStateListFromHex(cat.getColorHex()));
                expCatBox.setBackgroundColor(cat.getColor());
            }
        }

        String amount = favourite.getAmount();
        if (amount.isEmpty()) return;
        expAmt.setText(amount);
    }
    private void toggleFavouritesBtn(boolean show) {
        int id = (show) ? R.drawable.ic_baseline_star_24 : R.drawable.ic_baseline_star_outline_24;
        favouritesBtn.setImageResource(id);
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
    public void chooseReceiptItems(ArrayList<ReceiptItem> receiptItems) {
        String accName = expAccName.getText().toString();
        String accCurr = db.getAccount(accName).getCurrencySymbol();
        receiptItemAdapter = new ReceiptItemAdapter(this, receiptItems, accCurr);
        final View view = getLayoutInflater().inflate(R.layout.dialog_receipt, null);
        RecyclerView receiptItemList = view.findViewById(R.id.recyclerView);
        receiptItemList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        receiptItemList.setAdapter(receiptItemAdapter);

        receiptCatIcon = view.findViewById(R.id.selectCat);
        receiptCatIcon.setOnClickListener(view1 -> receiptCatDialog());

        dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Receipt items")
                .setView(view)
                .setPositiveButton(android.R.string.yes, (dialogInterface, i) -> {
                    expAmt.setText(String.format(MainActivity.locale, "%.2f", receiptItemAdapter.getTotalAmt()));
                    expAmt.setSelection(expAmt.getText().length()); // set cursor to end of text
                    scanReceiptBtn.setImageResource(R.drawable.ic_baseline_edit_24);
                    expCatIcon.setForeground(MainActivity.getIconFromId(WidgetDialogActivity.this, R.drawable.cat_multi));
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