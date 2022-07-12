package com.example.expensetracker.RecyclerViewAdapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensetracker.MainActivity;
import com.example.expensetracker.R;

public class IconGridAdapter extends RecyclerView.Adapter<IconGridAdapter.ViewHolder> {

    public static final int ICON_ICON = 0;
    public static final int ICON_COLOR = 1;

    private final String[] names;
    private final Context context;
    private final LayoutInflater inflater;
    private final int iconType;

    public final ColorStateList iconGray;
    public final ColorStateList iconWhite;
    public final Drawable iconCheck;

    private int selectedPos;

    /**
     * CONSTRUCTOR
     */
    public IconGridAdapter(Context context, String[] names, int selectedPos, int iconType) {
        this.names = names;
        this.selectedPos = selectedPos;
        this.iconType = iconType;
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.iconGray = MainActivity.getColorStateListFromId(context, R.color.generic_icon_gray);
        this.iconWhite = MainActivity.getColorStateListFromId(context, R.color.white);
        this.iconCheck = MainActivity.getIconFromId(context, R.drawable.action_check);
    }

    /**
     * INITIALISE ADAPTER
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_icon_grid, parent, false);
        return new IconGridAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        int pos = holder.getAdapterPosition();
        String name = names[pos];
        if (iconType == ICON_ICON) {
            holder.iconBtn.setForeground(MainActivity.getIconFromName(context, name));
            if (selectedPos == pos) { // selected
                holder.iconBtn.setBackgroundTintList(iconGray);
                holder.iconBtn.setForegroundTintList(iconWhite);
            } else { // not selected
                holder.iconBtn.setBackgroundTintList(null);
                holder.iconBtn.setForegroundTintList(iconGray);
            }
        } else if (iconType == ICON_COLOR) {
            holder.iconBtn.setBackgroundTintList(MainActivity.getColorStateListFromName(context, name));
            holder.iconBtn.setForegroundTintList(iconGray);
            if (selectedPos == pos) {
                holder.iconBtn.getBackground().setAlpha(100);
                holder.iconBtn.setForeground(iconCheck);
            } else {
                holder.iconBtn.getBackground().setAlpha(255);
                holder.iconBtn.setForeground(null);
            }
        }

        holder.iconBtn.setOnClickListener(view -> {
            selectedPos = pos;
            notifyDataSetChanged();
        });
    }

    @Override
    public int getItemCount() {
        return names.length;
    }

    /**
     * VIEWHOLDER CLASS
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageButton iconBtn;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            iconBtn = itemView.findViewById(R.id.iconBtn);
        }
    }

    /**
     * GETTERS & SETTERS
     */
    public String getSelectedIcon() {
        if (selectedPos >=0 && selectedPos < getItemCount())
            return names[selectedPos];
        else
            return "";
    }

}
