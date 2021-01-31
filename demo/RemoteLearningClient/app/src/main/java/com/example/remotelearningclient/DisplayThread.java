package com.example.remotelearningclient;

import android.graphics.SurfaceTexture;
import android.opengl.GLES30;
import android.util.Log;

import java.util.ArrayList;

import static com.example.remotelearningclient.MainActivity.mediaCodec;
import static com.example.remotelearningclient.MainActivity.mediaCodecBufferInfo;
import static com.example.remotelearningclient.MainActivity.missCnt;

public class DisplayThread extends Thread{

    private static SurfaceTexture displaySurfaceTexture;

    public DisplayThread(SurfaceTexture surfaceTexture){
        displaySurfaceTexture = surfaceTexture;
    }

    private static synchronized void updateDisplay() {
        // clear display surface texture
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);

        // draw tiles
        String frame = System.currentTimeMillis() + " ";
        for(int videoID : Utils.getCurVisibleTiles()) {
            FrameCache.drawSingleTile(videoID, displaySurfaceTexture, Config.COLOR);
            Stats.tileDispd.add(videoID);
            frame += (videoID + " ");
        }
        Stats.frameDisp.add(frame);


        // display what have been drawn
        OpenGLHelper.display();

        // update camera matrix according to sensor reading
        OpenGLHelper.updateCamera();

        /*// calculate FPS
        if(Stats.nDisplayed == 0) {
            Stats.startTS = System.currentTimeMillis();
        }
        Stats.nDisplayed++;

        if(Stats.nDisplayed == Config.FPS_CAL_INTERVAL) {
            float thisFPS = 1000.0f/((float)(System.currentTimeMillis() - Stats.startTS)/(Config.FPS_CAL_INTERVAL - 1));
            Stats.FPSs.add(System.currentTimeMillis() + " " + thisFPS);
            Stats.nDisplayed = 0;
            Log.e(Config.GLOBAL_TAG, "thisFPS = " + thisFPS);
        }*/

        Stats.nTotalDisplayed++;
    }

    @Override
    public void run(){
        while(true){
            /*
            int outputBufferIndex = mediaCodec.dequeueOutputBuffer(mediaCodecBufferInfo, 0);
            if (outputBufferIndex >= 0) {
                mediaCodec.releaseOutputBuffer(outputBufferIndex, true);
            }
            else{
                missCnt++;
            }*/

            //ArrayList<Integer> al = Utils.getCurVisibleTiles();
            try {
                Thread.sleep(16);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
