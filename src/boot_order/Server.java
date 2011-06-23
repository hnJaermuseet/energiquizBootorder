package boot_order;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Server waits for clients to come online. When both have a connection the server starts the argument program, waits 30 seconds and
 * then informs the clients that they can start their argument program.
 * 
 * Arguments are as follows:
 * 	1. Program to start up.
 *  2. Server port
 *  3. Client 1 IP
 *  4. Client 1 port
 *  5. Client 2 IP
 *  6. Client 2 port
 * 
 * @author Chris Håland
 *
 */

public class Server implements Runnable {
	private final static boolean DEBUG = true;
	
	private final static int MAX_RETRIES = 5;
	private final static int PLAYERS_ALLOWED = 2;
	private final static int ARGUMENTS_ALLOWED = 6;
	private final static int ME_WAIT_LONG_TIME = 30*1000;
	
	private static Logger log;
	private static FileHandler handler;
	
	public static void main(String[] args) {
		try {
			handler = new FileHandler("server_log.log", true);
			handler.setFormatter(new SimpleFormatter());
			log = Logger.getLogger("");
			log.addHandler(handler);
		} catch (SecurityException e) {
			if (Server.DEBUG) e.printStackTrace();
		} catch (IOException e) {
			if (Server.DEBUG) e.printStackTrace();
		} if (args.length > ARGUMENTS_ALLOWED || args.length < ARGUMENTS_ALLOWED) {
			log.log(Level.SEVERE, "Arguments not valid." + "\n" + "Arguments must be: Service to start, SERVER_PORT, " +
								  "PLAYER1_IP, PLAYER1_PORT, PLAYER2_IP, PLAYER2_PORT");
			System.exit(0);
		} new Server(args[0], Integer.parseInt(args[1]), args[2], Integer.parseInt(args[3]), 
					 args[4], Integer.parseInt(args[5]));
		
	}
	
	private Socket socket;
	private Thread thread;
	boolean waiting_on_players;
	private ServerSocket srv_socket;
	public Server(String task, int port, String player1_ip, int player1_port, 
				  String player2_ip, int player2_port) {

		try {
			srv_socket = new ServerSocket(port);
			while (waiting_on_players) {
				socket = srv_socket.accept();
				stream_in = socket.getInputStream();
				thread = new Thread(this);
				thread.start();
			} 
		} catch (IOException e) {
			if (DEBUG) e.printStackTrace();
			log.log(Level.SEVERE, "Server socket could not be established", e);
		} try {
			Runtime.getRuntime().exec(task);
			log.log(Level.SEVERE, "Starting "  + task);
		} catch (IOException e) {
			if (DEBUG) e.printStackTrace();
			log.log(Level.SEVERE, "Task could not be executed", e);
		} try {
			Thread.sleep(ME_WAIT_LONG_TIME);
		} catch (InterruptedException e) {
			if (DEBUG) e.printStackTrace();
			log.log(Level.SEVERE, "Could not delay client startup.", e);
		}
		
		informClient(player1_ip, player1_port);
		informClient(player2_ip, player2_port);
	}
	
	private int retries = 0;
	private OutputStream stream_out;
	private ObjectOutputStream message_out;
	private void informClient(String ip, int port) {
		try {
			stream_out = socket.getOutputStream();
			message_out = new ObjectOutputStream(stream_out);
			message_out.writeBoolean(true);
			message_out.flush();
		} catch (IOException e) {
			if (DEBUG) e.printStackTrace();
			log.log(Level.SEVERE, "Could not send message to " + ip + " at port " + String.valueOf(port), e);
			retries++;
			if (retries > MAX_RETRIES) {
				try {
					socket.close();
				} catch (IOException arg) {
					if (DEBUG) arg.printStackTrace();
					log.log(Level.SEVERE, "Could not close socket after to many retries.", arg);
				} System.exit(0);
			} else informClient(ip, port);
		}
	}
	
	private int players = 0;
	private InputStream stream_in;
	private ObjectInputStream message_in;
	public void run() {
		while (stream_in != null) {
			try {
				message_in = new ObjectInputStream(stream_in);
				if (message_in.readBoolean()) {
					players++;
				} if (players == PLAYERS_ALLOWED) {
					stream_in = null;
					socket.close();
					waiting_on_players = false;
				}
			} catch (IOException e) {
				if (DEBUG) e.printStackTrace();
				log.log(Level.SEVERE, "Server could not read message.", e);
			}
		}
	}

}
