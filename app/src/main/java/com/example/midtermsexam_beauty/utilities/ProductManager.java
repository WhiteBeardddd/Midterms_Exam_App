package com.example.midtermsexam_beauty.utilities;

import com.example.midtermsexam_beauty.models.Product;

import java.util.HashMap;
import java.util.Map;

public class ProductManager {
    private static ProductManager productInstance;

    // Renamed to cartItems to better reflect its purpose in a FoodPanda-style app
    private final Map<Product, Integer> cartItems;

    private ProductManager() {
        cartItems = new HashMap<>();
    }

    public static synchronized ProductManager getInstance() {
        if (productInstance == null) {
            productInstance = new ProductManager();
        }
        return productInstance;
    }

    public void addProduct(Product product, int quantity) {
        // Using getOrDefault ensures we don't hit a NullPointerException
        int currentQty = cartItems.getOrDefault(product, 0);
        cartItems.put(product, currentQty + quantity);
    }

    public void removeProduct(Product product) {
        cartItems.remove(product);
    }

    // FIXED: Renamed from getProduct() to getCartItems()
    // to match the call in Checkout.java
    public Map<Product, Integer> getCartItems() {
        return cartItems;
    }

    public void clearCart() {
        cartItems.clear();
    }
}