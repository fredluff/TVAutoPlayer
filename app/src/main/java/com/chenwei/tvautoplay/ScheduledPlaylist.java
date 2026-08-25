package com.chenwei.tvautoplay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Playback content assigned to one daily schedule rule. */
public final class ScheduledPlaylist {
    private final ScheduleRule rule;
    private final List<PlaylistItem> playlist;

    public ScheduledPlaylist(ScheduleRule rule, List<PlaylistItem> playlist) {
        this.rule = Objects.requireNonNull(rule, "rule");
        this.playlist = Collections.unmodifiableList(
                new ArrayList<>(Objects.requireNonNull(playlist, "playlist"))
        );
    }

    public ScheduleRule rule() {
        return rule;
    }

    public List<PlaylistItem> playlist() {
        return playlist;
    }
}
