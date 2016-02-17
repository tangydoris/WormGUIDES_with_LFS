package wormguides.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

import com.sun.javafx.scene.CameraHelper;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.CacheHint;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Material;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Shape3D;
import javafx.scene.shape.Sphere;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import wormguides.ColorComparator;
import wormguides.MainApp;
import wormguides.Search;
import wormguides.SearchOption;
import wormguides.SearchType;
import wormguides.StoriesLayer;
import wormguides.Xform;
import wormguides.model.CellCases;
import wormguides.model.ColorHash;
import wormguides.model.ColorRule;
import wormguides.model.Connectome;
import wormguides.model.LineageData;
import wormguides.model.MulticellularStructureRule;
import wormguides.model.Note;
import wormguides.model.Note.Display;
import wormguides.model.PartsList;
import wormguides.model.ProductionInfo;
import wormguides.view.AppFont;
import wormguides.model.Rule;
import wormguides.model.SceneElement;
import wormguides.model.SceneElementsList;

public class Window3DController {
    
    private Stage parentStage;

    private LineageData cellData;

    private SubScene subscene;
    
    private TextField searchField;
    
    // transformation stuff
    private Group root;
    private PerspectiveCamera camera;
    private Xform xform;
    private double mousePosX, mousePosY;
    private double mouseOldX, mouseOldY;
    private double mouseDeltaX, mouseDeltaY;
    private int newOriginX, newOriginY, newOriginZ;

    // housekeeping stuff
    private IntegerProperty time;
    private IntegerProperty totalNuclei;
    private int endTime;
    private int startTime;
    private static Sphere[] spheres;
    private static MeshView[] meshes;
    private static String[] cellNames;
    private static String[] meshNames;
    private boolean[] searchedCells;
    private boolean[] searchedMeshes;
    private Integer[][] positions;
    private Integer[] diameters;
    private DoubleProperty zoom;

    // switching timepoints stuff
    private BooleanProperty playingMovie;
    private PlayService playService;
    private RenderService renderService;

    // subscene click cell selection stuff
    private IntegerProperty selectedIndex;
    private StringProperty selectedName;
    private StringProperty selectedNameLabeled;
    private Stage contextMenuStage;
    private ContextMenuController contextMenuController;
    private CellCases cellCases;
    private BooleanProperty cellClicked;

    // searched highlighting stuff
    private boolean inSearch;
    private ObservableList<String> searchResultsList;
    private ArrayList<String> localSearchResults;

    // color rules stuff
    private ColorHash colorHash;
    private ObservableList<Rule> currentRulesList;
    private Comparator<Color> colorComparator;
    private Comparator<Shape3D> opacityComparator;

    // specific boolean listener for gene search results
    private BooleanProperty geneResultsUpdated;

    // opacity value for "other" cells (with no rule attached)
    private DoubleProperty othersOpacity;
    private ArrayList<String> otherCells;

    // rotation stuff
    private final Rotate rotateX;
    private final Rotate rotateY;
    private final Rotate rotateZ;

    // Scene Elements stuff
    private SceneElementsList sceneElementsList;
    private ArrayList<SceneElement> sceneElementsAtTime;
    private ArrayList<MeshView> currentSceneElementMeshes;
    private ArrayList<SceneElement> currentSceneElements;
    
    // Uniform nuclei sizef
    private boolean uniformSize;
    
    // Cell body and cell nucleus highlighting in search mode
    private boolean cellNucleusTicked;
    private boolean cellBodyTicked;
    // Story elements stuff
    private StoriesLayer storiesLayer;
    // currentNotes contains all notes that are 'active' within a scene
    // (any note that should be visible in a given frame)
    private ArrayList<Note> currentNotes;
    // Map of current note graphics to their note objects
    private HashMap<Node, Note> currentGraphicNoteMap;
    // Map of current notes to their scene elements
    private HashMap<Note, MeshView> currentNoteMeshMap;
    
    private VBox overlayVBox;
    private Pane spritesPane;
    
    // maps of sprite/billboard front notes attached to cell, or cell and time
    private HashMap<Node, VBox> entitySpriteMap;
    private HashMap<Node, Node> billboardFrontEntityMap;
    
    // Label stuff
    private ArrayList<String> allLabels;
    private ArrayList<String> currentLabels;
    private HashMap<Node, Text> entityLabelMap;
    private Text transientLabelText; // shows up on hover
    
    private EventHandler<MouseEvent> clickableMouseEnteredHandler;
    private EventHandler<MouseEvent> clickableMouseExitedHandler;
    
    private ProductionInfo productionInfo;
    private Connectome connectome;
    
    private BooleanProperty bringUpInfoProperty;
    
    private SubsceneSizeListener subsceneSizeListener;
    
