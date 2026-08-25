package com.chenwei.tvautoplay;

import java.util.List;

/** Pure decision logic kept independent from Android so critical startup behavior is testable. */
public final class PlaybackPolicy {
    private PlaybackPolicy() {
    }

    public static PlaybackDecision decide(
            PlaybackConfiguration configuration,
            boolean hasReadPermission
    ) {
        return decide(configuration, hasReadPermission ? 1 : 0);
    }

    public static PlaybackDecision decide(
            PlaybackConfiguration configuration,
            int readableVideoCount
    ) {
        return decide(configuration == null ? null : configuration.playlist(), readableVideoCount);
    }

    public static PlaybackDecision decide(
            List<PlaylistItem> effectivePlaylist,
            int readableMediaCount
    ) {
        if (effectivePlaylist == null || effectivePlaylist.isEmpty()) {
            return PlaybackDecision.NEEDS_SELECTION;
        }
        if (readableMediaCount <= 0) {
            return PlaybackDecision.PERMISSION_REQUIRED;
        }
        return PlaybackDecision.READY;
    }

    public static boolean shouldLaunchOnBoot(
            PlaybackConfiguration configuration,
            boolean hasReadPermission
    ) {
        return shouldLaunchOnBoot(configuration, hasReadPermission ? 1 : 0);
    }

    public static boolean shouldLaunchOnBoot(
            PlaybackConfiguration configuration,
            int readableVideoCount
    ) {
        return shouldLaunchOnBoot(
                configuration,
                configuration == null ? null : configuration.playlist(),
                readableVideoCount
        );
    }

    public static boolean shouldLaunchOnBoot(
            PlaybackConfiguration configuration,
            List<PlaylistItem> effectivePlaylist,
            int readableMediaCount
    ) {
        if (configuration == null || !configuration.autoStartEnabled()) {
            return false;
        }
        boolean waitingForFutureSchedule = configuration.scheduleEnabled()
                && ScheduleResolver.hasEnabledPlaylist(configuration.scheduledPlaylists())
                && (effectivePlaylist == null || effectivePlaylist.isEmpty());
        return waitingForFutureSchedule
                || decide(effectivePlaylist, readableMediaCount) == PlaybackDecision.READY;
    }
}
