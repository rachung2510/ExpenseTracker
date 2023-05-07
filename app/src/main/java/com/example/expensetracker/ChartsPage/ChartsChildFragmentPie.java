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
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensetracker.Category;
import com.example.expensetracker.Constants;
import com.example.expensetracker.HelperClasses.CustomPieChartRenderer;
import com.example.expensetracker.MainActivity;
import com.example.expensetracker.R;
import com.example.expensetracker.RecyclerViewAdapters.CatDataAdapter;
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

    // Main components
    private float totalAmt = 0;
    private ArrayList<Category> categories;
    private final HashMap<String,Integer> categoryNumExpMap = new HashMap<>();

    // View components
    private PieChart pieChart;
    private ImageView pieIcon;
    private TextView pieLabel, pieAmt;
    private RecyclerView catDataGrid;
    private CatDataAdapter catDataAdapter;
    private GridLayoutManager gridLayoutManager;

    // Others
    private boolean scrollToCat = true;

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
        configurePieChartSelection();
        configurePieChartRecyclerView();

        if (getActivity() != null)
            ((MainActivity) getActivity()).updateSummaryData(Constants.CHARTS);

        return view;
    }

    @Override
    protected void invalidateMenu() {
        if (getParentFragment() == null) return;
        Toolbar toolbar = ((ChartsFragment) getParentFragment()).getToolbar();
        toolbar.getMenu().clear();
    }
    @Override
    protected void updateDateRange() {
        Calendar fromDate = ((ChartsFragment) getParentFragment()).getDateRange()[0];
        Calendar toDate = ((ChartsFragment) getParentFragment()).getDateRange()[1];
        loadPieChartData();
        if (fromDate == null)
            categories = ((MainActivity) getActivity()).db.getSortedCategoriesByConvertedTotalAmt();
        else
            categories = ((MainActivity) getActivity()).db.getSortedCategoriesByConvertedTotalAmtInDateRange(fromDate, toDate);
        catDataAdapter = new CatDataAdapter(getActivity(), categories);
        catDataGrid.setAdapter(catDataAdapter);
    }
    @Override
    protected void updateCurrency(String curr) {
        ((TextView) view.findViewById(R.id.summaryCurrency)).setText(curr);
    }

    /**
     * Functions
     */
    private void loadPieChartData() {
        if (getActivity() == null)
            return;
        ArrayList<PieEntry> pieEntries = new ArrayList<>();
        ArrayList<Integer> colors = new ArrayList<>();
        int count = 0;
        Calendar fromDate = ((ChartsFragment) getParentFragment()).getDateRange()[0];
        Calendar toDate = ((ChartsFragment) getParentFragment()).getDateRange()[1];
        if (fromDate == null)
            categories = ((MainActivity) getActivity()).db.getSortedCategoriesByConvertedTotalAmt();
        else
            categories = ((MainActivity) getActivity()).db.getSortedCategoriesByConvertedTotalAmtInDateRange(fromDate, toDate);
        for (Category cat : categories) {
            float amt = cat.getAmount();
            if (amt == 0f) continue;
            pieEntries.add(new PieEntry(amt, cat.getIcon()));
            colors.add(MainActivity.getColorFromId(getActivity(), cat.getColorId()));
            categoryNumExpMap.put(cat.getName(), count);
            count++;
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
    private void setupPieChart() {
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
    private void configurePieChartSelection() {
        pieChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                if (getActivity() == null)
                    return;
                if (e == null) return;
                pieAmt.setText(String.format(MainActivity.locale,"%.2f",e.getY()));
                pieIcon.setVisibility(ImageButton.VISIBLE);
                int pos = (int) h.getX();
                Category cat =  categories.get(pos);
                pieIcon.setForeground(MainActivity.getIconFromId(getActivity(), cat.getIconId()));
                pieLabel.setText(cat.getName());
                pieLabel.setTypeface(ResourcesCompat.getFont(getActivity(), R.font.roboto_regular));
                if (scrollToCat) gridLayoutManager.scrollToPosition(pos);
                scrollToCat = true;
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
    private void configurePieChartRecyclerView() {
        if (getActivity() == null)
            return;
        catDataAdapter = new CatDataAdapter(getActivity(), categories);
        gridLayoutManager = new GridLayoutManager(getActivity(), 2, GridLayoutManager.VERTICAL, false);
        catDataGrid.setLayoutManager(gridLayoutManager);
        catDataGrid.setAdapter(catDataAdapter);
    }

    /**
     * Getters & Setters
     */
    public void setPieChartTotalAmt(float totalAmt) {
        this.totalAmt = totalAmt;
        pieAmt.setText(String.format(MainActivity.locale, "%.2f", this.totalAmt));
    }
    public void setPieChartTotalAmt(String totalAmt) {
        this.totalAmt = Float.parseFloat(totalAmt);
        pieAmt.setText(totalAmt);
    }

    /**
     * Others
     */
    public boolean isPieHighlighted(String name) {
        Highlight[] highlights = pieChart.getHighlighted();
        if (highlights != null) {
            int x = (int) highlights[0].getX();
            return categoryNumExpMap.get(name) == x;
        }
        return false;
    }
    public void highlightPieValue(String name) {
        float x = (float) categoryNumExpMap.get(name);
        scrollToCat = false; // don't scroll when highlighted from clicking on CatData
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
