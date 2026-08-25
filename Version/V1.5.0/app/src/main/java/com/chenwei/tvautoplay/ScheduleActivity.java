package com.chenwei.tvautoplay;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import java.util.List;
import java.util.Locale;

/** TV-friendly overview for daily scheduled playlists. */
public final class ScheduleActivity extends ComponentActivity {
    private PlaybackConfigStore configStore;
    private CheckBox scheduleEnabled;
    private TextView emptyMessage;
    private LinearLayout scheduleContainer;
    private Button clearAllButton;
    private ActivityResultLauncher<Intent> editorLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        editorLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> refreshSchedules()
        );
        setContentView(R.layout.activity_schedule);
        configStore = new PlaybackConfigStore(this);

        scheduleEnabled = findViewById(R.id.schedule_enabled_checkbox);
        emptyMessage = findViewById(R.id.schedule_empty_message);
        scheduleContainer = findViewById(R.id.schedule_container);
        clearAllButton = findViewById(R.id.clear_all_schedules_button);

        scheduleEnabled.setOnClickListener(view -> {
            configStore.setScheduleEnabled(scheduleEnabled.isChecked());
            refreshSchedules();
        });
        findViewById(R.id.add_schedule_button).setOnClickListener(view -> openEditor(null));
        clearAllButton.setOnClickListener(view -> confirmClearAll());
        findViewById(R.id.back_to_settings_button).setOnClickListener(view -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshSchedules();
    }

    private void refreshSchedules() {
        PlaybackConfiguration configuration = configStore.load();
        List<ScheduledPlaylist> schedules = configuration.scheduledPlaylists();
        scheduleEnabled.setChecked(configuration.scheduleEnabled());
        scheduleContainer.removeAllViews();
        emptyMessage.setVisibility(schedules.isEmpty() ? View.VISIBLE : View.GONE);
        clearAllButton.setVisibility(schedules.isEmpty() ? View.GONE : View.VISIBLE);

        for (ScheduledPlaylist schedule : schedules) {
            Button button = (Button) getLayoutInflater().inflate(
                    R.layout.item_schedule_button,
                    scheduleContainer,
                    false
            );
            ScheduleRule rule = schedule.rule();
            button.setText(getString(
                    R.string.schedule_item_summary,
                    formatTime(rule.startMinute()),
                    formatTime(rule.endMinute()),
                    getResources().getQuantityString(
                            R.plurals.schedule_media_count,
                            schedule.playlist().size(),
                            schedule.playlist().size()
                    ),
                    getString(rule.isEnabled()
                            ? R.string.schedule_rule_enabled
                            : R.string.schedule_rule_disabled)
            ));
            button.setOnClickListener(view -> openEditor(rule.id()));
            scheduleContainer.addView(button);
        }
    }

    private void openEditor(String ruleId) {
        Intent intent = new Intent(this, ScheduleEditorActivity.class);
        if (ruleId != null) {
            intent.putExtra(ScheduleEditorActivity.EXTRA_RULE_ID, ruleId);
        }
        editorLauncher.launch(intent);
    }

    private void confirmClearAll() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.clear_all_schedules_title)
                .setMessage(R.string.clear_all_schedules_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.clear, (dialog, which) -> {
                    configStore.saveScheduledPlaylists(java.util.Collections.emptyList());
                    configStore.setScheduleEnabled(false);
                    refreshSchedules();
                })
                .show();
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
