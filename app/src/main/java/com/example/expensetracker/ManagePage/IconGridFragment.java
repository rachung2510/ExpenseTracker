package com.example.expensetracker.ManagePage;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensetracker.HelperClasses.ItemOffsetDecoration;
import com.example.expensetracker.R;
import com.example.expensetracker.RecyclerViewAdapters.IconGridAdapter;

public class IconGridFragment extends Fragment {

    private final IconGridAdapter adapter;
    public View view;
    public RecyclerView iconGrid;

    public IconGridFragment(Fragment fragment, int selectedPos, String[] adapterArr, int iconType) {
        super();
        this.adapter = new IconGridAdapter(fragment.getActivity(), adapterArr, selectedPos, iconType);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_manage_icon_grid, container, false);
        iconGrid = view.findViewById(R.id.iconGrid);
        iconGrid.setAdapter(adapter);
        iconGrid.setLayoutManager(new GridLayoutManager(getActivity(), 5));
        ItemOffsetDecoration itemDecoration = new ItemOffsetDecoration(getActivity(), R.dimen.spacing);
        iconGrid.addItemDecoration(itemDecoration);
        return view;
    }

    public IconGridAdapter getAdapter() {
        return adapter;
    }

}
