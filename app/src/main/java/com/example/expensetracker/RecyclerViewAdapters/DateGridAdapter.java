package com.example.expensetracker.RecyclerViewAdapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensetracker.MainActivity;
import com.example.expensetracker.R;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

public class DateGridAdapter extends RecyclerView.Adapter<DateGridAdapter.ViewHolder>{

    private static final String TAG = "DateGridAdapter";
    private final ArrayList<String> filterDateIconNames;
    private final ArrayList<String> filterDateNames;
    private final ColorStateList iconGray, iconLightGray, iconWhite, bgSelectOrange;

    private final Context context;
    private final LayoutInflater inflater;
    private int selectedPos;
    private int state;
    public boolean errorState = false;
    private boolean[] disabledPos = { false,false,false,false,false,false };

    // components
    private Calendar fromDate, toDate;
    private TextView fromDayTextView, toDayTextView;
    private final DatePicker fromDayPicker, toDayPicker;
    private AlertDialog parentDialog, selDateDialog;
    private MaterialButtonToggleGroup selToggle;

    // constants
    public final static int DAY = 0;
    public final static int WEEK = 1;
    public final static int MONTH = 2;
    public final static int YEAR = 3;
    public final static int ALL = 4;
    public final static int SELECT_RANGE = 5;
    public final static int SELECT_SINGLE = 6;
    public final static int FROM = 0;
    public final static int TO = 1;
    public final static String[] months = new String[] { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };

    /**
     * CONSTRUCTOR
     */
    public DateGridAdapter(Context context, int[] args, Calendar fromDate, Calendar toDate) {
        String[] icon_names = new String[] { "date_day", "date_week", "date_month", "date_year", "date_infinity", "date_calendar_event" };
        String[] date_names = new String[] { "Today", "Week", "Month", "Year", "All time", "Select" };
        filterDateIconNames = new ArrayList<>(Arrays.asList(icon_names));
        filterDateNames = new ArrayList<>(Arrays.asList(date_names));

        this.context = context;
        this.inflater = LayoutInflater.from(context);
        selectedPos = args[0];
        state = args[1];

        this.fromDate = fromDate;
        this.toDate = toDate;
        fromDayPicker = new DatePicker(context);
        toDayPicker = new DatePicker(context);

        iconGray = MainActivity.getColorStateListFromId(context, R.color.generic_icon_gray);
        iconLightGray = MainActivity.getColorStateListFromId(context, R.color.tag_bg_gray);
        iconWhite = MainActivity.getColorStateListFromId(context, R.color.white);
        bgSelectOrange = MainActivity.getColorStateListFromId(context, R.color.orange_500);
    }

