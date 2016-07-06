package com.example.kolmike.multilapse;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import android.content.Intent;
import android.widget.FrameLayout;
import android.widget.ToggleButton;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MyActivity";
    Host host;
    Client client;
    CameraHelper camera;
    private CameraPreview mPreview;
    private RelativeLayout mLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        camera = new CameraHelper(this);
        update();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (((ToggleButton) findViewById(R.id.preview_switch)).isChecked()) {
            camera.showPreview();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        camera.turnOffPreview();
    }

    public void onTakeSnapshot(View view) {
        Log.d(TAG, "takePicture()");
        //camera.takePicture();
        Log.d(TAG, "/takePicture()");
    }

    void update() {
        Switch s = (Switch) findViewById(R.id.host_switch);
        if (s.isChecked()) {
            if (host != null) {
                return;
            }
            if (client != null) {
                client.stop();
                client = null;
            }
            host = new Host(this);
        } else {
            if (client != null) {
                return;
            }
            if (host != null) {
                host.stop();
                host = null;
            }
            client = new Client(this);
        }
    }

    public void onSwitchPreviewClick(View view) {
        ToggleButton tb = (ToggleButton) findViewById(R.id.preview_switch);
        if (tb.isChecked()) {
            Log.d(TAG, "Switched to ON");
            camera.showPreview();
        } else {
            Log.d(TAG, "Switched to OFF");
            camera.turnOffPreview();
        }

    }

    public void onHostSwitched(View view) {
        update();
    }
}
