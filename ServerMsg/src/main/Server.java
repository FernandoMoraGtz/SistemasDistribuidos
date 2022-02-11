package main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

public class Server {
	
	private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private Map<String, ConnectionThread> connectionMap = new ConcurrentHashMap<>();
    private List<String> groupListCount = new ArrayList<>();
    
    public Server(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        startHeartbeatThread();
        while (true)
        {
        	clientSocket = serverSocket.accept();
        	new ConnectionThread(clientSocket, this).start();
        }
        
    }
    
    public void reloadClients()
    {
    	for (ConnectionThread connectionThread : connectionMap.values())
    	{
    		connectionThread.sendAvailableClientsList();
    	}
    }
    
    private void startHeartbeatThread()
    {
    	new Thread(() -> {
    		while(true)
    		{
    			if(connectionMap.size() > 0)
        		{
        	    	for (ConnectionThread connectionThread : connectionMap.values())
        	    	{
        	    		connectionThread.sendHeartBeat();
        	    		if (connectionThread.isDisconnected())
        	    		{
        	    			String currentAlias = connectionThread.getAlias();
        	    			connectionMap.remove(currentAlias);
        	    		}
        	    	}
        	    	
        	    	for (ConnectionThread connectionThread : connectionMap.values())
        	    	{
        	    		connectionThread.sendAvailableClientsList();
        	    	}
        	    }
        		try {
    				Thread.sleep(1000);
    			} catch (InterruptedException e) {
    				e.printStackTrace();
    			}
    		}
    	}).start();
    }

    
    public void stop() throws IOException
    {
        in.close();
        out.close();
        clientSocket.close();
        serverSocket.close();
    }
    
    public static void main(String[] args) throws IOException
    {
    	Server server = new Server(8081);
    }
    
    public Map<String, ConnectionThread> getConnectionMap() {
		return connectionMap;
	}

    public List<String> getGroupListCount() {
		return groupListCount;
	}
    
}