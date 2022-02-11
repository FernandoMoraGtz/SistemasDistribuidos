package main;

import javafx.application.Application; 
import static javafx.application.Application.launch;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.geometry.Insets; 
import javafx.geometry.Pos; 

import javafx.scene.Scene; 
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text; 
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;  
         
public class MessengerApp extends Application {
	
	private final String IP = "127.0.0.1";
	private final int PORT = 8081;
	private String alias;
	private ClientConnection clientConnection;
	private Stage window;
	public Map<String, String> textHistoryMap = new HashMap<>();
	public Map<String, String> groupTextHistoryMap = new HashMap<>();
	TextArea messages = new TextArea();
	TextArea groupMessages = new TextArea();


	@Override 
	public void start(Stage stage)
	{
		window = stage;
		messages.setStyle("-fx-font-family: monospace");
		messages.setPrefHeight(550);
		
		groupMessages.setStyle("-fx-font-family: monospace");
		groupMessages.setPrefHeight(550);

		loadLogInWindow();		
	   	
	   	window.show(); 
	}
   
	private void loadLogInWindow()
	{
		Text aliasLabel = new Text("Alias");
	      
		TextField aliasTextField = new TextField();
		
		Button loginBtn = new Button("Login"); 
		loginBtn.setMaxSize(200, 200);
		loginBtn.setOnAction(e -> initClientConnection(aliasTextField.getText()));
		
		aliasTextField.setPromptText("Escribe tu nombre");
		aliasTextField.setOnKeyReleased(e -> {if (e.getCode() == KeyCode.ENTER) loginBtn.fire();});
		
		GridPane gridPane = new GridPane();    
		gridPane.setMinSize(400, 400); 
		gridPane.setPadding(new Insets(10, 10, 10, 10)); 
		gridPane.setVgap(5); 
		gridPane.setHgap(5);       
		gridPane.setAlignment(Pos.CENTER);  
		gridPane.add(aliasLabel, 0, 0); 
		gridPane.add(aliasTextField, 1, 0); 
		gridPane.add(loginBtn, 1, 2); 
       
		loginBtn.setStyle("-fx-background-color: darkslateblue; -fx-text-fill: white;"); 
		aliasLabel.setStyle("-fx-font: normal bold 20px 'serif' "); 
		gridPane.setStyle("-fx-background-color: BEIGE;");
		
		window.setTitle("Login");		
		window.setScene(new Scene(gridPane)); 
	}
	
	public void loadOnlineClientsWindow()
	{
		clientConnection.setInWindowChat(false);
		List<String> availableClientsList = clientConnection.getAvailableClientsList();
		List<String> currentClients = new ArrayList<>();
		for (String currentClient : availableClientsList)
		{
			if (!currentClient.equals(alias))
			{
				currentClients.add(currentClient);
			}
		}
		Image img;
	    ImageView view;
		int i = 1;
		ColumnConstraints columnConstraints = new ColumnConstraints();
		columnConstraints.setHgrow(Priority.NEVER);
		columnConstraints.setPercentWidth(100.00);

		RowConstraints rowConstraints = new RowConstraints();
		rowConstraints.setVgrow(Priority.NEVER);
		rowConstraints.setPercentHeight(100.0);

		GridPane gridPane = new GridPane();  
		gridPane.getRowConstraints().add(rowConstraints);
		gridPane.getColumnConstraints().add(columnConstraints);
		gridPane.setPadding(new Insets(5, 5, 5, 5)); 
		gridPane.setVgap(5); 
		gridPane.setHgap(5);       
		gridPane.setAlignment(Pos.TOP_LEFT);
		URL url = getClass().getResource("/msgicon.png");
		URL urlGrpChat = getClass().getResource("/grpchat.jpg");

		Button groupChatBtn = new Button();
		groupChatBtn.setOnAction(e -> {GroupBox.display(currentClients, this);});
		img = new Image(urlGrpChat.toString());
	    view = new ImageView(img);
	    view.setFitHeight(50);
	    view.setFitWidth(50);
	    groupChatBtn.setGraphic(view);
		HBox hbox1 = new HBox(groupChatBtn);
		hbox1.setAlignment(Pos.CENTER);
		hbox1.setPrefHeight(10);

	    if (availableClientsList.size() > 2)
	    gridPane.add(hbox1, 0, 0);
		for (String currentClient : currentClients)
		{
			Button chatBtn = new Button();
			chatBtn.setOnAction(e -> loadChatBox(currentClient));

			img = new Image(url.toString());
		    view = new ImageView(img);
		    view.setFitHeight(50);
		    view.setFitWidth(50);
		    chatBtn.setGraphic(view);
	        Label label = new Label(currentClient);

			Region leftRegion = new Region();
	        HBox.setHgrow(leftRegion, Priority.ALWAYS);
			HBox hbox = new HBox(label, leftRegion, chatBtn);

			hbox.setSpacing(50);
			
			hbox.setPrefWidth(300);
			hbox.setAlignment(Pos.CENTER_LEFT);
			hbox.setStyle("-fx-padding: 10;" + "-fx-border-style: solid inside;"
			        + "-fx-border-width: 2;" + "-fx-border-insets: 5;"
			        + "-fx-border-radius: 5;" + "-fx-border-color: black;");

			gridPane.add(hbox, 0, i++); 
			
		}
		ScrollPane sp = new ScrollPane(gridPane);
		sp.setMinSize(300, 1000);

		window.setOnCloseRequest(e -> {
			e.consume();
			disconnectFromServer();
		});
		window.setTitle("Messenger - " + alias);		
		window.setScene(new Scene(sp)); 
	}
	
