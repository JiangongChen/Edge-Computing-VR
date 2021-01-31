package com.example.remotelearningclient;

import android.content.Context;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.opengl.GLES30;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static android.opengl.GLU.gluErrorString;

/** GL utility methods. */
public class Utils {

    public static MediaExtractor megaExtractor;
    public static MediaFormat megaFmtTemplate;
    public static FrameDecoder[] mFrameDecoders; // last one is depth decoder
    public static ArrayList<String> mvrpdTrace = new ArrayList<>();
    public static ArrayList<Integer> avatorTrace = new ArrayList<>();
    public static HashMap<Integer, String> videoID2mp4Name = new HashMap<>(); // videoID(global tileID) -> mp4 filename(frameID_tileID)
    public static HashMap<String, Integer> pos2VideoID = new HashMap<>();
    public static int traceID = -1; // indicates which trace we are running
    public static int latestVideoId = 0;

    public static ArrayList<String> dispBuffer = new ArrayList<>();

    // navigation
    public static int naviIdx;
    public static int naviX;
    public static int naviZ;
    public static float naviLat = 0.0f;
    public static float naviLon = 0.0f;
    public static boolean endOfTrace = false;
    public static ArrayList<Integer> combineTileList(ArrayList<Integer> al1, ArrayList<Integer> al2) {
        if(endOfTrace) return new ArrayList<>();
        ArrayList<Integer> al3 = new ArrayList<>();
        for(int videoID : al1) al3.add(videoID);
        for(int videoID : al2) al3.add(videoID);
        return al3;
    }
    public static String getPosFromMsg(String recvStr){
        String[] coor = recvStr.split(",");
        float[] positions = new float[3];
        for(int i=0;i<3;i++){
            positions[i] = (int)(Float.parseFloat(coor[i])/Config.granular)*Config.granular;
        }
        //String posX = recvStr.substring(0,recvStr.indexOf(",")).trim();
        //String posY = recvStr.substring(recvStr.indexOf(",")+1).trim();
        String pos = "("+String.format("%.2f",positions[0])+","+String.format("%.2f",positions[1])+","+String.format("%.2f",positions[2])+")";
        return pos;
    }

    public static ArrayList<Integer> getCurVisibleTiles() {
        // get the latest available video id
        ArrayList<Integer> al = new ArrayList<>();
        int latestId = Utils.latestVideoId;
        /*
        long curTS = System.currentTimeMillis();
        double minTime = 1e26;
        for (Iterator<HashMap.Entry<Integer, Pair<Integer, Long>>> it = FrameCache.colorFrameCache.entrySet().iterator(); it.hasNext();) {
            HashMap.Entry<Integer, Pair<Integer, Long>> entry = it.next();
            double timepast = curTS - entry.getValue().second;
            if (timepast < minTime) {
                // check if current visible tiles contain this
                latestId = entry.getKey();
                minTime = timepast;
            }
        }
        */
        while(latestId>0){
            if(FrameCache.FBOcacheContainsMegaTile(latestId)) {
                break;
            }
            latestId--;
        }
        al.add(latestId);
        /*
        if(endOfTrace) return new ArrayList<>();
        ArrayList<Integer> al = new ArrayList<>();
        String line = Utils.mvrpdTrace.get(Utils.naviIdx);
        String part = line.split(",")[1];
        String[] items = part.split(" ");
        for(String item : items) {
            if(item.length() > 0)
                al.add(Integer.parseInt(item));
        }*/
        return al;
    }
    public static ArrayList<Integer> getCurPDTiles() {
        if(endOfTrace) return new ArrayList<>();
        ArrayList<Integer> al = new ArrayList<>();
        String line = Utils.mvrpdTrace.get(Utils.naviIdx);
        String[] parts = line.split(",");
        if(parts[2].trim().length() == 0)
            return al;
        String part = parts[2];
        String[] items = part.split(" ");
        for(String item : items) {
            if(item.length() > 0)
                al.add(Integer.parseInt(item));
        }
        return al;
    }
    public static ArrayList<Integer> getCurSurroundingTiles() {
        if(endOfTrace) return new ArrayList<>();
        ArrayList<Integer> al = new ArrayList<>();
        String line = Utils.mvrpdTrace.get(Utils.naviIdx);
        String[] parts = line.split(",");
        if(parts[3].trim().length() == 0)
            return al;
        String part = parts[3];
        String[] items = part.split(" ");
        for(String item : items) {
            if(item.length() > 0)
                al.add(Integer.parseInt(item));
        }
        return al;
    }



    public static String getMyIP(Context context) {
        WifiManager wifiMan = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInf = wifiMan.getConnectionInfo();
        int ipAddress = wifiInf.getIpAddress();
        String ip = String.format("%d.%d.%d.%d", (ipAddress & 0xff),(ipAddress >> 8 & 0xff),(ipAddress >> 16 & 0xff),(ipAddress >> 24 & 0xff));
        return ip;
    }

    /** Returns the consumer friendly device name */
    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        }
        return capitalize(manufacturer) + " " + model;
    }
    private static String capitalize(String str) {
        if (TextUtils.isEmpty(str)) {
            return str;
        }
        char[] arr = str.toCharArray();
        boolean capitalizeNext = true;

        StringBuilder phrase = new StringBuilder();
        for (char c : arr) {
            if (capitalizeNext && Character.isLetter(c)) {
                phrase.append(Character.toUpperCase(c));
                capitalizeNext = false;
                continue;
            } else if (Character.isWhitespace(c)) {
                capitalizeNext = true;
            }
            phrase.append(c);
        }

        return phrase.toString();
    }


    public static final int BYTES_PER_FLOAT = 4;
    /** Debug builds should fail quickly. Release versions of the app should have this disabled. */
    private static final boolean HALT_ON_GL_ERROR = false;
    /** Class only contains static methods. */
    private Utils() {}

    // to generate template raw, ffmpeg -i input.mp4 -t 0 -c:v copy output.mp4
    public static void initMegaTemplate() {
        /*try {
            megaExtractor = new MediaExtractor();
            megaExtractor.setDataSource(new HeaderTemplate(R.raw.crf19_0_0)); // mega
            megaExtractor.selectTrack(0);
            megaFmtTemplate = megaExtractor.getTrackFormat(0);
            megaExtractor.release();
        } catch (Exception e) {
            throw new RuntimeException("initColorTemplate error");
        }*/
        megaFmtTemplate = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, Config.FRAME_WIDTH, Config.FRAME_HEIGHT);
    }


    /** Checks GLES30.glGetError and fails quickly if the state isn't GL_NO_ERROR. */
    public static void checkGlError() {
        int error = GLES30.glGetError();
        int lastError;
        if (error != GLES30.GL_NO_ERROR) {
            do {
                lastError = error;
                Log.e(Config.GLOBAL_TAG, "glError " + gluErrorString(lastError));
                error = GLES30.glGetError();
            } while (error != GLES30.GL_NO_ERROR);

            if (HALT_ON_GL_ERROR) {
                RuntimeException e = new RuntimeException("glError " + gluErrorString(lastError));
                Log.e(Config.GLOBAL_TAG, "Exception: ", e);
                throw e;
            }
        }
    }
}
