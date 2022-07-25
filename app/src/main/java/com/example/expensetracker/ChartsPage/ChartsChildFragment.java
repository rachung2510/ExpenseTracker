package com.example.expensetracker.ChartsPage;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensetracker.Account;
import com.example.expensetracker.Category;
import com.example.expensetracker.Constants;
import com.example.expensetracker.Currency;
import com.example.expensetracker.Expense;
import com.example.expensetracker.HelperClasses.CustomPieChartRenderer;
import com.example.expensetracker.HelperClasses.CustomMarkerView;
import com.example.expensetracker.HelperClasses.CustomXAxisRenderer;
import com.example.expensetracker.MainActivity;
import com.example.expensetracker.R;
import com.example.expensetracker.RecyclerViewAdapters.AccountAdapter;
import com.example.expensetracker.RecyclerViewAdapters.CatDataAdapter;
import com.example.expensetracker.RecyclerViewAdapters.CategoryAdapter;
import com.example.expensetracker.RecyclerViewAdapters.DateGridAdapter;
import com.example.expensetracker.RecyclerViewAdapters.ExpenseAdapter;
import com.example.expensetracker.RecyclerViewAdapters.FilterAdapter;
import com.example.expensetracker.Section;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.IMarker;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;

public class ChartsChildFragment extends Fragment {

    private static final String TAG = "ChartsChildFragment";
    private final int chartType;
    private View view;

    // Piechart components
    private PieChart pieChart;
    private float totalAmt = 0;
    ImageView pieIcon;
    TextView pieLabel, pieAmt;
    RecyclerView catDataGrid;
    CatDataAdapter catDataAdapter;
    ArrayList<Category> pieCategories = new ArrayList<>();
    private Calendar fromCalPie, toCalPie;

    // Graph components
    private LineChart lineChart;
    TextView lineAmt, lineDate, avgDay, avgWeek, avgMonth, placeholder;
    LinearLayout summaryAmtBlk, statistics;
    RecyclerView expenseList;
    ArrayList<Expense> expenses = new ArrayList<>();
    ArrayList<String> dates = new ArrayList<>();
    private int selDateState;
    private boolean isSelRange = false;
    private Calendar fromCalLine, toCalLine;
    private int num_units = 1;

    // Filter components
    private RecyclerView filterList;
    private MenuItem clearFiltersMenuOption;
    private ArrayList<Account> accFilters = new ArrayList<>();
    private ArrayList<Category> catFilters = new ArrayList<>();

    public static final int TYPE_PIECHART = 0;
    public static final int TYPE_GRAPH = 1;
    public static final int TYPE_CALENDAR = 2;
    private static final float granDivisor = 10f;

