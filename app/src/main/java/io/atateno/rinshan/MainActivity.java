package io.atateno.rinshan;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.arch.lifecycle.ViewModelProviders;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    class FailedToCommitPreferences extends Exception {};

    private KyokuViewModel kyokuViewModel;

    private final KyokuViewModel.Savable savable = new KyokuViewModel.Savable() {
        @Override
        public void save(
                long extraTime,
                long baseTime,
                long eastTime,
                long southTime,
                long westTime,
                long northTime,
                long lastDiscardAt,
                long pausedAt,
                long totalTime,
                long time,
                KyokuViewModel.States resumeState) throws Exception {
            Log.d("", "saving");
            SharedPreferences.Editor editor = getPreferences(Context.MODE_PRIVATE).edit()
                    .putBoolean("paused", true)
                    .putLong("extraTime", extraTime)
                    .putLong("baseTime", baseTime)
                    .putLong("eastTime", eastTime)
                    .putLong("southTime", southTime)
                    .putLong("westTime", westTime)
                    .putLong("northTime", northTime)
                    .putLong("lastDiscardAt", lastDiscardAt)
                    .putLong("pausedAt", pausedAt)
                    .putLong("totalTime", totalTime)
                    .putLong("time", time)
                    .putString("resumeState", resumeState.name());
            if(!editor.commit()) {
                throw new FailedToCommitPreferences();
            }
        }
    };

    private void setImmersiveLandscapeUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        setImmersiveLandscapeUI();
    }

    @Override
    public void onResume() {
        super.onResume();
        setImmersiveLandscapeUI();
    }

    @Override
    public void onPause() {
        super.onPause();
        kyokuViewModel.pause(savable);
    }

    private void reset() {
        if (getPreferences(Context.MODE_PRIVATE).edit().clear().commit()) {
            Log.d("", String.format("reset preferences: %s", getPreferences(Context.MODE_PRIVATE).getBoolean("paused", true)));
        } else {
            Log.d("", "failed to reset preferences");
        }
        kyokuViewModel.init(getTick());
    }

    private Runnable tick;

    private Runnable getTick() {
        MediaPlayer mp = MediaPlayer.create(this, R.raw.tick);
        if (tick == null) {
            tick = () -> {
                mp.seekTo(0);
                mp.start();
            };
        }
        return tick;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button buttonStart = findViewById(R.id.buttonStart);
        Button buttonRelEast = findViewById(R.id.buttonRelEast);
        Button buttonRelSouth = findViewById(R.id.buttonRelSouth);
        Button buttonRelWest = findViewById(R.id.buttonRelWest);
        Button buttonRelNorth = findViewById(R.id.buttonRelNorth);
        TextView textViewDisplayTime = findViewById(R.id.textViewDisplayTime);
        ImageButton imageButtonMenu = findViewById(R.id.imageButtonMenu);

        kyokuViewModel = ViewModelProviders.of(this).get(KyokuViewModel.class);
        kyokuViewModel.init(getTick());

        SharedPreferences preferences = getPreferences(Context.MODE_PRIVATE);
        if (preferences.getBoolean("paused", false)) {
            Log.d("", "loading from preferences");
            kyokuViewModel.load(
                    preferences.getLong("extraTime", 0),
                    preferences.getLong("baseTime", 0),
                    preferences.getLong("eastTime", 0),
                    preferences.getLong("southTime", 0),
                    preferences.getLong("westTime", 0),
                    preferences.getLong("northTime", 0),
                    preferences.getLong("lastDiscardAt", 0),
                    preferences.getLong("pausedAt", 0),
                    preferences.getLong("totalTime", 0),
                    preferences.getLong("time", 0),
                    KyokuViewModel.States.valueOf(preferences.getString("resumeState", "")),
                    getTick());
        }

        kyokuViewModel.getState().observe(this, state -> {
            Log.d("", String.format("observed state: %s", state.name()));
            switch (state) {
                case WAITING_FOR_START:
                    buttonStart.setText(R.string.start);
                    break;
                case WAITING_FOR_RESUME:
                    buttonStart.setText(R.string.resume);
                    break;
            }

            if (state == KyokuViewModel.States.WAITING_FOR_START ||
                    state == KyokuViewModel.States.WAITING_FOR_RESUME) {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                buttonStart.setVisibility(View.VISIBLE);
                buttonRelEast.setEnabled(false);
                buttonRelSouth.setEnabled(false);
                buttonRelWest.setEnabled(false);
                buttonRelNorth.setEnabled(false);
            } else {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                buttonStart.setVisibility(View.GONE);
                buttonRelEast.setEnabled(true);
                buttonRelSouth.setEnabled(true);
                buttonRelWest.setEnabled(true);
                buttonRelNorth.setEnabled(true);
            }

            if (state == KyokuViewModel.States.WAITING_FOR_EAST) {
                buttonRelEast.setBackgroundResource(R.color.colorAccent);
            } else {
                buttonRelEast.setBackgroundResource(R.color.colorPrimaryDark);
            }

            if (state == KyokuViewModel.States.WAITING_FOR_SOUTH) {
                buttonRelSouth.setBackgroundResource(R.color.colorAccent);
            } else {
                buttonRelSouth.setBackgroundResource(R.color.colorPrimaryDark);
            }

            if (state == KyokuViewModel.States.WAITING_FOR_WEST) {
                buttonRelWest.setBackgroundResource(R.color.colorAccent);
            } else {
                buttonRelWest.setBackgroundResource(R.color.colorPrimaryDark);
            }

            if (state == KyokuViewModel.States.WAITING_FOR_NORTH) {
                buttonRelNorth.setBackgroundResource(R.color.colorAccent);
            } else {
                buttonRelNorth.setBackgroundResource(R.color.colorPrimaryDark);
            }
        });

        kyokuViewModel.getDisplay().observe(this, display -> {
            KyokuViewModel.States state = display.first;
            Integer time = display.second;

            if (time == null ||
                    state == KyokuViewModel.States.WAITING_FOR_RESUME ||
                    state == KyokuViewModel.States.WAITING_FOR_START) {
                textViewDisplayTime.setVisibility(View.GONE);
            } else {
                int colorId;
                if (time < 5) {
                    colorId = R.color.colorRed;
                } else if (time < 10) {
                    colorId = R.color.colorYellow;
                } else {
                    colorId = R.color.colorWhite;
                }
                textViewDisplayTime.setTextColor(getResources().getColor(colorId));
                textViewDisplayTime.setText(time.toString());
                textViewDisplayTime.setVisibility(View.VISIBLE);
            }

            switch (state) {
                case WAITING_FOR_EAST:
                    textViewDisplayTime.setRotation(0.0f);
                    break;
                case WAITING_FOR_SOUTH:
                    textViewDisplayTime.setRotation(270.0f);
                    break;
                case WAITING_FOR_WEST:
                    textViewDisplayTime.setRotation(180.0f);
                    break;
                case WAITING_FOR_NORTH:
                    textViewDisplayTime.setRotation(90.0f);
                    break;
            }
        });

        buttonStart.setOnClickListener(view -> {
            KyokuViewModel.States state = kyokuViewModel.getState().getValue();
            if (state == KyokuViewModel.States.WAITING_FOR_START) {
                kyokuViewModel.start();
            } else if (state == KyokuViewModel.States.WAITING_FOR_RESUME) {
                if(getPreferences(Context.MODE_PRIVATE).edit().putBoolean("paused", false).commit()) {
                    Log.d("", String.format("marked as unpaused: %s", getPreferences(Context.MODE_PRIVATE).getBoolean("paused", true)));
                }
                kyokuViewModel.resume();
            }
        });

        buttonRelEast.setOnClickListener(view -> {
            kyokuViewModel.eastDiscard();
        });

        buttonRelSouth.setOnClickListener(view -> {
            kyokuViewModel.southDiscard();
        });

        buttonRelWest.setOnClickListener(view -> {
            kyokuViewModel.westDiscard();
        });

        buttonRelNorth.setOnClickListener(view -> {
            kyokuViewModel.northDiscard();
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setItems(R.array.menu, (dialogInterface, which) -> {
                    switch (which) {
                        case 0:
                            kyokuViewModel.pause(savable);
                            break;
                        case 1:
                            reset();
                            break;
                        case 2:
                            reset();
                            finish();
                            break;
                    }
                })
                .create();

        AlertDialog pauseDialog = new AlertDialog.Builder(this)
                .setItems(R.array.pause_menu, (dialogInterface, which) -> {
                    switch (which) {
                        case 0:
                            reset();
                            kyokuViewModel.init(getTick());
                            break;
                        case 1:
                            reset();
                            finish();
                            break;
                    }
                })
                .create();

        imageButtonMenu.setOnClickListener(view -> {
            KyokuViewModel.States state = kyokuViewModel.getState().getValue();
            if (state == KyokuViewModel.States.WAITING_FOR_START ||
                    state == KyokuViewModel.States.WAITING_FOR_RESUME) {
                pauseDialog.show();
            } else {
                dialog.show();
            }
        });
    }
}
