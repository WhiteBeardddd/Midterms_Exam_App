package com.example.midtermsexam_beauty.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.midtermsexam_beauty.R;
import com.example.midtermsexam_beauty.models.Order;

import java.util.List;
import java.util.Locale;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.ViewHolder> {
    private List<Order> orders;

    public OrderAdapter(List<Order> orders) {
        this.orders = orders;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_buyer_order, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Order order = orders.get(position);

        // Safely substring the UUID for a shorter display string
        String shortId = order.getId() != null && order.getId().length() >= 8
                ? order.getId().substring(0, 8).toUpperCase()
                : "N/A";

        holder.tvOrderId.setText("Order #" + shortId);
        holder.tvOrderStatus.setText(order.getStatus() != null ? order.getStatus().toUpperCase() : "PENDING");
        holder.tvOrderTotal.setText(String.format(Locale.US, "₱%.2f", order.getTotalAmount()));

        if (order.getCreatedAt() != null && order.getCreatedAt().length() >= 10) {
            holder.tvOrderDate.setText("Placed on: " + order.getCreatedAt().substring(0, 10));
        } else {
            holder.tvOrderDate.setText("Placed recently");
        }
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderId, tvOrderStatus, tvOrderTotal, tvOrderDate;

        ViewHolder(View view) {
            super(view);
            tvOrderId = view.findViewById(R.id.tvOrderId);
            tvOrderStatus = view.findViewById(R.id.tvOrderStatus);
            tvOrderTotal = view.findViewById(R.id.tvOrderTotal);
            tvOrderDate = view.findViewById(R.id.tvOrderDate);
        }
    }
}