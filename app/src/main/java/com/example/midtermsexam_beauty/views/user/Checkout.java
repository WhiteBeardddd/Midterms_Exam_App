package com.example.midtermsexam_beauty.views.user;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.midtermsexam_beauty.R;
import com.example.midtermsexam_beauty.adapters.CheckOutCard;
import com.example.midtermsexam_beauty.adapters.NavbarCard;
import com.example.midtermsexam_beauty.models.Product;
import com.example.midtermsexam_beauty.utilities.ProductManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Checkout extends AppCompatActivity {
    private final List<Product> productList = new ArrayList<>();
    private TextView tvSubtotal, tvTotal;
    private ListView cartListView;
    private CheckOutCard checkOutAdapter;
    private final float deliveryFee = 49.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        NavbarCard.setupNavbar(this);

        // Bind Views
        ImageButton toPrevious = findViewById(R.id.back_btn);
        cartListView = findViewById(R.id.cart_list);
        tvSubtotal = findViewById(R.id.tv_subtotal);
        tvTotal = findViewById(R.id.tv_total_price);
        Button btnPlaceOrder = findViewById(R.id.btn_place_order);

        // Initialize Adapter once
        checkOutAdapter = new CheckOutCard(this, productList);
        cartListView.setAdapter(checkOutAdapter);

        // Listeners
        toPrevious.setOnClickListener(view -> finish());
        btnPlaceOrder.setOnClickListener(v -> handleOrderPlacement());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // REFRESH DATA EVERY TIME SCREEN BECOMES VISIBLE
        loadCartData();
        updateTotalPrice();
    }

    private void loadCartData() {
        Map<Product, Integer> cart = ProductManager.getInstance().getCartItems();
        productList.clear();
        for (Map.Entry<Product, Integer> entry : cart.entrySet()) {
            Product p = entry.getKey();
            p.setCounter(entry.getValue());
            productList.add(p);
        }

        // Notify the adapter that the underlying data has changed
        if (checkOutAdapter != null) {
            checkOutAdapter.notifyDataSetChanged();
        }
    }

    private void updateTotalPrice() {
        double subtotal = 0;
        for (Product product : productList) {
            subtotal += product.getPrice() * product.getCounter();
        }

        double total = subtotal > 0 ? subtotal + deliveryFee : 0;

        tvSubtotal.setText(String.format("₱%.2f", subtotal));
        tvTotal.setText(String.format("₱%.2f", total));
    }

    private void handleOrderPlacement() {
        if (productList.isEmpty()) {
            Toast.makeText(this, "Empty cart!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Order Placed Successfully!", Toast.LENGTH_SHORT).show();
            ProductManager.getInstance().getCartItems().clear();
            // Refresh local list and UI immediately
            loadCartData();
            updateTotalPrice();
            finish();
        }
    }
}