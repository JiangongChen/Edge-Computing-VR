package com.example.remotelearningclient;

public class Config {
    public static int FRAME_WIDTH = 2560;
    public static int FRAME_HEIGHT = 1440;
    public final static int SPHERE_SLICES = 180;
    public final static float SPHERE_RADIUS = 100.0f;
    public final static int SPHERE_INDICES_PER_VERTEX = 1;
    public final static int MSG_FRAME_SYNC = 0x101;
    public final static int MSG_SETUP = 0x102;
    public final static int MSG_ON_FRAME_AVAILABLE = 0x103;
    public final static int MSG_REPORT_STATS = 0x104;

    public static int screenWidth = 0;
    public static int screenHeight = 0;

    // Server ip and ports
    public static String SERVER_IP = "192.168.1.234";
    public final static int FUNC_PORT = 8000;
    public final static int FramePort = 8888;
    public final static int PosPort = 8848;


    // networkThread msg type
    public final static int CONNECT = 0x300;
    public final static int SEND_ACK = 0x301;

    public static boolean bProjection = true;
    public static int bOverlay = 0;

    public static int FOVX = 90;
    public static int FOVY = 60;

    public static double moveStep = 0.6;

    public static int nRows = 1;
    public static int nColumns = 1;
    public static int nTiles = 1;
    public static int nDecoders = 3;
    public static int SETUP_DELAY = 500;
    public static int WaitTimeToStop = 4000;
    public static int msgSize = 64;
    public static float granular = (float) 0.01;

    public static final String MSG_KEY = "hi";
    public static int COLOR = -1;
    //public static int DEPTH = -2;

    public static boolean RENDER = false;
    public static boolean NETWORK = true;
    public static int TARGET_FPS = 60;

    public final static boolean SENSOR_ROTATION = false;
    //sensor data
    public static float[] sensor_rotation = new float[4]; // x_rot, y_rot, z_rot, ang_rot

    // FPS calculation interval, unit ms
    public final static int FPS_CAL_INTERVAL = 500;
    // When the frame count reaches the following, report statistics
    public final static int REPORT_TIME = 1000;

    // draw objects
    public final static boolean OVERLAY_OBJECTS = false;

    // FBO CACHE
    public static double FBO_CACHE_LIFE = 100; // ms
    public static int FBO_CACHE_SIZE = 200; // depth + color FBO
    // video cache
    public static long VIDEO_CACHE_LIFE = 1000; // ms
    public static int VIDEO_CACHE_LIMIT = 3000;
    // Display buffer size
    public static int DISPLAY_CACHE_LIMIT = 5;

    public static String GLOBAL_TAG = "Remote Learning";
}
