import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;


public class ChatServer implements Runnable {
	ServerSocket inSocket;
	Socket connectionSocket;
	ChatClient client;
	int listenPort;
	/**
	 * Server program that continuously listens on a socket
	 * @param socket Socket for communicating
	 * @param cl ChatClient object
	 */
	public ChatServer( ChatClient cl ) {
		client = cl;
	}
	
	public void initializeSocket( int soc ) {
		try {
			inSocket = new ServerSocket( soc );
			listenPort = soc;
		} catch( IOException e ) {
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
				BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream())); 
				String clientSentence = inFromClient.readLine();
				messageReceived( clientSentence );
			}
		} catch( IOException e ) {
			
		}
	}
	
	/**
	 * Things todo on a message receive
	 * @param message
	 */
	public void messageReceived( String message ) {
		String content = message.substring( message.indexOf( ">" ) + 2 );
		// a command, so process it
		if ( content.startsWith( "/" ) ) {
			String command = content.substring( content.indexOf( "/" ) + 1 );
			String data = message.substring( 1, message.indexOf( ">" ) );
			evaluateCommand( command, data.substring( 0, data.indexOf( ":" ) ) );
			return;
		}
		client.cp.getChatArea().append( message + '\n' );
		if ( listenPort == Integer.parseInt( client.cp.getUserPanel().getPort() ) + ChatClient.JUMP ) {
			System.out.println("group message called");
			client.sendMessageToGroup( message );
		}
	}
	
	public void evaluateCommand( String command, String data ) {
		if ( command.equals( "join" ) ) {
			System.out.println( "join called by "+data );
			client.addUserToGroup( data );
		}
	}
	
	public void run() {
		startListen( inSocket );
	}
}
