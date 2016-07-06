package com.example.kolmike.multilapse;

import android.app.AlertDialog;
import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

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
            serviceInfo.setServiceType("_multilapse._tcp.");
            serviceInfo.setPort(port);
            mgr = (NsdManager) host.context.getSystemService(Context.NSD_SERVICE);
            mgr.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, regListener);
        }

        void unregister() {
            mgr.unregisterService(regListener);
        }
    }

    interface SnapCallback {
        void onPictureTaken(long connectTime, byte[] data);
    }

    class NetThread extends Thread {
        ListenerThread listener;
        Socket socket;
        BlockingQueue<SnapCallback> queue;
        long connectTime;

        public NetThread(ListenerThread listener_, Socket socket_) {
            connectTime = System.currentTimeMillis();
            listener = listener_;
            socket = socket_;
            queue = new LinkedBlockingQueue<>();
        }

        class ProtocolException extends Exception {}

        @Override
        public void run() {
            DataInputStream in;
            DataOutputStream out;
            Socket sock;
            synchronized (this) {
                sock = socket;
            }
            if (sock == null) {
                return;
            }
            try {
                in = new DataInputStream(sock.getInputStream());
                out = new DataOutputStream(sock.getOutputStream());
            } catch (IOException e) {
                Log.d(TAG, "Failed to create data streams: " + e);
                destroySelf();
                return;
            }
            try {
                while (!Thread.interrupted()) {
                    SnapCallback cb = queue.take();
                    out.writeChar('s');
                    out.writeInt(0);
                    out.flush();
                    char type = in.readChar();
                    if (type != 'p') {
                        throw new ProtocolException();
                    }
                    int sz = in.readInt();
                    byte[] data = new byte[sz];
                    in.readFully(data);
                    cb.onPictureTaken(connectTime, data);
                }
            }
            catch (InterruptedException e) {}
            catch (IOException e) {
                Log.d(TAG, "IO error: " + e);
            }
            catch (ProtocolException e) {
                Log.d(TAG, "Protocol error: " + e);
            }
            destroySelf();
        }

        public void snap(SnapCallback cb) {
            queue.offer(cb);
        }

        void destroySelf() {
            interrupt();
            Socket sock;
            synchronized (this) {
                if (socket == null) {
                    return;
                }
                sock = socket;
                socket = null;
                try {
                    sock.close();
                } catch (IOException e) {
                    Log.d(TAG, "Failed to close socket: " + e);
                }
            }
            synchronized (listener) {
                listener.connections.remove(sock);
            }
        }

        public void interruptIt() {
            interrupt();
            synchronized (this) {
                if (socket == null) {
                    return;
                }
                try {
                    socket.close();
                } catch (IOException e) {
                    Log.d(TAG, "Failed to close socket: " + e);
                }
                socket = null;
            }
        }
    }

    interface HypersnapCallback {
        void onPicturesTaken(ArrayList<byte[]> data);
    }

    static class TestHypersnapCallback implements HypersnapCallback {
        public TestHypersnapCallback() {}

        public void onPicturesTaken(ArrayList<byte[]> data) {
            Log.d(TAG, "Hypersnapped " + data.size() + " pictures!" +
                    (data.size() > 1 ? " Wooo!" : ""));
        }
    }

    class ListenerThread extends Thread {
        Host host;
        ServerSocket serverSocket;
        NsdRegistration nsd;
        public Map<Socket, NetThread> connections;

        public ListenerThread(Host host_) {
            host = host_;
            connections = new HashMap<Socket, NetThread>();
        }

        public void interruptIt() {
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
                for (Map.Entry<Socket, NetThread> entry : connections.entrySet()) {
                    entry.getValue().interruptIt();
                }
            }
        }

        class Frame implements Comparable<Frame> {
            public long connectTime;
            public byte[] data;

            public Frame(long connectTime_, byte[] data_) {
                connectTime = connectTime_;
                data = data_;
            }

            public int compareTo(Frame f) {
                if (connectTime < f.connectTime) {
                    return -1;
                } else if (connectTime > f.connectTime) {
                    return 1;
                } else {
                    return 0;
                }
            }
        }

        class HypersnapThread extends Thread implements SnapCallback {
            int count;
            HypersnapCallback cb;
            Semaphore sem;
            ArrayList<Frame> pics;

            public HypersnapThread(Collection<NetThread> conns, HypersnapCallback cb_) {
                count = conns.size();
                cb = cb_;
                sem = new Semaphore(0);
                pics = new ArrayList<>();
                for (NetThread conn : conns) {
                    conn.snap(this);
                }
            }

            public void onPictureTaken(long connectTime, byte[] data) {
                synchronized (this) {
                    pics.add(new Frame(connectTime, data));
                }
                sem.release();
            }

            @Override
            public void run() {
                try {
                    for (int i = 0; i < count; ++i) {
                        sem.acquire();
                    }
                    Collections.sort(pics);
                    ArrayList<byte[]> ims = new ArrayList<>();
                    for (Frame f : pics) {
                        ims.add(f.data);
                    }
                    cb.onPicturesTaken(ims);
                } catch (InterruptedException e) {
                    Log.e(TAG, "HypersnapThread interrupted: " + e);
                }
            }
        }

        public void hypersnap(HypersnapCallback cb) {
            Collection<NetThread> conns;
            synchronized (this) {
                conns = connections.values();
            }
            new HypersnapThread(conns, cb).start();
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
                    int port = sock.getLocalPort();
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
                    synchronized (this) {
                        NetThread t = new NetThread(this, socket);
                        t.start();
                        connections.put(socket, t);
                    }
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

    public Host(Context context_) {
        Log.d(TAG, "Starting Host");

        context = context_;
        listener = new ListenerThread(this);
        listener.start();
    }

    public void hypersnap(HypersnapCallback cb) {
        listener.hypersnap(cb);
    }

    public void stop() {
        listener.interruptIt();
        listener = null;
        Log.d(TAG, "Stopped Host");
    }
}
