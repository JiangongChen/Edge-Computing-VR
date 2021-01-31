import java.util.*;
// keep statistics for each client
public class Stats{
	public static int clientNum;
	public static HashMap<Integer, Long> videoSendTime = new HashMap<>(); 
    public static HashMap<Integer, Long> videoACKTime = new HashMap<>(); 
    public static ArrayList<Integer> videoACKReport = new ArrayList<Integer>(); 
    public static ArrayList<Long> timeACKReport = new ArrayList<Long>(); 
}