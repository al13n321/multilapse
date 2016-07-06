package com.example.kolmike.multilapse;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MyActivity";
    Host host;
    Client client;
    CameraHelper camera;
    private CameraPreview mPreview;
    private RelativeLayout mLayout;

    private TimeBulletMaker builder_;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        camera = new CameraHelper(this);
        client = new Client(this, camera);
        updateHost();
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

        if (builder_ != null) {
            CameraHelper.PhotoCallback callback = new CameraHelper.PhotoCallback() {
                @Override
                public void onPictureTaken(Context context, byte[] data) {
                    Log.v(TAG, "Photo taken, adding to gif builder...");
                    builder_.add(data);
                }
            };
            camera.takePicture(callback);
        } else {
            camera.takePicture(new CameraHelper.SaveToFileCallback());
        }
        Log.d(TAG, "/takePicture()");
    }

    class HypersnapCallback implements Host.HypersnapCallback {
        public void onPicturesTaken(ArrayList<byte[]> data) {
            TimeBulletMaker builder = new TimeBulletMaker();
            for (byte[] pic : data) {
                builder.add(pic);
            }
            Log.d(TAG, "Building a gif from " + data.size() + " frames");
            String fname = "ml_" + Utility.getDate() + ".gif";
            builder.saveToFile(fname);
            Log.d(TAG, "Save gif to file " + fname);
        }
    }

    public void onHypersnap(View view) {
        if (host == null) {
            Button b = (Button) findViewById(R.id.hypersnap_button);
            b.setEnabled(false);
            return;
        }
        host.hypersnap(new HypersnapCallback());
    }

    void updateHost() {
        Switch s = (Switch) findViewById(R.id.host_switch);
        Button b = (Button) findViewById(R.id.hypersnap_button);
        b.setEnabled(s.isChecked());
        if (s.isChecked() == (host != null)) {
            return;
        }
        if (s.isChecked()) {
            host = new Host(this);
        } else {
            host.stop();
            host = null;
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

    public void onGifButtonClick(View view) {
        ToggleButton tb = (ToggleButton) findViewById(R.id.gif_button);
        if (tb.isChecked()) {
            Log.d(TAG, "Start building gif");
            builder_ = new TimeBulletMaker();
        } else {
            Log.d(TAG, "Stop building gif");
            String fname = "ml_" + Utility.getDate() + ".gif";
            builder_.saveToFile(fname);
            Log.d(TAG, "Save gif to file " + fname);
            Toast.makeText(getApplicationContext(), "New gif saved:" + fname,
                    Toast.LENGTH_LONG).show();
            builder_ = null;
        }

    }

    public void onHostSwitched(View view) {
        updateHost();
    }
}
