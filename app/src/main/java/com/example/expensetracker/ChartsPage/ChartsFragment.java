package com.example.expensetracker.ChartsPage;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import androidx.viewpager2.widget.ViewPager2;

import com.example.expensetracker.Constants;
import com.example.expensetracker.HomePage.HomeFragment;
import com.example.expensetracker.MainActivity;
import com.example.expensetracker.R;
import com.example.expensetracker.RecyclerViewAdapters.DateGridAdapter;
import com.example.expensetracker.RecyclerViewAdapters.ViewPagerAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Calendar;
import java.util.Objects;

public class ChartsFragment extends Fragment {

    private static final String TAG = "ChartsFragment";

    public String[] fragmentTitles = new String[] { "Categories", "Time", "Calendar" };

    // Main components
    private Calendar fromDate, toDate;
    private int selectedDatePos = DateGridAdapter.MONTH;
    private int selectedDateState = DateGridAdapter.MONTH;
    private int currentPage = ChartsChildFragment.TYPE_PIECHART;

    // View components
    private TextView summaryDate;
    private ImageButton prevDate, nextDate;
    private Toolbar toolbar;
    private int indicatorWidth;
    private TabLayout tabLayout;
    private View tabIndicator;
    private ViewPager2 viewPager;

    // Others
    private DateGridAdapter filterDateAdapter;

