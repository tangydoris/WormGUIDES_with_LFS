package wormguides;

import java.util.ArrayList;

import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ListView;
import javafx.scene.paint.Color;
import wormguides.model.SceneElement;
import wormguides.model.SceneElementsList;

public class StructuresLayer {
	private SceneElementsList sceneElementsList;
	@FXML private ListView<String> allStructuresListView;
	@FXML private Button addStructureRuleBtn;
	private ObservableList<String> allStructuresList;
	private static Color selectedColor;
	private String selectedStructure;
	
	public StructuresLayer(SceneElementsList sceneElementsList, 
			ListView<String> allStructuresListView, Search search,
			Button addStructureRuleBtn) {
		this.sceneElementsList = sceneElementsList;
		this.allStructuresListView = allStructuresListView;
		this.allStructuresList = FXCollections.observableArrayList();
		this.addStructureRuleBtn = addStructureRuleBtn;
		selectedColor = Color.WHITE; //default color
		
		//add listeners
	}

	public void setStructuresLayer() {
		setStructuresList();
		allStructuresListView.setItems(this.allStructuresList);
		//how to set font of listview to 14 -- appfont.get
		
		//add listener for list view
		allStructuresListView.getSelectionModel().selectedItemProperty().addListener(
				new ChangeListener<String>() {
					@Override
					public void changed(
							ObservableValue<? extends String> observable,
							String oldValue, String newValue) {
						setSelectedInfo(newValue);
					}
		});
	}
	
	private void setSelectedInfo(String newValue) {
		selectedStructure = newValue;
	}
	
	public void setStructuresList() {	
		for (int i = 0; i < sceneElementsList.sceneElementsList.size(); i++) {
			SceneElement currSE = sceneElementsList.sceneElementsList.get(i);
			
			ArrayList<String> allCellNames = currSE.getAllCellNames();
			
			//check if the scene element is a multicellular structure
			if (allCellNames.size() > 1) {
				//add the scene name to the structures list
				allStructuresList.add(currSE.getSceneName());
			}
		}
	}

	public EventHandler<ActionEvent> getAddStructureRuleButtonListener() {
		return new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				//check search name

				Search.addShapeRule(selectedStructure, selectedColor);
			}
		};
	}
	
	public EventHandler<ActionEvent> getStructureRuleColorPickerListener() {
		return new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				selectedColor = ((ColorPicker)event.getSource()).getValue();
			}
		};
	}
}
