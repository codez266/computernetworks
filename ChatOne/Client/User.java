import java.util.ArrayList;


public class User {
	String name, ip, pass;
	int port;
	ArrayList<String> friends;
	
	public User( String name, String pass, String ip, int port ) {
		this.name = name;
		this.ip = ip;
		this.port = port;
		this.pass = pass;
		friends = new ArrayList<String>();
	}
	
	/**
	 * Extracts ip part from whole address
	 * @param addr
	 */
	public static String getIp( String addr ) {
		return addr.substring( 1, addr.indexOf(":") );
	}
}
