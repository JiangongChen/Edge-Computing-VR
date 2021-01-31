import java.lang.Thread;
import java.net.*;
import java.io.*;
import java.util.*;
import java.time.Instant;



public class FuncThread extends Thread {
	private Socket socket;
	private String clientAddr;
	//private Stats curStat;
	private FileWriter fwt;
	private String filename;
	private double oneWayDelayNoData;
    private int msgSize;
    private int clientNum;

	private ArrayList<String> videoACKReport; 
    private ArrayList<Long> timeACKReport; 

	public FuncThread(Socket sock){
		socket = sock;
		clientAddr = socket.getInetAddress().toString();
        while(!JavaServer.clientStats.containsKey(clientAddr)){
            try{
                Thread.sleep(1);
            }
            catch (InterruptedException e){

            }
        }
        clientNum = JavaServer.clientStats.get(clientAddr).clientNum;
		oneWayDelayNoData = 0.0;
        msgSize = 64;
		videoACKReport = new ArrayList<String>(); 
		timeACKReport = new ArrayList<Long>(); 
	}

	@Override
	public void run(){
        System.out.println("Functional Thread: New client connected, from: "+clientAddr);
		
		try {
			socket.setTcpNoDelay(true);
			
            InputStream input = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
            DataInputStream dIn = new DataInputStream(input);
            DataOutputStream dOut = new DataOutputStream(out);

            int n = 10;
            for(int i=0;i<n;i++){
            	long t1 = System.currentTimeMillis();
            	dOut.writeInt(1);
            	dIn.readInt();
            	long t2 = System.currentTimeMillis();
            	if (i>=1) oneWayDelayNoData += t2 - t1;
            }
            oneWayDelayNoData = oneWayDelayNoData/(2*(n-1));
            JavaServer.clientStats.get(clientAddr).oneWayDelay = oneWayDelayNoData;
            JavaServer.clientStats.get(clientAddr).delayReady = true;
            System.out.println(clientAddr+" one way delay without data: "+oneWayDelayNoData+"ms");


            while (socket != null){
            	try{
                    int recvSize = 0;
                    byte[] msgBytes = new byte[msgSize];
                    int length = 0;
                    // Receive displayed pose from the client
                    /*while (recvSize < msgSize)
                    {
                        length = input.read(msgBytes, 0, msgSize-recvSize);
                        if(length<0) break;
                        recvSize += length;
                    }
                    if(length<0) break;*/
                    String msg = dIn.readUTF();
                    long t = System.currentTimeMillis();
                    //String msg = new String(msgBytes);
                    videoACKReport.add(msg);
                    timeACKReport.add(t);
 
                }
                catch (IOException ex) {
                    break;
                }
            }
            socket.close();
        	//fwt.close();
        	System.out.println("Functional Thread: client from: "+clientAddr+" has been closed.");
        	reportStats();
        } catch (IOException e) {
        	System.out.println("Functional thread exception: " + e.getMessage());
            e.printStackTrace();
        }
	}

	private void reportStats(){
        System.out.println(clientAddr+" start writing the functional report");
        
        String fileName = null;
        if (clientAddr.equals(JavaServer.teacherAddr)){
            fileName = "teacher.txt";
        }
        else{
            fileName = "student"+clientNum+".txt";
        }
        try {
            String path = "report";
            File folder = new File(path);
            if (!folder.exists()) {
                System.out.print("No Folder");
                folder.mkdir();
                System.out.print("Folder created");
            }
            File file = new File("./report/"+fileName);
            file.createNewFile();
            FileWriter fwt = new FileWriter(file);
            for(int i=0; i< videoACKReport.size();i++){
                // minus the estimated one way RTT
                long dispTime = timeACKReport.get(i) - (long) oneWayDelayNoData;
                String dispPos = videoACKReport.get(i);
                fwt.write(dispTime + ", " + dispPos + " " + timeACKReport.get(i) + "\n");
                fwt.flush();
            }
            fwt.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Report of "+fileName+" has been done");
    
    }
}


	

