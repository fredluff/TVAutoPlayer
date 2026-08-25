package com.chenwei.tvautoplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public final class ScheduleConflictDetectorTest {
    @Test
    public void reportsTheFirstOverlappingEnabledRule() {
        ScheduledPlaylist morning = schedule("morning", 8 * 60, 12 * 60, true);
        ScheduledPlaylist evening = schedule("evening", 18 * 60, 22 * 60, true);
        ScheduleRule candidate = new ScheduleRule("new", 11 * 60, 13 * 60, true);

        assertEquals(
                "morning",
                ScheduleConflictDetector.findConflictId(
                        Arrays.asList(morning, evening),
                        candidate,
                        null
                )
        );
    }

    @Test
    public void allowsAdjacentWindowsAndTheRuleBeingEdited() {
        ScheduledPlaylist morning = schedule("morning", 8 * 60, 12 * 60, true);

        assertNull(ScheduleConflictDetector.findConflictId(
                Collections.singletonList(morning),
                new ScheduleRule("afternoon", 12 * 60, 18 * 60, true),
                null
        ));
        assertNull(ScheduleConflictDetector.findConflictId(
                Collections.singletonList(morning),
                new ScheduleRule("morning", 9 * 60, 13 * 60, true),
                "morning"
        ));
    }

    @Test
    public void disabledRulesDoNotCreateConflicts() {
        ScheduledPlaylist disabled = schedule("disabled", 8 * 60, 12 * 60, false);

        assertNull(ScheduleConflictDetector.findConflictId(
                Collections.singletonList(disabled),
                new ScheduleRule("new", 9 * 60, 10 * 60, true),
                null
        ));
    }

    private ScheduledPlaylist schedule(String id, int start, int end, boolean enabled) {
        return new ScheduledPlaylist(
                new ScheduleRule(id, start, end, enabled),
                Collections.singletonList(new PlaylistItem("content://" + id, id + ".mp4"))
        );
    }
}
