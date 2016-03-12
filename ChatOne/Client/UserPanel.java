import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

/**
 *  @author Sumit Asthana
 */
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SpringLayout;

class UserPanel extends JPanel implements ActionListener {
		private JPanel textPane;
		private String username = "Username";
		private JTextField userField, ipAddr, port, authIp;
		private JPasswordField passField;
		private String password = "Password";
		private String address = "Chat With:";
		private String portStr = "Port";
		private String authStr = "Auth IP:";
		JButton login, signup, refresh;
		JLabel status;
		ChatClient chatClient;
		JComboBox<String> cb;
		public UserPanel( ChatClient parent ) {
			setLayout( new BoxLayout( this, BoxLayout.PAGE_AXIS ) );
			setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder("User Pane"),
                    BorderFactory.createEmptyBorder(5,5,5,5)));
			
			textPane = new JPanel();
			textPane.setLayout( new SpringLayout() );
			
			JLabel l = new JLabel( this.username, JLabel.TRAILING );
			textPane.add( l );
			
			userField = new JTextField( 10 );
			l.setLabelFor( userField );
			textPane.add( userField );
			
			l = new JLabel( this.address, JLabel.TRAILING );
			textPane.add( l );
			
			/*ipAddr = new JTextField( 10 );
			l.setLabelFor( ipAddr );
			textPane.add( ipAddr );*/
			cb = new JComboBox<String>();
			textPane.add( cb );
			
			l = new JLabel( this.password, JLabel.TRAILING );
			textPane.add( l );
			
			passField = new JPasswordField( 10 );
			l.setLabelFor( passField );
			textPane.add( passField );
			
			l = new JLabel( this.portStr, JLabel.TRAILING );
			textPane.add( l );
			
			port = new JTextField( 10 );
			l.setLabelFor( port );
			port.setText("3780");
			textPane.add( port );
			
			status = new JLabel( "Not Logged In" );
			add( status );
			
			l = new JLabel( authStr );
			authIp = new JTextField();
			l.setLabelFor(authIp);
			authIp.setText( "127.0.0.1" );
			textPane.add( l );
			textPane.add( authIp );
			
			l = new JLabel("");
			textPane.add(l);
			l = new JLabel("");
			textPane.add(l);
			
			//Lay out the panel.
			SpringUtilities.makeCompactGrid(textPane,
			                                3, 4, //rows, cols
			                                6, 6,        //initX, initY
			                                6, 6);       //xPad, yPad
			add( textPane );
			//add(Box.createHorizontalGlue());
			
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout( new BoxLayout( buttonPane, BoxLayout.LINE_AXIS ) );
			
			login = new JButton( "Login" );
			login.addActionListener( this );
			signup = new JButton( "Signup" );
			signup.addActionListener( this );
			refresh = new JButton( "refresh" );
			refresh.addActionListener( this );
			add(Box.createRigidArea( new Dimension( 0, 5 ) ) );
			buttonPane.add(Box.createHorizontalGlue());
			buttonPane.add( login );
			buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
			buttonPane.add( signup );
			buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
			buttonPane.add( refresh );
			add( buttonPane );
			
			chatClient = parent;
		}
		
		public void actionPerformed( ActionEvent e ) {
			if ( e.getSource() == refresh  ) {
				chatClient.requestFriends(getName(), getPass(), chatClient);
			} else if ( e.getSource() == signup ) {
				String name = getName();
				String pass = getPass();
				chatClient.register( name, pass );
			} else if( e.getSource() == login ) {
				chatClient.login( getName(), getPass(), chatClient );
			}
		}
		
		public String getIpAddr() {
			return (chatClient.getUser((String)cb.getSelectedItem())).ip;
		}
		
		public int getToPort() {
			return (chatClient.getUser((String)cb.getSelectedItem())).port;
		}
		
		public String getPort() {
			return port.getText();
		}
		
		public String getName() {
			return userField.getText();
		}
		
		public String getPass() {
			return passField.getText();
		}
		
		public JLabel getStatusLabel() {
			return status;
		}
		
		public String getAuthIp() {
			return authIp.getText();
		}
	}