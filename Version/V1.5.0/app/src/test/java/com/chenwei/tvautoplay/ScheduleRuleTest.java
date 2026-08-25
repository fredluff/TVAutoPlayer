package com.chenwei.tvautoplay;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class ScheduleRuleTest {
    @Test
    public void daytimeWindowIncludesStartAndExcludesEnd() {
        ScheduleRule rule = new ScheduleRule("morning", 8 * 60, 12 * 60, true);

        assertFalse(rule.matches(7 * 60 + 59));
        assertTrue(rule.matches(8 * 60));
        assertTrue(rule.matches(11 * 60 + 59));
        assertFalse(rule.matches(12 * 60));
    }

    @Test
    public void overnightWindowMatchesAcrossMidnight() {
        ScheduleRule rule = new ScheduleRule("night", 22 * 60, 6 * 60, true);

        assertTrue(rule.matches(23 * 60));
        assertTrue(rule.matches(0));
        assertTrue(rule.matches(5 * 60 + 59));
        assertFalse(rule.matches(6 * 60));
        assertFalse(rule.matches(12 * 60));
    }

    @Test
    public void disabledRuleNeverMatches() {
        ScheduleRule rule = new ScheduleRule("disabled", 8 * 60, 12 * 60, false);

        assertFalse(rule.matches(9 * 60));
    }

    @Test
    public void equalStartAndEndIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ScheduleRule("invalid", 60, 60, true)
        );
    }

    @Test
    public void overlapDetectionHandlesDaytimeAndOvernightWindows() {
        ScheduleRule morning = new ScheduleRule("morning", 8 * 60, 12 * 60, true);
        ScheduleRule lunch = new ScheduleRule("lunch", 11 * 60, 14 * 60, true);
        ScheduleRule evening = new ScheduleRule("evening", 18 * 60, 22 * 60, true);
        ScheduleRule overnight = new ScheduleRule("overnight", 22 * 60, 6 * 60, true);
        ScheduleRule early = new ScheduleRule("early", 5 * 60, 7 * 60, true);

        assertTrue(morning.overlaps(lunch));
        assertFalse(morning.overlaps(evening));
        assertTrue(overnight.overlaps(early));
        assertFalse(overnight.overlaps(evening));
    }
}
