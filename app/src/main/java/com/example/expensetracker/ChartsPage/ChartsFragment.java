package com.example.expensetracker.ChartsPage;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.expensetracker.R;
import com.example.expensetracker.RecyclerViewAdapters.ViewPagerAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class ChartsFragment extends Fragment {

    private int indicatorWidth;
    public String[] fragmentTitles = new String[] { "Categories", "Monthly", "Calendar" };

    /**
     * CONSTRUCTOR
     */
    public ChartsFragment() {
        // Required empty public constructor
    }

    /**
     * OVERRIDEN METHODS
     */
    /**
     * Main
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_charts, container, false);
        TabLayout tabLayout = view.findViewById(R.id.chartsTab);
        View tabIndicator = view.findViewById(R.id.chartsTabIndicator);
        ViewPager2 viewPager2 = view.findViewById(R.id.viewPager);

        // toolbar
        view.findViewById(R.id.summaryAmtBlk).setVisibility(LinearLayout.GONE);
        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (int) getActivity().getResources().getDimension(R.dimen.actionBarSize));
        view.findViewById(R.id.toolbarContainer).setLayoutParams(params);

        // load fragments
        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        adapter.addFragment(new ChartsChildFragment(ChartsChildFragment.TYPE_PIECHART));
        adapter.addFragment(new ChartsChildFragment(ChartsChildFragment.TYPE_GRAPH));
        adapter.addFragment(new ChartsChildFragment(ChartsChildFragment.TYPE_CALENDAR));
        viewPager2.setAdapter(adapter);

        // configure tab layout
        new TabLayoutMediator(tabLayout, viewPager2,
                (tab, position) -> tab.setText(fragmentTitles[position])
        ).attach();
        tabLayout.post(() -> {
            indicatorWidth = tabLayout.getWidth() / tabLayout.getTabCount();
            FrameLayout.LayoutParams indicatorParams = (FrameLayout.LayoutParams) tabIndicator.getLayoutParams();
            indicatorParams.width = indicatorWidth;
            tabIndicator.setLayoutParams(indicatorParams);
        });

        // tab selection indicator behaviour
        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) tabIndicator.getLayoutParams();
                float translationOffset = (positionOffset + position) * indicatorWidth;
                params.leftMargin = (int) translationOffset;
                tabIndicator.setLayoutParams(params);
            }
        });

        return view;
    }

}