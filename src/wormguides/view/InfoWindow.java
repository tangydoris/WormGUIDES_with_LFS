package wormguides.view;


import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class InfoWindow {
	private Stage infoWindowStage;
	private TabPane tabPane;
	Scene scene;
	
	/*
	 * TODO
	 * if tab is closed --> remove case from cell cases i.e. internal memory
	 */
	
	public InfoWindow() {
		infoWindowStage = new Stage();
		infoWindowStage.setTitle("Info Window");
		
		tabPane = new TabPane();
		
		scene = new Scene(new Group());
		scene.setRoot(tabPane);
		
		infoWindowStage.setScene(scene);
		infoWindowStage.setResizable(false);
	}
	
	public void showWindow() {
		if (infoWindowStage != null) {
			infoWindowStage.show();
		}
	}
	
	public void addTab(InfoWindowDOM dom) {
		WebView webview = new WebView();
		webview.getEngine().loadContent(dom.DOMtoString());
		Tab tab = new Tab(dom.getName(), webview);
		tabPane.getTabs().add(tab);
		tabPane.setFocusTraversable(true);
	}
}