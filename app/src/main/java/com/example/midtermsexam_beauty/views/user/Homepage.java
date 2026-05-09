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
import com.example.midtermsexam_beauty.models.SellerProfile;
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

        ProductCard.OnItemClickListener listener = product -> openShopFromProduct(product);

        featuredListView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        executor.execute(() -> {
            List<Product> featured = supabase.getTopPickMenuItems(session.getToken());
            if (featured == null || featured.isEmpty()) {
                featured = supabase.getRandomMenuItems(session.getToken());
            }
            final List<Product> featuredItems = featured;
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
            List<Product> fetchedShops = supabase.getAllShops(session.getToken());
            if (fetchedShops == null || fetchedShops.isEmpty()) {
                fetchedShops = mapSellerProfilesToProducts(supabase.getFeaturedShops(session.getToken()));
            }
            final List<Product> shops = fetchedShops;
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

    private List<Product> mapSellerProfilesToProducts(List<SellerProfile> sellers) {
        List<Product> mapped = new ArrayList<>();
        if (sellers == null) return mapped;

        for (SellerProfile seller : sellers) {
            Product shop = new Product(
                    R.drawable.product_1,
                    sanitizeShopName(seller.getStoreName(), "Unnamed Shop"),
                    seller.getDescription() != null ? seller.getDescription() : "",
                    0.0f,
                    "Restaurant",
                    seller.isOpen(),
                    4.8f,
                    "All"
            );
            shop.setSellerId(seller.getId());
            shop.setImageUrl(seller.getSellerAvatarUrl());
            shop.setShopBackground(seller.getSellerProfileBg());
            shop.setAddress(seller.getAddress());
            mapped.add(shop);
        }
        return mapped;
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

    private void openShopFromProduct(Product product) {
        Product shopPayload = resolveShopPayload(product);

        Intent intent = new Intent(this, ViewShop.class);
        intent.putExtra("imageId",       shopPayload.getImageID());
        intent.putExtra("name",          shopPayload.getName());
        intent.putExtra("price",         shopPayload.getPrice());
        intent.putExtra("description",   shopPayload.getDescription());
        intent.putExtra("rating",        shopPayload.getRating());
        intent.putExtra("category",      shopPayload.getCategory());
        String targetSellerId = (product.getSellerId() != null && !product.getSellerId().trim().isEmpty())
                ? product.getSellerId()
                : shopPayload.getSellerId();
        intent.putExtra("sellerId",      targetSellerId);
        intent.putExtra("selectedMenuItemId", product.getId());
        intent.putExtra("imageUrl",      shopPayload.getImageUrl());
        intent.putExtra("backgroundUrl", shopPayload.getShopBackground());
        intent.putExtra("address",       shopPayload.getAddress());
        intent.putExtra("isOpen",        shopPayload.getAvalability());
        startActivity(intent);
    }

    private Product resolveShopPayload(Product clicked) {
        Product shopBySeller = findShopBySellerId(clicked.getSellerId());
        if (shopBySeller != null) {
            return shopBySeller;
        }

        Product fallback = new Product(
                clicked.getImageID(),
                sanitizeShopName(clicked.getShopName(), clicked.getName()),
                "",
                0.0f,
                "Restaurant",
                true,
                clicked.getRating(),
                "All"
        );
        fallback.setSellerId(clicked.getSellerId());
        fallback.setImageUrl(clicked.getImageUrl());
        fallback.setShopBackground(clicked.getShopBackground());
        fallback.setAddress(clicked.getAddress());
        return fallback;
    }

    private Product findShopBySellerId(String sellerId) {
        if (sellerId == null || sellerId.isEmpty()) return null;

        for (Product shop : allShops) {
            if (sellerId.equals(shop.getSellerId())) {
                return shop;
            }
        }
        return null;
    }

    private String sanitizeShopName(String preferred, String fallback) {
        if (preferred != null && !preferred.trim().isEmpty()) return preferred.trim();
        if (fallback != null && !fallback.trim().isEmpty()) return fallback.trim();
        return "Restaurant";
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
