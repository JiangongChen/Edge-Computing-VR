package com.example.remotelearningclient;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;

public class MovementThread extends Thread{
    final int SERVERPORT;
    final String SERVER_IP;

    int initX = 0;
    int initY = 0;
    double posX = 0.0;
    double posY = 0.0;
    String pos = null;
    String moveDir = "right";
    //boolean moveDir = true;
    int moveThre = 20;
    int stepX = 1;
    int stepY = 1;
    int stepCnt = 0;

    MovementThread(String ip, int port){
        SERVER_IP = ip;
        SERVERPORT = port;
    }

    @Override
    public void run() {
        Socket socket = null;

        try {
            InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
            socket = new Socket(serverAddr, SERVERPORT);

            socket.setTcpNoDelay(true);

            OutputStream out = socket.getOutputStream();
            PrintWriter output = new PrintWriter(out);
            DataOutputStream dOut = new DataOutputStream(out);

            while(socket != null&&Config.NETWORK) {
                Instant start = Instant.now();

                /*
                if (moveDir) {
                    posX += stepX;
                } else {
                    posY += stepY;
                }
                if (posX > moveThre) {
                    stepX = -1;
                }
                if (posY > moveThre) {
                    stepY = -1;
                }
                if (posX < -moveThre) {
                    stepX = 1;
                }
                if (posY < -moveThre) {
                    stepY = 1;
                }
                stepCnt++;
                if (stepCnt >= moveThre / 2) {
                    stepCnt = 0;
                    moveDir = !moveDir;
                }*/
                switch (moveDir){
                    case "forward" :{
                        posX += Config.moveStep;
                        if(posX>moveThre) moveDir = "right";
                        break;
                    }
                    case "right" :{
                        posY += Config.moveStep;
                        if(posY>moveThre) moveDir = "backward";
                        break;
                    }
                    case "backward" :{
                        posX -= Config.moveStep;
                        if(posX<-moveThre) moveDir = "left";
                        break;
                    }
                    case "left" :{
                        posY -= Config.moveStep;
                        if(posY<-moveThre) moveDir = "forward";
                        break;
                    }
                    default:
                        posX = 0;
                        posY = 0;
                }
                pos = String.format("%5.1f", posX) + "," + String.format("%5.1f", posY);
                //pos = String.format("%5d", (int)posX) + "," + String.format("%5d", (int)posY);
                //System.out.println("moveDir: "+moveDir+"pos: "+pos);
                output.print(pos);
                output.flush();
                //dOut.writeInt(posX);
                //dOut.writeInt(posY);
                Instant end = Instant.now();
                float timeEclipsed = Duration.between(start,end).toMillis();
                //System.out.println(timeEclipsed);
                if (timeEclipsed < 10) {
                    Thread.sleep(10-(long)timeEclipsed);
                }
            }
            socket.close();
            System.out.println("Movement thread has been closed.");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
