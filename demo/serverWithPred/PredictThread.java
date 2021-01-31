import java.lang.Thread;
import java.net.*;
import java.io.*;
import java.time.Instant;
import java.util.*;

public class PredictThread extends Thread {

	BufferedReader reader;
    int predWind;
    int dim;
    int msgLen;
    ARModel arPosX;
    ARModel arPosY;
    ARModel arPosZ;
    ARModel arOriX;
    ARModel arOriY;
	//String curPos;

	PredictThread(){
		try{
            reader = new BufferedReader(new FileReader("./modiTrace.txt"));
        } catch (IOException e){
            e.printStackTrace();
        }
        predWind = 3;
        dim = 3;
        msgLen = 64;
        arPosX = new ARModel(predWind,dim);
        arPosY = new ARModel(predWind,dim);
        arPosZ = new ARModel(predWind,dim);
        arOriX = new ARModel(predWind,dim);
        arOriY = new ARModel(predWind,dim);
		//curPos = pos;
	}

	String getPos(String recvStr){
        String posX = recvStr.substring(0,recvStr.indexOf(",")).trim();
        String posY = recvStr.substring(recvStr.indexOf(",")+1).trim();
        String pos = "("+posX+","+posY+")";
        return pos;
    }


	@Override
	public void run(){
        try{
            // wait for functional thread ready
            Thread.sleep(500);
            System.out.println("Start playing the trace");
            String line = reader.readLine();
            while(line!=null){
                long t1 = System.currentTimeMillis();
                if(line.length()>msgLen) System.out.println("error");
                line = String.format("%"+msgLen+"s", line);

                // Split value from message and make prediction
                // Only predict orientation, use position of last time slot
                double posX = Float.parseFloat(line.split(",")[0]);
                double predPosX = posX;
                //double predPosX = arPosX.posStep(posX);
                double posY = Float.parseFloat(line.split(",")[1]);
                double predPosY = posY;
                //double predPosY = arPosY.posStep(posY);
                double posZ = Float.parseFloat(line.split(",")[2]);
                double predPosZ = arPosZ.posStep(posZ);
                //double predPosZ = arPosZ.posStep(posZ);
                double oriX = Float.parseFloat(line.split(",")[3]);
                double predOriX = arOriX.oriStep(oriX);
                double oriY = Float.parseFloat(line.split(",")[4]);
                double predOriY = arOriY.oriStep(oriY);
                
                String predLine = predPosX+","+predPosY+","+predPosZ+","+predOriX+","+predOriY+","+0;
                
                JavaServer.curPos = line;
                JavaServer.predPos = predLine;
                line = reader.readLine();

                long t2 = System.currentTimeMillis();
                int timePast = (int) (t2-t1);
                if (timePast < 10){
                    Thread.sleep(10-timePast);
                }
                else{
                    System.out.println("Predict Time used: "+timePast);
                }
            }

            line = String.format("%"+msgLen+"s", "trace end");
            JavaServer.curPos = line;
            //System.out.println(JavaServer.curPos);
            System.out.println("Trace end");
            reader.close();
            JavaServer.netFlag = false;
        } catch (IOException | InterruptedException e){
            e.printStackTrace();
        }
/*
		try (ServerSocket serverSocket = new ServerSocket(PORT)) {
		 
            System.out.println("Prediction thread ready, listening on port " + PORT);
 
            while (true) {
                Socket socket = serverSocket.accept();
                clientAddr = socket.getInetAddress().toString();
                System.out.println("Prediction thread: Teacher connected, from: "+clientAddr);
                JavaServer.teacherAddr = clientAddr;

                InputStream input = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                DataInputStream dIn = new DataInputStream(input);

                while (socket != null){
                	try{
                        int bytesRead = input.read(posRecvBytes, 0, posRecvBytes.length);
                        if(bytesRead==-1) break;
                        String curPos = getPos(new String(posRecvBytes));
                        JavaServer.curID++;
                        JavaServer.iDMap.put(JavaServer.curID,curPos);
                        //int posX = dIn.readInt();
                        //int posY = dIn.readInt();
                        //JavaServer.curPos = "("+posX+","+posY+")";
                        //System.out.println("Received pos: "+curPos+ " Time: "+Instant.now());
                    }
                    catch (IOException ex) {
                        break;
                    }
                }
                socket.close();
                System.out.println("Prediction Thread: client from: "+clientAddr+" has been closed.");
                //reportStats();
            }
        } catch (IOException ex) {
            System.out.println("Prediction thread exception: " + ex.getMessage());
            ex.printStackTrace();
        }*/
		 
	}
/*
    public void reportStats(){
        System.out.println("Start writing the report");
        int clientNum = 0;
        for(String curAddr : JavaServer.clientStats.keySet()){
            Stats curStats = JavaServer.clientStats.get(curAddr);
            String fileName = null;
            if (curAddr.equals(clientAddr)){
                fileName = "teacher.txt";
            }
            else{
                fileName = "student"+clientNum+".txt";
                clientNum++;
            }
            File file = new File("./report/"+fileName);
            try {
            file.createNewFile();
            FileWriter fwt = new FileWriter(file);
            for(int videoId : curStats.videoACKTime.keySet()){
                // TODO: minus the estimated one way RTT
                fwt.write(curStats.videoACKTime.get(videoId) + " " + videoId + "\n");
                fwt.flush();
            }
            fwt.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Report of "+fileName+" has been done");
        }
    }
*/
}