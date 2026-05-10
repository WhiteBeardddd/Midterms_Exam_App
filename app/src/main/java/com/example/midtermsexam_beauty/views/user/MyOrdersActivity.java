package com.example.midtermsexam_beauty.views.user;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.midtermsexam_beauty.R;
import com.example.midtermsexam_beauty.adapters.OrderAdapter;
import com.example.midtermsexam_beauty.models.Order;
import com.example.midtermsexam_beauty.utilities.BaseAuthenticatedActivity;
import com.example.midtermsexam_beauty.utilities.SupabaseAuthService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyOrdersActivity extends BaseAuthenticatedActivity {

    private RecyclerView rvMyOrders;
    private OrderAdapter orderAdapter;
    private final List<Order> orderList = new ArrayList<>();

    private SupabaseAuthService authService;
    private ExecutorService executor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_orders);
        hideSystemUI();

        authService = new SupabaseAuthService();
        executor = Executors.newSingleThreadExecutor();

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        rvMyOrders = findViewById(R.id.rvMyOrders);
        rvMyOrders.setLayoutManager(new LinearLayoutManager(this));

        orderAdapter = new OrderAdapter(orderList, this::showOrderDetailsDialog);
        rvMyOrders.setAdapter(orderAdapter);

        fetchMyOrders();
    }

    private void fetchMyOrders() {
        String profileId = sessionManager.getProfileId();
        if (profileId == null) return;

        executor.execute(() -> {
            List<Order> fetchedOrders = authService.getBuyerOrders(sessionManager.getToken(), profileId);

            runOnUiThread(() -> {
                if (fetchedOrders != null && !fetchedOrders.isEmpty()) {
                    orderList.clear();
                    orderList.addAll(fetchedOrders);
                    orderAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(this, "No orders found.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void showOrderDetailsDialog(Order order) {
        Intent intent = new Intent(this, OrderDetailsActivity.class);
        intent.putExtra("ORDER_ID", order.getId());
        intent.putExtra("ORDER_DATE", order.getCreatedAt());
        intent.putExtra("ORDER_STATUS", order.getStatus());
        intent.putExtra("ORDER_ADDRESS", order.getAddress());
        intent.putExtra("ORDER_TOTAL", order.getTotalAmount());
        startActivity(intent);
    }

    private void hideSystemUI() {
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());

        // FIXED LINE: Added "Decor" to the method name
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
        fetchMyOrders();
    }

    @Override
    protected void onDestroy() {
        if (executor != null) executor.shutdown();
        super.onDestroy();
    }
}