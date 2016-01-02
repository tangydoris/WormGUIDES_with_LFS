package wormguides;

import java.io.IOException;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
//import wormguides.view.RootLayout;

public class MainApp extends Application {
	
	private Scene scene;
	private Stage primaryStage;
	private BorderPane rootLayout;
	
	public MainApp() { }
	
	@Override
	public void start(Stage primaryStage) {
		System.out.println("start");
		
		ImageLoader.loadImages(JAR_NAME);
		
		this.primaryStage = primaryStage;
		this.primaryStage.setTitle("WormGUIDES");
		
		long start_time = System.nanoTime();
		initRootLayout();
		long end_time = System.nanoTime();
		double difference = (end_time - start_time)/1e6;
		System.out.println("root layout init "+difference+"ms");
		
		primaryStage.setResizable(true);
		primaryStage.show();
		
		primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			@Override
			public void handle(WindowEvent event) {
				System.out.println("exiting...");
				System.exit(0);
			}
		});
		
	}
	
	public void initRootLayout() {
		try {
            // Load root layout from FXML file.
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("view/RootLayout.fxml"));
            this.rootLayout = (BorderPane) loader.load();
            
            this.scene = new Scene(rootLayout);
            primaryStage.setScene(scene);
            primaryStage.setResizable(true);
            
            Parent root = scene.getRoot();
            for (Node node : root.getChildrenUnmodifiable()) {
            	node.setStyle("-fx-focus-color: -fx-outer-border; "+
            					"-fx-faint-focus-color: transparent;");
            }
        } catch (IOException e) {
        	System.out.println("could not initialize root layout.");
            e.printStackTrace();
        }
	}
	
	public static void main(String[] args) {
		launch(args);
	}
	
	private static final String JAR_NAME = "WormGUIDES.jar";
	
}
