package com.example.expensetracker.ChartsPage;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.expensetracker.Expense;
import com.example.expensetracker.MainActivity;
import com.example.expensetracker.Section;

import java.util.ArrayList;
import java.util.Calendar;

public class ChartsChildFragment extends Fragment {

    private static final String TAG = "ChartsChildFragment";
    protected int chartType;
    protected View view;

    public static final int TYPE_PIECHART = 0;
    public static final int TYPE_GRAPH = 1;
    public static final int TYPE_CALENDAR = 2;

    public ChartsChildFragment() {

    }
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return null;
    }

    /**
     * Common update functions
     */
    public void updateDateFilters() {
    }
    public void invalidateMenu() {
    }
    public void updateCurrency() {
        updateCurrency(((MainActivity) getActivity()).getDefaultCurrencySymbol());
    }
    public void updateCurrency(String curr) {
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
