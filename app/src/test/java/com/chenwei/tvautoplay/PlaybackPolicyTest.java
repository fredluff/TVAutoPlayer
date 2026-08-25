package com.chenwei.tvautoplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Collections;

/**
 * Behavioral tests at the playback-decision seam used by both activities and boot startup.
 */
public final class PlaybackPolicyTest {

    @Test
    public void firstLaunchWithoutSelectionRequestsVideoSelection() {
        PlaybackConfiguration configuration = PlaybackConfiguration.empty();

        assertEquals(
                PlaybackDecision.NEEDS_SELECTION,
                PlaybackPolicy.decide(configuration, false)
        );
    }

    @Test
    public void savedVideoWithPermissionIsReadyForLoopingPlayback() {
        PlaybackConfiguration configuration = new PlaybackConfiguration(
                "content://videos/intro.mp4",
                "intro.mp4",
                true,
                true,
                ScaleMode.FIT
        );

        assertEquals(PlaybackDecision.READY, PlaybackPolicy.decide(configuration, true));
    }

    @Test
    public void revokedFilePermissionRequestsReselectionInsteadOfCrashing() {
        PlaybackConfiguration configuration = new PlaybackConfiguration(
                "content://videos/missing.mp4",
                "missing.mp4",
                true,
                true,
                ScaleMode.FIT
        );

        assertEquals(
                PlaybackDecision.PERMISSION_REQUIRED,
                PlaybackPolicy.decide(configuration, false)
        );
    }

    @Test
    public void bootLaunchRequiresEnabledSettingSelectedVideoAndPermission() {
        PlaybackConfiguration enabled = new PlaybackConfiguration(
                "content://videos/signage.mkv",
                "signage.mkv",
                true,
                false,
                ScaleMode.ZOOM
        );
        PlaybackConfiguration disabled = new PlaybackConfiguration(
                enabled.videoUri(),
                enabled.displayName(),
                false,
                enabled.soundEnabled(),
                enabled.scaleMode()
        );

        assertTrue(PlaybackPolicy.shouldLaunchOnBoot(enabled, true));
        assertFalse(PlaybackPolicy.shouldLaunchOnBoot(enabled, false));
        assertFalse(PlaybackPolicy.shouldLaunchOnBoot(disabled, true));
        assertFalse(PlaybackPolicy.shouldLaunchOnBoot(PlaybackConfiguration.empty(), true));
    }

    @Test
    public void activeScheduledPlaylistCanPlayWhenDefaultPlaylistIsEmpty() {
        PlaybackConfiguration configuration = PlaybackConfiguration.empty();
        ResolvedPlaylist effective = ResolvedPlaylist.scheduled(
                "morning",
                Collections.singletonList(new PlaylistItem("content://morning", "morning.mp4"))
        );

        assertEquals(
                PlaybackDecision.READY,
                PlaybackPolicy.decide(effective.playlist(), 1)
        );
        assertTrue(PlaybackPolicy.shouldLaunchOnBoot(
                configuration,
                effective.playlist(),
                1
        ));
    }

    @Test
    public void futureScheduledWindowKeepsTheAppReadyAfterBoot() {
        ScheduledPlaylist morning = new ScheduledPlaylist(
                new ScheduleRule("morning", 8 * 60, 12 * 60, true),
                Collections.singletonList(new PlaylistItem("content://morning", "morning.mp4"))
        );
        PlaybackConfiguration configuration = new PlaybackConfiguration(
                Collections.emptyList(),
                true,
                true,
                ScaleMode.FIT,
                ImageDuration.DEFAULT,
                true,
                Collections.singletonList(morning)
        );

        assertTrue(PlaybackPolicy.shouldLaunchOnBoot(
                configuration,
                Collections.emptyList(),
                0
        ));
    }
}
