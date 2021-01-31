package com.example.remotelearningclient;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.opengl.GLES30;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;


public class OpenGLThread extends HandlerThread {
    public Handler handler;

    private int nDecoders;
    private static SurfaceTexture displaySurfaceTexture;
    private Context context;

    // objects
    //public static ObjAnimationHelper[] objAnimationHelper;

    private static boolean setupdelay = true;
    // clocking thread pause/resume variables
    private static Object mPauseLock;
    private static boolean mPaused;
    private static boolean mFinished;

    private static FileWriter reportFwt;
    private static File reportFile;
    private static File rootDir;

    // clock thread, fire display signal
    static private class ClockThread implements Runnable {
        Handler handler;
        ClockThread(Handler h) {
            this.handler = h;
        }
        public void run() {
            int sleepInterval = (int)(1000/(double)(Config.TARGET_FPS) - 1);
            while (!mFinished) {
                // fire signal
                try {
                    // do not fire MSG_FRAME_SYNC immediately
                    if(setupdelay) {
                        Thread.sleep(Config.SETUP_DELAY);
                        setupdelay = false;
                    }
                    handler.sendEmptyMessage(Config.MSG_FRAME_SYNC);
                    Thread.sleep(sleepInterval);
                } catch (Exception e) { }

                // check if paused
                synchronized (mPauseLock) {
                    while (mPaused) {
                        try {
                            mPauseLock.wait();
                        } catch (InterruptedException e) { }
                    }
                }
            }
        }
        // call this to pause clock thread
        public static void pause() {
            synchronized (mPauseLock) {
                mPaused = true;
            }
        }
        // call this to resume clock thread
        public static void resume() {
            synchronized (mPauseLock) {
                mPaused = false;
                mPauseLock.notifyAll();
            }
        }
    }


    public OpenGLThread(String name, int nDecoders, SurfaceTexture surfaceTexture, Context context, File root) {
        super(name);
        this.nDecoders = nDecoders;
        this.context = context;
        displaySurfaceTexture = surfaceTexture;
        rootDir = root;
    }

/*
    private static void initObjects(Context context) {
        ObjSharedData swordObj = new ObjSharedData();
        swordObj.init(Config.OBJ_MODEL_H);

        // each ObjAnimationHelper animates one object
        objAnimationHelper = new ObjAnimationHelper[9];
        objAnimationHelper[0] = new ObjAnimationHelper(context, -1.0f, 0.5f, 2.0f, swordObj);
        objAnimationHelper[1] = new ObjAnimationHelper(context, 0.0f, 0.5f, 2.0f, swordObj);
        objAnimationHelper[2] = new ObjAnimationHelper(context, 1.0f, 0.5f, 2.0f, swordObj);
        objAnimationHelper[3] = new ObjAnimationHelper(context, -1.0f, -0.5f, 2.0f, swordObj);
        objAnimationHelper[4] = new ObjAnimationHelper(context, 0.0f, -0.5f, 2.0f, swordObj);
        objAnimationHelper[5] = new ObjAnimationHelper(context, 1.0f, -0.5f, 2.0f, swordObj);
        objAnimationHelper[6] = new ObjAnimationHelper(context, -1.0f, -1.5f, 2.0f, swordObj);
        objAnimationHelper[7] = new ObjAnimationHelper(context, 0.0f, -1.5f, 2.0f, swordObj);
        objAnimationHelper[8] = new ObjAnimationHelper(context, 1.0f, -1.5f, 2.0f, swordObj);
    }

    private static void drawObjects() {
        int currNumOfObj = Utils.avatorTrace.get(Utils.naviIdx);
        if(currNumOfObj > 9) currNumOfObj = 9;
        for(int i = 0; i < currNumOfObj; i++) {
            objAnimationHelper[i].drawCurrentAnimation();
        }
    }
*/

    private static synchronized void updateDisplay(int videoID, String posMsg) {
        // clear display surface texture
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);

