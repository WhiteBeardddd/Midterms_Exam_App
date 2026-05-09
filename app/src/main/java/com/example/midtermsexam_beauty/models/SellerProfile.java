package com.example.midtermsexam_beauty.models;

import com.google.gson.annotations.SerializedName;

public class SellerProfile {

    @SerializedName("id")
    private String id;

    @SerializedName("profile_id")
    private String profileId;

    @SerializedName("store_name")
    private String storeName;

    @SerializedName("description")
    private String description;

    @SerializedName("address")
    private String address;

    @SerializedName("is_open")
    private boolean isOpen;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

    // NEW
    @SerializedName("seller_avatar_url")
    private String sellerAvatarUrl;

    @SerializedName("seller_profile_bg")
    private String sellerProfileBg;

    public SellerProfile(
            String id,
            String profileId,
            String storeName,
            String description,
            String address,
            boolean isOpen,
            String sellerAvatarUrl,
            String sellerProfileBg
    ) {
        this.id = id;
        this.profileId = profileId;
        this.storeName = storeName;
        this.description = description;
        this.address = address;
        this.isOpen = isOpen;
        this.sellerAvatarUrl = sellerAvatarUrl;
        this.sellerProfileBg = sellerProfileBg;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getProfileId() {
        return profileId;
    }

    public String getStoreName() {
        return storeName;
    }

    public String getDescription() {
        return description;
    }

    public String getAddress() {
        return address;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    // NEW
    public String getSellerAvatarUrl() {
        return sellerAvatarUrl;
    }

    public String getSellerProfileBg() {
        return sellerProfileBg;
    }
}