package com.example.midtermsexam_beauty.views.user;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.midtermsexam_beauty.R;
import com.example.midtermsexam_beauty.utilities.BaseAuthenticatedActivity;
import com.example.midtermsexam_beauty.utilities.SupabaseAuthService;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OrderDetailsActivity extends BaseAuthenticatedActivity {

    private SupabaseAuthService authService;
    private ExecutorService executor;
    private String orderId;
    private Button btnLeaveReview; // Moved to class level so methods can access it

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_details);
        hideSystemUI();

        authService = new SupabaseAuthService();
        executor = Executors.newSingleThreadExecutor();

        // 1. Initialize UI Elements
        ImageButton btnBack = findViewById(R.id.btnBack);
        TextView tvOrderId = findViewById(R.id.tvDetailOrderId);
        TextView tvStatus = findViewById(R.id.tvDetailStatus);
        TextView tvDate = findViewById(R.id.tvDetailDate);
        TextView tvAddress = findViewById(R.id.tvDetailAddress);
        TextView tvTotal = findViewById(R.id.tvDetailTotal);
        btnLeaveReview = findViewById(R.id.btnLeaveReview);

        btnBack.setOnClickListener(v -> finish());

        // 2. Extract Data from Intent
        orderId = getIntent().getStringExtra("ORDER_ID");
        String status = getIntent().getStringExtra("ORDER_STATUS");
        String date = getIntent().getStringExtra("ORDER_DATE");
        String address = getIntent().getStringExtra("ORDER_ADDRESS");
        double total = getIntent().getDoubleExtra("ORDER_TOTAL", 0.0);

        // 3. Format and Set Data
        String cleanStatus = (status != null) ? status.trim() : "pending";
        String cleanDate = (date != null && date.length() >= 16)
                ? date.replace("T", " ").substring(0, 16)
                : "Recently";

        tvOrderId.setText(orderId);
        tvStatus.setText(cleanStatus.toUpperCase());
        tvDate.setText(cleanDate);
        tvAddress.setText(address != null ? address : "No address provided");
        tvTotal.setText(String.format(Locale.US, "₱%.2f", total));

        // 4. Implement Review Check Logic
        // Hide button by default while we check the database
        btnLeaveReview.setVisibility(View.GONE);

        if ("delivered".equalsIgnoreCase(cleanStatus) || "done".equalsIgnoreCase(cleanStatus)) {
            verifyReviewStatus();
        }
    }

    // --- NEW METHOD: Checks database before showing the button ---
    private void verifyReviewStatus() {
        executor.execute(() -> {
            boolean alreadyReviewed = authService.hasUserReviewedOrder(sessionManager.getToken(), orderId);

            runOnUiThread(() -> {
                if (alreadyReviewed) {
                    // They already reviewed it, keep the button hidden
                    btnLeaveReview.setVisibility(View.GONE);
                } else {
                    // No review found! Show the button.
                    btnLeaveReview.setVisibility(View.VISIBLE);
                    btnLeaveReview.setOnClickListener(v -> showReviewDialog());
                }
            });
        });
    }

    private void showReviewDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_review, null);
        RatingBar ratingBar = view.findViewById(R.id.ratingBar);
        EditText etComment = view.findViewById(R.id.etComment);

        new AlertDialog.Builder(this)
                .setView(view)
                .setTitle("Submit Review")
                .setPositiveButton("Submit", (dialog, which) -> {
                    int rating = (int) ratingBar.getRating();
                    String comment = etComment.getText().toString().trim();
                    submitReviewToDatabase(rating, comment);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void submitReviewToDatabase(int rating, String comment) {
        Toast.makeText(this, "Sending review...", Toast.LENGTH_SHORT).show();

        executor.execute(() -> {
            boolean success = authService.submitReview(
                    sessionManager.getToken(),
                    orderId,
                    sessionManager.getProfileId(),
                    rating,
                    comment
            );

            runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(this, "Review submitted! Thank you.", Toast.LENGTH_LONG).show();
                    // Instantly hide the button so they can't click it again
                    btnLeaveReview.setVisibility(View.GONE);
                } else {
                    Toast.makeText(this, "Failed to submit review. Try again.", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void hideSystemUI() {
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
    }

    @Override
    protected void onDestroy() {
        if (executor != null) executor.shutdown();
        super.onDestroy();
    }
}