package com.example.midtermsexam_beauty.views.user;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.midtermsexam_beauty.R;
import com.example.midtermsexam_beauty.utilities.SupabaseAuthService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etEmail;
    private Button btnSendReset;
    private SupabaseAuthService authService;
    private ExecutorService executor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        authService = new SupabaseAuthService();
        executor = Executors.newSingleThreadExecutor();

        ImageButton btnBack = findViewById(R.id.back_btn);
        etEmail = findViewById(R.id.etEmail);
        btnSendReset = findViewById(R.id.btnSendReset);

        btnBack.setOnClickListener(v -> finish());
        btnSendReset.setOnClickListener(v -> sendResetLink());
    }

    private void sendResetLink() {
        String email = etEmail.getText().toString().trim();

        if (email.isEmpty()) {
            etEmail.setError("Please enter your email");
            etEmail.requestFocus();
            return;
        }

        btnSendReset.setEnabled(false);
        btnSendReset.setText("Sending...");

        executor.execute(() -> {
            boolean success = authService.sendPasswordResetEmail(email);

            runOnUiThread(() -> {
                btnSendReset.setEnabled(true);
                btnSendReset.setText("Send Reset Link");

                if (success) {
                    Toast.makeText(this, "Reset link sent! Check your inbox.", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(this, "Failed to send email. Check if the address is correct.", Toast.LENGTH_LONG).show();
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