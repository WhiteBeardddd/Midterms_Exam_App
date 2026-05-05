package com.example.midtermsexam_beauty.views.seller;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.midtermsexam_beauty.R;
import com.example.midtermsexam_beauty.adapters.SellerNavCard;
import com.example.midtermsexam_beauty.models.Profile;
import com.example.midtermsexam_beauty.utilities.AppNavigator;
import com.example.midtermsexam_beauty.utilities.SessionManager;
import com.example.midtermsexam_beauty.utilities.SupabaseAuthService;

import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SellerDashboard extends AppCompatActivity {

    private static final String TAG = "SellerDashboard";

    private SessionManager sessionManager;
    private SupabaseAuthService authService;
    private ExecutorService executor;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private TextView tvTotalSales, tvCompletedOrders, tvPendingOrders, tvTotalItems;
    private ImageView ivShopBackground, ivSellerAvatar;
    private TextView tvStoreName, tvSellerName;

    private String token, authId, profileId;
    private boolean isUpdatingAvatar = false; // Tracks which image is being uploaded

    // Modern ActivityResultLauncher for picking images
    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        uploadSelectedImage(selectedImageUri, isUpdatingAvatar);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_dashboard);

        sessionManager = new SessionManager(this);
        authService = new SupabaseAuthService();
        executor = Executors.newSingleThreadExecutor();

        // Initialize user session variables
        token = sessionManager.getToken();
        authId = sessionManager.getUserId();
        profileId = sessionManager.getProfileId();

        ivShopBackground = findViewById(R.id.ivShopBackground);
        ivSellerAvatar   = findViewById(R.id.ivSellerAvatar);
        tvStoreName      = findViewById(R.id.tvStoreName);
        tvSellerName     = findViewById(R.id.tvSellerName);

        findViewById(R.id.btnEditAvatar).setOnClickListener(v -> {
            isUpdatingAvatar = true;
            openImagePicker();
        });

        findViewById(R.id.btnEditBackground).setOnClickListener(v -> {
            isUpdatingAvatar = false;
            openImagePicker();
        });

        SellerNavCard.setupNavbar(this);

        tvTotalSales = findViewById(R.id.tvTotalSales);
        tvCompletedOrders = findViewById(R.id.tvCompletedOrders);
        tvPendingOrders = findViewById(R.id.tvPendingOrders);
        tvTotalItems = findViewById(R.id.tvTotalItems);

        Button btnSellerLogout = findViewById(R.id.btnSellerLogout);
        btnSellerLogout.setOnClickListener(v -> AppNavigator.logout(this, sessionManager));

        loadStatistics();
    }

    private void loadStatistics() {
        if (token == null || authId == null) return;

        executor.execute(() -> {
            Profile profile  = authService.getProfile(token, authId);
            String storeName = authService.getStoreName(token, profileId);
            String bgUrl     = authService.getShopBackground(token, profileId);

            // 1. Resolve Seller ID from Profile
            String sellerId = authService.getSellerIdByAuthId(token, authId);

            if (sellerId != null) {
                // 2. Fetch Stats
                SupabaseAuthService.SellerStats stats = authService.getStats(token, sellerId);

                handler.post(() -> {
                    tvTotalSales.setText(String.format("₱%.2f", stats.totalSales));
                    tvCompletedOrders.setText(String.valueOf(stats.completedOrders));
                    tvPendingOrders.setText(String.valueOf(stats.pendingOrders));
                    tvTotalItems.setText(String.valueOf(stats.totalItems));

                    // Optional: load the fetched background/avatar here if not loaded elsewhere
                    if (bgUrl != null && !bgUrl.isEmpty()) {
                        Glide.with(this).load(bgUrl).into(ivShopBackground);
                    }
                });
            }
        });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void uploadSelectedImage(Uri imageUri, boolean isAvatar) {
        executor.execute(() -> {
            try {
                Log.d(TAG, "uploadSelectedImage uri: " + imageUri);
                InputStream is = getContentResolver().openInputStream(imageUri);
                if (is == null) {
                    Log.e(TAG, "InputStream is null");
                    return;
                }

                // Using the safe byte reader from your SellerMenu
                byte[] bytes = readStreamBytes(is);
                is.close();

                String bucket = isAvatar ? "seller-avatars" : "seller-backgrounds";

                String mimeType = getContentResolver().getType(imageUri);
                if (mimeType == null) mimeType = "image/jpeg";
                String ext  = mimeType.contains("png") ? "png" : "jpg";

                // Unique path generation using UUID
                String path = profileId + "/" + UUID.randomUUID() + "." + ext;

                String uploadedUrl = authService.uploadImage(token, bucket, path, bytes, mimeType);

                if (uploadedUrl == null) {
                    handler.post(() -> Toast.makeText(this, "Failed to upload image.", Toast.LENGTH_SHORT).show());
                    return;
                }

                boolean saved;
                if (isAvatar) {
                    // Reuse existing updateProfile — just update avatar_url
                    saved = authService.updateProfile(token, authId, null, null, true, uploadedUrl);
                } else {
                    saved = authService.saveShopBackground(token, profileId, uploadedUrl);
                }

                if (saved) {
                    handler.post(() -> {
                        Glide.with(this).load(uploadedUrl).into(isAvatar ? ivSellerAvatar : ivShopBackground);
                        Toast.makeText(this, isAvatar ? "Avatar updated!" : "Background updated!", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    handler.post(() -> Toast.makeText(this, "Failed to save to database.", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                Log.e(TAG, "Upload failed", e);
                handler.post(() -> Toast.makeText(this, "An error occurred during upload.", Toast.LENGTH_SHORT).show());
            }
        });
    }

    // Helper method to safely read bytes on all API levels
    private byte[] readStreamBytes(InputStream is) throws Exception {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        int nRead;
        byte[] chunk = new byte[4096];
        while ((nRead = is.read(chunk, 0, chunk.length)) != -1) {
            buffer.write(chunk, 0, nRead);
        }
        return buffer.toByteArray();
    }

    @Override
    protected void onDestroy() {
        if (executor != null) executor.shutdown();
        super.onDestroy();
    }
}