    public ChartsChildFragment(int chartType) {
        this.chartType = chartType;
    }
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (getActivity() == null || getParentFragment() == null)
            return null;
        switch (chartType) {
            case TYPE_PIECHART:
                view = inflater.inflate(R.layout.fragment_charts_piechart, container, false);
                pieChart = view.findViewById(R.id.pieChart);
                pieIcon = view.findViewById(R.id.pieIcon);
                pieLabel = view.findViewById(R.id.pieLabel);
                pieAmt = view.findViewById(R.id.pieAmt);
                catDataGrid = view.findViewById(R.id.catDataGrid);
                ((TextView) view.findViewById(R.id.summaryCurrency)).setText(new Currency(getActivity()).getSymbol());

                pieIcon.setVisibility(ImageView.GONE);
                loadPieChartData();
                setupPieChart();
                configPieChartSelection();
                configPieChartRecyclerView();

                if (getActivity() != null)
                    ((MainActivity) getActivity()).updateSummaryData(Constants.CHARTS);
                break;

            case TYPE_GRAPH:
                view = inflater.inflate(R.layout.fragment_charts_graph, container, false);
                lineChart = view.findViewById(R.id.lineChart);
                lineDate = view.findViewById(R.id.summaryDate);
                lineAmt = view.findViewById(R.id.summaryAmt);
                updateCurrency(new Currency(getActivity()).getSymbol());
                summaryAmtBlk = view.findViewById(R.id.summaryAmtBlk);
                summaryAmtBlk.setOnClickListener(view1 -> resetOnClick());
                statistics = view.findViewById(R.id.statistics);
                statistics.setOnClickListener(view1 -> resetOnClick());
                expenseList = view.findViewById(R.id.expenseList);
                avgDay = view.findViewById(R.id.dayAvg);
                avgWeek = view.findViewById(R.id.weekAvg);
                avgMonth = view.findViewById(R.id.monthAvg);
                placeholder = view.findViewById(R.id.placeholder);

                setupLineChart();
                configLineChartSelection();
                configLineChartScale();
                configLineChartRecyclerView();
                updateDateFilters();

                setHasOptionsMenu(true);
                filterList = view.findViewById(R.id.sectionFilters);
                break;

            case TYPE_CALENDAR:
                break;
        }
        return view;
    }

    /**
     * COMMON FUNCTIONS
     */
    public void updateDateFilters() {
        Calendar from = Calendar.getInstance();
        Calendar to = Calendar.getInstance();
        if (getParentFragment() != null) {
            from = ((ChartsFragment) getParentFragment()).getDateRange()[0];
            to = ((ChartsFragment) getParentFragment()).getDateRange()[1];
        }
        switch (chartType) {
            case TYPE_PIECHART:
                fromCalPie = MainActivity.getCalendarCopy(from, DateGridAdapter.FROM);
                toCalPie = MainActivity.getCalendarCopy(to, DateGridAdapter.TO);
                loadPieChartData();
                catDataAdapter = new CatDataAdapter(getParentFragment().getActivity(), fromCalPie, toCalPie);
                catDataGrid.setAdapter(catDataAdapter);
                break;

            case TYPE_GRAPH:
                updateSelDateState();
                fromCalLine = MainActivity.getCalendarCopy(from, DateGridAdapter.FROM);
                toCalLine = MainActivity.getCalendarCopy(to, DateGridAdapter.TO);
                if (fromCalLine == null) {
                    if (getActivity() == null)
                        break;
                    fromCalLine = ((MainActivity) getActivity()).db.getFirstLastDates()[0];
                    if (fromCalLine == null) {
                        fromCalLine = ((MainActivity) getActivity()).getInitSelectedDates(DateGridAdapter.FROM, DateGridAdapter.MONTH);
                        toCalLine = ((MainActivity) getActivity()).getInitSelectedDates(DateGridAdapter.TO, DateGridAdapter.MONTH);
                    } else
                        toCalLine = ((MainActivity) getActivity()).db.getFirstLastDates()[1];
                }
                Calendar old = MainActivity.getCalendarCopy(fromCalLine, DateGridAdapter.FROM);
                fromCalLine = updateDateRange(fromCalLine, DateGridAdapter.FROM);
                toCalLine = updateDateRange(toCalLine, DateGridAdapter.TO);
                loadLineChartData();
                updateExpenseRecyclerView();
                updateLineChartSummary();
                updateAverages();
                lineChart.fitScreen();
                lineChart.highlightValues(null);
                highlightLineAmt(false);
                if (selDateState == DateGridAdapter.DAY) {
                    lineChart.highlightValue((float) dates.indexOf(MainActivity.getDatetimeStr(old, getDtf())), 0);
                    highlightLineAmt(true);
                }
                break;

            default:
                break;
        }
    }
    public void invalidateMenu() {
        if (getParentFragment() == null) return;
        Toolbar toolbar = ((ChartsFragment) getParentFragment()).getToolbar();
        switch (chartType) {
            case TYPE_PIECHART:
                toolbar.getMenu().clear();
                break;
            case TYPE_GRAPH:
                createOptionsMenu(toolbar);
                break;
            default:
                break;
        }
    }

    /**
     * MENU
     */
    public void createOptionsMenu(Toolbar toolbar) {
        toolbar.inflateMenu(R.menu.home_menu);
        clearFiltersMenuOption = toolbar.getMenu().findItem(R.id.clearFilters);
        updateClearFiltersItem();
        toolbar.setOnMenuItemClickListener(menuItemClickListener);
    }
    public Toolbar.OnMenuItemClickListener menuItemClickListener = item -> {
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
        if (filterList.getAdapter() == null) {
            FlexboxLayoutManager manager = new FlexboxLayoutManager(getActivity());
            manager.setFlexDirection(FlexDirection.ROW);
            manager.setJustifyContent(JustifyContent.FLEX_START);
            filterList.setLayoutManager(manager);
        }
        filterList.setAdapter(new FilterAdapter(getActivity(), accFilters, catFilters));
        if (!accFilters.isEmpty())
            updateCurrency(accFilters.get(0).getCurrencySymbol());
        else
            updateCurrency(new Currency(getActivity()).getSymbol());
        if (clearFiltersMenuOption != null) // on view created
            updateClearFiltersItem();
        loadLineChartData();
        updateExpenseRecyclerView();
        updateLineChartSummary();
        updateAverages();
    }
    public void filterAccDialog(AccountAdapter adapter) {
        if (getActivity() == null)
            return;
        final View view = getLayoutInflater().inflate(R.layout.dialog_expense_opt_section, null);
        AlertDialog.Builder dialogBuilder = ((MainActivity) getActivity()).expenseSectionDialog(adapter, view);
        ((TextView) view.findViewById(R.id.expOptSectionTitle)).setText(getResources().getString(R.string.filter_dialog_title, getActivity().getString(R.string.ACC)));
        dialogBuilder.setView(view);
        dialogBuilder.setPositiveButton(android.R.string.yes, ((dialogInterface, i) -> {
            accFilters = adapter.getAllSelected();
            applyFilters(false);
        }));
        dialogBuilder.setNeutralButton(android.R.string.no, (((dialog, i) -> dialog.cancel())));
        dialogBuilder.show();
    }
    public void filterCatDialog(CategoryAdapter adapter) {
        if (getActivity() == null)
            return;
        final View expOptSectionView = getLayoutInflater().inflate(R.layout.dialog_expense_opt_section, null);
        AlertDialog.Builder dialogBuilder = ((MainActivity) getActivity()).expenseSectionDialog(adapter, expOptSectionView);
        ((TextView) expOptSectionView.findViewById(R.id.expOptSectionTitle)).setText(getResources().getString(R.string.filter_dialog_title, getResources().getString(R.string.CAT)));
        dialogBuilder.setView(expOptSectionView);
        dialogBuilder.setPositiveButton(android.R.string.yes, ((dialogInterface, i) -> {
            catFilters = adapter.getAllSelected();
            applyFilters(false);
        }));
        dialogBuilder.setNeutralButton(android.R.string.no, (((dialog, i) -> dialog.cancel())));
        dialogBuilder.show();
    }
    public void updateClearFiltersItem() {
        clearFiltersMenuOption.setVisible(!accFilters.isEmpty() || !catFilters.isEmpty());
    }
    public void setAccFilters(ArrayList<Account> accFilters) {
        this.accFilters = accFilters;
    }
    public void setCatFilters(ArrayList<Category> catFilters) {
        this.catFilters = catFilters;
    }

    /**
     * Piechart functions
     */
    public void loadPieChartData() {
        if (getActivity() == null)
            return;
        ArrayList<PieEntry> pieEntries = new ArrayList<>();
        ArrayList<Category> categories = ((MainActivity) getActivity()).db.getAllCategories();
        ArrayList<Integer> colors = new ArrayList<>();
        for (Category cat : categories) {
            float amt;
            if (fromCalPie == null) amt = ((MainActivity) getActivity()).db.getTotalAmtByCategory(cat);
            else amt = ((MainActivity) getActivity()).db.getTotalAmtByCategoryInRange(cat, fromCalPie, toCalPie);
            if (amt != 0f) {
                pieEntries.add(new PieEntry(amt, cat.getIcon()));
                colors.add(ContextCompat.getColor(getActivity(), cat.getColorId()));
                pieCategories.add(cat);
            }
        }

        PieDataSet dataSet = new PieDataSet(pieEntries, "data");
        dataSet.setColors(colors);
        dataSet.setSliceSpace(2f);
        dataSet.setValueTextColors(colors);
        dataSet.setSelectionShift(4f);

        // Value lines
        dataSet.setValueLinePart1Length(0.6f);
        dataSet.setValueLinePart2Length(0.3f);
        dataSet.setValueLinePart1OffsetPercentage(115f); // Line starts outside of chart
        dataSet.setUsingSliceColorAsValueLineColor(true);

        // Value text appearance
        dataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTypeface(Typeface.DEFAULT_BOLD);

        PieData pieData = new PieData(dataSet);
        pieData.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(MainActivity.locale, "$%.2f", value);
            }
        });

        pieChart.setData(pieData);
        pieChart.invalidate();

