package com.example.midtermsexam_beauty.views.user;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.widget.EditText;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextWatcher;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.midtermsexam_beauty.R;
import com.example.midtermsexam_beauty.adapters.NavbarCard;
import com.example.midtermsexam_beauty.adapters.SellerCard;
import com.example.midtermsexam_beauty.models.SellerProfile;
import com.example.midtermsexam_beauty.utilities.SessionManager;
import com.example.midtermsexam_beauty.utilities.SupabaseAuthService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PopularShops extends AppCompatActivity {

    private final ArrayList<SellerProfile> popularShops = new ArrayList<>();
    private SellerCard searchAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_popular_products);
        hideSystemUI();
        NavbarCard.setupNavbar(this);

        RecyclerView popularListView = findViewById(R.id.popular_recycler);
        popularListView.setLayoutManager(new LinearLayoutManager(this));
        EditText searchBar = findViewById(R.id.searchEditText);

        searchAdapter = new SellerCard(this, popularShops, this::openShopDetails);
        popularListView.setAdapter(searchAdapter);

        fetchRandomShops();

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                String query = s.toString().trim();

                if (query.isEmpty()) {
                    fetchRandomShops();
                } else {
                    searchShops(query);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void fetchRandomShops() {
        SessionManager session = new SessionManager(this);
        SupabaseAuthService supabase = new SupabaseAuthService();
        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(() -> {
            List<SellerProfile> dynamicItems = supabase.getRandomShops(session.getToken());
            runOnUiThread(() -> {
                popularShops.clear();
                popularShops.addAll(dynamicItems);
                searchAdapter.notifyDataSetChanged();
            });
        });
    }

    private void searchShops(String query) {

        SessionManager session = new SessionManager(this);
        SupabaseAuthService supabase = new SupabaseAuthService();
        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(() -> {

            List<SellerProfile> results =
                    supabase.searchShopsByMenuItem(
                            session.getToken(),
                            query
                    );

            runOnUiThread(() -> {
                popularShops.clear();
                popularShops.addAll(results);
                searchAdapter.notifyDataSetChanged();
            });
        });
    }


    private void hideSystemUI() {
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
    }
    private void openShopDetails(SellerProfile shop) {

        Intent intent = new Intent(this, ViewShop.class);

        intent.putExtra("name", shop.getStoreName());
        intent.putExtra("sellerId", shop.getId());
        intent.putExtra("imageUrl", shop.getSellerAvatarUrl());
        intent.putExtra("backgroundUrl", shop.getSellerProfileBg());
        intent.putExtra("address", shop.getAddress());
        intent.putExtra("description", shop.getDescription());
        intent.putExtra("isOpen", shop.isOpen());

        startActivity(intent);
    }
}