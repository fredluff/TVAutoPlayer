package com.chenwei.tvautoplay;

import java.util.Objects;

/**
 * Immutable daily playback window. Start is inclusive and end is exclusive.
 */
public final class ScheduleRule {
    public static final int MINUTES_PER_DAY = 24 * 60;

    private final String id;
    private final int startMinute;
    private final int endMinute;
    private final boolean enabled;

    public ScheduleRule(String id, int startMinute, int endMinute, boolean enabled) {
        String normalizedId = Objects.requireNonNull(id, "id").trim();
        if (normalizedId.isEmpty()) {
            throw new IllegalArgumentException("Schedule id must not be empty");
        }
        validateMinute(startMinute, "startMinute");
        validateMinute(endMinute, "endMinute");
        if (startMinute == endMinute) {
            throw new IllegalArgumentException("Schedule start and end must differ");
        }

        this.id = normalizedId;
        this.startMinute = startMinute;
        this.endMinute = endMinute;
        this.enabled = enabled;
    }

    public String id() {
        return id;
    }

    public int startMinute() {
        return startMinute;
    }

    public int endMinute() {
        return endMinute;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean matches(int minuteOfDay) {
        validateMinute(minuteOfDay, "minuteOfDay");
        if (!enabled) {
            return false;
        }
        return containsIgnoringEnabled(minuteOfDay);
    }

    public boolean overlaps(ScheduleRule other) {
        Objects.requireNonNull(other, "other");
        if (!enabled || !other.enabled) {
            return false;
        }
        for (int minute = 0; minute < MINUTES_PER_DAY; minute++) {
            if (containsIgnoringEnabled(minute) && other.containsIgnoringEnabled(minute)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsIgnoringEnabled(int minuteOfDay) {
        if (startMinute < endMinute) {
            return minuteOfDay >= startMinute && minuteOfDay < endMinute;
        }
        return minuteOfDay >= startMinute || minuteOfDay < endMinute;
    }

    private static void validateMinute(int minute, String fieldName) {
        if (minute < 0 || minute >= MINUTES_PER_DAY) {
            throw new IllegalArgumentException(fieldName + " must be between 0 and 1439");
        }
    }
}
