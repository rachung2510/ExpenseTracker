package com.example.expensetracker.ChartsPage;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensetracker.Category;
import com.example.expensetracker.Constants;
import com.example.expensetracker.HelperClasses.CustomPieChartRenderer;
import com.example.expensetracker.MainActivity;
import com.example.expensetracker.R;
import com.example.expensetracker.RecyclerViewAdapters.CatDataAdapter;
import com.example.expensetracker.RecyclerViewAdapters.DateGridAdapter;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

public class ChartsChildFragmentPie extends ChartsChildFragment {

    private static final String TAG = "ChartsChildFragmentPie";

    private float totalAmt = 0;

    private PieChart pieChart;
    ImageView pieIcon;
    TextView pieLabel, pieAmt;
    RecyclerView catDataGrid;
    CatDataAdapter catDataAdapter;
    ArrayList<Category> pieCategories = new ArrayList<>();
    HashMap<String,Integer> pieCategoriesMap = new HashMap<>();
    private Calendar fromCal, toCal;

    public ChartsChildFragmentPie() {
        super();
        this.chartType = ChartsChildFragment.TYPE_PIECHART;
    }
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (getActivity() == null || getParentFragment() == null)
            return null;
        view = inflater.inflate(R.layout.fragment_charts_piechart, container, false);
        pieChart = view.findViewById(R.id.pieChart);
        pieIcon = view.findViewById(R.id.pieIcon);
        pieLabel = view.findViewById(R.id.pieLabel);
        pieAmt = view.findViewById(R.id.pieAmt);
        catDataGrid = view.findViewById(R.id.catDataGrid);
        String currencySymbol = ((MainActivity) getActivity()).getDefaultCurrencySymbol();
        ((TextView) view.findViewById(R.id.summaryCurrency)).setText(currencySymbol);

        pieIcon.setVisibility(ImageView.GONE);
        loadPieChartData();
        setupPieChart();
        configPieChartSelection();
        configPieChartRecyclerView();

        if (getActivity() != null)
            ((MainActivity) getActivity()).updateSummaryData(Constants.CHARTS);
        return view;
    }

    @Override
    public void invalidateMenu() {
        if (getParentFragment() == null) return;
        Toolbar toolbar = ((ChartsFragment) getParentFragment()).getToolbar();
        toolbar.getMenu().clear();
    }
    @Override
    public void updateDateFilters() {
        Calendar from = Calendar.getInstance();
        Calendar to = Calendar.getInstance();
        if (getParentFragment() != null) {
            from = ((ChartsFragment) getParentFragment()).getDateRange()[0];
            to = ((ChartsFragment) getParentFragment()).getDateRange()[1];
        }
        fromCal = MainActivity.getCalendarCopy(from, DateGridAdapter.FROM);
        toCal = MainActivity.getCalendarCopy(to, DateGridAdapter.TO);
        long tic = System.currentTimeMillis();
        loadPieChartData();
        long tic1 = System.currentTimeMillis();
        catDataAdapter = new CatDataAdapter(getParentFragment().getActivity(), fromCal, toCal);
        long tic2 = System.currentTimeMillis();
        catDataGrid.setAdapter(catDataAdapter);
        long toc = System.currentTimeMillis();
//        Log.e(TAG,"loadPieChartData="+(tic1-tic));
//        Log.e(TAG,"newAdapter="+(tic2-tic1));
//        Log.e(TAG,"setAdapter="+(toc-tic2));

    }
    @Override
    public void updateCurrency(String curr) {
        ((TextView) view.findViewById(R.id.summaryCurrency)).setText(curr);
    }

    /**
     * Piechart functions
     */
    public void loadPieChartData() {
        if (getActivity() == null)
            return;
        ArrayList<PieEntry> pieEntries = new ArrayList<>();
        pieCategories = new ArrayList<>();
        ArrayList<Category> categories = ((MainActivity) getActivity()).db.getAllCategories();
        ArrayList<Integer> colors = new ArrayList<>();
        int count = 0;
        for (Category cat : categories) {
            float amt;
            if (fromCal == null) amt = ((MainActivity) getActivity()).db.getConvertedTotalAmtByCategory(cat);
            else amt = ((MainActivity) getActivity()).db.getConvertedTotalAmtByCategoryInDateRange(cat, fromCal, toCal);
            if (amt != 0f) {
                pieEntries.add(new PieEntry(amt, cat.getIcon()));
                colors.add(ContextCompat.getColor(getActivity(), cat.getColorId()));
                pieCategories.add(cat);
                pieCategoriesMap.put(cat.getName(), count);
                count++;
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
                clearPieHighlights();
            }
        });
        pieChart.invalidate();
    }
    public void configPieChartRecyclerView() {
        if (getActivity() == null)
            return;
        catDataAdapter = new CatDataAdapter(getActivity(), fromCal, toCal);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity(), 2, GridLayoutManager.VERTICAL, false);
        catDataGrid.setLayoutManager(gridLayoutManager);
        catDataGrid.setAdapter(catDataAdapter);
//        return new GridLayoutManager(getActivity(), 2, GridLayoutManager.VERTICAL, false);
    }
    public void setPieChartTotalAmt(float totalAmt) {
        this.totalAmt = totalAmt;
        pieAmt.setText(String.format(MainActivity.locale, "%.2f", this.totalAmt));
    }
    public void setPieChartTotalAmt(String totalAmt) {
        this.totalAmt = Float.parseFloat(totalAmt);
        pieAmt.setText(totalAmt);
    }
    public boolean isPieHighlighted(String name) {
        Highlight[] highlights = pieChart.getHighlighted();
        if (highlights != null) {
            int x = (int) highlights[0].getX();
            return pieCategoriesMap.get(name) == x;
        }
        return false;
    }
    public void highlightPieValue(String name) {
        float x = (float) pieCategoriesMap.get(name);
        pieChart.highlightValue(x,0);
    }
    public void clearPieHighlights() {
        pieIcon.setVisibility(ImageButton.GONE);
        pieLabel.setText(getString(R.string.EXPS));
        pieLabel.setTypeface(ResourcesCompat.getFont(getActivity(), R.font.roboto_medium));
        pieAmt.setText(String.format(MainActivity.locale,"%.2f", totalAmt));
        pieChart.highlightValues(null);
    }

}