	public void loadChatBox(String aliasReceiver)
	{
		clientConnection.clientInChatWith(aliasReceiver);
		messages.clear();
		String textHistory = textHistoryMap.get(aliasReceiver);
		if(textHistory != null)
		{
			messages.appendText(textHistory);
		}
		Button backBtn = getBackButton();
	    
		TextField input = new TextField();
		input.setOnAction(e -> {
			StringBuffer msgBuffer = new StringBuffer();
			msgBuffer.append(alias);
			msgBuffer.append(": ");
			msgBuffer.append(input.getText());
			msgBuffer.append("\n");
			input.clear();
			
			String message = msgBuffer.toString();
			messages.appendText(message);
			
			textHistoryMap.put(aliasReceiver, messages.getText());
			clientConnection.sendSingleMessage(aliasReceiver, message);
		});

		VBox vBox = new VBox(20, backBtn, messages, input);
		vBox.setPrefSize(600, 600);

		window.setTitle(String.format("Messenger (%s) / Chat - %s", alias, aliasReceiver));		
		window.setScene(new Scene(vBox));
	}
	
	public void loadGroupChatBox(List<String> aliasReceiverList)
	{
		String aliasReceiverStringList = aliasReceiverList.toString().replaceAll("[\\[\\]\\(\\)\\s+]", "");
		clientConnection.clientInGroupChatWith(aliasReceiverList);
		groupMessages.clear();
		String historyGroupText = groupTextHistoryMap.get(aliasReceiverStringList);
		if(historyGroupText != null)
		{
			groupMessages.appendText(historyGroupText);
		}
		Button backBtn = getBackButton();

		TextField input = new TextField();
		input.setOnAction(e -> {
			StringBuffer msgBuffer = new StringBuffer();
			msgBuffer.append(alias);
			msgBuffer.append(": ");
			msgBuffer.append(input.getText());
			msgBuffer.append("\n");
			input.clear();
			
			String message = msgBuffer.toString();
			groupMessages.appendText(message);
			
			groupTextHistoryMap.put(aliasReceiverStringList, groupMessages.getText());
			clientConnection.sendGroupMessage(aliasReceiverList, message);
		});

		VBox vBox = new VBox(20, backBtn, groupMessages, input);
		vBox.setPrefSize(600, 600);

		window.setTitle(String.format("Messenger (%s) / Chat - %s", alias, aliasReceiverList));		
		window.setScene(new Scene(vBox));
	}
	
	private void initClientConnection(String alias)
	{
		try 
		{
			if (!alias.isEmpty())
			{
				this.alias = alias;
				clientConnection = new ClientConnection(this.alias, IP, PORT, this);
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		} catch (IOException ex) 
		{
			ex.printStackTrace();
		}
	}
	
	private Button getBackButton()
	{
		Button backBtn = new Button();
		backBtn.setOnAction(e -> loadOnlineClientsWindow());
		URL url = getClass().getResource("/back.png");
		Image img = new Image(url.toString());
	    ImageView view = new ImageView(img);
	    view.setFitHeight(50);
	    view.setFitWidth(50);
	    backBtn.setGraphic(view);
	    
	    return backBtn;
	}
	private void disconnectFromServer()
	{
		if (clientConnection != null && !alias.isEmpty()) 
		{
			clientConnection.stopConnection();
			window.close();
		}
	}
	
	public static void main(String args[])
	{
		launch(args); 
	}

	public ClientConnection getClientConnection() {
		return clientConnection;
	}
	
}