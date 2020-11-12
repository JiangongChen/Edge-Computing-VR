package com.example.testmediacodec;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.sql.Time;
import java.time.Duration;
import java.time.Instant;

public class MainActivity extends Activity {

    private SurfaceView surfaceView;
    private TextView connText;
    private Button buttonStart;
    MediaCodec mediaCodec;
    MediaCodec.BufferInfo mediaCodecBufferInfo;
    private static final int SERVERPORT = 8080;
    private static final String SERVER_IP = "192.168.1.108";
    private boolean fw_flag = false;
    private boolean record_flag = true;
    private String filename;
    private FileWriter fwt;
    private BufferedOutputStream bos;
    private int framecnt = 0;
    private Instant last_time;
    private Instant last_frame_time;
    private Instant tran_beg;
    private Instant tran_end;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connText = (TextView) findViewById(R.id.conn);
        surfaceView = (SurfaceView) findViewById(R.id.surface);
        buttonStart = (Button) findViewById(R.id.buttonStart);

        buttonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonStart.setVisibility(View.GONE);
                startPlayback();
            }
        });
    }

    public void startPlayback() {
        try {
            mediaCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            int width = 640;
            int height = 360;
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
            //mediaFormat.setByteBuffer("csd-0",ByteBuffer.wrap());
            //mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 100000);
            mediaCodec.configure(mediaFormat, surfaceView.getHolder().getSurface(), null, 0);
            mediaCodec.start();

            mediaCodecBufferInfo = new MediaCodec.BufferInfo();
/*
            ReceiveThread receiveThread =
                    new ReceiveThread();
            receiveThread.start();
*/

            ClientRxThread clientRxThread =
                    new ClientRxThread();
            clientRxThread.start();
/*
            DecodeThread decodeThread =
                    new DecodeThread();
            decodeThread.start();*/

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
        private class DecodeThread extends Thread{

            DecodeThread(){

            }

            @Override
            public void run() {
                SystemClock.sleep(2000);
                while(true) {
                    try {
                        int outputBufferIndex = mediaCodec.dequeueOutputBuffer(mediaCodecBufferInfo, 0);
                        if (outputBufferIndex >= 0) {
                            mediaCodec.releaseOutputBuffer(outputBufferIndex, true);
                        } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {

                        }
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    } finally {
                        mediaCodec.stop();
                        mediaCodecelease();
                        break;
                    }
                }
            }

        }
    /*
        private class ReceiveThread extends Thread {


            ReceiveThread() {

            }

            @Override
            public void run() {
                File file = new File(getExternalFilesDir(null), "output.mp4");
                try {
                    FileInputStream fp = new FileInputStream(file);
                    int length;
                    while(true) {
                        byte[] buf = new byte[1024];
                        length = fp.read(buf);
                        if (length>=0) {
                            try {
                                // set the waiting time for decoding, 0 refers to no waiting, -1 refers to always waiting, others are time units
                                int inputBufferIndex = mediaCodec.dequeueInputBuffer(0);
                                // fill data to the input stream
                                if (inputBufferIndex >= 0) {
                                    ByteBuffer inputBuffer;
                                    inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex);
                                    inputBuffer.put(buf, 0, buf.length);
                                    mediaCodec.queueInputBuffer(inputBufferIndex, 0, buf.length, System.nanoTime(), 0);
                                }


                            } catch (IllegalStateException e) {
                                e.printStackTrace();
                            } finally {
                                break;
                            }
                        }
                        else{
                            break;
                        }
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }


            }
        }
    */
    private class ClientRxThread extends Thread {
        ClientRxThread() {

        }
        @Override
        public void run() {
            Socket socket = null;
            try {
                InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
                socket = new Socket(serverAddr, SERVERPORT);
                connText.setText("Connection to server established: "+SERVER_IP);

                InputStream is = socket.getInputStream();

                OutputStream out = socket.getOutputStream();
                PrintWriter output = new PrintWriter(out);

                if (fw_flag) {
                    //Creat a file to store the time data
                    filename = "latency_data.csv";
                    File file = new File(
                            getExternalFilesDir(null),
                            filename);
                    file.createNewFile();
                    fwt = new FileWriter(file);

                    last_time = Instant.now();
                    last_frame_time = Instant.now();
                }

                if (record_flag){
                    filename = "out_android.mp4";
                    File file = new File(
                            getExternalFilesDir(null),
                            filename);
                    file.createNewFile();
                    bos = new BufferedOutputStream(new FileOutputStream(file));
                }
                int frameNum = 0;

                while(socket != null) {
                    output.print("Next frame");
                    output.flush();

                    byte[] bytes = new byte[10240];
                    byte[] size_byte = new byte[32];
                    int recv_size = 0;
                    int pkt_num = 0;

                    if (frameNum>295){
                        break;
                    }
                    frameNum++;

                    //connText.setText("Begin receive image size");
                    int bytesNum = is.read(size_byte, 0, size_byte.length);
                    String str_size = new String(size_byte).trim();
                    final int image_size = Integer.valueOf(str_size);
                    //connText.setText("Image size: " + image_size);

                    if (fw_flag) {
                        tran_beg = Instant.now();
                    }

                    byte[] img_bytes = new byte[image_size];
                    output.print("Got it.");
                    output.flush();
                    //connText.setText("Reply sent to server.");
                    while (recv_size < image_size) {
                        int bytesRead = is.read(bytes, 0, bytes.length);
                        if (recv_size + bytesRead < image_size) {
                            System.arraycopy(bytes, 0, img_bytes, recv_size, bytesRead);
                        } else {
                            System.arraycopy(bytes, 0, img_bytes, recv_size, image_size - recv_size);
                        }
                        recv_size += bytesRead;
                        pkt_num += 1;
                        //connText.setText("Received size: " + recv_size + "/" + image_size);

                    }

                    if (fw_flag) {
                        tran_end = Instant.now();
                    }

                    int inputBufferIndex = mediaCodec.dequeueInputBuffer(0);
                    // fill data to the input stream
                    if (inputBufferIndex >= 0) {
                        ByteBuffer inputBuffer;
                        inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex);
                        inputBuffer.put(img_bytes, 0, img_bytes.length);
                        mediaCodec.queueInputBuffer(inputBufferIndex, 0, img_bytes.length, System.nanoTime(), 0);
                    }
                    int outputBufferIndex = mediaCodec.dequeueOutputBuffer(mediaCodecBufferInfo, 0);
                    if (outputBufferIndex >= 0) {
                        mediaCodec.releaseOutputBuffer(outputBufferIndex, true);
                    }
                    SystemClock.sleep(20);
                    if (fw_flag) {
                        framecnt += 1;
                        Instant cur_time = Instant.now();
                        Duration timeElapsed = Duration.between(last_time, cur_time);
                        if (timeElapsed.toMillis() > 1000) {
                            connText.setText("FPS:" + framecnt);
                            last_time = cur_time;
                            framecnt = 0;
                        }
                        //Calculate the elapsed time
                        Duration timeGap = Duration.between(last_frame_time, cur_time);
                        //Duration reqDelay = Duration.between(tran_beg,last_frame_time);
                        Duration tranDelay = Duration.between(tran_beg, tran_end);
                        float image_size_kB = image_size / 1000;
                        float speed = image_size_kB / (tranDelay.toMillis());
                        //Duration tranmidDelay = Duration.between(tran_mid, tran_end);
                        //Duration renDelay = Duration.between(tran_end,cur_time);
                        last_frame_time = cur_time;
                        //Write the data to the file
                        String new_line = "";
                        //new_line += Float.toString(reqDelay.toMillis()) + ", ";
                        new_line += Float.toString(tranDelay.toMillis()) + ", ";
                        new_line += image_size_kB + ", ";
                        new_line += speed + ", ";
                        //new_line += Float.toString(tranmidDelay.toMillis()) + ", ";
                        //new_line += Float.toString(renDelay.toMillis()) + ", ";
                        new_line += Float.toString(timeGap.toMillis()) + ", ";
                        new_line += "\n";

                        try {
                            fwt.write(new_line);
                            fwt.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }

                    if (record_flag){
                        try {
                            bos.write(img_bytes);
                            bos.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                }


                socket.close();
                //connText.setText("File write successfully! location:"+file.toString());
                mediaCodec.stop();
                mediaCodec.release();

                MainActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this,
                                "Finished",
                                Toast.LENGTH_LONG).show();
                    }});

            } catch (IOException e) {

                e.printStackTrace();

                final String eMsg = "Something wrong: " + e.getMessage();
                MainActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this,
                                eMsg,
                                Toast.LENGTH_LONG).show();
                    }});

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

}