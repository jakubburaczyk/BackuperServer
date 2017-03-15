package backuperserver;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.StringTokenizer;

/**
 * Klasa obslugujaca serwerowa strone polaczenia z klientem
 * @author Jakub Buraczyk
 *
 */
public class ServerService implements Runnable{
	
	/**
	 * Lokalna instancja klasy serwera
	 */
	private MyServer myserver;
	/**
	 * Gniazdo klienta polaczonego do serwera
	 */
	private Socket clientSocket;
	
	/**
	 * Buforowany czytnik strumienia znakow przychodzacego od klienta
	 */
    private BufferedReader input;
    /**
     * Obiekt obslugujacy wysylanie strumien znakow do klienta
     */
    private PrintWriter output;
    /**
     * Buforowany czytnik strumienia danych przychodzacych od klienta
     */
    private BufferedInputStream bis;
    /**
     * Czytnik strumienia danych
     */
    private FileInputStream fis;
    
    /**
     * Nazwa polaczonego klienta
     */
    private String clientNick;
    /**
     * Sciezka do folderu zawierajacego pliki danego uzytkownika
     */
    private String clientDirPath;
    /**
     * Pliki znajdujace sie w folderze danego uzytkownika
     */
    private File[] clientFiles;
    /**
     * Folder danego uzytkownika
     */
    private File clientDirectory;
    
    /**
     * Sciezka do pliku pobieranego od klienta
     */
    private String downloadFilePath;
    /**
     * Sciezka do pliku wysylanego do klienta
     */
    private String uploadFilePath;
    /**
     * Strumien danych do pliku od klienta
     */
    private FileOutputStream fos;
    /**
     * Strumien danych od klienta
     */
    private DataOutputStream dos;
    

    /**
	 * Konstruktor klasy
	 * @param clientSocket gniazdo podlaczonego klienta
	 * @param myserver instancja serwera
	 */
	public ServerService(Socket clientSocket, MyServer myserver) {
        this.myserver = myserver;
        this.clientSocket = clientSocket;
        OutputFrame.toOutput("Nowy klient");
    }
	
	/**
	 * Nadpisana metoda obslugi watku
	 */
	@Override
	public void run() {
        while (true) {
        	String request = receive();
        	OutputFrame.toOutput("Wiadomosc od klienta: " + request);
            StringTokenizer st = new StringTokenizer(request);
            String command = st.nextToken();
            
            /*
             * W zaleznosci od otrzymanego zapytania od klienta wykonywana jest dana operacja
             */
            if (command.equals(Protocol.NULLCOMMAND)) {
            	OutputFrame.toOutput("Rozlaczony klient");
                break;
            }
            
            else if (command.equals(Protocol.LOGIN)) {
            	clientNick = st.nextToken();            	
            	OutputFrame.toOutput("Logowanie klienta: " + clientNick);
            	
            	if (Config.isInBase(clientNick)){
            		send(Protocol.ASKFORPASSWORD);            	
	            	String tryPass = receive();
	            	OutputFrame.toOutput("Odebrano haslo: " + tryPass);
	            	if (tryPass.equals(Config.getPassword(clientNick))){
	            		OutputFrame.toOutput("Zalogowano klienta: " + clientNick);
	                	
	            		clientDirPath = "data" + "/" + clientNick;
	                	clientDir(false, clientDirPath);
	                	String clientFileNames = "";
	                	for (File file: clientFiles){
	                		clientFileNames += file.getName() + " ";
	                	}
	                	OutputFrame.toOutput(clientNick);
	                	send(Protocol.LOGGEDIN + " " + clientFileNames);
	            	}           	
	            	else {
	            		OutputFrame.toOutput("Podano bledne haslo");
	                	send(Protocol.LOGGEDOUT);
	                	break;
	            	}
            	}
            	else {
            		send(Protocol.NEWPASSWORD);
            		String password = receive();
            		OutputFrame.toOutput("Odebrano nowe haslo: " + password);
            		Config.setPassword(clientNick, password);
            		OutputFrame.toOutput("Zalogowano klienta: " + clientNick);
                	clientDirPath = "data" + "/" + clientNick;
                	clientDir(true, clientDirPath);
                	OutputFrame.toOutput(clientNick);
                	send(Protocol.LOGGEDIN);
            	}
            	
            }

            
            else if (command.equals(Protocol.UPDATEFILES)){
            	File clientDirectory = new File(clientDirPath);
            	File[] updatedClientFiles = clientDirectory.listFiles();
            	String clientFileNames = "";
            	for (File file: updatedClientFiles){
            		clientFileNames += file.getName() + " ";
            	}
            	send(Protocol.UPDATEFILES + " " + clientFileNames);
            }
            
            else if (command.equals(Protocol.ISONSERVER)){
            	String fileName = st.nextToken();
            	long fileSize = Long.parseLong(st.nextToken());
            	String checksum = st.nextToken();
            	int isNew = checkIfArchivized(fileName, fileSize, checksum);
            	if (isNew == 0){
            		send(Protocol.SAMEFILEARCHIVIZED);
            	} 
            	else if (isNew == 1){
            		send(Protocol.NOTONSERVER);
            	}           	
            	else {
            		send(Protocol.DIFFERENTVERSIONARCHIVIZED + " " + isNew);
            	}
            	         	
            }
            
            else if(command.equals(Protocol.SENDINGFILE)){
            	String fileName = st.nextToken();
            	long fileSize = Long.parseLong(st.nextToken());
            	int version = Integer.parseInt(st.nextToken());
            	downloadFileFromClient(fileName, fileSize, version);            	
            }
            
            else if (command.equals(Protocol.DOWNLOADFILE)){
            	String fileName = st.nextToken();
            	sendFileToClient(fileName);
            }
            
            else if (command.equals(Protocol.REMOVEFILE)){
            	String fileName = st.nextToken();
            	removeFile(fileName);
            }
            
            else if (command.equals(Protocol.LOGOUT)){
            	OutputFrame.toOutput("Wylogowywanie klienta");
            	send(Protocol.LOGGEDOUT);
            	break;
            }
            
            else if (command.equals(Protocol.STOP)){
            	OutputFrame.toOutput("Wylogowywanie klienta");
            	send(Protocol.STOP);
            	break;
            }
            
            
        }
        myserver.removeClientService(this);
    }

