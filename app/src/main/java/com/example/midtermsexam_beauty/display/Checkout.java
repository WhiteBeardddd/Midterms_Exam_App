package com.example.midtermsexam_beauty.display;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import com.example.midtermsexam_beauty.R;
import com.example.midtermsexam_beauty.adapters.CheckOutCard;
import com.example.midtermsexam_beauty.models.Product;
import com.example.midtermsexam_beauty.utilities.ProductManager;

import java.util.List;

public class Checkout extends AppCompatActivity {
    private List<Product> productList;
    private TextView totalPriceText;

    ImageButton toPrevious, proceedToRatings, proceedToHome,
            proceedToFeatured, proceedToSkinTypes;

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        toPrevious = findViewById(R.id.back_btn);
        proceedToFeatured = findViewById(R.id.toFeatured);
        proceedToRatings = findViewById(R.id.toRatings);
        proceedToHome = findViewById(R.id.toHome);
        proceedToSkinTypes = findViewById(R.id.toSkinTypes);

        ListView cartListView = findViewById(R.id.cart_list);
        totalPriceText = findViewById(R.id.total_price); // Get Total Price TextView

        productList = ProductManager.getInstance().getProduct();

        CheckOutCard checkOutAdapter = new CheckOutCard(this, productList);
        cartListView.setAdapter(checkOutAdapter);

        updateTotalPrice();

        // Navigation Buttons

        toPrevious.setOnClickListener(view -> startActivity(new Intent(Checkout.this, Homepage.class)));

        proceedToHome.setOnClickListener(view -> {
            Intent intent = new Intent(Checkout.this, Homepage.class);
            startActivity(intent);
        });

        proceedToFeatured.setOnClickListener(view -> {
            Intent intent = new Intent(Checkout.this, FeaturedProducts.class);
            startActivity(intent);
        });

        proceedToSkinTypes.setOnClickListener(view -> {
            Intent intent = new Intent(Checkout.this, SkinType.class);
            startActivity(intent);
        });

        proceedToRatings.setOnClickListener(view -> {
            Intent intent = new Intent(Checkout.this, PopularProducts.class);
            startActivity(intent);
        });


    }


    @SuppressLint("DefaultLocale")
    private void updateTotalPrice() {
        double total = 0;
        for (Product product : productList) {
            total += product.getPrice() * product.getCounter(); // Multiply price by quantity
        }
        totalPriceText.setText(String.format("₱%.2f", total)); // Format price to 2 decimal places
    }
}
