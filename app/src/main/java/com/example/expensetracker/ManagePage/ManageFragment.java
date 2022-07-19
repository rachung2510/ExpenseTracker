package com.example.expensetracker.ManagePage;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.expensetracker.R;
import com.example.expensetracker.RecyclerViewAdapters.ViewPagerAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.Objects;

public class ManageFragment extends Fragment {

    public ViewPager2 sectionPage;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (getActivity() == null)
            return null;
        View view = inflater.inflate(R.layout.fragment_manage, container, false);
        TabLayout sectionTypeTab = view.findViewById(R.id.sectionTypeTab);
        sectionPage = view.findViewById(R.id.sectionPage);
        ViewPagerAdapter catOptAdapter = new ViewPagerAdapter(this);

        catOptAdapter.addFragment(new ManageChildFragmentAccount(getActivity()));
        catOptAdapter.addFragment(new ManageChildFragmentCategory(getActivity()));
        sectionPage.setAdapter(catOptAdapter);
        new TabLayoutMediator(sectionTypeTab, sectionPage,
                (tab, position) -> tab.setText((position == 0) ? "ACCOUNTS" : "CATEGORIES")
        ).attach();

        // toolbar
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        Objects.requireNonNull(((AppCompatActivity) getActivity()).getSupportActionBar()).setTitle("Manage");
        ((ImageView) view.findViewById(R.id.toolbarBg)).setImageResource(R.drawable.red_background_material2);
        view.findViewById(R.id.summary).setVisibility(ConstraintLayout.GONE);
        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (int) getActivity().getResources().getDimension(R.dimen.actionBarSize));
        view.findViewById(R.id.toolbarContainer).setLayoutParams(params);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}