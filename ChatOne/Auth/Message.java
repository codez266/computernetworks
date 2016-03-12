
public class Message {
	/**
	 * 1. Signup
	 * 2. Login
	 * 3. add friend
	 * 
	 */
	String type;
	String name, pass, port;
	public Message( String type, String name, String pass, String port ) {
		this.type = type;
		this.name = name;
		this.pass = pass;
		this.port = port;
	}
}
