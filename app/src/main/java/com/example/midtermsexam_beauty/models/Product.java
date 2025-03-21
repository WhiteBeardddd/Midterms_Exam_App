package com.example.midtermsexam_beauty.models;

import com.example.midtermsexam_beauty.R;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class Product {
    private final int imageId;
    private final String name;
    private final float price;
    private final String description;
    private int counter;
    private final float rating;
    private final String category;
    private final String skin_type;

    public Product(int imageId, String name, float price, String description, int counter, float rating, String category, String skin_type) {
        this.imageId = imageId;
        this.name = name;
        this.price = price;
        this.description = description;
        this.counter = counter;
        this.rating = rating;
        this.category = category;
        this.skin_type = skin_type;
    }

    public int getImageId() {
        return imageId;
    }

    public String getName() {
        return name;
    }

    public float getPrice() {
        return price;
    }

    public String getDescription() {
        return description;
    }

    public int getCounter() {
        return counter;
    }

    public float getRating() {
        return rating;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }

    public  String getCategory(){
        return category;
    }

    public String getSkin_type(){
        return skin_type;
    }

    public static List<Product> getDefaultProducts() {
        List<Product> products = new ArrayList<>();
        products.add(new Product(R.drawable.product_1, "Cerave", 300.00f, "Moisturizing Lotion", 0, 2.2f,"Ranked","Normal"));
        products.add(new Product(R.drawable.product_2, "Cerave", 200.00f, "Hydrating Toner", 0, 3.8f,"Hot","Dry"));
        products.add(new Product(R.drawable.product_3, "Cetaphil", 250.00f, "Facial Cleanser", 0, 2.9f,"Loved","Oily"));
        products.add(new Product(R.drawable.product_4, "Vitamin C", 300.00f, "Brightening Serum", 0, 3.3f,"Secret","Combination"));
        products.add(new Product(R.drawable.product_5, "Ordinary", 300.00f, "Toning Solutions", 0, 2.7f,"Ranked","Normal"));
        products.add(new Product(R.drawable.product_6, "Zaron", 400.00f, "Brightening Toner", 0, 3.5f,"Hot","Dry"));
        return products;
    }

    public static List<Product> getPopularProducts() {
        List<Product> products = new ArrayList<>();
        products.add(new Product(R.drawable.popular_product_1, "Sleeping Mask", 300.00f, "Black Mask", 0, 4.9f,"Secret","Combination"));
        products.add(new Product(R.drawable.popular_product_2, "Garnier", 200.00f, "Charcoal Mask", 0, 4.7f,"Hot","Dry"));
        products.add(new Product(R.drawable.popular_product_3, "Cerave", 250.00f, "Hydrating Sunscreen", 0, 4.7f,"Ranked","Normal"));
        products.add(new Product(R.drawable.popular_product_4, "Mario Badescu", 300.00f, "Facial Spray", 0, 4.5f,"Hot","Dry"));
        products.add(new Product(R.drawable.popular_product_5, "Nivea Men", 300.00f, "Lip Balm", 0, 4.6f,"Secret","Combination"));
        products.add(new Product(R.drawable.popular_product_6, "Rouge Dior", 400.00f, "Lipstick", 0, 4.3f,"Hot","Dry"));
        return products;
    }


}
