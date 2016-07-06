package com.example.kolmike.multilapse;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Switch;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MyActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "Hello world");
    }

    public void onHostSwitched(View view) {
        Switch s = (Switch) findViewById(R.id.host_switch);
        Log.d(TAG, "switched to " + s.isChecked());
    }
}
