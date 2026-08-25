package com.chenwei.tvautoplay;

import java.util.Calendar;

/** Supplies local wall-clock time without requiring API 26 java.time classes. */
public final class ScheduleClock {
    private ScheduleClock() {
    }

    public static int currentMinuteOfDay() {
        Calendar now = Calendar.getInstance();
        return now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
    }
}
