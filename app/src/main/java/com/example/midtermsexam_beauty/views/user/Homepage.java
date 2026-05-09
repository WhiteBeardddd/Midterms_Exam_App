package com.example.midtermsexam_beauty.views.user;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.midtermsexam_beauty.R;
import com.example.midtermsexam_beauty.adapters.NavbarCard;
import com.example.midtermsexam_beauty.adapters.ProductCard;
import com.example.midtermsexam_beauty.adapters.RestaurantFeedAdapter;
import com.example.midtermsexam_beauty.models.Product;
import com.example.midtermsexam_beauty.utilities.SessionManager;
import com.example.midtermsexam_beauty.utilities.SupabaseAuthService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Homepage extends AppCompatActivity {

    // ── Pagination state ──────────────────────────────────────────────────────
    private static final int PAGE_SIZE = 5;
    private int     currentPage = 1;
    private int     totalPages  = 1;
    private boolean isLoading   = false;
    private final List<Product> allShops = new ArrayList<>();

    // ── Views ─────────────────────────────────────────────────────────────────
    private RecyclerView          nearbyListView;
    private RestaurantFeedAdapter popularAdapter;
    private final List<Product>   displayedShops = new ArrayList<>();
    private TextView              tvPageIndicator;
    private LinearLayout          btnLoadMore;
    private ProgressBar           paginationProgressBar;
    private TextView              tvEndOfList;

    // ── Misc ──────────────────────────────────────────────────────────────────
    private SessionManager      session;
    private SupabaseAuthService supabase;
    private ExecutorService     executor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homepage);
        hideSystemUI();

        session  = new SessionManager(this);
        supabase = new SupabaseAuthService();
        executor = Executors.newSingleThreadExecutor();

        RecyclerView featuredListView = findViewById(R.id.featured_recycler);
        nearbyListView        = findViewById(R.id.popular_recycler);
        EditText searchEditText       = findViewById(R.id.searchEditText);
        tvPageIndicator       = findViewById(R.id.tvPageIndicator);
        btnLoadMore           = findViewById(R.id.btnLoadMore);
        paginationProgressBar = findViewById(R.id.paginationProgressBar);
        tvEndOfList           = findViewById(R.id.tvEndOfList);

        NavbarCard.setupNavbar(this);

        ProductCard.OnItemClickListener listener = product -> {
            Intent intent = new Intent(this, ViewShop.class);
            intent.putExtra("imageId",       product.getImageID());
            intent.putExtra("name",          product.getName());
            intent.putExtra("price",         product.getPrice());
            intent.putExtra("description",   product.getDescription());
            intent.putExtra("rating",        product.getRating());
            intent.putExtra("category",      product.getCategory());
            intent.putExtra("sellerId",      product.getSellerId());
            intent.putExtra("imageUrl",      product.getImageUrl());
            intent.putExtra("backgroundUrl", product.getShopBackground());
            intent.putExtra("address",       product.getAddress());
            intent.putExtra("isOpen",        product.getAvalability());
            startActivity(intent);
        };

        featuredListView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        executor.execute(() -> {
            List<Product> featuredItems = supabase.getRandomMenuItems(session.getToken());
            runOnUiThread(() -> {
                ProductCard adapter = new ProductCard(this, featuredItems, listener);
                featuredListView.setAdapter(adapter);
            });
        });


        popularAdapter = new RestaurantFeedAdapter(this, displayedShops, listener);
        nearbyListView.setLayoutManager(new LinearLayoutManager(this));
        nearbyListView.setNestedScrollingEnabled(false);
        nearbyListView.setAdapter(popularAdapter);

        btnLoadMore.setOnClickListener(v -> loadNextPage());

        fetchAllShops();

        // ── Search ────────────────────────────────────────────────────────────
        searchEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                startActivity(new Intent(this, PopularProducts.class));
                v.clearFocus();
            }
        });
    }

    // ── Fetch all shops then show first page ──────────────────────────────────
    private void fetchAllShops() {
        paginationProgressBar.setVisibility(View.VISIBLE);
        btnLoadMore.setVisibility(View.GONE);

        executor.execute(() -> {
            List<Product> shops = supabase.getAllShops(session.getToken());
            runOnUiThread(() -> {
                paginationProgressBar.setVisibility(View.GONE);
                allShops.clear();
                allShops.addAll(shops);

                totalPages = (int) Math.ceil((double) allShops.size() / PAGE_SIZE);
                if (totalPages == 0) totalPages = 1;

                currentPage = 1;
                displayedShops.clear();
                popularAdapter.notifyDataSetChanged();

                showPage(currentPage);
            });
        });
    }

    // ── Append one page of restaurants to the list ────────────────────────────
    private void showPage(int page) {
        int fromIndex = (page - 1) * PAGE_SIZE;
        int toIndex   = Math.min(fromIndex + PAGE_SIZE, allShops.size());
        if (fromIndex >= allShops.size()) return;

        List<Product> pageItems  = allShops.subList(fromIndex, toIndex);
        int           insertStart = displayedShops.size();
        displayedShops.addAll(pageItems);
        popularAdapter.notifyItemRangeInserted(insertStart, pageItems.size());

        tvPageIndicator.setText(page + " / " + totalPages);

        if (page < totalPages) {
            btnLoadMore.setVisibility(View.VISIBLE);
            tvEndOfList.setVisibility(View.GONE);
        } else {
            btnLoadMore.setVisibility(View.GONE);
            tvEndOfList.setVisibility(allShops.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }

    // ── Load next page on button tap ──────────────────────────────────────────
    private void loadNextPage() {
        if (isLoading || currentPage >= totalPages) return;
        isLoading = true;

        btnLoadMore.setVisibility(View.GONE);
        paginationProgressBar.setVisibility(View.VISIBLE);

        nearbyListView.postDelayed(() -> {
            currentPage++;
            showPage(currentPage);
            paginationProgressBar.setVisibility(View.GONE);
            isLoading = false;
        }, 600);
    }

    // ── Fullscreen ────────────────────────────────────────────────────────────
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
        if (executor != null) executor.shutdown();
    }
}