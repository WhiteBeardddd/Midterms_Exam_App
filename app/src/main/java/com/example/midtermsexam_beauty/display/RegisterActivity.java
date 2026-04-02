package com.example.midtermsexam_beauty.display;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.example.midtermsexam_beauty.R;
import com.example.midtermsexam_beauty.utilities.DatabaseHelper;

public class RegisterActivity extends AppCompatActivity {

    private EditText etUsername, etEmail, etPassword, etConfirmPassword;
    private Button btnSignUp;
    private TextView tvBackToLogin;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        hideSystemUI();
        
        dbHelper = new DatabaseHelper(this);

        etUsername = findViewById(R.id.regUsernameEditText);
        etEmail = findViewById(R.id.regEmailEditText);
        etPassword = findViewById(R.id.regPasswordEditText);
        etConfirmPassword = findViewById(R.id.regConfirmPasswordEditText);
        btnSignUp = findViewById(R.id.registerButton);
        tvBackToLogin = findViewById(R.id.backToLoginTextView);

        btnSignUp.setOnClickListener(v -> {
            String user = etUsername.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String pass = etPassword.getText().toString().trim();
            String confirmPass = etConfirmPassword.getText().toString().trim();

            if (user.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            } else if (!pass.equals(confirmPass)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            } else {
                if (dbHelper.checkUserExists(user)) {
                    Toast.makeText(this, "User already exists!", Toast.LENGTH_SHORT).show();
                } else {
                    boolean inserted = dbHelper.addUser(user, email, pass);
                    if (inserted) {
                        Toast.makeText(this, "Registration Successful!", Toast.LENGTH_SHORT).show();
                        // Navigate back to Login
                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(this, "Registration Failed!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        tvBackToLogin.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }
}
