package com.example.kolmike.server;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    static final String TAG = "Server";

    NsdManager mgr;
    NsdManager.RegistrationListener listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listener = new NsdManager.RegistrationListener() {
            @Override
            public void onServiceRegistered(NsdServiceInfo info) {
                Log.d(TAG, "Registered " + info.getServiceName());
            }

            @Override
            public void onRegistrationFailed(NsdServiceInfo info, int errorCode) {
                Log.e(TAG, "Registration failed");
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo info) {
                Log.d(TAG, "Unregistered " + info.getServiceName());
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.e(TAG, "Unregistration failed");
            }
        };

        NsdServiceInfo serviceInfo  = new NsdServiceInfo();

        serviceInfo.setServiceName("NsdChat");
        serviceInfo.setServiceType("_http._tcp.");
        serviceInfo.setPort(42);

        mgr = (NsdManager) getSystemService(Context.NSD_SERVICE);
        mgr.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, listener);
    }
}
