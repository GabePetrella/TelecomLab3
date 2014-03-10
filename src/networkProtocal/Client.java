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
	private static Socket socket;
	private static final int PORT = 5001;
	private static final String HOSTNAME = "dsp2014.ece.mcgill.ca";
	private final static int TOTAL_HEADER_SIZE = 12;
	private static final int HEADER_SIZE = 4;
	static Thread autoQuery;
	static volatile boolean threadFlag = true;
	
	private enum MessageType {
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
			
		} catch(IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * gives the user the initial options of either logging in, creating a new user or exiting
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	private static void initialMenu() throws IOException, InterruptedException{
		int userSelection;
		System.out.println("\n\nPlease select one of the following options:\n" +
				"\t\t1 - Login\n" +
				"\t\t2 - Create Account\n" +
				"\t\t3 - exit");
		userSelection = Integer.parseInt(cli.next());
		
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
	 * @throws InterruptedException 
	 */
	private static void exit() throws IOException, InterruptedException {
		int messageType = MessageType.EXIT.getMessageType();
		int subMessage = 0;
		String message = " ";
		
		ArrayList<byte[]> request, response;
		
		request = convertToArrayList(messageType,subMessage,message);
		response = sendRequest(request.get(0), request.get(1), request.get(2), request.get(3));
		System.out.println(byteArrayToString(response.get(3)));
		
		System.out.println("Exiting, goodbye!");
		
		socket.close();
		cli.close();
		System.exit(0);
	}

	/**
	 * Creates a new user 
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	private static void createUser() throws IOException, InterruptedException {
		String username = "";
		String password = "";
		
		System.out.print("\n\nYou have chosen to create a new user.\n" +
				"Please enter the credentials you wish to use. Keep to single word with alphanumeric characters only.\n" +
				"Username:");
		
		if(cli.hasNext()){
			username = cli.next();
		}
		
		//make sure username doesn't have a comma
		while(username.contains(",")){
			System.out.print("Username CANNOT contain a comma!\n" +
					"Enter a new username:");
			username = cli.next();
		}
		
		System.out.print("Password:");
		if (cli.hasNext()){
			password = cli.next();
		}
		
		//Send proper request to the server
		int messageType = MessageType.CREATE_USER.getMessageType();
		int subMessage = 10;
		String message = username + "," + password;
		
		ArrayList<byte[]> request = convertToArrayList(messageType,subMessage,message);
		ArrayList<byte[]> response = new ArrayList<byte[]>();
		
		do {
			response = sendRequest(request.get(0), request.get(1), request.get(2), request.get(3));
			
			messageType = byteArrayToInt(response.get(0));
			subMessage = byteArrayToInt(response.get(1));
		}while(messageType != MessageType.CREATE_USER.getMessageType() || (subMessage != 0 && subMessage != 1 && subMessage != 2 && subMessage != 3));
		
		//Based on server response offer correct corresponding options to user
		System.out.println(byteArrayToString(response.get(3)));
		
		if(subMessage == 0){
			initialMenu();
		}
		else if(subMessage == 2){
			//go to logged in menu
			alreadyLoggedIn();
		}else {
			//go back to the initial menu
			initialMenu();
		}
	}
	
	/**
	 * Attempts to log the user into the server
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	private static void login() throws IOException, InterruptedException{
		//Ask for user input for username and password
		String username = "";
		String password = "";
		
		System.out.print("\nPlease enter your credentials.\nUsername:");
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
		ArrayList<byte[]> response = new ArrayList<byte[]>();
		
		do{
			response= sendRequest(request.get(0), request.get(1), request.get(2), request.get(3));
		
			//Based on server response offer correct corresponding options to user
			messageType = byteArrayToInt(response.get(0));
			subMessage = byteArrayToInt(response.get(1));
		}while(messageType != MessageType.LOGIN.getMessageType() || (subMessage != 0 && subMessage != 1 && subMessage != 2 && subMessage != 3));
		
		System.out.println(byteArrayToString(response.get(3)));
		
		//need to start thread before going to alreadyLoggedIn()
		if (subMessage == 0){
			createStore();
			alreadyLoggedIn();
		}
		else if (subMessage == 1){
			alreadyLoggedIn();
		}
		else if(subMessage == 2 || subMessage == 3){
			initialMenu();
		}
	}
	
	/**
	 * Creates data store for user
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public static void createStore() throws IOException, InterruptedException{
		int messageType = MessageType.CREATE_STORE.getMessageType();
		int subMessage = 0;
		String message = " ";
		
		ArrayList<byte[]> request = convertToArrayList(messageType,subMessage,message);
		
		ArrayList<byte[]> response = new ArrayList<byte[]>();
		
		do{
			response = sendRequest(request.get(0), request.get(1), request.get(2), request.get(3));
			subMessage = byteArrayToInt(response.get(1));
			messageType = byteArrayToInt(response.get(0));
		}while (messageType != MessageType.CREATE_STORE.getMessageType() || (subMessage != 0 && subMessage != 1 && subMessage != 2 ));
		
		//Based on server response offer correct corresponding options to user
		
		if(subMessage == 0){
			System.out.println(byteArrayToString(response.get(3)));
		}
		else if (subMessage ==2){
			initialMenu();
		}
	}
	
	/**
	 * Main menu once user is already logged in
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public static void alreadyLoggedIn() throws IOException, InterruptedException{
		Thread.sleep(1000);
		System.out.println("\n\nPlease select one of the following options:\n" +
				"\t\t1 - Send message to another user\n" +
				"\t\t2 - Check for new messages\n" +
				"\t\t3 - Delete your account\n" +
				"\t\t4 - Logoff\n" +
				"\t\t5 - Exit");
		int userSelection = 0;
		if(cli.hasNext()) {
			userSelection = Integer.parseInt(cli.next().trim());
		}
		
		switch(userSelection){
			case 1:
				sendMessage();
				break;
			case 2:
				queryMessages();
				alreadyLoggedIn();
				break;
			case 3:
				deleteAccount();
				Thread.sleep(2000);
				initialMenu();
				break;
			case 4:
				logoff();
				initialMenu();
				break;
			case 5:
				exit();
				break;
			default:
				System.out.println("Invalid choice.");
				alreadyLoggedIn();
				break;
		}
	}
	
	/**
	 * Send a message to another user
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	private static void sendMessage() throws IOException, InterruptedException {
		int messageType = MessageType.SEND_MESSAGE.getMessageType();
		int subMessage = 0;
		String username = "";
		String message = "";
		String data;
		
		//ask user to enter username to send message to
		System.out.print("\nPlease enter the username you wish to send a message to: ");
		if(cli.hasNext()){
			username = cli.next().trim();
		}
		cli.nextLine();
		//ask user to input message to send
		System.out.print("Please enter the message: ");
		
		message = cli.nextLine().toString();
		
		
		data = username + "," + message;
		System.out.println(data);
		
		ArrayList<byte[]> request = convertToArrayList(messageType,subMessage,data);
		ArrayList<byte[]> response = new ArrayList<byte[]>();
		
		do{
			response = sendRequest(request.get(0), request.get(1), request.get(2), request.get(3));
			
			messageType = byteArrayToInt(response.get(0));
			subMessage = byteArrayToInt(response.get(1));
		}while(messageType != MessageType.SEND_MESSAGE.getMessageType());
		
		System.out.println(byteArrayToString(response.get(3)));
		Thread.sleep(2000);
		alreadyLoggedIn();
	}
	
	/**
	 * Deletes the user from the system and logs them out
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	private static void deleteAccount() throws IOException, InterruptedException{
		int messageType = MessageType.DELETE_USER.getMessageType();
		int subMessage = 0;
		String message = " ";
		
		ArrayList<byte[]> request = convertToArrayList(messageType,subMessage,message);
		ArrayList<byte[]> response = new ArrayList<byte[]>();
		
		do{
			response = sendRequest(request.get(0), request.get(1), request.get(2), request.get(3));
			
			messageType = byteArrayToInt(response.get(0));
			subMessage = byteArrayToInt(response.get(1));
		}while (messageType != MessageType.DELETE_USER.getMessageType() || (subMessage != 0 && subMessage != 1));
		
		//Based on server response offer correct corresponding options to user
		System.out.println(byteArrayToString(response.get(3)));
	}
	
	/**
	 * Logs the user out of the system
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	private static void logoff() throws IOException, InterruptedException{
		int messageType = MessageType.LOGOFF.getMessageType();
		int subMessage = 0;
		String message = " ";
		
		ArrayList<byte[]> request = convertToArrayList(messageType,subMessage,message);
		ArrayList<byte[]> response = new ArrayList<byte[]>();
		do {
			response = sendRequest(request.get(0), request.get(1), request.get(2), request.get(3));
			
			//Based on server response offer correct corresponding options to user
			messageType = byteArrayToInt(response.get(0));
			subMessage = byteArrayToInt(response.get(1));
			
		}while(messageType != MessageType.LOGOFF.getMessageType() || (subMessage != 0 && subMessage != 1 && subMessage !=2));
		System.out.println(byteArrayToString(response.get(3)));
	}
	
	private static void queryMessages() throws IOException, InterruptedException{
		int messageType = MessageType.QUERY_MESSAGES.getMessageType();
		int subMessage = 0;
		String message = " ";
		
		ArrayList<byte[]> request = convertToArrayList(messageType,subMessage,message);
		ArrayList<byte[]> response = new ArrayList<byte[]>();
		
		do{
			response = sendRequest(request.get(0), request.get(1), request.get(2), request.get(3));
			
			messageType = byteArrayToInt(response.get(0));
			subMessage = byteArrayToInt(response.get(1));
			
			System.out.println(byteArrayToString(response.get(3)));
		
		} while(subMessage == 1 && messageType == MessageType.QUERY_MESSAGES.getMessageType());
		
	}

	/**
	 * Sends a request to the server and returns the server's response.
	 * @param messageType
	 * @param subMessage
	 * @param size
	 * @param data
	 * @return
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	private static ArrayList<byte[]> sendRequest (byte[] messageType, byte[] subMessage, byte[] size, byte[] data) throws IOException, InterruptedException {
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
//		availableBytes = 0;
//		while(availableBytes < messageSize){
//			fromServer = socket.getInputStream();
//			availableBytes = fromServer.available();
//			Thread.sleep(10);
//		}
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
	 * Helper method to store protocol parameters as an array list
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
	
	/**
	 * Checks for new messages from the server every 5 seconds
	 */
	private static class AutoQuery implements Runnable{
		@Override
		public void run(){
			
			while(threadFlag){
				try {
					queryMessages();
					Thread.sleep(5000);
				} catch (InterruptedException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		
	}
}
