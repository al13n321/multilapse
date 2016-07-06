package com.example.kolmike.multilapse;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import java.net.Socket;

public class Client {
    private static final String TAG = "Client";

    class NetThread extends Thread {
        Client client;
        Socket socket;

        NetThread(Client client_) {
            client = client_;
        }

        void interruptIt() {

        }

        public void run() {
            while (!Thread.interrupted()) {

            }
        }
    }

    class NsdDiscoverer {
        Client client;
        NsdManager mgr;
        NsdManager.DiscoveryListener discoveryListener;

        public NsdDiscoverer(Client client_) {
            client = client_;
            mgr = (NsdManager) client.context.getSystemService(Context.NSD_SERVICE);

            discoveryListener = new NsdManager.DiscoveryListener() {
                @Override
                public void onDiscoveryStarted(String regType) {
                    Log.d(TAG, "Service discovery started");
                }

                @Override
                public void onServiceFound(NsdServiceInfo service) {
                    Log.d(TAG, "Found service " + service);
                    if (!service.getServiceType().equals("_http._tcp.")) {
                        Log.d(TAG, "Found uninteresting service " + service);
                    } else if (service.getServiceName().startsWith("multilapse @")) {
                        Log.d(TAG, "Found multilapse service: " + service.getServiceName());
                    } else if (service.getServiceName().contains("NsdChat")){
                        NsdManager.ResolveListener resolveListener =
                                new NsdManager.ResolveListener() {
                            @Override
                            public void onServiceResolved (NsdServiceInfo serviceInfo) {
                                Log.d(TAG, "Resolved service " + serviceInfo);
                            }

                            @Override
                            public void onResolveFailed (NsdServiceInfo serviceInfo,
                                                         int errorCode) {
                                Log.d(TAG, "Failed to resolve service " + serviceInfo +
                                        "; error: " + errorCode);
                            }
                        };
                        mgr.resolveService(service, resolveListener);
                    }
                }

                @Override
                public void onServiceLost(NsdServiceInfo service) {
                    Log.d(TAG, "Lost service " + service);
                }

                @Override
                public void onDiscoveryStopped(String serviceType) {
                    Log.d(TAG, "Discovery stopped: " + serviceType);
                }

                @Override
                public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                    Log.d(TAG, "Discovery failed, error code: " + errorCode);
                }

                @Override
                public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                    Log.d(TAG, "Stop discovery failed, error code: " + errorCode);
                }
            };

            mgr.discoverServices(
                    "_http._tcp.", NsdManager.PROTOCOL_DNS_SD, discoveryListener);
        }

        public void stop() {
            mgr.stopServiceDiscovery(discoveryListener);
        }
    }

    Context context;
    NsdDiscoverer discoverer;

    public Client(Context context_) {
        Log.d(TAG, "Starting Client");

        context = context_;
        discoverer = new NsdDiscoverer(this);
    }

    public void stop() {
        discoverer.stop();
        discoverer = null;

        Log.d(TAG, "Stopped Client");
    }
}
