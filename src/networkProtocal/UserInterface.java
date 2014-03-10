package networkProtocal;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;


public class UserInterface {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		loginUI();
	}
	
	private static void loginUI() {
		JFrame frame = new JFrame("Login");
		frame.setSize(350, 200);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JPanel panel = new JPanel();
		frame.add(panel);
		panel.setLayout(null);
		
		JLabel userLabel = new JLabel("Username:");
		userLabel.setBounds(10,30,80,25);
		panel.add(userLabel);
		
		JTextField userText = new JTextField(20);
		userText.setBounds(100, 30, 160, 25);
		panel.add(userText);

		JLabel passwordLabel = new JLabel("Password: ");
		passwordLabel.setBounds(10, 70, 80, 25);
		panel.add(passwordLabel);

		JPasswordField passwordText = new JPasswordField(20);
		passwordText.setBounds(100, 70, 160, 25);
		panel.add(passwordText);

		JButton loginButton = new JButton("Login");
		loginButton.setBounds(10, 110, 80, 25);
		panel.add(loginButton);
		
		JButton createButton = new JButton("Create account");
		createButton.setBounds(140, 110, 140, 25);
		panel.add(createButton);
		
		frame.setVisible(true);
	}

}
