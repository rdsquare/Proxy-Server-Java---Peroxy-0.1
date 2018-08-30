/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package peroxy;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import javax.swing.JFrame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.InetAddress;
import javax.swing.JWindow;
import javax.swing.ImageIcon;
import javax.swing.SwingConstants;
/**
 *
 * @author rd_square
 */
public class PeroxyInterface extends JFrame implements Runnable {

    // Peroxy server name variable
    public static final String APPNAME = "Peroxy-0.1";
    /**
     * Creates server socket to connect with clients
     */
    private ServerSocket serverSocket;
    
    /**
     * Creates a volatile current running state variable
     * Default set to false
     */
    private static volatile boolean running = false;
    
    /**
     * List of service threads which handles the request of clients
     * using threading. It will be needed when closing the server to join
     * all the threads.
     */
    private ArrayList<Peroxy> servicingThreads;
    public static volatile ArrayList<Peroxy> delThreads;
    
    /**
     * Data Structure for constant order lookop for cached page
     * Key: Url of the page/image
     * Value: File which containing the page or image
     */
    static HashMap<String, File> cPagesMap;
    
    /**
     * Data Structure for constant order lookup for blocked clients
     * Key: ip of client's computer
     * Value: ip of client's computer
     */
    static HashMap<String, String> bClientsMap;
    
    /**
     * Data Structure for constant order lookup for blocked websites
     * Key: ip of website
     * Value: ip of website
     */
    static HashMap<String, String> bWebsitesMap;
    
    /**
     * Data structure to mantain logs of the users 
     * Hash of Hash of Hash of Hash of Hash - the five layer HashMap\
     * Key: fileName
     * Value: file
     * value: File Name (File object)
     */
    static HashMap<String, File> logManagerMap;
            
    // Port to create server socket - setting default to port: 10000
    private int PORT = 10000;
    
    //Keep alive maximum request
    public static volatile int maxClients = 5; //Setting to 100 request initially
    
    //Socket timeout for client socket
    public static volatile int clientSocketTimeout  = 2000; //Setting to 20 seconds
    
    //Maximum threads
    public static volatile int maxThreads = 500; //Setting initially threads to 500
    /**
     * String to object for accepting
     * signals from Peroxy class and save the message to it
     */
    public static volatile String SIGNALMESSAGE = "";
    
    /**
     * Data structure to store detail of active clients
     * Key: ip of active client
     * Value: Array of HostName and connect time of client
     */
    public static volatile HashMap<String, ArrayList<String>> activeClientDetails;
    public static volatile HashMap<String, ArrayList<String>> activeClientAllDetails; //more detail on active client
    public static volatile HashMap<String, ArrayList<String>> allClients; //client connected after server switched on

    
         private static void sleepThread() {
            try
                {
                    Thread.sleep(5000);
                }
                catch (InterruptedException ex)
                {
                    // Do something, if there is a exception
                    System.out.println(ex.toString());
                }
        }
    /**
     * Creates new form PeroxyInterface
     */
    public PeroxyInterface() {
        
        /*
         * Initializing and setting all the GUI Components of 
         * PeroxyInterface
         */
        initComponents();
        
        /**
         * initializing variables
         */
        cPagesMap = new HashMap<>();
        bClientsMap = new HashMap<>();
        bWebsitesMap = new HashMap<>();
        servicingThreads = new ArrayList<>();
        delThreads = new ArrayList<>();
        activeClientDetails = new HashMap<>();
        activeClientAllDetails = new HashMap<>();
        allClients = new HashMap<>();
        logManagerMap = new HashMap<>();
        
        // Setting default values of labels
        messageLabel.setText("Server is currently Close");
        tableLabel.setText("Not Connected to any Clients! - Server Closed");
        
        terminal.setContentType("text/html"); //setting JEditorPanel for html text.
        terminal.setText("<html><b>"+this.APPNAME+" >  "+"</b></html>");
        
        //configuring cache and log file
        try {
            //loading previously cached web pages
            File cachedSites = new File("cacheManager/cachedSites.txt");
            if(!cachedSites.exists()) {
                handleTerminal("No caching file found - Making new caching file...");
                cachedSites.getParentFile().mkdirs(); //making directories of the file
                cachedSites.createNewFile(); //creating new caching file for cache manager
            } else {
                FileInputStream fis = new FileInputStream(cachedSites);
                ObjectInputStream ois = new ObjectInputStream(fis);
                cPagesMap = (HashMap<String, File>)ois.readObject();
                fis.close();
                ois.close();
            }
            
        } catch (IOException e) {
            handleTerminal("Error loading previously cached sites file");
        } catch (ClassNotFoundException e) {
            handleTerminal("Class not found loading in previouly cached sites file");
        }
        
        try {
            //loading previously blocked clients
            File bClientsFile = new File("blocking/blockedClients.txt");
            if(!bClientsFile.exists()) {
                handleTerminal("No client blocking file found - Making new blocking file...");
                bClientsFile.getParentFile().mkdirs();
                bClientsFile.createNewFile();
            } else {
                FileInputStream fis = new FileInputStream(bClientsFile);
                ObjectInputStream ois = new ObjectInputStream(fis);
                bClientsMap = (HashMap<String, String>)ois.readObject();
                fis.close();
                ois.close();
            }
            
        } catch (IOException e) {
            handleTerminal("Error loading previously blocked clients file");
        } catch (ClassNotFoundException e) {
            handleTerminal("Class not found loading in previouly blocked clients file");
        }
        
        try {
            //loading previously blocked websites
            File bWebsitesFile = new File("blocking/blockedWebsites.txt");
            if(!bWebsitesFile.exists()) {
                handleTerminal("No website blocking file found - Making new blocking file...");
                bWebsitesFile.getParentFile().mkdirs();
                bWebsitesFile.createNewFile();
            } else {
                FileInputStream fis = new FileInputStream(bWebsitesFile);
                ObjectInputStream ois = new ObjectInputStream(fis);
                bWebsitesMap = (HashMap<String, String>)ois.readObject();
                fis.close();
                ois.close();
            }
        }  catch (IOException e) {
            handleTerminal("Error loading previously blocked sites file");
        } catch (ClassNotFoundException e) {
            handleTerminal("Class not found loading in previouly blocked sites file");
        }
        
        try {
            //loading previous logs of clients
            File logManagerFile = new File("logManager/logFiles.txt");
            if(!logManagerFile.exists()) {
                handleTerminal("No log file found - Making new log file...");
                logManagerFile.getParentFile().mkdirs();
                logManagerFile.createNewFile();
            } else {
                FileInputStream fis = new FileInputStream(logManagerFile);
                ObjectInputStream ois = new ObjectInputStream(fis);
                logManagerMap = (HashMap<String, File>)ois.readObject();
                fis.close();
                ois.close();
            }
        }  catch (IOException e) {
            handleTerminal("Error loading previously log file");
        } catch (ClassNotFoundException e) {
            handleTerminal("Class not found loading in previouly log file");
        }
        
        handleTerminal("Server not Connected...");
        
    }
    
