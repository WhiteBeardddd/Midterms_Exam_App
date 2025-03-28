package com.example.midtermsexam_beauty;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class SharedPref extends AppCompatActivity {

    EditText Name, Email;
    TextView RetrieveName, RetrieveEmail;
    Button Save, Retrieve, Delete;

    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_NAME = "nameKey";
    private static final String KEY_EMAIL = "emailKey";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_shared_pref);

        Name = findViewById(R.id.nameEditText);
        Email = findViewById(R.id.emailEditText);
        RetrieveName = findViewById(R.id.retrieveName);
        RetrieveEmail = findViewById(R.id.retrieveEmail);
        Save = findViewById(R.id.saveButton);
        Retrieve = findViewById(R.id.retrieveButton);
        Delete = findViewById(R.id.deleteButton);

        // Save Data
        Save.setOnClickListener(view -> {
            String name = Name.getText().toString().trim();
            String email = Email.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty()) {
                Toast.makeText(this, "Please enter both Name and Email", Toast.LENGTH_SHORT).show();
                return;
            }

            SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(KEY_NAME, name);
            editor.putString(KEY_EMAIL, email);
            editor.apply();

            Toast.makeText(this, "Data Saved!", Toast.LENGTH_SHORT).show();
        });

        // Retrieve Data
        Retrieve.setOnClickListener(view -> {
            SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            String name = sharedPreferences.getString(KEY_NAME, "No Name Saved");
            String email = sharedPreferences.getString(KEY_EMAIL, "No Email Saved");

            // Set retrieved values in TextViews
            RetrieveName.setText(name);
            RetrieveEmail.setText(email);

            Toast.makeText(this, "Data Retrieved!", Toast.LENGTH_SHORT).show();
        });

        // Delete Data
        Delete.setOnClickListener(view -> {
            SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove(KEY_NAME);
            editor.remove(KEY_EMAIL);
            editor.apply();

            Name.setText("");
            Email.setText("");
            RetrieveName.setText("No Name Saved");
            RetrieveEmail.setText("No Email Saved");

            Toast.makeText(this, "Data Deleted!", Toast.LENGTH_SHORT).show();
        });
    }
}
