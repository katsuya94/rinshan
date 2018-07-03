package io.atateno.rinshan;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.EditText;

public class SetupActivity extends AppCompatActivity {
    @Override
    protected void onResume() {
        super.onResume();
        EditText editTextBaseTime = findViewById(R.id.editTextBaseTime);
        EditText editTextExtraTime = findViewById(R.id.editTextExtraTime);
        SharedPreferences preferences = getPreferences(Context.MODE_PRIVATE);
        editTextBaseTime.setText(Integer.toString(preferences.getInt("baseTime", 15)));
        editTextExtraTime.setText(Integer.toString(preferences.getInt("extraTime", 45)));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            EditText editTextBaseTime = findViewById(R.id.editTextBaseTime);
            EditText editTextExtraTime = findViewById(R.id.editTextExtraTime);
            int baseTime = Integer.parseInt(editTextBaseTime.getText().toString());
            int extraTime = Integer.parseInt(editTextExtraTime.getText().toString());
            getPreferences(Context.MODE_PRIVATE)
                    .edit()
                    .putInt("baseTime", baseTime)
                    .putInt("extraTime", extraTime)
                    .apply();
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(MainActivity.BASE_TIME_MESSAGE, baseTime);
            intent.putExtra(MainActivity.EXTRA_TIME_MESSAGE, extraTime);
            startActivity(intent);
        });
    }
}
