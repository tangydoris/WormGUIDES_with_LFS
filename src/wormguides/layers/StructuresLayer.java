/*
 * Bao Lab 2016
 */

package wormguides.layers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.util.Callback;

import wormguides.models.subscenegeometry.SceneElementsList;
import wormguides.util.AppFont;

import static java.util.Objects.requireNonNull;

import static javafx.collections.FXCollections.observableArrayList;
import static javafx.scene.paint.Color.WHITE;

import static partslist.PartsList.getLineageNamesByFunctionalName;

public class StructuresLayer {

    private final SearchLayer searchLayer;

    private final ObservableList<String> allStructuresList;
    private final ObservableList<String> searchStructuresResultsList;

    private final Map<String, List<String>> nameToCellsMap;
    private final Map<String, String> nameToCommentsMap;
    private final Map<String, StructureListCellGraphic> nameListCellMap;

    private final StringProperty selectedStructureNameProperty;

    private final TextField searchField;

    private Color selectedColor;
    private String searchText;

    public StructuresLayer(
            final SearchLayer searchLayer,
            final SceneElementsList sceneElementsList,
            final StringProperty selectedEntityNameProperty,
            final TextField searchField,
            final ListView<String> structureSearchListView,
            final ListView<String> allStructuresListView,
            final Button addStructureRuleButton,
            final ColorPicker colorPicker,
            final BooleanProperty rebuildSceneFlag) {

        selectedColor = WHITE;

        allStructuresList = observableArrayList();
        searchStructuresResultsList = observableArrayList();

        nameListCellMap = new HashMap<>();

        selectedStructureNameProperty = new SimpleStringProperty("");
        selectedStructureNameProperty.addListener((observable, oldVlaue, newValue) -> {
            if (!newValue.isEmpty()) {
                requireNonNull(selectedEntityNameProperty).set(newValue);
            }
        });

        this.searchLayer = requireNonNull(searchLayer);

        allStructuresList.addListener(new ListChangeListener<String>() {
            @Override
            public void onChanged(Change<? extends String> change) {
                while (change.next()) {
                    if (!change.wasUpdated()) {
                        for (String string : change.getAddedSubList()) {
                            StructureListCellGraphic graphic = new StructureListCellGraphic(string);
                            nameListCellMap.put(string, graphic);
                        }
                    }
                }
            }
        });

        requireNonNull(sceneElementsList);
        allStructuresList.addAll(sceneElementsList.getAllMulticellSceneNames());
        nameToCellsMap = sceneElementsList.getNameToCellsMap();
        nameToCommentsMap = sceneElementsList.getNameToCommentsMap();

        this.searchField = requireNonNull(searchField);
        this.searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            searchText = newValue.toLowerCase();
            if (searchText.isEmpty()) {
                searchStructuresResultsList.clear();
            } else {
                setSelectedStructure("");
                deselectAllStructures();
                searchAndUpdateResults(newValue.toLowerCase());
            }
        });

        requireNonNull(structureSearchListView).setItems(searchStructuresResultsList);
        requireNonNull(allStructuresListView).setItems(allStructuresList);

        requireNonNull(rebuildSceneFlag);
        requireNonNull(addStructureRuleButton).setOnAction(event -> {
            final String name = selectedStructureNameProperty.get();
            if (!name.isEmpty()) {
                addStructureRule(name, selectedColor);
                deselectAllStructures();
            }
            // if no name is selected, add all results from search
            else {
                for (String string : searchStructuresResultsList) {
                    addStructureRule(string, selectedColor);
                }
                searchField.clear();
            }
            rebuildSceneFlag.set(true);
        });
        requireNonNull(colorPicker).setOnAction(event -> selectedColor = ((ColorPicker) event.getSource()).getValue());
    }

    public void setSelectedStructure(String structure) {
        // unhighlight previous selected structure
        if (!selectedStructureNameProperty.get().isEmpty()) {
            nameListCellMap.get(selectedStructureNameProperty.get()).setSelected(false);
        }

        selectedStructureNameProperty.set(structure);

        // highlight new selected structure
        if (!selectedStructureNameProperty.get().isEmpty()) {
            nameListCellMap.get(selectedStructureNameProperty.get()).setSelected(true);
        }
    }

    public void setSelectedColor(Color color) {
        selectedColor = color;
    }

    private void deselectAllStructures() {
        for (String name : nameListCellMap.keySet()) {
            nameListCellMap.get(name).setSelected(false);
        }
    }

    public void addStructureRule(String name, Color color) {
        if (name == null || color == null) {
            return;
        }

        // Check for validity of name
        name = name.trim();
        if (allStructuresList.contains(name)) {
            searchLayer.addMulticellularStructureRule(name, color);
        }
    }

    // Only searches names for now
    public void searchAndUpdateResults(String searched) {
        if (searched == null || searched.isEmpty()) {
            return;
        }

        String[] terms = searched.toLowerCase().split(" ");
        searchStructuresResultsList.clear();

        for (String name : allStructuresList) {

            if (!searchStructuresResultsList.contains(name)) {
                // search in structure scene names
                String nameLower = name.toLowerCase();

                boolean appliesToName = true;
                boolean appliesToCell = false;
                boolean appliesToComment = true;

                for (String term : terms) {
                    if (!nameLower.contains(term)) {
                        appliesToName = false;
                        break;
                    }
                }

                // search in cells
                List<String> cells = nameToCellsMap.get(nameLower);
                if (cells != null) {
                    for (String cell : cells) {
                        // we'll use the first term
                        if (terms.length > 0) {
                            // check if search term is a functional name
                            final List<String> lineageNames = new ArrayList<>(
                                    getLineageNamesByFunctionalName(terms[0]));
                            for (String lineageName : lineageNames) {
                                if (lineageName != null) {
                                    if (cell.toLowerCase().startsWith(lineageName.toLowerCase())) {
                                        appliesToCell = true;
                                        break;
                                    }
                                } else {
                                    if (cell.toLowerCase().startsWith(terms[0].toLowerCase())) {
                                        appliesToCell = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }

                // search in comments if name does not already apply
                String comment = nameToCommentsMap.get(nameLower);
                String commentLower = comment.toLowerCase();
                for (String term : terms) {
                    if (!commentLower.contains(term)) {
                        appliesToComment = false;
                        break;
                    }
                }

                if (appliesToName || appliesToCell || appliesToComment) {
                    searchStructuresResultsList.add(name);
                }
            }
        }
    }

    public StringProperty getSelectedStructureNameProperty() {
        return selectedStructureNameProperty;
    }

    public String getSearchText() {
        return searchText;
    }

    public Callback<ListView<String>, ListCell<String>> getCellFactory() {
        return new Callback<ListView<String>, ListCell<String>>() {
            @Override
            public ListCell<String> call(ListView<String> param) {
                ListCell<String> cell = new ListCell<String>() {
                    @Override
                    protected void updateItem(String name, boolean empty) {
                        super.updateItem(name, empty);
                        if (name != null) {
                            setGraphic(nameListCellMap.get(name));
                        } else {
                            setGraphic(null);
                        }

                        setStyle("-fx-focus-color: transparent;" + "-fx-background-color: transparent;");
                        setPadding(Insets.EMPTY);
                    }
                };
                return cell;
            }
        };
    }

    // Graphical representation of a structure list cell
    private class StructureListCellGraphic extends HBox {

        private final double UI_HEIGHT = 28.0;
        private BooleanProperty isSelected;
        private Label label;

        public StructureListCellGraphic(String name) {
            super();

            label = new Label(name);
            label.setFont(AppFont.getFont());

            label.setPrefHeight(UI_HEIGHT);
            label.setMinHeight(USE_PREF_SIZE);
            label.setMaxHeight(USE_PREF_SIZE);

            getChildren().add(label);

            setMaxWidth(Double.MAX_VALUE);
            setPadding(new Insets(5, 5, 5, 5));

            setPickOnBounds(false);
            isSelected = new SimpleBooleanProperty(false);
            setOnMouseClicked(event -> {
                isSelected.set(!isSelected());
                searchField.clear();
            });
            isSelected.addListener((observable, oldValue, newValue) -> {
                if (newValue) {
                    setSelectedStructure(label.getText());
                    highlightCell(true);
                } else {
                    setSelectedStructure("");
                    highlightCell(false);
                }
            });
            highlightCell(isSelected());
        }

        public boolean isSelected() {
            return isSelected.get();
        }

        public void setSelected(boolean selected) {
            isSelected.set(selected);
        }

        private void highlightCell(boolean highlight) {
            if (highlight) {
                setStyle("-fx-background-color: -fx-focus-color, -fx-cell-focus-inner-border, -fx-selection-bar;"
                        + "-fx-background: -fx-accent;");
                label.setTextFill(WHITE);
            } else {
                setStyle("-fx-background-color: white;");
                label.setTextFill(Color.BLACK);
            }
        }
    }

}
