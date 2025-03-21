package com.example.midtermsexam_beauty.pages;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.midtermsexam_beauty.R;
import com.example.midtermsexam_beauty.adapters.SkinTypeCard;
import com.example.midtermsexam_beauty.models.Product;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SkinType extends AppCompatActivity {

    private final ArrayList<Product> skinTypeList = new ArrayList<>();
    private RecyclerView skinTypeListView;
    private Spinner skinTypeSpinner;
    private SkinTypeCard SkinTypeAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_skin_type);

        // Initialize Buttons
        ImageButton toPrevious = findViewById(R.id.back_btn);
        ImageButton proceedToFeatured = findViewById(R.id.toFeatured);
        ImageButton proceedToRatings = findViewById(R.id.toRatings);
        ImageButton proceedToHome = findViewById(R.id.toHome);
        ImageButton proceedToCheckout = findViewById(R.id.toPayment);

        // Initialize RecyclerView & Spinner
        skinTypeListView = findViewById(R.id.skinTypeListView);
        skinTypeSpinner = findViewById(R.id.skinTypeSpinner);

        // Dynamic skin types list (new types can be added)
        String[] options = {"All", "Oily", "Dry", "Combination", "Normal", "Ranked", "Hot", "Secret", "Loved", "Ratings"};

        // Set up Spinner Adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        skinTypeSpinner.setAdapter(adapter);

        // Load products
        skinTypeList.addAll(Product.getPopularProducts());
        skinTypeList.addAll(Product.getDefaultProducts());

        // Set up RecyclerView Adapter
        SkinTypeAdapter = new SkinTypeCard(this, new ArrayList<>(skinTypeList));

        // Set Grid Layout (2 columns)
        skinTypeListView.setLayoutManager(new GridLayoutManager(this, 2));
        skinTypeListView.setAdapter(SkinTypeAdapter);

        // Spinner Selection Listener to filter products
        skinTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedSkinType = (String) parent.getItemAtPosition(position);
                filterProductsBySkinType(selectedSkinType);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                filterProductsBySkinType("All");
            }
        });

        // Navigation Buttons
        toPrevious.setOnClickListener(view -> startActivity(new Intent(SkinType.this, Homepage.class)));
        proceedToRatings.setOnClickListener(view -> startActivity(new Intent(SkinType.this, PopularProducts.class)));
        proceedToFeatured.setOnClickListener(view -> startActivity(new Intent(SkinType.this, FeaturedProducts.class)));
        proceedToHome.setOnClickListener(view -> startActivity(new Intent(SkinType.this, Homepage.class)));
        proceedToCheckout.setOnClickListener(view -> startActivity(new Intent(SkinType.this, Checkout.class)));
    }

    private void filterProductsBySkinType(String selectedSkinType) {
        List<Product> filteredList = new ArrayList<>();
        List<Product> allProducts = new ArrayList<>();
        allProducts.addAll(Product.getPopularProducts());
        allProducts.addAll(Product.getDefaultProducts());

        String selectedType = selectedSkinType.toLowerCase();

        if (selectedType.equals("ratings")) {
            // Sort products by rating from highest to lowest
            Collections.sort(allProducts, Comparator.comparingDouble(Product::getRating).reversed());
            filteredList.addAll(allProducts);
        } else {
            // Filter by skin type
            for (Product product : allProducts) {
                if (selectedType.equals("all") || product.getSkin_type().toLowerCase().contains(selectedType)) {
                    filteredList.add(product);
                } else if (selectedType.equals("all") || product.getCategory().toLowerCase().contains(selectedType)) {
                    filteredList.add(product);
                }
            }
        }

        // Update RecyclerView with filtered data
        SkinTypeAdapter.updateList(filteredList);
    }
}
