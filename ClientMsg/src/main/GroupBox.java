package main;

import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class GroupBox {

	private MessengerApp messengerApp;
	
	public static void display(List<String> availableClientsList, MessengerApp messengerApp)
	{
		ObservableList<String> clients = FXCollections.observableArrayList(availableClientsList); 
		ListView<String> clientListView = new ListView<String>(clients);
		clientListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		Stage window = new Stage();
		
		window.initModality(Modality.APPLICATION_MODAL);
		window.setTitle("Select Members of Group Chat");
		window.setMinWidth(250);
		
		Button closeBtn = new Button("Close");
		closeBtn.setOnAction(e -> window.close());
		
		Button acceptBtn = new Button("Initiate");
		acceptBtn.setOnAction(e -> {
			initiateGroupChat(clientListView.getSelectionModel().getSelectedItems(), messengerApp);
			window.close();
		});
		
		VBox layout = new VBox(10);
		layout.getChildren().addAll(clientListView, acceptBtn, closeBtn);
		layout.setAlignment(Pos.CENTER);
		
		Scene scene = new Scene(layout);
		window.setScene(scene);
		window.showAndWait();
	}
	
	private static void initiateGroupChat(ObservableList<String> clients, MessengerApp messengerApp)
	{
		//messengerApp.getClientConnection().sendActionAndAlias(Constants.POLL_GROUP_COUNT_ACTION);
		messengerApp.loadGroupChatBox(clients);
	}
}
