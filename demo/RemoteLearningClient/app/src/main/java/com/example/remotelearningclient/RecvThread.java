package com.example.remotelearningclient;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;

import static com.example.remotelearningclient.MainActivity.mediaCodec;
import static com.example.remotelearningclient.MainActivity.mediaCodecBufferInfo;
import static com.example.remotelearningclient.MainActivity.missCnt;

public class RecvThread extends Thread {

    final int SERVERPORT;
    final String SERVER_IP;
    boolean fw_flag = false;
    boolean record_flag = false;
    private TextView connText;
    private SurfaceView surfaceView;
    private String filename = "RTT50.csv";;
    private FileWriter fwt;
    private File rootDir;
    private BufferedOutputStream bos;
    private int framecnt = 0;
    private Instant last_time;
    private Instant last_frame_time;
    private Instant tran_beg;
    private Instant tran_end;
    float total_image_size = 0;
    float total_time = 0;
    byte[] sizeBuf;
    byte[] msgBytes;
    int videoID = 0;

    private ByteArrayOutputStream byteArrayOutputStream;


    RecvThread(TextView textview, SurfaceView surview, File root, String ip, int port) {
        this.connText = textview;
        this.surfaceView = surview;
        this.rootDir = root;
        this.SERVER_IP = ip;
        this.SERVERPORT = port;
        this.sizeBuf = new byte[8];
        this.byteArrayOutputStream = new ByteArrayOutputStream();
    }

