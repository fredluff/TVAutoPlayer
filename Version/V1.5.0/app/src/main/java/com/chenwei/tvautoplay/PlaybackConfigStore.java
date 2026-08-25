package com.chenwei.tvautoplay;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import java.util.Collections;
import java.util.List;

/** Stores only non-sensitive playback preferences; media content is never copied. */
public final class PlaybackConfigStore {
    private static final String PREFS_NAME = "playback_configuration";
    private static final String KEY_PLAYLIST = "playlist_json";
    private static final String KEY_SCHEDULE_ENABLED = "schedule_enabled";
    private static final String KEY_SCHEDULED_PLAYLISTS = "scheduled_playlists_json";
    private static final String KEY_VIDEO_URI = "video_uri";
    private static final String KEY_DISPLAY_NAME = "display_name";
    private static final String KEY_AUTO_START = "auto_start";
    private static final String KEY_SOUND = "sound";
    private static final String KEY_SCALE_MODE = "scale_mode";
    private static final String KEY_IMAGE_DURATION = "image_duration";
    private static final String KEY_HINT_SHOWN = "settings_hint_shown";

    private final SharedPreferences preferences;

    public PlaybackConfigStore(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public PlaybackConfiguration load() {
        List<PlaylistItem> playlist = loadPlaylist();
        return new PlaybackConfiguration(
                playlist,
                preferences.getBoolean(KEY_AUTO_START, true),
                preferences.getBoolean(KEY_SOUND, true),
                ScaleMode.fromStoredValue(preferences.getString(KEY_SCALE_MODE, null)),
                ImageDuration.fromStoredValue(
                        preferences.getString(KEY_IMAGE_DURATION, null)
                ),
                preferences.getBoolean(KEY_SCHEDULE_ENABLED, false),
                PlaybackConfigurationCodec.decodeSchedules(
                        preferences.getString(KEY_SCHEDULED_PLAYLISTS, null)
                )
        );
    }

    /** Retained for V1.0.0 call compatibility; new code should save the full playlist. */
    public void saveVideo(Uri uri, String displayName) {
        savePlaylist(Collections.singletonList(new PlaylistItem(uri.toString(), displayName)));
    }

    public void savePlaylist(List<PlaylistItem> playlist) {
        preferences.edit()
                .putString(KEY_PLAYLIST, PlaybackConfigurationCodec.encodePlaylist(playlist))
                .remove(KEY_VIDEO_URI)
                .remove(KEY_DISPLAY_NAME)
                .apply();
    }

    public void clearVideo() {
        clearPlaylist();
    }

    public void clearPlaylist() {
        preferences.edit()
                .remove(KEY_PLAYLIST)
                .remove(KEY_VIDEO_URI)
                .remove(KEY_DISPLAY_NAME)
                .apply();
    }

    public void setAutoStartEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_AUTO_START, enabled).apply();
    }

    public void setSoundEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_SOUND, enabled).apply();
    }

    public void setScaleMode(ScaleMode mode) {
        preferences.edit().putString(KEY_SCALE_MODE, mode.name()).apply();
    }

    public void setImageDuration(ImageDuration duration) {
        preferences.edit().putString(KEY_IMAGE_DURATION, duration.name()).apply();
    }

    public void setScheduleEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_SCHEDULE_ENABLED, enabled).apply();
    }

    public void saveScheduledPlaylists(List<ScheduledPlaylist> scheduledPlaylists) {
        preferences.edit()
                .putString(
                        KEY_SCHEDULED_PLAYLISTS,
                        PlaybackConfigurationCodec.encodeSchedules(scheduledPlaylists)
                )
                .apply();
    }

    public boolean isSettingsHintShown() {
        return preferences.getBoolean(KEY_HINT_SHOWN, false);
    }

    public void markSettingsHintShown() {
        preferences.edit().putBoolean(KEY_HINT_SHOWN, true).apply();
    }

    private List<PlaylistItem> loadPlaylist() {
        if (preferences.contains(KEY_PLAYLIST)) {
            return parsePlaylist(preferences.getString(KEY_PLAYLIST, "[]"));
        }

        String legacyUri = preferences.getString(KEY_VIDEO_URI, null);
        if (legacyUri == null || legacyUri.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<PlaylistItem> migrated = Collections.singletonList(new PlaylistItem(
                legacyUri,
                preferences.getString(KEY_DISPLAY_NAME, null)
        ));
        savePlaylist(migrated);
        return migrated;
    }

    private List<PlaylistItem> parsePlaylist(String storedValue) {
        return PlaybackConfigurationCodec.decodePlaylist(storedValue);
    }
}
