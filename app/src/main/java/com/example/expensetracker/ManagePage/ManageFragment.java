package com.example.expensetracker.ManagePage;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.expensetracker.R;
import com.example.expensetracker.RecyclerViewAdapters.SectionAdapter;
import com.example.expensetracker.RecyclerViewAdapters.ViewPagerAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class ManageFragment extends Fragment {

    private static final String TAG = "ManageFragment";
    public ViewPager2 viewPager;
    private Toolbar toolbar;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (getActivity() == null)
            return null;

        View view = inflater.inflate(R.layout.fragment_manage, container, false);
        TabLayout sectionTypeTab = view.findViewById(R.id.sectionTypeTab);

        viewPager = view.findViewById(R.id.viewPager);
        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        ManageChildFragmentAccount fragmentAccount = new ManageChildFragmentAccount();
        ManageChildFragmentCategory fragmentCategory = new ManageChildFragmentCategory();
        if (savedInstanceState != null) {
            for (int i = 0; i < getChildFragmentManager().getFragments().size() ; i++) {
                switch (i) {
                    case 0:
                        fragmentAccount = (ManageChildFragmentAccount) getChildFragmentManager().getFragments().get(i);
                        break;
                    case 1:
                        fragmentCategory = (ManageChildFragmentCategory) getChildFragmentManager().getFragments().get(i);
                        break;
                }
            }
        }
        adapter.addFragment(fragmentAccount);
        adapter.addFragment(fragmentCategory);
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                ManageChildFragment fragment = (ManageChildFragment) adapter.createFragment(position);
                fragment.invalidateMenu();
            }
        });
        viewPager.setAdapter(adapter);
        new TabLayoutMediator(sectionTypeTab, viewPager,
                (tab, position) -> tab.setText((position == 0) ? getString(R.string.ACCS) : getString(R.string.CATS))
        ).attach();

        // toolbar
        toolbar = view.findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.manage_options);
        toolbar.setTitle(getString(R.string.title_manage));
        ((ImageView) view.findViewById(R.id.toolbarBg)).setImageResource(R.drawable.red_background_material2);
        view.findViewById(R.id.summary).setVisibility(ConstraintLayout.GONE);
        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (int) getActivity().getResources().getDimension(R.dimen.actionBarSize));
        view.findViewById(R.id.toolbarContainer).setLayoutParams(params);

        // side menu
        ImageButton menuBtn = view.findViewById(R.id.menu_btn);
        menuBtn.setVisibility(View.GONE);

        return view;
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    public ManageChildFragment<? extends SectionAdapter> getChildFragment(int i) {
        return (ManageChildFragment<? extends SectionAdapter>) getChildFragmentManager().getFragments().get(i);
    }
    public Toolbar getToolbar() {
        return toolbar;
    }
    public void updateData() {
        getChildFragment(0).updateView();
    }
}