//        pieChart.animateY(300, Easing.EaseInOutQuad);
    }
    public void setupPieChart() {
        pieChart.setRenderer(new CustomPieChartRenderer(pieChart, 10f, pieChart.getAnimator(), pieChart.getViewPortHandler()));
        pieChart.setExtraOffsets(36f, 15f, 36f, 15f);

        // hole
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleRadius(64f);
        pieChart.setTransparentCircleRadius(69f);
        pieChart.setHoleColor(Color.TRANSPARENT);

        // disable legend & description
        pieChart.getDescription().setEnabled(false);
        pieChart.getLegend().setEnabled(false);
    }
    public void configPieChartSelection() {
        pieChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                if (getActivity() == null)
                    return;
                if (e == null) return;
                pieAmt.setText(String.format(MainActivity.locale,"%.2f",e.getY()));
                pieIcon.setVisibility(ImageButton.VISIBLE);
                Category cat =  pieCategories.get((int) h.getX());
                pieIcon.setForeground(MainActivity.getIconFromId(getActivity(), cat.getIconId()));
                pieLabel.setText(cat.getName());
                pieLabel.setTypeface(ResourcesCompat.getFont(getActivity(), R.font.roboto_regular));
            }

            @Override
            public void onNothingSelected() {
                if (getActivity() == null)
                    return;
                pieIcon.setVisibility(ImageButton.GONE);
                pieLabel.setText(getString(R.string.EXPS));
                pieLabel.setTypeface(ResourcesCompat.getFont(getActivity(), R.font.roboto_medium));
                pieAmt.setText(String.format(MainActivity.locale,"%.2f", totalAmt));
            }
        });
        pieChart.invalidate();
    }
    public void configPieChartRecyclerView() {
        if (getActivity() == null)
            return;
        catDataAdapter = new CatDataAdapter(getActivity(), fromCalPie, toCalPie);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity(), 2, GridLayoutManager.VERTICAL, false);
        catDataGrid.setLayoutManager(gridLayoutManager);
        catDataGrid.setAdapter(catDataAdapter);
