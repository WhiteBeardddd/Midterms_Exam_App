// Keep your existing imports...
package com.example.midtermsexam_beauty.views.user;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.midtermsexam_beauty.R;
import com.example.midtermsexam_beauty.adapters.CheckOutCard;
import com.example.midtermsexam_beauty.adapters.NavbarCard;
import com.example.midtermsexam_beauty.models.BuyerAddress;
import com.example.midtermsexam_beauty.models.Product;
import com.example.midtermsexam_beauty.utilities.ProductManager;
import com.example.midtermsexam_beauty.utilities.SessionManager;
import com.example.midtermsexam_beauty.utilities.SupabaseAuthService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Checkout extends AppCompatActivity implements CheckOutCard.CartActionListener {
    private final List<Product> productList = new ArrayList<>();
    private TextView tvSubtotal, tvTotal;
    private ListView cartListView;
    private CheckOutCard checkOutAdapter;
    private final float deliveryFee = 49.0f;
    private Button btnPlaceOrder;

    private SessionManager sessionManager;
    private SupabaseAuthService authService;
    private ExecutorService executor;
    private BuyerAddress userAddress = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);
        hideSystemUI();
        NavbarCard.setupNavbar(this);

        sessionManager = new SessionManager(this);
        authService = new SupabaseAuthService();
        executor = Executors.newSingleThreadExecutor();

        cartListView = findViewById(R.id.cart_list);
        tvSubtotal = findViewById(R.id.tv_subtotal);
        tvTotal = findViewById(R.id.tv_total_price);
        btnPlaceOrder = findViewById(R.id.btn_place_order);

        // Pass 'this' as the listener interface
        checkOutAdapter = new CheckOutCard(this, productList, this);
        cartListView.setAdapter(checkOutAdapter);

        btnPlaceOrder.setOnClickListener(v -> handleOrderPlacement());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCartData();
        updateTotalPrice();
        fetchUserAddress();
    }

    // --- Interface Implementation Methods ---

    @Override
    public void onQuantityChanged(Product product, int newQuantity) {
        // Update the central cart repository
        ProductManager.getInstance().getCartItems().put(product, newQuantity);

        // Refresh the list and prices locally
        loadCartData();
        updateTotalPrice();
    }

    @Override
    public void onItemDeleted(Product product) {
        // Remove item from the central cart repository
        ProductManager.getInstance().getCartItems().remove(product);

        // Refresh the list and prices locally
        loadCartData();
        updateTotalPrice();

        Toast.makeText(this, "Item removed from cart", Toast.LENGTH_SHORT).show();
    }

    // ----------------------------------------

    private void hideSystemUI() {
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        );
    }

    private void fetchUserAddress() {
        executor.execute(() -> {
            BuyerAddress address = authService.getBuyerAddress(sessionManager.getToken(), sessionManager.getProfileId());
            runOnUiThread(() -> {
                userAddress = address;
            });
        });
    }

    private void loadCartData() {
        Map<Product, Integer> cart = ProductManager.getInstance().getCartItems();
        productList.clear();
        for (Map.Entry<Product, Integer> entry : cart.entrySet()) {
            Product p = entry.getKey();
            p.setCounter(entry.getValue());
            productList.add(p);
        }

        if (checkOutAdapter != null) {
            checkOutAdapter.notifyDataSetChanged();
        }

        // Disable order button if cart is empty
        btnPlaceOrder.setEnabled(!productList.isEmpty());
    }

    @SuppressLint("DefaultLocale")
    private void updateTotalPrice() {
        double subtotal = 0;
        List<String> uniqueSellers = new ArrayList<>();

        for (Product product : productList) {
            subtotal += product.getPrice() * product.getCounter();
            String sId = product.getSellerId();
            if (sId != null && !uniqueSellers.contains(sId)) {
                uniqueSellers.add(sId);
            }
        }

        int shopCount = uniqueSellers.isEmpty() ? 1 : uniqueSellers.size();
        if (productList.isEmpty()) shopCount = 0;

        double total = subtotal + (deliveryFee * shopCount);

        tvSubtotal.setText(String.format("₱%.2f", subtotal));
        tvTotal.setText(String.format("₱%.2f", total));
    }

    private void handleOrderPlacement() {
        if (productList.isEmpty()) {
            Toast.makeText(this, "Empty cart!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (userAddress == null) {
            Toast.makeText(this, "Please set a delivery address in your Account settings first!", Toast.LENGTH_LONG).show();
            return;
        }

        btnPlaceOrder.setEnabled(false);
        btnPlaceOrder.setText("Placing Order...");

        executor.execute(() -> {
            Map<String, Double> subtotalsBySeller = new HashMap<>();
            Map<String, List<Product>> itemsBySeller = new HashMap<>();

            for (Product product : productList) {
                String sId = product.getSellerId() != null ? product.getSellerId() : "UNKNOWN";
                double itemTotal = product.getPrice() * product.getCounter();

                subtotalsBySeller.put(sId, subtotalsBySeller.getOrDefault(sId, 0.0) + itemTotal);

                if (!itemsBySeller.containsKey(sId)) {
                    itemsBySeller.put(sId, new ArrayList<>());
                }
                itemsBySeller.get(sId).add(product);
            }

            String addressId = userAddress.getId();
            String fullAddressString = userAddress.getStreet() + ", " + userAddress.getBarangay() + ", " + userAddress.getCity();

            boolean allSuccess = true;

            for (Map.Entry<String, Double> entry : subtotalsBySeller.entrySet()) {
                String currentSellerId = entry.getKey().equals("UNKNOWN") ? null : entry.getKey();
                double shopSubtotal = entry.getValue();
                double shopTotal = shopSubtotal + deliveryFee;

                String newOrderId = authService.placeOrder(
                        sessionManager.getToken(),
                        sessionManager.getProfileId(),
                        currentSellerId,
                        addressId,
                        shopTotal,
                        fullAddressString
                );

                if (newOrderId != null) {
                    List<Product> shopItems = itemsBySeller.get(entry.getKey());
                    boolean itemsSaved = authService.addOrderItems(sessionManager.getToken(), newOrderId, shopItems);
                    if (!itemsSaved) {
                        allSuccess = false;
                    }
                } else {
                    allSuccess = false;
                }
            }

            boolean finalSuccess = allSuccess;
            runOnUiThread(() -> {
                btnPlaceOrder.setEnabled(true);
                btnPlaceOrder.setText("Place Order");
                if (finalSuccess) {
                    Toast.makeText(this, "Orders Placed Successfully!", Toast.LENGTH_SHORT).show();
                    ProductManager.getInstance().getCartItems().clear();
                    loadCartData();
                    updateTotalPrice();
                    finish();
                } else {
                    Toast.makeText(this, "Failed to place some items. Check Logcat!", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    @Override
    protected void onDestroy() {
        if (executor != null) {
            executor.shutdown();
        }
        super.onDestroy();
    }
}