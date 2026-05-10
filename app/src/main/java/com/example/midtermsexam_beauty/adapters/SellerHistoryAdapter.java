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

        // Review views
        LinearLayout reviewContainer, noReviewContainer;
        TextView     tvRating, tvReviewComment, tvReviewDate, tvReviewerName;

        OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBuyerName        = itemView.findViewById(R.id.tvBuyerName);
            tvStatus           = itemView.findViewById(R.id.tvStatus);
            tvDate             = itemView.findViewById(R.id.tvDate);
            tvTotal            = itemView.findViewById(R.id.tvTotal);
            tvAddress          = itemView.findViewById(R.id.tvAddress);
            labelAddress       = itemView.findViewById(R.id.labelAddress);
            dividerAddress     = itemView.findViewById(R.id.dividerAddress);
            itemsContainer     = itemView.findViewById(R.id.itemsContainer);
            btnMarkDone        = itemView.findViewById(R.id.btnMarkDone);
            reviewContainer    = itemView.findViewById(R.id.reviewContainer);
            noReviewContainer  = itemView.findViewById(R.id.noReviewContainer);
            tvRating           = itemView.findViewById(R.id.tvRating);
            tvReviewComment    = itemView.findViewById(R.id.tvReviewComment);
            tvReviewDate       = itemView.findViewById(R.id.tvReviewDate);
            tvReviewerName     = itemView.findViewById(R.id.tvReviewerName);
        }

        void bind(OrderDetail order) {
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
            dividerAddress.setVisibility(hasAddress ? View.VISIBLE : View.GONE);
            labelAddress.setVisibility(hasAddress ? View.VISIBLE : View.GONE);
            tvAddress.setVisibility(hasAddress ? View.VISIBLE : View.GONE);
            if (hasAddress) {
                tvAddress.setText(order.street + ", " + order.barangay
                        + "\n" + order.city + " " + order.postalCode
                        + "\n" + order.country);
            }

            // ── Review section ────────────────────────────────────────────────────
            if (order.hasReview) {
                noReviewContainer.setVisibility(View.GONE);
                reviewContainer.setVisibility(View.VISIBLE);

                // Star rating: build visual stars + numeric
                tvRating.setText(buildStars(order.rating) + "  " + order.rating + " / 5");
                tvReviewComment.setText("\u201c" + order.reviewComment + "\u201d");
                tvReviewerName.setText("by " + order.buyerFullName);
                tvReviewDate.setText(formatDate(order.reviewDate));
            } else {
                reviewContainer.setVisibility(View.GONE);
                noReviewContainer.setVisibility(View.VISIBLE);
            }
        }

        /** Builds a star string like ★★★★☆ from a 1–5 rating */
        private String buildStars(double rating) {
            int full = (int) Math.round(rating);
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i <= 5; i++) {
                sb.append(i <= full ? "★" : "☆");
            }
            return sb.toString();
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