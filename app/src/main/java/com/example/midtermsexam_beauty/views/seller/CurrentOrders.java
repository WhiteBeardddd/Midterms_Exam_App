package com.example.midtermsexam_beauty.views.seller;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.midtermsexam_beauty.R;
import com.example.midtermsexam_beauty.adapters.SellerNavCard;
import com.example.midtermsexam_beauty.adapters.SellerOrderAdapter;
import com.example.midtermsexam_beauty.utilities.SessionManager;
import com.example.midtermsexam_beauty.utilities.SupabaseAuthService;
import com.example.midtermsexam_beauty.utilities.SupabaseAuthService.OrderDetail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CurrentOrders extends AppCompatActivity {

    private RecyclerView            recyclerView;
    private LinearLayout            emptyStateCard;
    private ProgressBar             progressBar;
    private SellerOrderAdapter      adapter;
    private final List<OrderDetail> orderList = new ArrayList<>();

    private SessionManager      sessionManager;
    private SupabaseAuthService supabase;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private String token;
    private String sellerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_current_orders);
        SellerNavCard.setupNavbar(this);
        hideSystemUI();

        recyclerView   = findViewById(R.id.ordersRecyclerView);
        emptyStateCard = findViewById(R.id.emptyStateCard);
        progressBar    = findViewById(R.id.progressBar);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setNestedScrollingEnabled(false);

        sessionManager = new SessionManager(this);
        supabase       = new SupabaseAuthService();
        token          = sessionManager.getToken();

        adapter = new SellerOrderAdapter(this, orderList, this::markOrderAsDone);
        recyclerView.setAdapter(adapter);

        loadOrders();
    }

    private void loadOrders() {
        showLoading(true);
        String authId = sessionManager.getUserId();

        executor.execute(() -> {
            sellerId = supabase.getSellerIdByAuthId(token, authId);

            // ── Fetch only pending orders ─────────────────────────────────────
            List<OrderDetail> pending = supabase.getPendingSellerOrders(token, sellerId);

            runOnUiThread(() -> {
                showLoading(false);
                orderList.clear();
                orderList.addAll(pending != null ? pending : new ArrayList<>());
                adapter.notifyDataSetChanged();

                if (orderList.isEmpty()) {
                    emptyStateCard.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    emptyStateCard.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            });
        });
    }

    private void markOrderAsDone(OrderDetail order, int position) {
        executor.execute(() -> {
            // ── Update status to "done" in Supabase ───────────────────────────
            boolean success = supabase.updateOrderStatus(token, order.orderId, "done");

            runOnUiThread(() -> {
                if (success) {
                    // Find actual index by orderId to avoid position shift bugs
                    int actualIndex = -1;
                    for (int i = 0; i < orderList.size(); i++) {
                        if (orderList.get(i).orderId.equals(order.orderId)) {
                            actualIndex = i;
                            break;
                        }
                    }

                    if (actualIndex >= 0) {
                        orderList.remove(actualIndex);
                        adapter.notifyItemRemoved(actualIndex);
                        adapter.notifyItemRangeChanged(actualIndex, orderList.size());
                    }

                    if (orderList.isEmpty()) {
                        emptyStateCard.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    }

                    Toast.makeText(this, "Order marked as done!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Failed to update. Try again.", Toast.LENGTH_SHORT).show();
                    adapter.notifyDataSetChanged();
                }
            });
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            recyclerView.setVisibility(View.GONE);
            emptyStateCard.setVisibility(View.GONE);
        }
    }

    private void hideSystemUI() {
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
        loadOrders();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}