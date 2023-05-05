package com.example.expensetracker.RecyclerViewAdapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.expensetracker.ChartsPage.ChartsFragment;
import com.example.expensetracker.Constants;
import com.example.expensetracker.HomePage.HomeFragment;
import com.example.expensetracker.ManagePage.ManageFragment;

import java.util.ArrayList;

public class ViewPagerAdapter extends FragmentStateAdapter {
    private final ArrayList<Fragment> fragments = new ArrayList<>();

    /**
     * CONSTRUCTOR
     */
    public ViewPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }
    public ViewPagerAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
        super(fragmentManager, lifecycle);
    }

    /**
     * FUNCTIONS
     */
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return fragments.get(position);
    }

    public void addFragment(Fragment fragment) {
        fragments.add(fragment);
    }

    @Override
    public int getItemCount() {
            return fragments.size();
        }

}
