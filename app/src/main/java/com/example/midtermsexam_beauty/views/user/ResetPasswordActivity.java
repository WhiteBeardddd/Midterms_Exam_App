package com.example.midtermsexam_beauty.views.user;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.midtermsexam_beauty.MainActivity;
import com.example.midtermsexam_beauty.R;
import com.example.midtermsexam_beauty.utilities.SupabaseAuthService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ResetPasswordActivity extends AppCompatActivity {

    private EditText etNewPassword, etConfirmPassword;
    private Button btnSavePassword;
    private String accessToken = null;

    private SupabaseAuthService authService;
    private ExecutorService executor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        authService = new SupabaseAuthService();
        executor = Executors.newSingleThreadExecutor();

        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnSavePassword = findViewById(R.id.btnSavePassword);

        // Supabase passes the token in the URL Fragment (after the # symbol)
        Uri uri = getIntent().getData();
        if (uri != null && uri.getFragment() != null) {
            String fragment = uri.getFragment();
            String[] params = fragment.split("&");
            for (String param : params) {
                String[] keyValue = param.split("=");
                if (keyValue.length == 2 && keyValue[0].equals("access_token")) {
                    accessToken = keyValue[1];
                    break;
                }
            }
        }

        if (accessToken == null) {
            Toast.makeText(this, "Invalid or expired reset link.", Toast.LENGTH_LONG).show();
            finish();
        }

        btnSavePassword.setOnClickListener(v -> saveNewPassword());
    }

    private void saveNewPassword() {
        String newPass = etNewPassword.getText().toString().trim();
        String confirmPass = etConfirmPassword.getText().toString().trim();

        if (newPass.isEmpty() || newPass.length() < 6) {
            etNewPassword.setError("Password must be at least 6 characters");
            return;
        }

        if (!newPass.equals(confirmPass)) {
            etConfirmPassword.setError("Passwords do not match");
            return;
        }

        btnSavePassword.setEnabled(false);
        btnSavePassword.setText("Saving...");

        executor.execute(() -> {
            boolean success = authService.updatePassword(accessToken, newPass);

            runOnUiThread(() -> {
                btnSavePassword.setEnabled(true);
                btnSavePassword.setText("Save New Password");

                if (success) {
                    Toast.makeText(this, "Password updated successfully!", Toast.LENGTH_LONG).show();
                    // Send them back to the login screen
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(this, "Failed to update password. Link may have expired.", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    @Override
    protected void onDestroy() {
        if (executor != null) executor.shutdown();
        super.onDestroy();
    }
}