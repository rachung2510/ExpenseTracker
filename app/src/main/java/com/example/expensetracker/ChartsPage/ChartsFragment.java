package com.example.expensetracker.ChartsPage;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.expensetracker.R;

public class ChartsFragment extends Fragment {

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_charts, container, false);
        TextView textView = view.findViewById(R.id.text_charts);
        textView.setText("Charts section not yet open.");
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}