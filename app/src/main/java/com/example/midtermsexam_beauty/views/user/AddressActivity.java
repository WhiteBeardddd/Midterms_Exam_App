package com.example.midtermsexam_beauty.views.user;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.midtermsexam_beauty.R;
import com.example.midtermsexam_beauty.models.BuyerAddress;
import com.example.midtermsexam_beauty.utilities.SessionManager;
import com.example.midtermsexam_beauty.utilities.SupabaseAuthService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddressActivity extends AppCompatActivity {

    private EditText etStreet, etBarangay, etCity, etPostalCode, etCountry;
    private Button btnSaveAddress;

    private SessionManager sessionManager;
    private SupabaseAuthService authService;
    private ExecutorService executor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address);

        sessionManager = new SessionManager(this);
        authService = new SupabaseAuthService();
        executor = Executors.newSingleThreadExecutor();

        ImageButton btnBack = findViewById(R.id.back_btn);
        etStreet = findViewById(R.id.etStreet);
        etBarangay = findViewById(R.id.etBarangay);
        etCity = findViewById(R.id.etCity);
        etPostalCode = findViewById(R.id.etPostalCode);
        etCountry = findViewById(R.id.etCountry);
        btnSaveAddress = findViewById(R.id.btnSaveAddress);

        btnBack.setOnClickListener(v -> finish());
        btnSaveAddress.setOnClickListener(v -> saveAddress());

        loadAddress();
    }

    private void loadAddress() {
        executor.execute(() -> {
            // UPDATED: Using getProfileId() instead of getUserId()
            BuyerAddress address = authService.getBuyerAddress(
                    sessionManager.getToken(),
                    sessionManager.getProfileId()
            );

            if (address != null) {
                runOnUiThread(() -> {
                    etStreet.setText(address.getStreet());
                    etBarangay.setText(address.getBarangay());
                    etCity.setText(address.getCity());
                    etCountry.setText(address.getCountry());
                    if (address.getPostalCode() > 0) {
                        etPostalCode.setText(String.valueOf(address.getPostalCode()));
                    }
                });
            }
        });
    }

    private void saveAddress() {
        String street = etStreet.getText().toString().trim();
        String barangay = etBarangay.getText().toString().trim();
        String city = etCity.getText().toString().trim();
        String postalStr = etPostalCode.getText().toString().trim();
        String country = etCountry.getText().toString().trim();

        if (street.isEmpty() || city.isEmpty()) {
            Toast.makeText(this, "Street and City are required", Toast.LENGTH_SHORT).show();
            return;
        }

        int postalCode = 0;
        try {
            if (!postalStr.isEmpty()) postalCode = Integer.parseInt(postalStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid Postal Code", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSaveAddress.setEnabled(false);
        btnSaveAddress.setText("Saving...");

        int finalPostalCode = postalCode;
        executor.execute(() -> {
            BuyerAddress address = new BuyerAddress();

            // UPDATED: Using getProfileId() instead of getUserId()
            address.setBuyerId(sessionManager.getProfileId());

            address.setStreet(street);
            address.setBarangay(barangay);
            address.setCity(city);
            address.setPostalCode(finalPostalCode);
            address.setCountry(country.isEmpty() ? "Philippines" : country);

            boolean success = authService.saveBuyerAddress(sessionManager.getToken(), address);

            runOnUiThread(() -> {
                btnSaveAddress.setEnabled(true);
                btnSaveAddress.setText("Save Address");
                if (success) {
                    Toast.makeText(this, "Address Saved!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "Failed to save address.", Toast.LENGTH_SHORT).show();
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