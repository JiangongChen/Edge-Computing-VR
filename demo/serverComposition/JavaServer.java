import java.io.*;
import java.net.*;
import java.util.HashMap; 
import java.nio.file.Files;

/**
 * This program demonstrates a simple TCP/IP socket server that echoes every
 * message from the client in reversed form.
 * This server is single-threaded.
 *
 */
public class JavaServer {

    // Create an empty hash map 
    private static HashMap<String, byte[]> map = new HashMap<>(); 
    public static HashMap<String, Stats> clientStats = new HashMap<>(); 
    // public static HashMap<Integer, String> iDMap = new HashMap<>();

    //public static volatile String curPos;
    public static volatile String curPos = null;
    public static volatile String predPos = null;
    public static String teacherAddr = null;
    public static boolean netFlag = true;

    static void prepareBuffer(){
        File folder = new File("cache_frame");
        File[] listOfFiles = folder.listFiles();
        for (File f : listOfFiles){
            String fileName = f.getName();
            byte[] curFrame = null;
            try{
                curFrame = Files.readAllBytes(f.toPath());
            }catch (IOException e) {
                System.out.println("Frame Buffer exception: " + e.getMessage());
                e.printStackTrace();
            }
            String tempPos = fileName.substring(fileName.indexOf("("),fileName.indexOf(")")+1);
            map.put(tempPos,curFrame);
        }
    }



    public static void main(String[] args) {
 
        final int FramePort = 8888;
        final int FuncPort = 8000;

        prepareBuffer();
        System.out.println("Buffer Ready");


        ServerThread funcServerThread = new ServerThread(FuncPort);
        funcServerThread.start();

        ServerSocket serverSocket = null;
        Socket socket = null;

        try {
            serverSocket = new ServerSocket(FramePort);
            System.out.println("Server ready, listening on port " + FramePort);
        } catch (IOException e) {
            e.printStackTrace();
        }

        while (true) {
            try {
                socket = serverSocket.accept();
                clientStats.put(socket.getInetAddress().toString(),new Stats());
                if (Stats.nextNum == 0){
                    teacherAddr = socket.getInetAddress().toString();
                    PredictThread predThread = new PredictThread();
                    predThread.start();
                }
                Stats.nextNum++;
            } catch (IOException e) {
                System.out.println("I/O error: " + e);
            }
            // new thread for a client
            new TranThread(map, socket).start();
        }

        //TranThread tranThread = new TranThread(map, FramePort);
        //tranThread.start();

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
                while (true) {
                    Socket socket = serverSocket.accept();
                    FuncThread funcThread = new FuncThread(socket);
                    funcThread.start();
                }
            } catch (IOException ex) {
                System.out.println("Functional thread exception: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }
}