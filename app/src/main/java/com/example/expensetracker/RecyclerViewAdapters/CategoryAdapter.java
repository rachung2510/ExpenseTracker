package com.example.expensetracker.RecyclerViewAdapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensetracker.Category;
import com.example.expensetracker.MainActivity;
import com.example.expensetracker.R;

import java.util.ArrayList;

public class CategoryAdapter extends SectionAdapter<Category> {

    private static final String TAG = "CategoryAdapter";

    /**
     * CONSTRUCTOR
     */
    public CategoryAdapter(Context context, ArrayList<Category> categories) {
        super(context, categories);

        // Move Others category to last
        if (sectionNames.contains("Others")) {
            Category catOthers = categories.get(sectionNames.indexOf("Others"));
            categories.remove(catOthers);
            categories.add(catOthers);
            sectionNames.remove("Others");
            sectionNames.add("Others");
        }
    }
    public CategoryAdapter(Context context, ArrayList<Category> categories, int mode) {
        super(context, categories, mode);

        // Move Others category to last
        if (sectionNames.contains("Others")) {
            Category catOthers = categories.get(sectionNames.indexOf("Others"));
            categories.remove(catOthers);
            categories.add(catOthers);
            sectionNames.remove("Others");
            sectionNames.add("Others");
        }
    }

    /**
     * INITIALISE ADAPTER
     */
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_section_grid, parent, false);
        return new GridViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        populateSectionGrid((GridViewHolder) holder, position);
    }

    /**
     * FUNCTIONS
     */
    public void addNewCat() {
        sections.add(new Category(context));
        notifyItemInserted(sections.size()-1);
    }
    @Override
    public void resetPositions() {
        for (Category cat : sections) {
            cat.setPosition(cat.getId() - 1);
            ((MainActivity) context).db.updateCategory(cat);
        }
    }
    public void updatePositions() {
        for (int i = 0;i < getItemCount()-2;i++) {
            Category cat = sections.get(i);
            cat.setPosition(i+1);
            ((MainActivity) context).db.updateCategory(cat);
        }
    }

}
