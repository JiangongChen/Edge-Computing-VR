import java.net.*;
import java.io.*;
import java.lang.Thread;

public class SimpleServer {
	public static volatile boolean netFlag = true;
    public static volatile String curPos = null;

    public static int msgLen = 64;
    public static String teacherAddr = null;
	public static void main(String[] args) {
 
        final int TranPort = 8080;
        final int FuncPort = 8000;

		ServerThread funcServerThread = new ServerThread(FuncPort);
        funcServerThread.start();

        ServerSocket serverSocket = null;
        Socket socket = null;
        int clientNum = 0;
        
        try {
            serverSocket = new ServerSocket(TranPort);
            System.out.println("Server ready, listening on port " + TranPort);
        } catch (IOException e) {
            e.printStackTrace();
        }

        while (true) {
            try {
                socket = serverSocket.accept();
            } catch (IOException e) {
                System.out.println("I/O error: " + e);
            }
            // start play the trace 
            if (clientNum == 0){
            	teacherAddr = socket.getInetAddress().toString();
        		new TraceThread().start();
            }
            // new thread for a client
            clientNum++;
            new TranThread(socket, clientNum).start();
        }
    }

	public static class ServerThread extends Thread {
        private int PORT;

        public ServerThread(int port){
            PORT = port;
        }

        @Override
        public void run(){
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
             
                System.out.println("Functional thread ready, listening on port " + PORT);
     			int num = 0;
                while (true) {
                    Socket socket = serverSocket.accept();
                    FuncThread funcThread = new FuncThread(socket,num);
                    funcThread.start();
                    num++;
                }
            } catch (IOException ex) {
                System.out.println("Functional thread exception: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    public static class TranThread extends Thread {
    	private Socket socket;
	    private String clientAddr;
		private int index;
	    private byte[] sendBuf;
	    private int bufSize;
		private OutputStream output;
		TranThread(Socket sock, int num){
			socket = sock;
			index = num;
	        bufSize = 64;
	        sendBuf = new byte[bufSize];
	        clientAddr = socket.getInetAddress().toString();
		}

		@Override
		public void run(){
			System.out.println("Tran Thread: New client connected, from: "+clientAddr);
			try{
	            socket.setTcpNoDelay(true);
		        output = socket.getOutputStream();

		        while(!socket.isClosed()&&SimpleServer.netFlag){

		            long startTime = System.currentTimeMillis();
	                while(true){
	                    String posToDeliver = SimpleServer.curPos;
	                    if (posToDeliver!= null && posToDeliver.length()==msgLen){
	                    	byte[] msg =  posToDeliver.getBytes();
	                        output.write(msg);
	                        break;
	                    }
	                    else{
	                        Thread.sleep(1);
	                    }
	                }
		            long endTime = System.currentTimeMillis();

		            long timeElapsed = endTime - startTime;

	                if (timeElapsed >= 15 ){
	                    System.out.println("Block occurs in " +clientAddr+ ". Send frame time used: "+timeElapsed+" ms");
	                }
		            long sendInterval = 15;
		            if (timeElapsed < sendInterval){
			            Thread.sleep(sendInterval-timeElapsed);
			        }
		        }

		        socket.close();
	            System.out.println("Transmission Thread: client from: "+clientAddr+" has been closed.");
            } catch (IOException | InterruptedException e){
            	System.out.println(clientAddr+" error occurs");
            }
		}
    }

    public static class TraceThread extends Thread {
    	BufferedReader reader;
    	TraceThread(){
    		try{
    			reader = new BufferedReader(new FileReader("./modiTrace.txt"));
    		} catch (IOException e){
    			e.printStackTrace();
    		}
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
    				SimpleServer.curPos = line;
    				line = reader.readLine();
    				Thread.sleep(10);
    			}
    			System.out.println(SimpleServer.curPos);
    			System.out.println("Trace end");
    			reader.close();
    			netFlag = false;
    		} catch (IOException | InterruptedException e){
    			e.printStackTrace();
    		}
    	}
    }
}