    /**
     * INITIALISE ADAPTER
     */
    @NonNull
    @Override
    public DateGridAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_date_grid, parent, false);
        return new DateGridAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DateGridAdapter.ViewHolder holder, int position) {
        int pos = holder.getAdapterPosition();
        holder.filterDateIcon.setForeground(MainActivity.getIconFromName(context, filterDateIconNames.get(pos)));
        Calendar now = Calendar.getInstance(MainActivity.locale);
        if (pos == 2) {
            holder.filterDateName.setText(now.getDisplayName(Calendar.MONTH, Calendar.LONG, MainActivity.locale));
        } else if (pos == 3) {
            holder.filterDateName.setText(String.valueOf(now.get(Calendar.YEAR)));
        } else {
            holder.filterDateName.setText(filterDateNames.get(pos));
        }
//            holder.itemView.setOnClickListener(view -> Toast.makeText(context, filterDateNames.get(pos), Toast.LENGTH_SHORT).show());

        if (pos < 5 && pos == selectedPos) holder.select();
        else if (pos == 5 && selectedPos >= 5) holder.select();
        else holder.deselect(pos);
        if (!disabledPos[pos])
            holder.itemView.setOnClickListener(view -> {
            int oldPos = selectedPos;
            if ((pos<5 && pos!=selectedPos) || (pos>=5 && selectedPos<5))
                selectedPos = pos;
            notifyItemChanged(oldPos);
            notifyItemChanged(selectedPos);
            updateState();

            // select dialog
            if (selectedPos >= 5)
                showSelDateDialog();
            else {
                errorState = false;
                fromDate = getInitSelectedDates(FROM, state);
                toDate = getInitSelectedDates(TO, state);
                if (parentDialog != null) parentDialog.dismiss();
            }
        });
    }

    @Override
    public int getItemCount() {
        return filterDateNames.size();
    }

    /**
     * VIEWHOLDER CLASS
     */
    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView filterDateIcon;
        TextView filterDateName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            filterDateIcon = itemView.findViewById(R.id.filterDateIcon);
            filterDateName = itemView.findViewById(R.id.filterDateName);
        }

        public void select() {
            itemView.setBackgroundTintList(bgSelectOrange);
            filterDateIcon.setForegroundTintList(iconWhite);
            filterDateName.setTextColor(ContextCompat.getColor(context, R.color.white));
        }

        public void deselect(int pos) {
            itemView.setBackgroundTintList(null);
            filterDateIcon.setForegroundTintList(disabledPos[pos] ? iconLightGray : iconGray);
            filterDateName.setTextColor(disabledPos[pos] ? ContextCompat.getColor(context, R.color.tag_bg_gray) : ContextCompat.getColor(context, R.color.text_mid_gray));
        }
    }

    /**
     * FUNCTIONS
     */
    public void showSelDateDialog() {
        AlertDialog.Builder selectDate = new AlertDialog.Builder(context);
        final View datePicker = inflater.inflate(R.layout.dialog_date_select, null);

        // blocks
        LinearLayout selDayBlock = datePicker.findViewById(R.id.selDayBlock);
        LinearLayout selMonthYearBlock = datePicker.findViewById(R.id.selMonthYearBlock);
        LinearLayout selDayRange = datePicker.findViewById(R.id.selDayRange);
        LinearLayout toSel = datePicker.findViewById(R.id.toSel); // to block for month/year

        // components
        TextView fromHeader = datePicker.findViewById(R.id.fromHeader);
        selToggle = datePicker.findViewById(R.id.selToggle);
        SwitchCompat toggleRange = datePicker.findViewById(R.id.toggleRange);
        fromDayTextView = datePicker.findViewById(R.id.fromDayTextView);
        toDayTextView = datePicker.findViewById(R.id.toDayTextView);

        // select
        DatePicker selDayPicker = datePicker.findViewById(R.id.selDayPicker);

        // range
        LinearLayout fromDay = datePicker.findViewById(R.id.fromDay);
        LinearLayout toDay = datePicker.findViewById(R.id.toDay);
        NumberPicker fromMonth = datePicker.findViewById(R.id.fromMonthPicker);
        NumberPicker fromYear = datePicker.findViewById(R.id.fromYearPicker);
        NumberPicker toMonth  = datePicker.findViewById(R.id.toMonthPicker);
        NumberPicker toYear  = datePicker.findViewById(R.id.toYearPicker);

        // Toggle behaviours
        selToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                switch (checkedId) {
                    case R.id.toggleDay:
                        selDayBlock.setVisibility(LinearLayout.VISIBLE);
                        selMonthYearBlock.setVisibility(LinearLayout.GONE);
                        break;
                    case R.id.toggleMonth:
                        selDayBlock.setVisibility(LinearLayout.GONE);
                        selMonthYearBlock.setVisibility(LinearLayout.VISIBLE);
                        fromMonth.setVisibility(NumberPicker.VISIBLE);
                        fromYear.setVisibility(NumberPicker.VISIBLE);
                        toMonth.setVisibility(NumberPicker.VISIBLE);
                        toYear.setVisibility(NumberPicker.VISIBLE);
                        break;
                    case R.id.toggleYear:
                        selDayBlock.setVisibility(LinearLayout.GONE);
                        selMonthYearBlock.setVisibility(LinearLayout.VISIBLE);
                        fromMonth.setVisibility(NumberPicker.GONE);
                        fromYear.setVisibility(NumberPicker.VISIBLE);
                        toMonth.setVisibility(NumberPicker.GONE);
                        toYear.setVisibility(NumberPicker.VISIBLE);
                        break;
                }
            }
        });
        if (state == MONTH) selToggle.check(R.id.toggleMonth);
        else if (state == YEAR) selToggle.check(R.id.toggleYear);
        else selToggle.check(R.id.toggleDay);
        toggleRange.setOnCheckedChangeListener((compoundButton, b) -> {
            if (b) {
                selDayRange.setVisibility(LinearLayout.VISIBLE);
                selDayPicker.setVisibility(DatePicker.GONE);
                toSel.setVisibility(LinearLayout.VISIBLE);
                fromHeader.setVisibility(TextView.VISIBLE);
                selectedPos = SELECT_RANGE;
            } else {
                selDayRange.setVisibility(LinearLayout.GONE);
                selDayPicker.setVisibility(DatePicker.VISIBLE);
                toSel.setVisibility(LinearLayout.GONE);
                fromHeader.setVisibility(TextView.GONE);
                selectedPos = SELECT_SINGLE;
            }
        });
        if (selectedPos == SELECT_SINGLE) toggleRange.setChecked(false);

        // Initialisation
        Calendar from = fromDate;
        Calendar to = toDate;
        if (from == null) {
            from = getInitSelectedDates(FROM, MONTH);
            to = getInitSelectedDates(TO, MONTH);
            fromDate = MainActivity.getCalendarCopy(from, FROM);
            toDate = MainActivity.getCalendarCopy(to, TO);
        }

        // Set values for Day
        selDayPicker.updateDate(from.get(Calendar.YEAR), from.get(Calendar.MONTH), from.get(Calendar.DAY_OF_MONTH));
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", MainActivity.locale);
        fromDayTextView.setText(sdf.format(from.getTime()).toUpperCase());
        toDayTextView.setText(sdf.format(to.getTime()).toUpperCase());
        fromDayPicker.updateDate(from.get(Calendar.YEAR), from.get(Calendar.MONTH), from.get(Calendar.DAY_OF_MONTH));
        toDayPicker.updateDate(to.get(Calendar.YEAR), to.get(Calendar.MONTH), to.get(Calendar.DAY_OF_MONTH));
        fromDay.setOnClickListener(view -> showSelDayRangeDialog(FROM));
        toDay.setOnClickListener(view -> showSelDayRangeDialog(TO));
        selDayPicker.updateDate(from.get(Calendar.YEAR), from.get(Calendar.MONTH), from.get(Calendar.DAY_OF_MONTH));

        // Set values for NumberPickers
        Calendar[] pair = ((MainActivity) context).db.getFirstLastDates();
        int minYear = (pair[0] == null) ? getInitSelectedDates(FROM, YEAR).get(Calendar.YEAR) : pair[0].get(Calendar.YEAR);
        int maxYear = (pair[0] == null) ? minYear : pair[1].get(Calendar.YEAR);
        for (NumberPicker picker : new NumberPicker[] { fromYear, toYear }) {
            picker.setMinValue(minYear);
            picker.setMaxValue(maxYear);
            picker.setWrapSelectorWheel(false);
        }
        fromYear.setValue(from.get(Calendar.YEAR));
        toYear.setValue(to.get(Calendar.YEAR));
        for (NumberPicker picker : new NumberPicker[] { fromMonth, toMonth }) {
            picker.setDisplayedValues(months);
            picker.setMinValue(0);
            picker.setMaxValue(months.length - 1);
            picker.setWrapSelectorWheel(false);
        }
        fromMonth.setValue(from.get(Calendar.MONTH));
        toMonth.setValue(to.get(Calendar.MONTH));

        // dialog builder
        selectDate.setView(datePicker);
        selectDate.setPositiveButton(android.R.string.yes, (dialogInterface, i) -> {
        });
        selectDate.setNeutralButton(android.R.string.no, (dialog, which) -> {
            dialog.cancel();
            errorState = true;
        });

        // dialog
        selDateDialog = selectDate.create();
        selDateDialog.show();
        selDateDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            switch (selToggle.getCheckedButtonId()) {
                case R.id.toggleDay:
                    fromDate.set(fromDayPicker.getYear(), fromDayPicker.getMonth(), fromDayPicker.getDayOfMonth(), 0,0,0);
                    toDate.set(toDayPicker.getYear(), toDayPicker.getMonth(), toDayPicker.getDayOfMonth(), 23, 59, 59);
                    break;
                case R.id.toggleMonth:
                    fromDate.set(fromYear.getValue(), fromMonth.getValue(), 1, 0, 0, 0);
                    toDate.set(toYear.getValue(), toMonth.getValue(), 1, 23, 59, 59);
                    toDate.set(Calendar.DAY_OF_MONTH, toDate.getActualMaximum(Calendar.DATE));
                    break;
                case R.id.toggleYear:
                    fromDate.set(fromYear.getValue(), 0, 1, 0, 0, 0);
                    toDate.set(toYear.getValue(), 11, 1, 23, 59, 59);
                    toDate.set(Calendar.DAY_OF_MONTH, toDate.getActualMaximum(Calendar.DATE));
                    break;
            }
            if (toggleRange.isChecked()) { // range
                if (validateDateRange(fromDate, toDate)) {
                    errorState = false;
                    selDateDialog.dismiss();
                    if (parentDialog != null) parentDialog.dismiss();
                } else {
                    errorState = true;
                    Toast.makeText(context, "Start date must be before end date", Toast.LENGTH_SHORT).show();
                }
            } else { // single
                errorState = false;
                switch (selToggle.getCheckedButtonId()) {
                    case R.id.toggleDay:
                        fromDate.set(selDayPicker.getYear(), selDayPicker.getMonth(), selDayPicker.getDayOfMonth(), 0, 0, 0);
                        toDate.set(selDayPicker.getYear(), selDayPicker.getMonth(), selDayPicker.getDayOfMonth());
                        break;
                    case R.id.toggleMonth:
                        toDate.set(Calendar.MONTH, fromDate.get(Calendar.MONTH));
                        toDate.set(Calendar.YEAR, fromDate.get(Calendar.YEAR));
                        break;
                    case R.id.toggleYear:
                        toDate.set(Calendar.YEAR, fromDate.get(Calendar.YEAR));
                        break;
                }
                toDate.set(toDate.get(Calendar.YEAR), toDate.get(Calendar.MONTH), toDate.get(Calendar.DAY_OF_MONTH), 23, 59, 59);
                if (parentDialog != null) parentDialog.dismiss();
                selDateDialog.dismiss();
            }
        });
    }
    public void showSelDayRangeDialog(int range) {
        Calendar cal = Calendar.getInstance();
        AlertDialog.Builder selDayRangeDialog = new AlertDialog.Builder(context);
        DatePicker datePicker = (range == FROM) ? fromDayPicker : toDayPicker;
        selDayRangeDialog.setView(datePicker);
        selDayRangeDialog.setPositiveButton(android.R.string.yes, (dialogInterface, i) -> {
            cal.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth());
            String newDate = new SimpleDateFormat("dd MMM yyyy", MainActivity.locale).format(cal.getTime()).toUpperCase();
            if (range == FROM) {
                fromDate.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth());
                fromDayTextView.setText(newDate);
            } else if (range == TO) {
                toDate.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth());
                toDayTextView.setText(newDate);
            }
        });
        selDayRangeDialog.setNeutralButton(android.R.string.no, (dialog, which) -> {
            if (range == FROM)
                datePicker.updateDate(fromDate.get(Calendar.YEAR), fromDate.get(Calendar.MONTH), fromDate.get(Calendar.DAY_OF_MONTH));
            else if (range == TO)
                datePicker.updateDate(toDate.get(Calendar.YEAR), toDate.get(Calendar.MONTH), toDate.get(Calendar.DAY_OF_MONTH));
            dialog.cancel();
        });
        if(datePicker.getParent() != null) {
            ((ViewGroup)datePicker.getParent()).removeView(datePicker);
        }
        selDayRangeDialog.show();
    }
    public void updateState() {
        if (selectedPos < 5) {
            state = selectedPos;
        } else { // select
            if (selToggle != null) {
                switch (selToggle.getCheckedButtonId()) {
                    case R.id.toggleDay:
                        state = DAY;
                        break;
                    case R.id.toggleMonth:
                        state = MONTH;
                        break;
                    case R.id.toggleYear:
                        state = YEAR;
                        break;
                }
            }
        }
    }
    public static Calendar getInitSelectedDates(int range, int state) {
        return getInitSelectedDates(range, state, Calendar.SUNDAY);
    }
    public static Calendar getInitSelectedDates(int range, int state, int firstDayOfWeek) {
        Calendar cal = Calendar.getInstance(MainActivity.locale);
        Calendar ref = MainActivity.getCalendarCopy(cal, FROM); // for checking if Sunday > Saturday
        int lastDayOfWeek = (firstDayOfWeek == Calendar.MONDAY) ? Calendar.SUNDAY : Calendar.SATURDAY;
        switch (state) {
            case WEEK:
                if ((range == DateGridAdapter.FROM) && ref.get(Calendar.DAY_OF_WEEK) == firstDayOfWeek)
                    break;
                if ((range == DateGridAdapter.TO) && ref.get(Calendar.DAY_OF_WEEK) == lastDayOfWeek)
                    break;
                cal.set(Calendar.DAY_OF_WEEK, (range == FROM) ? firstDayOfWeek : lastDayOfWeek);
                if ((range == FROM) && cal.after(ref)) cal.add(Calendar.DATE, -7); // roll back by a week
                if ((range == TO) && ref.after(cal)) cal.add(Calendar.DATE, 7); // append by a week
                break;
            case MONTH:
                cal.set(Calendar.DAY_OF_MONTH, (range == FROM) ? 1 : cal.getActualMaximum(Calendar.DATE));
                break;
            case YEAR:
                cal.set(Calendar.MONTH, (range == FROM) ? 0 : 11);
                cal.set(Calendar.DAY_OF_MONTH, (range == FROM) ? 1 : cal.getActualMaximum(Calendar.DATE));
                break;
        }
        if (range == FROM) {
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
        } else if (range == TO) {
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
        }
        return cal;
    }
    public boolean validateDateRange(Calendar from, Calendar to) {
        return (from.compareTo(to) <= 0);
    }

    /**
     * GETTERS & SETTERS
     */
    public int getSelectedState() {
        return state;
    }
    public int getSelectedPos() {
        return selectedPos;
    }
    public void setParentDialog(AlertDialog dialog) {
        this.parentDialog = dialog;
    }
    public Calendar[] getSelDateRange() {
        return new Calendar[] { fromDate, toDate };
    }
    public void setDisabledPos(String[] exclude) {
        for (String s : exclude) disabledPos[filterDateNames.indexOf(s)] = true;
    }
}
