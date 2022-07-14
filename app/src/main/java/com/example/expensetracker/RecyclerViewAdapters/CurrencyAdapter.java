package com.example.expensetracker.RecyclerViewAdapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensetracker.R;

import java.util.ArrayList;

import com.example.expensetracker.Currency;

public class CurrencyAdapter extends RecyclerView.Adapter<CurrencyAdapter.ViewHolder> {
    private static final String TAG = "CurrencyAdapter";

    private Context context;
    private LayoutInflater inflater;
    private ArrayList<Currency> currencies;
    private int selectedPos;

    public CurrencyAdapter(Context context, ArrayList<Currency> currencies, String selected) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.currencies = currencies;
        this.selectedPos = currencies.indexOf(new Currency(selected, "",""));
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_currency, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        populateRecyclerView(holder, position);
    }

    @Override
    public int getItemCount() {
        return currencies.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView currSymbol;
        RadioButton currDesc;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            currSymbol = itemView.findViewById(R.id.currencySymbol);
            currDesc = itemView.findViewById(R.id.currencyDesc);
        }
    }
    public void populateRecyclerView(ViewHolder holder, int position) {
        Currency curr = currencies.get(position);
        holder.currSymbol.setText(curr.getSymbol());
        holder.currDesc.setText(curr.getDescription());
        holder.currDesc.setChecked(position == selectedPos);
        holder.currDesc.setOnCheckedChangeListener((compoundButton, b) -> {
            if (b) {
                int oldPos = selectedPos;
                selectedPos = holder.getAdapterPosition();
                notifyItemChanged(oldPos);
                notifyItemChanged(selectedPos);
            }
        });
    }

    public String getSelected() {
        return currencies.get(selectedPos).getName();
    }
}
