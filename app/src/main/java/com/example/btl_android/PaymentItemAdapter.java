package com.example.btl_android;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

/** Hiển thị các thuốc trong treatment_plans.items mà bác sĩ đã kê. */
public class PaymentItemAdapter extends RecyclerView.Adapter<PaymentItemAdapter.Holder> {
    private final List<Map<String, Object>> items;

    public PaymentItemAdapter(List<Map<String, Object>> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View row = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_2, parent, false);
        return new Holder(row);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Map<String, Object> m = items.get(position);
        String name = str(m.get("name"));
        String unit = str(m.get("unit"));
        long quantity = number(m.get("quantity"));
        long price = number(m.get("unitPriceVnd"));
        long lineTotal = number(m.get("lineTotalVnd"));
        holder.title.setText(name + "  •  SL: " + quantity + " " + unit);
        holder.subtitle.setText("Đơn giá " + PriceText.vnd(price)
                + "  →  " + PriceText.vnd(lineTotal)
                + "\nLiều: " + str(m.get("dose")) + " • " + str(m.get("schedule")));
        holder.subtitle.setMaxLines(3);
    }

    @Override public int getItemCount() { return items.size(); }

    private static long number(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : 0;
    }
    private static String str(Object value) {
        return value == null ? "" : value.toString();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView subtitle;
        Holder(View view) {
            super(view);
            title = view.findViewById(android.R.id.text1);
            subtitle = view.findViewById(android.R.id.text2);
        }
    }
}
