package com.example.expensetracker.HelperClasses;

import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.renderer.XAxisRenderer;
import com.github.mikephil.charting.utils.Transformer;
import com.github.mikephil.charting.utils.Utils;
import com.github.mikephil.charting.utils.ViewPortHandler;

public class CustomXAxisRenderer extends XAxisRenderer {

    private int labelCount = 31;

    public CustomXAxisRenderer(ViewPortHandler viewPortHandler, XAxis xAxis, Transformer trans) {
        super(viewPortHandler, xAxis, trans);
    }

    @Override
    protected void computeAxisValues(float min, float max) {

        int labelCount = this.labelCount;
        double range = Math.abs(max - min);

        if (labelCount == 0 || range <= 0 || Double.isInfinite(range)) {
            mAxis.mEntries = new float[]{};
            mAxis.mCenteredEntries = new float[]{};
            mAxis.mEntryCount = 0;
            return;
        }

        // Find out how much spacing (in y value space) between axis values
        double rawInterval = range / labelCount;
        double interval = Utils.roundToNextSignificant(rawInterval);

        // If granularity is enabled, then do not allow the interval to go below specified granularity.
        // This is used to avoid repeated values when rounding values for display.
        if (mAxis.isGranularityEnabled())
            interval = interval < mAxis.getGranularity() ? mAxis.getGranularity() : interval;

        // Normalize interval
        double intervalMagnitude = Utils.roundToNextSignificant(Math.pow(10, (int) Math.log10(interval)));
        int intervalSigDigit = (int) (interval / intervalMagnitude);
        if (intervalSigDigit > 5) {
            // Use one order of magnitude higher, to avoid intervals like 0.9 or
            // 90
            interval = Math.floor(10 * intervalMagnitude);
        }

        int n = mAxis.isCenterAxisLabelsEnabled() ? 1 : 0;

        // force label count
        if (mAxis.isForceLabelsEnabled()) {

            interval = (float) range / (float) (labelCount - 1);
            mAxis.mEntryCount = labelCount;

            if (mAxis.mEntries.length < labelCount) {
                // Ensure stops contains at least numStops elements.
                mAxis.mEntries = new float[labelCount];
            }

            float v = min;

            for (int i = 0; i < labelCount; i++) {
                mAxis.mEntries[i] = v;
                v += interval;
            }

            n = labelCount;

            // no forced count
        } else {

            double first = interval == 0.0 ? 0.0 : Math.ceil(min / interval) * interval;
            if(mAxis.isCenterAxisLabelsEnabled()) {
                first -= interval;
            }

            double last = interval == 0.0 ? 0.0 : Utils.nextUp(Math.floor(max / interval) * interval);

            double f;
            int i;

            if (interval != 0.0) {
                for (f = first; f <= last; f += interval) {
                    ++n;
                }
            }

            mAxis.mEntryCount = n;

            if (mAxis.mEntries.length < n) {
                // Ensure stops contains at least numStops elements.
                mAxis.mEntries = new float[n];
            }

            for (f = first, i = 0; i < n; f += interval, ++i) {

                if (f == 0.0) // Fix for negative zero case (Where value == -0.0, and 0.0 == -0.0)
                    f = 0.0;

                mAxis.mEntries[i] = (float) f;
            }
        }

        // set decimals
        if (interval < 1) {
            mAxis.mDecimals = (int) Math.ceil(-Math.log10(interval));
        } else {
            mAxis.mDecimals = 0;
        }

        if (mAxis.isCenterAxisLabelsEnabled()) {

            if (mAxis.mCenteredEntries.length < n) {
                mAxis.mCenteredEntries = new float[n];
            }

            float offset = (float)interval / 2f;

            for (int i = 0; i < n; i++) {
                mAxis.mCenteredEntries[i] = mAxis.mEntries[i] + offset;
            }
        }
    }

    public void setLabelCount(int labelCount) {
        this.labelCount = labelCount;
    }
}