    @Override
    public void run() {
        Socket socket = null;

        try {


            InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
            socket = new Socket(serverAddr, SERVERPORT);
            //connText.setText("Connection to server established: "+SERVER_IP);

            InputStream input = socket.getInputStream();
            DataInputStream dIn = new DataInputStream(input);
            //BufferedInputStream bIn = new BufferedInputStream(input,512*1024);
            OutputStream output = socket.getOutputStream();
            DataOutputStream dOut = new DataOutputStream(output);
            //socket.setReceiveBufferSize(512*1024);
            System.out.println("Recv buffer size: "+socket.getReceiveBufferSize());


            if (fw_flag) {
                //Creat a file to store the time data
                File file = new File(
                        rootDir,
                        filename);
                file.createNewFile();
                fwt = new FileWriter(file);

                last_time = Instant.now();
                last_frame_time = Instant.now();

            }

            if (record_flag){
                String record_filename = "out_android.mp4";
                File file = new File(
                        rootDir,
                        record_filename);
                file.createNewFile();
                bos = new BufferedOutputStream(new FileOutputStream(file));
            }

            long lastTime = 0;
            while(socket!= null&&Config.NETWORK){

                if (fw_flag) {
                    tran_beg = Instant.now();
                }

                // bIn
                /*bIn.read(sizeBuf,0,8);
                String strSize = new String(sizeBuf);
                int image_size = Integer.parseInt(strSize.trim());
                System.out.println("Image size: " + image_size);
                byte[] img_bytes = new byte[image_size];

                int recvSize = 0;
                while(recvSize<image_size) {
                    int number = bIn.read(img_bytes,recvSize,image_size-recvSize);
                    recvSize += number;
                    //System.out.println(recvSize);
                }*/

                // receive pos from server
                /*msgBytes = new byte[Config.msgSize];
                int length = 0;
                int recvSize = 0;
                // Receive displayed pose from the client
                while (recvSize < Config.msgSize)
                {
                    length = input.read(msgBytes, 0, Config.msgSize-recvSize);
                    if(length<0) break;
                    recvSize += length;
                }
                if(length<0) break;
                String msg = new String(msgBytes);*/
                String msg = dIn.readUTF();
                if (msg.contains("end")){
                    MainActivity.mOpenGLThread.handler.sendEmptyMessage(Config.MSG_REPORT_STATS);
                    break;
                }
                //System.out.println("recv meg:"+msg);
                // add the received pose to display buffer
                Utils.dispBuffer.add(msg);

                // when do not receive frame for a long time, send message to report statistics
                long curTime = System.currentTimeMillis();

                long timeDiff = curTime - lastTime;

                if (timeDiff > 30){
                    Log.e(Config.GLOBAL_TAG, "Long Transmission Delay: " + timeDiff);
                }
                lastTime = curTime;

                // dIn
                //int videoID = dIn.readInt();
                int image_size = dIn.readInt();

                if(image_size!=0) {
                    // each video id corresponds to a position
                    String pos = Utils.getPosFromMsg(msg);
                    // System.out.println("receive new pos: "+pos+" id: "+videoID);
                    Utils.pos2VideoID.put(pos,videoID);
                    byte[] img_bytes = new byte[image_size];
                    dIn.readFully(img_bytes, 0, img_bytes.length); // read the message

                    if (fw_flag) {
                        tran_end = Instant.now();
                    }

                    byteArrayOutputStream.write(img_bytes, 0, image_size);
                    byteArrayOutputStream.flush();
                    byte[] buf_rec = byteArrayOutputStream.toByteArray();

                    // cache received video
                    NetworkBufferPool.VideoBuffer vb = new NetworkBufferPool.VideoBuffer(videoID, buf_rec);
                    NetworkBufferPool.videoCache.put(videoID, vb);
                    NetworkBufferPool.videoReceived.add(videoID);
                    Utils.latestVideoId = videoID;
                    //System.out.println(NetworkBufferPool.videoCache.size());

                    // video cache limits reached, save some video into disk
                    if (NetworkBufferPool.videoCache.size() > Config.VIDEO_CACHE_LIMIT) {
                        NetworkBufferPool.releaseVideoCache();
                    }

                    byteArrayOutputStream.reset();
                    // Log.e(Config.GLOBAL_TAG,"receive video id: "+videoID);
                    videoID++;
                }

                // release the display buffer once it is full
                if((double)Utils.dispBuffer.size() > 0.8 * (double)Config.DISPLAY_CACHE_LIMIT){
                    int i;
                    for(i=0;i<(int)(0.4*(double)Config.DISPLAY_CACHE_LIMIT);i++){
                        // System.out.println("Release video msg: "+Utils.dispBuffer.get(0));
                        Utils.dispBuffer.remove(0);
                    }
                    Log.e(Config.GLOBAL_TAG, "Display CACHE RELEASED: " + i);
                }
                //dOut.writeInt(1);
/*
                int inputBufferIndex = mediaCodec.dequeueInputBuffer(0);
                // fill data to the input stream
                if (inputBufferIndex >= 0) {
                    ByteBuffer inputBuffer;
                    inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex);
                    inputBuffer.put(img_bytes, 0, img_bytes.length);
                    mediaCodec.queueInputBuffer(inputBufferIndex, 0, img_bytes.length, System.nanoTime(), 0);
                }*/

                //SystemClock.sleep(20);
                if (fw_flag) {
                    framecnt += 1;


                    Duration tranDelay = Duration.between(tran_beg, tran_end);
                    float image_size_kB = image_size / 1000;
                    total_image_size += image_size_kB;
                    total_time += tranDelay.toMillis();
                    Instant cur_time = Instant.now();
                    Duration timeElapsed = Duration.between(last_time, cur_time);
                    if (timeElapsed.toMillis() > 1000) {
                        connText.setText("FPS:" + framecnt);
                        System.out.println(framecnt);
                        float speed = total_image_size / total_time;
                        float throughput = total_image_size/timeElapsed.toMillis();
                        //Write the data to the file
                        String new_line = "";
                        //new_line += Float.toString(reqDelay.toMillis()) + ", ";
                        //new_line += Float.toString(tranDelay.toMillis()) + ", ";
                        new_line += total_image_size + ", ";
                        new_line += timeElapsed.toMillis() + ", ";
                        new_line += throughput + ", ";
                        new_line += total_time + ", ";
                        new_line += speed + ", ";
                        new_line += framecnt + ", ";
                        new_line += missCnt;
                        //new_line += Float.toString(tranmidDelay.toMillis()) + ", ";
                        //new_line += Float.toString(renDelay.toMillis()) + ", ";
                        //new_line += Float.toString(timeGap.toMillis()) + ", ";
                        new_line += "\n";

                        try {
                            fwt.write(new_line);
                            fwt.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        total_image_size = 0;
                        total_time = 0;
                        last_time = cur_time;
                        framecnt = 0;
                        missCnt = 0;
                    }

                }


            }


            socket.close();
            System.out.println("Receive thread has been closed.");
            //connText.setText("File write successfully! location:"+file.toString());
            //mediaCodec.stop();
            //mediaCodec.release();


        } catch (IOException e) {

            e.printStackTrace();



        } finally {
            if(socket != null){
                try {
                    socket.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

}
