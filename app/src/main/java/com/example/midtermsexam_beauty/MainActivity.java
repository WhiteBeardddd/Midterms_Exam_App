package com.example.midtermsexam_beauty;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.midtermsexam_beauty.display.Homepage;
import com.example.midtermsexam_beauty.display.LoginActivity;
import com.example.midtermsexam_beauty.display.RegisterActivity;

public class MainActivity extends AppCompatActivity {
    Button logInBtn;
    Button signUpBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        hideSystemUI();

        logInBtn = findViewById(R.id.logInButton);
        signUpBtn = findViewById(R.id.signUpButton);

        logInBtn.setOnClickListener(v -> {
            Intent toLogin = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(toLogin);
        });

        signUpBtn.setOnClickListener(v -> {
            Intent toRegister = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(toRegister);
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