package com.example.expensetracker.HelperClasses;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import androidx.core.graphics.drawable.DrawableCompat;

import com.github.mikephil.charting.animation.ChartAnimator;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.IPieDataSet;
import com.github.mikephil.charting.renderer.PieChartRenderer;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.utils.MPPointF;
import com.github.mikephil.charting.utils.Utils;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.util.List;

public class CustomPieChartRenderer extends PieChartRenderer {

    private final float circleRadius;

    public CustomPieChartRenderer(PieChart chart, float circleRadius, ChartAnimator animator, ViewPortHandler viewPortHandler) {
        super(chart, animator, viewPortHandler);
        this.circleRadius = circleRadius;
    }

    @Override
    public void drawValues(Canvas c) {
        super.drawValues(c);

        MPPointF center = mChart.getCenterCircleBox();

        // get whole the radius
        float radius = mChart.getRadius();
        float rotationAngle = mChart.getRotationAngle();
        float[] drawAngles = mChart.getDrawAngles();
        float[] absoluteAngles = mChart.getAbsoluteAngles();

        float phaseX = mAnimator.getPhaseX();
        float phaseY = mAnimator.getPhaseY();

        final float roundedRadius = (radius - (radius * mChart.getHoleRadius() / 100f)) / 2f;
        final float holeRadiusPercent = mChart.getHoleRadius() / 100.f;
        float labelRadiusOffset = radius / 10f * 3.6f;

        if (mChart.isDrawHoleEnabled()) {
            labelRadiusOffset = (radius - (radius * holeRadiusPercent)) / 2f;

            if (!mChart.isDrawSlicesUnderHoleEnabled() && mChart.isDrawRoundedSlicesEnabled()) {
                // Add curved circle slice and spacing to rotation angle, so that it sits nicely inside
                rotationAngle += roundedRadius * 360 / (Math.PI * 2 * radius);
            }
        }

        final float labelRadius = radius - labelRadiusOffset;

        PieData data = mChart.getData();
        List<IPieDataSet> dataSets = data.getDataSets();

        float yValueSum = data.getYValueSum();

        boolean drawEntryLabels = mChart.isDrawEntryLabelsEnabled();

        float angle;
        int xIndex = 0;

        c.save();

        float offset = Utils.convertDpToPixel(15.f);

        for (int i = 0; i < dataSets.size(); i++) {

            IPieDataSet dataSet = dataSets.get(i);

            float lineHeight = Utils.calcTextHeight(mValuePaint, "Q")
                    + Utils.convertDpToPixel(4f);

            IValueFormatter formatter = dataSet.getValueFormatter();

            final float sliceSpace = getSliceSpace(dataSet);

            for (int j = 0; j < dataSet.getEntryCount(); j++) {
                PieEntry entry = dataSet.getEntryForIndex(j);
                if (xIndex == 0) angle = 0.f;
                else angle = absoluteAngles[xIndex - 1] * phaseX;
                final float sliceAngle = drawAngles[xIndex];
                final float sliceSpaceMiddleAngle = sliceSpace / (Utils.FDEG2RAD * labelRadius);
                final float angleOffset = (sliceAngle - sliceSpaceMiddleAngle / 2.f) / 2.f;
                angle = angle + angleOffset;

//                if (dataSet.getValueLineColor() != ColorTemplate.COLOR_NONE) {
                final float transformedAngle = rotationAngle + angle * phaseY;

                float value = mChart.isUsePercentValuesEnabled() ? entry.getY()
                        / yValueSum * 100f : entry.getY();

                final float sliceXBase = (float) Math.cos(transformedAngle * Utils.FDEG2RAD);
                final float sliceYBase = (float) Math.sin(transformedAngle * Utils.FDEG2RAD);

                final float valueLineLength1 = dataSet.getValueLinePart1Length();
                final float valueLineLength2 = dataSet.getValueLinePart2Length();
                final float valueLinePart1OffsetPercentage = dataSet.getValueLinePart1OffsetPercentage() / 100.f;

                float pt2x, pt2y;
                float labelPtx, labelPty;

                float line1Radius;

                if (mChart.isDrawHoleEnabled())
                    line1Radius = (radius - (radius * holeRadiusPercent))
                            * valueLinePart1OffsetPercentage
                            + (radius * holeRadiusPercent);
                else
                    line1Radius = radius * valueLinePart1OffsetPercentage;

                final float polyline2Width = dataSet.isValueLineVariableLength()
                        ? labelRadius * valueLineLength2 * (float) Math.abs(Math.sin(
                        transformedAngle * Utils.FDEG2RAD))
                        : labelRadius * valueLineLength2;

                final float pt0x = line1Radius * sliceXBase + center.x;
                final float pt0y = line1Radius * sliceYBase + center.y;
                if (dataSet.isUsingSliceColorAsValueLineColor()) {
                    mRenderPaint.setColor(dataSet.getColor(j));
                }
                c.drawCircle(pt0x, pt0y, circleRadius, mRenderPaint);

                final float pt1x = labelRadius * (1 + valueLineLength1) * sliceXBase + center.x;
                final float pt1y = labelRadius * (1 + valueLineLength1) * sliceYBase + center.y;

                if (transformedAngle % 360.0 >= 90.0 && transformedAngle % 360.0 <= 270.0) {
                    pt2x = pt1x - polyline2Width;
                    pt2y = pt1y;
                    labelPtx = pt2x - offset;
                } else {
                    pt2x = pt1x + polyline2Width;
                    pt2y = pt1y;
                    labelPtx = pt2x + offset;
                }
                labelPty = pt2y;

                // draw lines
                int lineColor = ColorTemplate.COLOR_NONE;

                lineColor = dataSet.getColor(j);

                mValueLinePaint.setColor(lineColor);
                c.drawLine(pt0x, pt0y, pt1x, pt1y, mValueLinePaint);
                c.drawLine(pt1x, pt1y, pt2x, pt2y, mValueLinePaint);

                // draw icons
                if (entry.getIcon() != null) {
                    Drawable icon = entry.getIcon();
                    Drawable wrappedIcon = DrawableCompat.wrap(icon);
                    DrawableCompat.setTint(wrappedIcon, dataSet.getColor(j));
                    Utils.drawImage(
                            c,
                            icon,
                            (int) labelPtx,
                            (int) labelPty,
                            100,
                            100);
                }

                dataSet.setDrawValues(false);
                dataSet.setDrawIcons(false);

//                }
                xIndex++;
            }
        }
        MPPointF.recycleInstance(center);
        c.restore();

    }
}
