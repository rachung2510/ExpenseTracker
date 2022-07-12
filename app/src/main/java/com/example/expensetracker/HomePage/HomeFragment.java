package com.example.expensetracker.HomePage;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.example.expensetracker.Account;
import com.example.expensetracker.Category;
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

import java.util.ArrayList;
import java.util.Calendar;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    // Layout components
    private RecyclerView expenseList;
    private TextView summaryDate, summaryAmt;
    private ImageButton prevDate, nextDate;
    private RecyclerView filterList;
    private MenuItem clearFilters;
    private FloatingActionButton fab;
    private Toolbar toolbar;

    // Filter components
    private DateGridAdapter filterDateAdapter;
    private Calendar fromDate, toDate;
    private int selDatePos, selDateState;
    private ArrayList<Account> selAccFilters = new ArrayList<>();
    private ArrayList<Category> selCatFilters = new ArrayList<>();

    // Constants
    public static final int PREV = -1;
    public static final int NEXT = 1;

    /**
     * MAIN
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // summary & expense list
        summaryDate = view.findViewById(R.id.summaryDate);
        summaryDateAction();
        summaryAmt = view.findViewById(R.id.summaryAmt);
        expenseList = view.findViewById(R.id.expenseList);
        ((SimpleItemAnimator) expenseList.getItemAnimator()).setSupportsChangeAnimations(false);

        // date navigation buttons
        prevDate = view.findViewById(R.id.prevDate);
        nextDate = view.findViewById(R.id.nextDate);
        prevDate.setOnClickListener((l) -> navDateAction(PREV));
        nextDate.setOnClickListener((l) -> navDateAction(NEXT));

        // update data
        ((MainActivity) getActivity()).updateHomeData(); // update summary & expense list

        // apply filters
        filterList = view.findViewById(R.id.accFilters);
        applyFilters();

        // floating action button
        fab = view.findViewById(R.id.floatingActionButton);
        fab.setOnClickListener(view1 -> ((MainActivity) getActivity()).addExpense());

        // toolbar
        toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle("");
        setHasOptionsMenu(true);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    /**
     * Menu
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.home_menu, menu);
        clearFilters = menu.findItem(R.id.clearFilters);
        updateClearFiltersItem();
        super.onCreateOptionsMenu(menu, inflater);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.filterAcc:
                AccountAdapter accAdapter = ((MainActivity) getActivity()).getAccountData();
                accAdapter.setSelectionMode(true);
                for (Account acc : selAccFilters) {
                    accAdapter.setSelected(acc.getName());
                }
                filterAccDialog(accAdapter);
                return true;

            case R.id.filterCat:
                CategoryAdapter catAdapter = ((MainActivity) getActivity()).getCategoryData();
                catAdapter.setSelectionMode(true);
                for (Category cat : selCatFilters) {
                    catAdapter.setSelected(cat.getName());
                }
                filterCatDialog(catAdapter);
                return true;

            case R.id.clearFilters:
                selAccFilters.clear();
                selCatFilters.clear();
                applyFilters();
                updateClearFiltersItem();
                ((MainActivity) getActivity()).updateHomeData(); // update summary & expense list
                return true;

            default:
                return false;
        }
    }

    /**
     * UPDATE
     */
    public void setExpenseData(LinearLayoutManager linearLayoutManager, ExpenseAdapter expAdapter) {
        expenseList.setLayoutManager(linearLayoutManager);
        expenseList.setAdapter(expAdapter);
    }
    public void setSummaryData(String summaryDateText, float summaryAmtText) {
        summaryDate.setText(summaryDateText);
        summaryAmt.setText(String.format("%.2f", summaryAmtText));
    }

    /**
     * FUNCTIONS
     */
    public void summaryDateAction() {
        selDatePos = DateGridAdapter.MONTH;
        selDateState = DateGridAdapter.MONTH;
        if (fromDate == null) {
            fromDate = DateGridAdapter.getInitSelectedDates(DateGridAdapter.FROM, selDateState);
            toDate = DateGridAdapter.getInitSelectedDates(DateGridAdapter.TO, selDateState);
        }
        summaryDate.setOnClickListener(view -> {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity(), R.style.WrapContentDialog);
            final View filterDateView = getLayoutInflater().inflate(R.layout.dialog_date, null);
            RecyclerView dateGrid = filterDateView.findViewById(R.id.dateGrid);

            // set RecyclerView behaviour and adapter
            ((SimpleItemAnimator) dateGrid.getItemAnimator()).setSupportsChangeAnimations(false);
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
                    fromDate = filterDateAdapter.getSelDateRange()[0];
                    toDate = filterDateAdapter.getSelDateRange()[1];
                    selDatePos = filterDateAdapter.getSelectedPos();
                    selDateState = filterDateAdapter.getSelectedState();
                    ((MainActivity) getActivity()).updateHomeData(); // update summary & expense list
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
        ((MainActivity) getActivity()).updateHomeData(); // update summary & expense list
    }
    public void applyFilters() {
        FilterAdapter filterAdapter = new FilterAdapter(getActivity(), selAccFilters, selCatFilters);
        FlexboxLayoutManager manager = new FlexboxLayoutManager(getActivity());
        manager.setFlexDirection(FlexDirection.ROW);
        manager.setJustifyContent(JustifyContent.FLEX_START);
//        ((SimpleItemAnimator) dateGrid.getItemAnimator()).setSupportsChangeAnimations(false);
        filterList.setLayoutManager(manager);
        filterList.setAdapter(filterAdapter);
    }
    public void filterAccDialog(AccountAdapter adapter) {
        final View expOptSectionView = getLayoutInflater().inflate(R.layout.dialog_expense_opt_section, null);
        AlertDialog.Builder dialogBuilder = ((MainActivity) getActivity()).expOptSectionDialog(adapter, expOptSectionView);
        ((TextView) expOptSectionView.findViewById(R.id.expOptSectionTitle)).setText("FILTER BY " + getResources().getString(R.string.acc_caps));
        dialogBuilder.setView(expOptSectionView);

        dialogBuilder.setPositiveButton(android.R.string.yes, ((dialogInterface, i) -> {
            selAccFilters = adapter.getAllSelected();
            applyFilters();
            updateClearFiltersItem();
            if (!selAccFilters.isEmpty() || !selCatFilters.isEmpty())
                ((MainActivity) getActivity()).updateHomeData(); // update summary & expense list
        }));
        dialogBuilder.setNeutralButton(android.R.string.no, (((dialog, i) -> dialog.cancel())));

        dialogBuilder.show();
    }
    public void filterCatDialog(CategoryAdapter adapter) {
        final View expOptSectionView = getLayoutInflater().inflate(R.layout.dialog_expense_opt_section, null);
        AlertDialog.Builder dialogBuilder = ((MainActivity) getActivity()).expOptSectionDialog(adapter, expOptSectionView);
        ((TextView) expOptSectionView.findViewById(R.id.expOptSectionTitle)).setText("FILTER BY " + getResources().getString(R.string.cat_caps));
        dialogBuilder.setView(expOptSectionView);

        dialogBuilder.setPositiveButton(android.R.string.yes, ((dialogInterface, i) -> {
            selCatFilters = adapter.getAllSelected();
            applyFilters();
            updateClearFiltersItem();
            if (!selAccFilters.isEmpty() || !selCatFilters.isEmpty())
                ((MainActivity) getActivity()).updateHomeData(); // update summary & expense list
        }));
        dialogBuilder.setNeutralButton(android.R.string.no, (((dialog, i) -> dialog.cancel())));

        dialogBuilder.show();
    }
    public void updateClearFiltersItem() {
        clearFilters.setVisible(!selAccFilters.isEmpty() || !selCatFilters.isEmpty());
    }

    /**
     * GETTERS & SETTERS
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

    public void setSelAccFilters(ArrayList<Account> selAccFilters) {
        this.selAccFilters = selAccFilters;
    }
    public void setSelCatFilters(ArrayList<Category> selCatFilters) {
        this.selCatFilters = selCatFilters;
    }
}