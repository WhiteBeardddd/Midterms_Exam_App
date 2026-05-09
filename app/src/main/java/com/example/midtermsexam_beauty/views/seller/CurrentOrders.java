package com.example.midtermsexam_beauty.views.seller;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

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

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CurrentOrders extends AppCompatActivity {

    private RecyclerView    recyclerView;
    private LinearLayout    emptyStateCard;
    private ProgressBar     progressBar;

    private SessionManager      sessionManager;
    private SupabaseAuthService supabase;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

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

        loadOrders();
    }

    private void loadOrders() {
        showLoading(true);
        String token  = sessionManager.getToken();
        String authId = sessionManager.getUserId();

        // ADD THESE ↓
        Log.d("CurrentOrders", "token=" + token);
        Log.d("CurrentOrders", "authId=" + authId);

        executor.execute(() -> {
            String sellerId = supabase.getSellerIdByAuthId(token, authId);

            // ADD THIS ↓
            Log.d("CurrentOrders", "sellerId=" + sellerId);

            List<OrderDetail> orders = supabase.getSellerOrders(token, sellerId);

            // ADD THIS ↓
            Log.d("CurrentOrders", "orders count=" + (orders == null ? "null" : orders.size()));

            runOnUiThread(() -> {
                showLoading(false);
                if (orders == null || orders.isEmpty()) {
                    emptyStateCard.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    emptyStateCard.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    recyclerView.setAdapter(new SellerOrderAdapter(this, orders));
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}