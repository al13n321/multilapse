package com.example.kolmike.multilapse;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Switch;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MyActivity";
    Host host;
    Client client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        update();
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

    public void onHostSwitched(View view) {
        update();
    }
}
