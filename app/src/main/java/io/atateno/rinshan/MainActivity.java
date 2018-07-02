package io.atateno.rinshan;

import android.app.AlertDialog;
import android.content.Context;
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
    class FailedToCommitPreferences extends RuntimeException {}

    private KyokuViewModel kyokuViewModel;

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

    private void reset() {
        boolean success = getPreferences(Context.MODE_PRIVATE)
                .edit()
                .remove("kyokuViewModelState")
                .commit();
        if (!success) {
            throw new FailedToCommitPreferences();
        }
        kyokuViewModel.reset();
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
        if (isFinishing()) {
            reset();
        } else {
            kyokuViewModel.pause();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MediaPlayer mp = MediaPlayer.create(this, R.raw.tick);

        kyokuViewModel = ViewModelProviders.of(this).get(KyokuViewModel.class);
        kyokuViewModel.init();
        kyokuViewModel.setBaseTime(5);
        kyokuViewModel.setExtraTime(15);
        kyokuViewModel.setOnTick(() -> {
            mp.seekTo(0);
            mp.start();
        });
        kyokuViewModel.setOnPause(marshalled -> {
            boolean success = getPreferences(Context.MODE_PRIVATE)
                    .edit()
                    .putString("kyokuViewModelState", marshalled)
                    .commit();
            if (!success) {
                throw new FailedToCommitPreferences();
            }
        });

        Button buttonStart = findViewById(R.id.buttonStart);
        Button buttonRelEast = findViewById(R.id.buttonRelEast);
        Button buttonRelSouth = findViewById(R.id.buttonRelSouth);
        Button buttonRelWest = findViewById(R.id.buttonRelWest);
        Button buttonRelNorth = findViewById(R.id.buttonRelNorth);
        TextView textViewDisplayTime = findViewById(R.id.textViewDisplayTime);
        ImageButton imageButtonMenu = findViewById(R.id.imageButtonMenu);

        kyokuViewModel.getState().observe(this, state -> {
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
                            kyokuViewModel.pause();
                            break;
                        case 1:
                            reset();
                            break;
                        case 2:
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
                            break;
                        case 1:
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

        String marshalled = getPreferences(Context.MODE_PRIVATE).getString("kyokuViewModelState", null);
        if (marshalled != null) {
            try {
                kyokuViewModel.unmarshall(marshalled);
            } catch (KyokuViewModel.UnmarshallException e) {
                reset();
            }
        } else {
            reset();
        }
    }
}
