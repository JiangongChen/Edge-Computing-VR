import java.util.*;
// keep statistics for each client
public class Stats{
	public int clientNum;
	public static int nextNum = 0;
	public double oneWayDelay;
	public boolean delayReady = false;
	public HashMap<Integer, Long> videoSendTime = new HashMap<>(); 
    public HashMap<Integer, Long> videoACKTime = new HashMap<>(); 
    public ArrayList<Integer> videoACKReport = new ArrayList<Integer>(); 
    public ArrayList<Long> timeACKReport = new ArrayList<Long>(); 

    public Stats(){
    	clientNum = nextNum;
    }
}