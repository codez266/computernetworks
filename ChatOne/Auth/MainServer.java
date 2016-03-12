import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;


public class MainServer {
	ServerSocket inSocket;
	Socket connectionSocket;
	private static int inPort = 2222;
	private static int outPort = 3780;
	ArrayList<String> ips;
	private static int maxClientsCount = 10;
	static ArrayList<User> users;
	static HashMap<String,ArrayList<String>> map;
	private static String datafile = "users.dat";
	ObjectOutputStream fileout;
	ObjectInputStream filein;
	ServerThread threads[] = new ServerThread[maxClientsCount];
	public static void main(String[] args) {
		MainServer ms = new MainServer( inPort );
		ms.startListen( ms.inSocket );
	}
	
	public MainServer( int soc ) {
		users = new ArrayList<User>();
		
		try {
			fileout = new ObjectOutputStream( new FileOutputStream( datafile ) );
			filein = new ObjectInputStream( new FileInputStream( datafile ) );
			inSocket = new ServerSocket( soc );
			map = (HashMap<String,ArrayList<String>>)filein.readObject();
			//if( map == null ) {
				map = new HashMap<String,ArrayList<String>>();
				fileout.writeObject( map );
			//}
			filein.close();
			fileout.close();
		} catch( IOException e ) {
			System.out.println( e.getMessage() );
		} catch( ClassNotFoundException e ) {
			System.out.println( e.getMessage() );
		}
	}
	
	/**
	 * Starts to accept connections on a socket
	 * @param inSocket
	 */
	public void startListen( ServerSocket soc ) {
		try {
			while(true) {
				connectionSocket = soc.accept();
				int i = 0;
				for ( i = 0;i < maxClientsCount;i++ ) {
					if ( threads[i] == null ) {
						(threads[i] = new ServerThread( connectionSocket )).start();
						break;
					}
				}
				
			}
		} catch( IOException e ) {
			System.out.println( e.getMessage() );
		}
	}
	
	class ServerThread extends Thread {
		Socket clientSocket;
		
		public ServerThread( Socket clientSocket ) {
			this.clientSocket = clientSocket;
		}
		
		public void run() {
			try {
				BufferedReader br = new BufferedReader( new InputStreamReader( clientSocket.getInputStream() ) );
				PrintWriter out = new PrintWriter(clientSocket.getOutputStream());
				while( true ) {
					String r;
					if ( (r = br.readLine()) != null ) {
						String m1 = r;
						String m2 = br.readLine();
						String m3 = br.readLine();
						String m4 = br.readLine();
						Message m = new Message( m1, m2, m3, m4 );
						//System.out.println(m1+" "+m2+" "+m3);
						ArrayList<String> resp = processMessage( m, clientSocket );
						for(String s:resp) {
							out.println(s);
						}
						out.flush();
					}
					br.close();
					out.close();
					clientSocket.close();
				}
			} catch ( IOException e ) {
				System.out.println( e.getMessage() );
			}
		}
		
		public ArrayList<String> processMessage( Message m, Socket soc ) {
			ArrayList<String> resp = new ArrayList<String>();
			if ( m.type.equals("1") ) {
				if ( isRegistered( m.name, m.pass, User.getIp(soc.getRemoteSocketAddress().toString()), m.port ) ) {
					resp.add("0");
					resp.add( "You are already registered" );
				} else {
					addUser( m.name, m.pass, User.getIp(soc.getRemoteSocketAddress().toString() ), m.port );
					resp.add("1");
					resp.add( "You have been registered successfully" );
					System.out.println("Registering "+m.name+"...");
				}
				
			} else if( m.type.equals("2") ) {
				if ( isRegistered( m.name, m.pass, User.getIp(soc.getRemoteSocketAddress().toString()), m.port ) ) {
					resp.add( "1" );
					resp.add( "You have been logged in" );
					System.out.println("Logging in "+m.name+"...");
					for ( User u:users ) {
						resp.add( u.name+"&"+u.ip+"&"+u.port );
						System.out.println(u.name+"&"+u.ip+"&"+u.port);
					}
				} else {
					resp.add( "0" );
					resp.add( "Failed to log in" );
				}
			} else if( m.type.equals( "3" ) ) {
				// currently returns all users
				for ( User u:users ) {
					resp.add( u.name+"&"+u.ip+"&"+u.port );
					System.out.println(u.name+"&"+u.ip+"&"+u.port);
				}	
			} else if( m.type.equals( "4" ) ) {
				if ( isRegistered( m.name, m.pass, User.getIp(soc.getRemoteSocketAddress().toString()), m.port ) ) {
					resp.add("0");
					resp.add( "Group is already registered" );
				} else {
					addUser( m.name, m.pass, User.getIp(soc.getRemoteSocketAddress().toString() ), m.port );
					resp.add("1");
					resp.add( "Group has been registered successfully" );
					System.out.println("Registering group"+m.name+"...");
					for ( User u:users ) {
						resp.add( u.name+"&"+u.ip+"&"+u.port );
						System.out.println(u.name+"&"+u.ip+"&"+u.port);
					}
				}	
			}
			return resp;
		}
		
		public boolean isRegistered( String name, String pass, String ip, String port ) {
			/*if ( map.containsKey( name ) ) {
				if ( map.get( name ).get(0).equals( pass ) ) {
					return true;
				}
			}*/
			for( User u:users ) {
				if ( u.name.equals(name) ) {
					return true;
				}
			}
			return false;
		}
		
		public void addUser( String name, String pass, String ip, String port ) {
			/*if ( !map.containsKey( name ) ) {
				ArrayList<String> val = new ArrayList<String>();
				val.add( pass );
				val.add( ip );
				map.put(name, val);
				try {
					fileout = new ObjectOutputStream( new FileOutputStream( datafile ) );
					fileout.writeObject( map );
				} catch ( IOException e ) {
					System.out.println( e.getMessage() );
				}
			}*/
			users.add( new User( name, pass, ip, Integer.parseInt(port) ) );
		}
	}
	
}
