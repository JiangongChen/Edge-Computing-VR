import java.lang.Thread;
import java.io.*;
import java.net.*;
import java.util.HashMap; 
import java.time.Duration;
import java.time.Instant;

public class TranThread extends Thread {

	//final int PORT;
	private static HashMap<String, byte[]> map = null; 
    private HashMap<String, Long> frameSendCache;
	private Socket socket;
    private String clientAddr;
    //private Stats curStat;
	private int index;
    private byte[] sendBuf;
    private int bufSize;
    private int cacheSize;
    private int cacheLife;
    private float granular;
	private OutputStream output;
	private DataOutputStream dOut;
	private BufferedOutputStream bOut;
    private InputStream input;
    private DataInputStream dIn;
    private int videoId;
    private HashMap<String, Long> videoSendTime = new HashMap<>(); 

    // send frame time used specific time
    long t1 = 0;
    long t2 = 0;
    long t3 = 0;
    long t4 = 0;
	//String curPos;

	TranThread(HashMap<String, byte[]> hash_map, Socket sock, int num){
		map = hash_map;
        frameSendCache = new HashMap<>();
		socket = sock;
		index = num;
        bufSize = 1024;
        cacheSize = 600;
        cacheLife = 500;
        sendBuf = new byte[bufSize];
        videoId = 0;
        granular = (float) 0.01;
        clientAddr = socket.getInetAddress().toString();
        //curStat = JavaServer.clientStats.get(clientAddr);
		//curPos = pos;
	}

    void releaseCache(){
        int n = 0;
        for(String pos : frameSendCache.keySet()){
            long t = System.currentTimeMillis();
            long timepast = t - frameSendCache.get(pos);
            if(timepast>cacheLife){
                frameSendCache.remove(pos);
                n++;
            }
        }
        System.out.println("CACHE RELEASED: " + n);
    }

    String getPos(String recvStr){
        String[] coor = recvStr.split(",");
        float[] positions = new float[3];
        for(int i=0;i<3;i++){
            positions[i] = (int)(Float.parseFloat(coor[i])/granular)*granular;
        }
        //String posX = recvStr.substring(0,recvStr.indexOf(",")).trim();
        //String posY = recvStr.substring(recvStr.indexOf(",")+1).trim();
        String pos = "("+String.format("%.2f",positions[0])+","+String.format("%.2f",positions[1])+","+String.format("%.2f",positions[2])+")";
        return pos;
    }

    byte[] getFrame(String pos){
        //System.out.print("Client " + index + " Try to get frame:"+pos);
        byte[] wholeImage = null;
        if (map.containsKey(pos)) { 
            wholeImage = map.get(pos); 
            //System.out.print(" Successful ");
        } 
        else{
            System.out.println("Cannot find frame: "+pos);
        }
        
        return wholeImage;
    }

    void sendFrame( String pos){
        String indexPos = getPos(pos);
        byte[] wholeImage = getFrame(indexPos);
        int size = wholeImage.length;
        byte[] poseBytes = pos.getBytes();
        //String sizeStr = String.format("%8d",size);
        if (frameSendCache.containsKey(indexPos)) size = 0;
        //System.out.println("Frame size: "+size);
        //String size2str = String.format("%16d",size);
        try{
        	//Instant start = Instant.now();
            //dOut.writeInt(videoId); // write video ID
            // send pos to client
            t1 = System.currentTimeMillis();
            dOut.writeUTF(pos);
            //output.write(poseBytes);
            t2 = System.currentTimeMillis();
            dOut.writeInt(size); // write length of the message
            t3 = System.currentTimeMillis();
            if (size!=0){
                dOut.write(wholeImage);           // write the message
                frameSendCache.put(indexPos,System.currentTimeMillis());
            }
            t4 = System.currentTimeMillis();
            if((double)frameSendCache.size() > 0.8 * (double)cacheSize) {
                //releaseCache();
            }
            //bOut.write(sizeStr.getBytes(),0,8);
            //bOut.write(wholeImage,0,size);
            //bOut.flush();
            /*output.write(sizeStr.getBytes(),0,8);
            int sendSize = 0;
            while(sendSize + bufSize < size){
                output.write(wholeImage,sendSize,bufSize);
                sendSize += bufSize;
            }
            output.write(wholeImage,sendSize,size-sendSize);*/
            //Instant end = Instant.now();
            //float timeElapsed = Duration.between(start,end).toMillis();
            //System.out.println("Send time used: "+timeElapsed+" ms");
        }catch (IOException e) {
            try{
                dOut.close();
                socket.close();
            }
            catch (IOException ie) {
                ie.printStackTrace();
            }
            System.out.println("Sender exception: " + e.getMessage());
            e.printStackTrace();
        }
        
    }