    public ChartsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Define all views
        View view = inflater.inflate(R.layout.fragment_charts, container, false);
        tabLayout = view.findViewById(R.id.chartsTab);
        tabIndicator = view.findViewById(R.id.chartsTabIndicator);
        viewPager = view.findViewById(R.id.viewPager);
        summaryDate = view.findViewById(R.id.summaryDate);
        prevDate = view.findViewById(R.id.prevDate);
        nextDate = view.findViewById(R.id.nextDate);
        prevDate.setOnClickListener((l) -> navDateAction(Constants.PREV));
        nextDate.setOnClickListener((l) -> navDateAction(Constants.NEXT));
        view.findViewById(R.id.summaryAmtBlk).setVisibility(LinearLayout.GONE);
        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (int) getResources().getDimension(R.dimen.actionBarSize));
        view.findViewById(R.id.toolbarContainer).setLayoutParams(params);
        toolbar = view.findViewById(R.id.toolbar);
        ImageButton menuBtn = view.findViewById(R.id.menu_btn);
        menuBtn.setVisibility(View.GONE);

        // Restore state if saved
        if (savedInstanceState != null) {
            Gson gson = new GsonBuilder().create();
            fromDate = gson.fromJson((String) savedInstanceState.get("fromDate"), Calendar.class);
            toDate = gson.fromJson((String) savedInstanceState.get("toDate"), Calendar.class);
            selectedDatePos = (int) savedInstanceState.get("selectedDatePos");
            selectedDateState = (int) savedInstanceState.get("selectedDateState");
            currentPage = (int) savedInstanceState.get("currentPage");
        }

        // summary
        setUpSummaryAction();

        // child fragments
        setUpChildFragments(savedInstanceState);
        tabLayout.post(() -> {
            FrameLayout.LayoutParams tabLayoutParams = (FrameLayout.LayoutParams) tabIndicator.getLayoutParams();
            tabLayoutParams.leftMargin = currentPage * indicatorWidth;
            tabIndicator.setLayoutParams(tabLayoutParams);
        });

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
        outState.putInt("currentPage", currentPage);
    }

    /**
     * Functions
     */
    public void updateData() {
        ChartsChildFragmentPie pieFrag = getChildFragmentPie();
        pieFrag.updateDateRange();
        pieFrag.updateCurrency();
        pieFrag.setPieChartTotalAmt(((HomeFragment) ((MainActivity) getActivity()).getFragment(Constants.HOME)).getSummaryAmt());
        if (getNumFragments() > 1) {
            ChartsChildFragmentGraph graphFrag = getChildFragmentGraph();
            graphFrag.updateDateRange();
            graphFrag.updateCurrency();
        }
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
            if (getChildFragmentManager().getFragments().size() > 1) {
                Fragment childFrag = getChildFragmentManager().getFragments().get(1);
                if (childFrag.isVisible())
                    filterDateAdapter.setDisabledPos(new String[] { "All time" });
            }

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
                    ((MainActivity) getActivity()).updateSummaryData(Constants.CHARTS); // update summary
                    ((MainActivity) getActivity()).updateDateRange(Constants.HOME, fromDate, toDate, selectedDatePos, selectedDateState);
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
    private void navDateAction(int direction) {
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
        ((MainActivity) getActivity()).updateSummaryData(Constants.CHARTS); // update summary
        ((MainActivity) getActivity()).updateDateRange(Constants.HOME, fromDate, toDate, selectedDatePos, selectedDateState);
    }
    public void setDateRange(Calendar from, Calendar to, int selectedDatePos, int selectedDateState) {
        // reset to default if selected state is ALL
        if (selectedDatePos == DateGridAdapter.ALL) {
            this.selectedDatePos = DateGridAdapter.MONTH;
            this.selectedDateState = DateGridAdapter.MONTH;
            this.fromDate = ((MainActivity) getActivity()).getInitSelectedDates(DateGridAdapter.FROM, this.selectedDateState);
            this.toDate = ((MainActivity) getActivity()).getInitSelectedDates(DateGridAdapter.TO, this.selectedDateState);
        } else {
            this.selectedDatePos = selectedDatePos;
            this.selectedDateState = selectedDateState;
            this.fromDate = from;
            this.toDate = to;
        }
    }
    private void setUpChildFragments(Bundle savedInstanceState) {
        // load fragments
        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        ChartsChildFragmentPie fragmentPie = new ChartsChildFragmentPie();
        ChartsChildFragmentGraph fragmentGraph = new ChartsChildFragmentGraph();
        if (savedInstanceState != null) {
            fragmentPie = (ChartsChildFragmentPie) getChildFragmentManager().getFragments().get(ChartsChildFragment.TYPE_PIECHART);
            fragmentGraph = (ChartsChildFragmentGraph) getChildFragmentManager().getFragments().get(ChartsChildFragment.TYPE_GRAPH);
        }
        adapter.addFragment(fragmentPie);
        adapter.addFragment(fragmentGraph);
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(2);

        // configure tab layout
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(fragmentTitles[position])
        ).attach();
        tabLayout.post(() -> {
            indicatorWidth = tabLayout.getWidth() / tabLayout.getTabCount();
            FrameLayout.LayoutParams indicatorParams = (FrameLayout.LayoutParams) tabIndicator.getLayoutParams();
            indicatorParams.width = indicatorWidth;
            tabIndicator.setLayoutParams(indicatorParams);
            tabLayout.getTabAt(currentPage).select();
        });

        // tab selection indicator behaviour
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) tabIndicator.getLayoutParams();
                float translationOffset = (positionOffset + position) * indicatorWidth;
                params.leftMargin = (int) translationOffset;
                tabIndicator.setLayoutParams(params);
            }

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                ((ChartsChildFragment) adapter.createFragment(position)).invalidateMenu();
                if (position == ChartsChildFragment.TYPE_GRAPH && selectedDateState == DateGridAdapter.ALL) {
                    if (getActivity() == null)
                        return;
                    selectedDatePos = DateGridAdapter.MONTH;
                    selectedDateState = DateGridAdapter.MONTH;
                    prevDate.setVisibility(ImageButton.VISIBLE);
                    nextDate.setVisibility(ImageButton.VISIBLE);
                    fromDate = ((MainActivity) getActivity()).getInitSelectedDates(DateGridAdapter.FROM, selectedDateState);
                    toDate = ((MainActivity) getActivity()).getInitSelectedDates(DateGridAdapter.TO, selectedDateState);
                    ((MainActivity) getActivity()).updateSummaryData(Constants.CHARTS); // update summary
                }
                currentPage = position;
            }
        });
    }

    /**
     * Getters & Setters
     */
    public ChartsChildFragmentPie getChildFragmentPie() {
        return (ChartsChildFragmentPie) getChildFragmentManager().getFragments().get(ChartsChildFragment.TYPE_PIECHART);
    }
    public ChartsChildFragmentGraph getChildFragmentGraph() {
        return (ChartsChildFragmentGraph) getChildFragmentManager().getFragments().get(ChartsChildFragment.TYPE_GRAPH);
    }
    public int getNumFragments() {
        return getChildFragmentManager().getFragments().size();
    }
    public Calendar[] getDateRange() {
        Calendar from = MainActivity.getCalendarCopy(fromDate, DateGridAdapter.FROM);
        Calendar to = MainActivity.getCalendarCopy(toDate, DateGridAdapter.TO);
        return new Calendar[] { from, to };
    }
    public int getSelectedDateState() {
        return selectedDateState;
    }
    public int getSelectedDatePos() {
        return selectedDatePos;
    }
    public Toolbar getToolbar() {
        return toolbar;
    }
    public void setSummaryData(String summaryDateText, float summaryAmt, boolean update) {
        summaryDate.setText(summaryDateText);
        ChartsChildFragmentPie pieChartFrag = getChildFragmentPie();
        pieChartFrag.setPieChartTotalAmt(summaryAmt);
        if (!update)
            return;
        pieChartFrag.updateDateRange();
        if (getChildFragmentManager().getFragments().size() > 1) {
            ChartsChildFragmentGraph lineChartFrag = getChildFragmentGraph();
            if (lineChartFrag.isInitialised()) lineChartFrag.updateDateRange();
        }
    }
    public void setFragmentHeight(int bottomMargin) {
        ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) viewPager.getLayoutParams();
        marginParams.setMargins(marginParams.leftMargin, marginParams.topMargin, marginParams.rightMargin, bottomMargin);
        viewPager.setLayoutParams(marginParams);
    }

}