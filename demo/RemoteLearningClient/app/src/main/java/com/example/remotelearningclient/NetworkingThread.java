package com.example.remotelearningclient;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

public class NetworkingThread extends HandlerThread {
    public Handler handler;
    public static boolean isConnected = false;
    private static Socket client = null;
    private OutputStream out;
    private DataOutputStream dOut;
    private InputStream input;
    private DataInputStream dIn;

    public NetworkingThread(String name) {
        super(name);
        isConnected = false;
    }

    private void connectToServer() {
        try {
            Log.e(Config.GLOBAL_TAG, "TCP thread: connecting to server");
            client = new Socket(Config.SERVER_IP, Config.FUNC_PORT);
            client.setTcpNoDelay(true);

            out = client.getOutputStream();
            dOut = new DataOutputStream(out);
            input = client.getInputStream();
            dIn = new DataInputStream(input);

            if (client.isConnected()) {
                isConnected = true;
            }

            int n = 10;
            for(int i=0;i<n;i++){
                dIn.readInt();
                dOut.writeInt(1);
            }

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }

        Log.e(Config.GLOBAL_TAG, "Functional TCP thread: server connected, start waiting for msg.");
    }

    public void start() {
        super.start();

        handler = new Handler(getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == Config.CONNECT) {
                    if (!isConnected)
                        connectToServer();

                } else if (msg.what == Config.SEND_ACK) {
                    Bundle bundle = msg.getData();
                    String poseACK = bundle.getString(Config.MSG_KEY);
                    try {
                        //byte[] ackMsg =  poseACK.getBytes();
                        dOut.writeUTF(poseACK);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                } else {
                    try {
                        Log.d(Config.GLOBAL_TAG, "Functional TCP thread: closing TCP connection");
                        out.close();
                        dOut.close();
                        client.close();
                    } catch (Exception e) {
                        //
                    }
                }
            }
        };
    }

}
