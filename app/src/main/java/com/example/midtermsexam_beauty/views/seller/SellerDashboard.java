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
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
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
    private boolean isUpdatingAvatar = false;

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
        hideSystemUI();
        sessionManager = new SessionManager(this);
        authService = new SupabaseAuthService();
        executor = Executors.newSingleThreadExecutor();

        token     = sessionManager.getToken();
        authId    = sessionManager.getUserId();
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

        tvTotalSales      = findViewById(R.id.tvTotalSales);
        tvCompletedOrders = findViewById(R.id.tvCompletedOrders);
        tvPendingOrders   = findViewById(R.id.tvPendingOrders);
        tvTotalItems      = findViewById(R.id.tvTotalItems);

        Button btnSellerLogout = findViewById(R.id.btnSellerLogout);
        btnSellerLogout.setOnClickListener(v -> AppNavigator.logout(this, sessionManager));

        loadStatistics();
    }

    private void hideSystemUI() {
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
    }
    private void loadStatistics() {
        if (token == null || authId == null) return;

        executor.execute(() -> {
            Profile profile  = authService.getProfile(token, authId);
            String storeName = authService.getStoreName(token, profileId);
            String bgUrl     = authService.getShopBackground(token, profileId);
            String avatarUrl = authService.getSellerAvatarUrl(token, profileId);
            String sellerId  = authService.getSellerIdByAuthId(token, authId);

            if (sellerId != null) {
                SupabaseAuthService.SellerStats stats = authService.getStats(token, sellerId);

                handler.post(() -> {
                    tvTotalSales.setText(String.format("₱%.2f", stats.totalSales));
                    tvCompletedOrders.setText(String.valueOf(stats.completedOrders));
                    tvPendingOrders.setText(String.valueOf(stats.pendingOrders));
                    tvTotalItems.setText(String.valueOf(stats.totalItems));

                    tvStoreName.setText(storeName != null && !storeName.isEmpty() ? storeName : "My Store");
                    tvSellerName.setText(profile != null ? profile.getFullName() : "Seller");

                    if (bgUrl != null && !bgUrl.isEmpty()) {
                        Glide.with(this)
                                .load(bgUrl)
                                .skipMemoryCache(true)
                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                .into(ivShopBackground);
                    }

                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        Glide.with(this)
                                .load(avatarUrl)
                                .skipMemoryCache(true)
                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                .into(ivSellerAvatar);
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

    // Same pattern as SellerMenu's extractPathFromUrl()
    private String extractPathFromUrl(String imageUrl, boolean isAvatar) {
        try {
            String marker = isAvatar
                    ? "/object/public/seller-avatars/"
                    : "/object/public/seller-backgrounds/";
            int idx = imageUrl.indexOf(marker);
            if (idx != -1) return imageUrl.substring(idx + marker.length());
        } catch (Exception e) { Log.e(TAG, "extractPathFromUrl error", e); }
        return null;
    }

    private void uploadSelectedImage(Uri imageUri, boolean isAvatar) {
        executor.execute(() -> {
            try {
                // Read bytes — same as SellerMenu's uploadImage()
                InputStream is = getContentResolver().openInputStream(imageUri);
                if (is == null) {
                    Log.e(TAG, "InputStream is null");
                    return;
                }
                byte[] bytes = readStreamBytes(is);
                is.close();

                String bucket   = isAvatar ? "seller-avatars" : "seller-backgrounds";
                String mimeType = getContentResolver().getType(imageUri);
                if (mimeType == null) mimeType = "image/jpeg";
                String ext  = mimeType.contains("png") ? "png" : "jpg";
                String path = authId + "/" + UUID.randomUUID() + "." + ext;

                // Delete old — same pattern as SellerMenu's saveMenuItem()
                String oldUrl = isAvatar
                        ? authService.getSellerAvatarUrl(token, profileId)
                        : authService.getShopBackground(token, profileId);
                Log.d(TAG, "Old URL: " + oldUrl);

                if (oldUrl != null && !oldUrl.isEmpty()) {
                    String oldPath = extractPathFromUrl(oldUrl, isAvatar);
                    Log.d(TAG, "Deleting old path: " + oldPath);
                    if (oldPath != null) authService.deleteImage(token, bucket, oldPath);
                }

                // Upload new
                String uploadedUrl = authService.uploadImage(token, bucket, path, bytes, mimeType);
                Log.d(TAG, "Uploaded new URL: " + uploadedUrl);

                if (uploadedUrl == null) {
                    handler.post(() -> Toast.makeText(this, "Failed to upload image.", Toast.LENGTH_SHORT).show());
                    return;
                }

                // Save to DB
                boolean saved = isAvatar
                        ? authService.saveSellerAvatarUrl(token, profileId, uploadedUrl)
                        : authService.saveShopBackground(token, profileId, uploadedUrl);

                if (saved) {
                    handler.post(() -> {
                        Glide.with(this)
                                .load(uploadedUrl)
                                .skipMemoryCache(true)
                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                .into(isAvatar ? ivSellerAvatar : ivShopBackground);
                        Toast.makeText(this,
                                isAvatar ? "Avatar updated!" : "Background updated!",
                                Toast.LENGTH_SHORT).show();
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