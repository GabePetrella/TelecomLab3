package networkProtocal;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Scanner;

public class Client {
	private static Scanner cli = new Scanner(System.in);
	public static Socket socket;
	private static final int PORT = 5001;
	private static final String HOSTNAME = "dsp2014.ece.mcgill.ca";
	public final static int TOTAL_HEADER_SIZE = 12;
	public static final int HEADER_SIZE = 4;
	static Thread autoQuery;
	public static String username;
	public static String password;
	
	public enum MessageType {
		EXIT(0),
		BADLY_FORMATTED_MESSAGE(1),
		ECHO(2),
		LOGIN(3),
		LOGOFF(4),
		CREATE_USER(5),
		DELETE_USER(6),
		CREATE_STORE(7),
		SEND_MESSAGE(8),
		QUERY_MESSAGES(9);
		
		private int messageType;
		
		private MessageType(int value){
			messageType = value;
		}
		
		public int getMessageType(){
			return messageType;
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("Connecting to server...");
		try {
			socket = new Socket(HOSTNAME, PORT);
			System.out.println("Connection established with " + HOSTNAME);
			
			initialMenu();
			
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * gives the user the initial options of either logging in, creating a new user or exiting
	 * @throws IOException
	 */
	private static void initialMenu() throws IOException{
		int userSelection;
		System.out.println("Please select one of the following options:\n" +
				"\t\t1 - Login\n" +
				"\t\t2 - Create Account\n" +
				"\t\t3 - exit");
		userSelection = cli.nextInt();
		
		if(userSelection == 1){
			login();
//			autoQuery = new Thread(new AutoQuery());
//			autoQuery.start();
		}
		else if (userSelection == 2){
			createUser();
		}
		else if (userSelection == 3){
			exit();
		}
	}
	
	/**
	 * Disconnects the connection from the server
	 * @throws IOException
	 */
	private static void exit() throws IOException {
//		int messageType = MessageType.EXIT.getMessageType();
//		int subMessage = 0;
//		String message = " ";
//		
//		ArrayList<byte[]> request, response;
//		
//		request = convertToArrayList(messageType,subMessage,message);
//		response = sendRequest(request.get(0), request.get(1), request.get(2), request.get(3));
//		System.out.println(byteArrayToString(response.get(3)));
		
		System.out.println("Exiting, goodbye!");
		
		socket.close();
		cli.close();
		System.exit(0);
	}

	
	private static void createUser() throws IOException {
		System.out.print("You have chosen to create a new user.\n" +
				"Please enter the credentials you wish to use.\n" +
				"Username:");
		if(cli.hasNext()){
			username = cli.next();
		}
		System.out.print("Password:");
		if (cli.hasNext()){
			password = cli.next();
		}
		
		//Send proper request to the server
		int messageType = MessageType.CREATE_USER.getMessageType();
		int subMessage = 0;
		String message = username + "," + password;
		
		ArrayList<byte[]> request = convertToArrayList(messageType,subMessage,message);
		
		ArrayList<byte[]> response = sendRequest(request.get(0), request.get(1), request.get(2), request.get(3));
		
		//Based on server response offer correct corresponding options to user
		subMessage = byteArrayToInt(response.get(1));
		System.out.println(byteArrayToString(response.get(3)));
		
		if(subMessage == 2){
			//go to logged in menu
		}else {
			//go back to the initial menu
			initialMenu();
		}
	}
	
	/**
	 * Attempts to log the user into the server
	 * @throws IOException
	 */
	private static void login() throws IOException{
		//Ask for user input for username and password
		System.out.print("Please enter your credentials.\nUsername:");
		if(cli.hasNext()){
			username = cli.next();
		}
		System.out.print("Password:");
		if (cli.hasNext()){
			password = cli.next();
		}
		
		//Send proper request to the server
		int messageType = MessageType.LOGIN.getMessageType();
		int subMessage = 0;
		String message = username + "," + password;
		
		ArrayList<byte[]> request = convertToArrayList(messageType,subMessage,message);
		
		ArrayList<byte[]> response = sendRequest(request.get(0), request.get(1), request.get(2), request.get(3));
		
		//Based on server response offer correct corresponding options to user
		subMessage = byteArrayToInt(response.get(1));
		System.out.println(byteArrayToString(response.get(3)));
		
		if (subMessage == 0 || subMessage == 1){
			
		}
		else if(subMessage == 2 || subMessage == 3){
			login();
		}
		
	}
	
	/**
	 * Sends a request to the server and returns the server's response.
	 * @param messageType
	 * @param subMessage
	 * @param size
	 * @param data
	 * @return
	 * @throws IOException
	 */
	private static ArrayList<byte[]> sendRequest (byte[] messageType, byte[] subMessage, byte[] size, byte[] data) throws IOException {
		byte[] request = concatenateByteArrays(messageType, subMessage, size, data);
		byte[] totalHeaderBytes = new byte[TOTAL_HEADER_SIZE];
		byte[] sizeHeader = new byte[HEADER_SIZE];
		byte[] message, sub;
		
		int availableBytes;
		int messageSize;
		InputStream fromServer = null;
		ArrayList<byte[]> serverResponse = new ArrayList<byte[]>();
		
		//Output the request to the server
		OutputStream sendToServer = socket.getOutputStream();
		DataOutputStream send = new DataOutputStream(sendToServer);
		send.write(request);
		
		//Make sure to receive all the headers
		availableBytes = 0;
		while(availableBytes < TOTAL_HEADER_SIZE){
			fromServer = socket.getInputStream();
			availableBytes = fromServer.available();
		}
		
		//figure out size of payload message so that buffer can be allocated accordingly
		fromServer.read(totalHeaderBytes, 0, TOTAL_HEADER_SIZE);
		System.arraycopy(totalHeaderBytes, HEADER_SIZE*2, sizeHeader, 0, HEADER_SIZE);
		messageSize = ByteBuffer.wrap(sizeHeader).getInt();
		
		//read the payload message from the socket
		message = new byte[messageSize];
		fromServer.read(message, 0, messageSize);
		
		sub = new byte[HEADER_SIZE];
		System.arraycopy(totalHeaderBytes, HEADER_SIZE, sub, 0, HEADER_SIZE);
		
		//store each of the headers and the message as a separate component of an arrayList<byte[]>
		serverResponse.add(0, messageType);
		serverResponse.add(1, sub);
		serverResponse.add(2, sizeHeader);
		serverResponse.add(3, message);
		
		return serverResponse;
	}
	
	/**
	 * Checks for new messages from the server every 5 seconds
	 */
	private static class AutoQuery implements Runnable{
		@Override
		public void run(){
			
			while(true){
				try {
					
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
	}
	
	/**
	 * helper method to convert from integer to byte array
	 * @param i
	 * @return
	 */
	private static byte[] intToByteArray(int integer){
		byte[] intInBytes = ByteBuffer.allocate(4).putInt(integer).array();
		return intInBytes;
	}
	
	/**
	 * helper method to convert from byte array to integer
	 * @param bytes
	 * @return
	 */
	private static int byteArrayToInt(byte[] bytes) {
		int myInt = ByteBuffer.wrap(bytes).getInt();
		return myInt;
	}
	
	private static byte[] stringToByteArray (String s){
		byte[] stringInBytes = s.getBytes();
		return stringInBytes;
	}
	
	private static String byteArrayToString (byte[] bytes){
		String s = new String(bytes);
		return s;
	}
	
	/**
	 * Helper method to concatenate byte arrays into 1 request
	 * @param messageType
	 * @param subMessage
	 * @param size
	 * @param data
	 * @return
	 */
	private static byte[] concatenateByteArrays(byte[] messageType, byte[] subMessage, byte[] size, byte[] data){
		byte[] total = new byte[TOTAL_HEADER_SIZE + data.length];
		
		System.arraycopy(messageType,0,total,0,HEADER_SIZE);
		System.arraycopy(subMessage,0,total,HEADER_SIZE,HEADER_SIZE);
		System.arraycopy(size,0,total,HEADER_SIZE*2,HEADER_SIZE);
		System.arraycopy(data,0,total,TOTAL_HEADER_SIZE,data.length);
		
		return total;
	}
	
	/**
	 * Helper method to store protocal parameters as an array list
	 * @param messageType
	 * @param subMessage
	 * @param message
	 * @return
	 */
	private static ArrayList<byte[]> convertToArrayList(int messageType, int subMessage, String message){
		ArrayList<byte[]> list = new ArrayList<byte[]>();
		
		byte[] type = intToByteArray(messageType);
		byte[] sub = intToByteArray(subMessage);
		byte[] data = stringToByteArray(message);
		byte[] size = intToByteArray(data.length);
		
		list.add(0, type);
		list.add(1, sub);
		list.add(2, size);
		list.add(3, data);
		
		return list;
	}
	
	
}
