package com.example.midtermsexam_beauty.views.user;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.midtermsexam_beauty.R;
import com.example.midtermsexam_beauty.utilities.AppNavigator;
import com.example.midtermsexam_beauty.utilities.SessionManager;
import com.example.midtermsexam_beauty.utilities.SupabaseAuthService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RegisterActivity extends AppCompatActivity {

    private EditText etUsername, etEmail, etPassword, etConfirmPassword;
    private Button btnSignUp;
    private ImageButton btnBack;
    private TextView tvBackToLogin;
    private SupabaseAuthService authService;
    private ExecutorService executor;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        hideSystemUI();
        handleKeyboardOverlap();
        sessionManager = new SessionManager(this);
        if (sessionManager.isLoggedIn()) {
            AppNavigator.openAuthenticatedHome(this, sessionManager.isSeller(), true);
            return;
        }

        authService = new SupabaseAuthService();
        executor = Executors.newSingleThreadExecutor();

        etUsername = findViewById(R.id.regUsernameEditText);
        etEmail = findViewById(R.id.regEmailEditText);
        etPassword = findViewById(R.id.regPasswordEditText);
        etConfirmPassword = findViewById(R.id.regConfirmPasswordEditText);
        btnSignUp = findViewById(R.id.registerButton);
        btnBack = findViewById(R.id.back_btn);
        tvBackToLogin = findViewById(R.id.backToLoginTextView);

        btnBack.setOnClickListener(v -> finish());

        btnSignUp.setOnClickListener(v -> {
            String user = etUsername.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String pass = etPassword.getText().toString().trim();
            String confirmPass = etConfirmPassword.getText().toString().trim();

            if (user.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            } else if (!pass.equals(confirmPass)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            } else if (!authService.isConfigured()) {
                Toast.makeText(this, "Supabase is not configured. Add SUPABASE_URL and SUPABASE_ANON_KEY.", Toast.LENGTH_LONG).show();
            } else {
                btnSignUp.setEnabled(false);
                executor.execute(() -> {
                    SupabaseAuthService.AuthResult result = authService.signUp(email, pass, user);
                    runOnUiThread(() -> {
                        btnSignUp.setEnabled(true);
                        if (result.success) {
                            Toast.makeText(this, result.message, Toast.LENGTH_LONG).show();
                            AppNavigator.openLogin(RegisterActivity.this, true);
                        } else {
                            Toast.makeText(this, result.message, Toast.LENGTH_LONG).show();
                        }
                    });
                });
            }
        });

        tvBackToLogin.setOnClickListener(v -> AppNavigator.openLogin(RegisterActivity.this, true));
    }

    private void handleKeyboardOverlap() {
        View rootView = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            int imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            int navHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            v.setPadding(0, 0, 0, Math.max(imeHeight - navHeight, 0));
            return insets;
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
