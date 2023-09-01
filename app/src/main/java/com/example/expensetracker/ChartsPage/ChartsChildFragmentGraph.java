package com.example.expensetracker.ChartsPage;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensetracker.Account;
import com.example.expensetracker.Category;
import com.example.expensetracker.Constants;
import com.example.expensetracker.Expense;
import com.example.expensetracker.HelperClasses.CustomMarkerView;
import com.example.expensetracker.HelperClasses.CustomXAxisRenderer;
import com.example.expensetracker.HomePage.HomeFragment;
import com.example.expensetracker.MainActivity;
import com.example.expensetracker.R;
import com.example.expensetracker.RecyclerViewAdapters.AccountAdapter;
import com.example.expensetracker.RecyclerViewAdapters.CategoryAdapter;
import com.example.expensetracker.RecyclerViewAdapters.DateGridAdapter;
import com.example.expensetracker.RecyclerViewAdapters.ExpenseAdapter;
import com.example.expensetracker.RecyclerViewAdapters.FilterAdapter;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.IMarker;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.MPPointD;
import com.github.mikephil.charting.utils.Transformer;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.JustifyContent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class ChartsChildFragmentGraph extends ChartsChildFragment {

    private static final String TAG = "ChartsChildFragmentGraph";

    // Main components
    private float totalAmt = 0;
    private Calendar fromDate, toDate;
    private int selDateState;
    private final ArrayList<String> dates = new ArrayList<>();
    private ArrayList<Account> accFilters = new ArrayList<>();
    private ArrayList<Category> catFilters = new ArrayList<>();
    private Pair<Float,Float> highlightEntry;
    private String highlightDate;

    // View components
    private LineChart lineChart;
    private TextView lineAmt, lineDate, avgDay, avgWeek, avgMonth, placeholder, link;
    private RecyclerView expenseList, filterList;
    private MenuItem clearFiltersMenuOption;

    // Others
    private boolean isHighlighted = false;
    private boolean isSelRange = false;
    private boolean isInitialised = false;
    private int numUnits = 1;
    private static final float granDivisor = 10f;

    public ChartsChildFragmentGraph() {
        super();
        this.chartType = ChartsChildFragment.TYPE_GRAPH;
    }
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (getActivity() == null || getParentFragment() == null)
            return null;

        isInitialised = true;

        // Define all views
        view = inflater.inflate(R.layout.fragment_charts_graph, container, false);
        lineChart = view.findViewById(R.id.lineChart);
        lineDate = view.findViewById(R.id.summaryDate);
        lineAmt = view.findViewById(R.id.summaryAmt);
        updateCurrency(((MainActivity) getActivity()).getDefaultCurrencySymbol());
        LinearLayout summaryAmtBlk = view.findViewById(R.id.summaryAmtBlk);
        summaryAmtBlk.setOnClickListener(view1 -> removeHighlight());
        LinearLayout statistics = view.findViewById(R.id.statistics);
        statistics.setOnClickListener(view1 -> removeHighlight());
        expenseList = view.findViewById(R.id.expenseList);
        avgDay = view.findViewById(R.id.dayAvg);
        avgWeek = view.findViewById(R.id.weekAvg);
        avgMonth = view.findViewById(R.id.monthAvg);
        placeholder = view.findViewById(R.id.placeholder);
        link = view.findViewById(R.id.link);


        link.setOnClickListener(view -> {
            HomeFragment homeFrag = (HomeFragment) ((MainActivity) getActivity()).getFragment(Constants.HOME);
            ChartsFragment chartsFrag = (ChartsFragment) ((MainActivity) getActivity()).getFragment(Constants.CHARTS);
            homeFrag.setSelFilters(MainActivity.clone(accFilters), MainActivity.clone(catFilters));
            homeFrag.setDateRange(new Calendar[] {fromDate, toDate}, chartsFrag.getSelectedDatePos(), getActualSelDateState());
            ((MainActivity) getActivity()).updateHomeData();
            ((MainActivity) getActivity()).goToFragment(Constants.HOME);
        });

        setupLineChart();
        configLineChartSelection();
        configLineChartScale();
        configLineChartRecyclerView();
        updateDateRange();

        setHasOptionsMenu(true);
        filterList = view.findViewById(R.id.sectionFilters);

        // to hide error msg "No adapter attached; skipping layout"
        filterList.setAdapter((new FilterAdapter(getActivity(), new ArrayList<>(), new ArrayList<>())));
        FlexboxLayoutManager manager = new FlexboxLayoutManager(getActivity());
        manager.setFlexDirection(FlexDirection.ROW);
        manager.setJustifyContent(JustifyContent.FLEX_START);
        filterList.setLayoutManager(manager);

        if (savedInstanceState != null) {
            Gson gson = new GsonBuilder().create();
            Type type = new TypeToken<Pair<Float,Float>>(){}.getType();
            highlightEntry = gson.fromJson((String) savedInstanceState.get("highlightEntry"), type);
            if (highlightEntry == null) return view;
            highlightDate = (String) savedInstanceState.get("highlightDate");
            lineChart.highlightValue(highlightEntry.first, highlightEntry.second, 0);
            setHighlight();
        }

        return view;
    }
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Gson gson = new GsonBuilder().create();
        outState.putString("highlightEntry", gson.toJson(highlightEntry));
        outState.putString("highlightDate", highlightDate);
    }

    @Override
    protected void invalidateMenu() {
        if (getParentFragment() == null) return;
        Toolbar toolbar = ((ChartsFragment) getParentFragment()).getToolbar();
        createOptionsMenu(toolbar);
    }
    @Override
    protected void updateDateRange() {
        Calendar from = Calendar.getInstance();
        Calendar to = Calendar.getInstance();
        if (getParentFragment() != null) {
            from = ((ChartsFragment) getParentFragment()).getDateRange()[0];
            to = ((ChartsFragment) getParentFragment()).getDateRange()[1];
        }
        updateSelDateState();
        
        fromDate = MainActivity.getCalendarCopy(from, DateGridAdapter.FROM);
        toDate = MainActivity.getCalendarCopy(to, DateGridAdapter.TO);
        Calendar old = MainActivity.getCalendarCopy(fromDate, DateGridAdapter.FROM);
        fromDate = updateDateRangeForDay(fromDate, DateGridAdapter.FROM);
        toDate = updateDateRangeForDay(toDate, DateGridAdapter.TO);

        loadLineChartData();
        updateExpenseList();
        updateLineChartSummary();
        updateAverages();
        lineChart.fitScreen();
        lineChart.highlightValues(null);
        highlightLineAmt(false);
        if (selDateState == DateGridAdapter.DAY) {
            lineChart.highlightValue((float) dates.indexOf(MainActivity.getDatetimeStr(old, getDtf())), 0);
            highlightLineAmt(true);
        }
    }
    @Override
    protected void updateCurrency(String curr) {
        ((TextView) view.findViewById(R.id.summaryCurrency)).setText(curr);
        ((TextView) view.findViewById(R.id.curr1)).setText(curr);
        ((TextView) view.findViewById(R.id.curr2)).setText(curr);
        ((TextView) view.findViewById(R.id.curr3)).setText(curr);
    }

    /**
     * Menu
     */
    private void createOptionsMenu(Toolbar toolbar) {
        toolbar.inflateMenu(R.menu.home_menu);
        clearFiltersMenuOption = toolbar.getMenu().findItem(R.id.clearFilters);
        updateClearFiltersMenuItem();
        toolbar.setOnMenuItemClickListener(menuItemClickListener);
    }
    private final Toolbar.OnMenuItemClickListener menuItemClickListener = item -> {
        int id = item.getItemId();
        if (getActivity() == null)
            return false;
        if (id == R.id.filterAcc) {
            AccountAdapter accAdapter = ((MainActivity) getActivity()).getAccountData();
            accAdapter.setSelectionMode(true);
            for (Account acc : accFilters)
                accAdapter.setSelected(acc.getName());
            filterAccDialog(accAdapter);
            return true;
        }
        if (id == R.id.filterCat) {
            CategoryAdapter catAdapter = ((MainActivity) getActivity()).getCategoryData();
            catAdapter.setSelectionMode(true);
            for (Category cat : catFilters)
                catAdapter.setSelected(cat.getName());
            filterCatDialog(catAdapter);
            return true;
        }
        if (id == R.id.clearFilters) {
            accFilters.clear();
            catFilters.clear();
            applyFilters(true);
            return true;
        }
        return false;
    };
    public void applyFilters(boolean isDelete) {
        if (accFilters.isEmpty() && catFilters.isEmpty() && !isDelete)
            return;
        filterList.setAdapter(new FilterAdapter(getActivity(), accFilters, catFilters));
        if (!accFilters.isEmpty())
            updateCurrency(accFilters.get(0).getCurrencySymbol());
        else
            updateCurrency();
        if (clearFiltersMenuOption != null) // on view created
            updateClearFiltersMenuItem();
        updateDateRange();
    }
    private void filterAccDialog(AccountAdapter adapter) {
        if (getActivity() == null)
            return;
        final View view = getLayoutInflater().inflate(R.layout.dialog_expense_opt_section, null);
        AlertDialog.Builder dialogBuilder = ((MainActivity) getActivity()).expenseSectionDialog(adapter, view);
        ((TextView) view.findViewById(R.id.expOptSectionTitle)).setText(getResources().getString(R.string.filter_dialog_title, getActivity().getString(R.string.ACC)));
        dialogBuilder.setView(view);
        dialogBuilder.setPositiveButton(android.R.string.yes, ((dialogInterface, i) -> setAccFilters(adapter.getAllSelected(), true)));
        dialogBuilder.setNeutralButton(android.R.string.no, (((dialog, i) -> dialog.cancel())));
        dialogBuilder.show();
    }
    private void filterCatDialog(CategoryAdapter adapter) {
        if (getActivity() == null)
            return;
        final View expOptSectionView = getLayoutInflater().inflate(R.layout.dialog_expense_opt_section, null);
        AlertDialog.Builder dialogBuilder = ((MainActivity) getActivity()).expenseSectionDialog(adapter, expOptSectionView);
        ((TextView) expOptSectionView.findViewById(R.id.expOptSectionTitle)).setText(getResources().getString(R.string.filter_dialog_title, getResources().getString(R.string.CAT)));
        dialogBuilder.setView(expOptSectionView);
        dialogBuilder.setPositiveButton(android.R.string.yes, ((dialogInterface, i) -> setCatFilters(adapter.getAllSelected(), true)));
        dialogBuilder.setNeutralButton(android.R.string.no, (((dialog, i) -> dialog.cancel())));
        dialogBuilder.show();
    }
    private void updateClearFiltersMenuItem() {
        clearFiltersMenuOption.setVisible(!accFilters.isEmpty() || !catFilters.isEmpty());
    }
    public void setAccFilters(ArrayList<Account> accFilters, boolean isDelete) {
        this.accFilters = accFilters;
        applyFilters(isDelete);
    }
    public void setCatFilters(ArrayList<Category> catFilters, boolean isDelete) {
        this.catFilters = catFilters;
        applyFilters(isDelete);
    }

    /**
     * Linechart functions
     */
    @SuppressWarnings("ConstantConditions")
    private void loadLineChartData() {
        if (getActivity() == null)
            return;
        
        ArrayList<Entry> values = new ArrayList<>();
        totalAmt = 0; // reset to 0

        String dateFormat;
        int range;
        LocalDate fromDate = LocalDateTime.ofInstant(this.fromDate.toInstant(), this.fromDate.getTimeZone().toZoneId()).toLocalDate();
        LocalDate toDate = LocalDateTime.ofInstant(this.toDate.toInstant(), this.toDate.getTimeZone().toZoneId()).toLocalDate();
        if (selDateState == DateGridAdapter.YEAR) { // by months
            dateFormat = "%Y-%m";
            range = Calendar.MONTH;
            numUnits = isSelRange ? (int) ChronoUnit.MONTHS.between(fromDate, toDate) + 1 : 12;
        } else { // by day
            dateFormat = "%Y-%m-%d";
            range = Calendar.DATE;
            if (selDateState <= DateGridAdapter.WEEK) // range is a week
                numUnits = 7;
            else // range is a month
                numUnits = (int) (ChronoUnit.DAYS.between(fromDate, toDate) + 1);
        }

        // create date xticks
        Calendar calUnit = MainActivity.getCalendarCopy(this.fromDate, DateGridAdapter.FROM);
        dates.clear();
        for (int i = 0; i < numUnits; i++) {
            values.add(new Entry(i, 0f));
//            Log.e(TAG, "i=" + i + ", date=" + MainActivity.getDatetimeStr(cal, getDtf()));
            dates.add(MainActivity.getDatetimeStr(calUnit, getDtf()));
            calUnit.add(range, 1);
        }

        // populate y labels
        float maxAmt = 0;
        float prevAmt = 0;
        float nextAmt = 0;
        Calendar prev = MainActivity.getCalendarCopy(this.fromDate, DateGridAdapter.FROM);
        Calendar next = MainActivity.getCalendarCopy(this.toDate, DateGridAdapter.TO);
        prev.add(range, -1);
        next.add(range, 1);
        HashMap<String, Float> dateAmtMap;
        if (accFilters.isEmpty() && catFilters.isEmpty())
            dateAmtMap = ((MainActivity) getActivity()).db.getSortedAmountsByDateRange(prev, next, dateFormat);
        else
            dateAmtMap = ((MainActivity) getActivity()).db.getSortedFilteredAmountsByDateRange(accFilters, catFilters, prev, next, dateFormat);
        int index = 0;
        for (Map.Entry<String, Float> entry : dateAmtMap.entrySet()) {
            if (!dates.contains(entry.getKey())) {
                if (index == 0) prevAmt = entry.getValue();
                else if (index == (dateAmtMap.size() - 1)) nextAmt = entry.getValue();
                continue;
            }
            int x = dates.indexOf(entry.getKey());
            float amt = entry.getValue();
            values.set(x, new Entry(x, amt));
            if (amt > maxAmt) maxAmt = amt;
            totalAmt += amt;
            index++;
        }

        float xMin = (float) (-numUnits * 0.08);
        float xMax = (float) (numUnits * 1.08 - 1);
        values.add(0, new Entry(xMin, prevAmt));
        values.add(new Entry(xMax, nextAmt));

        // set graph properties
        float yMin = (float) (maxAmt == 0 ? -0.45 : -0.45 * maxAmt);
        float yMax = (float) (maxAmt == 0 ? 1.2 : 1.2 * maxAmt);
        lineChart.getAxisLeft().setAxisMinimum(yMin);
        lineChart.getAxisRight().setAxisMinimum(yMin);
        lineChart.getAxisLeft().setAxisMaximum(yMax);
        lineChart.getAxisRight().setAxisMaximum(yMax);
        lineChart.getXAxis().setAxisMinimum(xMin);
        lineChart.getXAxis().setAxisMaximum(xMax);
        updateXLabels(numUnits / granDivisor);

        LineDataSet set;
        if (lineChart.getData() != null && lineChart.getData().getDataSetCount() > 0) {
            set = (LineDataSet) lineChart.getData().getDataSetByIndex(0);
            set.setValues(values);
            lineChart.getData().notifyDataChanged();
            lineChart.notifyDataSetChanged();
            lineChart.invalidate();
        } else {
            set = new LineDataSet(values, "");
            set.setDrawValues(false);
            set.setDrawFilled(true);
            set.setDrawCircles(false);
            set.setDrawCircleHole(true);
            set.setCircleRadius(2);
            set.setDrawHorizontalHighlightIndicator(false);
            set.setDrawVerticalHighlightIndicator(false);
            set.setCircleColor(MainActivity.getColorFromId(getActivity(),R.color.red_500));
            set.setLineWidth(0);
            set.setColor(MainActivity.getColorFromId(getActivity(), R.color.red_500));
            set.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);
        }

        Drawable fadeRed = MainActivity.getIconFromId(getActivity(), R.drawable.fade_red);
        set.setFillDrawable(fadeRed); // Color.RED
        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(set);
        LineData data = new LineData(dataSets);
        lineChart.setData(data);
    }
    private void setupLineChart() {
        if (getActivity() == null)
            return;
        lineChart.getDescription().setEnabled(false);
        lineChart.getXAxis().setEnabled(true);
        lineChart.getXAxis().setDrawGridLines(false);
        lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChart.getXAxis().setYOffset(-48);
        lineChart.getXAxis().setTextColor(MainActivity.getColorFromId(getActivity(), R.color.text_light_gray));
        lineChart.getXAxis().setTextColor(Color.parseColor("#c8c8c8"));
        lineChart.getAxisLeft().setEnabled(false);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.setScaleYEnabled(false);
        lineChart.setDoubleTapToZoomEnabled(false);
        lineChart.getLegend().setEnabled(false);
        IMarker marker = new CustomMarkerView(getActivity(), R.layout.custom_marker_view_layout);
        lineChart.setTouchEnabled(true);
        lineChart.setMarker(marker);
        lineChart.setXAxisRenderer(new CustomXAxisRenderer(lineChart.getViewPortHandler(), lineChart.getXAxis(), lineChart.getTransformer(YAxis.AxisDependency.LEFT)));
    }
    private void configLineChartSelection() {
        lineChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                highlightEntry = new Pair<>(e.getX(), e.getY());
                int x = (int) e.getX();
                highlightDate = dates.get(x);
                setHighlight();
            }

            @Override
            public void onNothingSelected() {
                removeHighlight();
            }
        });
    }
    private void configLineChartScale() {
        lineChart.setOnChartGestureListener(new OnChartGestureListener() {
            float scale = 1f;
            float scaleX = 1f;
            int diff;
            @Override
            public void onChartScale(MotionEvent me, float scaleX, float scaleY) {
                this.scaleX = scaleX;
                final Transformer transformer = lineChart.getTransformer(YAxis.AxisDependency.LEFT);
                final MPPointD start = transformer.getValuesByTouchPoint(0, 0);
                final MPPointD end = transformer.getValuesByTouchPoint(lineChart.getWidth(), 0);
                diff = (int) (end.x - start.x);
                if (diff > numUnits)
                    updateXLabels(numUnits / granDivisor);
                else
                    updateXLabels((float) (end.x - start.x) / granDivisor);
            }
            @Override
            public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
                if (lastPerformedGesture == ChartTouchListener.ChartGesture.X_ZOOM) {
                    scale *= scaleX;
                    if (scale <= 1) scale = 1;
                    updateXLabels(numUnits / scale / granDivisor);
                }
            }

            @Override
            public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

            }
            @Override
            public void onChartLongPressed(MotionEvent me) {

            }
            @Override
            public void onChartDoubleTapped(MotionEvent me) {

            }
            @Override
            public void onChartSingleTapped(MotionEvent me) {

            }
            @Override
            public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {

            }
            @Override
            public void onChartTranslate(MotionEvent me, float dX, float dY) {

            }
        });
    }
    private void configLineChartRecyclerView() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        expenseList.setLayoutManager(linearLayoutManager);
    }

    /**
     * Update
     */
    private void updateAverages() {
        if (getActivity() == null)
            return;
        float[] averages = ((MainActivity) getActivity()).db.getAverages(fromDate, toDate);
        avgDay.setText((averages[0] < 0) ? "–" : String.format(MainActivity.locale,"%.2f", averages[0]));
        avgWeek.setText((averages[1] < 0) ? "–" : String.format(MainActivity.locale,"%.2f", averages[1]));
        avgMonth.setText((averages[2] < 0) ? "–" : String.format(MainActivity.locale,"%.2f", averages[2]));
    }
    private Calendar updateDateRangeForDay(Calendar cal, int range) {
        if (getActivity() == null)
            return null;
        int firstDayOfWeek = ((MainActivity) getActivity()).getDefaultFirstDayOfWeek();
        Calendar copy = MainActivity.getCalendarCopy(cal, range);
        if (selDateState == DateGridAdapter.DAY) {
            int lastDayOfWeek = (firstDayOfWeek == Calendar.MONDAY) ? Calendar.SUNDAY : Calendar.SATURDAY;
            if ((range == DateGridAdapter.FROM) && copy.get(Calendar.DAY_OF_WEEK) == firstDayOfWeek)
                return copy;
            if ((range == DateGridAdapter.TO) && copy.get(Calendar.DAY_OF_WEEK) == lastDayOfWeek)
                return copy;
            copy.set(Calendar.DAY_OF_WEEK, (range == DateGridAdapter.FROM) ? firstDayOfWeek : lastDayOfWeek);
            if ((range == DateGridAdapter.FROM) && copy.after(cal))
                copy.add(Calendar.DATE, -7); // roll back by a week
            if ((range == DateGridAdapter.TO) && cal.after(copy))
                copy.add(Calendar.DATE, 7); // append by a week
        }
        return copy;
    }
    private void updateExpenseList() {
        if (hideExpenseList()) {
            expenseList.setVisibility(View.GONE);
            placeholder.setVisibility(View.GONE);
            link.setVisibility(View.VISIBLE);
            return;
        }
        expenseList.setVisibility(View.VISIBLE);
        link.setVisibility(View.GONE);
        ArrayList<Expense> expenses = ((MainActivity) getActivity()).db.getSortedFilteredExpensesInDateRange(accFilters, catFilters, fromDate, toDate, Constants.DESCENDING, "");
        expenses = MainActivity.insertExpDateHeaders(expenses);
        expenseList.setAdapter(new ExpenseAdapter(getActivity(), expenses, true));
        if (expenses.size() > 0) placeholder.setVisibility(View.GONE);
        else placeholder.setVisibility(View.VISIBLE);
    }
    private void updateLineChartSummary() {
        String lineDateText;
        if (isSelRange) {
            if (selDateState == DateGridAdapter.YEAR)
                lineDateText = getString(R.string.date_range,MainActivity.getDatetimeStr(fromDate, "MMM yyyy"),MainActivity.getDatetimeStr(toDate, "MMM yyyy")).toUpperCase();
            else
                lineDateText = getString(R.string.date_range,MainActivity.getDatetimeStr(fromDate, "dd MMM yyyy"),MainActivity.getDatetimeStr(toDate, "dd MMM yyyy")).toUpperCase();
        } else {
            if (selDateState == DateGridAdapter.YEAR)
                lineDateText = MainActivity.getDatetimeStr(fromDate, "yyyy").toUpperCase();
            else if (selDateState <= DateGridAdapter.WEEK)
                lineDateText = getString(R.string.date_range,MainActivity.getDatetimeStr(fromDate, "dd MMM yyyy"),MainActivity.getDatetimeStr(toDate, "dd MMM yyyy")).toUpperCase();
            else
                lineDateText = MainActivity.getDatetimeStr(fromDate, "MMMM yyyy").toUpperCase();
        }
        lineAmt.setText(String.format(MainActivity.locale,"%.2f", totalAmt));
        lineDate.setText(lineDateText);
    }
    private void updateSelDateState() {
        if (getParentFragment() == null)
            return;
        selDateState = ((ChartsFragment) getParentFragment()).getSelectedDateState();
        isSelRange = ((ChartsFragment) getParentFragment()).getSelectedDatePos() == DateGridAdapter.SELECT_RANGE;
        isHighlighted = selDateState == DateGridAdapter.DAY && !isSelRange;
        if (isSelRange) {
            if (selDateState == DateGridAdapter.MONTH)
                selDateState = DateGridAdapter.YEAR;
            else if (selDateState == DateGridAdapter.DAY)
                selDateState = DateGridAdapter.MONTH;
        }
    }
    private void updateXLabels(float granularityRef) {
        lineChart.getXAxis().setGranularity(1f);
        lineChart.getXAxis().setGranularityEnabled(true);
        ((CustomXAxisRenderer) lineChart.getRendererXAxis()).setLabelCount(numUnits);
        lineChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                if (value < 0 || value >= numUnits) return "";

                int x = (int) value;
                Calendar cal = MainActivity.getCalFromString(getDtf(), dates.get(x));

                if (selDateState <= DateGridAdapter.WEEK)
                    return MainActivity.getDatetimeStr(cal, "EEE").toUpperCase();

                else {
                    float granularity;
                    if (granularityRef < 1) granularity = 1;
                    else if (granularityRef < 2.2) granularity = 2;
                    else if (granularityRef < 5) granularity = 5;
                    else granularity = 10;

                    if (selDateState == DateGridAdapter.MONTH) {
                        int day = cal.get(Calendar.DAY_OF_MONTH);
                        if (day == 1 && value != 0f)
                            return MainActivity.getDatetimeStr(cal, "MMM-YY").toUpperCase();
                        if (value == 0f || value == (numUnits -1))
                            return String.valueOf(day);
                        if ((granularity > 2 && value == numUnits -2) || (numUnits > 27 && day >= 30))
                            return "";
                        if (day % granularity == 0)
                            return String.valueOf(day);
                        return "";
                    } else if (selDateState == DateGridAdapter.YEAR) {
                        int mm = cal.get(Calendar.MONTH);
                        String month = MainActivity.getDatetimeStr(cal, "MMM").toUpperCase();
                        if (mm == 0 && value != 0f)
                            return MainActivity.getDatetimeStr(cal, "MMM-YY").toUpperCase();
                        if (value == 0f || value == (numUnits -1))
                            return month;
                        if (granularity > 2 && value == numUnits -2)
                            return "";
                        if (mm % granularity == 0)
                            return month;
                        return "";
                    }
                    return "";
                }
            }
        });
    }

    /**
     * Helper functions
     */
    private String getDtf() {
        if (selDateState == DateGridAdapter.YEAR) return "yyyy-MM";
        else return "yyyy-MM-dd";
    }
    private void setHighlight() {
        isHighlighted = true;
        if (highlightEntry.first < 0 || highlightEntry.first > numUnits) return;
        lineAmt.setText(String.format(MainActivity.locale,"%.2f", highlightEntry.second));
        highlightLineAmt(true);
        ChartsChildFragmentGraph.this.fromDate = MainActivity.getCalFromString(getDtf(), highlightDate);
        ChartsChildFragmentGraph.this.fromDate = MainActivity.getCalendarCopy(ChartsChildFragmentGraph.this.fromDate, DateGridAdapter.FROM);
        toDate = MainActivity.getCalendarCopy(ChartsChildFragmentGraph.this.fromDate, DateGridAdapter.TO);
        if (selDateState == DateGridAdapter.YEAR) {
            toDate.set(Calendar.DAY_OF_MONTH, fromDate.getActualMaximum(Calendar.DATE));
            lineDate.setText(MainActivity.getDatetimeStr(fromDate, "MMM yyyy").toUpperCase());
        } else
            lineDate.setText(getString(R.string.full_date,MainActivity.getRelativePrefix(fromDate), MainActivity.getDatetimeStr(fromDate, "dd MMM yyyy")).toUpperCase());
        updateExpenseList();
    }
    private void highlightLineAmt(boolean enable) {
        if (getActivity() == null)
            return;
        lineAmt.setTextColor(enable? MainActivity.getColorFromId(getActivity(), R.color.red_500) :
                MainActivity.getColorFromId(getActivity(), R.color.text_dark_gray));
    }
    private void removeHighlight() {
        if (getParentFragment() == null)
            return;
        highlightEntry = null;
        isHighlighted = false;
        fromDate = ((ChartsFragment) getParentFragment()).getDateRange()[0];
        toDate = ((ChartsFragment) getParentFragment()).getDateRange()[1];
        fromDate = updateDateRangeForDay(fromDate, DateGridAdapter.FROM);
        toDate = updateDateRangeForDay(toDate, DateGridAdapter.TO);
        lineChart.highlightValues(null);
        highlightLineAmt(false);
        updateLineChartSummary();
        updateExpenseList();
    }
    private boolean hideExpenseList() {
//        Log.e(TAG, "state=" + selDateState + ", numUnits=" + numUnits + ", highlight=" + isHighlighted);
        if (selDateState == DateGridAdapter.YEAR)
            return true;
        return !(isHighlighted);
    }
    private int getActualSelDateState() {
        if (isSelRange)
            return selDateState - 1;
        else if (!isHighlighted)
            return selDateState;
        else
            return selDateState - 1;
    }
    public boolean isInitialised() { return isInitialised; }
}
