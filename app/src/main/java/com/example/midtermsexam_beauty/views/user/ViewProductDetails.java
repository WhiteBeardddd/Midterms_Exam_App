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
import com.example.midtermsexam_beauty.adapters.MenuAdapter; // UPDATED: Using the new Adapter
import com.example.midtermsexam_beauty.models.MenuItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ViewProductDetails extends AppCompatActivity {
    private static final String DEFAULT_SHOP_NAME = "Restaurant Placeholder";
    private static final float DEFAULT_RATING = 5.0f;

    private ImageView shopCoverImage;
    private ImageView shopLogoImage;
    private TextView shopLogoInitials;
    private TextView shopTitle;
    private TextView shopRating;
    private TextView shopSectionNote;

    // Header Buttons for FoodPanda design
    private ImageButton backButton;
    private ImageButton favoriteButton;
    private ImageButton shareButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.product_view_details);

        // 1. Initialize UI bindings
        bindViews();
        styleViews();

        // 2. ACTIVATE THE NAVBAR
        // This ensures the navigation to Home, Search, and Cart works
        NavbarCard.setupNavbar(this);

        // 3. Load Storefront Data
        ShopPayload payload = readShopPayload();
        bindShopHeader(payload);

        // 4. Initialize the dynamic menu with the NEW Adapter
        setupMenuGrid(payload.shopName);

        // 5. Setup Action Listeners
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

        favoriteButton.setOnClickListener(v ->
                Toast.makeText(this, "Added " + payload.shopName + " to Favorites!", Toast.LENGTH_SHORT).show()
        );

        shareButton.setOnClickListener(v ->
                Toast.makeText(this, "Sharing " + payload.shopName + " storefront...", Toast.LENGTH_SHORT).show()
        );
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
                intent.getFloatExtra("rating", DEFAULT_RATING)
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

    private void setupMenuGrid(String shopName) {
        RecyclerView rvMenu = findViewById(R.id.rv_shop_menu);
        rvMenu.setLayoutManager(new GridLayoutManager(this, 2));

        // Mock Menu Data
        List<MenuItem> menuItems = new ArrayList<>();

        MenuItem item1 = new MenuItem();
        item1.setName("Classic Cheeseburger");
        item1.setDescription("100% Beef with cheese");
        item1.setPrice(120.00);
        menuItems.add(item1);

        MenuItem item2 = new MenuItem();
        item2.setName("Large Fries");
        item2.setDescription("Crispy and golden");
        item2.setPrice(65.00);
        menuItems.add(item2);

        MenuItem item3 = new MenuItem();
        item3.setName("House Iced Tea");
        item3.setDescription("Refreshing cold drink");
        item3.setPrice(45.00);
        menuItems.add(item3);

        MenuItem item4 = new MenuItem();
        item4.setName("Chicken Nuggets");
        item4.setDescription("6 pieces with dip");
        item4.setPrice(95.00);
        menuItems.add(item4);

        // UPDATED: Now using MenuAdapter with the new item_menu_row layout
        MenuAdapter adapter = new MenuAdapter(this, menuItems, shopName);
        rvMenu.setAdapter(adapter);
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
            if (initials.length() == 2) {
                break;
            }
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

        private ShopPayload(int coverImageId, int logoImageId, String shopName, float rating) {
            this.coverImageId = coverImageId;
            this.logoImageId = logoImageId;
            this.shopName = shopName;
            this.rating = rating;
        }
    }
}