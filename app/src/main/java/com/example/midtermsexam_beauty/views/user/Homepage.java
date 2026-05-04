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

import java.util.ArrayList;
import java.util.List;

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
        List<Product> popularProducts = Product.getMeals(this, "popular");

        ProductCard.OnItemClickListener listener = product -> {
            Toast.makeText(this, "Clicked: " + product.getName(), Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(this, ViewProductDetails.class);
            intent.putExtra("imageId", product.getImageID());
            intent.putExtra("name", product.getName());
            intent.putExtra("price", product.getPrice());
            intent.putExtra("description", product.getDescription());
            intent.putExtra("rating", product.getRating());
            intent.putExtra("category", product.getCategory());
            intent.putExtra("skin_type", product.getSkin_type());
            intent.putExtra("availability", product.getAvalability());
            startActivity(intent);
        };

        ProductCard featuredAdapter = new ProductCard(this, featuredProducts, listener);
        RestaurantFeedAdapter popularAdapter = new RestaurantFeedAdapter(this, popularProducts, listener);

        featuredListView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );
        popularListView.setLayoutManager(new LinearLayoutManager(this));

        featuredListView.setAdapter(featuredAdapter);
        popularListView.setAdapter(popularAdapter);

        searchEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                startActivity(new Intent(this, PopularProducts.class));
                v.clearFocus();
            }
        });
    }

    private List<Product> getStaticFeaturedShops() {
        List<Product> shops = new ArrayList<>();
        shops.add(new Product(
                R.drawable.product_1,
                "Minute Burger",
                "Quick burgers and budget-friendly bites that are easy to grab anytime.",
                99.00f,
                "Fast Food",
                true,
                4.8f,
                "Always featured"
        ));
        shops.add(new Product(
                R.drawable.product_2,
                "Jollibee",
                "Well-known comfort food with crowd favorites and familiar combo meals.",
                149.00f,
                "Chicken & Rice",
                true,
                4.9f,
                "Always featured"
        ));
        shops.add(new Product(
                R.drawable.product_3,
                "McDonalds",
                "Reliable fast-food staples with burgers, fries, and drinks users already know.",
                139.00f,
                "Burgers",
                true,
                4.7f,
                "Always featured"
        ));
        shops.add(new Product(
                R.drawable.product_4,
                "KFC",
                "Crispy chicken meals and box deals that fit the featured restaurant lane well.",
                179.00f,
                "Fried Chicken",
                true,
                4.8f,
                "Always featured"
        ));
        return shops;
    }
}
