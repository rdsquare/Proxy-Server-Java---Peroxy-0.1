/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package peroxy;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.File;
import java.net.Socket;
import java.net.SocketException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.net.SocketTimeoutException;
import java.util.Date;
import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;

/**
 *
 * @author rd_square
 */
public class Peroxy extends Thread{

    /**
     * Creating socket to connect with clients
     * over server socket
     */
    Socket clientSocket;
    
    /**
     * Creating Buffer Reader object to read over clients
     * input stream on server socket port
     */
    BufferedReader fromClientBr;
    
    /**
     * Creating Buffer Writer object to write over client's
     * output stream on server socket port
     */
    BufferedWriter toClientBw;
    
    /**
     * This thread will be used to transfer data directly from server to clients
     * or clients to server without interfering with data for security aspects
     * , this object will be used to close it when completed!
     */
    private Thread httpsClientToServer;
    
    /**
     * save lists of the log entries of the log file
     */
    private ArrayList<String> logFileList;
    
    /**
     * Constructor of Peroxy class to initialize client socket 
     * and Burffered Reader and Writer
     * @param clientSocket - socket connected to any client over server socket
     */
    Peroxy(Socket clientSocket) {
        this.clientSocket = clientSocket;
        logFileList = new ArrayList<>();
        try {
            this.clientSocket.setSoTimeout(PeroxyInterface.clientSocketTimeout); //timeout the clientSocket after 20 seconds
            fromClientBr = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
            toClientBw = new BufferedWriter(new OutputStreamWriter(this.clientSocket.getOutputStream()));
        } catch (SocketException e) {
            PeroxyInterface.SIGNALMESSAGE += e.toString()+"<br>";
        } catch (IOException e) {
            PeroxyInterface.SIGNALMESSAGE += e.toString()+"<br>";
        }
    }
    
