package com.chenwei.tvautoplay;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Creates or edits one daily time window and its independent playlist. */
public final class ScheduleEditorActivity extends ComponentActivity {
    public static final String EXTRA_RULE_ID = "rule_id";

    private static final String STATE_RULE_ID = "rule_id";
    private static final String STATE_CREATING_NEW = "creating_new";
    private static final String STATE_START_MINUTE = "start_minute";
    private static final String STATE_END_MINUTE = "end_minute";
    private static final String STATE_RULE_ENABLED = "rule_enabled";
    private static final String STATE_PLAYLIST = "playlist";

    private PlaybackConfigStore configStore;
    private Button startButton;
    private Button endButton;
    private CheckBox ruleEnabled;
    private TextView selectedMedia;
    private Button deleteButton;
    private ActivityResultLauncher<Intent> mediaLibraryLauncher;
    private String ruleId;
    private boolean creatingNew;
    private int startMinute;
    private int endMinute;
    private List<PlaylistItem> playlist = new ArrayList<>();

    private interface TimeSelection {
        void onSelect(int minuteOfDay);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mediaLibraryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> handleMediaSelection(result.getResultCode(), result.getData())
        );
        setContentView(R.layout.activity_schedule_editor);
        configStore = new PlaybackConfigStore(this);

        startButton = findViewById(R.id.schedule_start_button);
        endButton = findViewById(R.id.schedule_end_button);
        ruleEnabled = findViewById(R.id.schedule_rule_enabled_checkbox);
        selectedMedia = findViewById(R.id.schedule_selected_media);
        deleteButton = findViewById(R.id.delete_schedule_button);

