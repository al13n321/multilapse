package com.example.kolmike.multilapse;

import android.app.AlertDialog;
import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Host {

    private static final String TAG = "Host";

    class NsdRegistration {
        Host host;
        NsdManager mgr;
        NsdManager.RegistrationListener regListener;

        NsdRegistration(Host host_, int port) {
            host = host_;
            regListener = new NsdManager.RegistrationListener() {
                @Override
                public void onServiceRegistered(NsdServiceInfo NsdServiceInfo) {
                    Log.d(TAG, "Nsd service registered");
                }

                @Override
                public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                    Log.d(TAG, "Failed to register service: " + errorCode);
                }

                @Override
                public void onServiceUnregistered(NsdServiceInfo arg0) {
                    Log.d(TAG, "Nsd service unregistered");
                }

                @Override
                public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                    Log.d(TAG, "Failed to unregister service: " + errorCode);
                }
            };

            NsdServiceInfo serviceInfo  = new NsdServiceInfo();
            serviceInfo.setServiceName("multilapse @" + System.currentTimeMillis());
            serviceInfo.setServiceType("_http._tcp.");
            serviceInfo.setPort(port);
            mgr = (NsdManager) host.context.getSystemService(Context.NSD_SERVICE);
            mgr.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, regListener);
        }

        void unregister() {
            mgr.unregisterService(regListener);
        }
    }

    class ListenerThread extends Thread {
        Host host;
        ServerSocket serverSocket;
        NsdRegistration nsd;

        public ListenerThread(Host host_) {
            host = host_;
        }

        public void interruptListener() {
            synchronized (this) {
                if (serverSocket != null) {
                    try {
                        serverSocket.close();
                    } catch (IOException e) {
                        Log.d(TAG, "Failed to close ServerSocket: " + e);
                    }
                    serverSocket = null;
                }
                interrupt();
            }
        }

        public void run() {
            while (!Thread.interrupted()) {
                ServerSocket sock;
                synchronized (this) {
                    sock = serverSocket;
                }
                if (sock == null) {
                    try {
                        sock = new ServerSocket(0);
                        sock.setSoTimeout(10000);
                    } catch (IOException e) {
                        Log.d(TAG, "Failed to create ServerSocket: " + e);
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e1) {
                            break;
                        }
                        continue;
                    }
                    int port = serverSocket.getLocalPort();
                    synchronized (this) {
                        serverSocket = sock;
                        nsd = new NsdRegistration(host, port);
                    }
                    continue;
                }

                try {
                    Log.d(TAG, "Listening");
                    Socket socket = sock.accept();
                    Log.d(TAG, "Got a connection");
                    DataOutputStream out =
                        new DataOutputStream(socket.getOutputStream());
                    out.writeUTF("Thank you for connecting to "
                        + socket.getLocalSocketAddress() + "\nGoodbye!");
                    socket.close();
                    Log.d(TAG, "Closed the connection");
                } catch (IOException e) {
                    Log.d(TAG, "Accept failed: " + e);
                }
            }

            synchronized (this) {
                if (nsd != null) {
                    nsd.unregister();
                    nsd = null;
                }
                if (serverSocket != null) {
                    try {
                        serverSocket.close();
                    } catch (IOException e) {
                        Log.d(TAG, "Failed to close socket");
                    }
                    serverSocket = null;
                }
            }
        }
    }

    Context context;
    ListenerThread listener;

    public Host() {
        Log.d(TAG, "Starting Host");

        listener = new ListenerThread(this);
        listener.start();
    }

    public void stop() {
        listener.interruptListener();
        Log.d(TAG, "Stopped Host");
    }
}
