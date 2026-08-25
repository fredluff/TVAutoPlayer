package com.chenwei.tvautoplay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Effective playlist selected for the current time. */
public final class ResolvedPlaylist {
    public static final String DEFAULT_SOURCE_ID = "default";

    private final String sourceId;
    private final List<PlaylistItem> playlist;
    private final boolean scheduled;

    private ResolvedPlaylist(String sourceId, List<PlaylistItem> playlist, boolean scheduled) {
        this.sourceId = Objects.requireNonNull(sourceId, "sourceId");
        this.playlist = Collections.unmodifiableList(
                new ArrayList<>(Objects.requireNonNull(playlist, "playlist"))
        );
        this.scheduled = scheduled;
    }

    public static ResolvedPlaylist defaultPlaylist(List<PlaylistItem> playlist) {
        return new ResolvedPlaylist(DEFAULT_SOURCE_ID, playlist, false);
    }

    public static ResolvedPlaylist scheduled(String sourceId, List<PlaylistItem> playlist) {
        return new ResolvedPlaylist(sourceId, playlist, true);
    }

    public String sourceId() {
        return sourceId;
    }

    public List<PlaylistItem> playlist() {
        return playlist;
    }

    public boolean isScheduled() {
        return scheduled;
    }
}
