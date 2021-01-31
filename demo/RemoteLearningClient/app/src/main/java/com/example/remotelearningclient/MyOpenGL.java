package com.example.remotelearningclient;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Handler;

public class MyOpenGL extends Thread{
    public Handler handler;

    private int nDecoders;
    private static SurfaceTexture displaySurfaceTexture;
    private Context context;
    private boolean bFrameCached;


    private static boolean setupdelay = true;

    public void init(){
        // init opengl
        OpenGLHelper.oneTimeSetup(displaySurfaceTexture);

        // init frameCache (surfaceTextures, surfaces, extTextures, myGLTextures...)
        FrameCache.init(Config.FBO_CACHE_SIZE, nDecoders);


        // init template header for decoding
        Utils.initMegaTemplate();

        // start decoders
        Utils.mFrameDecoders = new FrameDecoder[nDecoders];
        for(int i = 0; i < nDecoders; i++) {
            Utils.mFrameDecoders[i] = new FrameDecoder(i); // one decoding thread instance
            FrameDecoderWrapper.runExtractor(Utils.mFrameDecoders[i]);
        }


    }

    public MyOpenGL(String name, int nDecoders, SurfaceTexture surfaceTexture, Context context){
        this.nDecoders = nDecoders;
        this.context = context;
        displaySurfaceTexture = surfaceTexture;
        init();
    }

    @Override
    public void run(){
        while(true) {
            try {
                if(!Config.RENDER) {
                    Thread.sleep(100);
                }
                else{

                    int decoderID = 0;
                    int videoID = Utils.mFrameDecoders[decoderID].videoID;

                    if (videoID < 0) throw new RuntimeException("MSG_ON_FRAME_AVAILABLE error");

                    // latch the data
                    FrameCache.mSurfaceTextures[decoderID].updateTexImage();
                    // cache to FBO, upper half is color lower half is depth
                    FrameCache.cache2FBO(videoID, FrameCache.mSurfaceTextures[decoderID], FrameCache.extTextures[decoderID]);
                    // release decoder awaitNewImage() lock
                    Utils.mFrameDecoders[decoderID].bFrameCached = true;

                    awaitNewImage();

                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void awaitNewImage() {
        while(!bFrameCached) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                throw new RuntimeException("decoder " + " - sleep error");
            }
        }
        bFrameCached = false;
    }

}
