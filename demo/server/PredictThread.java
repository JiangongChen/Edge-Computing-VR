import java.lang.Thread;
import java.net.*;
import java.io.*;
import java.time.Instant;
import java.util.*;

public class PredictThread extends Thread {

	BufferedReader reader;
    int predWind;
    int msgLen;
	//String curPos;

	PredictThread(){
		try{
            reader = new BufferedReader(new FileReader("./modiTrace.txt"));
        } catch (IOException e){
            e.printStackTrace();
        }
        predWind = 20;
        msgLen = 64;
		//curPos = pos;
	}

	String getPos(String recvStr){
        String posX = recvStr.substring(0,recvStr.indexOf(",")).trim();
        String posY = recvStr.substring(recvStr.indexOf(",")+1).trim();
        String pos = "("+posX+","+posY+")";
        return pos;
    }

    double arPredict(ArrayList<Double> input){
        double result = 0.0;
        return result;
    }

	@Override
	public void run(){
        try{
            // wait for functional thread ready
            Thread.sleep(500);
            System.out.println("Start playing the trace");
            String line = reader.readLine();
            while(line!=null){
                if(line.length()>msgLen) System.out.println("error");
                line = String.format("%"+msgLen+"s", line);
                JavaServer.curPos = line;
                line = reader.readLine();
                Thread.sleep(10);
            }
            //System.out.println(JavaServer.curPos);
            line = String.format("%"+msgLen+"s", "trace end");
            JavaServer.curPos = line;
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