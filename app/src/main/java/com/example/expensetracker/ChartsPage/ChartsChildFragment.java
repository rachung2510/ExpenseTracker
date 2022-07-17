package com.example.expensetracker.ChartsPage;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensetracker.Category;
import com.example.expensetracker.Constants;
import com.example.expensetracker.Currency;
import com.example.expensetracker.Expense;
import com.example.expensetracker.HelperClasses.CustomPieChartRenderer;
import com.example.expensetracker.HelperClasses.CustomMarkerView;
import com.example.expensetracker.HelperClasses.CustomXAxisRenderer;
import com.example.expensetracker.MainActivity;
import com.example.expensetracker.R;
import com.example.expensetracker.RecyclerViewAdapters.CatDataAdapter;
import com.example.expensetracker.RecyclerViewAdapters.DateGridAdapter;
import com.example.expensetracker.RecyclerViewAdapters.ExpenseAdapter;
import com.github.mikephil.charting.animation.Easing;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;

public class ChartsChildFragment extends Fragment {

    private static final String TAG = "ChartsChildFragment";
    private final int chartType;

    // Piechart components
    private PieChart pieChart;
    private float totalAmt = 0;
    ImageView pieIcon;
    TextView pieLabel, pieAmt;
    RecyclerView catDataGrid;
    CatDataAdapter catDataAdapter;
    ArrayList<Category> pieCategories = new ArrayList<>();

    // Graph components
    private LineChart lineChart;
    TextView lineAmt, lineDate, avgDay, avgWeek, avgMonth;
    LinearLayout summaryAmtBlk;
    RecyclerView expenseList;
    ExpenseAdapter expenseAdapter;
    ArrayList<Expense> expenses = new ArrayList<>();
    ArrayList<String> dates = new ArrayList<>();
    int num_units = 1;
    private int selDateState;
    private boolean isSelRange = false;

    public static final int TYPE_PIECHART = 0;
    public static final int TYPE_GRAPH = 1;
    public static final int TYPE_CALENDAR = 2;
    private static final float granDivisor = 10f;

    public ChartsChildFragment(int chartType) {
        this.chartType = chartType;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = null;
        Calendar from, to;

        switch (chartType) {
            case TYPE_PIECHART:
                view = inflater.inflate(R.layout.fragment_charts_piechart, container, false);
                pieChart = view.findViewById(R.id.pieChart);
                pieIcon = view.findViewById(R.id.pieIcon);
                pieLabel = view.findViewById(R.id.pieLabel);
                pieAmt = view.findViewById(R.id.pieAmt);
                catDataGrid = view.findViewById(R.id.catDataGrid);
                ((TextView) view.findViewById(R.id.summaryCurrency)).setText((new Currency()).getSymbol());

                from = DateGridAdapter.getInitSelectedDates(DateGridAdapter.FROM, DateGridAdapter.MONTH);
                to = DateGridAdapter.getInitSelectedDates(DateGridAdapter.TO, DateGridAdapter.MONTH);
                pieIcon.setVisibility(ImageView.GONE);
                loadPieChartData(from, to);
                setupPieChart();
                configPieChartSelection();
                configPieChartRecyclerView(from, to);

                ((MainActivity) getActivity()).updateSummaryData(((MainActivity) getActivity()).getExpenseList());
                break;

            case TYPE_GRAPH:
                view = inflater.inflate(R.layout.fragment_charts_graph, container, false);
                lineChart = view.findViewById(R.id.lineChart);
                lineDate = view.findViewById(R.id.summaryDate);
                ((TextView) view.findViewById(R.id.summaryCurrency)).setText((new Currency()).getSymbol());
                ((TextView) view.findViewById(R.id.curr1)).setText((new Currency()).getSymbol());
                ((TextView) view.findViewById(R.id.curr2)).setText((new Currency()).getSymbol());
                ((TextView) view.findViewById(R.id.curr3)).setText((new Currency()).getSymbol());
                summaryAmtBlk = view.findViewById(R.id.summaryAmtBlk);
                summaryAmtBlk.setOnClickListener(view1 -> {
                    Calendar from1 = ((ChartsFragment) getParentFragment()).getDateRange()[0];
                    Calendar to1 = ((ChartsFragment) getParentFragment()).getDateRange()[1];
                    from1 = updateDateRange(from1, DateGridAdapter.FROM);
                    to1 = updateDateRange(to1, DateGridAdapter.TO);
                    lineChart.highlightValues(null);
                    highlightLineAmt(false);
                    updateLineChartSummary(from1, to1);
                    updateExpenseList(from1, to1);
                });
                lineAmt = view.findViewById(R.id.summaryAmt);
                expenseList = view.findViewById(R.id.expenseList);
                avgDay = view.findViewById(R.id.dayAvg);
                avgWeek = view.findViewById(R.id.weekAvg);
                avgMonth = view.findViewById(R.id.monthAvg);

                from = ((ChartsFragment) getParentFragment()).getDateRange()[0];
                to = ((ChartsFragment) getParentFragment()).getDateRange()[1];
                setupLineChart();
                configLineChartSelection(from, to);
                configLineChartScale();
                configLineChartRecyclerView();
                updateDateFilters(from, to);
                break;

            case TYPE_CALENDAR:
                break;
        }
        return view;
    }