//        return new GridLayoutManager(getActivity(), 2, GridLayoutManager.VERTICAL, false);
    }
    public void setPieChartTotalAmt(float totalAmt) {
        this.totalAmt = totalAmt;
        pieAmt.setText(String.format(MainActivity.locale, "%.2f", this.totalAmt));
    }

    /**
     * Linechart functions
     */
    @SuppressWarnings("ConstantConditions")
    public void loadLineChartData() {
        if (getActivity() == null)
            return;
        ArrayList<Entry> values = new ArrayList<>();
        float maxAmt = 0;
        totalAmt = 0;
        updateExpenses();
        MainActivity.sortExpenses(expenses, Constants.ASCENDING);

        LocalDate fromDate = LocalDateTime.ofInstant(fromCalLine.toInstant(), fromCalLine.getTimeZone().toZoneId()).toLocalDate();
        LocalDate toDate = LocalDateTime.ofInstant(toCalLine.toInstant(), toCalLine.getTimeZone().toZoneId()).toLocalDate();
        if (selDateState == DateGridAdapter.YEAR)
            num_units = isSelRange ? (int) ChronoUnit.MONTHS.between(fromDate, toDate)+1 : 12;
        else if (selDateState <= DateGridAdapter.WEEK)
            num_units = 7;
        else
            num_units = (int) (ChronoUnit.DAYS.between(fromDate, toDate) + 1);

        dates.clear();
        Calendar cal = MainActivity.getCalendarCopy(fromCalLine, DateGridAdapter.FROM);
        for (int i = 0;i < num_units;i++) {
            values.add(new Entry(i, 0f));
//            Log.e(TAG, "i=" + i + ", date=" + MainActivity.getDatetimeStr(cal, getDtf()));
            dates.add(MainActivity.getDatetimeStr(cal, getDtf()));
            cal.add((selDateState == DateGridAdapter.YEAR) ? Calendar.MONTH : Calendar.DATE, 1);
        }
        for (Expense e : expenses) {
            int x = dates.indexOf(e.getDatetimeStr(getDtf()));
//            Log.e(TAG, "x=" + x + "/" + num_units);
            float newAmt = values.get(x).getY() + e.getAmount();
            values.set(x, new Entry(x, newAmt));
            if (newAmt > maxAmt)
                maxAmt = newAmt;
            totalAmt += e.getAmount();
        }
//        for (Entry v : values) Log.e(TAG, "x=" + v.getX() + ", y=" + v.getY());

        float xMin = (float) (-num_units*0.08);
        float xMax = (float) (num_units*1.08-1);
        values.add(0, new Entry(xMin, 0f));
        values.add(new Entry(xMax, 0f));
        updateXLabels(num_units / granDivisor);
        float yMin = (float) (maxAmt == 0 ? -0.45 : -0.45 * maxAmt);
        float yMax = (float) (maxAmt == 0 ? 1.2 : 1.2 * maxAmt);
        lineChart.getAxisLeft().setAxisMinimum(yMin);
        lineChart.getAxisRight().setAxisMinimum(yMin);
        lineChart.getAxisLeft().setAxisMaximum(yMax);
        lineChart.getAxisRight().setAxisMaximum(yMax);
        lineChart.getXAxis().setAxisMinimum(xMin);
        lineChart.getXAxis().setAxisMaximum(xMax);

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
            set.setCircleColor(ContextCompat.getColor(getActivity(),R.color.red_500));
            set.setLineWidth(0);
            set.setColor(ContextCompat.getColor(getActivity(), R.color.red_500));
            set.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);
        }

        Drawable drawable = ContextCompat.getDrawable(getActivity(), R.drawable.fade_red);
        set.setFillDrawable(drawable);
