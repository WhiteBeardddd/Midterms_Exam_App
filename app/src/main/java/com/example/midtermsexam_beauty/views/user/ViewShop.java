package com.example.midtermsexam_beauty.views.user;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.midtermsexam_beauty.R;
import com.example.midtermsexam_beauty.adapters.NavbarCard;
import com.example.midtermsexam_beauty.adapters.MenuAdapter;
import com.example.midtermsexam_beauty.models.MenuItem;
import com.example.midtermsexam_beauty.utilities.SessionManager;
import com.example.midtermsexam_beauty.utilities.SupabaseAuthService;

import java.util.List;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ViewShop extends AppCompatActivity {
    private static final String DEFAULT_SHOP_NAME = "Restaurant Placeholder";
    private static final float  DEFAULT_RATING    = 5.0f;

    private ImageView   shopCoverImage;
    private ImageView   shopLogoImage;
    private TextView    shopTitle;
    private TextView    shopLoc;
    private TextView    shopDesc;
    private TextView    shopSectionNote;
    private ImageButton backButton;
    private ImageButton favoriteButton;
    private ImageButton shareButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_shop);
        hideSystemUI();
        bindViews();
        NavbarCard.setupNavbar(this);

        ShopPayload payload = readShopPayload();
        bindShopHeader(payload);
        setupMenuGrid(payload.shopName, payload.sellerId, payload.selectedMenuItemId);
        setupClickListeners(payload);
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

    private void bindViews() {
        shopCoverImage  = findViewById(R.id.shop_cover_image);
        shopLogoImage   = findViewById(R.id.shop_logo_image);
        shopTitle       = findViewById(R.id.shop_title);
        shopLoc         = findViewById(R.id.shop_loc);
        shopDesc        = findViewById(R.id.shop_desc);
        shopSectionNote = findViewById(R.id.shop_section_note);
        backButton      = findViewById(R.id.back_btn);
        favoriteButton  = findViewById(R.id.favorite_btn);
        shareButton     = findViewById(R.id.share_btn);
    }

    private void setupClickListeners(ShopPayload payload) {
        backButton.setOnClickListener(v -> finish());
        favoriteButton.setOnClickListener(v ->
                Toast.makeText(this, "Added " + payload.shopName + " to Favorites!", Toast.LENGTH_SHORT).show());
        shareButton.setOnClickListener(v ->
                Toast.makeText(this, "Sharing " + payload.shopName + " storefront...", Toast.LENGTH_SHORT).show());
    }

    private ShopPayload readShopPayload() {
        Intent intent = getIntent();
        return new ShopPayload(
                intent.getIntExtra("imageId", R.drawable.tarabytes),
                sanitize(intent.getStringExtra("name"), DEFAULT_SHOP_NAME),
                intent.getFloatExtra("rating", DEFAULT_RATING),
                intent.getStringExtra("sellerId"),
                intent.getStringExtra("selectedMenuItemId"),
                intent.getStringExtra("imageUrl"),
                intent.getStringExtra("backgroundUrl"),
                intent.getStringExtra("address"),
                intent.getStringExtra("description"),
                intent.getBooleanExtra("isOpen", false)
        );
    }

    private void bindShopHeader(ShopPayload payload) {
        // ── Cover / background image ──────────────────────────────────────────
        if (payload.backgroundUrl != null && !payload.backgroundUrl.isEmpty()) {
            Glide.with(this)
                    .load(payload.backgroundUrl)
                    .centerCrop()
                    .placeholder(R.drawable.tarabytes)
                    .into(shopCoverImage);
        } else {
            shopCoverImage.setImageResource(payload.coverImageId);
        }

        // ── Avatar / logo image ───────────────────────────────────────────────
        if (payload.avatarUrl != null && !payload.avatarUrl.isEmpty()) {
            Glide.with(this)
                    .load(payload.avatarUrl)
                    .centerCrop()
                    .placeholder(R.drawable.tarabytes)
                    .into(shopLogoImage);
        } else {
            shopLogoImage.setImageResource(R.drawable.tarabytes);
        }

        // ── Shop title ────────────────────────────────────────────────────────
        shopTitle.setText(payload.shopName);

        // ── Address shown in shop_loc ─────────────────────────────────────────
        if (payload.address != null && !payload.address.isEmpty()) {
            shopLoc.setText("📍 " + payload.address);
        } else {
            shopLoc.setText("");
        }

        // ── Description shown in shop_desc ────────────────────────────────────
        if (payload.description != null && !payload.description.isEmpty()) {
            shopDesc.setText(payload.description);
        } else {
            shopDesc.setText("");
        }

        // ── Section note below Popular ────────────────────────────────────────
        shopSectionNote.setText("Most ordered right now at " + payload.shopName + ".");
    }

    private void setupMenuGrid(String shopName, String sellerId, String selectedMenuItemId) {
        RecyclerView rvMenu = findViewById(R.id.rv_shop_menu);
        rvMenu.setLayoutManager(new GridLayoutManager(this, 2));

        if (sellerId == null || sellerId.isEmpty()) {
            Toast.makeText(this, "Store menu unavailable.", Toast.LENGTH_SHORT).show();
            return;
        }

        SessionManager      session  = new SessionManager(this);
        SupabaseAuthService supabase = new SupabaseAuthService();
        ExecutorService     executor = Executors.newSingleThreadExecutor();

        executor.execute(() -> {
            List<MenuItem> items = supabase.getMenuItems(session.getToken(), sellerId);
            prioritizeSelectedItem(items, selectedMenuItemId);
            runOnUiThread(() -> {
                MenuAdapter adapter = new MenuAdapter(ViewShop.this, items, shopName);
                rvMenu.setAdapter(adapter);
            });
        });
    }

    private void prioritizeSelectedItem(List<MenuItem> items, String selectedMenuItemId) {
        if (items == null || items.isEmpty()) return;
        if (selectedMenuItemId == null || selectedMenuItemId.trim().isEmpty()) return;

        for (int i = 0; i < items.size(); i++) {
            MenuItem item = items.get(i);
            if (selectedMenuItemId.equals(item.getId())) {
                if (i > 0) Collections.swap(items, 0, i);
                return;
            }
        }
    }

    private String sanitize(String value, String fallback) {
        if (value == null) return fallback;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    // ── ShopPayload ───────────────────────────────────────────────────────────
    private static final class ShopPayload {
        private final int     coverImageId;
        private final String  shopName;
        private final float   rating;
        private final String  sellerId;
        private final String  selectedMenuItemId;
        private final String  avatarUrl;
        private final String  backgroundUrl;
        private final String  address;
        private final String  description;
        private final boolean isOpen;

        private ShopPayload(int coverImageId, String shopName, float rating,
                            String sellerId, String selectedMenuItemId,
                            String avatarUrl, String backgroundUrl,
                            String address, String description, boolean isOpen) {
            this.coverImageId  = coverImageId;
            this.shopName      = shopName;
            this.rating        = rating;
            this.sellerId      = sellerId;
            this.selectedMenuItemId = selectedMenuItemId;
            this.avatarUrl     = avatarUrl;
            this.backgroundUrl = backgroundUrl;
            this.address       = address;
            this.description   = description;
            this.isOpen        = isOpen;
        }
    }
}