    /**
     * Initialize server Socket and start listening for clients
     */
    private void startConnection() {
        if (running) {
            JOptionPane.showMessageDialog(this, "Server is already Running.", "Server Running - "+this.APPNAME, JOptionPane.INFORMATION_MESSAGE);
        } else {
            try {
                serverSocket = new ServerSocket(PORT);
                running = true;
                
                //setting timeout for debugging
                //serverSocket.setSoTimeout(1000);
                
                messageLabel.setText("Server Started - Waiting for Clients...");
                tableLabel.setText("Server Connected - Total Clients (0)");
                handleTerminal("Server Started - Waiting for Clients...");
            } catch (SocketException se) {
               handleTerminal("Socket Exception when connecting to client.");
               messageLabel.setText("Server Error! - Server is unavailable");
               handleTerminal(se.toString());
            } catch (SocketTimeoutException st) {
                handleTerminal("Timeout occured while connecting to client.");
                messageLabel.setText("Server Error! - Server is unavailable");
            } catch (IOException io) {
                handleTerminal("IOExcetion when connecting to client.");
                messageLabel.setText("Server Error! - Server is unavailable");
                handleTerminal(io.toString());
            }
        }
        
        //listen to client requests by thread
        new Thread(this).start();
    }
    
    /**
     * Thread to listen requests of clients on the server socket port
     * 
     */
    @Override
    public void run() {
        //setting timer to remove content of terminal
        Timer removeTextTimer = new Timer();
        TimerTask removeTextTask = new TimerTask(){
            @Override
            public void run() {
                try{
                    terminal.setText("<html><b>"+PeroxyInterface.APPNAME+" >  "+"</b></html>");
                } catch (Exception e) {
                    terminal.setText("");
                    System.out.println("counter eror");
                }
            }};
                
        //Executing terminal timer every after 1 minute
        removeTextTimer.schedule(removeTextTask, 0, 6000);
        while(running) {
            if(maxThreads <= servicingThreads.size()){
                messageLabel.setText("Limit reached - Maximum threads on Server.");
                continue;
            }
            try {
                Socket socket = serverSocket.accept();
                messageLabel.setText("Server Connected.");
                if(activeClientDetails.size() >= maxClients) {
                    if(activeClientDetails.get(socket.getInetAddress().getHostAddress()) == null){
                        messageLabel.setText("Limit reached - Maximum clients on server.");
                        continue;
                    }
                }
                //Create a service thread for client
                Peroxy thread = new Peroxy(socket);
                
                //Puting servicing thread into data structure
                servicingThreads.add(thread);
                
                thread.start();
            } catch (SocketException se) {
                handleTerminal("Socket is Closed - Connection lose!");
                handleTerminal(se.toString());
                messageLabel.setText("Server Error! - Server is unavailable");
            } catch (IOException io) {
                handleTerminal(io.toString());
                messageLabel.setText("Server Error! - Server is unavailable");
            }
        }
    }
    
    /**
     * Close server after closing all the files for log manager and cache manager
     * and send message to terminal as well
     * @return boolean
     */
    public boolean closeConnection() {
        if (!running) {
            JOptionPane.showMessageDialog(this, "Server is already Closed.", "Server Closed - "+this.APPNAME, JOptionPane.INFORMATION_MESSAGE);
            return true;
        } else {
            int status = JOptionPane.showOptionDialog(this, 
                    "<html><body><h3>Do you want to close Server ?</h3><p>Active clients connection will be lost.</p></body></html>",
                    "Do you Want to close? - " + this.APPNAME,
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, 
                    null, 
                    new String[]{"Yes, Close Server!", "No, Don't Close Server"}, 
                    "No, Don't Close Server");
            if(status == 1){
                return false;
            }
        }
        
        messageLabel.setText("Closing Peroxy Server...");
        handleTerminal("Closing Peroxy-0.1 Server...");
        this.running = false; // setting running to false for accept() function 
        
        //saving informatio to file for future use
        saveFileManager();
        
        try {
            handleTerminal("Connection Terminating...");
            serverSocket.close();
            messageLabel.setText("Peroxy Server is Closed.");
            handleTerminal("Peroxy Server Socket is Closed.");
        } catch(Exception e) {
            handleTerminal("Exception in closing Peroxy's server socket");
        }
        
        //closing all the servicing threads 
        activeClientDetails = new HashMap<>();
        activeClientAllDetails  = new HashMap<>();
        closeClientConnections();
        
        return true; //returning true as server is closed now
    }
    
    /**
     * It will join all the servicing threads
     */
    private void closeClientConnections() {
        try {
            for (Peroxy thread : servicingThreads) {
                if(thread.isAlive()) {
                    handleTerminal("Waiting on "+thread.getId()+ " to close...");
                    try{
                        thread.join();
                    } catch (Exception e) {
                        handleTerminal(e.toString());
                    }
                    handleTerminal(thread.getId() + " is closed.");
                }
            }
        } catch (Exception e) {
            handleTerminal(e.toString());
        }
        servicingThreads = new ArrayList<>();
    }
    
    /**
     * To remove threads which are of no use 
     * or not currently alive every after 10 seconds
     */
    public void removeFinishedThreads() {
        ReentrantLock lock = new ReentrantLock();
        lock.lock();
        try{
            for (Peroxy thread : delThreads) {
                servicingThreads.remove(thread);
            }
        } catch (Exception e) {
            handleTerminal(e.toString());
        } finally {
            delThreads = new ArrayList<>();
            lock.unlock();
        }
    }
    
