/**
 *  @author Sumit Asthana
 */
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;


public class ChatClient extends JFrame {
	
	private static String toIp;
	private int toPort, inPort;
	private static int authPort = 2222;
	private static String authIp = "127.0.0.1";
	BufferedReader inFromUser, inFromServer;
	Socket clientSocket, serverSocket;
	DataOutputStream outToServer;
	ChatPanel cp;
	ChatServer cs;
	ChatServer gs;
	boolean isLoggedIn = false;
	ArrayList<User> users;
	ArrayList<User> group;
	static int JUMP = 5000;
	Thread cst;// chat server thread
	Thread gst;// group server thread
	/**
	 * Initializes the ChatClient window
	 */
	public ChatClient() {
		users = new ArrayList<User>();
		group = new ArrayList<User>();
		setTitle( "ChatClient" );
		setSize( 1100, 600 );
		setLocation( 200, 200 );
		
		// Window Listeners
		addWindowListener(new WindowAdapter() {
		  	public void windowClosing(WindowEvent e) {
			   System.exit(0);
		  	} //windowClosing
		} );
		
		Container contentPane = getContentPane();
		cp = new ChatPanel( this );
		contentPane.add( cp );
		
		// hardcoding in port and to port, overridden in initiate chat
		inPort = 3780;
		toPort = 3781;
		cs = new ChatServer( this );
		gs = new ChatServer( this );
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
		    public void run() {
		    	JFrame f = new ChatClient();
		    	f.show();
		    }
		});
	}
	
	class ChatPanel extends JPanel implements ActionListener {
		private JButton send;
		private UserPanel up;
		private JTextArea chatArea, friends;
		private JTextField line;
		
		private String sendString = "Send";
		ChatClient chatClient;
		/**
		 * Initializes the root panel in the Chat window
		 */
		public ChatPanel( ChatClient parent ) {
			chatClient = parent;
			
			setLayout( new BorderLayout() );
			send = new JButton( sendString );
			//add( button );
			send.addActionListener( this );
			
			// for the bottom line + button
			JPanel bottomPanel = new JPanel();
			bottomPanel.setLayout( new BoxLayout( bottomPanel, BoxLayout.LINE_AXIS ) );
			
			up = new UserPanel( parent );
			
			chatArea = new JTextArea( 40, 40 );
			chatArea.setEditable( false );
			//chatArea.setMinimumSize( new Dimension( 70, 120 ) );
			
			line = new JTextField( 60 );
			bottomPanel.add( line );
			bottomPanel.add(Box.createRigidArea(new Dimension(10, 0)));
			bottomPanel.add( send );
			
			JPanel leftPane = new JPanel( new BorderLayout( 10, 10 ) );
			//leftPane.setPreferredSize( new Dimension( 800, 600 ) );
			leftPane.add( up, BorderLayout.PAGE_START );
			leftPane.add(chatArea,
                    BorderLayout.CENTER);
			leftPane.add( bottomPanel, BorderLayout.PAGE_END );
			leftPane.setBorder( BorderFactory.createEmptyBorder(10,10,10,10) );
			
			JPanel rightPane = new JPanel( new BorderLayout( 10, 10 ) );
			friends = new JTextArea( 45, 20 );
			friends.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder("Friends"),
                    BorderFactory.createEmptyBorder(10,10,10,10)));
			rightPane.add( friends, BorderLayout.PAGE_START );
			
			add( leftPane, BorderLayout.LINE_START );
			add( rightPane, BorderLayout.LINE_END );
		}
		
		/**
		 * {@inheritDoc}
		 */
		public void actionPerformed( ActionEvent e ) {
			if ( e.getSource() == send ) {
				String mesg = line.getText();
				chatClient.sendMessage( mesg );
			}
		}
		
		public UserPanel getUserPanel() {
			return up;
		}
		
		public JTextArea getChatArea() {
			return chatArea;
		}
		
		public JTextArea getFriends() {
			return friends;
		}
	}
	
	public void initiateChat() {
		cs.initializeSocket( Integer.parseInt(cp.up.getPort()) );
		if ( cst == null ) {
			cst = new Thread( cs );
			cst.start();
		} else {
			cst.stop();
			cst = new Thread();
			cst.start();
		}
	}
	
	public void initiateGroupChat() {
		gs.initializeSocket( Integer.parseInt(cp.up.getPort()) + JUMP );
		if ( gst == null ) {
			gst = new Thread( gs );
			gst.start();
		} else {
			gst.stop();
			gst = new Thread();
			gst.start();
		}
	}
	
	public void sendMessage( String text ) {
		try {
			Socket conn = new Socket(cp.up.getIpAddr(), cp.up.getToPort()); 
			outToServer = new DataOutputStream(conn.getOutputStream());
			outToServer.writeBytes( "<" + cp.up.getName() + ":" + cp.up.getPort() + ">:" + text + '\n' );
			cp.getChatArea().append( "To - <" + cp.up.getIpAddr() + ":" + cp.up.getToPort() + ">:" + text + "\n" );
			
			closeChat( conn );
		} catch( IOException e ) {
			System.out.println(e.getMessage());
			//System.out.println( toIp + ' ' + toPort );
		}
	}
	
	public void sendMessageTo( String text, User u ) {
		try {
			Socket conn = new Socket(u.ip, u.port); 
			outToServer = new DataOutputStream(conn.getOutputStream());
			outToServer.writeBytes( "<" + cp.up.getName() + ":" + cp.up.getPort() + ">:" + text + '\n' );
			cp.getChatArea().append( "To - <" + cp.up.getName() + ":" + cp.up.getPort() + ">:" + text + "\n" );
			closeChat( conn );
		} catch( IOException e ) {
			System.out.println(e.getMessage());
			//System.out.println( toIp + ' ' + toPort );
		}
	}
	
	public void closeChat( Socket conn ) {
		if ( conn != null ) {
			try {
				conn.close(); 
			} catch( IOException e ) {
				System.out.println(e.getMessage());
			}
		}
	}
	
	public void register( String name, String pass ) {
		System.out.println("trying register...");
		try {
			Socket conn = new Socket( cp.up.getAuthIp(), authPort ); 
			PrintWriter out = new PrintWriter(conn.getOutputStream());
			out.println( "1" );
			out.println(name);
			out.println(pass);
			out.println( cp.up.getPort() );
			out.flush();
			BufferedReader br = new BufferedReader( new InputStreamReader( conn.getInputStream() ) );
			int stat = Integer.parseInt(br.readLine());
			String mesg = br.readLine();
			System.out.println(mesg);
			cp.up.getStatusLabel().setText( mesg );
			out.close();
			br.close();
			//conn.close();
		} catch( IOException e ) {
			System.out.println( e.getMessage() );
		}
	}
	
	public void login( String name, String pass, ChatClient cl ) {
		System.out.println("trying login...");
		ArrayList<User> userList = new ArrayList<User>();
		Thread readStdIn = new Thread(new Runnable(){
			public void run() {
				try {
					Socket conn = new Socket( cp.up.getAuthIp(), authPort ); 
					PrintWriter out = new PrintWriter(conn.getOutputStream());
					out.println( "2" );
					out.println(name);
					out.println(pass);
					out.println( cp.up.getPort() );
					out.flush();
					BufferedReader br = new BufferedReader( new InputStreamReader( conn.getInputStream() ) );
					int stat = Integer.parseInt(br.readLine());
					String mesg = br.readLine();
					System.out.println(mesg);
					cp.up.getStatusLabel().setText( mesg );
					if ( stat == 1 ) {
						isLoggedIn = true;
						cl.initiateChat();
						String friend;
						/*while((friend=br.readLine())!=null) {
							String str[] = friend.split("&");
							System.out.println(str[0]+" "+str[1]+" "+str[2]);
							userList.add( new User( str[0], "", str[1], Integer.parseInt(str[2]) ) );
						}
						cl.updateFriends(userList);*/
						createGroup( name, pass, cl );
					}
					out.close();
					br.close();
					conn.close();
				} catch( IOException e ) {
					System.out.println( e.getMessage() );
				}
			}
		});
		readStdIn.start();
	}
	
	public void createGroup(String name, String pass, ChatClient cl) {
		System.out.println("Creating group...");
		ArrayList<User> userList = new ArrayList<User>();
		Thread readStdIn = new Thread(new Runnable(){
			public void run() {
				try {
					Socket conn = new Socket( cp.up.getAuthIp(), authPort ); 
					PrintWriter out = new PrintWriter(conn.getOutputStream());
					out.println( "4" );
					out.println(name+"(group)");
					out.println(pass);
					out.println( Integer.parseInt(cp.up.getPort()) + JUMP );
					out.flush();
					BufferedReader br = new BufferedReader( new InputStreamReader( conn.getInputStream() ) );
					int stat = Integer.parseInt(br.readLine());
					String mesg = br.readLine();
					System.out.println(mesg);
					//cp.up.getStatusLabel().setText( mesg );
					if ( stat == 1 ) {
						isLoggedIn = true;
						cl.initiateGroupChat();
						String friend;
						while((friend=br.readLine())!=null) {
							String str[] = friend.split("&");
							System.out.println(str[0]+" "+str[1]+" "+str[2]);
							userList.add( new User( str[0], "", str[1], Integer.parseInt(str[2]) ) );
						}
						cl.updateFriends(userList);
					}
					out.close();
					br.close();
					conn.close();
				} catch( IOException e ) {
					System.out.println( e.getMessage() );
				}
			}
		});
		readStdIn.start();
	}
	
	public void requestFriends(String name, String pass, ChatClient cl) {
		System.out.println("getting friends...");
		users = new ArrayList<User>();
		Thread readStdIn = new Thread(new Runnable(){
			public void run() {
				try {
					Socket conn = new Socket( cp.up.getAuthIp(), authPort ); 
					PrintWriter out = new PrintWriter(conn.getOutputStream());
					out.println( "3" );
					out.println(name);
					out.println(pass);
					out.println( cp.up.getPort() );
					out.flush();
					BufferedReader br = new BufferedReader( new InputStreamReader( conn.getInputStream() ) );
					
					String friend;
					while((friend=br.readLine())!=null) {
						String str[] = friend.split("&");
						System.out.println(str[0]+" "+str[1]+" "+str[2]);
						users.add( new User( str[0], "", str[1], Integer.parseInt(str[2]) ) );
					}
					System.out.println("1");
					
					
					out.close();
					br.close();
					conn.close();
				} catch( IOException e ) {
					System.out.println( e.getMessage() );
				} finally {
					cl.updateFriends(users);
					System.out.println("2");
				}
			}
		});
		readStdIn.start();
	}
	
	public User getUser( String name ) {
		for( User u:users ) {
			if ( u.name.equals( name ) ) {
				return u;
			}
		}
		return null;
	}
	
	public void updateFriends( ArrayList<User> ulist ) {
		cp.friends.setText("");
		System.out.println(ulist.size());
		users = ulist;
		cp.up.cb.removeAllItems();
		for( User u:users ) {
			System.out.println(u.name);
			cp.getFriends().append(u.name+"\n");
			cp.up.cb.addItem( u.name );
			System.out.println("3");
		}
	}
	
	public void addUserToGroup( String name ) {
		User u = getUser( name );
		if ( u != null ) {
			group.add( u );
			System.out.println("Added "+name);
		}
	}
	
	public void sendMessageToGroup( String message ) {
		for(User u:group) {
			sendMessageTo( message, u );
		}
	}
}
