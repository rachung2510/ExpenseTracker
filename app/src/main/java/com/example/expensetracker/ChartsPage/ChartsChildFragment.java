package com.example.expensetracker.ChartsPage;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.expensetracker.MainActivity;

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
    protected void updateDateRange() {
    }
    protected void invalidateMenu() {
    }
    protected void updateCurrency() {
        updateCurrency(((MainActivity) getActivity()).getDefaultCurrencySymbol());
    }
    protected void updateCurrency(String curr) {
    }
}