	/**
	 * Metoda wczytujaca pliki nalezace do polaczonego klienta
	 * @param isNew czy dany klient laczyl sie juz z serwerem
	 * @param path sciezka  do folderu klienta
	 */
	public void clientDir(boolean isNew, String path){
		clientDirectory = new File(path);
		if(isNew){
			clientDirectory.mkdir();
		}
		clientFiles = clientDirectory.listFiles();
	}
	
	/**
	 * Metoda sprawdzajaca czy dany plik zostal juz zarchiwizowany na serwerze
	 * @param name nazwa sprawdzanego pliku
	 * @param size rozmiar sprawdzanego pliku
	 * @param checksum suma kontrolna tego pliku
	 * @return jesli 0 to ten sam plik znajduje sie juz na serwerze, jesli 1 lub wiecej to inna wersja (inne wersje) tego pliku znajduje sie juz na serwerze 
	 */
	public int checkIfArchivized(String name, long size, String checksum){
		int versions = 1;
		for (File file : clientFiles){			
			if ((file.getName().contains(name)) && (file.length() == size) && (getFileChecksum(file).equals(checksum))){
				return 0;
			}
			else if ((file.getName().contains(name)) && !(getFileChecksum(file).equals(checksum))){
				versions += 1;
				OutputFrame.toOutput("Jest " + (versions-1) + " wersji tego pliku");
			}
		}		
		return versions;		
	}
	
	/**
	 * Metoda sprawdzajaca sume kontrolna danego pliku
	 * @param file sprawdzany plik
	 * @return suma kontrolna MD5 sprawdzanego pliku
	 */
	public String getFileChecksum(File file) {
		MessageDigest md;
		InputStream is;
		byte[] bytes;
		StringBuffer sb = new StringBuffer("");
		try {
			md = MessageDigest.getInstance("MD5");
			
			bytes = new byte[128 * 1024];
			is = new FileInputStream(file);
			int numBytes;
			while ((numBytes = is.read(bytes)) != -1) {
				md.update(bytes, 0, numBytes);
			}
			byte[] mdbytes = md.digest();
				
			for (int i = 0; i < mdbytes.length; i++) {
			    sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
			}
			    
			is.close();
			
		} catch (NoSuchAlgorithmException e) {
			OutputFrame.toError("Exception: " + e);
		} catch (FileNotFoundException e) {
			OutputFrame.toError("Exception: " + e);
		} catch (IOException e) {
			OutputFrame.toError("Exception: " + e);
		} 
										
		return sb.toString();
	}
		
	/**
	 * Inicjacja strumieni odczytu i wysylania danych
	 * @throws IOException rzucany wyjatek gdy blad przy inicjacji strumieni
	 */
	public void init() throws IOException{
    	input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        output = new PrintWriter(clientSocket.getOutputStream(), true);
    }
	
