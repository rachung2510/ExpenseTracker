package com.example.expensetracker.HomePage;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.example.expensetracker.Account;
import com.example.expensetracker.Category;
import com.example.expensetracker.ChartsPage.ChartsFragment;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Objects;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    // Components to save
    private Calendar fromDate, toDate;
    private int selectedDatePos = DateGridAdapter.MONTH;
    private int selectedDateState = DateGridAdapter.MONTH;
    private ArrayList<Account> accFilters = new ArrayList<>();
    private ArrayList<Category> catFilters = new ArrayList<>();
    private String searchQuery = "";

    // View components
    private RecyclerView expenseList;
    public TextView placeholder, summaryDate, summaryAmt, summaryCurr;
    private ImageButton prevDate, nextDate;
    private RecyclerView filterList;
    private MenuItem clearFilters;
    private LinearLayout expenseListLayout, searchBg;
    private SearchView searchView;
    private FloatingActionButton addExpBtn, searchBtn;
    private View dummyView;

    // Others
    private DateGridAdapter filterDateAdapter;

    /**
     * Main
     */
    @SuppressWarnings("unchecked")
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (getActivity() == null)
            return null;

        // Define all views
        MainActivity context = (MainActivity) getActivity();
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        summaryAmt = view.findViewById(R.id.summaryAmt);
        summaryCurr = view.findViewById(R.id.summaryCurrency);
        summaryDate = view.findViewById(R.id.summaryDate);
        expenseList = view.findViewById(R.id.expenseList);
        expenseListLayout = view.findViewById(R.id.homeFragLinearLayout);
        ((SimpleItemAnimator) Objects.requireNonNull(expenseList.getItemAnimator())).setSupportsChangeAnimations(false);
        filterList = view.findViewById(R.id.sectionFilters);
        placeholder = view.findViewById(R.id.placeholder);
        prevDate = view.findViewById(R.id.prevDate);
        nextDate = view.findViewById(R.id.nextDate);
        prevDate.setOnClickListener((l) -> navDateOnClick(Constants.PREV));
        nextDate.setOnClickListener((l) -> navDateOnClick(Constants.NEXT));
        searchBtn = view.findViewById(R.id.searchBtn);
        searchBg = view.findViewById(R.id.searchBg);
        searchView = view.findViewById(R.id.searchView);
        dummyView = view.findViewById(R.id.dummy);
        addExpBtn = view.findViewById(R.id.addExpBtn);

        // Restore state if saved
        if (savedInstanceState != null) {
            Gson gson = new GsonBuilder().create();
            fromDate = gson.fromJson((String) savedInstanceState.get("fromDate"), Calendar.class);
            toDate = gson.fromJson((String) savedInstanceState.get("toDate"), Calendar.class);
            selectedDatePos = (int) savedInstanceState.get("selectedDatePos");
            selectedDateState = (int) savedInstanceState.get("selectedDateState");
            accFilters = context.getFilterAccounts((ArrayList<Integer>) savedInstanceState.get("accFilters"));
            catFilters = context.getFilterCategories((ArrayList<Integer>) savedInstanceState.get("catFilters"));
            searchQuery = (String) savedInstanceState.get("searchQuery");
            if (!searchQuery.isEmpty()) searchView.setVisibility(View.VISIBLE);
        }

        // summary
        setUpSummaryAction();
        if (selectedDateState == DateGridAdapter.ALL) {
            prevDate.setVisibility(View.GONE);
            nextDate.setVisibility(View.GONE);
        }

        // apply filters
        setUpFilters();

        // search
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
                toggleFloatingButtons(false);
            }
        });
        searchView.setOnQueryTextFocusChangeListener((view14, b) -> {
            if (!b) return;
            searchBg.setVisibility(View.VISIBLE);
            ((MainActivity) getActivity()).enableBottomNavView(false);
            toggleFloatingButtons(false);
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

        // add expense
        addExpBtn.setOnClickListener(view1 -> ((MainActivity) getActivity()).addExpense());

        // toolbar
        createOptionsMenu(view.findViewById(R.id.toolbar));

        // side menu
        context.setupMenuBtn(view.findViewById(R.id.menu_btn));

        // update data
        updateData(); // update summary & expense list

        return view;
    }
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Gson gson = new GsonBuilder().create();
        outState.putString("fromDate", gson.toJson(fromDate));
        outState.putString("toDate", gson.toJson(toDate));
        outState.putInt("selectedDatePos", selectedDatePos);
        outState.putInt("selectedDateState", selectedDateState);
        outState.putIntegerArrayList("accFilters", ((MainActivity) getActivity()).getFilterIds(accFilters));
        outState.putIntegerArrayList("catFilters", ((MainActivity) getActivity()).getFilterIds(catFilters));
        outState.putString("searchQuery", searchQuery);
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
            for (Account acc : accFilters) {
                accAdapter.setSelected(acc.getName());
            }
            filterAccDialog(accAdapter);
            return true;
        }
        if (id == R.id.filterCat) {
            CategoryAdapter catAdapter = ((MainActivity) getActivity()).getCategoryData();
            catAdapter.setSelectionMode(true);
            for (Category cat : catFilters) {
                catAdapter.setSelected(cat.getName());
            }
            filterCatDialog(catAdapter);
            return true;
        }
        if (id == R.id.clearFilters) {
            accFilters.clear();
            catFilters.clear();
            setUpFilters();
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
        toggleFloatingButtons(true);
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
        if (retainScrollPos) // restore scroll position
            linearLayoutManager.scrollToPositionWithOffset(index, top);
        expenseList.setLayoutManager(linearLayoutManager);
        expenseList.setAdapter(expAdapter);
        if (expAdapter.getItemCount() > 0) placeholder.setVisibility(View.GONE);
        else placeholder.setVisibility(View.VISIBLE);
        ((MainActivity) getActivity()).updateSummaryData(Constants.HOME);
    }
    public void updateDateRangeFromState() {
        if (getActivity() == null)
            return;
        fromDate = ((MainActivity) getActivity()).getInitSelectedDates(DateGridAdapter.FROM, selectedDateState);
        toDate = ((MainActivity) getActivity()).getInitSelectedDates(DateGridAdapter.TO, selectedDateState);
    }
    private void setUpSummaryAction() {
        if (getActivity() == null)
            return;
        if (fromDate == null) {
            fromDate = ((MainActivity) getActivity()).getInitSelectedDates(DateGridAdapter.FROM, selectedDateState);
            toDate = ((MainActivity) getActivity()).getInitSelectedDates(DateGridAdapter.TO, selectedDateState);
        }
        summaryDate.setOnClickListener(view -> {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity(), R.style.WrapContentDialog);
            final View filterDateView = getLayoutInflater().inflate(R.layout.dialog_date_grid, null);
            RecyclerView dateGrid = filterDateView.findViewById(R.id.dateGrid);

            // set RecyclerView behaviour and adapter
            ((SimpleItemAnimator) Objects.requireNonNull(dateGrid.getItemAnimator())).setSupportsChangeAnimations(false);
            GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity(), 2, GridLayoutManager.VERTICAL, false);
            filterDateAdapter = new DateGridAdapter(getActivity(), new int[] {selectedDatePos, selectedDateState}, fromDate, toDate);

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
                selectedDatePos = filterDateAdapter.getSelectedPos();
                selectedDateState = filterDateAdapter.getSelectedState();
                if (!filterDateAdapter.errorState) {
                    fromDate = (selectedDatePos == DateGridAdapter.ALL) ? null : filterDateAdapter.getSelDateRange()[0];
                    toDate = filterDateAdapter.getSelDateRange()[1];
                    selectedDatePos = filterDateAdapter.getSelectedPos();
                    selectedDateState = filterDateAdapter.getSelectedState();
                    updateData(); // update summary & expense list
                    ((MainActivity) getActivity()).updateDateRange(Constants.CHARTS, fromDate, toDate, selectedDatePos, selectedDateState);
                }

                if (selectedDatePos == DateGridAdapter.ALL) {
                    prevDate.setVisibility(ImageButton.GONE);
                    nextDate.setVisibility(ImageButton.GONE);
                } else {
                    prevDate.setVisibility(ImageButton.VISIBLE);
                    nextDate.setVisibility(ImageButton.VISIBLE);
                }
            });
        });
    }
    private void navDateOnClick(int direction) {
        if (getActivity() == null)
            return;
        if (selectedDatePos != DateGridAdapter.WEEK && selectedDatePos != DateGridAdapter.SELECT_RANGE)
            selectedDatePos = DateGridAdapter.SELECT_SINGLE;
        switch (selectedDateState) {
            case DateGridAdapter.DAY:
                if (selectedDatePos != DateGridAdapter.SELECT_SINGLE) direction *= 7;
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
        ((MainActivity) getActivity()).updateDateRange(Constants.CHARTS, fromDate, toDate, selectedDatePos, selectedDateState);
    }
    public void setDateRange(Calendar from, Calendar to, int selectedDatePos, int selectedDateState) {
        this.selectedDatePos = selectedDatePos;
        this.selectedDateState = selectedDateState;
        // bring back navDate buttons if previously invisible
        prevDate.setVisibility(ImageButton.VISIBLE);
        nextDate.setVisibility(ImageButton.VISIBLE);
        this.fromDate = from;
        this.toDate = to;
    }
    private void setUpFilters() {
        FilterAdapter filterAdapter = new FilterAdapter(getActivity(), accFilters, catFilters);
        FlexboxLayoutManager manager = new FlexboxLayoutManager(getActivity());
        manager.setFlexDirection(FlexDirection.ROW);
        manager.setJustifyContent(JustifyContent.FLEX_START);
        filterList.setLayoutManager(manager);
        filterList.setAdapter(filterAdapter);
        String currencySymbol = ((MainActivity) getActivity()).getDefaultCurrencySymbol();
        setSummaryCurr(currencySymbol);
    }
    private void filterAccDialog(AccountAdapter adapter) {
        if (getActivity() == null)
            return;
        final View expOptSectionView = getLayoutInflater().inflate(R.layout.dialog_expense_opt_section, null);
        AlertDialog.Builder dialogBuilder = ((MainActivity) getActivity()).expenseSectionDialog(adapter, expOptSectionView);
        ((TextView) expOptSectionView.findViewById(R.id.expOptSectionTitle)).setText(getString(R.string.filter_dialog_title,getResources().getString(R.string.ACC)));
        dialogBuilder.setView(expOptSectionView);

        dialogBuilder.setPositiveButton(android.R.string.yes, ((dialogInterface, i) -> setAccFilters(adapter.getAllSelected())));
        dialogBuilder.setNeutralButton(android.R.string.no, (((dialog, i) -> dialog.cancel())));

        dialogBuilder.show();
    }
    private void filterCatDialog(CategoryAdapter adapter) {
        if (getActivity() == null)
            return;
        final View expOptSectionView = getLayoutInflater().inflate(R.layout.dialog_expense_opt_section, null);
        AlertDialog.Builder dialogBuilder = ((MainActivity) getActivity()).expenseSectionDialog(adapter, expOptSectionView);
        ((TextView) expOptSectionView.findViewById(R.id.expOptSectionTitle)).setText(getString(R.string.filter_dialog_title,getResources().getString(R.string.CAT)));
        dialogBuilder.setView(expOptSectionView);

        dialogBuilder.setPositiveButton(android.R.string.yes, ((dialogInterface, i) -> setCatFilters(adapter.getAllSelected())));
        dialogBuilder.setNeutralButton(android.R.string.no, (((dialog, i) -> dialog.cancel())));

        dialogBuilder.show();
    }
    private void updateClearFiltersMenuItem() {
        clearFilters.setVisible(!accFilters.isEmpty() || !catFilters.isEmpty());
    }

    /**
     * Getters & Setters
     */
    public ArrayList<Expense> getExpenseList() {
        ArrayList<Expense> expenses;
        Calendar from = getDateRange()[0];
        Calendar to = getDateRange()[1];
        if (getSelDateState() == DateGridAdapter.ALL)
            expenses = ((MainActivity) getActivity()).db.getSortedFilteredExpenses(getAccFilters(), getCatFilters(), Constants.DESCENDING, searchQuery);
        else
            expenses = ((MainActivity) getActivity()).db.getSortedFilteredExpensesInDateRange(getAccFilters(), getCatFilters(), from, to, Constants.DESCENDING, searchQuery);
        return expenses;
    }
    public Calendar[] getDateRange() {
        return new Calendar[] { fromDate, toDate };
    }
    public int getSelDateState() {
        return selectedDateState;
    }
    public ArrayList<Account> getAccFilters() {
        return accFilters;
    }
    public ArrayList<Category> getCatFilters() {
        return catFilters;
    }
    public String getSearchQuery() {
        return searchQuery;
    }
    public String getSummaryAmt() { return summaryAmt.getText().toString(); }

    public void setSummaryData(String summaryDateText, float summaryAmtText) {
        summaryDate.setText(summaryDateText);
        summaryAmt.setText(String.format(MainActivity.locale, "%.2f", summaryAmtText));
    }
    public void setSummaryCurr(String currency) {
        summaryCurr.setText(currency);
    }
    public void setDateRange(Calendar[] range, int selDatePos, int selDateState) {
        this.fromDate = range[0];
        this.toDate = range[1];
        this.selectedDatePos = selDatePos;
        this.selectedDateState = selDateState;
    }
    public void setAccFilters(ArrayList<Account> accFilters) {
        setSelFilters(accFilters, null);
    }
    public void setCatFilters(ArrayList<Category> catFilters) {
        setSelFilters(null, catFilters);
    }
    public void setSelFilters(ArrayList<Account> selAccFilters, ArrayList<Category> selCatFilters) {
        if (selAccFilters != null) this.accFilters = selAccFilters;
        if (selCatFilters != null) this.catFilters = selCatFilters;
        setUpFilters();
        updateClearFiltersMenuItem();
        updateData();
    }

    /**
     * Others
     */
    public void toggleFloatingButtons(boolean show) {
        int visibility = (show) ? View.VISIBLE : View.GONE;
        addExpBtn.setVisibility(visibility);
        searchBtn.setVisibility(visibility);
    }

}