package com.example.midtermsexam_beauty.adapters;

import java.util.Locale;

final class ShopCardFormatter {
    private ShopCardFormatter() {
    }

    static String buildRatingLabel(float rating, int position, int[] reviewCounts) {
        int reviewCount = reviewCounts[position % reviewCounts.length];
        return String.format(Locale.US, "%.1f (%d)", rating, reviewCount);
    }

    static String buildMetaLabel(String category, int position, int[] etaMinutes) {
        int eta = etaMinutes[position % etaMinutes.length];
        return String.format(Locale.US, "From %d min - %s", eta, category);
    }
}
