package com.chenwei.tvautoplay;

import java.util.List;
import java.util.Objects;

/** Resolves a stored schedule into the playlist that should play now. */
public final class ScheduleResolver {
    private ScheduleResolver() {
    }

    public static ResolvedPlaylist resolve(
            List<PlaylistItem> defaultPlaylist,
            List<ScheduledPlaylist> scheduledPlaylists,
            boolean schedulingEnabled,
            int minuteOfDay
    ) {
        Objects.requireNonNull(defaultPlaylist, "defaultPlaylist");
        Objects.requireNonNull(scheduledPlaylists, "scheduledPlaylists");

        if (schedulingEnabled) {
            for (ScheduledPlaylist scheduledPlaylist : scheduledPlaylists) {
                if (scheduledPlaylist != null
                        && !scheduledPlaylist.playlist().isEmpty()
                        && scheduledPlaylist.rule().matches(minuteOfDay)) {
                    return ResolvedPlaylist.scheduled(
                            scheduledPlaylist.rule().id(),
                            scheduledPlaylist.playlist()
                    );
                }
            }
        }
        return ResolvedPlaylist.defaultPlaylist(defaultPlaylist);
    }

    public static boolean hasEnabledPlaylist(List<ScheduledPlaylist> scheduledPlaylists) {
        Objects.requireNonNull(scheduledPlaylists, "scheduledPlaylists");
        for (ScheduledPlaylist scheduledPlaylist : scheduledPlaylists) {
            if (scheduledPlaylist != null
                    && scheduledPlaylist.rule().isEnabled()
                    && !scheduledPlaylist.playlist().isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
