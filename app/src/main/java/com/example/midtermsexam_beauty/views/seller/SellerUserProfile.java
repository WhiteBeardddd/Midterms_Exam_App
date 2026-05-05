package com.example.midtermsexam_beauty.views.seller;

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

import com.example.midtermsexam_beauty.R;
import com.example.midtermsexam_beauty.adapters.SellerNavCard;
import com.example.midtermsexam_beauty.models.Profile;
import com.example.midtermsexam_beauty.utilities.AppNavigator;
import com.example.midtermsexam_beauty.utilities.SessionManager;
import com.example.midtermsexam_beauty.utilities.SupabaseAuthService;
import com.example.midtermsexam_beauty.views.user.Homepage;
import com.example.midtermsexam_beauty.views.user.UserProfile;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SellerUserProfile extends AppCompatActivity {

    private EditText etFullName, etPhone;
    private SwitchMaterial switchIsSeller;
    private ImageButton settingBtn, favBtn, addressBtn;
    private Button btnSave, btnLogout;
    private SessionManager session;
    private SupabaseAuthService authService;
    private ExecutorService executor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_user_profile);

        session = new SessionManager(this);
        authService = new SupabaseAuthService();
        executor = Executors.newSingleThreadExecutor();

        // Ensure we use the Seller Navbar logic
        SellerNavCard.setupNavbar(this);

        etFullName = findViewById(R.id.etFullName);
        etPhone = findViewById(R.id.etPhone);
        switchIsSeller = findViewById(R.id.switchIsSeller);
        settingBtn = findViewById(R.id.settings_btn);
        favBtn = findViewById(R.id.fav_btn);
        addressBtn = findViewById(R.id.address_btn);
        btnSave = findViewById(R.id.btnSaveProfile);
        btnLogout = findViewById(R.id.btnLogout);

        loadProfile();

        settingBtn.setOnClickListener(v ->
                Toast.makeText(this, "Going to settings", Toast.LENGTH_SHORT).show()
        );

        favBtn.setOnClickListener(v ->
                Toast.makeText(this, "Fav Product Lists", Toast.LENGTH_SHORT).show()
        );

        addressBtn.setOnClickListener(v ->
                Toast.makeText(this, "Set the Location", Toast.LENGTH_SHORT).show()
        );

        btnSave.setOnClickListener(v -> saveProfile());
        btnLogout.setOnClickListener(v -> AppNavigator.logout(this, session));

        setupDropdown(R.id.headerSupport, R.id.contentSupport, R.id.arrowSupport);
        setupDropdown(R.id.headerTerms, R.id.contentTerms, R.id.arrowTerms);
    }

    private void loadProfile() {
        executor.execute(() -> {
            Profile profile = authService.getProfile(
                    session.getToken(),
                    session.getUserId()
            );
            if (profile != null) {
                runOnUiThread(() -> {
                    etFullName.setText(profile.getFullName());
                    etPhone.setText(profile.getPhone());
                    switchIsSeller.setChecked(profile.isSeller());
                });
            }
        });
    }

    private void saveProfile() {
        String fullName = etFullName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        boolean isSeller = switchIsSeller.isChecked();

        if (session.getToken() == null || session.getUserId() == null) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
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
            boolean success = authService.updateProfile(session.getToken(), session.getUserId(), fullName, phone, isSeller, "");

            runOnUiThread(() -> {
                btnSave.setEnabled(true);
                if (success) {
                    session.setIsSeller(isSeller);
                    Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show();
                    
                    if (!isSeller) {
                        // Redirect to standard UserProfile if they disabled seller mode
                        startActivity(new Intent(this, UserProfile.class));
                    } else {
                        // Stay in seller dashboard
                        startActivity(new Intent(this, SellerDashboard.class));
                    }
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
