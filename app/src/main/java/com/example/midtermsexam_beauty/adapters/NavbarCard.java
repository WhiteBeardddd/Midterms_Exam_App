package com.example.midtermsexam_beauty.adapters;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.midtermsexam_beauty.R;
import com.example.midtermsexam_beauty.views.user.Checkout;
import com.example.midtermsexam_beauty.views.user.FeaturedProducts;
import com.example.midtermsexam_beauty.views.user.Homepage;
import com.example.midtermsexam_beauty.views.user.PopularShops;
import com.example.midtermsexam_beauty.views.user.UserProfile;

public class NavbarCard {

    private static final int ACTIVE_COLOR = 0xFF2ED573;
    private static final int INACTIVE_COLOR = 0xFFAAB3BC;

    public static void setupNavbar(Activity activity) {
        View navigationBar = activity.findViewById(R.id.navigation_bar);
        View navFoodTab = activity.findViewById(R.id.nav_food_tab);
        View navSearchTab = activity.findViewById(R.id.nav_search_tab);
        View navCartTab = activity.findViewById(R.id.nav_cart_tab);
        View navAccountTab = activity.findViewById(R.id.nav_account_tab);

        ImageButton navFood = activity.findViewById(R.id.nav_food);
        ImageButton navSearch = activity.findViewById(R.id.nav_search);
        ImageButton navCart = activity.findViewById(R.id.nav_cart);
        ImageButton navAccount = activity.findViewById(R.id.nav_account);

        TextView navFoodLabel = activity.findViewById(R.id.nav_food_label);
        TextView navSearchLabel = activity.findViewById(R.id.nav_search_label);
        TextView navCartLabel = activity.findViewById(R.id.nav_cart_label);
        TextView navAccountLabel = activity.findViewById(R.id.nav_account_label);

        applySystemBarInsets(navigationBar);

        if (navFoodTab != null) {
            navFoodTab.setOnClickListener(v -> navigateTo(activity, Homepage.class));
        }
        if (navFood != null) {
            navFood.setOnClickListener(v -> navigateTo(activity, Homepage.class));
        }
        if (navSearchTab != null) {
            navSearchTab.setOnClickListener(v -> navigateTo(activity, PopularShops.class));
        }
        if (navSearch != null) {
            navSearch.setOnClickListener(v -> navigateTo(activity, PopularShops.class));
        }
        if (navCartTab != null) {
            navCartTab.setOnClickListener(v -> navigateTo(activity, Checkout.class));
        }
        if (navCart != null) {
            navCart.setOnClickListener(v -> navigateTo(activity, Checkout.class));
        }
        if (navAccountTab != null) {
            navAccountTab.setOnClickListener(v -> navigateTo(activity, UserProfile.class));
        }
        if (navAccount != null) {
            navAccount.setOnClickListener(v -> navigateTo(activity, UserProfile.class));
        }

        setInactive(navFood, navFoodLabel);
        setInactive(navSearch, navSearchLabel);
        setInactive(navCart, navCartLabel);
        setInactive(navAccount, navAccountLabel);

        if (activity instanceof Homepage) {
            setActive(navFood, navFoodLabel);
        } else if (activity instanceof PopularShops || activity instanceof FeaturedProducts) {
            setActive(navSearch, navSearchLabel);
        } else if (activity instanceof Checkout) {
            setActive(navCart, navCartLabel);
        } else if (activity instanceof UserProfile) {
            setActive(navAccount, navAccountLabel);
        }
    }

    private static void applySystemBarInsets(View navigationBar) {
        if (navigationBar == null) {
            return;
        }

        final int left = navigationBar.getPaddingLeft();
        final int top = navigationBar.getPaddingTop();
        final int right = navigationBar.getPaddingRight();
        final int bottom = navigationBar.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(navigationBar, (view, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(left, top, right, bottom + insets.bottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(navigationBar);
    }

    private static void navigateTo(Activity activity, Class<?> targetClass) {
        if (!activity.getClass().equals(targetClass)) {
            Intent intent = new Intent(activity, targetClass);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NO_ANIMATION);
            activity.startActivity(intent);
            activity.overridePendingTransition(0, 0);
        }
    }

    private static void setActive(ImageButton button, TextView label) {
        if (button != null) {
            button.setColorFilter(ACTIVE_COLOR);
        }
        if (label != null) {
            label.setTextColor(ACTIVE_COLOR);
        }
    }

    private static void setInactive(ImageButton button, TextView label) {
        if (button != null) {
            button.setColorFilter(INACTIVE_COLOR);
        }
        if (label != null) {
            label.setTextColor(INACTIVE_COLOR);
        }
    }
}
