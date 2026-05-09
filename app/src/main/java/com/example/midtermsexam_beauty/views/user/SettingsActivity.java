package com.example.midtermsexam_beauty.views.user;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.midtermsexam_beauty.R;
import com.example.midtermsexam_beauty.utilities.SessionManager;
import com.example.midtermsexam_beauty.utilities.SupabaseAuthService;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SettingsActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private SupabaseAuthService authService;
    private ExecutorService executor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        hideSystemUI();
        sessionManager = new SessionManager(this);
        authService = new SupabaseAuthService();
        executor = Executors.newSingleThreadExecutor();

        ImageButton btnBack = findViewById(R.id.back_btn);
        LinearLayout btnChangePassword = findViewById(R.id.btnChangePassword);
        LinearLayout btnDeleteAccount = findViewById(R.id.btnDeleteAccount);
        SwitchMaterial switchNotifications = findViewById(R.id.switchNotifications);

        // Back Button
        btnBack.setOnClickListener(v -> finish());

        // Change Password Click
        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        // Notifications Toggle
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String status = isChecked ? "enabled" : "disabled";
            Toast.makeText(this, "Notifications " + status, Toast.LENGTH_SHORT).show();
        });

        // Delete Account Click
        btnDeleteAccount.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Account")
                    .setMessage("Are you absolutely sure you want to delete your account? This action cannot be undone.")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        Toast.makeText(this, "Account deletion requested", Toast.LENGTH_LONG).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void showChangePasswordDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null);
        EditText etNewPassword = view.findViewById(R.id.etNewPassword);
        EditText etConfirmPassword = view.findViewById(R.id.etConfirmPassword);

        new AlertDialog.Builder(this)
                .setTitle("Change Password")
                .setView(view)
                .setPositiveButton("Update", (dialog, which) -> {
                    String newPass = etNewPassword.getText().toString();
                    String confirmPass = etConfirmPassword.getText().toString();

                    if (newPass.isEmpty() || newPass.length() < 6) {
                        Toast.makeText(this, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!newPass.equals(confirmPass)) {
                        Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    executeUpdatePassword(newPass);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void executeUpdatePassword(String newPassword) {
        String token = sessionManager.getToken();

        if (token == null) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show a brief loading toast
        Toast.makeText(this, "Updating password...", Toast.LENGTH_SHORT).show();

        // Run the network call on a background thread
        executor.execute(() -> {
            boolean success = authService.updatePassword(token, newPassword);

            // Return to the main thread to update the UI
            runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(this, "Password updated successfully!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Failed to update password. Please try again.", Toast.LENGTH_LONG).show();
                }
            });
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

    @Override
    protected void onDestroy() {
        if (executor != null) {
            executor.shutdown();
        }
        super.onDestroy();
    }
}