    public void updateDateFilters(Calendar from, Calendar to) {
        switch (chartType) {
            case TYPE_PIECHART:
                loadPieChartData(from, to);
                catDataAdapter = new CatDataAdapter(getParentFragment().getActivity(), from, to);
                catDataGrid.setAdapter(catDataAdapter);
                break;

            case TYPE_GRAPH:
                updateSelDateState();
                if (from == null) {
                    from = ((MainActivity) getActivity()).db.getFirstLastDates()[0];
                    if (from == null) {
                        from = DateGridAdapter.getInitSelectedDates(DateGridAdapter.FROM, DateGridAdapter.MONTH);
                        to = DateGridAdapter.getInitSelectedDates(DateGridAdapter.TO, DateGridAdapter.MONTH);
                    } else
                        to = ((MainActivity) getActivity()).db.getFirstLastDates()[1];
                }
                Calendar old = MainActivity.getCalendarCopy(from, DateGridAdapter.FROM);
                from = updateDateRange(from, DateGridAdapter.FROM);
                to = updateDateRange(to, DateGridAdapter.TO);
                loadLineChartData(from, to);
                updateExpenseList(from, to);
                updateLineChartSummary(from, to);
                updateAverages(from, to);
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

    /**
     * Piechart functions
     */
    public void loadPieChartData(Calendar from, Calendar to) {
        ArrayList<PieEntry> pieEntries = new ArrayList<>();
        ArrayList<Category> categories = ((MainActivity) getActivity()).db.getAllCategories();
        ArrayList<Integer> colors = new ArrayList<>();
        for (Category cat : categories) {
            float amt;
            if (from == null) amt = ((MainActivity) getActivity()).db.getTotalAmtByCategory(cat);
            else amt = ((MainActivity) getActivity()).db.getTotalAmtByCategoryInRange(cat, from, to);
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
//        dataSet.setValueLineWidth(2f);
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

        pieChart.animateY(500, Easing.EaseInOutQuad);
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
                if (e == null) return;
                pieAmt.setText(String.format(MainActivity.locale,"%.2f",e.getY()));
                pieIcon.setVisibility(ImageButton.VISIBLE);
                Category cat =  pieCategories.get((int) h.getX());
                pieIcon.setForeground(MainActivity.getIconFromId(getActivity(), cat.getIconId()));
                pieLabel.setText(cat.getName());
                pieLabel.setTypeface(ResourcesCompat.getFont(getParentFragment().getActivity(), R.font.roboto_regular));
            }

            @Override
            public void onNothingSelected() {
                pieIcon.setVisibility(ImageButton.GONE);
                pieLabel.setText(getString(R.string.exps_caps));
                pieLabel.setTypeface(ResourcesCompat.getFont(getParentFragment().getActivity(), R.font.roboto_medium));
                pieAmt.setText(String.format(MainActivity.locale,"%.2f", totalAmt));
            }
        });
        pieChart.invalidate();
    }
    public void configPieChartRecyclerView(Calendar from, Calendar to) {
        catDataAdapter = new CatDataAdapter(getParentFragment().getActivity(), from, to);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getParentFragment().getActivity(), 2, GridLayoutManager.VERTICAL, false);
        catDataGrid.setLayoutManager(gridLayoutManager);
        catDataGrid.setAdapter(catDataAdapter);
    }
    public void setPieChartTotalAmt(float totalAmt) {
        this.totalAmt = totalAmt;
        pieAmt.setText(String.format(MainActivity.locale, "%.2f", this.totalAmt));
    }

    /**
     * Linechart functions
     */
    private void loadLineChartData(Calendar from, Calendar to) {
        ArrayList<Entry> values = new ArrayList<>();
        float maxAmt = 0;
        totalAmt = 0;
        if (from == null) expenses = ((MainActivity) getParentFragment().getActivity()).db.getAllExpenses();
        else expenses =  ((MainActivity) getParentFragment().getActivity()).db.getExpensesByDateRange(from, to);
        expenses = MainActivity.sortExpenses(expenses, Constants.ASCENDING);

        LocalDate fromDate = LocalDateTime.ofInstant(from.toInstant(), from.getTimeZone().toZoneId()).toLocalDate();
        LocalDate toDate = LocalDateTime.ofInstant(to.toInstant(), to.getTimeZone().toZoneId()).toLocalDate();
        if (selDateState == DateGridAdapter.YEAR)
            num_units = isSelRange ? (int) ChronoUnit.MONTHS.between(fromDate, toDate)+1 : 12;
        else if (selDateState <= DateGridAdapter.WEEK)
            num_units = 7;
        else
            num_units = (int) (ChronoUnit.DAYS.between(fromDate, toDate) + 1);

        dates.clear();
        Calendar cal = MainActivity.getCalendarCopy(from, DateGridAdapter.FROM);
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
            if (newAmt > maxAmt) maxAmt = newAmt;
            totalAmt += e.getAmount();
        }
