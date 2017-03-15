package backuperserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

/**
 * Glowna klasa serwera
 * @author Jakub Buraczyk
 *
 */
public class MyServer implements Runnable{
	
	/**
	 * Gniazdo serwera
	 */
	private ServerSocket serverSocket;

	/**
	 * Wektor klientow podlaczonych do serwera
	 */
    private Vector<ServerService> clients = new Vector<ServerService>();
    	
    
    /**
     * Konstruktor serwera
     * @param port port na ktorym serwer bedzie nasluchiwal na polaczenie
     */
    public MyServer(int port) {
        try {
            serverSocket = new ServerSocket(port);
            new Thread(this).start();
            
        } catch (IOException e) {
        	OutputFrame.toError("Error starting server: " + e);;
            //System.exit(1);
        }
    }
    
    /**
     * Nadpisana metoda obslugi watku
     */
    public void run() {
        while (true)
            try {
            	OutputFrame.toOutput("Czekam na polaczenie na porcie " + Config.getPort() + "...");
                Socket clientSocket = serverSocket.accept();
                OutputFrame.toOutput("Polaczono z: " + clientSocket.getInetAddress().toString());
                ServerService clientService = new ServerService(clientSocket, this);
                addClientService(clientService);
            } catch (IOException e) {
            	OutputFrame.toError("Error accepting connection: " + e);;
            }
    }
    
    /**
     * Metoda dodajaca obsluge nowego klienta
     * @param clientService obiekt klasy obslugujacej klienta
     * @throws IOException rzucany wyjatek
     */
    synchronized void addClientService(ServerService clientService)throws IOException {
        clientService.init();
        clients.addElement(clientService);
        new Thread(clientService).start();
        OutputFrame.toOutput("Dodaje klienta nr." + (clients.indexOf(clientService)+1) );
    }

	
    /**
     * Metoda usuwajaca klienta z serwera
     * @param clientService obiekt klasy obslugujacej klienta
     */
	synchronized void removeClientService(ServerService clientService) {
		OutputFrame.toOutput("Usuwanie klienta nr." + (clients.indexOf(clientService)+1) );
        clients.removeElement(clientService);
        clientService.close();
        OutputFrame.toOutput("Czekam na polaczenie na porcie " + Config.getPort() + "...");
    }

    /**
     * Glowna metoda programu serwera
     * @param args poczatkowe argumenty wywolania programu
     */
    public static void main(String args[]) {
    	new Config();
    	new OutputFrame();
        new MyServer(Config.getPort());
    }

}