    /**
     * This will reads and examine the request string
     * and call the apropriate method based on request type
     * like GET HTTP (cached or non-cached) or HTTPs
     */
    @Override
    public void run() {
        //Setting client as active client
        String cIP = clientSocket.getInetAddress().getHostAddress();
        String cName = clientSocket.getInetAddress().getHostName();
        String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
        ArrayList<String> clientDataonIP = new ArrayList<>();
        
        //Adding data to clientdataonip
        clientDataonIP.add(cName);
        clientDataonIP.add(timeStamp);
        PeroxyInterface.activeClientDetails.put(cIP, clientDataonIP);
        
        
        // Getting request from client
        String requestString= "";
        try {
            requestString = fromClientBr.readLine();
        } catch (IOException e) {
            PeroxyInterface.SIGNALMESSAGE += e.toString()+"<br>";
            PeroxyInterface.SIGNALMESSAGE += "Error reading request from client...<br>";
        }
        
        
        /**
         * 
         * Temporary removing data from stream
         * 
         */
        HashMap<String, String> tempStoreDataofClient = new HashMap<>();
        try {
            String line;
            tempStoreDataofClient.put("Host", "");
            tempStoreDataofClient.put("User-Agent", "");
            tempStoreDataofClient.put("Connection", "");
            while((line = fromClientBr.readLine()) != null) {
                // Go to the log manager as well as client detail
                if(line.contains("Host") || line.contains("User-Agent") || line.contains("Connection")){
                    tempStoreDataofClient.put(line.substring(0, line.indexOf(":")),
                            line.substring(line.indexOf(":")+1));
                }
            }
            clientDataonIP.add(tempStoreDataofClient.get("User-Agent"));  
            clientDataonIP.add(tempStoreDataofClient.get("Connection"));
        } catch(Exception e) {
            //Continue normal processing
        }
        
        //Parse URL
        PeroxyInterface.SIGNALMESSAGE += "Request Received: " + requestString+ "<br>";
        
        String tokens[] = requestString.split("\\s+");
        
        /**
         * If URL String is not http requested then make it http request
         * Restriction of Peroxy - only parse http or https requests
         */
        try {
            if(tokens[1].length() > 4) {
                if(!tokens[1].substring(0,4).equals("http")){
                    tokens[1] = "http://" + tokens[1]; //making request http request if not so
                }
            } else {
                tokens[1] = "http://" + tokens[1]; //making request http reques if not so
            }
        } catch (Exception e) {
            PeroxyInterface.SIGNALMESSAGE += "No http request - request failed...<br>";
            return; //Thread in Exception so return - http request failed
        }
        
        //check if site is blocked
        String websiteIp;
        String websiteUrl;
        try {
            websiteUrl = new URL(tokens[1]).getHost();
            websiteIp = InetAddress.getByName(websiteUrl).getHostAddress();
            
            if(PeroxyInterface.isBlockedWeb(websiteIp, websiteUrl)) {
                PeroxyInterface.SIGNALMESSAGE += "Blocked site " + websiteUrl + " requested...<br>";
                blockedSiteRequested(websiteUrl);
                return;
            }
        } catch (UnknownHostException | MalformedURLException e) {
            PeroxyInterface.SIGNALMESSAGE += e.toString()+"<br>";
        }
        
        //check if client is blocked
        try {
            String clientIp = clientSocket.getInetAddress().getHostAddress();
            String clientName = clientSocket.getInetAddress().getHostName();
            
            if(PeroxyInterface.isBlockedClient(clientIp)){
                PeroxyInterface.SIGNALMESSAGE += "Blocked client trying to access " + clientName + " the server...<br>";
                blockedClientRequesting(clientName);
                return;
            }
        } catch (Exception e) {
            PeroxyInterface.SIGNALMESSAGE += e.toString()+"<br>";
        }
        
        if(tokens[0].equals("CONNECT")){
            PeroxyInterface.SIGNALMESSAGE += "HTTPS request for : " + tokens[1]+"<br>";
            handleHttpsRequest(tokens[1]);
        } else {
            //checking for web page in cache manager
            File file;
            if((file=PeroxyInterface.getCachedPage(tokens[1])) != null){
                PeroxyInterface.SIGNALMESSAGE += "Cached copy found for - " + tokens[1]+"<br>";
                sendCachedCopyToClient(file);
            } else {
                PeroxyInterface.SIGNALMESSAGE += "HTTP GET request for - " + tokens[1]+"<br>";
                sendNonCachedCopyToClient(tokens[1]);
            }
        }
        
        
        
        //Setting data of full detail of active clients
        clientDataonIP.add(tempStoreDataofClient.get("User-Agent"));
        clientDataonIP.add(tempStoreDataofClient.get("Connection"));
        String endTime = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
        clientDataonIP.add(endTime);
        PeroxyInterface.activeClientAllDetails.put(cIP, clientDataonIP);
        PeroxyInterface.allClients.put(cIP, clientDataonIP);
        
        String RequestType = "";
        if (tokens[0].equals("CONNECT")){
            RequestType = "https";
        } else {
            RequestType = "http";
        }
        String httpVersion = tokens[2]; 
        String webHost = tempStoreDataofClient.get("Host");
        String connectionType = tempStoreDataofClient.get("Connection");
        String userAgent = tempStoreDataofClient.get("User-Agent");
        userAgent = userAgent.trim();
        String product = userAgent.substring(0, userAgent.indexOf(" "));
        String osDetail = userAgent.substring(userAgent.indexOf("("), userAgent.indexOf(")"));
        String browser = userAgent.substring(userAgent.lastIndexOf(" "));
        
        String tempString = webHost.replace(".", "0");
        tempString = tempString.replace(":", "_");
        LocalDateTime now = LocalDateTime.now();
        String logFileName = Integer.toString(now.getYear())+ "/" +
                Integer.toString(now.getMonthValue()) + "/"+
                Integer.toString(now.getDayOfMonth()) + "/" + 
                cIP.replace(".", "0") + "/" +
                tempString+
                Integer.toString(now.getMinute())+ ".txt";
        logFileName = ".logManager/"+logFileName;
        File logFile = new File(logFileName);
        //dateTime - timeStamp
        //Ip -client cIP
        //Host-Name-client cName
        //Request-Type RequestType
        //Http-Version httpVersion
        //WebHost webHost
        //Request-Query tokens[1]
        //Connection-Type connectionType
        //product  product
        //Browser  browser
        //OS Detial osDetail
        logFileList.add(timeStamp);
        logFileList.add(cIP);
        logFileList.add(cName);
        logFileList.add(RequestType);
        logFileList.add(httpVersion);
        logFileList.add(webHost);
        logFileList.add(tokens[1]);
        logFileList.add(connectionType);
        logFileList.add(product);
        logFileList.add(browser);
        logFileList.add(osDetail);
        
        PeroxyInterface.SIGNALMESSAGE += "Saving log of client....<br>";
        try {
            if (! logFile.exists()) {
                PeroxyInterface.SIGNALMESSAGE += "Log file Creating...<br>";
                logFile.getParentFile().mkdirs(); //making all directories
                logFile.createNewFile(); //Creatin new file
            }
            
            FileOutputStream fos = new FileOutputStream(logFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(logFileList);
            PeroxyInterface.saveClientLog(logFileName, logFile);
            fos.close();
            oos.close();
            
        } catch (IOException e) {
            PeroxyInterface.SIGNALMESSAGE += "Error while saving log file...Log not saved.<br>";
        }
        //Sending thread to delThreads
        try{
            clientSocket.close();
        } catch (Exception e) {
            //continue
        }
        PeroxyInterface.delThreads.add(this);
    }
    
    private void blockedSiteRequested(String website) {
        PeroxyInterface.SIGNALMESSAGE += "Blocked site requested : "+website+"<br>";
        String response = "HTTP/1.0 403 Access Forbidden \n"+
                "User-Agent: " + PeroxyInterface.APPNAME+ "\n"+
                "\r\n";
        //System.out.println("Blocked site requested..." + website);
        System.out.println("\n");
        try {
            toClientBw.write(response);
            toClientBw.flush();
            
            //cloasing all down stream
            if(toClientBw != null){
                toClientBw.close();
            }
            
        } catch (IOException e) {
            PeroxyInterface.SIGNALMESSAGE += e.toString()+"<br>";
        }
    }
    
    private void blockedClientRequesting(String client) {
        PeroxyInterface.SIGNALMESSAGE += "Blocked client requesting : "+client+"<br>";
        String response = "HTTP/1.0 403 Access Forbidden \n"+
                "User-Agent: " + PeroxyInterface.APPNAME+ "\n"+
                "\r\n";
        try {
            toClientBw.write(response);
            toClientBw.flush();
        } catch (IOException e) {
            PeroxyInterface.SIGNALMESSAGE +=  e.toString()+"<br>";
        }
    }
    
    private void handleHttpsRequest(String urlString) {
        try {
            URL url = new URL(urlString);
            int port;
            if(url.getPort()== -1){
                port = url.getDefaultPort();
            } else {
                port = url.getPort();
            }
            
            //Getting inet address object of url
            InetAddress address = InetAddress.getByName(url.getHost());
            
            //Open a server socket with address
            Socket ptsSocket = new Socket(address, port);
            ptsSocket.setSoTimeout(5000);
            
            //Send connection established response to client
            String response = "HTTP/1.0 200 Connection established\r\n"+
                    "Proxy-Agent: "+PeroxyInterface.APPNAME+ "\r\n"+
                    "\r\n";
            toClientBw.write(response);
            toClientBw.flush();
            
            //Now, Server and Client both starts sending data to Peroxy
            //Peroxy needs to accept data from both the parties and transfer to other party
            //Done by threading - asynchronous transfer
            
            //buffer writer between proxy and server
            BufferedWriter proxyToServerBw = new BufferedWriter(new OutputStreamWriter(ptsSocket.getOutputStream()));
            
            //buffer reader between proxy and server
            BufferedReader proxyToServerBr = new BufferedReader(new InputStreamReader(ptsSocket.getInputStream()));
            
            //Creating new thread to listen over client and transmit to server
            ClientToServerHttpsTransmit clientToServerHttps = new 
               ClientToServerHttpsTransmit(clientSocket.getInputStream(), ptsSocket.getOutputStream());
            httpsClientToServer = new Thread(clientToServerHttps);
            httpsClientToServer.start();
            
            //Listen to remote server and display to client
            try {
                byte[] buffer = new byte[4096];
                int read;
                do {
                    read = ptsSocket.getInputStream().read(buffer);
                    if (read>0) {
                        clientSocket.getOutputStream().write(buffer,0,read);
                        if(ptsSocket.getInputStream().available() < 1){
                            clientSocket.getOutputStream().flush();
                        }
                    }
                } while (read >= 0);
            } catch (SocketTimeoutException e) {
                PeroxyInterface.SIGNALMESSAGE +=  e.toString()+"<br>";
                String error = "HTTP/1.0 504 Timeout Occured after 50s\n"+
                        "User-Agent: "+ PeroxyInterface.APPNAME + "\n"+
                        "\r\n";
                try {
                    toClientBw.write(error);
                    toClientBw.flush();
                } catch (IOException ioe) {
                    PeroxyInterface.SIGNALMESSAGE +=  ioe.toString()+"<br>";
                }
            } catch (IOException e) {
                PeroxyInterface.SIGNALMESSAGE += e.toString()+"<br>";
            }
            
            //closing down sources
            if(ptsSocket != null){
                ptsSocket.close();
            }
            
            if(proxyToServerBw != null) {
                proxyToServerBw.close();
            }
            
            if(proxyToServerBr != null) {
                proxyToServerBr.close();
            }
            
            if(toClientBw != null) {
                toClientBw.close();
            }
            
        } catch (SocketTimeoutException e) {
            PeroxyInterface.SIGNALMESSAGE += e.toString()+"<br>";
            String response = "HTTP/1.0 504 Timeout Occured after 50s\n"+
                    "User-Agent: "+ PeroxyInterface.APPNAME + "\n"+
                    "\r\n";
            try {
                toClientBw.write(response);
                toClientBw.flush();
            } catch (IOException ioe) {
                PeroxyInterface.SIGNALMESSAGE +=  ioe.toString()+"<br>";
            }
        } catch (MalformedURLException | UnknownHostException e) {
            PeroxyInterface.SIGNALMESSAGE += e.toString()+"<br>";
        } catch (IOException e) {
            PeroxyInterface.SIGNALMESSAGE += "Error on Https url: " + urlString+"<br>";
        }
    }
    
    /**
     * Sends cached file to the client
     * @param file - cached file (image/text)
     */
    private void sendCachedCopyToClient(File file) {
        //Reads file from Cache Manager
        try {
            //Getting extension of the file to check if it is a image file or not
            String fileExtension = file.getName().substring(file.getName().lastIndexOf("."));
            
            //Response and file sending to the clients
            String response ; 
            if((fileExtension.contains(".png")) || fileExtension.contains(".jpg") || 
                    fileExtension.contains(".jpeg") || fileExtension.contains(".gif")){
                //Reading image from storage
                BufferedImage image = ImageIO.read(file);
                
                if(image == null){
                    PeroxyInterface.SIGNALMESSAGE += "Image "+file.getName() + " was null.<br>";
                    response = "HTTP/1.0 404 NOT FOUND \n"+
                            "Proxy-agent: " + PeroxyInterface.APPNAME+"\n"+
                            "\r\n";
                    toClientBw.write(response);
                    toClientBw.flush();
                } else {
                    response = "HTTP/1.0 200 OK\n" +
                            "Proxy-agent: " + PeroxyInterface.APPNAME+"\n"+
                            "\r\n";
                    toClientBw.write(response);
                    toClientBw.flush();
                    ImageIO.write(image, fileExtension.substring(1), clientSocket.getOutputStream());
                }
            }
            
            //standard based text file
            else {
                BufferedReader cachedFileBr = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                
                response = "HTTP/1.0 200 OK\n"+
                        "Proxy-Agent: " + PeroxyInterface.APPNAME+"\n"+
                        "\r\n";
                toClientBw.write(response);
                toClientBw.flush();
                
                String line;
                while((line = cachedFileBr.readLine()) != null) {
                    toClientBw.write(line);
                }
                toClientBw.flush();
                
                // Close resources
                if(cachedFileBr!=null){
                    cachedFileBr.close();
                }
            }
            
            // Close client stream Buffer writer
            if(toClientBw != null) {
                toClientBw.close();
            }
        } catch (IOException e) {
            PeroxyInterface.SIGNALMESSAGE += e.toString()+"<br>";
        }
    }
    
    /**
     * Making GET HTTP request to server and get the file
     * then send file to client with response
     * @param urlString - client's request of web page
     */
    private void sendNonCachedCopyToClient(String urlString) {
        try {
            //compute name of file
            //and find extension of file
            int extInd = urlString.lastIndexOf(".");
            String extension;
            
            // Getting extension of file
            extension = urlString.substring(extInd,urlString.length());
            
            //Getting filename for urlString
            String fileName = urlString.substring(0, extInd);
            
            //Trimming http://WWW. - no need of it in filename
            fileName = fileName.substring(fileName.indexOf(".")+1);
            
            //Removing illegal characters from filename
            fileName = fileName.replace(".", "0");
            fileName = fileName.replace(":", "_");
            
            //If extension containing /
            if(extension.contains("/")){
                extension = extension.replace(":", "0");
                extension = extension.replace(".", "0");
                extension += ".html";
            } else {
                extension = extension.replace(":", "0");
            }
            
            fileName = fileName+extension; // Assigning full file name
            
            //Attemp to create file in Cache Manager
            boolean caching = true;
            File fileToCache = null;
            BufferedWriter fileToCacheBw = null;
            
            try {
                // Create file to cache
                fileToCache = new File(".cacheManager/cached/"+fileName);
                
                if(!fileToCache.exists()) {
                    PeroxyInterface.SIGNALMESSAGE += "Creating cache file in Cache Manager...<br>";
                    fileToCache.getParentFile().mkdirs(); //creating all the directories for file
                    fileToCache.createNewFile(); //new file creating
                    
                }
                
                fileToCacheBw = new BufferedWriter(new FileWriter(fileToCache));
            } catch (IOException e) {
                PeroxyInterface.SIGNALMESSAGE += "Couldn't cache: " + fileName+"<br>"; 
            } catch (NullPointerException e) {
                PeroxyInterface.SIGNALMESSAGE += "NPE file opening exception...<br>";
            }
            
            //If file is image file
            if(extension.contains(".png") || extension.contains(".jpg") || 
                    extension.contains(".jpeg") || extension.contains(".gif")){
                //Create URL object
                URL remoteURL = new URL(urlString);
                BufferedImage image = ImageIO.read(remoteURL);
                
                if(image != null) {
                    //Cache image to the Cache Manager
                    ImageIO.write(image, extension.substring(1), fileToCache);
                    
                    //send response to client
                    String response = "HTTP/1.0 200 OK\n"+
                            "Proxy-agent: "+PeroxyInterface.APPNAME+"\n"+
                            "\r\n";
                    toClientBw.write(response);
                    toClientBw.flush();
                    
                    //sending image to the client
                    ImageIO.write(image, extension.substring(1), clientSocket.getOutputStream());
                } else {
                    //No image received from server
                    PeroxyInterface.SIGNALMESSAGE += "<br>Sending 404 to client as image was not received from server: " + fileName;
                    
                    String response = "HTTP/1.0 404 NOT FOUND\n"+
                            "Proxy-agent: "+ PeroxyInterface.APPNAME+"\n"+
                            "\r\n";
                    
                    fileToCacheBw.write(response);
                    fileToCacheBw.flush();
                    return;
                }
            }
            //If file is text file
            else {
                //Create URL object
                URL remoteUrl = new URL(urlString); 
                //Create a connection to remote server
                HttpURLConnection proxyToServerCon = (HttpURLConnection)remoteUrl.openConnection();
                proxyToServerCon.setRequestProperty("Content-Type", 
                        "application/x-www-form-urlencoded");
                proxyToServerCon.setRequestProperty("Content-Language", "en");
                proxyToServerCon.setUseCaches(false);
                proxyToServerCon.setDoOutput(true);
                
                //create a buffered reader from server
                BufferedReader fromServerBr = new BufferedReader(new InputStreamReader(proxyToServerCon.getInputStream()));
                
                //send source code to client
                String response = "HTTP/1.0 200 OK\n"+
                        "Proxy-agent: " + PeroxyInterface.APPNAME+ "\n"+
                        "\r\n";
                toClientBw.write(response);
                
                // Read input stream data from server
                String line;
                while((line = fromServerBr.readLine()) != null) {
                    //sent data to client
                    toClientBw.write(line);
                    
                    //Wrtie the web page to the cache
                    if(caching) {
                        fileToCacheBw.write(line);
                    }
                }
                
                toClientBw.flush();
                
                if(fromServerBr != null) {
                    fromServerBr.close();
                }
            }
            
            if(caching) {
                //Writing data to the file now - caching
                fileToCacheBw.flush();
                PeroxyInterface.addCachedPage(urlString, fileToCache);
            }
            
            //Closing client output stream
            if(fileToCacheBw != null){
                fileToCacheBw.close();
            }
            
            if(toClientBw != null) {
                toClientBw.close();
            }
        } catch (Exception e) {
            PeroxyInterface.SIGNALMESSAGE +=  e.toString()+"<br>";
        }
    }
    
    /**
     * Listen to Data from clients and send it to the 
     * server over https connection asynchronously.
     */
    class ClientToServerHttpsTransmit implements Runnable {
        
        /**
         * Input Stream from proxy server to client
         */
        InputStream proxyToClientIs;
        
        /**
         * Output stream from proxy server to main server
         */
        OutputStream proxyToServerOs;
        
        /**
         * Constructor to initialize input and output stream from client and
         * server respectively
         * @param proxyToClientsIs Stream from proxy server to client
         * @param proxyToServerOs  stream from proxy server to main server
         */
        ClientToServerHttpsTransmit(InputStream proxyToClientsIs, OutputStream proxyToServerOs) {
            this.proxyToClientIs = proxyToClientsIs;
            this.proxyToServerOs = proxyToServerOs;
        }
        
        @Override
        public void run() {
            try {
                //Read byte by byte from client and send it to server
                byte[] buffer = new byte[4096];
                int read;
                do {
                    read = proxyToClientIs.read(buffer);
                    if(read > 0){
                        proxyToServerOs.write(buffer, 0, read);
                        if(proxyToClientIs.available() < 1) {
                            proxyToServerOs.flush();
                        }
                    }
                } while (read >= 0);
            } catch (SocketTimeoutException e) {
                
            } catch (IOException e) {
                PeroxyInterface.SIGNALMESSAGE += e.toString()+"<br>";
            }
        }
    }
    /**
     * @param args the command line arguments
     */
    //public static void main(String[] args) {
        // TODO code application logic here
    //}
    
}
