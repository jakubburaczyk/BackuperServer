package backuperserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Klasa konfigacji serwera
 * @author Jakub Buraczyk
 *
 */
public class Config {
	/**
	 * plik zawierajacy ustawienia serwera
	 */
	private static File propertyFile;
	/**
	 * Plik zawierajacy dane o znanych uzytkownikach
	 */
	private static File userinfoFile;
	/**
	 * Wlasciwosci serwera
	 */
	private static Properties prop1;
	/**
	 * Wlasciwosci uzytkownikow
	 */
	private static Properties prop2;
	/**
	 * Strumien wczytujacy dane z pliku
	 */
	private static InputStream is;
	/**
	 * Strumien wczytujacy dane do pliku
	 */
	private static FileOutputStream out;

	/**
	 * Numer portu na ktorym serwer nasluchuje na polaczenie
	 */
	private static int port;

	/**
	 * Konstruktor klasy, wczytuje pliki i ustawienia 
	 */
	public Config(){
		propertyFile = new File("settings/Config.txt");
		userinfoFile = new File("settings/SavedUsers.txt");
		prop1 = new Properties();
		prop2 = new Properties();
		readConfig();
	}
	
	/**
	 * Odczytanie pliku z ustawieniami
	 */
	public static void readConfig()  
	{
        try {
        	is = new FileInputStream(propertyFile);
            prop1.load(is);   
            is.close();
            
            port = Integer.parseInt(prop1.getProperty("port_nasluchiwania"));
        } catch (Exception e) {
			OutputFrame.toError("Exception: " + e);
		} 
	}
	
	/**
	 * Metdoda sprawdzajaca czy dany klient logowal sie juz na serwer
	 * @param username nazwa uzytkownika
	 * @return true jesli uzytkownik jest znany serwerowi, false jesli jest nowy
	 */
	public static  boolean isInBase(String username){	
		try {			
			is = new FileInputStream(userinfoFile);			
			prop2.load(is);
			is.close();		
			
		} catch (FileNotFoundException e) {
			OutputFrame.toError("Exception: " + e);
		} catch (IOException e) {
			OutputFrame.toError("Exception: " + e);
		}
		
		if (prop2.getProperty(username) == null)
			return false;
		else 
			return true;
		
	}
	
	/**
	 * Metoda wczytujaca haslo danego klienta
	 * @param username nazwa uzytkownika
	 * @return haslo tego uzytkownika zapisane w bazie danych
	 */
	public static String getPassword(String username){
		return prop2.getProperty(username);
	}
	
	/**
	 * Metoda nadajaca haslo dla nowego uzytkownika i zapisujaca je do pliku
	 * @param username nazwa uzytkownika
	 * @param password haslo przypisane do tego uzytkownika
	 */
	public static void setPassword(String username, String password){
		try {
			out = new FileOutputStream(userinfoFile);
			prop2.setProperty(username, password);
			prop2.store(out, null);
			out.close();
		} catch (FileNotFoundException e) {
			OutputFrame.toError("Exception: " + e);
		}catch (IOException e) {
			OutputFrame.toError("Exception: " + e);
		}
	}
	
	/**
	 * Getter numeru portu na ktorym serwer nasluchuje na polaczenie
	 * @param sPort numer portu nasluchiwania 
	 */
	public static void setPort(int sPort)
	{
		port = sPort;
	}
	
	/**
	 * Metoda zwracaja numer portu na ktorym serwer nasluchuje na polaczenie
	 * @return numer portu nasluchiwania
	 */
	public static int getPort()
	{
		return port;
	}
	
}