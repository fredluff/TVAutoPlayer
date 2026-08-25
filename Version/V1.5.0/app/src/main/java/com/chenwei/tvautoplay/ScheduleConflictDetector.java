package com.chenwei.tvautoplay;

import java.util.List;
import java.util.Objects;

/** Detects ambiguous enabled time windows before a schedule is saved. */
public final class ScheduleConflictDetector {
    private ScheduleConflictDetector() {
    }

    public static String findConflictId(
            List<ScheduledPlaylist> existingSchedules,
            ScheduleRule candidate,
            String excludedRuleId
    ) {
        Objects.requireNonNull(existingSchedules, "existingSchedules");
        Objects.requireNonNull(candidate, "candidate");
        for (ScheduledPlaylist existing : existingSchedules) {
            if (existing == null) {
                continue;
            }
            ScheduleRule existingRule = existing.rule();
            if (existingRule.id().equals(excludedRuleId)) {
                continue;
            }
            if (candidate.overlaps(existingRule)) {
                return existingRule.id();
            }
        }
        return null;
    }
}