	@Override
	public void run(){
		try{
			System.out.println("Tran Thread: New client connected, from: "+clientAddr);
			//System.out.println(socket.getSendBufferSize());
			//socket.setSendBufferSize(512*1024);
            socket.setTcpNoDelay(true);
			//System.out.println("Send buffer size: "+socket.getSendBufferSize());
	        output = socket.getOutputStream();
	        //PrintWriter writer = new PrintWriter(output);
	        dOut = new DataOutputStream(output);
	        //bOut = new BufferedOutputStream(output,512*1024);
            input = socket.getInputStream();
            dIn = new DataInputStream(input);


	        //String testPos = "0,0";
            String prevPos = null;

	        while(!socket.isClosed()){
	            //System.out.println("test");
	            //String curPos = getPos(testPos);
	            long start = System.currentTimeMillis();
	            //System.out.println(clientAddr + " Time before send to buffer: "+start);
                String curPos = JavaServer.curPos;
                
                // Do not send a same frame in the continues slot
                while(curPos==null ){
                    Thread.sleep(1);
                    curPos = JavaServer.curPos;
                }
                //System.out.println(curPos);
                // When the trace end, send msg to clients
                if (curPos.contains("end")) {
                    dOut.writeUTF(curPos);
                    break;
                }
                sendFrame(curPos);
                prevPos = curPos;
                /*
                while(true){
                    videoId = JavaServer.curID;
                    //if(!JavaServer.clientStats.get(clientAddr).videoSendTime.containsKey(videoId)&&
                    //    JavaServer.iDMap.containsKey(videoId)){
                    if(!videoSendTime.containsKey(videoId)&&
                        JavaServer.iDMap.containsKey(videoId)){
                        sendFrame(JavaServer.iDMap.get(videoId));
                        break;
                    }
                    else{
                        Thread.sleep(1);
                    }
                }*/
                //dIn.readInt();
	            long end = System.currentTimeMillis();
	            //System.out.println(clientAddr+" Time after send to buffer: "+end);
	            long timeElapsed = end - start;
                videoSendTime.put(curPos,end);
                //JavaServer.clientStats.get(clientAddr).videoSendTime.put(videoId,t);
                //curStat.get(clientAddr).videoSendTime.put(videoId,t);
                
                if (timeElapsed >= 15 ){
                    System.out.println("Block occurs in " +clientAddr+ ". Send frame time used: "+timeElapsed+" ms");
                    System.out.println("First gap: "+(t1-start)+" Second gap: "+(t2-t1)+" Third gap: "+(t3-t2)+" Fourth gap: "+(t4-t3)+" Final gap: "+(end-t4));
                }
	            long sendInterval = 15;
	            if (timeElapsed < sendInterval){
		            Thread.sleep(sendInterval-timeElapsed);
		        }
                //videoId++;
                //if(videoId>65535) videoId=0;
	        }

	        socket.close();
            System.out.println("Transmission Thread: client from: "+clientAddr+" has been closed.");
            
	    } catch (IOException | InterruptedException ex) {
            System.out.println("Transmission thread exception: " + ex.getMessage());
            ex.printStackTrace();
        }

/*
		try (ServerSocket serverSocket = new ServerSocket(PORT)) {
 
            System.out.println("Transmission thread ready, listening on port " + PORT);
 
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected, from: "+socket.getRemoteSocketAddress().toString());

                OutputStream output = socket.getOutputStream();
                //PrintWriter writer = new PrintWriter(output);
                DataOutputStream dOut = new DataOutputStream(output);

                //String testPos = "0,0";

                while(socket!=null){
                    //System.out.println("test");
                    //String curPos = getPos(testPos);
                    sendFrame(dOut,JavaServer.curPos);
                    Thread.sleep(1);
                }
                
 
                socket.close();
            }
 
        } catch (IOException | InterruptedException ex) {
            System.out.println("Transmission thread exception: " + ex.getMessage());
            ex.printStackTrace();
        }*/
	}

}