package com.example.remotelearningclient;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
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
    public static TextView connText;
    private EditText mEditText;
    private Button buttonTeacher;
    private Button buttonStudent;
    private static Context mContext;
    private boolean posFlag;
    private View mControlsView;

    File rootDir;

    public static Surface mSurface;
    public static SurfaceTexture mSurfaceTexture;
    public static MediaCodec mediaCodec;
    public static MediaCodec.BufferInfo mediaCodecBufferInfo;
    public static volatile int missCnt=0;

    public static OpenGLThread mOpenGLThread;
    public static NetworkingThread functionalNetThread;
    public static DisplayThread mDisplayThread;
    public static MovementThread mMovementThread;
    public static RecvThread mRecvThread;

    public static TextureView mTextureView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //set full screen
        mControlsView=getWindow().getDecorView();

        connText = (TextView) findViewById(R.id.conn);
        mEditText = (EditText) findViewById(R.id.ipAddr);
        //surfaceView = (SurfaceView) findViewById(R.id.surface);
        mTextureView = (TextureView) findViewById(R.id.view);
        buttonTeacher = (Button) findViewById(R.id.buttonTeacher);
        buttonStudent = (Button) findViewById(R.id.buttonStudent);

        //connText.setText("Test");
        connText.bringToFront();

        buttonTeacher.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                posFlag = true;
                startPlayback();
            }
        });

        buttonStudent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                posFlag = false;
                startPlayback();
            }
        });
    }

    public static Context getContext() {
        return mContext;
    }

    public void startPlayback() {
        // hide the navigation bar
        Config.SERVER_IP = mEditText.getText().toString();
        buttonTeacher.setVisibility(View.GONE);
        buttonStudent.setVisibility(View.GONE);
        mEditText.setVisibility(View.GONE);
        int uiOptions = View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        mControlsView.setSystemUiVisibility(uiOptions);

        Config.screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
        Config.screenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
        System.out.println("width: "+Config.screenWidth+" height: "+Config.screenHeight);
/*
        try {
            mediaCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        } catch (IOException e) {
            e.printStackTrace();
        }
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, Config.FRAME_WIDTH, Config.FRAME_HEIGHT);
        //mediaFormat.setByteBuffer("csd-0",ByteBuffer.wrap());
        //mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 100000);
        mediaCodec.configure(mediaFormat, surfaceView.getHolder().getSurface(), null, 0);
        mediaCodec.start();

        mediaCodecBufferInfo = new MediaCodec.BufferInfo();*/

/*
        ReceiveThread receiveThread =
                new ReceiveThread();
        receiveThread.start();
*/
        rootDir = getExternalFilesDir(null);

        mRecvThread =
                new RecvThread(connText,surfaceView,rootDir,Config.SERVER_IP,Config.FramePort);
        mRecvThread.start();

        if (posFlag) {
            mMovementThread =
                    new MovementThread(Config.SERVER_IP, Config.PosPort);
            mMovementThread.start();
        }
/*
        mDisplayThread =
                new DisplayThread(mTextureView.getSurfaceTexture());
        mDisplayThread.start();*/
/*
        DecodeThread decodeThread =
                new DecodeThread();
        decodeThread.start();*/

        // start opengl thread
        mOpenGLThread =new OpenGLThread("opengl thread",Config.nDecoders, mTextureView.getSurfaceTexture(), getApplicationContext(),rootDir);
        mOpenGLThread.start(); // mOpenGLThread are ready  receive msg after this point
        mOpenGLThread.handler.sendEmptyMessage(Config.MSG_SETUP);

        // start networking thread
        functionalNetThread = new NetworkingThread("functional net thread");
        functionalNetThread.start();
        functionalNetThread.handler.sendEmptyMessage(Config.CONNECT);
    }



}