        // draw tiles
        String frame = Instant.now() + " ";
        //for(int videoID : Utils.getCurVisibleTiles()) {
        //String posMsg = Utils.dispBuffer.get(0);
        String pos = Utils.getPosFromMsg(posMsg);
        //int videoID = Utils.pos2VideoID.get(pos);
        //int videoID = Utils.dispBuffer.get(0);
            FrameCache.drawSingleTile(videoID, displaySurfaceTexture, Config.COLOR);
            Stats.tileDispd.add(videoID);
            Stats.lastDisplayed = posMsg;
            frame += (videoID + " ");
            //Stats.videoDisp[Stats.nTotalDisplayed%Config.REPORT_INTERVAL] = videoID;
            /*try {
                reportFwt.write(videoID+", \n");
                reportFwt.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }*/
            Message msg = new Message();
            msg.what = Config.SEND_ACK;
            Bundle bundle = new Bundle();
            bundle.putString(Config.MSG_KEY, posMsg);
            msg.setData(bundle);
            MainActivity.functionalNetThread.handler.sendMessage(msg);
        //}
        Stats.frameDisp.add(frame);
        //System.out.println("frame pose: "+posMsg);
        // overlay object
        //if(Config.OVERLAY_OBJECTS)
        //    drawObjects();

        // display what have been drawn
        OpenGLHelper.display();

        // update camera matrix according to sensor reading
        OpenGLHelper.updateCamera();

        // calculate FPS
        if(Stats.nDisplayed == 0) {
            Stats.startTS = System.currentTimeMillis();
        }
        Stats.nDisplayed++;
        long curTime = System.currentTimeMillis();
        long timeGap = curTime - Stats.startTS;
        //if(Stats.nDisplayed == Config.FPS_CAL_INTERVAL) {
        if(timeGap > Config.FPS_CAL_INTERVAL){
            //float thisFPS = 1000.0f/((float)(System.currentTimeMillis() - Stats.startTS)/(Config.FPS_CAL_INTERVAL - 1));
            float thisFPS = (Stats.nDisplayed-1)*1000.0f/((float)timeGap);
            Stats.FPSs.add(System.currentTimeMillis() + " " + thisFPS);
            Stats.repFrames.add(System.currentTimeMillis() + " " + Stats.repFrameCnt);
            Stats.skipFrames.add(System.currentTimeMillis() + " " + Stats.skipFrameCnt);
            Stats.nDisplayed = 0;
            Stats.repFrameCnt = 0;
            Stats.skipFrameCnt = 0;
            MainActivity.connText.setText("FPS: "+(int) thisFPS);
            Log.e(Config.GLOBAL_TAG, "thisFPS = " + thisFPS);
        }

        Stats.nTotalDisplayed++;
    }

