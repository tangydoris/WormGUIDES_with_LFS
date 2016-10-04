/*
 * Bao Lab 2016
 */

package wormguides.layers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
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

import wormguides.models.SceneElementsList;
import wormguides.util.AppFont;

import static java.util.Objects.requireNonNull;

import static javafx.collections.FXCollections.observableArrayList;
import static javafx.scene.paint.Color.WHITE;

import static partslist.PartsList.getLineageNameByFunctionalName;

public class StructuresLayer {

    private final SearchLayer searchLayer;

    private ObservableList<String> allStructuresList;
    private ObservableList<String> searchResultsList;

    private Color selectedColor;
    private String searchText;

    private Map<String, List<String>> nameToCellsMap;
    private Map<String, String> nameToCommentsMap;
    private Map<String, StructureListCellGraphic> nameListCellMap;

    private StringProperty selectedNameProperty;

    private TextField searchField;

    public StructuresLayer(
            final SearchLayer searchLayer,
            final SceneElementsList sceneElementsList,
            final TextField searchField,
            final ListView<String> structureSearchListView,
            final ListView<String> allStructuresListView,
            final Button addStructureRuleButton,
            final ColorPicker colorPicker) {

        this.searchLayer = requireNonNull(searchLayer);

        selectedColor = WHITE;

        allStructuresList = observableArrayList();
        searchResultsList = observableArrayList();

        nameListCellMap = new HashMap<>();
        selectedNameProperty = new SimpleStringProperty("");

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

        setSearchField(requireNonNull(searchField));

        requireNonNull(structureSearchListView).setItems(searchResultsList);
        requireNonNull(allStructuresListView).setItems(allStructuresList);

        requireNonNull(addStructureRuleButton).setOnAction(event -> {
            final String name = selectedNameProperty.get();
            if (!name.isEmpty()) {
                addStructureRule(name, selectedColor);
                deselectAllStructures();
            }
            // if no name is selected, add all results from search
            else {
                for (String string : searchResultsList) {
                    addStructureRule(string, selectedColor);
                }
                searchField.clear();
            }
        });

        requireNonNull(colorPicker).setOnAction(event -> selectedColor = ((ColorPicker) event.getSource()).getValue());
    }

    public void setSelectedStructure(String structure) {
        // unhighlight previous selected structure
        if (!selectedNameProperty.get().isEmpty()) {
            nameListCellMap.get(selectedNameProperty.get()).setSelected(false);
        }

        selectedNameProperty.set(structure);

        // highlight new selected structure
        if (!selectedNameProperty.get().isEmpty()) {
            nameListCellMap.get(selectedNameProperty.get()).setSelected(true);
        }
    }

    public void setSelectedColor(Color color) {
        selectedColor = color;
    }

    private void setSearchField(final TextField searchField) {
        this.searchField = searchField;

        this.searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            searchText = newValue.toLowerCase();
            if (searchText.isEmpty()) {
                searchResultsList.clear();
            } else {
                setSelectedStructure("");
                deselectAllStructures();
                searchAndUpdateResults(newValue.toLowerCase());
            }
        });
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
        searchResultsList.clear();

        for (String name : allStructuresList) {

            if (!searchResultsList.contains(name)) {
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
                            final String lineageName = getLineageNameByFunctionalName(terms[0]);
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
                    searchResultsList.add(name);
                }
            }
        }
    }

    public StringProperty getSelectedNameProperty() {
        return selectedNameProperty;
    }

    public String getSearchText() {
        return searchText;
    }

    public void addSelectedNameListener(ChangeListener<String> listener) {
        if (listener != null) {
            selectedNameProperty.addListener(listener);
        }
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
