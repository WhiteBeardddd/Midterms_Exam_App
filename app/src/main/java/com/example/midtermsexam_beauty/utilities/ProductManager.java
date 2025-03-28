package com.example.midtermsexam_beauty.utilities;

import com.example.midtermsexam_beauty.models.Product;

import java.util.ArrayList;
import java.util.List;

public class ProductManager {
    private static ProductManager productInstance;

    private final List<Product> productList;

    private ProductManager() {
        productList = new ArrayList<>();
    }

    public static synchronized ProductManager getInstance() {
        if (productInstance == null) {
            productInstance = new ProductManager();
        }
        return productInstance;
    }

    public void addProduct(Product product) {
        productList.add(product);
    }


    public void removeProduct(Product product) {
        int index = productList.indexOf(product);
        if (index != -1) {
            productList.remove(index);
        }
    }

    public List<Product> getProduct(){
        return productList;
    }

    public void clearProduct() {
        productList.clear();
    }
}