/*
    public static void restartApp(Context context) {
        // terminate networking
        Bundle b = new Bundle(3);
        Message msg = MainActivity.networkingThread.handler.obtainMessage();
        msg.what = Config.TERMINATE;
        msg.setData(b);
        try {
            MainActivity.networkingThread.handler.sendMessage(msg);
        }
        catch (Exception e) {
            Log.e(Config.GLOBAL_TAG, "send terminate msg to network thread error:" + e.getMessage());
        }
        // rename trace to finished
        String newname = Config.TRACE_FILE.getAbsolutePath() + ".finished";
        Config.TRACE_FILE.renameTo(new File(newname));
        // restart
        Intent mStartActivity = new Intent(context, MainActivity.class);
        int mPendingIntentId = 123456;
        PendingIntent mPendingIntent = PendingIntent.getActivity(context, mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager mgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 10000, mPendingIntent);
        System.exit(0);
    }*/

    /** Entry point. */
    public void start() {
        super.start();

        handler = new Handler(getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case Config.MSG_SETUP: {
                        // init opengl
                        OpenGLHelper.oneTimeSetup(displaySurfaceTexture);

                        // init frameCache (surfaceTextures, surfaces, extTextures, myGLTextures...)
                        FrameCache.init(Config.FBO_CACHE_SIZE, nDecoders);

                        // init objects
                        //if(Config.OVERLAY_OBJECTS)
                        //    initObjects(context);

                        // init template header for decoding
                        Utils.initMegaTemplate();

                        // start decoders
                        Utils.mFrameDecoders = new FrameDecoder[nDecoders];
                        for(int i = 0; i < nDecoders; i++) {
                            Utils.mFrameDecoders[i] = new FrameDecoder(i); // one decoding thread instance
                            FrameDecoderWrapper.runExtractor(Utils.mFrameDecoders[i]);
                        }

                        /*
                        // connect to server, request initial list (step 0)
                        Utils.naviIdx = 0;
                        connectToServer();
                        requestTileListByTraceIdx(Utils.naviIdx);*/

                        // start clock thread to fire MSG_FRAME_SYNC every 1/FPS second
                        mPauseLock = new Object();
                        mPaused = false;
                        mFinished = false;
                        new Thread(new ClockThread(handler)).start();

                        break;
                    }

                    case Config.MSG_ON_FRAME_AVAILABLE: {
                        //long t1 = System.currentTimeMillis();
                        int decoderID = msg.arg1;
                        int videoID = Utils.mFrameDecoders[decoderID].videoID;
                        //System.out.println("available video id: "+videoID);
                        if(videoID < 0) throw new RuntimeException("MSG_ON_FRAME_AVAILABLE error");

                        /*
                        int latestIDInDisplayBuffer;
                        if(!Utils.dispBuffer.isEmpty()){
                            latestIDInDisplayBuffer = Utils.dispBuffer.get(Utils.dispBuffer.size()-1);
                        } else{
                            latestIDInDisplayBuffer = Utils.getCurVisibleTiles().get(0);
                        }
                        if(latestIDInDisplayBuffer<videoID) Utils.dispBuffer.add(videoID);*/

                        // latch the data
                        FrameCache.mSurfaceTextures[decoderID].updateTexImage();
                        //FrameCache.mSurfaceTextures[decoderID].detachFromGLContext();
                        //MainActivity.mTextureView.setSurfaceTexture(FrameCache.mSurfaceTextures[decoderID]);
                        // cache to FBO, upper half is color lower half is depth
                        FrameCache.cache2FBO(videoID, FrameCache.mSurfaceTextures[decoderID], FrameCache.extTextures[decoderID]);
                        // release decoder awaitNewImage() lock
                        Utils.mFrameDecoders[decoderID].bFrameCached = true;
                        //long t2 = System.currentTimeMillis();
                        //System.out.println("FBO time used: "+(t2-t1));
                        // System.out.println("available video id: "+videoID);
                        break;
                    }

                    case Config.MSG_FRAME_SYNC: {
                        //System.out.println("frame sync");
                        // set pos from mvr trace
                        //if(Stats.lastMissTS == 0) {
                            // only when last display was successful
                            // get orientation from the received pos
                            //Utils.naviLat = 0;
                            //Utils.naviLon = 50;

                            /*String line = Utils.mvrpdTrace.get(Utils.naviIdx);
                            Utils.naviX = Integer.parseInt(line.split(",")[0].split(" ")[0]);
                            Utils.naviZ = Integer.parseInt(line.split(",")[0].split(" ")[1]);
                            if (!Config.SENSOR_ROTATION) {
                                // if we rotate camera by trace reading
                                Utils.naviLat = Float.parseFloat(line.split(",")[0].split(" ")[2]);
                                Utils.naviLon = Float.parseFloat(line.split(",")[0].split(" ")[3]);
                            }*/
                        //}

                        // report stats periodically
                        /*if(Stats.nTotalDisplayed == Config.REPORT_TIME){
                            reportStats();
                            Config.NETWORK = false;
                            ClockThread.pause();
                            MainActivity.functionalNetThread.handler.sendEmptyMessage(0);
                            break;
                        }*/

                        /*
                        // report stats when replay finish
                        // if(Utils.naviIdx == Utils.mvrpdTrace.size() - 100) {
                        if(Utils.naviIdx == Utils.mvrpdTrace.size() - 100) {
                            ClockThread.pause();
                            reportStats(context);
                            restartApp(context);
                            break;
                        }*/

                        // check if current visible tiles are cached in FBO
                        /*if(!FrameCache.FBOcacheContainsCurVisibleTiles()) {
                            Log.e(Config.GLOBAL_TAG, "FATAL: tiles not available for step " + Utils.naviIdx);
                            Stats.missedStep.add(Utils.naviIdx);
                            if(Stats.lastMissTS != 0) Stats.totalStall += System.currentTimeMillis() - Stats.lastMissTS;
                            Stats.lastMissTS = System.currentTimeMillis();
                            // per trace idx num of stalls
                            if(Stats.traceIdxStalls.containsKey(Utils.naviIdx))
                                Stats.traceIdxStalls.put(Utils.naviIdx, Stats.traceIdxStalls.get(Utils.naviIdx) + 1);
                            else
                                Stats.traceIdxStalls.put(Utils.naviIdx, 1);*/
                        //int videoToDisplay = Utils.getCurVisibleTiles().get(0);
                        //int videoToDisplay;
                        String posMsg;
                        if(!Utils.dispBuffer.isEmpty()) {
                            posMsg = Utils.dispBuffer.get(0);
                            String pos = Utils.getPosFromMsg(posMsg);
                            if(!Utils.pos2VideoID.containsKey(pos)||
                                    //posMsg.equals(Stats.lastDisplayed)||
                                    !FrameCache.FBOcacheContainsMegaTile(Utils.pos2VideoID.get(pos))) {
                                Log.e(Config.GLOBAL_TAG,"No proper frame to display, pos: "+pos+"id: "+Utils.pos2VideoID.get(pos));
                                break;
                            }
                            int videoToDisplay = Utils.pos2VideoID.get(pos);
                            Utils.dispBuffer.remove(0);
                            // get orientation from the received pos
                            String[] coor = posMsg.split(",");
                            Utils.naviLat = 0-calAngle(Float.parseFloat(coor[3]));
                            Utils.naviLon = 90-calAngle(Float.parseFloat(coor[4]));

                            //Utils.naviLat = 20;
                            //Utils.naviLon = 20;
                            //System.out.println(Utils.naviLon);
                            updateDisplay(videoToDisplay,posMsg);

                        }
                        /*int diff = videoToDisplay - Stats.lastDisplayed;
                        if(diff==0){
                            Stats.repFrameCnt+=1;
                        }
                        else {
                            Stats.lastMissTS = 0;
                            if (diff>1) {
                                Stats.skipFrameCnt += diff;
                            }
                            // update display
                            updateDisplay();

                            // proceed trace
                            //if(Config.traceNavigation) requestTileListByTraceIdx(++Utils.naviIdx);
                        }*/

                        break;
                    }

                    case Config.MSG_REPORT_STATS: {
                        reportStats();
                    }

                    default: {
                        Log.e(Config.GLOBAL_TAG, "handler thread receive unknown msg");
                        break;
                    }
                }
            }
        };
    }

    private static float calAngle(float degree){
        double result =  (degree+180.)%360. - 180.;
        return (float) result;
    }

    private static void reportStats(){
        Log.e(Config.GLOBAL_TAG, "Statistics report start.");

        reportFile = new File(
                rootDir,
                "FPS.csv");
        try {
            reportFile.createNewFile();
            reportFwt = new FileWriter(reportFile);
            for(String item : Stats.FPSs) {
                float fps = Float.parseFloat(item.split(" ")[1]);
                if (fps > 65.0f) fps = 65.0f;
                // fw.write(item.split(" ")[0] + " " + fps + "\n");
                reportFwt.write(fps + ",\n");
            }
            reportFwt.flush();
            reportFwt.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
/*
        reportFile = new File(
                rootDir,
                "repeteFrames.txt");
        try {
            reportFile.createNewFile();
            reportFwt = new FileWriter(reportFile);
            for(String item : Stats.repFrames) {
                float repFrame = Float.parseFloat(item.split(" ")[1]);
                reportFwt.write(repFrame + "\n");
            }
            reportFwt.flush();
            reportFwt.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        reportFile = new File(
                rootDir,
                "skipFrames.txt");
        try {
            reportFile.createNewFile();
            reportFwt = new FileWriter(reportFile);
            for(String item : Stats.skipFrames) {
                float skipFrame = Float.parseFloat(item.split(" ")[1]);
                reportFwt.write(skipFrame + "\n");
            }
            reportFwt.flush();
            reportFwt.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // frame disp
        reportFile = new File(
                rootDir,
                "dispFrames.txt");
        try {
            reportFile.createNewFile();
            reportFwt = new FileWriter(reportFile);
            for(String frame : Stats.frameDisp)
                reportFwt.write(frame + "\n");
            reportFwt.flush();
            reportFwt.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
*/
        Log.e(Config.GLOBAL_TAG, "Statistics report has been done.");
    }
/*
    private static void connectToServer() {
        Message msg = MainActivity.networkingThread.handler.obtainMessage();
        msg.what = Config.CONNECT;
        try {
            MainActivity.networkingThread.handler.sendMessage(msg);
        }
        catch (Exception e) {
            Log.e(Config.GLOBAL_TAG, "connectToServer() sendMessage to network thread error:" + e.getMessage());
        }
    }


    private static synchronized void requestTileListByTraceIdx(int traceIdx) {
        Bundle b = new Bundle(3);
        Message msg = MainActivity.networkingThread.handler.obtainMessage();
        msg.what = Config.REQUEST;
        b.putInt("traceIdx", traceIdx);
        msg.setData(b);
        try {
            MainActivity.networkingThread.handler.sendMessage(msg);
        }
        catch (Exception e) {
            Log.e(Config.GLOBAL_TAG, "requestList() sendMessage to network thread error:" + e.getMessage() + ", traceIdx = " + traceIdx);
        }
    }


    public static void reportStats(Context context) {
        // sleep and log to prevent ConcurrentModificationException
        try {
            Thread.sleep(100);
        } catch (Exception e) {}

        Log.e(Config.GLOBAL_TAG, "TOTAL STALL = " + Stats.totalStall );
        Log.e(Config.GLOBAL_TAG, "DISPLAY SUCCESS RATE (PREDICTION ACCURACY) = " + (1-(float)Stats.missedStep.size()/Stats.nTotalDisplayed) );
        Log.e(Config.GLOBAL_TAG, "Tile received=" + Stats.tileRecvd.size());
        Log.e(Config.GLOBAL_TAG, "Tile displayed=" + Stats.tileDispd.size());
        Log.e(Config.GLOBAL_TAG, "Tile swapped L3->L2=" + Stats.nSwap);
        Log.e(Config.GLOBAL_TAG, System.currentTimeMillis()+"");

        File FILES_DIR = Environment.getExternalStorageDirectory();

        // total stall
        File file = new File(FILES_DIR, Config.STATS_PATH + "stalls.txt");
        try {
            FileWriter fw = new FileWriter(file,true);
            fw.write(Stats.totalStall + "\n");
            fw.close();
        } catch(IOException ioe) {}

        // misc
        file = new File(FILES_DIR, Config.STATS_PATH + "misc.txt");
        try {
            FileWriter fw = new FileWriter(file,true);
            fw.write("Tile received=" + Stats.tileRecvd.size() + "\n");
            fw.write("Tile displayed=" + Stats.tileDispd.size() + "\n");
            fw.write("Tile swapped L3->L2=" + Stats.nSwap + "\n");
            fw.close();
        } catch(IOException ioe) {}

        // per stall duration
        file = new File(FILES_DIR, Config.STATS_PATH + "perStallDuration.txt");
        try {
            FileWriter fw = new FileWriter(file,true);
            Iterator it = Stats.traceIdxStalls.entrySet().iterator();
            int noStalls = Utils.mvrpdTrace.size() - Stats.traceIdxStalls.size();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry)it.next();
                fw.write((Integer.parseInt(pair.getValue().toString()) + 1) + "\n");
                it.remove(); // avoids a ConcurrentModificationException
            }
            for(int i = 0; i < noStalls; i++) fw.write("1\n");
            fw.close();
        } catch(IOException ioe) {}

        // FPS
        file = new File(FILES_DIR, Config.STATS_PATH + "FPS.txt");
        try {
            // remove first and last record (startup lag)
            Stats.FPSs.remove(Stats.FPSs.size() - 1);
            Stats.FPSs.remove(0);
            FileWriter fw = new FileWriter(file,true);
            for(String item : Stats.FPSs) {
                float fps = Float.parseFloat(item.split(" ")[1]);
                if (fps > 65.0f) fps = 65.0f;
                // fw.write(item.split(" ")[0] + " " + fps + "\n");
                fw.write(fps + "\n");
            }
            fw.close();
        } catch(IOException ioe) {}

        // frame disp
        file = new File(FILES_DIR, Config.STATS_PATH + "frameDisp_" + Utils.traceID + ".txt");
        try {
            FileWriter fw = new FileWriter(file,true);
            for(String frame : Stats.frameDisp)
                fw.write(frame + "\n");
            fw.close();
        } catch(IOException ioe) {}

        Log.e(Config.GLOBAL_TAG, System.currentTimeMillis()+"");
    }*/
}
