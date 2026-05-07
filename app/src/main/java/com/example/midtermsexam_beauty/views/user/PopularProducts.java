package com.example.midtermsexam_beauty.views.user;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.widget.EditText;
import android.widget.ListView;
import android.text.TextWatcher;

import androidx.appcompat.app.AppCompatActivity;

import com.example.midtermsexam_beauty.R;
import com.example.midtermsexam_beauty.adapters.NavbarCard;
import com.example.midtermsexam_beauty.adapters.PopularAndFeaturedAdapter;
import com.example.midtermsexam_beauty.models.Product;
import com.example.midtermsexam_beauty.utilities.SessionManager;
import com.example.midtermsexam_beauty.utilities.SupabaseAuthService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PopularProducts extends AppCompatActivity {

    private final ArrayList<Product> popularProducts = new ArrayList<>();
    private PopularAndFeaturedAdapter popularAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_popular_products);

        NavbarCard.setupNavbar(this);

        ListView popularListView = findViewById(R.id.popular_recycler);
        EditText searchBar = findViewById(R.id.searchEditText);

        popularAdapter = new PopularAndFeaturedAdapter(this, popularProducts);
        popularListView.setAdapter(popularAdapter);

        popularListView.setOnItemClickListener((parent, view, position, id) -> {
            Product product = popularProducts.get(position);
            openProductDetails(product);
        });

        fetchDynamicMenuItems();

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                String query = s.toString().trim();

                if (query.isEmpty()) {
                    fetchDynamicMenuItems();
                } else {
                    searchProducts(query);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void fetchDynamicMenuItems() {
        SessionManager session = new SessionManager(this);
        SupabaseAuthService supabase = new SupabaseAuthService();
        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(() -> {
            List<Product> dynamicItems = supabase.getRandomMenuItems(session.getToken());
            runOnUiThread(() -> {
                popularProducts.clear();
                popularProducts.addAll(dynamicItems);
                popularAdapter.notifyDataSetChanged();
            });
        });
    }

    private void searchProducts(String query) {

        SessionManager session = new SessionManager(this);
        SupabaseAuthService supabase = new SupabaseAuthService();
        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(() -> {

            List<Product> results =
                    supabase.getMenuItemByName(
                            session.getToken(),
                            query
                    );

            runOnUiThread(() -> {
                popularProducts.clear();
                popularProducts.addAll(results);
                popularAdapter.notifyDataSetChanged();
            });
        });
    }

    private void openProductDetails(Product product) {
        Intent intent = new Intent(this, MenuItemDetailsActivity.class);
        intent.putExtra("item_name", product.getName());
        intent.putExtra("item_desc", product.getDescription());
        intent.putExtra("item_price", (double) product.getPrice());
        intent.putExtra("shop_name", product.getShopName());
        intent.putExtra("image_url", product.getImageUrl());
        intent.putExtra("seller_id", product.getSellerId()); // Pass seller ID
        startActivity(intent);
    }
}