        restoreEditorState(savedInstanceState);
        startButton.setOnClickListener(view -> showTimePicker(
                R.string.schedule_start_time,
                startMinute,
                value -> startMinute = value
        ));
        endButton.setOnClickListener(view -> showTimePicker(
                R.string.schedule_end_time,
                endMinute,
                value -> endMinute = value
        ));
        findViewById(R.id.select_schedule_media_button).setOnClickListener(
                view -> openMediaLibrary()
        );
        findViewById(R.id.save_schedule_button).setOnClickListener(view -> saveSchedule());
        deleteButton.setOnClickListener(view -> confirmDelete());
        findViewById(R.id.cancel_schedule_edit_button).setOnClickListener(view -> finish());
        refreshEditor();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(STATE_RULE_ID, ruleId);
        outState.putBoolean(STATE_CREATING_NEW, creatingNew);
        outState.putInt(STATE_START_MINUTE, startMinute);
        outState.putInt(STATE_END_MINUTE, endMinute);
        outState.putBoolean(STATE_RULE_ENABLED, ruleEnabled.isChecked());
        outState.putString(STATE_PLAYLIST, PlaybackConfigurationCodec.encodePlaylist(playlist));
        super.onSaveInstanceState(outState);
    }

    private void restoreEditorState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            ruleId = savedInstanceState.getString(STATE_RULE_ID);
            creatingNew = savedInstanceState.getBoolean(STATE_CREATING_NEW, true);
            startMinute = savedInstanceState.getInt(STATE_START_MINUTE, 8 * 60);
            endMinute = savedInstanceState.getInt(STATE_END_MINUTE, 18 * 60);
            playlist = PlaybackConfigurationCodec.decodePlaylist(
                    savedInstanceState.getString(STATE_PLAYLIST)
            );
            ruleEnabled.setChecked(savedInstanceState.getBoolean(STATE_RULE_ENABLED, true));
            return;
        }

        String requestedRuleId = getIntent().getStringExtra(EXTRA_RULE_ID);
        ScheduledPlaylist existing = findSchedule(
                configStore.load().scheduledPlaylists(),
                requestedRuleId
        );
        creatingNew = existing == null;
        if (existing == null) {
            ruleId = UUID.randomUUID().toString();
            startMinute = 8 * 60;
            endMinute = 18 * 60;
            ruleEnabled.setChecked(true);
            return;
        }

        ruleId = existing.rule().id();
        startMinute = existing.rule().startMinute();
        endMinute = existing.rule().endMinute();
        ruleEnabled.setChecked(existing.rule().isEnabled());
        playlist = new ArrayList<>(existing.playlist());
    }

    private void refreshEditor() {
        startButton.setText(getString(
                R.string.schedule_start_time_value,
                formatTime(startMinute)
        ));
        endButton.setText(getString(
                R.string.schedule_end_time_value,
                formatTime(endMinute)
        ));
        selectedMedia.setText(playlist.isEmpty()
                ? getString(R.string.schedule_no_media)
                : formatPlaylist(playlist));
        deleteButton.setVisibility(creatingNew ? View.GONE : View.VISIBLE);
    }

    private void showTimePicker(
            int titleResource,
            int selectedMinute,
            TimeSelection onSelected
    ) {
        String[] labels = new String[48];
        for (int index = 0; index < labels.length; index++) {
            labels[index] = formatTime(index * 30);
        }
        new AlertDialog.Builder(this)
                .setTitle(titleResource)
                .setSingleChoiceItems(labels, selectedMinute / 30, (dialog, which) -> {
                    onSelected.onSelect(which * 30);
                    dialog.dismiss();
                    refreshEditor();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void openMediaLibrary() {
        Intent intent = new Intent(this, MediaLibraryActivity.class)
                .putExtra(MediaLibraryActivity.EXTRA_RETURN_SELECTION, true)
                .putExtra(
                        MediaLibraryActivity.EXTRA_INITIAL_PLAYLIST_JSON,
                        PlaybackConfigurationCodec.encodePlaylist(playlist)
                );
        mediaLibraryLauncher.launch(intent);
    }

    private void handleMediaSelection(int resultCode, Intent data) {
        if (resultCode != RESULT_OK || data == null) {
            return;
        }
        List<PlaylistItem> selected = PlaybackConfigurationCodec.decodePlaylist(
                data.getStringExtra(MediaLibraryActivity.EXTRA_SELECTED_PLAYLIST_JSON)
        );
        if (!selected.isEmpty()) {
            playlist = selected;
            refreshEditor();
        }
    }

    private void saveSchedule() {
        if (playlist.isEmpty()) {
            Toast.makeText(this, R.string.schedule_media_required, Toast.LENGTH_LONG).show();
            return;
        }

        ScheduleRule candidate;
        try {
            candidate = new ScheduleRule(
                    ruleId,
                    startMinute,
                    endMinute,
                    ruleEnabled.isChecked()
            );
        } catch (IllegalArgumentException exception) {
            Toast.makeText(this, R.string.schedule_time_invalid, Toast.LENGTH_LONG).show();
            return;
        }

        PlaybackConfiguration configuration = configStore.load();
        List<ScheduledPlaylist> schedules = new ArrayList<>(
                configuration.scheduledPlaylists()
        );
        String conflictId = ScheduleConflictDetector.findConflictId(
                schedules,
                candidate,
                creatingNew ? null : ruleId
        );
        if (conflictId != null) {
            ScheduledPlaylist conflict = findSchedule(schedules, conflictId);
            String conflictWindow = conflict == null
                    ? getString(R.string.schedule_conflict_unknown)
                    : getString(
                            R.string.schedule_window_value,
                            formatTime(conflict.rule().startMinute()),
                            formatTime(conflict.rule().endMinute())
                    );
            Toast.makeText(
                    this,
                    getString(R.string.schedule_conflict, conflictWindow),
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        ScheduledPlaylist updated = new ScheduledPlaylist(candidate, playlist);
        boolean replaced = false;
        for (int index = 0; index < schedules.size(); index++) {
            if (schedules.get(index).rule().id().equals(ruleId)) {
                schedules.set(index, updated);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            schedules.add(updated);
        }
        configStore.saveScheduledPlaylists(schedules);
        if (configuration.scheduledPlaylists().isEmpty()) {
            configStore.setScheduleEnabled(true);
        }
        setResult(RESULT_OK);
        finish();
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_schedule_title)
                .setMessage(R.string.delete_schedule_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> deleteSchedule())
                .show();
    }

    private void deleteSchedule() {
        List<ScheduledPlaylist> schedules = new ArrayList<>(
                configStore.load().scheduledPlaylists()
        );
        for (int index = schedules.size() - 1; index >= 0; index--) {
            if (schedules.get(index).rule().id().equals(ruleId)) {
                schedules.remove(index);
            }
        }
        configStore.saveScheduledPlaylists(schedules);
        if (schedules.isEmpty()) {
            configStore.setScheduleEnabled(false);
        }
        setResult(RESULT_OK);
        finish();
    }

    private ScheduledPlaylist findSchedule(List<ScheduledPlaylist> schedules, String id) {
        if (id == null) {
            return null;
        }
        for (ScheduledPlaylist schedule : schedules) {
            if (schedule.rule().id().equals(id)) {
                return schedule;
            }
        }
        return null;
    }

    private String formatPlaylist(List<PlaylistItem> items) {
        StringBuilder value = new StringBuilder(
                getString(R.string.selected_video_count, items.size())
        );
        for (int index = 0; index < items.size(); index++) {
            PlaylistItem item = items.get(index);
            value.append('\n')
                    .append(index + 1)
                    .append(". ")
                    .append(getString(item.kind() == MediaKind.IMAGE
                            ? R.string.media_kind_image_short
                            : R.string.media_kind_video_short))
                    .append(' ')
                    .append(item.displayName());
        }
        return value.toString();
    }

    private String formatTime(int minuteOfDay) {
        return String.format(
                Locale.getDefault(),
                "%02d:%02d",
                minuteOfDay / 60,
                minuteOfDay % 60
        );
    }
}
