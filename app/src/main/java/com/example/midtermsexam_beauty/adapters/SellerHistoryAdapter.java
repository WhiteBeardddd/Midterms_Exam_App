package com.example.midtermsexam_beauty.adapters;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.midtermsexam_beauty.R;
import com.example.midtermsexam_beauty.utilities.SupabaseAuthService.OrderDetail;
import com.example.midtermsexam_beauty.utilities.SupabaseAuthService.OrderItemDetail;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class SellerHistoryAdapter extends RecyclerView.Adapter<SellerHistoryAdapter.OrderViewHolder> {

    private final Context           context;
    private final List<OrderDetail> orders;
    private final LayoutInflater    inflater;

    public SellerHistoryAdapter(Context context, List<OrderDetail> orders) {
        this.context  = context;
        this.orders   = orders;
        this.inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_seller_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        holder.bind(orders.get(position));
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    class OrderViewHolder extends RecyclerView.ViewHolder {

        TextView     tvBuyerName, tvStatus, tvDate, tvTotal;
        TextView     tvAddress, labelAddress;
        View         dividerAddress;
        LinearLayout itemsContainer;
        View         btnMarkDone;

        OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBuyerName    = itemView.findViewById(R.id.tvBuyerName);
            tvStatus       = itemView.findViewById(R.id.tvStatus);
            tvDate         = itemView.findViewById(R.id.tvDate);
            tvTotal        = itemView.findViewById(R.id.tvTotal);
            tvAddress      = itemView.findViewById(R.id.tvAddress);
            labelAddress   = itemView.findViewById(R.id.labelAddress);
            dividerAddress = itemView.findViewById(R.id.dividerAddress);
            itemsContainer = itemView.findViewById(R.id.itemsContainer);
            btnMarkDone    = itemView.findViewById(R.id.btnMarkDone);
        }

        void bind(OrderDetail order) {
            // ── Always hide Mark as Done in history ───────────────────────────
            btnMarkDone.setVisibility(View.GONE);

            tvBuyerName.setText(order.buyerFullName);
            tvDate.setText(formatDate(order.createdAt));

            tvStatus.setText(order.status.toUpperCase(Locale.ROOT));
            tvStatus.setBackground(statusBackground(order.status));

            itemsContainer.removeAllViews();
            for (OrderItemDetail item : order.items) {
                View row = inflater.inflate(R.layout.item_order_line, itemsContainer, false);
                ((TextView) row.findViewById(R.id.tvItemName))
                        .setText("• " + item.menuItemName);
                ((TextView) row.findViewById(R.id.tvItemMeta))
                        .setText("×" + item.quantity
                                + "  ₱" + String.format(Locale.ROOT, "%.2f", item.unitPrice));
                itemsContainer.addView(row);
            }

            tvTotal.setText("₱" + String.format(Locale.ROOT, "%.2f", order.totalAmount));

            boolean hasAddress = order.street != null && !order.street.isEmpty();
            int visibility = hasAddress ? View.VISIBLE : View.GONE;
            dividerAddress.setVisibility(visibility);
            labelAddress.setVisibility(visibility);
            tvAddress.setVisibility(visibility);

            if (hasAddress) {
                String full = order.street + ", " + order.barangay
                        + "\n" + order.city + " " + order.postalCode
                        + "\n" + order.country;
                tvAddress.setText(full);
            }
        }

        private GradientDrawable statusBackground(String status) {
            GradientDrawable gd = new GradientDrawable();
            gd.setCornerRadius(dp(20));
            int color;
            switch (status.toLowerCase(Locale.ROOT)) {
                case "done":       color = 0xFF2E7D32; break;
                case "cancelled":  color = 0xFFC62828; break;
                case "preparing":  color = 0xFFF57F17; break;
                case "on the way": color = 0xFF1565C0; break;
                default:           color = 0xFF424242; break;
            }
            gd.setColor(color);
            return gd;
        }

        private String formatDate(String iso) {
            try {
                SimpleDateFormat parser =
                        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ROOT);
                parser.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date d = parser.parse(iso);
                SimpleDateFormat fmt =
                        new SimpleDateFormat("MMM dd, yyyy  hh:mm a", Locale.ROOT);
                fmt.setTimeZone(TimeZone.getDefault());
                return fmt.format(d);
            } catch (Exception e) { return iso; }
        }

        private int dp(int v) {
            return Math.round(v * context.getResources().getDisplayMetrics().density);
        }
    }
}