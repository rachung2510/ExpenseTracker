package com.example.expensetracker.HelperClasses;

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.TextView;

import com.example.expensetracker.MainActivity;
import com.example.expensetracker.R;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;

/**
 * Adapter from rafaelasguerra and Andrea Scalabrini - https://stackoverflow.com/questions/33241952/
 */
@SuppressLint("ViewConstructor")
public class CustomMarkerView extends MarkerView {

    private final TextView tvContent;
    private MPPointF mOffset;

    public CustomMarkerView(Context context, int layoutResource) {
        super(context, layoutResource);
        tvContent = findViewById(R.id.tvContent);
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        tvContent.setText(String.format(MainActivity.locale, "%.2f", e.getY()));
        tvContent.setVisibility(GONE);
        super.refreshContent(e, highlight); // this will perform necessary layouting
    }

    @Override
    public MPPointF getOffset() {
        if(mOffset == null)
            mOffset = new MPPointF(-((float) getWidth() / 2), 24-getHeight());
        return mOffset;
    }}