	/**
	 * Metoda sluzaca do pobrania pliku na serwer 
	 * @param fileName nazwa pliku
	 * @param fileSize rozmiar pliku w bajtach
	 * @param version numer wersji pobieranego pliku
	 */
	public void downloadFileFromClient(String fileName, long fileSize, int version) {
		try {
			downloadFilePath = clientDirPath + "/(ver." + version +")" + fileName;				
		    byte [] bytes  = new byte [16 * 1024];
		    InputStream is = clientSocket.getInputStream();
			
		    fos = new FileOutputStream(downloadFilePath);
		    dos = new DataOutputStream(fos);
		    long bytesRead = 0;
		    long totalRead=0;
		    OutputFrame.toOutput("pobieranie pliku o rozmiarze: " + fileSize);
		    	    		  
		    while(totalRead != fileSize){
		    	bytesRead = is.read(bytes, 0, bytes.length);
		    	totalRead += bytesRead;
		        dos.write(bytes, 0 , (int)bytesRead);
		        dos.flush();
		      } 	   
		    OutputFrame.toOutput("File " + fileName + " downloaded (" + totalRead + " bytes read)"); 
		    clientFiles = clientDirectory.listFiles();
		    send(Protocol.FILEARCHIVIZED);
		    
		} catch (IOException e) {
			OutputFrame.toError("Exception: " + e);
			File file = new File(downloadFilePath);
			file.delete();
		} catch (Exception e) {			
			OutputFrame.toError("Exception: " + e);
			File file = new File(downloadFilePath);
			file.delete();		 
		}		  
		finally {
		      try {
				fos.close();
				dos.close();
			} catch (IOException e) {
				OutputFrame.toError("Exception: " + e);
			}		      
		}				
	}
	
	/**
	 * Metoda obslugujaca wysylanie pliku z serwera do klienta
	 * @param fileName
	 */
	public void sendFileToClient(String fileName) {
    	new Thread(new Runnable() {
    	    public void run() {
    	    	uploadFilePath = clientDirPath + "/" + fileName;	
    	    	File file = new File(uploadFilePath);
    	    	long fileSize = file.length();
				OutputFrame.toOutput("File length: " + fileSize);
				send(Protocol.SENDINGFILE + " " + fileName + " " + fileSize);
				byte[] bytes = new byte[16 * 1024];
				
				try {
					OutputStream os = clientSocket.getOutputStream();
					fis = new FileInputStream(file);
					bis = new BufferedInputStream(fis);
				
							
					long bytesRead = 0;
				    long totalRead=0;
				  		    
				    while(totalRead != fileSize){
				    	bytesRead = bis.read(bytes,0,bytes.length);
				        totalRead += bytesRead;
				        double procent = 100.0*totalRead/fileSize;
				        OutputFrame.toOutput("Trwa wysylanie pliku do klienta: " + (int)procent + "%");
				        os.write(bytes,0, (int)bytesRead);
				        os.flush();
					}
				    
				    OutputFrame.toOutput("Wysylanie zakonczone!");
				    			    
				} catch (FileNotFoundException e) {
					OutputFrame.toError("Exception: " + e);
				} catch (IOException e){
					OutputFrame.toError("Exception: " + e);
				} finally {
					try {
						fis.close();
					    bis.close();
					} catch (IOException e){
						OutputFrame.toError("Exception: " + e);
					}
				}
    	    }
    	}).start();  	
    }

	/**
	 * Metoda do usuwania pliku z serwera
	 * @param fileName nazwa pliku przeznaczonego do usuniecia
	 */
	public void removeFile(String fileName){
		String filePath = clientDirPath + "/" + fileName;
		File file = new File(filePath);
		file.delete();	
    	send(Protocol.FILEREMOVED);
	}
		
	/**
	 * Metoda zamykajaca komunikacje miedzy serwerem a klientem
	 */
	public void close() {
        try {
            output.close();
            input.close();
            clientSocket.close();
        } catch (IOException e) {
        	OutputFrame.toError("Error closing connection: " + e);;
        } finally {
            output = null;
            input = null;
            clientSocket = null;
        }
    }
	
	/**
	 * Metoda sluzaca do wysylanie komend tekstowych do klienta
	 * @param command komenda do klienta
	 */
	public void send(String command) {
		if (output != null){
			OutputFrame.toOutput("Wiadomosc do klienta: " + command);
	        output.println(command);
		}
    }

	/**
	 * Metoda zwracajaca rozkaz odebrany od klienta
	 * @return rozkaz od klienta
	 */
    public String receive() {
    	if (input != null){
	        try {
	            return input.readLine();
	        } catch (IOException e) {
	        	OutputFrame.toError("Error reading from client: " + e);;
	        }
    	}
        return Protocol.NULLCOMMAND;
    }
}
