package com.example.midtermsexam_beauty.utilities;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "TaraBytesSession";
    private static final String KEY_TOKEN = "access_token";
    private static final String KEY_USER_ID = "user_id"; // This is the Auth ID
    private static final String KEY_PROFILE_ID = "profile_id"; // NEW: This is the Profile table ID
    private static final String KEY_IS_SELLER = "is_seller";
    private static final String KEY_SELLER_ID = "seller_id";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveSession(String token, String userId) {
        prefs.edit()
                .putString(KEY_TOKEN, token)
                .putString(KEY_USER_ID, userId)
                .apply();
    }

    // --- NEW PROFILE ID METHODS ---
    public void setProfileId(String profileId) {
        prefs.edit().putString(KEY_PROFILE_ID, profileId).apply();
    }

    public String getProfileId() {
        return prefs.getString(KEY_PROFILE_ID, null);
    }
    // ------------------------------

    public void setSellerId(String sellerId) {
        prefs.edit().putString(KEY_SELLER_ID, sellerId).apply();
    }

    public void setIsSeller(boolean isSeller) {
        prefs.edit().putBoolean(KEY_IS_SELLER, isSeller).apply();
    }

    public String getToken() { return prefs.getString(KEY_TOKEN, null); }
    public String getUserId() { return prefs.getString(KEY_USER_ID, null); }
    public String getSellerId() { return prefs.getString(KEY_SELLER_ID, null); }
    public boolean isSeller() { return prefs.getBoolean(KEY_IS_SELLER, false); }

    public void clearSession() { prefs.edit().clear().apply(); }
    public boolean isLoggedIn() { return getToken() != null; }
}