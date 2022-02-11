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
import java.util.Collections;
import java.util.List;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

public class ConnectionThread extends Thread {

	private Socket socket;
	private Server server;
	private String alias;
    private PrintWriter out;
    private BufferedReader in;
    boolean exists = true;
    
	public ConnectionThread(Socket socket, Server server) 
	{
		this.socket = socket;
		this.server = server;
	}
	
	@Override
	public void run()
	{
		try
		{
			out = new PrintWriter(socket.getOutputStream(), true);
	        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		} catch (IOException ex)
		{
			ex.printStackTrace();
		}
		
		while (true)
		{
			try 
			{
				JsonReader jsonReader = Json.createReader(new StringReader(in.readLine()));
	            JsonObject msgObject = jsonReader.readObject();
	            jsonReader.close();
	            
	            String action = msgObject.getString(Constants.ACTION);
                alias = msgObject.getString(Constants.SENDER_ALIAS).replaceAll("\\s+", "");
	            System.out.println(alias + " - " + action);
	            switch (action)
	            {
	            	case Constants.LOGGED_IN_ACTION:
	            	{
	                    server.getConnectionMap().put(alias, this);
	                    sendAvailableClientsList();
	                    break;
	            	}
	            	
	            	case Constants.LOGGED_OUT_ACTION:
	            	{
	                    server.getConnectionMap().remove(alias);
	                    server.reloadClients();
	                    socket.close();
	                    return;
	            	}
	            	
	            	case Constants.SEND_MSG_ACTION:
	            	{
	            		sendUserMessage(msgObject);
	            		break;
	            	}
	            	
	            	case Constants.SEND_GROUPMSG_ACTION:
	            	{
	            		sendGroupMessage(msgObject);
	            		break;
	            	}
	            	
	            	case Constants.POLL_GROUP_COUNT_ACTION:
	            	{
	            		pollGroupCount();
	            		break;
	            	}

	            }
			} catch (IOException e) 
			{
				e.printStackTrace();
				return;
			}
		}
	}
	
	private void pollGroupCount()
	{
		sendGroupCount(server.getGroupListCount().size());
	}
	
    public void sendUserMessage(JsonObject msgObject)
    {
        String receiverAlias = msgObject.getString(Constants.RECEIVER_ALIAS).replaceAll("\\s+", "");
    	String msgContent = msgObject.getString(Constants.CONTENT);
    	
    	List<String> clientComboList = new ArrayList<>();
    	clientComboList.add(receiverAlias);
    	clientComboList.add(alias);

    	server.getConnectionMap().get(receiverAlias).sendMessageFromClient(alias, msgContent);
    }
    
    public void sendGroupMessage(JsonObject msgObject)
    {
		String receiverAliasStringList = msgObject.getString(Constants.RECEIVER_ALIAS).replaceAll("[\\[\\]\\(\\)\\s+]", "");
        List<String> receiverAliasList = Arrays.asList(receiverAliasStringList.split(","));
    	String msgContent = msgObject.getString(Constants.CONTENT);
    	List<String> groupList = new ArrayList<>();
    	for(String receiver : receiverAliasList)
    	{
    		groupList.clear();
    		for (String rec : receiverAliasList)
    		{
    			if (!receiver.equals(rec)) 
    			{
    				groupList.add(rec);
    			}
    		}
			groupList.add(alias);
			
	    	Collections.sort(groupList);
	    	String groupListString = groupList.toString().replaceAll("[\\[\\]\\(\\)\\s+]", "");
	    	
	    	int groupCount = server.getGroupListCount().indexOf(groupListString);
	    	if (groupCount == -1)
	    	{
	    		server.getGroupListCount().add(groupListString);
	    		groupCount = server.getGroupListCount().indexOf(groupListString);
	    	}
        	server.getConnectionMap().get(receiver).sendMessageFromClientToGroup(groupListString, msgContent, groupCount);
    	}
    }

	public void sendAvailableClientsList()
	{
		buildJsonTemplateAndSend(Constants.POLL_CLIENTS_ACTION, Constants.SERVER, server.getConnectionMap().keySet().toString(), -1);
	}
	
	public void sendMessageFromClient(String senderAlias, String content)
	{
		buildJsonTemplateAndSend(Constants.SEND_MSG_ACTION, senderAlias, content, -1);
	}
	
	public void sendMessageFromClientToGroup(String senderAlias, String content, int groupCount)
	{
		buildJsonTemplateAndSend(Constants.SEND_GROUPMSG_ACTION, senderAlias, content, groupCount);
	}
	
	public void sendHeartBeat()
	{
		buildJsonTemplateAndSend(Constants.SEND_HEARTBEAT_ACTION, Constants.SERVER, "", -1);
	}
	
	public void sendGroupCount(int groupCount)
	{
		buildJsonTemplateAndSend(Constants.POLL_GROUP_COUNT_ACTION, Constants.SERVER, "", groupCount);
	}
	
	private void buildJsonTemplateAndSend(String action, String senderAlias, String content, int groupCount)
	{
		StringWriter stringWriter = new StringWriter();
    	Json.createWriter(stringWriter).writeObject(Json.createObjectBuilder()
    			.add(Constants.ACTION, action)
    			.add(Constants.SENDER_ALIAS, senderAlias)
    			.add(Constants.CONTENT, content)
    			.add(Constants.GROUP_COUNT, groupCount)
    			.build());
    	sendMessage(stringWriter.toString());
	}
	
	private void sendMessage(String msg)
	{
		out.println(msg);
		out.flush();
	}
	
	public boolean isDisconnected()
	{
		return out.checkError();
	}
	
	public String getAlias()
	{
		return alias;
	}
}
