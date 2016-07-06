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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

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

        class BlockingPhotoCallback implements CameraHelper.PhotoCallback {
            Semaphore sem;
            byte[] data;

            BlockingPhotoCallback(Semaphore sem_) {
                sem = sem_;
            }

            @Override
            public void onPictureTaken(Context context, byte[] data_) {
                data = data_;
                sem.release();
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
                        in = new DataInputStream(sock.getInputStream());
                        out = new DataOutputStream(sock.getOutputStream());
                    } catch (IOException e) {
                        Log.d(TAG, "Failed to connect to " + host + ":" + port + ", error: " + e);
                        try {
                            Thread.sleep(1000);
                            continue;
                        } catch (InterruptedException e1) {
                            break;
                        }
                    }
                    synchronized (this) {
                        socket = sock;
                    }
                    continue;
                }

                boolean disconnect = false;
                try {
                    char type = in.readChar();
                    int sz = in.readInt();
                    do {
                        if (type == 's') {
                            Semaphore sem = new Semaphore(0);
                            BlockingPhotoCallback cb = new BlockingPhotoCallback(sem);
                            camera.takePicture(cb);
                            Log.d(TAG, "Taking picture");
                            sem.acquire();
                            byte[] data = cb.data;
                            Log.d(TAG, "Took picture, " + data.length + " bytes");

                            out.writeChar('p');
                            out.writeInt(data.length);
                            out.write(data);
                            out.flush();
                        } else {
                            Log.d(TAG, "Unexpected message type: " + type + ", size: " + sz);
                            disconnect = true;
                        }
                    } while (false);
                    if (!disconnect) {
                        in.skipBytes(sz);
                    }
                } catch (IOException e) {
                    Log.d(TAG, "Failed socket IO: " + e);
                    disconnect = true;
                }
                catch (InterruptedException ei) {
                    break;
                }

                if (disconnect) {
                    try {
                        sock.close();
                    } catch (IOException e) {
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
        Map<String, NetThread> connections;

        public NsdDiscoverer(Client client_) {
            client = client_;
            mgr = (NsdManager) client.context.getSystemService(Context.NSD_SERVICE);
            connections = new HashMap<>();

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
                        NsdManager.ResolveListener resolveListener =
                                new NsdManager.ResolveListener() {
                            @Override
                            public void onServiceResolved (NsdServiceInfo serviceInfo) {
                                Log.d(TAG, "Resolved service " + serviceInfo);
                                NetThread t = new NetThread(
                                        client, serviceInfo.getHost(), serviceInfo.getPort());
                                t.start();
                                synchronized (this) {
                                    if (connections == null) {
                                        return;
                                    }
                                    connections.put(serviceInfo.getServiceName(), t);
                                }
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
                    NetThread c;
                    synchronized (this) {
                        if (connections == null) {
                            return;
                        }
                        c = connections.get(service.getServiceName());
                        if (c == null) {
                            return;
                        }
                        connections.remove(c);
                    }
                    c.interruptIt();
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
            synchronized (this) {
                for (Map.Entry<String, NetThread> c : connections.entrySet()) {
                    c.getValue().interruptIt();
                }
                connections.clear();
                connections = null;
            }
        }
    }

    Context context;
    CameraHelper camera;
    NsdDiscoverer discoverer;

    public Client(Context context_, CameraHelper camera_) {
        Log.d(TAG, "Starting Client");

        context = context_;
        camera = camera_;
        discoverer = new NsdDiscoverer(this);
    }

    public void stop() {
        discoverer.stop();
        discoverer = null;

        Log.d(TAG, "Stopped Client");
    }
}
