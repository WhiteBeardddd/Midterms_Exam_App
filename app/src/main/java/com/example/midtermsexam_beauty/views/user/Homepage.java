package com.example.midtermsexam_beauty.views.user;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homepage);

        RecyclerView featuredListView = findViewById(R.id.featured_recycler);
        RecyclerView popularListView = findViewById(R.id.popular_recycler);
        EditText searchEditText = findViewById(R.id.searchEditText);

        NavbarCard.setupNavbar(this);

        List<Product> featuredProducts = getStaticFeaturedShops();

        ProductCard.OnItemClickListener listener = product -> {
            Intent intent = new Intent(this, ViewProductDetails.class);
            intent.putExtra("imageId", product.getImageID());
            intent.putExtra("name", product.getName());
            intent.putExtra("price", product.getPrice());
            intent.putExtra("description", product.getDescription());
            intent.putExtra("rating", product.getRating());
            intent.putExtra("category", product.getCategory());
            intent.putExtra("skin_type", product.getSkin_type());
            intent.putExtra("availability", product.getAvalability());
            intent.putExtra("sellerId", product.getSellerId());
            startActivity(intent);
        };

        ProductCard featuredAdapter = new ProductCard(this, featuredProducts, listener);
        featuredListView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        featuredListView.setAdapter(featuredAdapter);
        popularListView.setLayoutManager(new LinearLayoutManager(this));

        SessionManager session = new SessionManager(this);
        SupabaseAuthService supabase = new SupabaseAuthService();
        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(() -> {
            List<Product> dynamicShops = supabase.getAllShops(session.getToken());
            runOnUiThread(() -> {
                RestaurantFeedAdapter popularAdapter = new RestaurantFeedAdapter(this, dynamicShops, listener);
                popularListView.setAdapter(popularAdapter);
            });
        });

        searchEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                startActivity(new Intent(this, PopularProducts.class));
                v.clearFocus();
            }
        });
    }

    private List<Product> getStaticFeaturedShops() {
        List<Product> shops = new ArrayList<>();
        shops.add(new Product(R.drawable.product_1, "Minute Burger", "Quick burgers and budget-friendly bites.", 99.00f, "Fast Food", true, 4.8f, "All"));
        shops.add(new Product(R.drawable.product_2, "Jollibee", "Comfort food with crowd favorites.", 149.00f, "Chicken & Rice", true, 4.9f, "All"));
        shops.add(new Product(R.drawable.product_3, "McDonalds", "Reliable fast-food staples.", 139.00f, "Burgers", true, 4.7f, "All"));
        shops.add(new Product(R.drawable.product_4, "KFC", "Crispy chicken meals and box deals.", 179.00f, "Fried Chicken", true, 4.8f, "All"));
        return shops;
    }
}