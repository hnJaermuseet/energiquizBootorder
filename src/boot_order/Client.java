package boot_order;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
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
 *  2. Client port
 *  3. Server IP
 *  4. Server port
 * 
 * @author Chris Håland
 *
 */

public class Client implements Runnable {
	private final static boolean DEBUG = true;
	
	private final static int ARGUMENTS_ALLOWED = 4;
	private final static int ME_WAIT_LONG_TIME = 5*1000;
	
	private static Logger log;
	private static FileHandler handler;

	public static void main(String[] args) {
		try {
			handler = new FileHandler("client_log.log", true);
			handler.setFormatter(new SimpleFormatter());
			log = Logger.getLogger("");
			log.addHandler(handler);
		} catch (SecurityException e) {
			if (DEBUG) e.printStackTrace();
		} catch (IOException e) {
			if (DEBUG) e.printStackTrace();
		} if (args.length > ARGUMENTS_ALLOWED || args.length < ARGUMENTS_ALLOWED) {
			log.log(Level.SEVERE, "Arguments not valid." + "\n" + "Arguments must be: Service to start, PORT, SERVER_IP, SERVER_PORT");
			System.exit(0);
		} new Client(args[0], Integer.parseInt(args[1]), args[2], Integer.parseInt(args[3]));
	}

	private Thread thread;
	private Socket client_socket;
	private Socket server_socket;
	private ServerSocket client_srv;
	private boolean players_synchronized = false;
	public Client(String task, int port, String  srv_ip, int srv_port) {
		try {
			client_srv = new ServerSocket(port);
		} catch (IOException e) {
			if (DEBUG) e.printStackTrace();
			log.log(Level.SEVERE, "Client could not set up its own socket.", e);
		} while (!players_synchronized) {
			try {
				client_socket = client_srv.accept();
				server_socket = new Socket(srv_ip, srv_port);
				
				thread = new Thread(this);
				thread.start();
			} catch (IOException e) {
				if (DEBUG) e.printStackTrace();
				log.log(Level.SEVERE, "Client socket could not be established", e);
			}
		}
	}

	private InputStream stream_in;
	private OutputStream stream_out;
	private ObjectInputStream message_in;
	private ObjectOutputStream message_out;
	private boolean connection_made = false;
	public void run() {
		while (!connection_made) {
			try {
				stream_out = server_socket.getOutputStream();
				message_out = new ObjectOutputStream(stream_out);
				message_out.writeBoolean(true);
				message_out.flush();
				connection_made = true;
				try {
					Thread.sleep(ME_WAIT_LONG_TIME);
				} catch (InterruptedException e) {
					if (DEBUG) e.printStackTrace();
					log.log(Level.SEVERE, "Thread waiting for host was interrupted.", e);
				}
			} catch (UnknownHostException e) {
				if (DEBUG) e.printStackTrace();
				log.log(Level.SEVERE, "Could not connect to host.", e);
			} catch (IOException e) {
				if (DEBUG) e.printStackTrace();
				log.log(Level.SEVERE, "Could not write to host.", e);
			}
		} try {
			stream_in = client_socket.getInputStream();
		} catch (IOException e) {
			if (DEBUG) e.printStackTrace();
			log.log(Level.SEVERE, "Server stream could not be found.", e);
		} while (stream_in != null) {
			try {
				message_in = new ObjectInputStream(stream_in);
				if (message_in.readBoolean()) {
					stream_in = null;
					client_socket.close();
					server_socket.close();
					players_synchronized = true;
				} try {
					Thread.sleep(ME_WAIT_LONG_TIME);
				} catch (InterruptedException e) {
					if (DEBUG) e.printStackTrace();
					log.log(Level.SEVERE, "Thread awaiting message from host was interrupted.", e);
				}
			} catch (IOException e) {
				e.printStackTrace();
				log.log(Level.SEVERE, "Could not recieve from host.", e);
			}
		}
	}
}