//        set.setFillColor(Color.RED);
        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(set);
        LineData data = new LineData(dataSets);

        lineChart.setData(data);
    }
    public void setupLineChart() {
        if (getActivity() == null)
            return;
        lineChart.getDescription().setEnabled(false);
        lineChart.getXAxis().setEnabled(true);
        lineChart.getXAxis().setDrawGridLines(false);
        lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChart.getXAxis().setYOffset(-48);
        lineChart.getXAxis().setTextColor(ContextCompat.getColor(getActivity(), R.color.text_light_gray));
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
    public void configLineChartSelection() {
        lineChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                if (e.getX() < 0 || e.getX() > num_units) return;
                lineAmt.setText(String.format(MainActivity.locale,"%.2f",e.getY()));
                highlightLineAmt(true);
                int x = (int) e.getX();
                ChartsChildFragment.this.fromCalLine = MainActivity.getCalFromString(getDtf(), dates.get(x));
                ChartsChildFragment.this.fromCalLine = MainActivity.getCalendarCopy(ChartsChildFragment.this.fromCalLine, DateGridAdapter.FROM);
                toCalLine = MainActivity.getCalendarCopy(ChartsChildFragment.this.fromCalLine, DateGridAdapter.TO);
                if (selDateState == DateGridAdapter.YEAR) {
                    toCalLine.set(Calendar.DAY_OF_MONTH, fromCalLine.getActualMaximum(Calendar.DATE));
                    lineDate.setText(MainActivity.getDatetimeStr(fromCalLine, "MMM yyyy").toUpperCase());
                } else
                    lineDate.setText(getString(R.string.full_date,MainActivity.getRelativePrefix(fromCalLine), MainActivity.getDatetimeStr(fromCalLine, "dd MMM yyyy")).toUpperCase());
                updateExpenseRecyclerView();
            }

            @Override
            public void onNothingSelected() {
                resetOnClick();
            }
        });
    }
    public void configLineChartScale() {
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
                if (diff > num_units)
                    updateXLabels(num_units / granDivisor);
                else
                    updateXLabels((float) (end.x - start.x) / granDivisor);
            }
            @Override
            public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
                if (lastPerformedGesture == ChartTouchListener.ChartGesture.X_ZOOM) {
                    scale *= scaleX;
                    if (scale <= 1) scale = 1;
                    updateXLabels(num_units / scale / granDivisor);
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
    public void configLineChartRecyclerView() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        expenseList.setLayoutManager(linearLayoutManager);
    }

    /**
     * Update
     */
    public void updateAverages() {
        if (getActivity() == null)
            return;
        float avgDayValue = ((MainActivity) getActivity()).db.getDayAverage(expenses);
        float avgWeekValue = ((MainActivity) getActivity()).db.getWeekAverage(expenses);
        float avgMonthValue = ((MainActivity) getActivity()).db.getMonthAverage(expenses);
        avgDay.setText(String.format(MainActivity.locale,"%.2f", avgDayValue));
        avgWeek.setText(String.format(MainActivity.locale,"%.2f", avgWeekValue));
        avgMonth.setText(String.format(MainActivity.locale,"%.2f", avgMonthValue));
    }
    public void updateCurrency(String curr) {
        ((TextView) view.findViewById(R.id.summaryCurrency)).setText(curr);
        ((TextView) view.findViewById(R.id.curr1)).setText(curr);
        ((TextView) view.findViewById(R.id.curr2)).setText(curr);
        ((TextView) view.findViewById(R.id.curr3)).setText(curr);
    }
    public Calendar updateDateRange(Calendar cal, int range) {
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
    public void updateExpenses() {
        if (getActivity() == null)
            return;
        if (fromCalLine == null) expenses = ((MainActivity) getActivity()).db.getAllExpenses();
        else expenses = ((MainActivity) getActivity()).db.getExpensesByDateRange(fromCalLine, toCalLine);
        ArrayList<Expense> expensesByFilter = getExpensesBySectionFilter();
        if (expensesByFilter != null)
            expenses.retainAll(expensesByFilter);
    }
    public void updateExpenseRecyclerView() {
        updateExpenses();
        MainActivity.sortExpenses(expenses, Constants.DESCENDING);
        expenses = MainActivity.insertExpDateHeaders(expenses);
        expenseList.setAdapter(new ExpenseAdapter(getActivity(), expenses, true));
        if (expenses.size() > 0) placeholder.setVisibility(View.GONE);
        else placeholder.setVisibility(View.VISIBLE);
    }
    public void updateLineChartSummary() {
        String lineDateText;
        if (isSelRange) {
            if (selDateState == DateGridAdapter.YEAR)
                lineDateText = getString(R.string.date_range,MainActivity.getDatetimeStr(fromCalLine, "MMM yyyy"),MainActivity.getDatetimeStr(toCalLine, "MMM yyyy")).toUpperCase();
            else
                lineDateText = getString(R.string.date_range,MainActivity.getDatetimeStr(fromCalLine, "dd MMM yyyy"),MainActivity.getDatetimeStr(toCalLine, "dd MMM yyyy")).toUpperCase();
        } else {
            if (selDateState == DateGridAdapter.YEAR)
                lineDateText = MainActivity.getDatetimeStr(fromCalLine, "yyyy").toUpperCase();
            else if (selDateState <= DateGridAdapter.WEEK)
                lineDateText = getString(R.string.date_range,MainActivity.getDatetimeStr(fromCalLine, "dd MMM yyyy"),MainActivity.getDatetimeStr(toCalLine, "dd MMM yyyy")).toUpperCase();
            else
                lineDateText = MainActivity.getDatetimeStr(fromCalLine, "MMMM yyyy").toUpperCase();
        }
        lineAmt.setText(String.format(MainActivity.locale,"%.2f",totalAmt));
        lineDate.setText(lineDateText);
    }
    public void updateSelDateState() {
        if (getParentFragment() == null)
            return;
        selDateState = ((ChartsFragment) getParentFragment()).getSelDateState();
        isSelRange = ((ChartsFragment) getParentFragment()).getSelDatePos() == DateGridAdapter.SELECT_RANGE;
        if (isSelRange) {
            if (selDateState == DateGridAdapter.MONTH)
                selDateState = DateGridAdapter.YEAR;
            else if (selDateState == DateGridAdapter.DAY)
                selDateState = DateGridAdapter.MONTH;
        }
    }
    public void updateXLabels(float granularityRef) {
        lineChart.getXAxis().setGranularity(1f);
        lineChart.getXAxis().setGranularityEnabled(true);
        ((CustomXAxisRenderer) lineChart.getRendererXAxis()).setLabelCount(num_units);
        lineChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                if (value < 0 || value >= num_units) return "";
                int x = (int) value;
                Calendar cal = MainActivity.getCalFromString(getDtf(), dates.get(x));
                if (selDateState == DateGridAdapter.YEAR) {
                    return MainActivity.getDatetimeStr(cal, "MMM").toUpperCase();
                }
                if (selDateState <= DateGridAdapter.WEEK) {
                    return MainActivity.getDatetimeStr(cal, "EEE").toUpperCase();
                }
//                        Log.e(TAG, "num_units=" + num_units + ", gran=" + granularityRef);
                float granularity;
                if (granularityRef < 1) granularity = 1;
                else if (granularityRef < 2.2) granularity = 2;
                else if (granularityRef < 5) granularity = 5;
                else granularity = 10;
                String d = MainActivity.getDatetimeStr(cal, "d");
                int dd = Integer.parseInt(d);
//                        Log.e(TAG, "d=" + d + ", cal=" + MainActivity.getDatetimeStr(cal, getDtf()));
                if (d.equals("1") && value != 0f)
                    return MainActivity.getDatetimeStr(cal, "MMM").toUpperCase();
                if (value == 0f || value == (num_units-1))
                    return d;
                if ((granularity>2 && value==num_units-2) || (num_units>27 && dd>=30))
                    return "";
                if (dd%granularity == 0)
                    return d;
                return "";
            }
        });
    }

    public ArrayList<Expense> getExpensesBySectionFilter() {
        if (getActivity() == null)
            return null;
        if (accFilters.isEmpty() && catFilters.isEmpty())
            return null;
        return ((MainActivity) getActivity()).db.getExpensesByFilters(accFilters, catFilters);
    }
    public String getDtf() {
        if (selDateState == DateGridAdapter.YEAR) return "MM-yyyy";
        else return "dd-MM-yyyy";
    }
    public void highlightLineAmt(boolean enable) {
        if (getActivity() == null)
            return;
        lineAmt.setTextColor(enable? ContextCompat.getColor(getActivity(), R.color.red_500) :
                ContextCompat.getColor(getActivity(), R.color.text_dark_gray));
    }
    public void resetOnClick() {
        if (getParentFragment() == null)
            return;
        fromCalLine = ((ChartsFragment) getParentFragment()).getDateRange()[0];
        toCalLine = ((ChartsFragment) getParentFragment()).getDateRange()[1];
        fromCalLine = updateDateRange(fromCalLine, DateGridAdapter.FROM);
        toCalLine = updateDateRange(toCalLine, DateGridAdapter.TO);
        lineChart.highlightValues(null);
        highlightLineAmt(false);
        updateLineChartSummary();
        updateExpenseRecyclerView();
        updateAverages();
    }

    // debug
    public static void logFromTo(String tag, Calendar from, Calendar to) {
        Log.e(TAG, String.format("from%s=", tag) + MainActivity.getDatetimeStr(from,"dd MMM yyyy") + String.format(", to%s=", tag) + MainActivity.getDatetimeStr(to,"dd MMM yyyy"));
    }
    public static String logExpenses(ArrayList<Expense> expenses) {
        StringBuilder msg = new StringBuilder();
        for (Expense e : expenses)
            msg.append(e.getDescription().isEmpty() ? "date" : e.getDescription()).append(", ");
        return msg.toString();
    }
    public static <T extends Section> void logFilters(ArrayList<T> arrayList, String arrayName) {
        StringBuilder msg = new StringBuilder();
        for (T t : arrayList) {
            if (msg.length() > 0) msg.append(", ");
            msg.append(t.getName());
        }
        Log.e(TAG, arrayName + "={ " + msg + " }");
    }
}
