package com.example.midtermsexam_beauty.utilities;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class BaseAuthenticatedActivity extends AppCompatActivity {
    protected SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionManager = new SessionManager(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkSession();
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        // This triggers anytime the user taps, scrolls, or types
        checkSession();
    }

    private void checkSession() {
        if (sessionManager == null) return;

        // If there is no token, or the inactivity timer ran out
        if (sessionManager.getToken() == null || sessionManager.isSessionExpired()) {
            forceLogout();
        } else {
            // User is active, reset the clock!
            sessionManager.updateLastActiveTime();
        }
    }

    private void forceLogout() {
        Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show();
        sessionManager.clearSession(); // Erase the data

        // This is already in your AppNavigator!
        // The 'true' boolean clears the backstack so they can't press back to return.
        AppNavigator.openLanding(this, true);
    }
}