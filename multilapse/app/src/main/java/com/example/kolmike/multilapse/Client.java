package com.example.kolmike.multilapse;

import android.util.Log;

public class Client {
    private static final String TAG = "Host";

    public Client() {
        Log.d(TAG, "Started Client");
    }

    public void stop() {
        Log.d(TAG, "Stopped Client");
    }
}
