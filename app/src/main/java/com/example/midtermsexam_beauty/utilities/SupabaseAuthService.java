package com.example.midtermsexam_beauty.utilities;

import android.util.Log;
import com.example.midtermsexam_beauty.BuildConfig;
import com.example.midtermsexam_beauty.models.MenuItem;
import com.example.midtermsexam_beauty.models.Profile;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class SupabaseAuthService {
    private static final String TAG = "SupabaseAuth";

    public static class AuthResult {
        public final boolean success;
        public final String message;
        public final String accessToken;
        public final String userId;

        public AuthResult(boolean success, String message, String accessToken, String userId) {
            this.success = success;
            this.message = message;
            this.accessToken = accessToken;
            this.userId = userId;
        }
    }

    public static class SellerStats {
        public double totalSales = 0;
        public int completedOrders = 0;
        public int pendingOrders = 0;
        public int totalItems = 0;
    }

    public boolean isConfigured() {
        return !BuildConfig.SUPABASE_URL.isEmpty() && !BuildConfig.SUPABASE_ANON_KEY.isEmpty();
    }

    private String getBaseUrl() {
        String url = BuildConfig.SUPABASE_URL;
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public AuthResult signUp(String email, String password, String username) {
        try {
            JSONObject payload = new JSONObject()
                    .put("email", email)
                    .put("password", password)
                    .put("data", new JSONObject().put("username", username));
            HttpResponse res = post("/auth/v1/signup", payload.toString(), null);
            return res.statusCode >= 200 && res.statusCode < 300
                    ? new AuthResult(true, "Check your email.", null, null)
                    : new AuthResult(false, extractErrorMessage(res.body), null, null);
        } catch (Exception e) { return new AuthResult(false, e.getMessage(), null, null); }
    }

    public AuthResult signIn(String email, String password) {
        try {
            JSONObject payload = new JSONObject().put("email", email).put("password", password);
            HttpResponse res = post("/auth/v1/token?grant_type=password", payload.toString(), null);
            if (res.statusCode >= 200 && res.statusCode < 300) {
                JSONObject json = new JSONObject(res.body);
                return new AuthResult(true, "Login success.", json.optString("access_token"),
                        json.has("user") ? json.getJSONObject("user").getString("id") : null);
            }
            return new AuthResult(false, extractErrorMessage(res.body), null, null);
        } catch (Exception e) { return new AuthResult(false, e.getMessage(), null, null); }
    }

    public Profile getProfile(String token, String authId) {
        if (token == null || authId == null) return null;
        try {
            URL url = new URL(getBaseUrl() + "/rest/v1/profile?auth_id=eq." + authId + "&select=*");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + token);
            int code = conn.getResponseCode();
            String body = readStream(code < 300 ? conn.getInputStream() : conn.getErrorStream());
            conn.disconnect();
            if (code >= 200 && code < 300) {
                JSONArray arr = new JSONArray(body);
                if (arr.length() > 0) {
                    JSONObject obj = arr.getJSONObject(0);
                    Profile p = new Profile();
                    p.setId(obj.optString("id"));
                    p.setAuthId(obj.optString("auth_id"));
                    p.setFullName(obj.optString("full_name"));
                    p.setPhone(obj.optString("phone"));
                    p.setSeller(obj.optBoolean("is_seller"));
                    return p;
                }
            }
            Log.e(TAG, "getProfile failed: " + code + " " + body);
        } catch (Exception e) { Log.e(TAG, "getProfile error", e); }
        return null;
    }

    public boolean updateProfile(String token, String authId, String fullName, String phone, boolean isSeller) {
        if (token == null || authId == null) return false;
        try {
            JSONObject payload = new JSONObject();
            payload.put("auth_id", authId);
            payload.put("full_name", fullName != null ? fullName : "");
            payload.put("phone", phone != null ? phone : "");
            payload.put("is_seller", isSeller);

            URL url = new URL(getBaseUrl() + "/rest/v1/profile?on_conflict=auth_id");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Prefer", "resolution=merge-duplicates,return=minimal");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            String body = readStream(code < 300 ? conn.getInputStream() : conn.getErrorStream());
            conn.disconnect();
            Log.d(TAG, "updateProfile: " + code + " " + body);
            return code >= 200 && code < 300;
        } catch (Exception e) { Log.e(TAG, "updateProfile error", e); return false; }
    }

    public String getSellerIdByAuthId(String token, String authId) {
        try {
            Log.d(TAG, "getSellerIdByAuthId authId: " + authId);
            URL url = new URL(getBaseUrl() + "/rest/v1/profile?auth_id=eq." + authId + "&select=id");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + token);
            int code = conn.getResponseCode();
            String body = readStream(code < 300 ? conn.getInputStream() : conn.getErrorStream());
            conn.disconnect();

            Log.d(TAG, "profile query: " + code + " " + body);

            if (code >= 200 && code < 300) {
                JSONArray arr = new JSONArray(body);
                if (arr.length() == 0) {
                    Log.e(TAG, "No profile found for authId: " + authId);
                    return null;
                }
                String profileId = arr.getJSONObject(0).getString("id");
                Log.d(TAG, "profileId: " + profileId);

                URL sUrl = new URL(getBaseUrl() + "/rest/v1/seller_profiles?profile_id=eq." + profileId + "&select=id");
                HttpURLConnection sConn = (HttpURLConnection) sUrl.openConnection();
                sConn.setRequestMethod("GET");
                sConn.setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY);
                sConn.setRequestProperty("Authorization", "Bearer " + token);
                int sCode = sConn.getResponseCode();
                String sBody = readStream(sCode < 300 ? sConn.getInputStream() : sConn.getErrorStream());
                sConn.disconnect();

                Log.d(TAG, "seller_profiles query: " + sCode + " " + sBody);

                if (sCode >= 200 && sCode < 300) {
                    JSONArray sArr = new JSONArray(sBody);
                    if (sArr.length() == 0) {
                        Log.e(TAG, "No seller_profile for profileId: " + profileId);
                        return null;
                    }
                    return sArr.getJSONObject(0).getString("id");
                }
            }
        } catch (Exception e) { Log.e(TAG, "getSellerIdByAuthId error", e); }
        return null;
    }

    public List<MenuItem> getMenuItems(String token, String sellerId) {
        List<MenuItem> items = new ArrayList<>();
        if (token == null || sellerId == null) return items;
        try {
            URL url = new URL(getBaseUrl() + "/rest/v1/menu_items?seller_id=eq." + sellerId + "&select=*&order=created_at.desc");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + token);
            int code = conn.getResponseCode();
            String body = readStream(code < 300 ? conn.getInputStream() : conn.getErrorStream());
            conn.disconnect();

            Log.d(TAG, "getMenuItems: " + code + " items raw: " + body);

            if (code >= 200 && code < 300) {
                JSONArray arr = new JSONArray(body);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    MenuItem item = new MenuItem();
                    item.setId(obj.getString("id"));
                    item.setSellerId(obj.getString("seller_id"));
                    item.setName(obj.getString("name"));
                    item.setDescription(obj.optString("description", ""));
                    item.setPrice(obj.optDouble("price", 0));
                    item.setCategory(obj.optString("category", ""));
                    item.setAvailable(obj.optBoolean("is_available", true));
                    item.setImageUrl(obj.optString("image_url", ""));
                    items.add(item);
                }
            }
        } catch (Exception e) { Log.e(TAG, "getMenuItems error", e); }
        return items;
    }

    public String addMenuItem(String token, MenuItem item) {
        try {
            JSONObject payload = new JSONObject()
                    .put("seller_id", item.getSellerId())
                    .put("name", item.getName())
                    .put("description", item.getDescription() != null ? item.getDescription() : "")
                    .put("price", item.getPrice())
                    .put("category", item.getCategory() != null ? item.getCategory() : "")
                    .put("is_available", item.isAvailable())
                    .put("image_url", item.getImageUrl() != null ? item.getImageUrl() : "");

            URL url = new URL(getBaseUrl() + "/rest/v1/menu_items");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Prefer", "return=representation"); // return the created row

            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            String body = readStream(code < 300 ? conn.getInputStream() : conn.getErrorStream());
            conn.disconnect();
            Log.d(TAG, "addMenuItem response: " + code + " " + body);

            if (code >= 200 && code < 300) {
                // Parse and return the new row's id
                JSONArray arr = new JSONArray(body);
                if (arr.length() > 0) {
                    return arr.getJSONObject(0).getString("id");
                }
            }
        } catch (Exception e) { Log.e(TAG, "addMenuItem error", e); }
        return null; // null means failed
    }

    public boolean updateMenuItem(String token, MenuItem item) {
        try {
            if (item.getId() == null || item.getId().isEmpty()) {
                Log.e(TAG, "updateMenuItem called with null/empty id!");
                return false;
            }

            JSONObject payload = new JSONObject()
                    .put("name", item.getName())
                    .put("description", item.getDescription() != null ? item.getDescription() : "")
                    .put("price", item.getPrice())
                    .put("category", item.getCategory() != null ? item.getCategory() : "")
                    .put("is_available", item.isAvailable())
                    .put("image_url", item.getImageUrl() != null ? item.getImageUrl() : "");
                    payload.put("seller_id", item.getSellerId());
            // DO NOT include id or seller_id in payload — only filter by id in URL
            Log.d(TAG, "updateMenuItem id: " + item.getId());
            Log.d(TAG, "updateMenuItem payload: " + payload);

            return patch("/rest/v1/menu_items?id=eq." + item.getId(), payload.toString(), token);
        } catch (Exception e) { Log.e(TAG, "updateMenuItem error", e); return false; }
    }

    public boolean updateMenuItemAvailability(String token, String itemId, boolean isAvailable) {
        try {
            Log.d(TAG, "updateAvailability itemId: " + itemId + " -> " + isAvailable);
            JSONObject payload = new JSONObject().put("is_available", isAvailable);
            boolean result = patch("/rest/v1/menu_items?id=eq." + itemId, payload.toString(), token);
            Log.d(TAG, "updateAvailability result: " + result);
            return result;
        } catch (Exception e) { Log.e(TAG, "updateMenuItemAvailability error", e); return false; }
    }

    public boolean deleteMenuItem(String token, String itemId) {
        try {
            URL url = new URL(getBaseUrl() + "/rest/v1/menu_items?id=eq." + itemId);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("DELETE");
            conn.setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Prefer", "return=minimal");
            int code = conn.getResponseCode();
            String body = readStream(code < 300 ? conn.getInputStream() : conn.getErrorStream());
            conn.disconnect();
            Log.d(TAG, "deleteMenuItem: " + code + " " + body);
            return code >= 200 && code < 300;
        } catch (Exception e) { Log.e(TAG, "deleteMenuItem error", e); return false; }
    }

    public String uploadImage(String token, String bucket, String path, byte[] data, String mimeType) {
        try {
            Log.d(TAG, "uploadImage path: " + path + " size: " + (data != null ? data.length : 0));
            URL url = new URL(getBaseUrl() + "/storage/v1/object/" + bucket + "/" + path);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("PUT");
            conn.setDoOutput(true);
            conn.setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Content-Type", mimeType);
            conn.setRequestProperty("x-upsert", "true");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(data);
            }
            int code = conn.getResponseCode();
            String body = readStream(code < 300 ? conn.getInputStream() : conn.getErrorStream());
            conn.disconnect();

            Log.d(TAG, "uploadImage response: " + code + " " + body);

            if (code >= 200 && code < 300) {
                return getBaseUrl() + "/storage/v1/object/public/" + bucket + "/" + path;
            }
        } catch (Exception e) { Log.e(TAG, "uploadImage error", e); }
        return null;
    }

    public SellerStats getStats(String token, String sellerId) {
        SellerStats stats = new SellerStats();
        if (sellerId == null) return stats;
        try {
            URL url = new URL(getBaseUrl() + "/rest/v1/orders?seller_id=eq." + sellerId + "&select=total_amount,status");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + token);
            int code = conn.getResponseCode();
            String body = readStream(code < 300 ? conn.getInputStream() : conn.getErrorStream());
            conn.disconnect();

            if (code >= 200 && code < 300) {
                JSONArray orders = new JSONArray(body);
                for (int i = 0; i < orders.length(); i++) {
                    JSONObject order = orders.getJSONObject(i);
                    String status = order.optString("status", "");
                    double amount = order.optDouble("total_amount", 0);
                    if (status.equalsIgnoreCase("delivered")) {
                        stats.totalSales += amount;
                        stats.completedOrders++;
                    } else if (!status.equalsIgnoreCase("cancelled")) {
                        stats.pendingOrders++;
                    }
                }
            }

            URL mUrl = new URL(getBaseUrl() + "/rest/v1/menu_items?seller_id=eq." + sellerId + "&select=id");
            HttpURLConnection mConn = (HttpURLConnection) mUrl.openConnection();
            mConn.setRequestMethod("GET");
            mConn.setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY);
            mConn.setRequestProperty("Authorization", "Bearer " + token);
            int mCode = mConn.getResponseCode();
            String mBody = readStream(mCode < 300 ? mConn.getInputStream() : mConn.getErrorStream());
            mConn.disconnect();
            if (mCode >= 200 && mCode < 300) stats.totalItems = new JSONArray(mBody).length();
        } catch (Exception e) { Log.e(TAG, "getStats error", e); }
        return stats;
    }

    // --- HELPERS ---

    private boolean patch(String path, String body, String token) throws IOException {
        URL url = new URL(getBaseUrl() + path);

        // Android-compatible PATCH workaround
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            // This reflection approach works on Android's okhttp-backed HttpURLConnection
            Object delegate = conn;
            try {
                java.lang.reflect.Field f = conn.getClass().getDeclaredField("delegate");
                f.setAccessible(true);
                delegate = f.get(conn);
            } catch (Exception ignored) {}

            java.lang.reflect.Method m = delegate.getClass().getDeclaredMethod("setRequestMethod", String.class);
            m.setAccessible(true);
            m.invoke(delegate, "PATCH");
        } catch (Exception e) {
            Log.e(TAG, "PATCH reflection failed: " + e.getMessage());
        }

        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Prefer", "return=minimal");
        conn.setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY);
        conn.setRequestProperty("Authorization", "Bearer " + token);

        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        conn.setRequestProperty("Content-Length", String.valueOf(bodyBytes.length));

        Log.d(TAG, "=== PATCH REQUEST ===");
        Log.d(TAG, "URL: " + url);
        Log.d(TAG, "Body: " + body);
        Log.d(TAG, "Method: " + conn.getRequestMethod());

        try (OutputStream os = conn.getOutputStream()) {
            os.write(bodyBytes);
            os.flush();
        }

        int code = conn.getResponseCode();
        String responseBody = readStream(code < 300 ? conn.getInputStream() : conn.getErrorStream());
        conn.disconnect();

        Log.d(TAG, "=== PATCH RESPONSE ===");
        Log.d(TAG, "Status: " + code);
        Log.d(TAG, "Response: " + responseBody);

        return code >= 200 && code < 300;
    }
    private HttpResponse post(String path, String body, String token) throws IOException {
        URL url = new URL(getBaseUrl() + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY);
        if (token != null) conn.setRequestProperty("Authorization", "Bearer " + token);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
        int code = conn.getResponseCode();
        String resp = readStream(code < 300 ? conn.getInputStream() : conn.getErrorStream());
        conn.disconnect();
        return new HttpResponse(code, resp);
    }

    private String readStream(InputStream is) throws IOException {
        if (is == null) return "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        return sb.toString();
    }

    private String extractErrorMessage(String body) {
        try {
            JSONObject json = new JSONObject(body);
            return json.optString("message", json.optString("msg", body));
        } catch (Exception e) { return body; }
    }

    private static class HttpResponse {
        final int statusCode;
        final String body;
        HttpResponse(int s, String b) { this.statusCode = s; this.body = b; }
    }

    public boolean isSeller(String token, String authId) {
        Profile p = getProfile(token, authId);
        return p != null && p.isSeller();
    }

    public void deleteImage(String token, String bucket, String path) {
        try {
            URL url = new URL(getBaseUrl() + "/storage/v1/object/" + bucket + "/" + path);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("DELETE");
            conn.setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + token);
            int code = conn.getResponseCode();
            Log.d(TAG, "deleteImage: " + code + " path: " + path);
            conn.disconnect();
        } catch (Exception e) { Log.e(TAG, "deleteImage error", e); }
    }
}