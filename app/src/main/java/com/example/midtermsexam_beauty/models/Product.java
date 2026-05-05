package com.example.midtermsexam_beauty.models;

import android.content.Context;
import android.content.res.Resources;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Product {
    private final int imageID;
    private final String name;
    private final String description;
    private final float price;
    private final String category;
    private final boolean availability;
    private final float rating;
    private final String skinType;
    private int counter;
    private String sellerId;
    private String imageUrl; // NEW: To hold the dynamic image from Supabase

    public Product(int imageID, String name, String description, float price, String category,
                   boolean availability, float rating, String skinType) {
        this.imageID = imageID;
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.availability = availability;
        this.rating = rating;
        this.skinType = skinType;
        this.counter = 0;
    }

    public int getImageID() { return imageID; }
    public int getImageId() { return imageID; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public float getPrice() { return price; }
    public String getCategory() { return category; }
    public boolean getAvalability() { return availability; }
    public boolean isAvailability() { return availability; }
    public float getRating() { return rating; }
    public String getSkin_type() { return skinType; }
    public int getCounter() { return counter; }
    public void setCounter(int counter) { this.counter = counter; }

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }

    // NEW: Getters and Setters for the Image URL
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    private static List<Product> loadMealsFromJSON(Context context, String fileName, String key) {
        List<Product> productList = new ArrayList<>();
        try {
            InputStream writer = context.getAssets().open(fileName);
            int size = writer.available();
            byte[] buffer = new byte[size];
            writer.read(buffer);
            writer.close();
            String json = new String(buffer, StandardCharsets.UTF_8);
            JSONObject jsonObject = new JSONObject(json);
            JSONArray mealsArray = jsonObject.getJSONArray(key);
            Resources res = context.getResources();

            for (int i = 0; i < mealsArray.length(); i++) {
                JSONObject obj = mealsArray.getJSONObject(i);
                int imageID = res.getIdentifier(obj.getString("imageID"), "drawable", context.getPackageName());
                String name = obj.getString("name");
                float price = (float) obj.getDouble("price");
                String description = obj.getString("description");
                String category = obj.getString("category");
                boolean availability = obj.getBoolean("availability");
                float rating = (float) obj.optDouble("rating", availability ? 4.8 : 4.2);
                String skinType = obj.optString("skin_type", availability ? "Available now" : "Unavailable");

                productList.add(new Product(imageID, name, description, price, category, availability, rating, skinType));
            }
        } catch (IOException | JSONException e) { e.printStackTrace(); }
        return productList;
    }

    public static List<Product> getMeals(Context context, String fromWhere) {
        return loadMealsFromJSON(context, "meals.json", fromWhere);
    }
}