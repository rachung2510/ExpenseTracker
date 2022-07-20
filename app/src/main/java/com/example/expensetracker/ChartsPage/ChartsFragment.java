package com.example.expensetracker.ChartsPage;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import androidx.viewpager2.widget.ViewPager2;

import com.example.expensetracker.Constants;
import com.example.expensetracker.MainActivity;
import com.example.expensetracker.R;
import com.example.expensetracker.RecyclerViewAdapters.DateGridAdapter;
import com.example.expensetracker.RecyclerViewAdapters.ViewPagerAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.Calendar;
import java.util.Objects;

public class ChartsFragment extends Fragment {

    private static final String TAG = "ChartsFragment";
    private int indicatorWidth;
    public String[] fragmentTitles = new String[] { "Categories", "Time", "Calendar" };
    private TextView summaryDate;
    private ImageButton prevDate, nextDate;

    // Filter components
    private DateGridAdapter filterDateAdapter;
    private Calendar fromDate, toDate;
    private int selDatePos, selDateState;

    public ChartsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_charts, container, false);
        TabLayout tabLayout = view.findViewById(R.id.chartsTab);
        View tabIndicator = view.findViewById(R.id.chartsTabIndicator);
        ViewPager2 viewPager2 = view.findViewById(R.id.viewPager);

        // summary
        summaryDate = view.findViewById(R.id.summaryDate);
        summaryDateAction();
        prevDate = view.findViewById(R.id.prevDate);
        nextDate = view.findViewById(R.id.nextDate);
        prevDate.setOnClickListener((l) -> navDateAction(Constants.PREV));
        nextDate.setOnClickListener((l) -> navDateAction(Constants.NEXT));

        // toolbar
        view.findViewById(R.id.summaryAmtBlk).setVisibility(LinearLayout.GONE);
        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (int) getResources().getDimension(R.dimen.actionBarSize));
        view.findViewById(R.id.toolbarContainer).setLayoutParams(params);

        // load fragments
        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        adapter.addFragment(new ChartsChildFragment(ChartsChildFragment.TYPE_PIECHART));
        adapter.addFragment(new ChartsChildFragment(ChartsChildFragment.TYPE_GRAPH));
//        adapter.addFragment(new ChartsChildFragment(ChartsChildFragment.TYPE_CALENDAR));
        viewPager2.setAdapter(adapter);

        // configure tab layout
        new TabLayoutMediator(tabLayout, viewPager2,
                (tab, position) -> tab.setText(fragmentTitles[position])
        ).attach();
        tabLayout.post(() -> {
            indicatorWidth = tabLayout.getWidth() / tabLayout.getTabCount();
            FrameLayout.LayoutParams indicatorParams = (FrameLayout.LayoutParams) tabIndicator.getLayoutParams();
            indicatorParams.width = indicatorWidth;
            tabIndicator.setLayoutParams(indicatorParams);
        });

        // tab selection indicator behaviour
        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) tabIndicator.getLayoutParams();
                float translationOffset = (positionOffset + position) * indicatorWidth;
                params.leftMargin = (int) translationOffset;
                tabIndicator.setLayoutParams(params);
                if (position == 1 && selDateState == DateGridAdapter.ALL) {
                    if (getActivity() == null)
                        return;
                    selDatePos = DateGridAdapter.MONTH;
                    selDateState = DateGridAdapter.MONTH;
                    prevDate.setVisibility(ImageButton.VISIBLE);
                    nextDate.setVisibility(ImageButton.VISIBLE);
                    fromDate = ((MainActivity) getActivity()).getInitSelectedDates(DateGridAdapter.FROM, selDateState);
                    toDate = ((MainActivity) getActivity()).getInitSelectedDates(DateGridAdapter.TO, selDateState);
                    ((MainActivity) getActivity()).updateSummaryData(((MainActivity) getActivity()).getExpenseList()); // update summary
                }
            }
        });

        // toolbar
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        if (getActivity() == null)
            return view;
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        Objects.requireNonNull(((AppCompatActivity) getActivity()).getSupportActionBar()).setTitle("");

        ImageButton menuBtn = view.findViewById(R.id.menu_btn);
        menuBtn.setVisibility(View.GONE);

        return view;
    }

    public void summaryDateAction() {
        selDatePos = DateGridAdapter.MONTH;
        selDateState = DateGridAdapter.MONTH;
        if (getActivity() == null)
            return;
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
                selDatePos = filterDateAdapter.getSelectedPos();
                selDateState = filterDateAdapter.getSelectedState();
                if (!filterDateAdapter.errorState) {
                    fromDate = (selDatePos == DateGridAdapter.ALL) ? null : filterDateAdapter.getSelDateRange()[0];
                    toDate = filterDateAdapter.getSelDateRange()[1];
                    selDatePos = filterDateAdapter.getSelectedPos();
                    selDateState = filterDateAdapter.getSelectedState();
                    ((MainActivity) getActivity()).updateSummaryData(((MainActivity) getActivity()).getExpenseList()); // update summary
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
        ((MainActivity) getActivity()).updateSummaryData(((MainActivity) getActivity()).getExpenseList()); // update summary
    }
    public Calendar[] getDateRange() {
        return new Calendar[] { fromDate, toDate };
    }
    public int getSelDateState() {
        return selDateState;
    }
    public int getSelDatePos() {
        return selDatePos;
    }
    public void setSummaryData(String summaryDateText, float summaryAmt) {
        summaryDate.setText(summaryDateText);
        ChartsChildFragment pieChartFrag = (ChartsChildFragment) getChildFragmentManager().getFragments().get(0);
        pieChartFrag.setPieChartTotalAmt(summaryAmt);
        pieChartFrag.updateDateFilters();
        if (getChildFragmentManager().getFragments().size() > 1) {
            ChartsChildFragment lineChartFrag = (ChartsChildFragment) getChildFragmentManager().getFragments().get(1);
            lineChartFrag.updateDateFilters();
        }
    }

}