package com.example.kolmike.multilapse;

import android.util.Log;

//import java.net.ServerSocket;

public class Host {

    private static final String TAG = "Host";
    //ServerSocket socket;

    public Host() {
        Log.d(TAG, "Started Host");
    }

    public void stop() {
        Log.d(TAG, "Stopped Host");
    }
/*
    public void initializeServerSocket() {
        // Initialize a server socket on the next available port.
        mServerSocket = new ServerSocket(0);

        // Store the chosen port.
        mLocalPort =  mServerSocket.getLocalPort();
        ...
    }

    public void registerService(int port) {
        NsdServiceInfo serviceInfo  = new NsdServiceInfo();
        serviceInfo.setServiceName("multilapse");
        serviceInfo.setServiceType("_http._tcp.");
        serviceInfo.setPort(port);
    }*/
}
