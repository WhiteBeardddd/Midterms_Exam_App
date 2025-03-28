package com.example.midtermsexam_beauty;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.midtermsexam_beauty.display.Homepage;

public class MainActivity extends AppCompatActivity {
    Button logInBtn;
    Button signUpBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        logInBtn = findViewById(R.id.logInButton);
        signUpBtn = findViewById(R.id.signUpButton);

        logInBtn.setOnClickListener(v -> {
            Intent toHome_Page= new Intent(MainActivity.this, Homepage.class);
            startActivity(toHome_Page);
        });

        signUpBtn.setOnClickListener(v -> {
            Intent toHome_Page= new Intent(MainActivity.this, Homepage.class);
            startActivity(toHome_Page);
        });


    }
}