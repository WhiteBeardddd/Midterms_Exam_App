package com.example.midtermsexam_beauty.utilities;

import android.os.Build;
import android.util.Log;

import com.example.midtermsexam_beauty.BuildConfig;
import com.example.midtermsexam_beauty.models.BuyerAddress;
import com.example.midtermsexam_beauty.models.MenuItem;
import com.example.midtermsexam_beauty.models.Product;
import com.example.midtermsexam_beauty.models.Profile;
import com.example.midtermsexam_beauty.models.Order;
import com.example.midtermsexam_beauty.R;
import com.example.midtermsexam_beauty.models.SellerProfile;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

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

    // ─── Paste this inside SupabaseAuthService.java ───────────────────────────

    public static class OrderDetail {
        public String orderId;
        public String status;
        public double totalAmount;
        public String createdAt;

        // Buyer info
        public String buyerFullName;

        // Address fields
        public String street;
        public String barangay;
        public String city;
        public String postalCode;
        public String country;

        // Items
        public List<OrderItemDetail> items = new ArrayList<>();
    }

    public static class OrderItemDetail {
        public String menuItemName;
        public int quantity;
        public double unitPrice;
    }

    /**
     * Fetches all orders for the currently logged-in seller, including:
     *  - buyer profile (full_name)
     *  - buyer_address (street, barangay, city, postal_code, country)
     *  - order_items joined with menu_items (name, quantity, unit_price)
     *
     * RLS on the DB ensures only the seller's own orders are returned.
     */
    public List<OrderDetail> getSellerOrders(String token, String sellerId) {
        List<OrderDetail> result = new ArrayList<>();
        if (token == null || sellerId == null) return result;
        try {
            // Single query: join order_items→menu_items, buyer profile, and buyer_address
            String query = "/rest/v1/orders"
                    + "?seller_id=eq." + sellerId
                    + "&select="
                    + "id,"
                    + "status,"
                    + "total_amount,"
                    + "created_at,"
                    + "profile!orders_buyer_id_fkey(full_name),"
                    + "buyer_address!orders_buyer_address_id_fkey(street,barangay,city,postal_code,country),"
                    + "order_items(quantity,unit_price,menu_items(name))"
                    + "&order=created_at.desc";

            URL url = new URL(getBaseUrl() + query);
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
                    JSONObject o = arr.getJSONObject(i);
                    OrderDetail od = new OrderDetail();

                    od.orderId     = o.optString("id");
                    od.status      = o.optString("status", "pending");
                    od.totalAmount = o.optDouble("total_amount", 0);
                    od.createdAt   = o.optString("created_at", "");

                    // Buyer name via profile join
                    if (!o.isNull("profile")) {
                        JSONObject profile = o.getJSONObject("profile");
                        od.buyerFullName = profile.optString("full_name", "Unknown Buyer");
                    } else {
                        od.buyerFullName = "Unknown Buyer";
                    }

                    // Buyer address via buyer_address join
                    if (!o.isNull("buyer_address")) {
                        JSONObject addr = o.getJSONObject("buyer_address");
                        od.street     = addr.optString("street", "");
                        od.barangay   = addr.optString("barangay", "");
                        od.city       = addr.optString("city", "");
                        od.postalCode = String.valueOf(addr.optInt("postal_code", 0));
                        od.country    = addr.optString("country", "");
                    }

                    // Order items joined with menu_items
                    if (!o.isNull("order_items")) {
                        JSONArray itemsArr = o.getJSONArray("order_items");
                        for (int j = 0; j < itemsArr.length(); j++) {
                            JSONObject oi = itemsArr.getJSONObject(j);
                            OrderItemDetail item = new OrderItemDetail();
                            item.quantity  = oi.optInt("quantity", 1);
                            item.unitPrice = oi.optDouble("unit_price", 0);

                            if (!oi.isNull("menu_items")) {
                                item.menuItemName = oi.getJSONObject("menu_items").optString("name", "Item");
                            } else {
                                item.menuItemName = "Item";
                            }
                            od.items.add(item);
                        }
                    }

                    result.add(od);
                }
            } else {
                Log.e(TAG, "getSellerOrders failed [" + code + "]: " + body);
            }
        } catch (Exception e) {
            Log.e(TAG, "getSellerOrders error", e);
        }
        return result;
    }


    public boolean isConfigured() {
        return !BuildConfig.SUPABASE_URL.isEmpty() && !BuildConfig.SUPABASE_ANON_KEY.isEmpty();
    }

    private String getBaseUrl() {
        String url = BuildConfig.SUPABASE_URL;
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    // --- FULLY UPDATED ORDER METHOD ---
    // --- FULLY UPDATED ORDER METHOD ---
    public String placeOrder(String token, String buyerId, String sellerId, String buyerAddressId, double totalAmount, String address) {
        try {
            JSONObject orderPayload = new JSONObject();
            orderPayload.put("buyer_id", buyerId);
            orderPayload.put("total_amount", totalAmount);
            orderPayload.put("status", "pending");
            orderPayload.put("address", address != null ? address : "Default Delivery Address");

            if (sellerId != null && !sellerId.isEmpty()) {
                orderPayload.put("seller_id", sellerId);
            }
            if (buyerAddressId != null && !buyerAddressId.isEmpty()) {
                orderPayload.put("buyer_address_id", buyerAddressId);
            }

            URL url = new URL(getBaseUrl() + "/rest/v1/orders");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Content-Type", "application/json");

            // IMPORTANT: This tells Supabase to return the newly created row!
            conn.setRequestProperty("Prefer", "return=representation");

            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = orderPayload.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                String body = readStream(conn.getInputStream());
                JSONArray arr = new JSONArray(body);
                conn.disconnect();
                if (arr.length() > 0) {
                    return arr.getJSONObject(0).getString("id"); // Return the Order UUID
                } else {
                    // THE TRAP: If Supabase hides the data, print this error!
                    Log.e(TAG, "CRITICAL: Order inserted, but Supabase returned a blank array! Your SELECT policy on the 'orders' table is blocking the buyer from seeing their own order.");
                }
            } else {
                Log.e(TAG, "Failed order: " + readStream(conn.getErrorStream()));
            }
            conn.disconnect();
        } catch (Exception e) {
            Log.e(TAG, "placeOrder error", e);
        }
        return null;
    }

    public boolean addOrderItems(String token, String orderId, List<Product> items) {
        try {
            JSONArray payload = new JSONArray();
            for (Product p : items) {
                // Ensure the item has a valid UUID (prevents crashes from dummy data)
                if (p.getId() != null && p.getId().length() > 20) {
                    JSONObject itemObj = new JSONObject();
                    itemObj.put("order_id", orderId);
                    itemObj.put("menu_item_id", p.getId());
                    itemObj.put("quantity", p.getCounter());
                    itemObj.put("unit_price", p.getPrice());
                    payload.put(itemObj);
                }
            }

            if (payload.length() == 0) {
                // Add this log so you can see if the Cart IDs are broken
                Log.e(TAG, "SKIPPED: No valid Item IDs found in the cart! Are you using Dummy Data?");
                return true;
            } // Nothing valid to insert

            URL url = new URL(getBaseUrl() + "/rest/v1/order_items");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int code = conn.getResponseCode();
            if (code >= 300) {
                Log.e(TAG, "Failed order items: " + readStream(conn.getErrorStream()));
                return false;
            }
            conn.disconnect();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "addOrderItems error", e);
        }
        return false;
    }
    public List<Order> getBuyerOrders(String token, String buyerId) {
        List<Order> orders = new ArrayList<>();
        try {
            URL url = new URL(getBaseUrl() + "/rest/v1/orders?buyer_id=eq." + buyerId + "&select=*&order=created_at.desc");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + token);

            int code = conn.getResponseCode();
            if (code == 200) {
                String responseBody = readStream(conn.getInputStream());
                Gson gson = new Gson();
                Type listType = new TypeToken<List<Order>>(){}.getType();
                orders = gson.fromJson(responseBody, listType);
            }
            conn.disconnect();
        } catch (Exception e) {
            Log.e(TAG, "getBuyerOrders error", e);
        }
        return orders;
    }

    // --- OTHER METHODS ---
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
            URL url = new URL(getBaseUrl() + "/rest/v1/seller_profiles?select=id,store_name,description,address,seller_avatar_url,seller_profile_bg,profile(full_name)");
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

                    // ✅ Store name
                    String shopName = "Unnamed Shop";
                    if (obj.has("store_name") && !obj.isNull("store_name") && !obj.getString("store_name").isEmpty()) {
                        shopName = obj.optString("store_name", shopName);
                    } else {
                        String profileName = extractProfileFullName(obj.opt("profile"), "");
                        if (!profileName.isEmpty()) {
                            shopName = profileName;
                        }
                    }

                    // ✅ Seller username from profile join
                    String sellerUsername = extractProfileFullName(obj.opt("profile"), "");

                    // ✅ Avatar from seller_profiles.seller_avatar_url
                    String avatarUrl = obj.optString("seller_avatar_url", "");

                    Product shop = new Product(R.drawable.product_1, shopName, obj.optString("description", "A great place to eat!"), 0.0f, "Restaurant", true, 4.8f, "All");
                    shop.setSellerId(obj.getString("id"));
                    shop.setImageUrl(avatarUrl);
                    shop.setShopName(sellerUsername);
                    shop.setShopBackground(obj.optString("seller_profile_bg", ""));// seller's real name as subtitle
                    shops.add(shop);
                    shop.setDescription(obj.optString("description", ""));
                    shop.setAddress(obj.optString("address", "No address available"));
                }
            }
        } catch (Exception e) { Log.e(TAG, "getAllShops error", e); }
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
                    String shopName = extractShopNameFromSellerProfiles(
                            obj.opt("seller_profiles"),
                            "Unknown Shop"
                    );

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

                    // THIS IS THE CRITICAL LINE THAT WAS MISSING!
                    // This pulls the UUID so the cart knows it's real data.
                    product.setId(obj.getString("id"));

                    product.setImageUrl(obj.optString("image_url", ""));
                    product.setShopName(shopName);
                    items.add(product);
                }
            }
        } catch (Exception e) { Log.e(TAG, "getRandomMenuItems error", e); }
        return items;
    }

    public List<Product> getTopPickMenuItems(String token) {
        List<Product> items = new ArrayList<>();
        if (token == null) return items;
        try {
            URL url = new URL(
                    getBaseUrl()
                            + "/rest/v1/menu_items"
                            + "?select=*,seller_profiles(store_name,profile(full_name))"
                            + "&is_available=eq.true"
                            + "&order=created_at.asc"
            );
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + token);
            int code = conn.getResponseCode();
            String body = readStream(code < 300 ? conn.getInputStream() : conn.getErrorStream());
            conn.disconnect();

            if (code >= 200 && code < 300) {
                JSONArray arr = new JSONArray(body);
                java.util.HashSet<String> seenSellerIds = new java.util.HashSet<>();

                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    String sellerId = obj.optString("seller_id", "");
                    if (sellerId.isEmpty() || seenSellerIds.contains(sellerId)) continue;

                    String shopName = extractShopNameFromSellerProfiles(
                            obj.opt("seller_profiles"),
                            "Unknown Shop"
                    );

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
                    product.setSellerId(sellerId);
                    product.setId(obj.optString("id", ""));
                    product.setImageUrl(obj.optString("image_url", ""));
                    product.setShopName(shopName);
                    items.add(product);
                    seenSellerIds.add(sellerId);

                    if (items.size() >= 20) break;
                }
            }
        } catch (Exception e) { Log.e(TAG, "getTopPickMenuItems error", e); }
        return items;
    }

    public List<SellerProfile> searchShopsByMenuItem(String token, String query) {
        List<SellerProfile> shops = new ArrayList<>();

        if (token == null || query == null) return shops;

        try {
            String encodedQuery = URLEncoder.encode(query, "UTF-8");

            URL url = new URL(
                    getBaseUrl()
                    + "/rest/v1/menu_items"
                    + "?select=seller_profiles(*)"
                    + "&name=ilike.*" + encodedQuery + "*"
                    + "&is_available=eq.true"
            );

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");
            conn.setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + token);

            int code = conn.getResponseCode();

            String body = readStream(code < 300 ? conn.getInputStream() : conn.getErrorStream());

            if (code >= 200 && code < 300) {
                JSONArray arr = new JSONArray(body);

                HashSet<String> addedShopIds = new HashSet<>();

                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);

                    if (!obj.has("seller_profiles") || obj.isNull("seller_profiles")) { continue; }

                    JSONObject shopObj = obj.getJSONObject("seller_profiles");

                    String shopId = shopObj.optString("id");

                    if (addedShopIds.contains(shopId)) { continue; }

                    addedShopIds.add(shopId);

                    SellerProfile shop = new SellerProfile(
                            shopObj.optString("id"),
                            shopObj.optString("profile_id"),
                            shopObj.optString("store_name"),
                            shopObj.optString("description"),
                            shopObj.optString("address"),
                            shopObj.optBoolean("is_open"),
                            shopObj.optString("seller_avatar_url"),
                            shopObj.optString("seller_profile_bg")
                    );

                    shops.add(shop);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "searchShopsByMenuItem Error", e);
        }

        return shops;
    }

    public List<SellerProfile> getRandomShops(String token) {
        List<SellerProfile> shops = new ArrayList<>();

        if (token == null) return shops;

        try {

            URL url = new URL(
                    getBaseUrl()
                            + "/rest/v1/seller_profiles"
                            + "?select=*"
                            + "&is_open=eq.true"
                            + "&order=created_at.desc"
            );

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

                for (int i = 0; i < arr.length(); i++) {
                    jsonList.add(arr.getJSONObject(i));
                }

                java.util.Collections.shuffle(jsonList);

                for (JSONObject obj : jsonList) {
                    SellerProfile shop =
                            new SellerProfile(
                                    obj.optString("id"),
                                    obj.optString("profile_id"),
                                    obj.optString("store_name"),
                                    obj.optString("description"),
                                    obj.optString("address"),
                                    obj.optBoolean("is_open"),
                                    obj.optString("seller_avatar_url"),
                                    obj.optString("seller_profile_bg")
                            );
                    shops.add(shop);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "getRandomShops error", e);
        }

        return shops;
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
        } catch (Exception e) {
            Log.e(TAG, "getMenuItems error for sellerId=" + sellerId, e);
        }
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
                    if (status.equalsIgnoreCase("done")) {
                        stats.totalSales += amount;
                        stats.completedOrders++;
                    } else if (status.equalsIgnoreCase("pending")) {
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

    public String getAddress(String token, String profileId) {
        try {
            URL url = new URL(getBaseUrl()
                    + "/rest/v1/seller_profiles?profile_id=eq." + profileId
                    + "&select=address");
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
                    return arr.getJSONObject(0).optString("address", "");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "getAddress error", e);
        }
        return "";
    }

    public boolean saveAddress(String token, String profileId, String address) {
        if (token == null || profileId == null) return false;
        try {
            URL checkUrl = new URL(getBaseUrl()
                    + "/rest/v1/seller_profiles?profile_id=eq." + profileId
                    + "&select=id");
            HttpURLConnection checkConn = (HttpURLConnection) checkUrl.openConnection();
            checkConn.setRequestMethod("GET");
            checkConn.setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY);
            checkConn.setRequestProperty("Authorization", "Bearer " + token);
            int checkCode = checkConn.getResponseCode();
            String checkBody = readStream(
                    checkCode < 300 ? checkConn.getInputStream() : checkConn.getErrorStream());
            checkConn.disconnect();

            JSONArray arr = new JSONArray(checkBody);

            JSONObject payload = new JSONObject()
                    .put("profile_id", profileId)
                    .put("address", address != null ? address : "");

            if (arr.length() > 0) {
                String existingId = arr.getJSONObject(0).getString("id");
                return patch("/rest/v1/seller_profiles?id=eq." + existingId,
                        payload.toString(), token);
            } else {
                payload.put("is_open", true);
                HttpResponse res = post("/rest/v1/seller_profiles",
                        payload.toString(), token);
                return res.statusCode >= 200 && res.statusCode < 300;
            }
        } catch (Exception e) {
            Log.e(TAG, "saveAddress error", e);
            return false;
        }
    }

    public String getShopBackground(String token, String profileId) {
        try {
            URL url = new URL(getBaseUrl() + "/rest/v1/seller_profiles?profile_id=eq." + profileId + "&select=seller_profile_bg");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + token);
            int code = conn.getResponseCode();
            String body = readStream(code < 300 ? conn.getInputStream() : conn.getErrorStream());
            conn.disconnect();
            if (code >= 200 && code < 300) {
                JSONArray arr = new JSONArray(body);
                if (arr.length() > 0) return arr.getJSONObject(0).optString("seller_profile_bg", "");
            }
        } catch (Exception e) { Log.e(TAG, "getShopBackground error", e); }
        return "";
    }

    public boolean saveShopBackground(String token, String profileId, String backgroundUrl) {
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
                    .put("seller_profile_bg", backgroundUrl != null ? backgroundUrl : "");

            if (arr.length() > 0) {
                String existingId = arr.getJSONObject(0).getString("id");
                return patch("/rest/v1/seller_profiles?id=eq." + existingId, payload.toString(), token);
            } else {
                payload.put("is_open", true);
                HttpResponse res = post("/rest/v1/seller_profiles", payload.toString(), token);
                return res.statusCode >= 200 && res.statusCode < 300;
            }
        } catch (Exception e) { Log.e(TAG, "saveShopBackground error", e); }
        return false;
    }

    public List<SellerProfile> getFeaturedShops(String token) {
        List<SellerProfile> shops = new ArrayList<>();

        if (token == null) return shops;

        try {
            URL url = new URL(
                    getBaseUrl()
                    + "/rest/v1/seller_profiles"
                    + "?select=*"
            );

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

                    SellerProfile seller = new SellerProfile(
                            obj.optString("id", ""),
                            obj.optString("profile_id", ""),
                            obj.optString("store_name", ""),
                            obj.optString("description", ""),
                            obj.optString("address", ""),
                            obj.optBoolean("is_open", true),
                            obj.optString("seller_avatar_url", ""),
                            obj.optString("seller_profile_bg", "")
                    );

                    shops.add(seller);
                }
            }

        } catch (Exception e) { Log.e(TAG, "getFeaturedShops", e); }

        return shops;
    }

    public List<SellerProfile> getNearbyShopss(String token) {
        List<SellerProfile> shops = new ArrayList<>();

        if (token == null) return shops;

        try {
            URL url = new URL(
                    getBaseUrl()
                            + "/rest/v1/seller_profiles"
                            + "?select=*"
            );

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

                    SellerProfile seller = new SellerProfile(
                            obj.optString("id", ""),
                            obj.optString("profile_id", ""),
                            obj.optString("store_name", ""),
                            obj.optString("description", ""),
                            obj.optString("address", ""),
                            obj.optBoolean("is_open", true),
                            obj.optString("seller_avatar_url", ""),
                            obj.optString("seller_profile_bg", "")
                    );

                    shops.add(seller);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "getFeaturedShops", e);
        }

        return shops;
    }

    public boolean saveSellerAvatarUrl(String token, String profileId, String avatarUrl) {
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
                    .put("seller_avatar_url", avatarUrl != null ? avatarUrl : "");

            if (arr.length() > 0) {
                String existingId = arr.getJSONObject(0).getString("id");
                return patch("/rest/v1/seller_profiles?id=eq." + existingId, payload.toString(), token);
            } else {
                payload.put("is_open", true);
                HttpResponse res = post("/rest/v1/seller_profiles", payload.toString(), token);
                return res.statusCode >= 200 && res.statusCode < 300;
            }
        } catch (Exception e) { Log.e(TAG, "saveSellerAvatarUrl error", e); }
        return false;
    }

    public String getSellerAvatarUrl(String token, String profileId) {
        try {
            URL url = new URL(getBaseUrl() + "/rest/v1/seller_profiles?profile_id=eq." + profileId + "&select=seller_avatar_url");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + token);
            int code = conn.getResponseCode();
            String body = readStream(code < 300 ? conn.getInputStream() : conn.getErrorStream());
            conn.disconnect();
            if (code >= 200 && code < 300) {
                JSONArray arr = new JSONArray(body);
                if (arr.length() > 0) return arr.getJSONObject(0).optString("seller_avatar_url", "");
            }
        } catch (Exception e) { Log.e(TAG, "getSellerAvatarUrl error", e); }
        return "";
    }

    public String getShopBackgroundBySellerId(String token, String sellerId) {
        try {
            URL url = new URL(getBaseUrl() + "/rest/v1/seller_profiles?id=eq." + sellerId + "&select=seller_profile_bg");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + token);
            int code = conn.getResponseCode();
            String body = readStream(code < 300 ? conn.getInputStream() : conn.getErrorStream());
            conn.disconnect();
            if (code >= 200 && code < 300) {
                JSONArray arr = new JSONArray(body);
                if (arr.length() > 0) return arr.getJSONObject(0).optString("seller_profile_bg", "");
            }
        } catch (Exception e) { Log.e(TAG, "getShopBackgroundBySellerId error", e); }
        return "";
    }

    public String getSellerAvatarUrlBySellerId(String token, String sellerId) {
        try {
            URL url = new URL(getBaseUrl() + "/rest/v1/seller_profiles?id=eq." + sellerId + "&select=seller_avatar_url");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + token);
            int code = conn.getResponseCode();
            String body = readStream(code < 300 ? conn.getInputStream() : conn.getErrorStream());
            conn.disconnect();
            if (code >= 200 && code < 300) {
                JSONArray arr = new JSONArray(body);
                if (arr.length() > 0) return arr.getJSONObject(0).optString("seller_avatar_url", "");
            }
        } catch (Exception e) { Log.e(TAG, "getSellerAvatarUrlBySellerId error", e); }
        return "";
    }

    private String extractProfileFullName(Object profileField, String fallback) {
        if (profileField instanceof JSONObject) {
            return ((JSONObject) profileField).optString("full_name", fallback);
        }

        if (profileField instanceof JSONArray) {
            JSONArray profileArr = (JSONArray) profileField;
            if (profileArr.length() > 0) {
                JSONObject first = profileArr.optJSONObject(0);
                if (first != null) return first.optString("full_name", fallback);
            }
        }

        return fallback;
    }

    private String extractShopNameFromSellerProfiles(Object sellerProfilesField, String fallback) {
        JSONObject sellerProfileObj = null;

        if (sellerProfilesField instanceof JSONObject) {
            sellerProfileObj = (JSONObject) sellerProfilesField;
        } else if (sellerProfilesField instanceof JSONArray) {
            JSONArray arr = (JSONArray) sellerProfilesField;
            if (arr.length() > 0) sellerProfileObj = arr.optJSONObject(0);
        }

        if (sellerProfileObj == null) return fallback;

        String storeName = sellerProfileObj.optString("store_name", "");
        if (!storeName.isEmpty()) return storeName;

        return extractProfileFullName(sellerProfileObj.opt("profile"), fallback);
    }

    public AuthResult sendPasswordResetEmail(String email) {
        try {
            JSONObject payload = new JSONObject().put("email", email);
            URL url = new URL(getBaseUrl() + "/auth/v1/recover?redirect_to=midtermsapp://reset");

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY);
            conn.setRequestProperty("Content-Type", "application/json");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                conn.disconnect();
                return new AuthResult(true, "Check your email.", null, null);
            } else {
                // If it fails, read the EXACT error from Supabase!
                String errorBody = readStream(conn.getErrorStream());
                conn.disconnect();
                return new AuthResult(false, extractErrorMessage(errorBody), null, null);
            }
        } catch (Exception e) {
            Log.e(TAG, "sendPasswordResetEmail error", e);
            return new AuthResult(false, e.getMessage(), null, null);
        }
    }

    public String getDescription(String token, String profileId) {
        try {
            URL url = new URL(getBaseUrl() + "/rest/v1/seller_profiles?profile_id=eq." + profileId + "&select=description");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + token);
            int code = conn.getResponseCode();
            String body = readStream(code < 300 ? conn.getInputStream() : conn.getErrorStream());
            conn.disconnect();
            if (code >= 200 && code < 300) {
                JSONArray arr = new JSONArray(body);
                if (arr.length() > 0) return arr.getJSONObject(0).optString("description", "");
            }
        } catch (Exception e) { Log.e(TAG, "getDescription error", e); }
        return "";
    }

    public boolean saveDescription(String token, String profileId, String description) {
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
                    .put("description", description != null ? description : "");

            if (arr.length() > 0) {
                String existingId = arr.getJSONObject(0).getString("id");
                return patch("/rest/v1/seller_profiles?id=eq." + existingId, payload.toString(), token);
            } else {
                payload.put("is_open", true);
                HttpResponse res = post("/rest/v1/seller_profiles", payload.toString(), token);
                return res.statusCode >= 200 && res.statusCode < 300;
            }
        } catch (Exception e) { Log.e(TAG, "saveDescription error", e); }
        return false;
    }

    public boolean updateOrderStatus(String token, String orderId, String newStatus) {
        try {
            JSONObject payload = new JSONObject().put("status", newStatus);
            return patch("/rest/v1/orders?id=eq." + orderId, payload.toString(), token);
        } catch (Exception e) {
            Log.e(TAG, "updateOrderStatus error", e);
            return false;
        }
    }

    // ── Only pending orders for CurrentOrders ─────────────────────────────────
    public List<OrderDetail> getPendingSellerOrders(String token, String sellerId) {
        return fetchSellerOrdersByStatus(token, sellerId, "pending");
    }

    // ── Only done orders for Transactions ────────────────────────────────────
    public List<OrderDetail> getDoneSellerOrders(String token, String sellerId) {
        return fetchSellerOrdersByStatus(token, sellerId, "done");
    }

    private List<OrderDetail> fetchSellerOrdersByStatus(String token, String sellerId, String status) {
        List<OrderDetail> result = new ArrayList<>();
        if (token == null || sellerId == null) return result;
        try {
            String query = "/rest/v1/orders"
                    + "?seller_id=eq." + sellerId
                    + "&status=eq." + status
                    + "&select="
                    + "id,"
                    + "status,"
                    + "total_amount,"
                    + "created_at,"
                    + "profile!orders_buyer_id_fkey(full_name),"
                    + "buyer_address!orders_buyer_address_id_fkey(street,barangay,city,postal_code,country),"
                    + "order_items(quantity,unit_price,menu_items(name))"
                    + "&order=created_at.desc";

            URL url = new URL(getBaseUrl() + query);
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
                    JSONObject o = arr.getJSONObject(i);
                    OrderDetail od = new OrderDetail();

                    od.orderId     = o.optString("id");
                    od.status      = o.optString("status", "pending");
                    od.totalAmount = o.optDouble("total_amount", 0);
                    od.createdAt   = o.optString("created_at", "");

                    if (!o.isNull("profile")) {
                        od.buyerFullName = o.getJSONObject("profile")
                                .optString("full_name", "Unknown Buyer");
                    } else {
                        od.buyerFullName = "Unknown Buyer";
                    }

                    if (!o.isNull("buyer_address")) {
                        JSONObject addr = o.getJSONObject("buyer_address");
                        od.street     = addr.optString("street", "");
                        od.barangay   = addr.optString("barangay", "");
                        od.city       = addr.optString("city", "");
                        od.postalCode = String.valueOf(addr.optInt("postal_code", 0));
                        od.country    = addr.optString("country", "");
                    }

                    if (!o.isNull("order_items")) {
                        JSONArray itemsArr = o.getJSONArray("order_items");
                        for (int j = 0; j < itemsArr.length(); j++) {
                            JSONObject oi = itemsArr.getJSONObject(j);
                            OrderItemDetail item = new OrderItemDetail();
                            item.quantity  = oi.optInt("quantity", 1);
                            item.unitPrice = oi.optDouble("unit_price", 0);
                            if (!oi.isNull("menu_items")) {
                                item.menuItemName = oi.getJSONObject("menu_items")
                                        .optString("name", "Item");
                            } else {
                                item.menuItemName = "Item";
                            }
                            od.items.add(item);
                        }
                    }
                    result.add(od);
                }
            } else {
                Log.e(TAG, "fetchSellerOrdersByStatus [" + code + "]: " + body);
            }
        } catch (Exception e) {
            Log.e(TAG, "fetchSellerOrdersByStatus error", e);
        }
        return result;
    }
}