    public Window3DController(Stage parent, AnchorPane parentPane, LineageData data, 
            CellCases cases, ProductionInfo info, Connectome connectome) {
        parentStage = parent;
        
        root = new Group();
        cellData = data;       
        productionInfo = info;
        this.connectome = connectome;
        
        startTime = productionInfo.getDefaultStartTime();

        time = new SimpleIntegerProperty(startTime);
        time.addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable,
                                Number oldValue, Number newValue) {
                int t = newValue.intValue();
                if (t<startTime)
                    t = startTime;
                if (startTime<=t && t<=endTime)
                    buildScene();
            }
        });

        spheres = new Sphere[1];
        meshes = new MeshView[1];
        cellNames = new String[1];
        meshNames = new String[1];
        positions = new Integer[1][3];
        diameters = new Integer[1];
        searchedCells = new boolean[1];
        searchedMeshes = new boolean[1];

        selectedIndex = new SimpleIntegerProperty(-1);

        selectedName = new SimpleStringProperty("");
        selectedName.addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable,
                    String oldValue, String newValue) {
                int selected = getIndexByCellName(newValue);
                if (selected != -1)
                    selectedIndex.set(selected);
            }
        });
        selectedNameLabeled = new SimpleStringProperty("");
        selectedNameLabeled.addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, 
					String oldValue, String newValue) {
				if (!newValue.isEmpty()) {
					selectedName.set(newValue);
					if (!allLabels.contains(newValue)) {
						allLabels.add(newValue);
						currentLabels.add(newValue);
					}
					insertLabelFor(newValue, getEntityWithName(newValue));
				}
			}
        });
        
        cellClicked = new SimpleBooleanProperty(false);

        inSearch = false;

        totalNuclei = new SimpleIntegerProperty();
        totalNuclei.set(0);

        endTime = data.getTotalTimePoints();

        createSubScene(parentPane.widthProperty().get(), parentPane.heightProperty().get());
        parentPane.getChildren().add(subscene);
        
        subsceneSizeListener = new SubsceneSizeListener();
        parentPane.widthProperty().addListener(subsceneSizeListener);
        parentPane.heightProperty().addListener(subsceneSizeListener);

        mousePosX = 0;
        mousePosY = 0;
        mouseOldX = 0;
        mouseOldY = 0;
        mouseDeltaX = 0;
        mouseDeltaY = 0;

        playService = new PlayService();
        playingMovie = new SimpleBooleanProperty();
        playingMovie.set(false);
        playingMovie.addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable,
                    Boolean oldValue, Boolean newValue) {
                hideContextPopups();
                
                if (newValue)
                    playService.restart();
                else
                    playService.cancel();
            }
        });

        renderService = new RenderService();
        
        zoom = new SimpleDoubleProperty(2.5);
        zoom.addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable,
                    Number oldValue, Number newValue) {
                xform.setScale(zoom.get());
                repositionSprites();
                repositionNoteBillboardFronts();
            }
        });
        xform.setScale(zoom.get());

        localSearchResults = new ArrayList<String>();

        geneResultsUpdated = new SimpleBooleanProperty();

        othersOpacity = new SimpleDoubleProperty(1);
        otherCells = new ArrayList<String>();

        rotateX = new Rotate(0, 0, newOriginY, newOriginZ, Rotate.X_AXIS);
        rotateY = new Rotate(0, newOriginX, 0, newOriginZ, Rotate.Y_AXIS);
        rotateZ = new Rotate(0, newOriginX, newOriginY, 0, Rotate.Z_AXIS);
        
        uniformSize = false;
        
        currentRulesList = FXCollections.observableArrayList();
        
        colorHash = new ColorHash();
        colorComparator = new ColorComparator();
        opacityComparator = new OpacityComparator();
        
        currentSceneElementMeshes = new ArrayList<MeshView>();
        currentSceneElements = new ArrayList<SceneElement>();
        
        currentNotes = new ArrayList<Note>();
        currentGraphicNoteMap = new HashMap<Node, Note>();
        currentNoteMeshMap = new HashMap<Note, MeshView>();
        entitySpriteMap = new HashMap<Node, VBox>();
        billboardFrontEntityMap = new HashMap<Node, Node>();
        
        allLabels = new ArrayList<String>();
        currentLabels = new ArrayList<String>();
        entityLabelMap = new HashMap<Node, Text>();
        
        EventHandler<MouseEvent> handler = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                handleMouseEvent(event);
            }
        };
        subscene.setOnMouseClicked(handler);
        subscene.setOnMouseDragged(handler);
        subscene.setOnMouseEntered(handler);
        subscene.setOnMousePressed(handler);
        subscene.setOnMouseReleased(handler);
        
        setNotesPane(parentPane);
        
        clickableMouseEnteredHandler = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                spritesPane.setCursor(Cursor.HAND);
            }
        };
        clickableMouseExitedHandler = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                spritesPane.setCursor(Cursor.DEFAULT);
            }
        };
        
        cellCases = cases;
    }
    
    
    /*
     * Insert transient label into sprites pane
     * name = name that appears on the label
     * entity = entity that the label should show up on
     */
    private void showTransientLabel(String name, Node entity) {
        Bounds b = entity.getBoundsInParent();
        
        if (b!=null) {
            transientLabelText = makeNoteSpriteText(name);
            
            transientLabelText.setWrappingWidth(-1);
            transientLabelText.setFill(Color.web("#F0F0F0"));
            transientLabelText.setOnMouseEntered(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    event.consume();
                }
            });
            transientLabelText.setOnMouseClicked(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    event.consume();
                }
            });
            
            Point2D p = CameraHelper.project(camera, 
                    new Point3D((b.getMinX()+b.getMaxX())/2, 
                            (b.getMinY()+b.getMaxY())/2, 
                            (b.getMaxZ()+b.getMinZ())/2));
            double x = p.getX();
            double y = p.getY();
            
            double vOffset = b.getHeight()/2;
            double hOffset = b.getWidth()/2;
    
            x += hOffset;
            y -= vOffset+5;
            
            transientLabelText.setText(name);
            transientLabelText.getTransforms().add(new Translate(x, y));
            
            spritesPane.getChildren().add(transientLabelText);
        }
    }
    
    
    /*
     * Remove transient label from sprites pane
     */
    private void removeTransientLabel() {
        spritesPane.getChildren().remove(transientLabelText);
    }
    
    
    public void setBringUpInfoProperty(BooleanProperty bringUpInfoProperty) {
        this.bringUpInfoProperty = bringUpInfoProperty;
    }
    

    // Called by RootLayoutController to set the loaded SceneElementsList
    public void setSceneElementsList(SceneElementsList list) {
        if (list!=null)
            sceneElementsList = list;
    }
    
    
    public void setStoriesLayer(StoriesLayer layer) {
        if (layer!=null) {
            storiesLayer = layer;
            buildScene();
        }
    }
    

    public IntegerProperty getTimeProperty() {
        return time;
    }

    
    public DoubleProperty getZoomProperty() {
        return zoom;
    }

    
    public IntegerProperty getSelectedIndex() {
        return selectedIndex;
    }
    
    
    public StringProperty getSelectedNameLabeled() {
    	return selectedNameLabeled;
    }

    
    public StringProperty getSelectedName() {
        return selectedName;
    }
    
    
    public BooleanProperty getCellClicked() {
        return cellClicked;
    }

    
    public IntegerProperty getTotalNucleiProperty() {
        return totalNuclei;
    }

    
    public BooleanProperty getPlayingMovieProperty() {
        return playingMovie;
    }
    
    
    @SuppressWarnings("unchecked")
    public void handleMouseEvent(MouseEvent me) {
        EventType<MouseEvent> type = (EventType<MouseEvent>) me.getEventType();
        
        if (type==MouseEvent.MOUSE_ENTERED_TARGET || type==MouseEvent.MOUSE_ENTERED
                || type==MouseEvent.MOUSE_RELEASED || type==MouseEvent.MOUSE_MOVED)
            handleMouseReleasedOrEntered();
        
        else if (type==MouseEvent.MOUSE_CLICKED && me.isStillSincePress())
            handleMouseClicked(me);
        
        else if (type==MouseEvent.MOUSE_DRAGGED)
            handleMouseDragged(me);
        
        else if (type==MouseEvent.MOUSE_PRESSED)
            handleMousePressed(me);
    }
    
    
    private void handleMouseDragged(MouseEvent event) {
        hideContextPopups();
        
        spritesPane.setCursor(Cursor.CLOSED_HAND);

        mouseOldX = mousePosX;
        mouseOldY = mousePosY;
        mousePosX = event.getSceneX();
        mousePosY = event.getSceneY();
        mouseDeltaX = (mousePosX - mouseOldX);
        mouseDeltaY = (mousePosY - mouseOldY);
        mouseDeltaX /= 4;
        mouseDeltaY /= 4;

        if (event.isSecondaryButtonDown() || (event.isPrimaryButtonDown() 
                && (event.isMetaDown() || event.isControlDown()))) {
            double tx = xform.t.getTx()-mouseDeltaX;
            double ty = xform.t.getTy()-mouseDeltaY;

            if (tx>0 && tx<450)
                xform.t.setX(tx);
            if (ty>0 && ty<450)
                xform.t.setY(ty);
            
            repositionSprites();
            repositionNoteBillboardFronts();
        }
        
        else if (event.isPrimaryButtonDown()) {
            mouseDeltaX /= 2;
            mouseDeltaY /= 2;
            
            rotateX.setAngle((rotateX.getAngle()+mouseDeltaY)%360);
            rotateY.setAngle((rotateY.getAngle()-mouseDeltaX)%360);
            
            repositionSprites();
            repositionNoteBillboardFronts();
        }
    }
    
    
    private void handleMouseReleasedOrEntered() {
        spritesPane.setCursor(Cursor.DEFAULT);
    }
    
    
    private void handleMouseClicked(MouseEvent event) {
        spritesPane.setCursor(Cursor.HAND);
        
        hideContextPopups();
        
        Node node = event.getPickResult().getIntersectedNode();
        
        // Nucleus
        if (node instanceof Sphere) {           
            Sphere picked = (Sphere) node;
            selectedIndex.set(getPickedSphereIndex(picked));
            String name = normalizeName(cellNames[selectedIndex.get()]);
            selectedName.set(name);
            cellClicked.set(true);
            
            if (event.getButton()==MouseButton.SECONDARY)
                showContextMenu(name, event.getScreenX(), event.getScreenY(), SearchOption.CELL);
            else if (event.getButton()==MouseButton.PRIMARY) {
                if (allLabels.contains(name)) {
                    allLabels.remove(name);
                    currentLabels.remove(name);
                    
                    removeLabelFor(name);
                }
                else {
                    allLabels.add(name);
                    currentLabels.add(name);
                    
                    insertLabelFor(name, node);
                }
            }

        }
        
        // Cell body/structure
        else if (node instanceof MeshView) {
            boolean found = false;
            for (int i = 0; i < currentSceneElementMeshes.size(); i++) {
                MeshView curr = currentSceneElementMeshes.get(i);
                if (curr.equals(node)) {
                    SceneElement clickedSceneElement = currentSceneElements.get(i);
                    String name = normalizeName(clickedSceneElement.getSceneName());
                    selectedName.set(name);
                    found = true;
                    
                    if (event.getButton()==MouseButton.SECONDARY) {
                        if (sceneElementsList.isMulticellStructureName(name))
                            showContextMenu(name, event.getScreenX(), event.getScreenY(), SearchOption.MULTICELLULAR);
                        else
                            showContextMenu(name, event.getScreenX(), event.getScreenY(), SearchOption.CELLBODY);
                    }
                    
                    else if (event.getButton()==MouseButton.PRIMARY) {
                        if (allLabels.contains(name)) {
                            allLabels.remove(name);
                            currentLabels.remove(name);
                            
                            removeLabelFor(name);
                        }
                        else {
                            allLabels.add(name);
                            currentLabels.add(name);
                            
                            insertLabelFor(name, node);
                        }
                    }
                    
                    break;
                }
            }
            
            // Note structure
            if (!found) {
                for (Note note : currentNoteMeshMap.keySet()) {
                    if (currentNoteMeshMap.get(note).equals(node))
                        selectedName.set(note.getTagName());
                }
            }
        }
        else {
            selectedIndex.set(-1);
            selectedName.set("");
        }
    }
    
    
    private String normalizeName(String name) {
        if (name.indexOf("(") > -1)
            name = name.substring(0, name.indexOf("("));
        name = name.trim();
        return name;
    }
    
    
    private void handleMousePressed(MouseEvent event) {
        mousePosX = event.getSceneX();
        mousePosY = event.getSceneY();
    }
    
    
    private void showContextMenu(String name, double sceneX, double sceneY, SearchOption option) {
        if (contextMenuStage==null) {
            contextMenuController = new ContextMenuController(parentStage, bringUpInfoProperty, cellCases, productionInfo, connectome);
            
            contextMenuStage = new Stage();
            contextMenuStage.initStyle(StageStyle.UNDECORATED);
            
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("view/layouts/ContextMenuLayout.fxml"));
            
            loader.setController(contextMenuController);
            loader.setRoot(contextMenuController);
            
            try {
                contextMenuStage.setScene(new Scene((AnchorPane) loader.load()));
                contextMenuStage.initOwner(parentStage);
                contextMenuStage.initModality(Modality.NONE);
                contextMenuStage.setResizable(false);
                contextMenuStage.setTitle("Menu");
                
                for (Node node : contextMenuStage.getScene().getRoot().getChildrenUnmodifiable()) {
                    node.setStyle("-fx-focus-color: -fx-outer-border; "+
                                    "-fx-faint-focus-color: transparent;");
                }
                
            } catch (IOException e) {
                System.out.println("error in initializing context menu for "+name+".");
                e.printStackTrace();
            }
        }
        
        String funcName = PartsList.getFunctionalNameByLineageName(name);
        if (funcName==null) {
        	contextMenuController.setName(name);
        	contextMenuController.removeTerminalCaseFunctions(true);
        }
        else {
        	contextMenuController.setName(funcName);
        	contextMenuController.removeTerminalCaseFunctions(false);
        }
        
        contextMenuController.setColorButtonListener(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                Rule rule = Search.addColorRule(SearchType.LINEAGE, name, 
                        Color.WHITE, option);
                rule.showEditStage(parentStage);
                
                contextMenuStage.hide();
            }
        });
        
        /*
        contextMenuController.setExpressesButtonListener(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				// TODO Auto-generated method stub
				
			}
        });
        */
        
        contextMenuStage.setX(sceneX);
        contextMenuStage.setY(sceneY);
        
        contextMenuStage.show();
    }

    
    private void createSubScene(Double width, Double height) {
        subscene = new SubScene(root, width, height, true, SceneAntialiasing.BALANCED);
        subscene.setFill(Color.web(FILL_COLOR_HEX));
        buildCamera();
    }
    
    
    private void repositionNoteBillboardFronts() {
        for (Node billboard : billboardFrontEntityMap.keySet()) {
            Node entity = billboardFrontEntityMap.get(billboard);
            if (entity!=null) {
                Bounds b = entity.getBoundsInParent();
                
                if (b!=null) {
                    billboard.getTransforms().clear();
                    double x = b.getMaxX();
                    double y = b.getMaxY()+b.getHeight()/2;
                    double z = b.getMaxZ();
                    billboard.getTransforms().addAll(new Translate(x, y, z),
                            new Scale(BILLBOARD_SCALE, BILLBOARD_SCALE));
                }
            }
        }
    }
    
    
    // Reposition sprites by projecting the sphere's 3d coordinate 
    // onto the front of the subscene
    private void repositionSprites() {      
        for (Node entity : entitySpriteMap.keySet()) {
            alignTextWithEntity(entitySpriteMap.get(entity), entity, false);
        }
        
        for (Node entity : entityLabelMap.keySet()) {
            alignTextWithEntity(entityLabelMap.get(entity), entity, true);
        }
    }
    
    
    // Input text is the note/label geometry
    // Input entity is the cell/body/mesh that the text should align with
    private void alignTextWithEntity(Node noteGraphic, Node node, boolean isLabel) {
        if (node!=null) {
            Bounds b = node.getBoundsInParent();
            //System.out.println("static - "+b.toString());
            
            if (b!=null) {
                noteGraphic.getTransforms().clear();
                Point2D p = CameraHelper.project(camera, 
                        new Point3D((b.getMinX()+b.getMaxX())/2, 
                                (b.getMinY()+b.getMaxY())/2, 
                                (b.getMaxZ()+b.getMinZ())/2));
                double x = p.getX();
                double y = p.getY();
                
                double vOffset = b.getHeight()/2;
                double hOffset = b.getWidth()/2;

                if (isLabel) {
                    x += hOffset;
                    y -= (vOffset+5);
                }
                else {
                    x += hOffset;
                    y += vOffset+5;
                }
                
                noteGraphic.getTransforms().add(new Translate(x, y));
            }
        }
    }

    
    private int getIndexByCellName(String name) {
        for (int i = 0; i < cellNames.length; i++) {
            if (cellNames[i].equals(name))
                return i;
        }
        return -1;
    }
    

    private int getPickedSphereIndex(Sphere picked) {
        for (int i = 0; i < cellNames.length; i++) {
            if (spheres[i].equals(picked)) {
                return i;
            }
        }
        return -1;
    }
    

    /*
     *  Calls service to retrieve subscene data at current time point
     *  then render entities, notes, and labels
     */
    private void buildScene() {
        // Spool thread for actual rendering to subscene
        renderService.restart();
    }
    
    
    private void getSceneData() {
        final int requestedTime = time.get();
        cellNames = cellData.getNames(requestedTime);
        positions = cellData.getPositions(requestedTime);
        diameters = cellData.getDiameters(requestedTime);
        otherCells.clear();
        
        totalNuclei.set(cellNames.length);
        
        spheres = new Sphere[cellNames.length];
        meshes = new MeshView[meshNames.length];
        
        // Start scene element list, find scene elements present at time, build and meshes
        //empty meshes and scene element references from last rendering. Same for story elements
        if (sceneElementsList!=null)
            meshNames = sceneElementsList.getSceneElementNamesAtTime(requestedTime);
        
        if (!currentSceneElementMeshes.isEmpty()) {
            currentSceneElementMeshes.clear();
            currentSceneElements.clear();
        }
        
        if (sceneElementsList != null) {
            sceneElementsAtTime = sceneElementsList.getSceneElementsAtTime(requestedTime);
            for (int i = 0; i < sceneElementsAtTime.size(); i++) {
                //add meshes from each scene element
                SceneElement se = sceneElementsAtTime.get(i);
                MeshView mesh = se.buildGeometry(requestedTime-1);
                
                if (mesh != null) {
                    //null mesh when file not found thrown
                    mesh.getTransforms().addAll(rotateZ, rotateY, rotateX);
                    
                    //add rendered mesh to meshes list
                    currentSceneElementMeshes.add(mesh);
                    
                    //add scene element to rendered scene element reference for on click responsiveness
                    currentSceneElements.add(se);
                }
            }   
        }
        // End scene element mesh loading/building
        
        
        // Label stuff
        entityLabelMap.clear();
        currentLabels.clear();
        
        for (String label : allLabels) {
            for (String cell : cellNames) {
                if (!currentLabels.contains(label) && cell.equalsIgnoreCase(label)) {
                    currentLabels.add(label);
                    break;
                }
            }
            
            for (int i=0; i<currentSceneElements.size(); i++) {
                if (!currentLabels.contains(label) 
                        && label.equalsIgnoreCase(normalizeName(currentSceneElements.get(i).getSceneName()))) {
                    currentLabels.add(label);
                    break;
                }
            }
        }
        // End label stuff
        
        // Story stuff
        // Notes are indexed starting from 1 (or 1+offset shown to user)
        if (storiesLayer!=null) {           
            currentNotes.clear();
            
            currentNoteMeshMap.clear();
            currentGraphicNoteMap.clear();
            
            entitySpriteMap.clear();
            billboardFrontEntityMap.clear();
            
            currentNotes = storiesLayer.getNotesAtTime(requestedTime);
            
            for (Note note : currentNotes) {
                // Revert to overlay display if we have invalid display/attachment 
                // type combination
                if (note.hasLocationError() || note.hasEntityNameError())
                    note.setTagDisplay(Display.OVERLAY);
                
                // make mesh views for scene elements from note resources
                if (note.hasSceneElements()) {
                    for (SceneElement se : note.getSceneElements()) {
                        MeshView mesh = se.buildGeometry(requestedTime);
                        
                        if (mesh!=null) {
                            mesh.setMaterial(colorHash.getNoteSceneElementMaterial());
                            mesh.getTransforms().addAll(rotateZ, rotateY, rotateX);
                            currentNoteMeshMap.put(note, mesh);
                        }
                    }
                }
            }
        }
        // End story stuff
        
        // Search stuff
        if (localSearchResults.isEmpty()) {
            searchedCells = new boolean[cellNames.length];
            searchedMeshes = new boolean[meshNames.length];
        }
        else
            consultSearchResultsList();
        // End search stuff
    }

    
    private void updateLocalSearchResults() {
        if (searchResultsList==null)
            return;

        localSearchResults.clear();

        for (String name : searchResultsList) {
            if (name.indexOf("(")!=-1)
                localSearchResults.add(name.substring(0, name.indexOf("(")).trim());
            else
                localSearchResults.add(name);
        }

        buildScene();
    }
    
    
    public ChangeListener<Boolean> getRebuildFlagListener() {
        return new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, 
                                Boolean oldValue, Boolean newValue) {
                if (newValue)
                    buildScene();
            }
        };
    }

    
    private void refreshScene() {
        // clear note billboards, cell spheres and meshes
        root.getChildren().clear();
        root.getChildren().add(xform);
        
        // clear note sprites and overlays
        overlayVBox.getChildren().clear();
        
        Iterator<Node> iter = spritesPane.getChildren().iterator();
        while (iter.hasNext()) {
            Node node = iter.next();
            if (node instanceof Text)
                iter.remove();
            else if (node instanceof VBox && node!=overlayVBox)
                iter.remove();
        }
    }

    
    private void addEntitiesToScene() {
        ArrayList<Shape3D> entities = new ArrayList<Shape3D>();
        ArrayList<Node> notes = new ArrayList<Node>();
        
        // add spheres
        addCellGeometries(entities);
        
        // add scene element meshes (from notes and from scene elements list)
        addSceneElementGeometries(entities);
        
        Collections.sort(entities, opacityComparator);
        root.getChildren().addAll(entities);
        
        // add notes
        insertOverlayTitles();
        if (!currentNotes.isEmpty())
            addNoteGeometries(notes);
        
        // add labels
        for (String name : currentLabels)
            insertLabelFor(name, getEntityWithName(name));
        
        if (!notes.isEmpty())
            root.getChildren().addAll(notes);
        
        repositionSprites();
        repositionNoteBillboardFronts();
    }
    
    
    private void addSceneElementGeometries(ArrayList<Shape3D> list) {
        // add scene elements from note resources
        for (Note note : currentNoteMeshMap.keySet()) {
            list.add(currentNoteMeshMap.get(note));
        }
        
        // Consult rules
        if (!currentSceneElements.isEmpty()) {  
            for (int i=0; i<currentSceneElements.size(); i++) {
                SceneElement se = currentSceneElements.get(i);
                MeshView mesh = currentSceneElementMeshes.get(i);
                
                // in search mode
                if (inSearch) {
                    if (cellBodyTicked && searchedMeshes[i])
                        mesh.setMaterial(colorHash.getHighlightMaterial());
                    else 
                        mesh.setMaterial(colorHash.getTranslucentMaterial());
                }
                
                else {
                    // in regular view mode
                    ArrayList<String> allNames = se.getAllCellNames();
                    String sceneName = se.getSceneName();
                    
                    // default white meshes
                    if (allNames.isEmpty()) {
                        mesh.setMaterial(new PhongMaterial(Color.WHITE));
                        mesh.setCullFace(CullFace.NONE);
                    }
                    
                    // If mesh has with name(s), then process rules (cell or shape)
                    // that apply to it
                    else {
                        TreeSet<Color> colors = new TreeSet<Color>(colorComparator);
                        
                        //iterate over rulesList
                        for (Rule rule: currentRulesList) {
                            
                            if (rule instanceof MulticellularStructureRule) {
                                //check equivalence of shape rule to scene name
                                if (((MulticellularStructureRule)rule).appliesTo(sceneName)) {
                                    colors.add(Color.web(rule.getColor().toString()));
                                }
                            }
                            
                            else if(rule instanceof ColorRule) {
                                //iterate over cells and check if cells apply
                                for (String name: allNames) {
                                    if(((ColorRule)rule).appliesToBody(name)) {
                                        colors.add(rule.getColor());
                                    }
                                }   
                            }
                        }
                        
                        // if any rules applied
                        if (!colors.isEmpty())
                            mesh.setMaterial(colorHash.getMaterial(colors));
                        else
                            mesh.setMaterial(colorHash.getOthersMaterial(othersOpacity.get()));
                    }
                }
                
                mesh.setOnMouseEntered(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent event) {
                        spritesPane.setCursor(Cursor.HAND);
                        
                        // make label appear
                        String name = normalizeName(se.getSceneName());
                        
                        if (!currentLabels.contains(name)) {
                            String funcName = PartsList.getFunctionalNameByLineageName(name);
                            if (funcName!=null)
                                name = funcName;
                            showTransientLabel(name, mesh);
                        }
                    }
                });
                mesh.setOnMouseExited(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent event) {
                        spritesPane.setCursor(Cursor.DEFAULT);
                        
                        // make label disappear
                        removeTransientLabel();
                    }
                });
                
                list.add(mesh);
            }
        }
    }
    
    
    private void addCellGeometries(ArrayList<Shape3D> list) {
        // Sphere stuff
        for (int i = 0; i < cellNames.length; i ++) {
            double radius;
            if (!uniformSize)
                radius = SIZE_SCALE*diameters[i]/2;
            else
                radius = SIZE_SCALE*UNIFORM_RADIUS;
            Sphere sphere = new Sphere(radius);

            Material material = new PhongMaterial();
            // if in search, do highlighting
            if (inSearch) {
                if (cellNucleusTicked && searchedCells[i])
                    material = colorHash.getHighlightMaterial();
                else
                    material = colorHash.getTranslucentMaterial();
            }
            // not in search mode
            else {
                TreeSet<Color> colors = new TreeSet<Color>(colorComparator);
                for (Rule rule : currentRulesList) {
                    // just need to consult rule's active list
                    if (rule.appliesToCell(cellNames[i])) {
                        colors.add(Color.web(rule.getColor().toString()));
                    }
                }
                material = colorHash.getMaterial(colors);

                if (colors.isEmpty())
                    material = colorHash.getOthersMaterial(othersOpacity.get());
            }

            sphere.setMaterial(material);
            
            sphere.getTransforms().addAll(rotateZ, rotateY, rotateX,
                    new Translate(positions[i][X_COR_INDEX]*X_SCALE,
                            positions[i][Y_COR_INDEX]*Y_SCALE, 
                            positions[i][Z_COR_INDEX]*Z_SCALE));
            
            spheres[i] = sphere;
            
            final int index = i;
            sphere.setOnMouseEntered(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    spritesPane.setCursor(Cursor.HAND);
                    
                    // make label appear
                    String name = cellNames[index];
                    
                    if (!currentLabels.contains(name)) {
                        String funcName = PartsList.getFunctionalNameByLineageName(name);
                        if (funcName!=null)
                            name = funcName;
                        showTransientLabel(name, spheres[index]);
                    }
                }
            });
            sphere.setOnMouseExited(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    spritesPane.setCursor(Cursor.DEFAULT);
                    
                    // make label disappear
                    removeTransientLabel();
                }
            });
            
            list.add(sphere);
        }
        // End sphere stuff
    }
    
    
    private void removeLabelFor(String name) {
        Node entity = getEntityWithName(name);
        
        if (entity!=null)
            removeLabelFrom(entity);
    }
    
    
    private void removeLabelFrom(Node entity) {
        if (entity!=null) {
            spritesPane.getChildren().remove(entityLabelMap.get(entity));
            entityLabelMap.remove(entity);
        }
    }
    
    private void insertLabelFor(String name, Node entity) {
        String funcName = PartsList.getFunctionalNameByLineageName(name);
        Text text;
        if (funcName!=null)
            text = makeNoteSpriteText(funcName);
        else
            text = makeNoteSpriteText(name);
        
        text.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                allLabels.remove(name);
                currentLabels.remove(name);

                removeLabelFor(name);
            }
        });
        
        for (Node shape3D : entityLabelMap.keySet())
            entityLabelMap.get(shape3D).setFill(Color.web(SPRITE_COLOR_HEX));
        
        if (name.equals(selectedName.get()))
        	text.setFill(Color.web(ACTIVE_LABEL_COLOR_HEX));
        
        text.setWrappingWidth(-1);
        
        entityLabelMap.put(entity, text);
        
        spritesPane.getChildren().add(text);
        alignTextWithEntity(text, entity, true);
    }
    
    
    private Shape3D getEntityWithName(String name) {
        // sphere label
        for (int i=0; i<cellNames.length; i++) {
            if (spheres[i]!=null) {             
                if (cellNames[i].equals(name)) {
                    return spheres[i];
                }
            }
            
        }
        
        // mesh view label
        for (int i=0; i<currentSceneElements.size(); i++) {
            if (normalizeName(currentSceneElements.get(i).getSceneName()).equalsIgnoreCase(name)
                    && currentSceneElementMeshes.get(i)!=null) {
                return currentSceneElementMeshes.get(i);
            }
        }
        
        return null;
    }
    
    
    // Inserts note geometries to scene
    // Input list is the list that billboards are added to which are added to the subscene
    // Note overlays and sprites are added to the pane that contains the subscene
    private void addNoteGeometries(ArrayList<Node> list) {
        for (Note note : currentNotes) {
            // map notes to their sphere/mesh view
            Node text = makeNoteGraphic(note);
            currentGraphicNoteMap.put(text, note);
            
            text.setOnMouseEntered(clickableMouseEnteredHandler);
            text.setOnMouseExited(clickableMouseExitedHandler);
            
            // SPRITE
            if (note.isSprite()) {
                // location attachment
                if (note.attachedToLocation()) {
                    VBox box = new VBox(3);
                    box.getChildren().add(text);
                    // add marker to scene
                    Sphere marker = createLocationMarker(note.getX(), note.getY(), note.getZ());
                    root.getChildren().add(marker);
                    entitySpriteMap.put(marker, box);
                    
                    spritesPane.getChildren().add(box);
                }
                
                // cell attachment
                else if (note.attachedToCell()) {
                    for (int i=0; i<cellNames.length; i++) {
                        if (cellNames[i].equalsIgnoreCase(note.getCellName()) && spheres[i]!=null) {
                            // if another note is already attached to the same sphere,
                            // create a vbox for note positioning
                            if (!entitySpriteMap.containsKey(spheres[i])) {
                                VBox box = new VBox(3);
                                box.getChildren().add(text);
                                entitySpriteMap.put(spheres[i], box);
                                spritesPane.getChildren().add(box);
                            }
                            else {
                                entitySpriteMap.get(spheres[i]).getChildren().add(text);
                            }
                            
                            break;
                        }
                    }
                }
                
                // structure attachment
                else if (note.attachedToStructure()) {
                    for (int i=0; i<currentSceneElements.size(); i++) {
                        if (currentSceneElements.get(i).getSceneName()
                                .equalsIgnoreCase(note.getCellName())) {
                            MeshView mesh = currentSceneElementMeshes.get(i);
                            if (!entitySpriteMap.containsKey(mesh)) {
                                VBox box = new VBox(3);
                                box.getChildren().add(text);
                                entitySpriteMap.put(mesh, box);
                                spritesPane.getChildren().add(box);
                            }
                            else
                                entitySpriteMap.get(mesh).getChildren().add(text);
                        }
                    }
                }
            }
            
            // BILLBOARD_FRONT
            else if (note.isBillboardFront()) {
                // location attachment
                if (note.attachedToLocation()) {
                    Sphere marker = createLocationMarker(note.getX(), note.getY(), note.getZ());
                    root.getChildren().add(marker);
                    billboardFrontEntityMap.put(text, marker);
                }
                // cell attachment
                else if (note.attachedToCell()) {
                    for (int i=0; i<cellNames.length; i++) {
                        if (cellNames[i].equalsIgnoreCase(note.getCellName()) && spheres[i]!=null) {
                            billboardFrontEntityMap.put(text, spheres[i]);
                        }
                    }
                }
                // structure attachment
                else if (note.attachedToStructure()) {
                    for (int i=0; i<currentSceneElements.size(); i++) {
                        if (currentSceneElements.get(i).getSceneName()
                                .equalsIgnoreCase(note.getCellName())) {
                            billboardFrontEntityMap.put(text, currentSceneElementMeshes.get(i));
                        }
                    }
                }
            }
            
            // BILLBOARD
            else if (note.isBillboard()) {
                // location attachment
                if (note.attachedToLocation()) {
                    text.getTransforms().addAll(rotateZ, rotateY, rotateX);
                    text.getTransforms().addAll(
                            new Translate(note.getX(), note.getY(), note.getZ()),
                            new Scale(BILLBOARD_SCALE, BILLBOARD_SCALE));
                }
                // cell attachment
                else if (note.attachedToCell()) {
                    for (int i=0; i<cellNames.length; i++) {
                        if (cellNames[i].equalsIgnoreCase(note.getCellName()) && spheres[i]!=null) {
                            double offset = 5;
                            if (!uniformSize)
                                offset = spheres[i].getRadius()+2;
                            
                            text.getTransforms().addAll(spheres[i].getTransforms());
                            text.getTransforms().addAll(new Translate(offset, offset),
                                    new Scale(BILLBOARD_SCALE, BILLBOARD_SCALE));
                        }
                    }
                }
                // structure attachment
                else if (note.attachedToStructure()) {
                    for (int i=0; i<currentSceneElements.size(); i++) {
                        if (currentSceneElements.get(i).getSceneName()
                                .equalsIgnoreCase(note.getCellName())) {
                            text.getTransforms().addAll(currentSceneElementMeshes.get(i)
                                    .getTransforms());
                            double offset = 5;
                            text.getTransforms().addAll(new Translate(offset, offset),
                                    new Scale(BILLBOARD_SCALE, BILLBOARD_SCALE));
                        }
                    }
                }
            }
            
            // add graphic to appropriate place (scene, overlay box, or on top of scene)
            Display display = note.getTagDisplay();
            if (display!=null) {
                switch (display) {
                case SPRITE:
                                break;
                
                case BILLBOARD_FRONT:   // fall to billboard case
                                
                case BILLBOARD:
                                list.add(text);
                                break;
                                
                case OVERLAY:   // fall to default case
                
                case BLANK:     // fall to default case
                
                default:
                                overlayVBox.getChildren().add(text);
                                break;
                }
            }
        }
    }
    
    
    private void insertOverlayTitles() {
        if (storiesLayer!=null) {
            if (storiesLayer.getActiveStory()!=null) {
                Text infoPaneTitle = makeNoteOverlayText("Info Pane:");
                Text storyTitle = makeNoteOverlayText(storiesLayer.getActiveStory().getName());
                overlayVBox.getChildren().addAll(infoPaneTitle, storyTitle);
            }
        }
    }
    
    
    private Text makeNoteOverlayText(String title) {
        Text text = new Text(title);
        text.setFill(Color.web(SPRITE_COLOR_HEX));
        text.setFontSmoothingType(FontSmoothingType.LCD);
        text.setWrappingWidth(overlayVBox.getWidth());
        text.setFont(AppFont.getSpriteAndOverlayFont());
        return text;
    }
    
    
    private Text makeNoteSpriteText(String title) {
        Text text = makeNoteOverlayText(title);
        text.setWrappingWidth(160);
        return text;
    }
    
    
    private Text makeNoteBillboardText(String title) {
        Text text = new Text(title);
        text.setWrappingWidth(90);
        text.setFont(AppFont.getBillboardFont());
        text.setSmooth(false);
        text.setStrokeWidth(2);
        text.setFontSmoothingType(FontSmoothingType.LCD);
        text.setCacheHint(CacheHint.QUALITY);
        text.setFill(Color.web(SPRITE_COLOR_HEX));
        return text;
    }
    
    
    private Sphere createLocationMarker(double x, double y, double z) {
        Sphere sphere = new Sphere(1);
        sphere.getTransforms().addAll(rotateZ, rotateY, rotateX,
                new Translate(x*X_SCALE, y*Y_SCALE, z*Z_SCALE));
        // make marker transparent
        sphere.setMaterial(colorHash.getOthersMaterial(0));
        return sphere;
    }
    
    
    // Makes an anchor pane that contains the text to be shown
    // if isOverlay is true, then the text is larger
    private Text makeNoteGraphic(Note note) {
        String title = note.getTagName();
        if (note.isSceneExpanded())
            title += ": "+note.getTagContents();
        
        Text node = null;
        if (note.getTagDisplay()!=null) {
            switch (note.getTagDisplay()) {
                case SPRITE:
                            node = makeNoteSpriteText(title);
                            break;
                            
                case BILLBOARD:
                            node = makeNoteBillboardText(title);
                            break;
                            
                case BILLBOARD_FRONT:
                            node = makeNoteBillboardText(title);
                            break;
                            
                case OVERLAY:   // fall to default case
                    
                case BLANK:     // fall to default case
                
                default:
                            node = makeNoteOverlayText(title);
                            break;
                            
            }
        }
        return node;
    }

    
    private void buildCamera() {
        this.camera = new PerspectiveCamera(true);
        this.xform = new Xform();
        xform.reset();

        root.getChildren().add(xform);
        xform.getChildren().add(camera);

        camera.setNearClip(CAMERA_NEAR_CLIP);
        camera.setFarClip(CAMERA_FAR_CLIP);
        camera.setTranslateZ(CAMERA_INITIAL_DISTANCE);

        xform.setScaleX(X_SCALE);
        xform.setScaleY(Y_SCALE);

        setNewOrigin();

        subscene.setCamera(camera);
    }

    
    private void setNewOrigin() {
        // Find average X Y positions of initial timepoint
        Integer[][] positions = cellData.getPositions(startTime);
        int numCells = positions.length;
        int sumX = 0;
        int sumY = 0;
        int sumZ = 0;
        for (int i = 0; i < numCells; i++) {
            sumX += positions[i][X_COR_INDEX];
            sumY += positions[i][Y_COR_INDEX];
            sumZ += positions[i][Z_COR_INDEX];
        }
        newOriginX = (int) Math.round(X_SCALE*sumX/numCells);
        newOriginY = (int) Math.round(Y_SCALE*sumY/numCells);
        newOriginZ = (int) Math.round(Z_SCALE*sumZ/numCells);

        // Set new origin to average X Y positions
        xform.setTranslate(newOriginX, newOriginY, newOriginZ);
        System.out.println("origin xyz: "+newOriginX+" "+newOriginY+" "+newOriginZ);
    }
    

    public void setSearchResultsList(ObservableList<String> list) {
        searchResultsList = list;
    }

    
    public void setSearchResultsUpdateService(Service<Void> service) {
        service.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                updateLocalSearchResults();
            }
        });
    }

    
    public void setGeneResultsUpdated(BooleanProperty updated) {
        geneResultsUpdated = updated;
        geneResultsUpdated.addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable,
                    Boolean oldValue, Boolean newValue) {
                updateLocalSearchResults();
            }
        });
    }

    
    public void consultSearchResultsList() {
        searchedCells = new boolean[cellNames.length];
        searchedMeshes = new boolean[meshNames.length];
        
        // look for searched cells
        for (int i=0; i<cellNames.length; i++) {
            if (localSearchResults.contains(cellNames[i]))
                searchedCells[i] = true;
            else
                searchedCells[i] = false;
        }
        
        // look for single celled meshes
        for (int i=0; i<meshNames.length; i++) {
            if (sceneElementsAtTime.get(i).isMulticellular()) {
                searchedMeshes[i] = true;
                for (String name : sceneElementsAtTime.get(i).getAllCellNames()) {
                    if (localSearchResults.contains(name))
                        searchedMeshes[i] &= true;
                    else
                        searchedMeshes[i] &= false;
                }
            }
            else {
                if (localSearchResults.contains(meshNames[i]))
                    searchedMeshes[i] = true;
                else
                    searchedMeshes[i] = false;
            }
        }
    }

    
    public void printCellNames() {
        for (int i = 0; i < cellNames.length; i++)
            System.out.println(cellNames[i]+CS+spheres[i]);
    }
    
    
    public void printMeshNames() {
        for (int i = 0; i < meshNames.length; i++)
            System.out.println(meshNames[i]+CS+meshes[i]);
    }

    
    // sets everything associated with color rules
    public void setRulesList(ObservableList<Rule> list) {
        if (list == null)
            return;
        
        currentRulesList = list;
        currentRulesList.addListener(new ListChangeListener<Rule>() {
            @Override
            public void onChanged(
                    ListChangeListener.Change<? extends Rule> change) {
                while (change.next()) {
                    if (change.getAddedSize()>0) {
                        buildScene();
                        
                        for (Rule rule : change.getAddedSubList()) {
                            rule.getRuleChangedProperty().addListener(new ChangeListener<Boolean>() {
                                @Override
                                public void changed(ObservableValue<? extends Boolean> observable,
                                        Boolean oldValue, Boolean newValue) {
                                    if (newValue)
                                        buildScene();
                                    
                                    rule.setChanged(false);
                                }
                            });
                        }
                    }
                    
                    if (!change.getRemoved().isEmpty()) {
                        buildScene();
                    }
                }
            }
        });
    }
    
    
    // Sets transparent anchor pane overlay for sprite notes display
    public void setNotesPane(AnchorPane parentPane) {
        if (parentPane!=null) {
            spritesPane = parentPane;
            
            overlayVBox = new VBox(5);
            overlayVBox.setPrefWidth(170);
            overlayVBox.setMaxWidth(overlayVBox.getPrefWidth());
            overlayVBox.setMinWidth(overlayVBox.getPrefWidth());
            
            AnchorPane.setTopAnchor(overlayVBox, 5.0);
            AnchorPane.setRightAnchor(overlayVBox, 5.0);
            
            spritesPane.getChildren().add(overlayVBox);
        }
    }
    
    
    // Hides cell name label/context menu
    private void hideContextPopups() {
        if (contextMenuStage!=null)
            contextMenuStage.hide();
    }

    
    public ArrayList<ColorRule> getColorRulesList() {
        ArrayList<ColorRule> list = new ArrayList<ColorRule>();
        for (Rule rule : currentRulesList) {
            if (rule instanceof ColorRule)
                list.add((ColorRule)rule);
        }
        return list;
    }

    
    public ObservableList<Rule> getObservableColorRulesList() {
        return currentRulesList;
    }

    
    public void setColorRulesList(ArrayList<ColorRule> list) {
        currentRulesList.clear();
        currentRulesList.setAll(list);
    }

    
    public int getTime() {
        return time.get();
    }

    
    public void setTime(int t) {
        if (startTime<=t && t<=endTime) {
            time.set(t);
            hideContextPopups();
        }
    }
    
    
    public void setRotations(double rx, double ry, double rz) {
        /*
        rx = Math.toDegrees(rx);
        ry = Math.toDegrees(ry);
        rx = Math.toDegrees(rz);
        */
        
        //rotateX.setAngle(rx+180);
        rotateX.setAngle(rx);
        rotateY.setAngle(ry);
        rotateZ.setAngle(rz);
    }
    
    
    public double getRotationX() {

        return rotateX.getAngle();
    }

    
    public double getRotationY() {
        return rotateY.getAngle();
    }
    
    
    public double getRotationZ() {
        return rotateZ.getAngle();
    }

    
    public double getTranslationX() {
        return xform.t.getTx()-newOriginX;
    }
    
    
    public void setTranslationX(double tx) {
        double newTx = tx+newOriginX;
        if (newTx>0 && newTx<450)
            xform.t.setX(newTx);
    }
    
    
    public double getTranslationY() {
        return xform.t.getTy()-newOriginY;
    }
    
    
    public void setTranslationY(double ty) {
        double newTy = ty+newOriginY;
        if (newTy>0 && newTy<450)
            xform.t.setY(newTy);
    }

    
    public double getScale() {
        double scale = zoom.get()-0.5;
        scale = 1-(scale/6.5);
        return scale;
    }

    
    public void setScale(double scale) {
        if (scale > 1)
            scale = 1;
        scale = 6.5*(1-scale);
        // smaller zoom value means larger picture
        zoom.set((scale+0.5));
    }

    
    public double getOthersVisibility() {
        return othersOpacity.get();
    }
    

    public void setOthersVisibility(double dim) {
        othersOpacity.set(dim);
    }
    

    public SubScene getSubScene() {
        return subscene;
    }

    
    public Group getRoot() {
        return root;
    }
    

    public ChangeListener<Number> getOthersOpacityListener() {
        return new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable,
                    Number oldValue, Number newValue) {
                othersOpacity.set(Math.round(newValue.doubleValue())/100d);
                
                buildScene();
            }
        };
    }

    
    public void addListenerToOpacitySlider(Slider slider) {
        othersOpacity.addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> arg0, Number arg1, Number arg2) {
                Double arg = arg0.getValue().doubleValue();
                if (arg>=0 && arg<=1.0) {
                    slider.setValue(arg*100.0);
                }
            }
        });
    }

    
    public ChangeListener<String> getSearchFieldListener() {
        return new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable,
                                        String oldValue, String newValue) {
                if (newValue.isEmpty()) {
                    inSearch = false;
                    buildScene();
                }
                else
                    inSearch = true;
            }
        };
    }
    
    
    public void setSearchField(TextField field) {
        searchField = field;
        searchField.textProperty().addListener(getSearchFieldListener());
    }
    

    public int getEndTime() {
        return endTime;
    }

    
    public int getStartTime() {
        return startTime;
    }

    public EventHandler<ActionEvent> getZoomInButtonListener() {
        return new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                hideContextPopups();
                
                double z = zoom.get();
                z-=0.25;
                if (z<0.25)
                    z = 0.25;
                else if (z>5)
                    z = 5;
                
                zoom.set(z);
            }
        };
    }
    

    public EventHandler<ActionEvent> getZoomOutButtonListener() {
        return new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                hideContextPopups();
                
                double z = zoom.get();
                z+=0.25;
                if (z<0.25)
                    z = 0.25;
                else if (z>5)
                    z = 5;
                
                zoom.set(z);
            }
        };
    }
    

    public EventHandler<ActionEvent> getBackwardButtonListener() {
        return new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                hideContextPopups();
                
                if (!playingMovie.get()) {
                    int t = time.get();
                    if (t>1 && t<=getEndTime())
                        time.set(t-1);
                }
            }
        };
    }
    

    public EventHandler<ActionEvent> getForwardButtonListener() {
        return new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                hideContextPopups();
                
                if (!playingMovie.get()) {
                    int t = time.get();
                    if (t>=1 && t<getEndTime()-1)
                        time.set(t+1);
                }
            }
        };
    }
    
    
    public ChangeListener<Boolean> getUniformSizeCheckBoxListener() {
        return new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, 
                                    Boolean oldValue, Boolean newValue) {
                uniformSize = newValue.booleanValue();
                buildScene();
            }
        };
    }
    

    private final class RenderService extends Service<Void> {
        @Override
        protected Task<Void> createTask() {
            return new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            getSceneData();
                            refreshScene();
                            addEntitiesToScene();
                        }
                    });
                    return null;
                }
            };
        }
    }
    

    private final class PlayService extends Service<Void> {
        @Override
        protected final Task<Void> createTask() {
            return new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    while (true) {
                        if (isCancelled()) {
                            break;
                        }
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                if (time.get()<endTime)
                                    setTime(time.get()+1);
                                else
                                    setTime(endTime);
                            }
                        });
                        try {
                            Thread.sleep(WAIT_TIME_MILLI);
                        } catch (InterruptedException ie) {
                            break;
                        }
                    }
                    return null;
                }
            };
        }
    }
    
    
    private final class SubsceneSizeListener implements ChangeListener<Number> {
        @Override
        public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
            repositionSprites();
            repositionNoteBillboardFronts();
        }
    }
    

    public ChangeListener<Boolean> getCellNucleusTickListener() {
        return new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, 
                    Boolean oldValue, Boolean newValue) {
                cellNucleusTicked = newValue;
                buildScene();
            }
        };
    }
    
    
    public ChangeListener<Boolean> getCellBodyTickListener() {
        return new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, 
                    Boolean oldValue, Boolean newValue) {
                cellBodyTicked = newValue;
                buildScene();
            }
        };
    }
    
    
    private class OpacityComparator implements Comparator<Shape3D> {
        @Override
        public int compare(Shape3D o1, Shape3D o2) {
            double op1 = colorHash.getMaterialOpacity(o1.getMaterial());
            double op2 = colorHash.getMaterialOpacity(o2.getMaterial());
            if (op1 < op2)
                return 1;
            else if (op1 > op2)
                return -1;
            else
                return 0;
        }
    }
    
    
    public ChangeListener<Boolean> getMulticellModeListener() {
        return new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
            }
        };
    }
    
    
    public EventHandler<MouseEvent> getNoteClickHandler() {
        return new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.isStillSincePress()) {
                    Node result = event.getPickResult().getIntersectedNode();
                    if (result instanceof Text) {
                        Text picked = (Text) result;
                        Note note = currentGraphicNoteMap.get(picked);
                        if (note!=null) {
                            note.setSceneExpanded(!note.isSceneExpanded());
                            if (note.isSceneExpanded())
                                picked.setText(note.getTagName()+": "+note.getTagContents());
                            else
                                picked.setText(note.getTagName());
                        }
                    }
                }
            }
        };
    }

    
    public Stage getStage() {
        return this.parentStage;
    }


    private final String CS = ", ";

    private final String FILL_COLOR_HEX = "#272727";
    
    private final String ACTIVE_LABEL_COLOR_HEX = "#ffff66";
    private final String SPRITE_COLOR_HEX = "#ffffff";

    private final long WAIT_TIME_MILLI = 200;

    private final double CAMERA_INITIAL_DISTANCE = -220;

    private final double CAMERA_NEAR_CLIP = 1,
                        CAMERA_FAR_CLIP = 2000;

    private final int X_COR_INDEX = 0,
                    Y_COR_INDEX = 1,
                    Z_COR_INDEX = 2;

    private final double Z_SCALE = 5,
                        X_SCALE = 1,
                        Y_SCALE = 1;
    private final double BILLBOARD_SCALE = 0.9;
    
    private final double SIZE_SCALE = 1;
    private final double UNIFORM_RADIUS = 4;
    
}
