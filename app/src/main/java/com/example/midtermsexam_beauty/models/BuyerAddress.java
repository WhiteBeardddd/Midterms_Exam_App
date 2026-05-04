package com.example.midtermsexam_beauty.models;

import com.google.gson.annotations.SerializedName;

public class BuyerAddress {
    @SerializedName("id")
    private String id;

    @SerializedName("buyer_id")
    private String buyerId;

    @SerializedName("postal_code")
    private int postalCode;

    @SerializedName("street")
    private String street;

    @SerializedName("barangay")
    private String barangay;

    @SerializedName("city")
    private String city;

    @SerializedName("country")
    private String country;

    @SerializedName("longitude")
    private double longitude;

    @SerializedName("latitude")
    private double latitude;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

    // Getters
    public String getId() { return id; }
    public String getBuyerId() { return buyerId; }
    public int getPostalCode() { return postalCode; }
    public String getStreet() { return street; }
    public String getBarangay() { return barangay; }
    public String getCity() { return city; }
    public String getCountry() { return country; }
    public double getLongitude() { return longitude; }
    public double getLatitude() { return latitude; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setBuyerId(String buyerId) { this.buyerId = buyerId; }
    public void setPostalCode(int postalCode) { this.postalCode = postalCode; }
    public void setStreet(String street) { this.street = street; }
    public void setBarangay(String barangay) { this.barangay = barangay; }
    public void setCity(String city) { this.city = city; }
    public void setCountry(String country) { this.country = country; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
}