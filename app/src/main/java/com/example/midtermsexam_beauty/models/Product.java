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
    private String address;
    private final String description;
    private final float price;
    private final String category;
    private final boolean availability;
    private final float rating;
    private final String skinType;
    private int counter;
    private String sellerId;
    private String imageUrl;
    private String shopName;
    private String id;
    private String mutableDescription;
    private String shopBackground;

    public Product(int imageID, String name, String description, float price, String category,
                   boolean availability, float rating, String skinType) {
        this.imageID      = imageID;
        this.name         = name;
        this.description  = description;
        this.price        = price;
        this.category     = category;
        this.availability = availability;
        this.rating       = rating;
        this.skinType     = skinType;
        this.counter      = 0;
    }

    public int getImageID()    { return imageID; }
    public int getImageId()    { return imageID; }
    public String getName()    { return name; }
    public float getPrice()    { return price; }
    public String getCategory(){ return category; }
    public boolean getAvalability()  { return availability; }
    public boolean isAvailability()  { return availability; }
    public float getRating()         { return rating; }
    public String getSkin_type()     { return skinType; }
    public int getCounter()          { return counter; }
    public void setCounter(int counter) { this.counter = counter; }

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }

    public String getShopBackground() { return shopBackground; }
    public void setShopBackground(String shopBackground) { this.shopBackground = shopBackground; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getMutableDescription() { return mutableDescription; }
    public void setDescription(String description) { this.mutableDescription = description; }

    // Single getDescription() — prefers mutableDescription, falls back to constructor value
    public String getDescription() {
        return (mutableDescription != null && !mutableDescription.isEmpty())
                ? mutableDescription
                : description;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

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
                JSONObject obj    = mealsArray.getJSONObject(i);
                int imageID       = res.getIdentifier(obj.getString("imageID"), "drawable", context.getPackageName());
                String name       = obj.getString("name");
                float price       = (float) obj.getDouble("price");
                String desc       = obj.getString("description");
                String category   = obj.getString("category");
                boolean avail     = obj.getBoolean("availability");
                float rating      = (float) obj.optDouble("rating", avail ? 4.8 : 4.2);
                String skinType   = obj.optString("skin_type", avail ? "Available now" : "Unavailable");

                productList.add(new Product(imageID, name, desc, price, category, avail, rating, skinType));
            }
        } catch (IOException | JSONException e) { e.printStackTrace(); }
        return productList;
    }

    public static List<Product> getMeals(Context context, String fromWhere) {
        return loadMealsFromJSON(context, "meals.json", fromWhere);
    }
}