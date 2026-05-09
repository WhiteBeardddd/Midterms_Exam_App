package com.example.midtermsexam_beauty.views.user;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.midtermsexam_beauty.R;
import com.example.midtermsexam_beauty.adapters.OrderAdapter;
import com.example.midtermsexam_beauty.models.Order;
import com.example.midtermsexam_beauty.utilities.SessionManager;
import com.example.midtermsexam_beauty.utilities.SupabaseAuthService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyOrdersActivity extends AppCompatActivity {

    private RecyclerView rvMyOrders;
    private OrderAdapter orderAdapter;
    private List<Order> orderList = new ArrayList<>();

    private SessionManager sessionManager;
    private SupabaseAuthService authService;
    private ExecutorService executor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_orders);
        hideSystemUI();
        sessionManager = new SessionManager(this);
        authService = new SupabaseAuthService();
        executor = Executors.newSingleThreadExecutor();

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        rvMyOrders = findViewById(R.id.rvMyOrders);
        rvMyOrders.setLayoutManager(new LinearLayoutManager(this));

        orderAdapter = new OrderAdapter(orderList, this::showOrderDetailsDialog);
        rvMyOrders.setAdapter(orderAdapter);

        // Fetch orders at the very end of onCreate!
        fetchMyOrders();
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
    }

    private void fetchMyOrders() {
        if (sessionManager.getToken() == null || sessionManager.getProfileId() == null) {
            Toast.makeText(this, "ERROR: Missing User Session Data!", Toast.LENGTH_LONG).show();
            return;
        }

        executor.execute(() -> {
            List<Order> fetchedOrders = authService.getBuyerOrders(sessionManager.getToken(), sessionManager.getProfileId());

            runOnUiThread(() -> {
                // Tracker to tell you exactly how many orders were downloaded
                Toast.makeText(MyOrdersActivity.this, "Found " + fetchedOrders.size() + " orders", Toast.LENGTH_SHORT).show();

                orderList.clear();
                orderList.addAll(fetchedOrders);

                // Safety check so it doesn't crash the layout manager
                if (orderAdapter != null) {
                    orderAdapter.notifyDataSetChanged();
                }
            });
        });
    }

    private void showOrderDetailsDialog(Order order) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Order Details");

        String dateString = "Recently";
        if (order.getCreatedAt() != null && order.getCreatedAt().length() >= 16) {
            dateString = order.getCreatedAt().replace("T", " ").substring(0, 16);
        }

        String message = "Order ID:\n" + order.getId() + "\n\n"
                + "Date Placed:\n" + dateString + "\n\n"
                + "Status:\n" + (order.getStatus() != null ? order.getStatus().toUpperCase() : "PENDING") + "\n\n"
                + "Delivery Address:\n" + order.getAddress() + "\n\n"
                + "Total Amount:\n₱" + String.format(java.util.Locale.US, "%.2f", order.getTotalAmount());

        builder.setMessage(message);
        builder.setPositiveButton("Close", null);
        builder.show();
    }

    @Override
    protected void onDestroy() {
        if (executor != null) executor.shutdown();
        super.onDestroy();
    }
}