package com.example.expensetracker.ManagePage;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.expensetracker.Constants;
import com.example.expensetracker.MainActivity;
import com.example.expensetracker.R;
import com.example.expensetracker.RecyclerViewAdapters.IconGridAdapter;
import com.example.expensetracker.RecyclerViewAdapters.ViewPagerAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.Arrays;
import java.util.List;

public class SectionOptDialogFragment extends DialogFragment {

    private final int sectionType;
    private int selectedIconPos, selectedColorPos;
    private IconGridFragment iconFragment, colorFragment;

    public SectionOptDialogFragment(int sectionType, String selectedIconName, String selectedColorName) {
        super();
        this.sectionType = sectionType;
        this.selectedIconPos = 0;

        List<String> defaultAccIconNames = Arrays.asList(Constants.defaultAccIcons);
        List<String> defaultCatIconNames = Arrays.asList(Constants.defaultCatIcons);
        List<String> defaultColorNames = Arrays.asList(Constants.defaultColors);
        if (sectionType == Constants.SECTION_ACCOUNT) this.selectedIconPos = defaultAccIconNames.indexOf(selectedIconName);
        else if (sectionType == Constants.SECTION_CATEGORY) this.selectedIconPos = defaultCatIconNames.indexOf(selectedIconName);
        this.selectedColorPos = defaultColorNames.indexOf(selectedColorName);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_section_opt, null);
        TabLayout catOptTab = view.findViewById(R.id.catOptTab);
        ViewPager2 catOptPage = view.findViewById(R.id.catOptPage);
        TextView catOptTitle = view.findViewById(R.id.catOptTitle);
        catOptPage.setOffscreenPageLimit(2);
        catOptPage.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (getChildFragmentManager().getFragments().size() > position) {
                    Fragment fragment = getChildFragmentManager().getFragments().get(position);
                    View child = fragment.getView();
                    int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(child.getWidth(), View.MeasureSpec.EXACTLY);
                    int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
                    child.measure(widthMeasureSpec, heightMeasureSpec);
                    ViewGroup.LayoutParams params = catOptPage.getLayoutParams();
                    if (params.height != child.getMeasuredHeight()) {
                        params.width = child.getMeasuredWidth();
                        params.height = child.getMeasuredHeight();
                        catOptPage.setLayoutParams(params);
                    }
                }
            }
        });
        ViewPagerAdapter catOptAdapter = new ViewPagerAdapter(this);

        if (sectionType == Constants.SECTION_ACCOUNT) iconFragment = new IconGridFragment(this, selectedIconPos, Constants.defaultAccIcons, IconGridAdapter.ICON_ICON);
        else if (sectionType == Constants.SECTION_CATEGORY) iconFragment = new IconGridFragment(this, selectedIconPos, Constants.defaultCatIcons, IconGridAdapter.ICON_ICON);
        colorFragment = new IconGridFragment(this, selectedColorPos, Constants.defaultColors, IconGridAdapter.ICON_COLOR);
        catOptAdapter.addFragment(iconFragment);
        catOptAdapter.addFragment(colorFragment);
        catOptPage.setAdapter(catOptAdapter);
        new TabLayoutMediator(catOptTab, catOptPage,
                (tab, position) -> tab.setText((position == 0) ? "ICON" : "COLOUR")
        ).attach();

        catOptTitle.setText((sectionType == Constants.SECTION_ACCOUNT) ? "ACCOUNT ICON" : "CATEGORY ICON");
        builder.setView(view)
                .setPositiveButton("Done", (dialogInterface, i) -> {
                    String iconName = iconFragment.getAdapter().getSelectedIcon();
                    String colorName = colorFragment.getAdapter().getSelectedIcon();
                    if (sectionType == Constants.SECTION_ACCOUNT) ((MainActivity) getActivity()).setEditAccOptions(iconName, colorName);
                    else if (sectionType == Constants.SECTION_CATEGORY) ((MainActivity) getActivity()).setEditCatOptions(iconName, colorName);
                })
                .setNeutralButton(android.R.string.no, (dialogInterface, i) -> {});

        return builder.create();
    }
}