    /**
     * Saves all the cached or blocked Data Structure 
     * values to the file
     */
    private void saveFileManager() {
        //saving cached sites information to file....
        try {
            File cachedSitesFile = new File("cacheManager/cachedSites.txt");
            FileOutputStream fos = new FileOutputStream(cachedSitesFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(cPagesMap);
            oos.close();
            fos.close();
        } catch(IOException e) {
            handleTerminal("Error in saving cached pages to file...");
        }
        
        //saving blocked clients information to file...
        try {
            File blockedClientsFile = new File("blocking/blockedClients.txt");
            FileOutputStream fos = new FileOutputStream(blockedClientsFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(bClientsMap);
            oos.close();
            fos.close();
        } catch(IOException e) {
            handleTerminal("Error in saving blocked clients to file...");
        }
        
        //saving blocked websites information to file...
        try {
            File blockedWebsitesFile = new File("blocking/blockedWebsites.txt");
            FileOutputStream fos = new FileOutputStream(blockedWebsitesFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(bWebsitesMap);
            oos.close();
            fos.close();
        } catch(IOException e) {
            handleTerminal("Error in saving blocked clients to file...");
        }
        
        //saving client log information to file....
        try {
            File clientLogFiles = new File("logManager/logFiles.txt");
            FileOutputStream fos = new FileOutputStream(clientLogFiles);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(logManagerMap);
            oos.close();
            fos.close();
        } catch(IOException e) {
            handleTerminal("Error in saving log files...");
        }
    }
    
    /**
     * Show error messages to the user using terminal
     * @param text 
     */
    private void handleTerminal(String text) {
        try {
            String terminalText = terminal.getText();
            String startEditor = terminalText.substring(0, terminalText.indexOf("<body>")+6);
            String endEditor = terminalText.substring(terminalText.indexOf("<body>")+6, terminalText.length());
            terminal.setText(startEditor+"<b>"+this.APPNAME+" >  "+"</b>" + text+"<br>"+endEditor);
            terminal.setCaretPosition(0);
        } catch(Exception e) {
            terminal.setText("<html><b>"+PeroxyInterface.APPNAME+" >  "+"</b></html>");
        }
    }
    
    /**
     * It will accept string from SIGNALMESSAGE
     * and show the error message to the user - acts as a slot for 
     * raised signal from Peroxy calss
     */
    public void signalToTerminal() {
        try{
            if(!SIGNALMESSAGE.isEmpty()){
                String terminalText = terminal.getText();
                String startEditor = terminalText.substring(0, terminalText.indexOf("<body>")+6);
                String endEditor = terminalText.substring(terminalText.indexOf("<body>")+6, terminalText.length());
                terminal.setText(startEditor+"<b>"+this.APPNAME+" >  "+"</b>" + SIGNALMESSAGE +"<br>"+endEditor);
                //System.out.println(SIGNALMESSAGE);
                SIGNALMESSAGE = "";
                terminal.setCaretPosition(0);
            }
        } catch (Exception e){
            terminal.setText("<html><b>"+PeroxyInterface.APPNAME+" >  "+"</b></html>");
        }
    }
    
    /**
     * saves log of the client
     * @param fileName file name of log file
     * @param file  log file of client
     */
    public static void saveClientLog(String fileName, File file){
        logManagerMap.put(fileName, file);
    }
    
    /**
     * Get Cahced page file from directory
     * @param Url
     * @return File
     */
    public static File getCachedPage(String Url) {
        return cPagesMap.get(Url);
    }
    
    /**
     * Add Cached page file to cahce manager
     * @param urlString url of the web page
     * @param file file name of web page contenting file
     */
    public static void addCachedPage(String urlString, File file) {
        cPagesMap.put(urlString, file);
    }
    /**
     * Delete cached page file from directory
     * @param Url  - URL of cached web page
     */
    public static void delCachedPage(String Url) {
        if(cPagesMap.get(Url) != null){
            cPagesMap.remove(Url);
        }
    }
    
    /**
     * It will delete whole cahce files
     * @param file  - directory which is going to be deleted
     * It will delete files on recurssive nature
     */
    public static void delCache(String fileName) {
        if(fileName == "cacheManager/"){
            int status = JOptionPane.showOptionDialog(null, 
                    "<html><h3>Delete Cache?<h3><p>Are your sure! You want o delete cache of server.<br>"
                            + "Cache improves server's searching time and reliability.<br>You will be messaged when cache get deleted.</p></html>",
                    "Delete Cache? - "+PeroxyInterface.APPNAME,
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    new String[] {"Yes, delete cache!", "No, don't delete cache!"},
                    "No, don't delete cache!"
                    );
            if (status == 1) {
                return ;
            }
        }
        File file = new File(fileName);
        File[] dirs = file.listFiles(File::isDirectory);
        File[] files = file.listFiles(File::isFile);
        try {
            for (File singleFile : files) {
                singleFile.delete();
            }
        } catch (Exception e) {
            //nothing to do now
        }
        
        try {
            for(File dir : dirs) {
                delCache(dir.getAbsoluteFile().getAbsolutePath());
                dir.getAbsoluteFile().delete(); //delete empty directory
            }
        } catch (Exception e) {
            //nothing to do now
        }
        cPagesMap = new HashMap<>();
        
        if(fileName == "cacheManager/"){
            JOptionPane.showMessageDialog(null, 
                "<html><h3>Cache Deleted!</h3><p>Server's cache has been deleted!</p></html>",
                "Cache Deleted! - " + PeroxyInterface.APPNAME,
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    /**
     * Removing client with given ip address
     * @param ip ip of client 
     */
    public static void unblockClient(String ip) {
        if(bClientsMap.get(ip) != null) {
            bClientsMap.remove(ip);
        }
    }
    
    public static void unblockWebsite(String ip) {
        if(bWebsitesMap.get(ip) != null) {
            bWebsitesMap.remove(ip);
        }
    }
    
    /**
     * To find if website is blocked or not
     * @param ip - ip address of website
     * @return boolean
     */
    public static boolean isBlockedWeb(String ip, String websiteName) {
        if(bWebsitesMap.get(ip) != null) {
            return true;
        } else {
            if(bWebsitesMap.values().contains(websiteName)){
                bWebsitesMap.put(ip, websiteName);
                return true; 
            } else {
                return false;
            }
        }
    }
    
    /**
     *  To find if client is blocked or not
     * @param ip - ip address of client
     * @return bolean
     */
    public static boolean isBlockedClient(String ip) {
        if(bClientsMap.get(ip) != null) {
            return true;
        } else {
            return false;   
        }
    }
    
    /**
     * To block website with given ip
     * @param ip - ip of website
     * @param hostName - host name of website
     */
    public static void blockWebsite(String ip, String hostName) {
        bWebsitesMap.put(ip, hostName);
    }
    
    /**
     * Block a website from gui mode
     */
    private void blockWebsite() {
        /**
         * Create new thread and then show dialog to run processes of main window in
         * background without putting them to hold
         */
        Thread tempThread = new Thread() {
            public String hostName;
            public String hostAddress;
            
            @Override
            public void run() {
                String webName = (String)JOptionPane.showInputDialog(null,
                        "<html><h3>Enter a Website to block:<h3><p><font size=\"4\">e.g. www.mywebsite.com</font></p></html>",
                        "Block Website - " + PeroxyInterface.APPNAME,
                        JOptionPane.QUESTION_MESSAGE,
                        null,null,null);
                
                if(!webName.isEmpty()) {
                    if(webName.length()>=4){
                        if(!webName.substring(0, 4).contains("http")){
                            webName = "https://"+webName;
                        }
                    } else {
                        webName = "https://"+webName;
                    }
                } else {
                    return; //do nothing
                }
                
                if(checkUrl(webName)){
                    try {
                        PeroxyInterface.SIGNALMESSAGE += "Website is get blocking : " + webName +" with ip address " + hostAddress + "<br>";
                        hostName = new URL(webName).getHost();
                        hostAddress = InetAddress.getByName(hostName).getHostAddress();
                        
                        //Saving website as blocked website
                        if (!isBlockedWeb(hostAddress, hostName)) {
                            PeroxyInterface.blockWebsite(hostAddress, hostName);
                        } else {
                            JOptionPane.showMessageDialog(null,
                                "<html><h3>Website already Blocked!</h3><p>"+hostName+" is blocked already.</p></html>",
                                hostName +" - "+ PeroxyInterface.APPNAME,
                                JOptionPane.INFORMATION_MESSAGE);
                                return;
                        }
                        
                        PeroxyInterface.SIGNALMESSAGE += "Website blocked : " + webName + " with ip address " + hostAddress+ "<br>";
                        JOptionPane.showMessageDialog(null,
                            "<html><h3>Website Blocked!</h3><p>"+hostName+" is blocked now.</p></html>",
                            hostName +" - "+ PeroxyInterface.APPNAME,
                            JOptionPane.INFORMATION_MESSAGE);
                        return;
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(null,
                            "<html><h3>Url not Correct!</h3><p>You entered incorrect website url.<br><Please enter correct url.</p></html>",
                            hostName +" - "+ PeroxyInterface.APPNAME,
                            JOptionPane.ERROR_MESSAGE);
                        PeroxyInterface.SIGNALMESSAGE += "Website blocking failed : " + webName + " with ip address " + hostAddress+ "<br>";
                        return;
                    }
                } else {
                    JOptionPane.showMessageDialog(null,
                            "<html><h3>Url not Correct!</h3><p>You entered incorrect website url.<br><Please enter correct url.</p></html>",
                            hostName +" - "+ PeroxyInterface.APPNAME,
                            JOptionPane.ERROR_MESSAGE);
                    PeroxyInterface.SIGNALMESSAGE += "Website blocking failed : " + webName+ " with ip address " + hostAddress+ "<br>";
                    return;
                }
            }
            
            public boolean checkUrl(String urlString) {
                try {
                    new URL(urlString).toURI();
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
        };
        
        //starting thread
        tempThread.start();
    }

    /**
     * To block a client with given ip
     * @param ip - ip of client
     * @param hostName - host of client 
     */
    public static void blockClient(String ip, String hostName) {
        bClientsMap.put(ip, hostName);
    }
    
    /**
     * Update clients on the table of main window
     */
    public void updateActiveClientOnWindowTable(){
        HashMap<String, ArrayList<String>> tempMap = activeClientDetails;
        activeClientDetails = new HashMap<>();
        DefaultTableModel tableModel = new DefaultTableModel();
        tableModel.addColumn("S.No.");
        tableModel.addColumn("IP Address");
        tableModel.addColumn("Host Name");
        tableModel.addColumn("Connection Time");
        int count = 0;
        for (String key : tempMap.keySet()){
            String[] array = {Integer.toString(count+1),key, tempMap.get(key).get(0),tempMap.get(key).get(1)};
            tableModel.insertRow(count, array);
            count += 1;
        }
        panelTable.setModel(tableModel);
        panelTable.getColumnModel().getColumn(0).setResizable(false);
        panelTable.getColumnModel().getColumn(0).setPreferredWidth(20);
        tableLabel.setText("Server Connected - Total Clients ("+tempMap.size()+")");
    }
    
    /**
     * Update clients on the all detail of active clients
     */
    public void updateActiveClientAllDetail() {
        HashMap<String, ArrayList<String>> tempMap = activeClientAllDetails;
        activeClientAllDetails = new HashMap<>();
        String[] columns = {"S.No.","IP Address","Connection Time",  "Host Name", "Browser Detail", "Connection Type", "End Time", ""};
        String[][] data = new String[tempMap.size()][];
        int count = 0;
        for (String key : tempMap.keySet()) {
            String[] array = {Integer.toString(count+1),key ,tempMap.get(key).get(0),tempMap.get(key).get(1), 
                tempMap.get(key).get(2), tempMap.get(key).get(3),
                tempMap.get(key).get(4), "Block!"};
            data[count] = array;
            count += 1;
        }
        
        
        Thread dialog = new Thread(new ShowTable(this, true,
                data, columns));
        dialog.start();
    }
    
    /**
     * Update all clients on the server
     */
    public void updateAllClients() {
        HashMap<String, ArrayList<String>> tempMap = allClients;
        String[] columns = {"S.No.", "IP Address","Host Name", "Connection Time", "Browser Detail", "Connection Type", "End Time", ""};
        String[][] data = new String[tempMap.size()][];
        int count = 0;
        for (String key : tempMap.keySet()) {
            if (bClientsMap.keySet().contains(key)){
                String[] array = {Integer.toString(count+1), key,tempMap.get(key).get(0),tempMap.get(key).get(1),
                tempMap.get(key).get(2), tempMap.get(key).get(3),
                tempMap.get(key).get(4), "Unblock!!"};
                
                data[count] = array;
                count += 1;
            } else {
                String[] array = {Integer.toString(count+1), key, tempMap.get(key).get(0),tempMap.get(key).get(1),
                tempMap.get(key).get(2), tempMap.get(key).get(3),
                tempMap.get(key).get(4), "Block!"};
                
                data[count] = array;
                count += 1;
            }
        }
        
        
        Thread dialog = new Thread(new ShowTable(this, true,
                data, columns));
        dialog.start();
    }
    
    /**
     * This is will show blocked clients to the admin
     */
    public void showBlockClients() {
        String[] columns = {"S.No.", "IP Address", "Host Name", ""};
        String[][] data = new String[bClientsMap.size()][];
        int count = 0;
        for (String key: bClientsMap.keySet()) {
            String[] array = {Integer.toString(count+1), key, bClientsMap.get(key), "Unblock!"};
            data[count] = array;
            count += 1;
        }
        
        Thread dialog = new Thread(new ShowTable(this, true,
                data, columns));
        dialog.start();
    }
    
    /**
     * Show blocked website to the admin to unblock
     */
    public void showBlockWebsites() {
        String[] columns = {"S.No.", "IP Address", "Website", ""};
        String[][] data = new String[bWebsitesMap.size()][];
        int count = 0;
        for (String key: bWebsitesMap.keySet()) {
            String[] array = {Integer.toString(count+1), key, bWebsitesMap.get(key), "Unblock It!"};
            data[count] = array;
            count += 1;
        }
        
        Thread dialog = new Thread(new ShowTable(this, true,
                data, columns));
        dialog.start();
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jMenu1 = new javax.swing.JMenu();
        jMenu4 = new javax.swing.JMenu();
        jSeparator2 = new javax.swing.JSeparator();
        jSeparator4 = new javax.swing.JSeparator();
        jSeparator5 = new javax.swing.JSeparator();
        toolBar = new javax.swing.JToolBar();
        tStartButton = new javax.swing.JButton();
        tCloseButton = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JToolBar.Separator();
        restartServerButton = new javax.swing.JButton();
        jButton1 = new javax.swing.JButton();
        filler4 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));
        tLogButton = new javax.swing.JButton();
        tCacheButton = new javax.swing.JButton();
        tGettingButton = new javax.swing.JButton();
        jSeparator3 = new javax.swing.JToolBar.Separator();
        tAboutButton = new javax.swing.JButton();
        aboutUsToolButton = new javax.swing.JButton();
        filler3 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));
        filler7 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 32767));
        filler6 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));
        filler5 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));
        tExitButton = new javax.swing.JButton();
        filler8 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));
        mainPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        messageLabel = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        panelTable = new javax.swing.JTable();
        tableLabel = new javax.swing.JLabel();
        blockWebsiteButton = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        filler2 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 32767));
        unblockUserButton = new javax.swing.JButton();
        statusLabel = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        terminal = new javax.swing.JEditorPane();
        menuBar = new javax.swing.JMenuBar();
        display = new javax.swing.JMenu();
        clientsMenu = new javax.swing.JMenu();
        allClientsMenu = new javax.swing.JMenuItem();
        activeClientsMenu = new javax.swing.JMenuItem();
        blockedClientsMenu = new javax.swing.JMenuItem();
        websitesMenu = new javax.swing.JMenu();
        jMenuItem5 = new javax.swing.JMenuItem();
        jMenuItem6 = new javax.swing.JMenuItem();
        logMenu = new javax.swing.JMenuItem();
        settings = new javax.swing.JMenu();
        buClientMenu = new javax.swing.JMenuItem();
        delCacheMenu = new javax.swing.JMenuItem();
        restrictionMenu = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        startServerMenu = new javax.swing.JMenuItem();
        closeServerMenu = new javax.swing.JMenuItem();
        exitMenu = new javax.swing.JMenuItem();
        jMenu3 = new javax.swing.JMenu();
        aboutPeroxyMenu = new javax.swing.JMenuItem();
        aboutUsMenu = new javax.swing.JMenuItem();
        jMenu5 = new javax.swing.JMenu();
        peroxyRulesMenu = new javax.swing.JMenuItem();
        gsPeroxyMenu = new javax.swing.JMenuItem();

        jMenu1.setText("jMenu1");

        jMenu4.setText("jMenu4");

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
        });

        toolBar.setFloatable(false);
        toolBar.setRollover(true);

        tStartButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/peroxyimages/connect.png"))); // NOI18N
        tStartButton.setToolTipText("Start Connection");
        tStartButton.setFocusable(false);
        tStartButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        tStartButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        tStartButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tStartButtonActionPerformed(evt);
            }
        });
        toolBar.add(tStartButton);

        tCloseButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/peroxyimages/disconnect.png"))); // NOI18N
        tCloseButton.setToolTipText("Close Connection");
        tCloseButton.setFocusable(false);
        tCloseButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        tCloseButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        tCloseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tCloseButtonActionPerformed(evt);
            }
        });
        toolBar.add(tCloseButton);
        toolBar.add(jSeparator1);

        restartServerButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/peroxyimages/restart.png"))); // NOI18N
        restartServerButton.setToolTipText("Restart the Server Connection");
        restartServerButton.setFocusable(false);
        restartServerButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        restartServerButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        restartServerButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                restartServerButtonActionPerformed(evt);
            }
        });
        toolBar.add(restartServerButton);

        jButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/peroxyimages/setting.png"))); // NOI18N
        jButton1.setFocusable(false);
        jButton1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton1.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        toolBar.add(jButton1);
        toolBar.add(filler4);

        tLogButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/peroxyimages/logmanager.png"))); // NOI18N
        tLogButton.setToolTipText("Log Manager");
        tLogButton.setFocusable(false);
        tLogButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        tLogButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        tLogButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tLogButtonActionPerformed(evt);
            }
        });
        toolBar.add(tLogButton);

        tCacheButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/peroxyimages/delcache.png"))); // NOI18N
        tCacheButton.setToolTipText("Delete cahce files");
        tCacheButton.setFocusable(false);
        tCacheButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        tCacheButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        tCacheButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tCacheButtonActionPerformed(evt);
            }
        });
        toolBar.add(tCacheButton);

        tGettingButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/peroxyimages/rules.png"))); // NOI18N
        tGettingButton.setToolTipText("Getting started with Peroxy");
        tGettingButton.setFocusable(false);
        tGettingButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        tGettingButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        toolBar.add(tGettingButton);
        toolBar.add(jSeparator3);

        tAboutButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/peroxyimages/peroxy.jpg"))); // NOI18N
        tAboutButton.setToolTipText("About Peroxy");
        tAboutButton.setFocusable(false);
        tAboutButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        tAboutButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        tAboutButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tAboutButtonActionPerformed(evt);
            }
        });
        toolBar.add(tAboutButton);

        aboutUsToolButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/peroxyimages/aboutus.png"))); // NOI18N
        aboutUsToolButton.setToolTipText("About Us");
        aboutUsToolButton.setFocusable(false);
        aboutUsToolButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        aboutUsToolButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        aboutUsToolButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutUsToolButtonActionPerformed(evt);
            }
        });
        toolBar.add(aboutUsToolButton);
        toolBar.add(filler3);
        toolBar.add(filler7);
        toolBar.add(filler6);
        toolBar.add(filler5);
        toolBar.add(filler1);

        tExitButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/peroxyimages/exit.png"))); // NOI18N
        tExitButton.setToolTipText("Exit from Server");
        tExitButton.setFocusable(false);
        tExitButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        tExitButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        tExitButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tExitButtonActionPerformed(evt);
            }
        });
        toolBar.add(tExitButton);
        toolBar.add(filler8);

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("Welcome to Peroxy 0.1 Admin Panel");

        messageLabel.setFont(new java.awt.Font("Ubuntu", 1, 15)); // NOI18N
        messageLabel.setForeground(new java.awt.Color(247, 9, 15));
        messageLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

        panelTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "S.No.", "IP Address", "Host Name", "From"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        panelTable.setGridColor(new java.awt.Color(76, 64, 64));
        panelTable.setSurrendersFocusOnKeystroke(true);
        panelTable.getTableHeader().setReorderingAllowed(false);
        jScrollPane2.setViewportView(panelTable);
        if (panelTable.getColumnModel().getColumnCount() > 0) {
            panelTable.getColumnModel().getColumn(0).setResizable(false);
            panelTable.getColumnModel().getColumn(0).setPreferredWidth(20);
        }

        tableLabel.setFont(new java.awt.Font("Ubuntu", 1, 15)); // NOI18N
        tableLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

        blockWebsiteButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/peroxyimages/blockedClient.jpeg"))); // NOI18N
        blockWebsiteButton.setText("Block Website");
        blockWebsiteButton.setToolTipText("Block website");
        blockWebsiteButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                blockWebsiteButtonActionPerformed(evt);
            }
        });

        jButton2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/peroxyimages/blockedWeb.png"))); // NOI18N
        jButton2.setText("      Block User");
        jButton2.setToolTipText("Block user");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jButton3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/peroxyimages/logmanager.png"))); // NOI18N
        jButton3.setText("Log Manager");
        jButton3.setToolTipText("Log Manager");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        jButton4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/peroxyimages/delcache.png"))); // NOI18N
        jButton4.setText("   Del Cache");
        jButton4.setToolTipText("Delete cache files");
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });

        unblockUserButton.setText("Unblock User");
        unblockUserButton.setToolTipText("Unblock user");
        unblockUserButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                unblockUserButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(messageLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(tableLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(filler2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(jButton3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButton2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(blockWebsiteButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButton4, javax.swing.GroupLayout.DEFAULT_SIZE, 152, Short.MAX_VALUE)
                    .addComponent(unblockUserButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
            .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, 702, Short.MAX_VALUE)
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(messageLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(blockWebsiteButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(tableLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addComponent(jButton2)
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(mainPanelLayout.createSequentialGroup()
                                .addGap(31, 31, 31)
                                .addComponent(filler2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(mainPanelLayout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(unblockUserButton)))
                        .addGap(80, 80, 80)
                        .addComponent(jButton3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton4)
                        .addContainerGap(67, Short.MAX_VALUE))
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)))
        );

        statusLabel.setFont(new java.awt.Font("Ubuntu", 1, 15)); // NOI18N
        statusLabel.setForeground(new java.awt.Color(9, 105, 247));
        statusLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        statusLabel.setText("2018 Copyrights reserved by Peroxy-0.1");

        terminal.setEditable(false);
        terminal.setToolTipText("Output terminal");
        terminal.setName("terminal"); // NOI18N
        jScrollPane3.setViewportView(terminal);

        display.setText("Display");

        clientsMenu.setText("Users");

        allClientsMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/peroxyimages/user.png"))); // NOI18N
        allClientsMenu.setText("All Users");
        allClientsMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                allClientsMenuActionPerformed(evt);
            }
        });
        clientsMenu.add(allClientsMenu);

        activeClientsMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/peroxyimages/activeUser.png"))); // NOI18N
        activeClientsMenu.setText("Active Users");
        activeClientsMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                activeClientsMenuActionPerformed(evt);
            }
        });
        clientsMenu.add(activeClientsMenu);

        blockedClientsMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/peroxyimages/blockedWeb.png"))); // NOI18N
        blockedClientsMenu.setText("Blocked Users");
        blockedClientsMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                blockedClientsMenuActionPerformed(evt);
            }
        });
        clientsMenu.add(blockedClientsMenu);

        display.add(clientsMenu);

        websitesMenu.setText("Websites");

        jMenuItem5.setIcon(new javax.swing.ImageIcon(getClass().getResource("/peroxyimages/blockedWeb.png"))); // NOI18N
        jMenuItem5.setText("Blocked Websites");
        jMenuItem5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem5ActionPerformed(evt);
            }
        });
        websitesMenu.add(jMenuItem5);

        jMenuItem6.setIcon(new javax.swing.ImageIcon(getClass().getResource("/peroxyimages/delcache.png"))); // NOI18N
        jMenuItem6.setText("Delete Cache");
        jMenuItem6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem6ActionPerformed(evt);
            }
        });
        websitesMenu.add(jMenuItem6);

        display.add(websitesMenu);

        logMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_L, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        logMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/peroxyimages/logmanager.png"))); // NOI18N
        logMenu.setText("Log Manager");
        logMenu.setToolTipText("Log Manager");
        logMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                logMenuActionPerformed(evt);
            }
        });
        display.add(logMenu);

        menuBar.add(display);

        settings.setText("Settings");

        buClientMenu.setText("Block/Unblock Users");
        buClientMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buClientMenuActionPerformed(evt);
            }
        });
        settings.add(buClientMenu);

        delCacheMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/peroxyimages/delcache.png"))); // NOI18N
        delCacheMenu.setText("Delete Cached Page");
        settings.add(delCacheMenu);

        restrictionMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/peroxyimages/setting.png"))); // NOI18N
        restrictionMenu.setText("Set Restrictions");
        restrictionMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                restrictionMenuActionPerformed(evt);
            }
        });
        settings.add(restrictionMenu);

        menuBar.add(settings);

        jMenu2.setText("Connection");

        startServerMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, java.awt.event.InputEvent.CTRL_MASK));
        startServerMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/peroxyimages/connect.png"))); // NOI18N
        startServerMenu.setText("Start Server");
        startServerMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startServerMenuActionPerformed(evt);
            }
        });
        jMenu2.add(startServerMenu);

        closeServerMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_D, java.awt.event.InputEvent.CTRL_MASK));
        closeServerMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/peroxyimages/disconnect.png"))); // NOI18N
        closeServerMenu.setText("Close Server");
        closeServerMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeServerMenuActionPerformed(evt);
            }
        });
        jMenu2.add(closeServerMenu);

        exitMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_X, java.awt.event.InputEvent.CTRL_MASK));
        exitMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/peroxyimages/exit.png"))); // NOI18N
        exitMenu.setText("Exit");
        exitMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitMenuActionPerformed(evt);
            }
        });
        jMenu2.add(exitMenu);

        menuBar.add(jMenu2);

        jMenu3.setText("About");

        aboutPeroxyMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/peroxyimages/peroxy.jpg"))); // NOI18N
        aboutPeroxyMenu.setText("About Peroxy");
        aboutPeroxyMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutPeroxyMenuActionPerformed(evt);
            }
        });
        jMenu3.add(aboutPeroxyMenu);

        aboutUsMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/peroxyimages/aboutus.png"))); // NOI18N
        aboutUsMenu.setText("About Us");
        aboutUsMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutUsMenuActionPerformed(evt);
            }
        });
        jMenu3.add(aboutUsMenu);

        menuBar.add(jMenu3);

        jMenu5.setText("Help");

        peroxyRulesMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F2, 0));
        peroxyRulesMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/peroxyimages/getstarted.png"))); // NOI18N
        peroxyRulesMenu.setText("Peroxy Rules");
        jMenu5.add(peroxyRulesMenu);

        gsPeroxyMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F1, 0));
        gsPeroxyMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/peroxyimages/rules.png"))); // NOI18N
        gsPeroxyMenu.setText("Getting Started with Peroxy");
        jMenu5.add(gsPeroxyMenu);

        menuBar.add(jMenu5);

        setJMenuBar(menuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(toolBar, javax.swing.GroupLayout.DEFAULT_SIZE, 702, Short.MAX_VALUE)
            .addComponent(mainPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(statusLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 702, Short.MAX_VALUE)
            .addComponent(jScrollPane3, javax.swing.GroupLayout.Alignment.TRAILING)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(toolBar, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(mainPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 125, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(statusLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void tCloseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tCloseButtonActionPerformed
        // Closing Peroxy Connection to server
        closeConnection();
    }//GEN-LAST:event_tCloseButtonActionPerformed

    private void blockWebsiteButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_blockWebsiteButtonActionPerformed
        // calling blockWebsite method
        blockWebsite();
    }//GEN-LAST:event_blockWebsiteButtonActionPerformed

    private void tStartButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tStartButtonActionPerformed
        // Starting connection of server
        startConnection();
    }//GEN-LAST:event_tStartButtonActionPerformed

    private void startServerMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startServerMenuActionPerformed
        // Start server connection
        startConnection();
    }//GEN-LAST:event_startServerMenuActionPerformed

    private void closeServerMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeServerMenuActionPerformed
        // Close Peroxy server connection
        closeConnection();
    }//GEN-LAST:event_closeServerMenuActionPerformed

    private void tExitButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tExitButtonActionPerformed
        //Invoke window dispatch event to trigger close event
        this.dispatchEvent(new WindowEvent(this,WindowEvent.WINDOW_CLOSING));
    }//GEN-LAST:event_tExitButtonActionPerformed

    private void exitMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitMenuActionPerformed
        //Invoke window dispatch event to trigger close event
        this.dispatchEvent(new WindowEvent(this,WindowEvent.WINDOW_CLOSING));
    }//GEN-LAST:event_exitMenuActionPerformed

    private void activeClientsMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_activeClientsMenuActionPerformed
        //Calling active client dialgo box to show
        updateActiveClientAllDetail();
    }//GEN-LAST:event_activeClientsMenuActionPerformed

    private void allClientsMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_allClientsMenuActionPerformed
        //Show all the clients on the server after server is started
        updateAllClients();
    }//GEN-LAST:event_allClientsMenuActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        // Showing all clients to the user to block someone
        updateAllClients();
    }//GEN-LAST:event_jButton2ActionPerformed

    private void blockedClientsMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_blockedClientsMenuActionPerformed
        //Showing blocked clients to the user to unblock 
        showBlockClients();
    }//GEN-LAST:event_blockedClientsMenuActionPerformed

    private void unblockUserButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_unblockUserButtonActionPerformed
        // Unblock blocked users
        showBlockClients();
    }//GEN-LAST:event_unblockUserButtonActionPerformed

    private void jMenuItem5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem5ActionPerformed
        // Show Blocked websites to the admin
        showBlockWebsites();
    }//GEN-LAST:event_jMenuItem5ActionPerformed

    private void buClientMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buClientMenuActionPerformed
        // Show options to block and unblock clients
        showBlockClients();
    }//GEN-LAST:event_buClientMenuActionPerformed

    private void aboutUsToolButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutUsToolButtonActionPerformed
        // show about us dialog box
        Thread aboutus = new Thread(new AboutUs(this,true));
        aboutus.start();
    }//GEN-LAST:event_aboutUsToolButtonActionPerformed

    private void tAboutButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tAboutButtonActionPerformed
        // show about dialog
        Thread about = new Thread(new AboutPeroxy(this,true));
        about.start();
    }//GEN-LAST:event_tAboutButtonActionPerformed

    private void aboutPeroxyMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutPeroxyMenuActionPerformed
        // show about dialog
        Thread about = new Thread(new AboutPeroxy(this,true));
        about.start(); 
    }//GEN-LAST:event_aboutPeroxyMenuActionPerformed

    private void aboutUsMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutUsMenuActionPerformed
        // show about us dialog box
        Thread aboutus = new Thread(new AboutUs(this,true));
        aboutus.start();
    }//GEN-LAST:event_aboutUsMenuActionPerformed

    private void restartServerButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_restartServerButtonActionPerformed
        // restarting connecion
        closeConnection();
        startConnection();
    }//GEN-LAST:event_restartServerButtonActionPerformed

    private void restrictionMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_restrictionMenuActionPerformed
        // show restriction dialog
        Thread restriction = new Thread(new Restrictions(this, true));
        restriction.start();
    }//GEN-LAST:event_restrictionMenuActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        // show restriction dialog
        Thread restriction = new Thread(new Restrictions(this, true));
        restriction.start();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        //show log manager to clients
        Thread logManager = new Thread(new LogManager(this, true));
        logManager.start();
    }//GEN-LAST:event_jButton3ActionPerformed

    private void logMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_logMenuActionPerformed
        //show log manager to clients
        Thread logManager = new Thread(new LogManager(this, true));
        logManager.start();
    }//GEN-LAST:event_logMenuActionPerformed

    private void tLogButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tLogButtonActionPerformed
        //show log manager to clients
        Thread logManager = new Thread(new LogManager(this, true));
        logManager.start();
    }//GEN-LAST:event_tLogButtonActionPerformed

    private void tCacheButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tCacheButtonActionPerformed
        // delete whole cache manager files
        delCache("cacheManager/");
    }//GEN-LAST:event_tCacheButtonActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        // delete whole cache manager files
        delCache(".cacheManager/"); 
    }//GEN-LAST:event_jButton4ActionPerformed

    private void jMenuItem6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem6ActionPerformed
        // delete whole cache manager files
        delCache(".cacheManager/"); 
    }//GEN-LAST:event_jMenuItem6ActionPerformed

    private void formWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowOpened
        // open window on full screen
        setExtendedState(JFrame.MAXIMIZED_BOTH); 
    }//GEN-LAST:event_formWindowOpened

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(PeroxyInterface.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(PeroxyInterface.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(PeroxyInterface.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(PeroxyInterface.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        SplashScreen ss = new SplashScreen(null,true);
        Thread thread = new Thread(ss);
        thread.start();
        sleepThread(); 
        ss.dispose();
        
        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                PeroxyInterface gui = new PeroxyInterface();
                gui.setVisible(true);
                
                /**
                 * Creating terminal timer and timer task for 
                 * updating terminal messages
                 */
                Timer terminalTimer = new Timer();
                TimerTask terminalTimerTask = new TimerTask(){
                        @Override
                        public void run() {
                            gui.signalToTerminal();
                        }};
                
                /**
                 * Deleting dead threads from servicingThreads
                 */
                Timer removeThreadTimer = new Timer();
                TimerTask removeThreadTimerTask = new TimerTask() {
                    @Override
                    public void run() {
                        gui.removeFinishedThreads();
                    }
                };
                
                /**
                 * Updating active client table on main window Timer
                 */
                Timer tableClientTimer = new Timer();
                TimerTask tableClientTimerTask = new TimerTask() {
                    @Override
                    public void run() {
                        gui.updateActiveClientOnWindowTable();
                    }
                };
                
                //Executing terminal timer every after 1 second
                terminalTimer.schedule(terminalTimerTask, 0, 100);
                //Executing remove thread timer every after 10 seconds
                removeThreadTimer.schedule(removeThreadTimerTask,0 ,1000);
                //Executing active client update table timer every after 20 seconds
                tableClientTimer.schedule(tableClientTimerTask,0, 2000);
                
                /**
                * Adding window close event for 
                * confirmatio of closing the server
                */
                gui.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                gui.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent windowEvent) {
                        //ask question about closing the window
                        if(PeroxyInterface.running) {
                            if(gui.closeConnection()){
                                System.exit(0);
                            }
                        } else {
                            System.exit(0);
                        }
                    }
                });
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem aboutPeroxyMenu;
    private javax.swing.JMenuItem aboutUsMenu;
    private javax.swing.JButton aboutUsToolButton;
    private javax.swing.JMenuItem activeClientsMenu;
    private javax.swing.JMenuItem allClientsMenu;
    private javax.swing.JButton blockWebsiteButton;
    private javax.swing.JMenuItem blockedClientsMenu;
    private javax.swing.JMenuItem buClientMenu;
    private javax.swing.JMenu clientsMenu;
    private javax.swing.JMenuItem closeServerMenu;
    private javax.swing.JMenuItem delCacheMenu;
    private javax.swing.JMenu display;
    private javax.swing.JMenuItem exitMenu;
    private javax.swing.Box.Filler filler1;
    private javax.swing.Box.Filler filler2;
    private javax.swing.Box.Filler filler3;
    private javax.swing.Box.Filler filler4;
    private javax.swing.Box.Filler filler5;
    private javax.swing.Box.Filler filler6;
    private javax.swing.Box.Filler filler7;
    private javax.swing.Box.Filler filler8;
    private javax.swing.JMenuItem gsPeroxyMenu;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenu jMenu4;
    private javax.swing.JMenu jMenu5;
    private javax.swing.JMenuItem jMenuItem5;
    private javax.swing.JMenuItem jMenuItem6;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JToolBar.Separator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JToolBar.Separator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JMenuItem logMenu;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JLabel messageLabel;
    private javax.swing.JTable panelTable;
    private javax.swing.JMenuItem peroxyRulesMenu;
    private javax.swing.JButton restartServerButton;
    private javax.swing.JMenuItem restrictionMenu;
    private javax.swing.JMenu settings;
    private javax.swing.JMenuItem startServerMenu;
    private javax.swing.JLabel statusLabel;
    private javax.swing.JButton tAboutButton;
    private javax.swing.JButton tCacheButton;
    private javax.swing.JButton tCloseButton;
    private javax.swing.JButton tExitButton;
    private javax.swing.JButton tGettingButton;
    private javax.swing.JButton tLogButton;
    private javax.swing.JButton tStartButton;
    private javax.swing.JLabel tableLabel;
    private javax.swing.JEditorPane terminal;
    private javax.swing.JToolBar toolBar;
    private javax.swing.JButton unblockUserButton;
    private javax.swing.JMenu websitesMenu;
    // End of variables declaration//GEN-END:variables
}
