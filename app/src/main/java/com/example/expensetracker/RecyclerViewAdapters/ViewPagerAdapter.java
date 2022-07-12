package com.example.expensetracker.RecyclerViewAdapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.ArrayList;

public class ViewPagerAdapter extends FragmentStateAdapter {
    private final ArrayList<Fragment> fragmentArrayList = new ArrayList<>();

    /**
     * CONSTRUCTOR
     */
    public ViewPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    /**
     * INITIALISE ADAPTER
     */
    public void addFragment(Fragment fragment) {
        fragmentArrayList.add(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return fragmentArrayList.get(position);
    }

    @Override
    public int getItemCount() {
            return fragmentArrayList.size();
        }

}
