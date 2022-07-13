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

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensetracker.Category;
import com.example.expensetracker.HelperClasses.CustomPieChartRenderer;
import com.example.expensetracker.MainActivity;
import com.example.expensetracker.R;
import com.example.expensetracker.RecyclerViewAdapters.CatDataAdapter;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import java.util.ArrayList;

public class ChartsChildFragment extends Fragment {

    private final int chartType;

    // Piechart components
    private PieChart pieChart;
    private float totalAmt;
    ImageView pieIcon;
    TextView pieLabel, pieAmt;
    RecyclerView catDataGrid;
    CatDataAdapter catDataAdapter;
    ArrayList<Category> pieCategories = new ArrayList<>();

    public static final int TYPE_PIECHART = 0;
    public static final int TYPE_GRAPH = 1;
    public static final int TYPE_CALENDAR = 2;

    public ChartsChildFragment(int chartType) {
        this.chartType = chartType;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = null;
        switch (chartType) {
            case TYPE_PIECHART:
                view = inflater.inflate(R.layout.fragment_charts_piechart, container, false);
                pieChart = view.findViewById(R.id.pieChart);
                pieIcon = view.findViewById(R.id.pieIcon);
                pieLabel = view.findViewById(R.id.pieLabel);
                pieAmt = view.findViewById(R.id.pieAmt);
                catDataGrid = view.findViewById(R.id.catDataGrid);

                pieIcon.setVisibility(ImageView.GONE);
                totalAmt = ((MainActivity) getActivity()).db.getTotalAmt();
                pieAmt.setText(String.format(MainActivity.locale, "%.2f", totalAmt));
                loadPieChartData();
                setupPieChart();
                configPieChartSelection();
                configRecyclerView();
                break;

            case TYPE_GRAPH:
                view = inflater.inflate(R.layout.fragment_charts_graph, container, false);
                ((TextView) view.findViewById(R.id.textView)).setText("Graph segment");
                break;

            case TYPE_CALENDAR:
                view = inflater.inflate(R.layout.fragment_charts_graph, container, false);
                ((TextView) view.findViewById(R.id.textView)).setText("Calendar segment");
                break;
        }
        return view;
    }

    /**
     * LOCAL METHODS
     */
    /**
     * Piechart functions
     */
    public void loadPieChartData() {
        ArrayList<PieEntry> pieEntries = new ArrayList<>();
        ArrayList<Category> categories = ((MainActivity) getActivity()).db.getAllCategories();
        ArrayList<Integer> colors = new ArrayList<>();
        for (Category cat : categories) {
            float amt = ((MainActivity) getActivity()).db.getTotalAmtByCategory(cat);
            if (amt != 0f) {
                pieEntries.add(new PieEntry(amt, cat.getIcon()));
                colors.add(ContextCompat.getColor(getActivity(), cat.getColorId()));
                pieCategories.add(cat);
            }
        }

        PieDataSet dataSet = new PieDataSet(pieEntries, "aa");
        dataSet.setColors(colors);
        dataSet.setSliceSpace(2f);
        dataSet.setValueTextColors(colors);
        dataSet.setSelectionShift(4f);

        // Value lines
        dataSet.setValueLinePart1Length(0.6f);
        dataSet.setValueLinePart2Length(0.3f);
        dataSet.setValueLineWidth(2f);
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
                pieLabel.setText("EXPENSES");
                pieLabel.setTypeface(ResourcesCompat.getFont(getParentFragment().getActivity(), R.font.roboto_medium));
                pieAmt.setText(String.format(MainActivity.locale,"%.2f",totalAmt));
            }
        });
        pieChart.invalidate();
    }
    public void configRecyclerView() {
        catDataAdapter = new CatDataAdapter(getParentFragment().getActivity());
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getParentFragment().getActivity(), 2, GridLayoutManager.VERTICAL, false);
        catDataGrid.setLayoutManager(gridLayoutManager);
        catDataGrid.setAdapter(catDataAdapter);
    }

}
