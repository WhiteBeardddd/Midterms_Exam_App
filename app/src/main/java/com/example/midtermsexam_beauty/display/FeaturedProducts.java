package com.example.midtermsexam_beauty.display;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ListView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.midtermsexam_beauty.R;
import com.example.midtermsexam_beauty.adapters.PopularAndFeaturedAdapter;
import com.example.midtermsexam_beauty.models.Product;

import java.util.ArrayList;

public class FeaturedProducts extends AppCompatActivity {

    private final ArrayList<Product> featuredProducts = new ArrayList<>();

    ImageButton toPrevious, proceedToRatings, proceedToHome, proceedToSkinTypes, proceedToCheckout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_featured_products);

        toPrevious = findViewById(R.id.back_btn);
        proceedToRatings = findViewById(R.id.toRatings);
        proceedToHome = findViewById(R.id.toHome);
        proceedToSkinTypes = findViewById(R.id.toSkinTypes);
        proceedToCheckout = findViewById(R.id.toPayment);

        ListView featuredListView = findViewById(R.id.featured_recycler);

        featuredProducts.addAll(Product.getDefaultProducts());

        PopularAndFeaturedAdapter adapter = new PopularAndFeaturedAdapter(this, featuredProducts);
        featuredListView.setAdapter(adapter);

        toPrevious.setOnClickListener(view -> startActivity(new Intent(FeaturedProducts.this, Homepage.class)));
        proceedToHome.setOnClickListener(view -> startActivity(new Intent(FeaturedProducts.this, Homepage.class)));
        proceedToRatings.setOnClickListener(view -> startActivity(new Intent(FeaturedProducts.this, PopularProducts.class)));
        proceedToSkinTypes.setOnClickListener(view -> startActivity(new Intent(FeaturedProducts.this, SkinType.class)));
        proceedToCheckout.setOnClickListener(view -> startActivity(new Intent(FeaturedProducts.this, Checkout.class)));
    }
}
