package main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import javafx.application.Platform;

public class ClientConnection {
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean isClientActive = false;
    private boolean inWindowChat = false;
    private final String alias;
    private List<String> availableClientsList = new ArrayList<>();
    private MessengerApp messengerApp;
    public String currentChatAlias;
    public List<String> currentGroupChatAlias = new ArrayList<>();
    
    public ClientConnection(String alias, String ip, int port, MessengerApp messengerApp) throws IOException
    {
    	this.clientSocket = new Socket(ip, port);
        this.out = new PrintWriter(clientSocket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        this.isClientActive = true;
        this.alias = alias;
        this.messengerApp = messengerApp;
        logInUser();
        startListener();
    }

    private void startListener()
    {
    	new Thread(() -> {
    		try
    		{
    			String serverMsg = "";
    			while (true)
    			{
    				serverMsg = in.readLine();
    				if (serverMsg == null)
    				{
    					clientSocket.close();
    					return;
    				}
    				JsonReader jsonReader = Json.createReader(new StringReader(serverMsg));
    	            JsonObject msgObject = jsonReader.readObject();
    	            jsonReader.close();
    	            String action = msgObject.getString(Constants.ACTION);
    	            
    	            switch (action)
    	            { 	
    	            	case Constants.SEND_MSG_ACTION:
    	            	{
    	            		receiveMessage(msgObject);
    	            		break;
    	            	}
    	            	
    	            	case Constants.SEND_GROUPMSG_ACTION:
    	            	{
    	            		receiveGroupMessage(msgObject);
    	            		break;
    	            	}
    	            	
    	            	case Constants.POLL_CLIENTS_ACTION:
    	            	{
    	            		populateAvailableClientsList(msgObject);
    	            		break;
    	            	}
    	            	case Constants.POLL_GROUP_COUNT_ACTION:
    	            	{
    	            		increaseGroupCount(msgObject);
    	            		break;
    	            	}
    	            }
				}
            } catch (IOException e) 
    		{
            	e.printStackTrace();
            }
    		}).start();
    }
    
	private void logInUser()
	{
		sendActionAndAlias(Constants.LOGGED_IN_ACTION);
		
	}
    
    private void logOutUser()
    {
    	sendActionAndAlias(Constants.LOGGED_OUT_ACTION);
    }
   
    protected void sendActionAndAlias(String logAction)
    {
	    StringWriter stringWriter = new StringWriter();
	    Json.createWriter(stringWriter).writeObject(Json.createObjectBuilder()
   		 	.add(Constants.ACTION, logAction)
   		 	.add(Constants.SENDER_ALIAS, alias)
   		 	.build());

	    send(stringWriter.toString());
    }
    
    public void sendSingleMessage(String receiver, String content)
    {
    	sendMessage(Constants.SEND_MSG_ACTION, receiver, content);
    }
    
    public void sendGroupMessage(List<String> receiverList, String content)
    {
    	sendMessage(Constants.SEND_GROUPMSG_ACTION, receiverList.toString(), content);

    }
    
    public void sendMessage(String action, String receiver, String content)
    {
    	StringWriter stringWriter = new StringWriter();
	    Json.createWriter(stringWriter).writeObject(Json.createObjectBuilder()
   		 	.add(Constants.ACTION, action)
   		 	.add(Constants.SENDER_ALIAS, alias)
   		 	.add(Constants.RECEIVER_ALIAS, receiver)
   		 	.add(Constants.CONTENT, content)
   		 	.build());
	    send(stringWriter.toString());
    }
    
    private void send(String msg)
    {
        out.println(msg);
        out.flush();
    }
    
    private void receiveMessage(JsonObject msgObject)
    {
    	String senderAlias = msgObject.getString(Constants.SENDER_ALIAS);
    	String msgContent = msgObject.getString(Constants.CONTENT);
    	
    	if (currentChatAlias != null || !senderAlias.equals(currentChatAlias))
    	{
    		messengerApp.messages.clear();
        	messengerApp.messages.appendText(messengerApp.textHistoryMap.getOrDefault(senderAlias, ""));
    	}
    	
    	messengerApp.messages.appendText(msgContent);
    	messengerApp.textHistoryMap.put(senderAlias, messengerApp.messages.getText());
		Platform.runLater(() -> messengerApp.loadChatBox(senderAlias));
    }
    
    private void receiveGroupMessage(JsonObject msgObject)
    {
    	String receiverAliasStringList = msgObject.getString(Constants.SENDER_ALIAS);
        List<String> receiverAliasList = Arrays.asList(receiverAliasStringList.split(","));
        String msgContent = msgObject.getString(Constants.CONTENT);
        //String groupCount = msgObject.getString(Constants.GROUP_COUNT);

        
    	if (!currentGroupChatAlias.isEmpty() || !receiverAliasList.containsAll(currentGroupChatAlias))
    	{
    		messengerApp.groupMessages.clear();
        	messengerApp.groupMessages.appendText(messengerApp.groupTextHistoryMap.getOrDefault(receiverAliasStringList, ""));
    	}
    	
    	messengerApp.groupMessages.appendText(msgContent);
    	messengerApp.groupTextHistoryMap.put(receiverAliasStringList, messengerApp.groupMessages.getText());
		Platform.runLater(() -> messengerApp.loadGroupChatBox(receiverAliasList));
    }
    
    private void populateAvailableClientsList(JsonObject msgObject)
    {
    	if (!inWindowChat)
    	{
    		String clientsAvailable = msgObject.getString(Constants.CONTENT).replaceAll("[\\[\\]\\(\\)\\s+]", "");
            availableClientsList = Arrays.asList(clientsAvailable.split(","));
    		Platform.runLater(messengerApp::loadOnlineClientsWindow);
    	}
    }
    
    private void increaseGroupCount(JsonObject msgObject)
    {
        int groupCount = msgObject.getInt(Constants.GROUP_COUNT);

    }

    public void stopConnection() {
    	logOutUser();
    	try {
			in.close();
			out.close();
	        clientSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    public void clientInChatWith(String aliasClientChat)
    {
    	this.currentChatAlias = aliasClientChat;
		this.inWindowChat = true;
    }
    
    public void clientInGroupChatWith(List<String> aliasClientChat)
    {
    	this.currentGroupChatAlias.addAll(aliasClientChat);
		this.inWindowChat = true;
    }
    
    public List<String> getAvailableClientsList()
    {
    	return availableClientsList;
    }

	public boolean isClientActive() {
		return isClientActive;
	}

	public void setClientActive(boolean isClientActive) {
		this.isClientActive = isClientActive;
	}

	public boolean isInWindowChat() {
		return inWindowChat;
	}

	public void setInWindowChat(boolean inWindowChat) {
		this.inWindowChat = inWindowChat;
	}

	public String getCurrentChatAlias() {
		return currentChatAlias;
	}

	public void setCurrentChatAlias(String currentChatAlias) {
		this.currentChatAlias = currentChatAlias;
	}
}