package com.mindfocus.qa.support;

import java.time.LocalDate;

public final class Dates {
    private Dates() {}

    public static String daysAgo(int days) {
        return LocalDate.now().minusDays(days).toString();
    }
}
