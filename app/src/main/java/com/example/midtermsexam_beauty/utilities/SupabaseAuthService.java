package com.example.midtermsexam_beauty.utilities;

import android.util.Log;

import com.example.midtermsexam_beauty.BuildConfig;
import com.example.midtermsexam_beauty.models.BuyerAddress;
import com.example.midtermsexam_beauty.models.MenuItem;
import com.example.midtermsexam_beauty.models.Product;
import com.example.midtermsexam_beauty.models.Profile;
import com.example.midtermsexam_beauty.R;

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
                    p.setAvatarUrl(obj.optString("avatar_url", ""));
                    return p;
                }
            }
        } catch (Exception e) { Log.e(TAG, "getProfile error", e); }
        return null;
    }

    public boolean updateProfile(String token, String authId, String fullName, String phone, boolean isSeller, String avatarUrl) {
        if (token == null || authId == null) return false;
        try {
            JSONObject payload = new JSONObject()
                    .put("auth_id", authId)
                    .put("full_name", fullName != null ? fullName : "")
                    .put("phone", phone != null ? phone : "")
                    .put("is_seller", isSeller)
                    .put("avatar_url", avatarUrl != null ? avatarUrl : "");

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
            conn.disconnect();
            return code >= 200 && code < 300;
        } catch (Exception e) { return false; }
    }

    public boolean saveStoreName(String token, String profileId, String storeName) {
        if (token == null || profileId == null) return false;
        try {
            URL checkUrl = new URL(getBaseUrl() + "/rest/v1/seller_profiles?profile_id=eq." + profileId + "&select=id");
            HttpURLConnection checkConn = (HttpURLConnection) checkUrl.openConnection();
            checkConn.setRequestMethod("GET");
            checkConn.setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY);
            checkConn.setRequestProperty("Authorization", "Bearer " + token);

            int checkCode = checkConn.getResponseCode();
            String checkBody = readStream(checkCode < 300 ? checkConn.getInputStream() : checkConn.getErrorStream());
            checkConn.disconnect();

            JSONArray arr = new JSONArray(checkBody);
            JSONObject payload = new JSONObject()
                    .put("profile_id", profileId)
                    .put("store_name", storeName != null ? storeName : "")
                    .put("is_open", true);

            if (arr.length() > 0) {
                String existingSellerId = arr.getJSONObject(0).getString("id");
                return patch("/rest/v1/seller_profiles?id=eq." + existingSellerId, payload.toString(), token);
            } else {
                HttpResponse res = post("/rest/v1/seller_profiles", payload.toString(), token);
                return res.statusCode >= 200 && res.statusCode < 300;
            }
        } catch (Exception e) { return false; }
    }

    public String getStoreName(String token, String profileId) {
        try {
            URL url = new URL(getBaseUrl() + "/rest/v1/seller_profiles?profile_id=eq." + profileId + "&select=store_name");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + token);
            int code = conn.getResponseCode();
            String body = readStream(code < 300 ? conn.getInputStream() : conn.getErrorStream());
            conn.disconnect();

            if (code >= 200 && code < 300) {
                JSONArray arr = new JSONArray(body);
                if (arr.length() > 0) return arr.getJSONObject(0).optString("store_name", "");
            }
        } catch (Exception e) { }
        return "";
    }

    public String getSellerIdByAuthId(String token, String authId) {
        try {
            URL url = new URL(getBaseUrl() + "/rest/v1/profile?auth_id=eq." + authId + "&select=id");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + token);
            int code = conn.getResponseCode();
            String body = readStream(code < 300 ? conn.getInputStream() : conn.getErrorStream());
            conn.disconnect();

            if (code >= 200 && code < 300) {
                JSONArray arr = new JSONArray(body);
                if (arr.length() == 0) return null;
                String profileId = arr.getJSONObject(0).getString("id");

                URL sUrl = new URL(getBaseUrl() + "/rest/v1/seller_profiles?profile_id=eq." + profileId + "&select=id");
                HttpURLConnection sConn = (HttpURLConnection) sUrl.openConnection();
                sConn.setRequestMethod("GET");
                sConn.setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY);
                sConn.setRequestProperty("Authorization", "Bearer " + token);
                int sCode = sConn.getResponseCode();
                String sBody = readStream(sCode < 300 ? sConn.getInputStream() : sConn.getErrorStream());
                sConn.disconnect();

                if (sCode >= 200 && sCode < 300) {
                    JSONArray sArr = new JSONArray(sBody);
                    if (sArr.length() == 0) return null;
                    return sArr.getJSONObject(0).getString("id");
                }
            }
        } catch (Exception e) { }
        return null;
    }

    public List<Product> getAllShops(String token) {
        List<Product> shops = new ArrayList<>();
        if (token == null) return shops;
        try {
            URL url = new URL(getBaseUrl() + "/rest/v1/seller_profiles?select=id,store_name,description,profile(full_name,avatar_url)");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + token);
            int code = conn.getResponseCode();
            String body = readStream(code < 300 ? conn.getInputStream() : conn.getErrorStream());
            conn.disconnect();

            if (code >= 200 && code < 300) {
                JSONArray arr = new JSONArray(body);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    String id = obj.getString("id");

                    String shopName = "Unnamed Shop";
                    String avatarUrl = "";

                    if (obj.has("store_name") && !obj.isNull("store_name") && !obj.getString("store_name").isEmpty()){
                        shopName = obj.optString("store_name", shopName);
                    } else if (obj.has("profile") && !obj.isNull("profile")) {
                        shopName = obj.getJSONObject("profile").optString("full_name", shopName);
                    }

                    if (obj.has("profile") && !obj.isNull("profile")) {
                        avatarUrl = obj.getJSONObject("profile").optString("avatar_url", "");
                    }

                    Product shop = new Product(R.drawable.product_1, shopName, obj.optString("description", "A great place to eat!"), 0.0f, "Restaurant", true, 4.8f, "All");
                    shop.setSellerId(id);
                    shop.setImageUrl(avatarUrl);
                    shops.add(shop);
                }
            }
        } catch (Exception e) { }
        return shops;
    }

    public List<Product> getRandomMenuItems(String token) {
        List<Product> items = new ArrayList<>();
        if (token == null) return items;
        try {
            URL url = new URL(getBaseUrl() + "/rest/v1/menu_items?select=*,seller_profiles(store_name,profile(full_name))&is_available=eq.true&limit=50");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + token);
            int code = conn.getResponseCode();
            String body = readStream(code < 300 ? conn.getInputStream() : conn.getErrorStream());
            conn.disconnect();

            if (code >= 200 && code < 300) {
                JSONArray arr = new JSONArray(body);
                List<JSONObject> jsonList = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) jsonList.add(arr.getJSONObject(i));
                java.util.Collections.shuffle(jsonList);

                for (int i = 0; i < Math.min(jsonList.size(), 20); i++) {
                    JSONObject obj = jsonList.get(i);
                    String shopName = "Unknown Shop";

                    if (obj.has("seller_profiles") && !obj.isNull("seller_profiles")) {
                        JSONObject sp = obj.getJSONObject("seller_profiles");
                        if (sp.has("store_name") && !sp.isNull("store_name") && !sp.getString("store_name").isEmpty()) {
                            shopName = sp.getString("store_name");
                        } else if (sp.has("profile") && !sp.isNull("profile")) {
                            shopName = sp.getJSONObject("profile").optString("full_name", shopName);
                        }
                    }

                    Product product = new Product(
                            R.drawable.product_1,
                            obj.getString("name"),
                            obj.optString("description", ""),
                            (float) obj.optDouble("price", 0.0),
                            obj.optString("category", "Food"),
                            true,
                            4.8f,
                            shopName
                    );
                    product.setSellerId(obj.getString("seller_id"));
                    product.setImageUrl(obj.optString("image_url", ""));
                    product.setShopName(shopName);
                    items.add(product);
                }
            }
        } catch (Exception e) { Log.e(TAG, "getRandomMenuItems error", e); }
        return items;
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
        } catch (Exception e) { }
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
            conn.setRequestProperty("Prefer", "return=representation");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            String body = readStream(code < 300 ? conn.getInputStream() : conn.getErrorStream());
            conn.disconnect();

            if (code >= 200 && code < 300) {
                JSONArray arr = new JSONArray(body);
                if (arr.length() > 0) return arr.getJSONObject(0).getString("id");
            }
        } catch (Exception e) { }
        return null;
    }

    public boolean updateMenuItem(String token, MenuItem item) {
        try {
            if (item.getId() == null || item.getId().isEmpty()) return false;
            JSONObject payload = new JSONObject()
                    .put("name", item.getName())
                    .put("description", item.getDescription() != null ? item.getDescription() : "")
                    .put("price", item.getPrice())
                    .put("category", item.getCategory() != null ? item.getCategory() : "")
                    .put("is_available", item.isAvailable())
                    .put("image_url", item.getImageUrl() != null ? item.getImageUrl() : "")
                    .put("seller_id", item.getSellerId());

            return patch("/rest/v1/menu_items?id=eq." + item.getId(), payload.toString(), token);
        } catch (Exception e) { return false; }
    }

    public boolean updateMenuItemAvailability(String token, String itemId, boolean isAvailable) {
        try {
            JSONObject payload = new JSONObject().put("is_available", isAvailable);
            return patch("/rest/v1/menu_items?id=eq." + itemId, payload.toString(), token);
        } catch (Exception e) { return false; }
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
            conn.disconnect();
            return code >= 200 && code < 300;
        } catch (Exception e) { return false; }
    }

    public String uploadImage(String token, String bucket, String path, byte[] data, String mimeType) {
        try {
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
            conn.disconnect();

            if (code >= 200 && code < 300) {
                return getBaseUrl() + "/storage/v1/object/public/" + bucket + "/" + path;
            }
        } catch (Exception e) { }
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
        } catch (Exception e) { }
        return stats;
    }

    private boolean patch(String path, String body, String token) throws IOException {
        URL url = new URL(getBaseUrl() + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            Object delegate = conn;
            try {
                java.lang.reflect.Field f = conn.getClass().getDeclaredField("delegate");
                f.setAccessible(true);
                delegate = f.get(conn);
            } catch (Exception ignored) {}

            java.lang.reflect.Method m = delegate.getClass().getDeclaredMethod("setRequestMethod", String.class);
            m.setAccessible(true);
            m.invoke(delegate, "PATCH");
        } catch (Exception e) {}

        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Prefer", "return=minimal");
        conn.setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY);
        conn.setRequestProperty("Authorization", "Bearer " + token);

        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        conn.setRequestProperty("Content-Length", String.valueOf(bodyBytes.length));

        try (OutputStream os = conn.getOutputStream()) {
            os.write(bodyBytes);
            os.flush();
        }

        int code = conn.getResponseCode();
        conn.disconnect();
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
            conn.getResponseCode();
            conn.disconnect();
        } catch (Exception e) { }
    }

    public BuyerAddress getBuyerAddress(String token, String buyerId) {
        if (token == null || buyerId == null) return null;
        try {
            URL url = new URL(getBaseUrl() + "/rest/v1/buyer_address?buyer_id=eq." + buyerId + "&select=*");
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
                    BuyerAddress addr = new BuyerAddress();
                    addr.setId(obj.optString("id"));
                    addr.setBuyerId(obj.optString("buyer_id"));
                    addr.setStreet(obj.optString("street"));
                    addr.setBarangay(obj.optString("barangay"));
                    addr.setCity(obj.optString("city"));
                    addr.setCountry(obj.optString("country"));
                    addr.setPostalCode(obj.optInt("postal_code", 0));
                    return addr;
                }
            }
        } catch (Exception e) { Log.e(TAG, "getBuyerAddress error", e); }
        return null;
    }

    public boolean saveBuyerAddress(String token, BuyerAddress address) {
        try {
            JSONObject payload = new JSONObject()
                    .put("buyer_id", address.getBuyerId())
                    .put("street", address.getStreet())
                    .put("barangay", address.getBarangay())
                    .put("city", address.getCity())
                    .put("country", address.getCountry())
                    .put("postal_code", address.getPostalCode());

            URL url = new URL(getBaseUrl() + "/rest/v1/buyer_address?on_conflict=buyer_id");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Prefer", "resolution=merge-duplicates");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
            }
            int code = conn.getResponseCode();
            conn.disconnect();
            return code >= 200 && code < 300;
        } catch (Exception e) { return false; }
    }

    public boolean updatePassword(String token, String newPassword) {
        if (token == null || newPassword == null) return false;
        try {
            JSONObject payload = new JSONObject().put("password", newPassword);
            URL url = new URL(getBaseUrl() + "/auth/v1/user");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("PUT");
            conn.setDoOutput(true);
            conn.setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Content-Type", "application/json");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
            }
            int code = conn.getResponseCode();
            conn.disconnect();
            return code >= 200 && code < 300;
        } catch (Exception e) { return false; }
    }
}