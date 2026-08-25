package com.chenwei.tvautoplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class ScheduleResolverTest {
    private final List<PlaylistItem> defaultPlaylist = Collections.singletonList(
            item("content://default", "default.mp4")
    );

    @Test
    public void disabledSchedulingAlwaysUsesDefaultPlaylist() {
        ScheduledPlaylist scheduled = scheduled(
                "morning",
                8 * 60,
                12 * 60,
                item("content://morning", "morning.mp4")
        );

        ResolvedPlaylist result = ScheduleResolver.resolve(
                defaultPlaylist,
                Collections.singletonList(scheduled),
                false,
                9 * 60
        );

        assertFalse(result.isScheduled());
        assertEquals(defaultPlaylist, result.playlist());
    }

    @Test
    public void matchingRuleUsesItsPlaylist() {
        PlaylistItem morningItem = item("content://morning", "morning.mp4");
        ScheduledPlaylist scheduled = scheduled("morning", 8 * 60, 12 * 60, morningItem);

        ResolvedPlaylist result = ScheduleResolver.resolve(
                defaultPlaylist,
                Collections.singletonList(scheduled),
                true,
                9 * 60
        );

        assertTrue(result.isScheduled());
        assertEquals("morning", result.sourceId());
        assertEquals(Collections.singletonList(morningItem), result.playlist());
    }

    @Test
    public void noMatchingRuleFallsBackToDefaultPlaylist() {
        ScheduledPlaylist scheduled = scheduled(
                "morning",
                8 * 60,
                12 * 60,
                item("content://morning", "morning.mp4")
        );

        ResolvedPlaylist result = ScheduleResolver.resolve(
                defaultPlaylist,
                Collections.singletonList(scheduled),
                true,
                15 * 60
        );

        assertFalse(result.isScheduled());
        assertEquals(defaultPlaylist, result.playlist());
    }

    @Test
    public void firstMatchingRuleHasStablePriority() {
        ScheduledPlaylist first = scheduled(
                "first",
                8 * 60,
                12 * 60,
                item("content://first", "first.mp4")
        );
        ScheduledPlaylist second = scheduled(
                "second",
                9 * 60,
                13 * 60,
                item("content://second", "second.mp4")
        );

        ResolvedPlaylist result = ScheduleResolver.resolve(
                defaultPlaylist,
                Arrays.asList(first, second),
                true,
                10 * 60
        );

        assertEquals("first", result.sourceId());
    }

    @Test
    public void emptyScheduledPlaylistIsIgnored() {
        ScheduledPlaylist empty = new ScheduledPlaylist(
                new ScheduleRule("empty", 8 * 60, 12 * 60, true),
                Collections.emptyList()
        );

        ResolvedPlaylist result = ScheduleResolver.resolve(
                defaultPlaylist,
                Collections.singletonList(empty),
                true,
                9 * 60
        );

        assertFalse(result.isScheduled());
        assertEquals(defaultPlaylist, result.playlist());
    }

    private ScheduledPlaylist scheduled(
            String id,
            int startMinute,
            int endMinute,
            PlaylistItem... items
    ) {
        return new ScheduledPlaylist(
                new ScheduleRule(id, startMinute, endMinute, true),
                Arrays.asList(items)
        );
    }

    private PlaylistItem item(String uri, String name) {
        return new PlaylistItem(uri, name);
    }
}