//        for (Entry v : values) Log.e(TAG, "x=" + v.getX() + ", y=" + v.getY());

        float xMin = (float) (-num_units*0.08);
        float xMax = (float) (num_units*1.08-1);
        values.add(0, new Entry(xMin, 0f));
        values.add(new Entry(xMax, 0f));
        updateXLabels(num_units / granDivisor);
        float yMin = (float) (maxAmt == 0f ? -0.45 : -0.45 * maxAmt);
        float yMax = (float) (maxAmt == 0f ? 1.2 : 1.2 * maxAmt);
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
            set.setCircleColor(ContextCompat.getColor(getContext(),R.color.red_500));
            set.setLineWidth(0);
            set.setColor(ContextCompat.getColor(getContext(), R.color.red_500));
            set.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);
        }

        Drawable drawable = ContextCompat.getDrawable(getContext(), R.drawable.fade_red);
        set.setFillDrawable(drawable);
//        set.setFillColor(Color.RED);
        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(set);
        LineData data = new LineData(dataSets);

        lineChart.setData(data);
    }
    public void setupLineChart() {
        lineChart.getDescription().setEnabled(false);
        lineChart.getXAxis().setEnabled(true);
        lineChart.getXAxis().setDrawGridLines(false);
        lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChart.getXAxis().setYOffset(-48);
        lineChart.getXAxis().setTextColor(ContextCompat.getColor(getContext(), R.color.text_light_gray));
        lineChart.getXAxis().setTextColor(Color.parseColor("#c8c8c8"));
        lineChart.getAxisLeft().setEnabled(false);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.setScaleYEnabled(false);
        lineChart.setDoubleTapToZoomEnabled(false);
        lineChart.getLegend().setEnabled(false);
        IMarker marker = new CustomMarkerView(getContext(), R.layout.custom_marker_view_layout);
        lineChart.setTouchEnabled(true);
        lineChart.setMarker(marker);
        lineChart.setXAxisRenderer(new CustomXAxisRenderer(lineChart.getViewPortHandler(), lineChart.getXAxis(), lineChart.getTransformer(YAxis.AxisDependency.LEFT)));
    }
    public void configLineChartSelection(Calendar from, Calendar to) {
        lineChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                if (e.getX() < 0 || e.getX() > num_units) return;
                lineAmt.setText(String.format(MainActivity.locale,"%.2f",e.getY()));
                highlightLineAmt(true);
                int x = (int) e.getX();
                Calendar fromNew = Calendar.getInstance();
                try {
                    fromNew.setTime((new SimpleDateFormat(getDtf(), MainActivity.locale)).parse(dates.get(x)));
                    fromNew = MainActivity.getCalendarCopy(fromNew, DateGridAdapter.FROM);
                    Calendar toNew = MainActivity.getCalendarCopy(fromNew, DateGridAdapter.TO);
                    if (selDateState == DateGridAdapter.YEAR) {
                        toNew.set(Calendar.DAY_OF_MONTH, fromNew.getActualMaximum(Calendar.DATE));
                        lineDate.setText(MainActivity.getDatetimeStr(fromNew, "MMM yyyy").toUpperCase());
                    } else
                        lineDate.setText((MainActivity.getRelativePrefix(fromNew) + MainActivity.getDatetimeStr(fromNew, ", dd MMM yyyy")).toUpperCase());
                    updateExpenseList(fromNew, toNew);
                } catch (ParseException ex) {
                    ex.printStackTrace();
                }
            }

            @Override
            public void onNothingSelected() {
                highlightLineAmt(false);
                Calendar fromAll = ((ChartsFragment) getParentFragment()).getDateRange()[0];
                Calendar toAll = ((ChartsFragment) getParentFragment()).getDateRange()[1];
                updateLineChartSummary(fromAll, toAll);
                updateExpenseList(from, to);
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
//                    Log.e(TAG, "scaleX=" + String.format("%.2f", scaleX) + ", scale=" + String.format("%.2f", scale));
//                    Log.e(TAG, "gran=" + (num_units / scale /granDivisor));
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
        expenses = MainActivity.sortExpenses(expenses, Constants.DESCENDING);
        expenses = MainActivity.insertExpDateHeaders(expenses);
        expenseAdapter = new ExpenseAdapter(getContext(), expenses, true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        expenseList.setLayoutManager(linearLayoutManager);
        expenseList.setAdapter(expenseAdapter);
    }
    public void updateAverages(Calendar from, Calendar to) {
        float avg = ((MainActivity) getActivity()).db.getDayAverage(from, to);
        avgDay.setText(String.format(MainActivity.locale,"%.2f", avg));
        avg = ((MainActivity) getActivity()).db.getWeekAverage(from, to);
        avgWeek.setText(String.format(MainActivity.locale,"%.2f", avg));
        avg = ((MainActivity) getActivity()).db.getMonthAverage(from, to);
        avgMonth.setText(String.format(MainActivity.locale,"%.2f", avg));
    }
    public Calendar updateDateRange(Calendar cal, int range, int firstDayOfWeek) {
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
    public Calendar updateDateRange(Calendar cal, int range) {
        return updateDateRange(cal, range, Calendar.SUNDAY);
    }
    public void updateExpenseList(Calendar from, Calendar to) {
        if (from == null) expenses = ((MainActivity) getParentFragment().getActivity()).db.getAllExpenses();
        else expenses = ((MainActivity) getParentFragment().getActivity()).db.getExpensesByDateRange(from, to);
        expenses = MainActivity.sortExpenses(expenses, Constants.DESCENDING);
        expenses = MainActivity.insertExpDateHeaders(expenses);
        expenseList.setAdapter(new ExpenseAdapter(getContext(), expenses, true));
    }
    public void updateLineChartSummary(Calendar from, Calendar to) {
        lineAmt.setText(String.format(MainActivity.locale,"%.2f",totalAmt));
        if (isSelRange) {
            if (selDateState == DateGridAdapter.YEAR)
                lineDate.setText((MainActivity.getDatetimeStr(from, "MMM yyyy") + " - " + MainActivity.getDatetimeStr(to, "MMM yyyy")).toUpperCase());
            else
                lineDate.setText((MainActivity.getDatetimeStr(from, "dd MMM yyyy") + " - " + MainActivity.getDatetimeStr(to, "dd MMM yyyy")).toUpperCase());
        } else {
            if (selDateState == DateGridAdapter.YEAR)
                lineDate.setText(MainActivity.getDatetimeStr(from, "yyyy").toUpperCase());
            else if (selDateState <= DateGridAdapter.WEEK)
                lineDate.setText((MainActivity.getDatetimeStr(from, "dd MMM yyyy") + " - " + MainActivity.getDatetimeStr(to, "dd MMM yyyy")).toUpperCase());
            else
                lineDate.setText(MainActivity.getDatetimeStr(from, "MMMM yyyy").toUpperCase());
        }
    }
    public void updateSelDateState() {
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
                Calendar cal = Calendar.getInstance();
                try {
                    cal.setTime((new SimpleDateFormat(getDtf(), MainActivity.locale)).parse(dates.get(x)));
                    if (selDateState == DateGridAdapter.YEAR) {
                        return MainActivity.getDatetimeStr(cal, "MMM").toUpperCase();
                    } else if (selDateState <= DateGridAdapter.WEEK) {
                        return MainActivity.getDatetimeStr(cal, "EEE").toUpperCase();
                    } else {
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
                        else if (value == 0f || value == (num_units-1))
                            return d;
                        else if ((granularity>2 && value==num_units-2) || (num_units>27 && dd>=30))
                            return "";
                        else if (dd%granularity == 0)
                            return d;
                        else
                            return "";
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                return "";
            }
        });
    }
    public String getDtf() {
        if (selDateState == DateGridAdapter.YEAR) return "MM-yyyy";
        else return "dd-MM-yyyy";
    }
    public void highlightLineAmt(boolean enable) {
        lineAmt.setTextColor(enable? ContextCompat.getColor(getContext(), R.color.red_500) :
                ContextCompat.getColor(getContext(), R.color.text_dark_gray));
    }

    // debug
    public static void logFromTo(String tag, Calendar from, Calendar to) {
        Log.e(TAG, String.format("from%s=", tag) + MainActivity.getDatetimeStr(from,"dd MMM yyyy") + String.format(", to%s=", tag) + MainActivity.getDatetimeStr(to,"dd MMM yyyy"));
    }
}
