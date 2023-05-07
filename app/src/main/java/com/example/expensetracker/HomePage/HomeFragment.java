package com.example.expensetracker.HomePage;

import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.example.expensetracker.Account;
import com.example.expensetracker.Category;
import com.example.expensetracker.Constants;
import com.example.expensetracker.Expense;
import com.example.expensetracker.MainActivity;
import com.example.expensetracker.R;
import com.example.expensetracker.RecyclerViewAdapters.AccountAdapter;
import com.example.expensetracker.RecyclerViewAdapters.CategoryAdapter;
import com.example.expensetracker.RecyclerViewAdapters.DateGridAdapter;
import com.example.expensetracker.RecyclerViewAdapters.ExpenseAdapter;
import com.example.expensetracker.RecyclerViewAdapters.FilterAdapter;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.JustifyContent;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Objects;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    // Layout components
    private RecyclerView expenseList;
    public TextView placeholder, summaryDate, summaryAmt, summaryCurr;
    private ImageButton prevDate, nextDate;
    private RecyclerView filterList;
    private MenuItem clearFilters;
    private LinearLayout expenseListLayout, searchBg;
    private SearchView searchView;
    private FloatingActionButton addExpBtn, searchBtn;
    private View dummyView;

    // Filter components
    private DateGridAdapter filterDateAdapter;

    private Calendar fromDate, toDate;
    private int selDatePos, selDateState;
    private ArrayList<Account> selAccFilters = new ArrayList<>();
    private ArrayList<Category> selCatFilters = new ArrayList<>();
    private String searchQuery = "";

    /**
     * Main
     */
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (getActivity() == null)
            return null;

        MainActivity context = (MainActivity) getActivity();

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // summary & expense list
        summaryDate = view.findViewById(R.id.summaryDate);
        summaryDateAction();
        summaryCurr = view.findViewById(R.id.summaryCurrency);
        summaryAmt = view.findViewById(R.id.summaryAmt);
        expenseList = view.findViewById(R.id.expenseList);
        expenseListLayout = view.findViewById(R.id.homeFragLinearLayout);
        ((SimpleItemAnimator) Objects.requireNonNull(expenseList.getItemAnimator())).setSupportsChangeAnimations(false);
        placeholder = view.findViewById(R.id.placeholder);

        // date navigation buttons
        prevDate = view.findViewById(R.id.prevDate);
        nextDate = view.findViewById(R.id.nextDate);
        prevDate.setOnClickListener((l) -> navDateAction(Constants.PREV));
        nextDate.setOnClickListener((l) -> navDateAction(Constants.NEXT));

        // update data
        updateData(); // update summary & expense list

        // apply filters
        filterList = view.findViewById(R.id.sectionFilters);
        applyFiltersViewItems();

        // search
        dummyView = view.findViewById(R.id.dummy);
        searchBg = view.findViewById(R.id.searchBg);
        searchView = view.findViewById(R.id.searchView);
        searchView.setOnQueryTextFocusChangeListener((view14, b) -> {
            if (!b) return;
            searchBg.setVisibility(View.VISIBLE);
            ((MainActivity) getActivity()).enableBottomNavView(false);
            enableFloatingBtns(false);
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchQuery = query;
                updateData();
                searchBtn.setImageDrawable(MainActivity.getIconFromId(getActivity(), R.drawable.ic_baseline_search_off_24));
                closeSearch();
                dummyView.requestFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        searchBg.setOnClickListener(view12 -> closeSearch());

        // floating action buttons
        addExpBtn = view.findViewById(R.id.addExpBtn);
        addExpBtn.setOnClickListener(view1 -> ((MainActivity) getActivity()).addExpense());
        searchBtn = view.findViewById(R.id.searchBtn);
        searchBtn.setOnClickListener(view13 -> {
            if (searchView.getVisibility() == View.VISIBLE) {
                searchView.setQuery("", false);
                closeSearch();
            } else {
                searchBg.setVisibility(View.VISIBLE);
                searchView.setVisibility(View.VISIBLE);
                searchView.setIconified(false); // focus search edit
                searchView.setQuery(searchQuery, false);
                ((MainActivity) getActivity()).enableBottomNavView(false);
                enableFloatingBtns(false);
            }
        });

        // toolbar
        createOptionsMenu(view.findViewById(R.id.toolbar));

        // side menu
        context.setupMenuBtn(view.findViewById(R.id.menu_btn));

        return view;
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    /**
     * Menu
     */
    public void createOptionsMenu(Toolbar toolbar) {
        toolbar.inflateMenu(R.menu.home_menu);
        clearFilters = toolbar.getMenu().findItem(R.id.clearFilters);
        updateClearFiltersMenuItem();
        toolbar.setOnMenuItemClickListener(menuItemClickListener);
        setHasOptionsMenu(true);
    }
    public Toolbar.OnMenuItemClickListener menuItemClickListener = item -> {
        if (getActivity() == null)
            return false;
        int id = item.getItemId();
        if (id == R.id.filterAcc) {
            AccountAdapter accAdapter = ((MainActivity) getActivity()).getAccountData();
            accAdapter.setSelectionMode(true);
            for (Account acc : selAccFilters) {
                accAdapter.setSelected(acc.getName());
            }
            filterAccDialog(accAdapter);
            return true;
        }
        if (id == R.id.filterCat) {
            CategoryAdapter catAdapter = ((MainActivity) getActivity()).getCategoryData();
            catAdapter.setSelectionMode(true);
            for (Category cat : selCatFilters) {
                catAdapter.setSelected(cat.getName());
            }
            filterCatDialog(catAdapter);
            return true;
        }
        if (id == R.id.clearFilters) {
            selAccFilters.clear();
            selCatFilters.clear();
            applyFiltersViewItems();
            updateClearFiltersMenuItem();
            updateData(); // update summary & expense list
            return true;
        }
        return false;
    };

    /**
     * Search
     */
    public boolean isSearchOpen() {
        return (searchBg.getVisibility() == View.VISIBLE);
    }
    public void closeSearch() {
        searchBg.setVisibility(View.GONE);
        if (searchQuery.isEmpty() || searchView.getQuery().toString().isEmpty()) {
            if (!searchQuery.isEmpty()) {
                searchQuery = "";
                updateData();
            }
            searchView.setVisibility(View.GONE);
        } else {
            searchView.setQuery(searchQuery, false);
            MainActivity.hideKeyboard(getContext(), searchView);
        }
        if (searchQuery.isEmpty()) searchBtn.setImageDrawable(MainActivity.getIconFromId(getActivity(), R.drawable.ic_baseline_search_24));
        ((MainActivity) getActivity()).enableBottomNavView(true);
        enableFloatingBtns(true);
    }
    public void expandExpenseListLayout(boolean show) {
        int bottom = (show) ? (int) getResources().getDimension(R.dimen.bottom_nav_height) : 0;
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) expenseListLayout.getLayoutParams();
        params.setMargins(params.leftMargin, params.topMargin, params.rightMargin, bottom);
        expenseListLayout.setLayoutParams(params);
    }

    /**
     * Functions
     */
    public void summaryDateAction() {
        if (getActivity() == null)
            return;
        selDatePos = DateGridAdapter.MONTH;
        selDateState = DateGridAdapter.MONTH;
        if (fromDate == null) {
            fromDate = ((MainActivity) getActivity()).getInitSelectedDates(DateGridAdapter.FROM, selDateState);
            toDate = ((MainActivity) getActivity()).getInitSelectedDates(DateGridAdapter.TO, selDateState);
        }
        summaryDate.setOnClickListener(view -> {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity(), R.style.WrapContentDialog);
            final View filterDateView = getLayoutInflater().inflate(R.layout.dialog_date_grid, null);
            RecyclerView dateGrid = filterDateView.findViewById(R.id.dateGrid);

            // set RecyclerView behaviour and adapter
            ((SimpleItemAnimator) Objects.requireNonNull(dateGrid.getItemAnimator())).setSupportsChangeAnimations(false);
            GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity(), 2, GridLayoutManager.VERTICAL, false);
            filterDateAdapter = new DateGridAdapter(getActivity(), new int[] { selDatePos, selDateState }, fromDate, toDate);

            dateGrid.setLayoutManager(gridLayoutManager);
            dateGrid.setAdapter(filterDateAdapter);

            dialogBuilder.setView(filterDateView);
            AlertDialog dialog = dialogBuilder.create();
            filterDateAdapter.setParentDialog(dialog);
            dialog.show();

            // resize dialog to fit width
            dateGrid.measure( View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            int width = dateGrid.getMeasuredWidth();
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);

            dialog.setOnDismissListener(dialogInterface -> {
                filterDateAdapter.updateState();
                selDatePos = filterDateAdapter.getSelectedPos();
                selDateState = filterDateAdapter.getSelectedState();
                if (!filterDateAdapter.errorState) {
                    fromDate = (selDatePos == DateGridAdapter.ALL) ? null : filterDateAdapter.getSelDateRange()[0];
                    toDate = filterDateAdapter.getSelDateRange()[1];
                    selDatePos = filterDateAdapter.getSelectedPos();
                    selDateState = filterDateAdapter.getSelectedState();
                    updateData(); // update summary & expense list
                }

                if (selDatePos == DateGridAdapter.ALL) {
                    prevDate.setVisibility(ImageButton.GONE);
                    nextDate.setVisibility(ImageButton.GONE);
                } else {
                    prevDate.setVisibility(ImageButton.VISIBLE);
                    nextDate.setVisibility(ImageButton.VISIBLE);
                }
            });
        });
    }
    public void navDateAction(int direction) {
        if (getActivity() == null)
            return;
        if (selDatePos != DateGridAdapter.WEEK && selDatePos != DateGridAdapter.SELECT_RANGE)
            selDatePos = DateGridAdapter.SELECT_SINGLE;
        switch (selDateState) {
            case DateGridAdapter.DAY:
                if (selDatePos != DateGridAdapter.SELECT_SINGLE) direction *= 7;
                fromDate.set(Calendar.DAY_OF_YEAR, fromDate.get(Calendar.DAY_OF_YEAR) + direction);
                toDate.set(Calendar.DAY_OF_YEAR, toDate.get(Calendar.DAY_OF_YEAR) + direction);
                break;
            case DateGridAdapter.MONTH:
                fromDate.set(Calendar.MONTH, fromDate.get(Calendar.MONTH) + direction);
                toDate.set(toDate.get(Calendar.YEAR), toDate.get(Calendar.MONTH) + direction, 1);
                toDate.set(Calendar.DAY_OF_MONTH, toDate.getActualMaximum(Calendar.DATE));
                break;
            case DateGridAdapter.YEAR:
                fromDate.set(Calendar.YEAR, fromDate.get(Calendar.YEAR) + direction);
                toDate.set(Calendar.YEAR, toDate.get(Calendar.YEAR) + direction);
                break;
            default:
                fromDate.set(Calendar.DAY_OF_YEAR, fromDate.get(Calendar.DAY_OF_YEAR) + direction * 7);
                toDate.set(Calendar.DAY_OF_YEAR, toDate.get(Calendar.DAY_OF_YEAR) + direction * 7);
        }
        updateData(); // update summary & expense list
    }
    public void applyFiltersViewItems() {
        FilterAdapter filterAdapter = new FilterAdapter(getActivity(), selAccFilters, selCatFilters);
        FlexboxLayoutManager manager = new FlexboxLayoutManager(getActivity());
        manager.setFlexDirection(FlexDirection.ROW);
        manager.setJustifyContent(JustifyContent.FLEX_START);
        filterList.setLayoutManager(manager);
        filterList.setAdapter(filterAdapter);
        if (!selAccFilters.isEmpty()) {
            summaryCurr.setText(selAccFilters.get(0).getCurrencySymbol()); // get currency of first filter
        } else {
            String currencySymbol = ((MainActivity) getActivity()).getDefaultCurrencySymbol();
            summaryCurr.setText(currencySymbol); // default
        }
    }
    public void filterAccDialog(AccountAdapter adapter) {
        if (getActivity() == null)
            return;
        final View expOptSectionView = getLayoutInflater().inflate(R.layout.dialog_expense_opt_section, null);
        AlertDialog.Builder dialogBuilder = ((MainActivity) getActivity()).expenseSectionDialog(adapter, expOptSectionView);
        ((TextView) expOptSectionView.findViewById(R.id.expOptSectionTitle)).setText(getString(R.string.filter_dialog_title,getResources().getString(R.string.ACC)));
        dialogBuilder.setView(expOptSectionView);

        dialogBuilder.setPositiveButton(android.R.string.yes, ((dialogInterface, i) -> setSelAccFilters(adapter.getAllSelected())));
        dialogBuilder.setNeutralButton(android.R.string.no, (((dialog, i) -> dialog.cancel())));

        dialogBuilder.show();
    }
    public void filterCatDialog(CategoryAdapter adapter) {
        if (getActivity() == null)
            return;
        final View expOptSectionView = getLayoutInflater().inflate(R.layout.dialog_expense_opt_section, null);
        AlertDialog.Builder dialogBuilder = ((MainActivity) getActivity()).expenseSectionDialog(adapter, expOptSectionView);
        ((TextView) expOptSectionView.findViewById(R.id.expOptSectionTitle)).setText(getString(R.string.filter_dialog_title,getResources().getString(R.string.CAT)));
        dialogBuilder.setView(expOptSectionView);

        dialogBuilder.setPositiveButton(android.R.string.yes, ((dialogInterface, i) -> setSelCatFilters(adapter.getAllSelected())));
        dialogBuilder.setNeutralButton(android.R.string.no, (((dialog, i) -> dialog.cancel())));

        dialogBuilder.show();
    }
    public void updateClearFiltersMenuItem() {
        clearFilters.setVisible(!selAccFilters.isEmpty() || !selCatFilters.isEmpty());
    }
    public void updateDateRange() {
        if (getActivity() == null)
            return;
        fromDate = ((MainActivity) getActivity()).getInitSelectedDates(DateGridAdapter.FROM, selDateState);
        toDate = ((MainActivity) getActivity()).getInitSelectedDates(DateGridAdapter.TO, selDateState);
    }

    /**
     * Getters & Setters
     */
    public Calendar[] getDateRange() {
        return new Calendar[] { fromDate, toDate };
    }
    public int getSelDateState() {
        return selDateState;
    }
    public ArrayList<Account> getSelAccFilters() {
        return selAccFilters;
    }
    public ArrayList<Category> getSelCatFilters() {
        return selCatFilters;
    }
    public String getSearchQuery() {
        return searchQuery;
    }
    public String getSummaryAmt() { return summaryAmt.getText().toString(); }
    public TextView getSummaryDate() { return summaryDate; }

    public void updateData() {
        updateData(false);
    }
    public void updateData(boolean retainScrollPos) {
        int index, top;
        index = top = 0;
        if (retainScrollPos) { // save scroll position
            LinearLayoutManager linearLayoutManager = (LinearLayoutManager) expenseList.getLayoutManager();
            index = linearLayoutManager.findFirstVisibleItemPosition();
            View v = linearLayoutManager.getChildAt(0);
            top = (v == null) ? 0 : (v.getTop() - linearLayoutManager.getPaddingTop());
        }
        ArrayList<Expense> expenses = getExpenseList();
        expenses = MainActivity.insertExpDateHeaders(expenses);
        ExpenseAdapter expAdapter = new ExpenseAdapter(getActivity(), expenses);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        if (retainScrollPos)
            linearLayoutManager.scrollToPositionWithOffset(index, top);
        expenseList.setLayoutManager(linearLayoutManager);
        expenseList.setAdapter(expAdapter);
        if (expAdapter.getItemCount() > 0) placeholder.setVisibility(View.GONE);
        else placeholder.setVisibility(View.VISIBLE);
        ((MainActivity) getActivity()).updateSummaryData(Constants.HOME);
    }
    public ArrayList<Expense> getExpenseList() {
        ArrayList<Expense> expenses;
        Calendar from = getDateRange()[0];
        Calendar to = getDateRange()[1];
        if (getSelDateState() == DateGridAdapter.ALL)
            expenses = ((MainActivity) getActivity()).db.getSortedFilteredExpenses(getSelAccFilters(), getSelCatFilters(), Constants.DESCENDING, searchQuery);
        else
            expenses = ((MainActivity) getActivity()).db.getSortedFilteredExpensesInDateRange(getSelAccFilters(), getSelCatFilters(), from, to, Constants.DESCENDING, searchQuery);
        return expenses;
    }
    public void setSummaryData(String summaryDateText, float summaryAmtText) {
        summaryDate.setText(summaryDateText);
        summaryAmt.setText(String.format(MainActivity.locale, "%.2f", summaryAmtText));
    }
    public void setSummaryCurr(String currency) {
        summaryCurr.setText(currency);
    }
    public void setSelAccFilters(ArrayList<Account> selAccFilters) {
        setSelFilters(selAccFilters, null);
    }
    public void setSelCatFilters(ArrayList<Category> selCatFilters) {
        setSelFilters(null, selCatFilters);
    }
    public void setSelFilters(ArrayList<Account> selAccFilters, ArrayList<Category> selCatFilters) {
        if (selAccFilters != null) this.selAccFilters = selAccFilters;
        if (selCatFilters != null) this.selCatFilters = selCatFilters;
        applyFiltersViewItems();
        updateClearFiltersMenuItem();
        updateData();
    }
    public void setDateRange(Calendar[] range, int selDatePos, int selDateState) {
        this.fromDate = range[0];
        this.toDate = range[1];
        this.selDatePos = selDatePos;
        this.selDateState = selDateState;
    }

    /**
     * Others
     */
    public void enableFloatingBtns(boolean show) {
        int visibility = (show) ? View.VISIBLE : View.GONE;
        addExpBtn.setVisibility(visibility);
        searchBtn.setVisibility(visibility);
    }
}