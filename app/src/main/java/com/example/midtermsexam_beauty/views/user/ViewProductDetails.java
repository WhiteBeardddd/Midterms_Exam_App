package com.example.midtermsexam_beauty.views.user;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.midtermsexam_beauty.R;
import com.example.midtermsexam_beauty.adapters.NavbarCard;
import com.example.midtermsexam_beauty.adapters.MenuAdapter;
import com.example.midtermsexam_beauty.models.MenuItem;
import com.example.midtermsexam_beauty.utilities.SessionManager;
import com.example.midtermsexam_beauty.utilities.SupabaseAuthService;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ViewProductDetails extends AppCompatActivity {
    private static final String DEFAULT_SHOP_NAME = "Restaurant Placeholder";
    private static final float DEFAULT_RATING = 5.0f;

    private ImageView shopCoverImage;
    private ImageView shopLogoImage;
    private TextView shopLogoInitials;
    private TextView shopTitle;
    private TextView shopRating;
    private TextView shopSectionNote;

    private ImageButton backButton;
    private ImageButton favoriteButton;
    private ImageButton shareButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.product_view_details);

        bindViews();
        styleViews();
        NavbarCard.setupNavbar(this);

        ShopPayload payload = readShopPayload();
        bindShopHeader(payload);

        setupMenuGrid(payload.shopName, payload.sellerId);

        setupClickListeners(payload);
    }

    private void bindViews() {
        shopCoverImage = findViewById(R.id.shop_cover_image);
        shopLogoImage = findViewById(R.id.shop_logo_image);
        shopLogoInitials = findViewById(R.id.shop_logo_initials);
        shopTitle = findViewById(R.id.shop_title);
        shopRating = findViewById(R.id.rating_text);
        shopSectionNote = findViewById(R.id.shop_section_note);

        backButton = findViewById(R.id.back_btn);
        favoriteButton = findViewById(R.id.favorite_btn);
        shareButton = findViewById(R.id.share_btn);
    }

    private void setupClickListeners(ShopPayload payload) {
        backButton.setOnClickListener(v -> finish());
        favoriteButton.setOnClickListener(v -> Toast.makeText(this, "Added " + payload.shopName + " to Favorites!", Toast.LENGTH_SHORT).show());
        shareButton.setOnClickListener(v -> Toast.makeText(this, "Sharing " + payload.shopName + " storefront...", Toast.LENGTH_SHORT).show());
    }

    private void styleViews() {
        shopRating.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
    }

    private ShopPayload readShopPayload() {
        Intent intent = getIntent();
        return new ShopPayload(
                intent.getIntExtra("imageId", R.drawable.product_1),
                intent.getIntExtra("logoImageId", 0),
                sanitize(intent.getStringExtra("name"), DEFAULT_SHOP_NAME),
                intent.getFloatExtra("rating", DEFAULT_RATING),
                intent.getStringExtra("sellerId")
        );
    }

    private void bindShopHeader(ShopPayload payload) {
        shopCoverImage.setImageResource(payload.coverImageId);
        bindLogo(payload);
        shopTitle.setText(payload.shopName);
        shopRating.setText(buildRatingLabel(payload.rating));
        shopSectionNote.setText(buildSectionNote(payload.shopName));
    }

    private void bindLogo(ShopPayload payload) {
        if (payload.logoImageId != 0) {
            shopLogoImage.setVisibility(View.VISIBLE);
            shopLogoInitials.setVisibility(View.GONE);
            shopLogoImage.setImageResource(payload.logoImageId);
            return;
        }
        shopLogoImage.setVisibility(View.GONE);
        shopLogoInitials.setVisibility(View.VISIBLE);
        shopLogoInitials.setText(buildInitials(payload.shopName));
    }

    private void setupMenuGrid(String shopName, String sellerId) {
        RecyclerView rvMenu = findViewById(R.id.rv_shop_menu);
        rvMenu.setLayoutManager(new GridLayoutManager(this, 2));

        if (sellerId == null || sellerId.isEmpty()) {
            Toast.makeText(this, "Store menu unavailable.", Toast.LENGTH_SHORT).show();
            return;
        }

        SessionManager session = new SessionManager(this);
        SupabaseAuthService supabase = new SupabaseAuthService();
        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(() -> {
            List<MenuItem> items = supabase.getMenuItems(session.getToken(), sellerId);

            runOnUiThread(() -> {
                MenuAdapter adapter = new MenuAdapter(ViewProductDetails.this, items, shopName);
                rvMenu.setAdapter(adapter);
            });
        });
    }

    private String buildRatingLabel(float rating) {
        float resolvedRating = rating > 0 ? rating : DEFAULT_RATING;
        return String.format(Locale.US, "%.1f (100+ ratings)", resolvedRating);
    }

    private String buildSectionNote(String shopName) {
        return "Most ordered right now at " + shopName + ".";
    }

    private String buildInitials(String shopName) {
        String[] parts = shopName.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty() && Character.isLetterOrDigit(part.charAt(0))) {
                initials.append(Character.toUpperCase(part.charAt(0)));
            }
            if (initials.length() == 2) break;
        }
        return initials.length() > 0 ? initials.toString() : "ST";
    }

    private String sanitize(String value, String fallback) {
        if (value == null) return fallback;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private static final class ShopPayload {
        private final int coverImageId;
        private final int logoImageId;
        private final String shopName;
        private final float rating;
        private final String sellerId;

        private ShopPayload(int coverImageId, int logoImageId, String shopName, float rating, String sellerId) {
            this.coverImageId = coverImageId;
            this.logoImageId = logoImageId;
            this.shopName = shopName;
            this.rating = rating;
            this.sellerId = sellerId;
        }
    }
}