package com.example.midtermsexam_beauty.views.user;

import android.content.Intent;
import android.os.Bundle;
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
import com.example.midtermsexam_beauty.adapters.NavbarCard;
import com.example.midtermsexam_beauty.adapters.SellerNavCard;
import com.example.midtermsexam_beauty.models.Profile;
import com.example.midtermsexam_beauty.utilities.AppNavigator;
import com.example.midtermsexam_beauty.utilities.SessionManager;
import com.example.midtermsexam_beauty.utilities.SupabaseAuthService;
import com.example.midtermsexam_beauty.views.seller.SellerDashboard;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.example.midtermsexam_beauty.utilities.BaseAuthenticatedActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserProfile extends BaseAuthenticatedActivity {

    private EditText etFullName, etPhone, etStoreName;
    private LinearLayout layoutStoreName;
    private SwitchMaterial switchIsSeller;
    private ImageButton settingBtn, orderBtn, addressBtn;
    private Button btnSave, btnLogout;

    private SessionManager session;
    private SupabaseAuthService authService;
    private ExecutorService executor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);
        hideSystemUI();
        session = new SessionManager(this);
        authService = new SupabaseAuthService();
        executor = Executors.newSingleThreadExecutor();

        if (session.isSeller()) {
            SellerNavCard.setupNavbar(this);
        } else {
            NavbarCard.setupNavbar(this);
        }

        etFullName = findViewById(R.id.etFullName);
        etPhone = findViewById(R.id.etPhone);
        switchIsSeller = findViewById(R.id.switchIsSeller);

        layoutStoreName = findViewById(R.id.layoutStoreName);
        etStoreName = findViewById(R.id.etStoreName);

        settingBtn = findViewById(R.id.settings_btn);
        orderBtn = findViewById(R.id.order_btn);
        addressBtn = findViewById(R.id.address_btn);
        btnSave = findViewById(R.id.btnSaveProfile);
        btnLogout = findViewById(R.id.btnLogout);

        loadProfile();

        settingBtn.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));

        // This takes the user to the new page!
        orderBtn.setOnClickListener(v -> {
            Intent intent = new Intent(UserProfile.this, MyOrdersActivity.class);
            startActivity(intent);
        });
        addressBtn.setOnClickListener(v -> startActivity(new Intent(this, AddressActivity.class)));

        btnSave.setOnClickListener(v -> saveProfile());
        btnLogout.setOnClickListener(v -> AppNavigator.logout(this, session));

        setupDropdown(R.id.headerSupport, R.id.contentSupport, R.id.arrowSupport);
        setupDropdown(R.id.headerTerms, R.id.contentTerms, R.id.arrowTerms);
    }

    private void loadProfile() {
        executor.execute(() -> {
            Profile profile = authService.getProfile(session.getToken(), session.getUserId());
            String storeName = "";

            if (profile != null && profile.getId() != null) {
                storeName = authService.getStoreName(session.getToken(), profile.getId());
            }

            String finalStoreName = storeName;
            if (profile != null) {
                runOnUiThread(() -> {
                    etFullName.setText(profile.getFullName());
                    etPhone.setText(profile.getPhone());
                    etStoreName.setText(finalStoreName);
                    switchIsSeller.setChecked(profile.isSeller());
                });
            }
        });
    }

    private void saveProfile() {
        String fullName = etFullName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        boolean isSeller = switchIsSeller.isChecked();
        String storeName = etStoreName.getText().toString().trim();

        if (session.getToken() == null || session.getUserId() == null) {
            AppNavigator.logout(this, session);
            return;
        }

        if (fullName.isEmpty()) {
            etFullName.setError("Name is required");
            etFullName.requestFocus();
            return;
        }


        btnSave.setEnabled(false);
        executor.execute(() -> {
            boolean profileSuccess = authService.updateProfile(session.getToken(), session.getUserId(), fullName, phone, isSeller, "");
            boolean storeSuccess = true;

            if (isSeller && profileSuccess) {
                storeSuccess = authService.saveStoreName(session.getToken(), session.getProfileId(), storeName);
            }

            boolean finalSuccess = profileSuccess && storeSuccess;

            runOnUiThread(() -> {
                btnSave.setEnabled(true);
                if (finalSuccess) {
                    session.setIsSeller(isSeller);
                    Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, isSeller ? SellerDashboard.class : Homepage.class));
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