package com.chenwei.tvautoplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class PlaybackConfigurationCodecTest {
    @Test
    public void scheduledPlaylistsRoundTripWithoutLosingMediaKinds() {
        ScheduledPlaylist schedule = new ScheduledPlaylist(
                new ScheduleRule("morning", 8 * 60, 12 * 60, true),
                Arrays.asList(
                        new PlaylistItem("content://video", "intro.mp4", MediaKind.VIDEO),
                        new PlaylistItem("content://image", "notice.png", MediaKind.IMAGE)
                )
        );

        String stored = PlaybackConfigurationCodec.encodeSchedules(
                Collections.singletonList(schedule)
        );
        List<ScheduledPlaylist> restored = PlaybackConfigurationCodec.decodeSchedules(stored);

        assertEquals(1, restored.size());
        assertEquals("morning", restored.get(0).rule().id());
        assertEquals(8 * 60, restored.get(0).rule().startMinute());
        assertEquals(12 * 60, restored.get(0).rule().endMinute());
        assertTrue(restored.get(0).rule().isEnabled());
        assertEquals(schedule.playlist(), restored.get(0).playlist());
    }

    @Test
    public void corruptScheduleStorageFailsClosed() {
        assertTrue(PlaybackConfigurationCodec.decodeSchedules("not-json").isEmpty());
        assertTrue(PlaybackConfigurationCodec.decodeSchedules(null).isEmpty());
    }

    @Test
    public void invalidScheduleEntriesAreIgnoredWithoutDroppingValidEntries() {
        String stored = "{\"schemaVersion\":1,\"schedules\":["
                + "{\"id\":\"invalid\",\"startMinute\":60,\"endMinute\":60,"
                + "\"enabled\":true,\"playlist\":[]},"
                + "{\"id\":\"valid\",\"startMinute\":120,\"endMinute\":180,"
                + "\"enabled\":true,\"playlist\":[{\"uri\":\"content://ok\","
                + "\"name\":\"ok.mp4\",\"kind\":\"VIDEO\"}]}]}";

        List<ScheduledPlaylist> restored = PlaybackConfigurationCodec.decodeSchedules(stored);

        assertEquals(1, restored.size());
        assertEquals("valid", restored.get(0).rule().id());
    }

    @Test
    public void legacyConfigurationDefaultsToSchedulingOff() {
        PlaybackConfiguration configuration = new PlaybackConfiguration(
                Collections.singletonList(new PlaylistItem("content://legacy", "legacy.mp4")),
                true,
                true,
                ScaleMode.FIT,
                ImageDuration.DEFAULT
        );

        assertFalse(configuration.scheduleEnabled());
        assertTrue(configuration.scheduledPlaylists().isEmpty());
    }
}
