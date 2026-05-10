package com.example.midtermsexam_beauty.views.seller;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.midtermsexam_beauty.R;
import com.example.midtermsexam_beauty.adapters.SellerNavCard;
import com.example.midtermsexam_beauty.models.Profile;
import com.example.midtermsexam_beauty.utilities.AppNavigator;
import com.example.midtermsexam_beauty.utilities.SessionManager;
import com.example.midtermsexam_beauty.utilities.SupabaseAuthService;
import com.example.midtermsexam_beauty.views.user.UserProfile;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SellerUserProfile extends AppCompatActivity {
    // Add with other EditText declarations at the top
    private EditText etFullName, etPhone, etStoreName, etAddress, etDescription;
    private SwitchMaterial switchIsSeller;

    private Button btnSave, btnLogout;
    private SessionManager session;
    private SupabaseAuthService authService;
    private ExecutorService executor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_user_profile);
        hideSystemUI();
        session = new SessionManager(this);
        authService = new SupabaseAuthService();
        executor = Executors.newSingleThreadExecutor();

        SellerNavCard.setupNavbar(this);

        etFullName = findViewById(R.id.etFullName);
        etPhone = findViewById(R.id.etPhone);
        etDescription = findViewById(R.id.etDescription);
        etStoreName = findViewById(R.id.etStoreName);
        etAddress = findViewById(R.id.etAddress);
        switchIsSeller = findViewById(R.id.switchIsSeller);
        btnSave = findViewById(R.id.btnSaveProfile);
        btnLogout = findViewById(R.id.btnLogout);

        loadProfile();

        btnSave.setOnClickListener(v -> saveProfile());
        btnLogout.setOnClickListener(v -> AppNavigator.logout(this, session));

        setupDropdown(R.id.headerSupport, R.id.contentSupport, R.id.arrowSupport);
        setupDropdown(R.id.headerTerms, R.id.contentTerms, R.id.arrowTerms);
    }

    private void loadProfile() {
        executor.execute(() -> {
            String token     = session.getToken();
            String userId    = session.getUserId();
            String profileId = session.getProfileId();

            Profile profile  = authService.getProfile(token, userId);

            String storeName    = "";
            String address      = "";
            String description  = "";

            if (profile != null && profile.getId() != null) {
                storeName   = authService.getStoreName(token, profile.getId());
                address     = authService.getAddress(token, profile.getId());
                description = authService.getDescription(token, profile.getId());
            }

            String finalStoreName   = storeName;
            String finalAddress     = address;
            String finalDescription = description;

            if (profile != null) {
                runOnUiThread(() -> {
                    etFullName.setText(profile.getFullName());
                    etPhone.setText(profile.getPhone());
                    etStoreName.setText(finalStoreName);
                    etAddress.setText(finalAddress);
                    etDescription.setText(finalDescription);
                    switchIsSeller.setChecked(profile.isSeller());
                });
            }
        });
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
    private void saveProfile() {
        String fullName    = etFullName.getText().toString().trim();
        String phone       = etPhone.getText().toString().trim();
        boolean isSeller   = switchIsSeller.isChecked();
        String storeName   = etStoreName.getText().toString().trim();
        String address     = etAddress.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        if (session.getToken() == null || session.getUserId() == null) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
            AppNavigator.logout(this, session);
            return;
        }

        btnSave.setEnabled(false);
        executor.execute(() -> {
            Profile existing = authService.getProfile(session.getToken(), session.getUserId());
            String existingAvatarUrl = existing != null ? existing.getAvatarUrl() : "";

            boolean profileSuccess = authService.updateProfile(
                    session.getToken(), session.getUserId(),
                    fullName, phone, isSeller, existingAvatarUrl
            );

            boolean storeSuccess       = true;
            boolean addressSuccess     = true;
            boolean descriptionSuccess = true;

            if (profileSuccess) {
                storeSuccess       = authService.saveStoreName(session.getToken(), session.getProfileId(), storeName);
                addressSuccess     = authService.saveAddress(session.getToken(), session.getProfileId(), address);
                descriptionSuccess = authService.saveDescription(session.getToken(), session.getProfileId(), description);
            }

            boolean finalSuccess = profileSuccess && storeSuccess && addressSuccess && descriptionSuccess;

            runOnUiThread(() -> {
                btnSave.setEnabled(true);
                if (finalSuccess) {
                    session.setIsSeller(isSeller);
                    Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, isSeller
                            ? SellerDashboard.class
                            : com.example.midtermsexam_beauty.views.user.UserProfile.class));
                    finish();
                } else {
                    Toast.makeText(this, "Failed to update profile.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    protected void onDestroy() {
        if (executor != null) executor.shutdown();
        super.onDestroy();
    }

    private void setupDropdown(int headerId, int contentId, int arrowId) {
        LinearLayout header = findViewById(headerId);
        LinearLayout content = findViewById(contentId);
        ImageView arrow = findViewById(arrowId);

        header.setOnClickListener(v -> {
            if (content.getVisibility() == View.GONE) {
                content.setVisibility(View.VISIBLE);
                content.setAlpha(0f);
                content.animate().alpha(1f).setDuration(200);
                arrow.animate().rotation(180f).setDuration(200);
            } else {
                content.animate().alpha(0f).setDuration(200)
                        .withEndAction(() -> content.setVisibility(View.GONE));
                arrow.animate().rotation(0f).setDuration(200);
            }
        });
    }
}