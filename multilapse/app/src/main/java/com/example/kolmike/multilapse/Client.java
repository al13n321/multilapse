package com.example.kolmike.multilapse;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public class Client {
    private static final String TAG = "Client";

    class NetThread extends Thread {
        Client client;
        InetAddress host;
        int port;
        Socket socket;

        NetThread(Client client_, InetAddress host_, int port_) {
            client = client_;
            host = host_;
            port = port_;
        }

        void interruptIt() {
            synchronized (this) {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        Log.d(TAG, "Failed to close Socket: " + e);
                    }
                    socket = null;
                }
                interrupt();
            }
        }

        public void run() {
            DataInputStream in = null;
            DataOutputStream out = null;

            while (!Thread.interrupted()) {
                Socket sock;
                synchronized (this) {
                    sock = socket;
                }
                if (sock == null) {
                    try {
                        sock = new Socket(host, port);
                    } catch (IOException e) {
                        Log.d(TAG, "Failed to connect to " + host + ":" + port + ", error: " + e);
                        try {
                            Thread.sleep(1000);
                            continue;
                        } catch (InterruptedException e1) {
                            break;
                        }
                    }
                    in = new DataInputStream(server.getInputStream());
                    out = new DataOutputStream(server.getOutputStream());
                    synchronized (this) {
                        socket = sock;
                    }
                    continue;
                }

                boolean disconnect = false;
                try {
                    char type = in.readChar();
                    int sz = in.readInt();
                    if (type == 's') {
                        
                    } else {
                        Log.d(TAG, "Unexpected message type: " + type + ", size: " + sz);
                        disconnect = true;
                    }
                    if (!disconnect) {
                        in.skipBytes(sz);
                    }
                } catch (IOException e) {
                    Log.d(TAG, "Failed socket IO: " + e);
                    disconnect = true;
                }

                if (disconnect) {
                    try {
                        sock.close();
                    } catch (IOException e1) {
                        Log.d(TAG, "Failed close socket: " + e);
                    }
                    synchronized (this) {
                        socket = null;
                    }
                    try {
                        Thread.sleep(1000);
                        continue;
                    } catch (InterruptedException e1) {
                        break;
                    }
                }
            }

            synchronized (this) {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        Log.d(TAG, "Failed to close socket: " + e);
                    }
                    socket = null;
                }
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
                    if (!service.getServiceType().equals("_multilapse._tcp.")) {
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
                    "_multilapse._tcp.", NsdManager.PROTOCOL_DNS_SD, discoveryListener);
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
