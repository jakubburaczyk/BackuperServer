package backuperserver;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;

/**
 * Klasa okienka zawierajcego zapis komunikacji serwera z klientami
 * @author Jakub Buraczyk
 *
 */
public class OutputFrame extends JFrame implements WindowListener{
	
	/**
	 * Numer seryjny klasy
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Obszar w ktorym pokazywane beda komunikaty
	 */
	private static JTextArea logArea, errorArea;
	
    /**
     * Obiekt do przekierowywania outputu do pliku
     */
    private static BufferedWriter bw;
	
	/**
	 * Konstruktor okna
	 */
	public OutputFrame(){
		super("Serwer output");
			
		setSize(500, 600);
		setLocationRelativeTo(null);
		
		
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setMinimumSize(new Dimension(200,300));
		
		
		JPanel gui = new JPanel(new BorderLayout());
		JPanel upPanel = new JPanel(new GridLayout());
		JPanel downPanel = new JPanel(new GridLayout());
		
		upPanel.setPreferredSize(new Dimension(500,500));
		downPanel.setPreferredSize(new Dimension(500,100));
		
		JSplitPane spliter = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, upPanel, downPanel);
		spliter.setDividerSize(6);
		spliter.setDividerLocation(upPanel.getHeight());		
		
		
		logArea = new JTextArea();
		logArea.setEditable(false);
		logArea.setBackground(Color.DARK_GRAY);
		logArea.setForeground(Color.WHITE);
		logArea.setLineWrap(false);
		
		JScrollPane areaScrollPane1 = new JScrollPane(logArea, 
				   JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	
		upPanel.add(areaScrollPane1);
		
		errorArea = new JTextArea();
		errorArea.setEditable(false);
		errorArea.setBackground(Color.LIGHT_GRAY);
		errorArea.setForeground(Color.RED);
		errorArea.setLineWrap(false);
		
		JScrollPane areaScrollPane2 = new JScrollPane(errorArea, 
				   JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		
		
		downPanel.add(areaScrollPane2);
		
		gui.add(spliter);
		gui.add(upPanel, BorderLayout.CENTER);		
		gui.add(downPanel, BorderLayout.PAGE_END);
		
		add(gui);
		
		
		
		
		setVisible(true);
		
		try {
			Image logo=ImageIO.read(new File("img/output-icon.png"));
			setIconImage(logo);

			FileWriter fw = new FileWriter("log.txt", true);
		    bw = new BufferedWriter(fw);
		    
		    DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy | HH:mm:ss");
		    Date date = new Date();
		    
		    toOutput("*********************************\n" + "Log z dnia: " + dateFormat.format(date));
		    
		} catch (IOException e) {
			toError("Błąd otwierania pliku: " + e);
		}	
	}
	
	/**
	 * Dodanie danego napisu do okna
	 * @param line dodawany napis
	 */
	public static void toOutput(String line){
		logArea.append(line + "\n");
		try {
			bw.write(line + "\n");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void toError(String line){
		errorArea.append(line + "\n");
	}

	@Override
	public void windowActivated(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowClosed(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowClosing(WindowEvent arg0) {
		try {
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	@Override
	public void windowDeactivated(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowDeiconified(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowIconified(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowOpened(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}
}
