/*
 * Bao Lab 2017
 */

package wormguides.controllers;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SnapshotParameters;
import javafx.scene.SubScene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Material;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Shape3D;
import javafx.scene.shape.Sphere;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

import acetree.LineageData;
import com.sun.javafx.scene.CameraHelper;
import connectome.Connectome;
import wormguides.layers.SearchLayer;
import wormguides.layers.StoriesLayer;
import wormguides.models.camerageometry.Quaternion;
import wormguides.models.camerageometry.Xform;
import wormguides.models.cellcase.CasesLists;
import wormguides.models.colorrule.Rule;
import wormguides.models.subscenegeometry.SceneElement;
import wormguides.models.subscenegeometry.SceneElementsList;
import wormguides.resources.ProductionInfo;
import wormguides.stories.Note;
import wormguides.stories.Note.Display;
import wormguides.util.ColorComparator;
import wormguides.util.ColorHash;
import wormguides.util.subscenesaving.JavaPicture;
import wormguides.util.subscenesaving.JpegImagesToMovie;

import static java.lang.Math.pow;
import static java.lang.Math.round;
import static java.lang.Math.sqrt;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import static javafx.application.Platform.runLater;
import static javafx.scene.CacheHint.QUALITY;
import static javafx.scene.Cursor.CLOSED_HAND;
import static javafx.scene.Cursor.DEFAULT;
import static javafx.scene.Cursor.HAND;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import static javafx.scene.input.MouseEvent.MOUSE_DRAGGED;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED_TARGET;
import static javafx.scene.input.MouseEvent.MOUSE_MOVED;
import static javafx.scene.input.MouseEvent.MOUSE_PRESSED;
import static javafx.scene.input.MouseEvent.MOUSE_RELEASED;
import static javafx.scene.layout.AnchorPane.setRightAnchor;
import static javafx.scene.layout.AnchorPane.setTopAnchor;
import static javafx.scene.paint.Color.RED;
import static javafx.scene.paint.Color.WHITE;
import static javafx.scene.paint.Color.web;
import static javafx.scene.text.FontSmoothingType.LCD;
import static javafx.scene.transform.Rotate.X_AXIS;
import static javafx.scene.transform.Rotate.Y_AXIS;
import static javafx.scene.transform.Rotate.Z_AXIS;

import static javax.imageio.ImageIO.write;
import static partslist.PartsList.getFunctionalNameByLineageName;
import static search.SearchType.LINEAGE;
import static search.SearchType.NEIGHBOR;
import static search.SearchUtil.getFirstOccurenceOf;
import static search.SearchUtil.getLastOccurenceOf;
import static wormguides.models.colorrule.SearchOption.CELL_BODY;
import static wormguides.models.colorrule.SearchOption.CELL_NUCLEUS;
import static wormguides.stories.Note.Display.OVERLAY;
import static wormguides.util.AppFont.getBillboardFont;
import static wormguides.util.AppFont.getSpriteAndOverlayFont;

/**
 * The controller for the 3D subscene inside the rootEntitiesGroup layout. This class contains the subscene itself, and
 * places it into the AnchorPane called modelAnchorPane inside the rootEntitiesGroup layout. It is also responsible
 * for refreshing the scene on timeProperty, search, wormguides.stories, notes, and rules change. This class contains
 * observable properties that are passed to other classes so that a subscene refresh can be trigger from that other
 * class.
 * <p>
 * An "entity" in the subscene is either a cell, cell body, or multicellular structure. These are graphically
 * represented by the Shape3Ds Sphere and MeshView available in JavaFX. {@link Sphere}s represent cells, and
 * {@link MeshView}s represent cell bodies and multicellular structures. Notes and labels are rendered as
 * {@link Text}s. This class queries the {@link LineageData} and {@link SceneElementsList} for a certain timeProperty
 * and renders the entities, notes, story, and labels present in that timeProperty point.
 * <p>
 * For the coloring of entities, an observable list of {@link Rule}s is queried to see which ones apply to a
 * particular entity, then queries the {@link ColorHash} for the {@link Material} to use for the entity.
 */
public class Window3DController {

    private final double CANNONICAL_ORIENTATION_X = 145.0;
    private final double CANNONICAL_ORIENTATION_Y = -166.0;
    private final double CANNONICAL_ORIENTATION_Z = 24.0;
    private final String CS = ", ";
    private final String FILL_COLOR_HEX = "#272727",
            ACTIVE_LABEL_COLOR_HEX = "#ffff66",
            SPRITE_COLOR_HEX = "#ffffff",
            TRANSIENT_LABEL_COLOR_HEX = "#f0f0f0";
    /** The wait timeProperty (in millis) between consecutive timeProperty frames while a movie is playing. */
    private final long WAIT_TIME_MILLI = 200;
    /**
     * Initial zoom of embryo view. On program startup, the embryo is zoomed in so that the entire embryo is not
     * visible.
     */
    private final double INITIAL_ZOOM = 2.75;
    private final double INITIAL_TRANSLATE_X = -14.0,
            INITIAL_TRANSLATE_Y = 18.0;
    private final double CAMERA_INITIAL_DISTANCE = -220;
    private final double CAMERA_NEAR_CLIP = 1,
            CAMERA_FAR_CLIP = 2000;
    private final int X_COR_INDEX = 0,
            Y_COR_INDEX = 1,
            Z_COR_INDEX = 2;
    /** Text size scale used for the rendering of billboard notes. */
    private final double BILLBOARD_SCALE = 0.9;
    /**
     * Scale used for the radii of spheres that represent cells, multiplied with the cell's radius loaded from the
     * nuc files.
     */
    private final double SIZE_SCALE = 1;
    /** The radius of all spheres when 'uniform size' is ticked. */
    private final double UNIFORM_RADIUS = 4;
    /**
     * The number of pixels that a sprite (note or label) can be outside the sprite pane bounds before being removed
     * from the subscene.
     */
    private final int OUT_OF_BOUNDS_THRESHOLD = 5;
    /** The y-offset from a sprite to a label label for one cell entity. */
    private final int LABEL_SPRITE_Y_OFFSET = 5;
    /** Default transparency of 'other' entities on startup */
    private final double DEFAULT_OTHERS_OPACITY = 0.25;
    private final double VISIBILITY_CUTOFF = 0.05;

    // rotation stuff
    private final Rotate rotateX;
    private final Rotate rotateY;
    private final Rotate rotateZ;
    // transformation stuff
    private final Group rootEntitiesGroup;
    // switching timepoints stuff
    private final BooleanProperty playingMovieProperty;
    private final PlayService playService;
    private final RenderService renderService;
    /** Local version of the serach results that only contains lineage names */
    private final List<String> localSearchResults;
    // color rules stuff
    private final ColorHash colorHash;
    private final Comparator<Color> colorComparator;
    private final Comparator<Shape3D> opacityComparator;
    // opacity value for "other" cells (with no rule attached)
    private final DoubleProperty othersOpacityProperty;
    private final List<String> otherCells;
    private final Vector<JavaPicture> javaPictures;
    private final SearchLayer searchLayer;
    private final Stage parentStage;
    private final LineageData lineageData;
    private final SubScene subscene;
    private final TextField searchField;
    // housekeeping stuff
    private final BooleanProperty rebuildSubsceneFlag;
    private final DoubleProperty rotateXAngleProperty;
    private final DoubleProperty rotateYAngleProperty;
    private final DoubleProperty rotateZAngleProperty;
    private final DoubleProperty translateXProperty;
    private final DoubleProperty translateYProperty;
    private final IntegerProperty timeProperty;
    private final IntegerProperty totalNucleiProperty;
    /** Start timeProperty of the lineage without movie timeProperty offset */
    private final int startTime;
    /** End timeProperty of the lineage without movie timeProperty offset */
    private final int endTime;
    private final DoubleProperty zoomProperty;
    // subscene click cell selection stuff
    private final IntegerProperty selectedIndex;
    private final StringProperty selectedNameProperty;
    private final StringProperty selectedNameLabeledProperty;
    private final Stage contextMenuStage;
    private final ContextMenuController contextMenuController;
    private final CasesLists casesLists;
    private final BooleanProperty cellClickedProperty;
    private final ObservableList<String> searchResultsList;
    private final ObservableList<Rule> rulesList;
    // Scene Elements stuff
    private final boolean defaultEmbryoFlag;
    private final SceneElementsList sceneElementsList;
    // Story elements stuff
    private final StoriesLayer storiesLayer;
    /** Map of current note graphics to their note objects */
    private final HashMap<Node, Note> currentGraphicNoteMap;
    /** Map of current notes to their scene elements */
    private final HashMap<Note, MeshView> currentNoteMeshMap;
    /** Map of note sprites attached to cell, or cell and timeProperty */
    private final HashMap<Node, VBox> entitySpriteMap;
    /** Map of front-facing billboards attached to cell, or cell and timeProperty */
    private final HashMap<Node, Node> billboardFrontEntityMap;
    /** Map of a cell entity to its label */
    private final Map<Node, Text> entityLabelMap;
    // orientation indicator
    private final Cylinder orientationIndicator;
    // rotation
    private final double[] keyValuesRotate = {90, 30, 30, 90};
    //  private final double[] keyValuesRotate = {60, 1, 1, 60};
    private final double[] keyFramesRotate = {1, 16, 321, 359};

    private final EventHandler<MouseEvent> clickableMouseEnteredHandler;
    private final EventHandler<MouseEvent> clickableMouseExitedHandler;
    private final ProductionInfo productionInfo;
    private final Connectome connectome;
    private final BooleanProperty bringUpInfoFlag;
    private final SubsceneSizeListener subsceneSizeListener;
    // subscene state parameters
    private Sphere[] spheres;
    private MeshView[] meshes;
    private String[] cellNames;
    private String[] meshNames;
    private boolean[] isCellSearchedFlags;
    private boolean[] isMeshSearchedFlags;
    private double[][] positions;
    private double[] diameters;
    private List<SceneElement> sceneElementsAtCurrentTime;
    private List<MeshView> currentSceneElementMeshes;
    private List<SceneElement> currentSceneElements;
    private PerspectiveCamera camera;
    private Xform xform;
    private double mousePosX, mousePosY, mousePosZ;
    private double mouseOldX, mouseOldY, mouseOldZ;

    // Label stuff
    private double mouseDeltaX, mouseDeltaY;
    // average position offsets of nuclei from zero
    private int offsetX, offsetY, offsetZ;
    private double angleOfRotation;
    // searched highlighting stuff
    private boolean isInSearchMode;
    // Uniform nuclei sizef
    private boolean uniformSize;
    // Cell body and cell nucleus highlighting in search mode
    private boolean cellNucleusTicked;
    private boolean cellBodyTicked;
    /** All notes that are active, or visible, in a frame */
    private List<Note> currentNotes;
    /**
     * Rectangular box that resides in the upper-right-hand corner of the subscene. The active story title and
     * description are shown here.
     */
    private VBox overlayVBox;
    /** Overlay of the subscene. Note sprites are inserted into this overlay. */
    private Pane spritesPane;
    /** Labels that exist in any of the timeProperty frames */
    private List<String> allLabels;
    /** Labels currently visible in the frame */
    private List<String> currentLabels;
    /** Label that shows up on hover */
    private Text transientLabelText;
    private Rotate indicatorRotation;// this is the timeProperty varying component of
    private BooleanProperty captureVideo;
    private Timer timer;
    private Vector<File> movieFiles;
    private int count;
    private String movieName;
    private String moviePath;
    private File frameDir;

    private Quaternion quaternion;

    /** X-scale of the subscene coordinate axis read from ProductionInfo.csv */
    private double xScale;
    /** Y-scale of the subscene coordinate axis read from ProductionInfo.csv */
    private double yScale;
    /** Z-scale of the subscene coordinate axis read from ProductionInfo.csv */
    private double zScale;

    public Window3DController(
            final Stage parentStage,
            final Group rootEntitiesGroup,
            final SubScene subscene,
            final AnchorPane parentPane,
            final LineageData lineageData,
            final CasesLists casesLists,
            final ProductionInfo productionInfo,
            final Connectome connectome,
            final SceneElementsList sceneElementsList,
            final StoriesLayer storiesLayer,
            final SearchLayer searchLayer,
            final BooleanProperty bringUpInfoFlag,
            final int offsetX,
            final int offsetY,
            final int offsetZ,
            final boolean defaultEmbryoFlag,
            final double xScale,
            final double yScale,
            final double zScale,
            final AnchorPane modelAnchorPane,
            final Button backwardButton,
            final Button forwardButton,
            final Button zoomOutButton,
            final Button zoomInButton,
            final Button clearAllLabelsButton,
            final TextField searchField,
            final Slider opacitySlider,
            final CheckBox uniformSizeCheckBox,
            final CheckBox cellNucleusCheckBox,
            final CheckBox cellBodyCheckBox,
            final RadioButton multiRadioBtn,
            final int startTime,
            final int endTime,
            final IntegerProperty timeProperty,
            final IntegerProperty totalNucleiProperty,
            final DoubleProperty zoomProperty,
            final DoubleProperty othersOpacityProperty,
            final DoubleProperty rotateXAngleProperty,
            final DoubleProperty rotateYAngleProperty,
            final DoubleProperty rotateZAngleProperty,
            final DoubleProperty translateXProperty,
            final DoubleProperty translateYProperty,
            final StringProperty selectedNameProperty,
            final StringProperty selectedNameLabeledProperty,
            final BooleanProperty cellClickedFlag,
            final BooleanProperty playingMovieFlag,
            final BooleanProperty geneResultsUpdatedFlag,
            final BooleanProperty rebuildSubsceneFlag,
            final ObservableList<Rule> rulesList,
            final ColorHash colorHash,
            final Stage contextMenuStage,
            final ContextMenuController contextMenuController,
            final Service<Void> searchResultsUpdateService,
            final ObservableList<String> searchResultsList) {

        this.parentStage = requireNonNull(parentStage);

        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;

        this.startTime = startTime;
        this.endTime = endTime;

        this.rootEntitiesGroup = requireNonNull(rootEntitiesGroup);
        this.lineageData = lineageData;
        this.productionInfo = requireNonNull(productionInfo);
        this.connectome = requireNonNull(connectome);
        this.sceneElementsList = requireNonNull(sceneElementsList);
        this.storiesLayer = requireNonNull(storiesLayer);
        this.searchLayer = requireNonNull(searchLayer);

        this.defaultEmbryoFlag = defaultEmbryoFlag;

        this.timeProperty = requireNonNull(timeProperty);
        this.timeProperty.addListener((observable, oldValue, newValue) -> {
            final int newTime = newValue.intValue();
            final int oldTime = oldValue.intValue();
            if (startTime <= newTime && newTime <= endTime) {
                hideContextPopups();
            } else if (newTime < startTime) {
                timeProperty.set(startTime);
            } else if (newTime > endTime) {
                timeProperty.set(endTime);
            }
        });

        spheres = new Sphere[1];
        meshes = new MeshView[1];
        cellNames = new String[1];
        meshNames = new String[1];
        positions = new double[1][3];
        diameters = new double[1];
        isCellSearchedFlags = new boolean[1];
        isMeshSearchedFlags = new boolean[1];

        selectedIndex = new SimpleIntegerProperty(-1);

        this.selectedNameProperty = requireNonNull(selectedNameProperty);
        this.selectedNameProperty.addListener((observable, oldValue, newValue) -> {
            int selected = getIndexByCellName(newValue);
            if (selected != -1) {
                selectedIndex.set(selected);
            }
        });

        this.selectedNameLabeledProperty = requireNonNull(selectedNameLabeledProperty);
        this.selectedNameLabeledProperty.addListener((observable, oldValue, newValue) -> {
            if (!newValue.isEmpty()) {
                String lineageName = newValue;
                this.selectedNameProperty.set(lineageName);

                if (!allLabels.contains(lineageName)) {
                    allLabels.add(lineageName);
                }

                final Shape3D entity = getEntityWithName(lineageName);

                // go to labeled name
                int startTime1;
                int endTime1;

                startTime1 = getFirstOccurenceOf(lineageName);
                endTime1 = getLastOccurenceOf(lineageName);

                // do not change scene is entity does not exist at any timeProperty
                if (startTime1 <= 0 || endTime1 <= 0) {
                    return;
                }

                if (timeProperty.get() < startTime1 || timeProperty.get() > endTime1) {
                    timeProperty.set(startTime1);
                } else {
                    insertLabelFor(lineageName, entity);
                }
                highlightActiveCellLabel(entity);
            }
        });

        this.rulesList = requireNonNull(rulesList);

        this.cellClickedProperty = requireNonNull(cellClickedFlag);
        this.totalNucleiProperty = requireNonNull(totalNucleiProperty);

        this.subscene = requireNonNull(subscene);
        buildCamera();
        parentPane.getChildren().add(this.subscene);

        isInSearchMode = false;

        subsceneSizeListener = new SubsceneSizeListener();
        parentPane.widthProperty().addListener(subsceneSizeListener);
        parentPane.heightProperty().addListener(subsceneSizeListener);

        mousePosX = 0.0;
        mousePosY = 0.0;
        mousePosZ = 0.0;
        mouseOldX = 0.0;
        mouseOldY = 0.0;
        mouseOldZ = 0.0;
        mouseDeltaX = 0.0;
        mouseDeltaY = 0.0;
        angleOfRotation = 0.0;

        playService = new PlayService();
        this.playingMovieProperty = requireNonNull(playingMovieFlag);
        this.playingMovieProperty.addListener((observable, oldValue, newValue) -> {
            hideContextPopups();
            if (newValue) {
                playService.restart();
            } else {
                playService.cancel();
            }
        });

        renderService = new RenderService();

        this.zoomProperty = requireNonNull(zoomProperty);
        this.zoomProperty.set(INITIAL_ZOOM);
        this.zoomProperty.addListener((observable, oldValue, newValue) -> {
            xform.setScale(zoomProperty.get());
            repositionSprites();
            repositionNoteBillboardFronts();
        });
        xform.setScale(zoomProperty.get());

        localSearchResults = new ArrayList<>();

        requireNonNull(geneResultsUpdatedFlag).addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                updateLocalSearchResults();
                geneResultsUpdatedFlag.set(false);
            }
        });

        otherCells = new ArrayList<>();

        rotateX = new Rotate(0, X_AXIS);
        rotateY = new Rotate(0, Y_AXIS);
        rotateZ = new Rotate(0, Z_AXIS);

        // initialize
        this.rotateXAngleProperty = requireNonNull(rotateXAngleProperty);
        this.rotateXAngleProperty.set(rotateX.getAngle());
        this.rotateYAngleProperty = requireNonNull(rotateYAngleProperty);
        this.rotateYAngleProperty.set(rotateY.getAngle());
        this.rotateZAngleProperty = requireNonNull(rotateZAngleProperty);
        this.rotateZAngleProperty.set(rotateZ.getAngle());

        // add listener for control from rotationcontroller
        this.rotateXAngleProperty.addListener(getRotateXAngleListener());
        this.rotateYAngleProperty.addListener(getRotateYAngleListener());
        this.rotateZAngleProperty.addListener(getRotateZAngleListener());

        //initializeWithCannonicalOrientation();

        this.translateXProperty = requireNonNull(translateXProperty);
        this.translateXProperty.addListener(getTranslateXListener());
        this.translateXProperty.set(INITIAL_TRANSLATE_X);
        this.translateYProperty = requireNonNull(translateYProperty);
        this.translateYProperty.addListener(getTranslateYListener());
        this.translateYProperty.set(INITIAL_TRANSLATE_Y);

        quaternion = new Quaternion();

        this.colorHash = requireNonNull(colorHash);
        colorComparator = new ColorComparator();
        opacityComparator = new OpacityComparator();

        if (defaultEmbryoFlag) {
            currentSceneElementMeshes = new ArrayList<>();
            currentSceneElements = new ArrayList<>();
        }

        currentNotes = new ArrayList<>();
        currentGraphicNoteMap = new HashMap<>();
        currentNoteMeshMap = new HashMap<>();
        entitySpriteMap = new HashMap<>();
        billboardFrontEntityMap = new HashMap<>();

        allLabels = new ArrayList<>();
        currentLabels = new ArrayList<>();
        entityLabelMap = new HashMap<>();

        final EventHandler<MouseEvent> mouseHandler = this::handleMouseEvent;
        subscene.setOnMouseClicked(mouseHandler);
        subscene.setOnMouseDragged(mouseHandler);
        subscene.setOnMouseEntered(mouseHandler);
        subscene.setOnMousePressed(mouseHandler);
        subscene.setOnMouseReleased(mouseHandler);

        setNotesPane(parentPane);

        clickableMouseEnteredHandler = event -> spritesPane.setCursor(HAND);
        clickableMouseExitedHandler = event -> spritesPane.setCursor(DEFAULT);

        this.casesLists = requireNonNull(casesLists);

        movieFiles = new Vector<>();
        javaPictures = new Vector<>();
        count = -1;

        // set up the orientation indicator in bottom right corner
        double radius = 5.0;
        double height = 15.0;
        final PhongMaterial material = new PhongMaterial();
        material.setDiffuseColor(RED);
        orientationIndicator = new Cylinder(radius, height);
        orientationIndicator.getTransforms().addAll(rotateX, rotateY, rotateZ);
        orientationIndicator.setMaterial(material);

        xform.getChildren().add(createOrientationIndicator());

        this.bringUpInfoFlag = requireNonNull(bringUpInfoFlag);

        this.rebuildSubsceneFlag = requireNonNull(rebuildSubsceneFlag);
        // reset rebuild subscene flag to false because it may have been set to true by another layer's initialization
        this.rebuildSubsceneFlag.set(false);
        this.rebuildSubsceneFlag.addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                buildScene();
                rebuildSubsceneFlag.set(false);
            }
        });

        // set up the scaling value to convert from microns to pixel values, we set x,y = 1 and z = ratio of z to
        // original y note that xScale and yScale are not the same
        if (xScale != yScale) {
            System.err.println("xScale does not equal yScale - using ratio of Z to X for zScale value in pixels\n"
                    + "X, Y should be the same value");
        }
        this.xScale = 1;
        this.yScale = 1;
        this.zScale = zScale / xScale;

        this.searchField = requireNonNull(searchField);
        this.searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.isEmpty()) {
                isInSearchMode = false;
                buildScene();
            } else {
                isInSearchMode = true;
            }
        });

        requireNonNull(modelAnchorPane).setOnMouseClicked(getNoteClickHandler());

        requireNonNull(backwardButton).setOnAction(getBackwardButtonListener());
        requireNonNull(forwardButton).setOnAction(getForwardButtonListener());
        requireNonNull(zoomOutButton).setOnAction(getZoomOutButtonListener());
        requireNonNull(zoomInButton).setOnAction(getZoomInButtonListener());

        this.othersOpacityProperty = requireNonNull(othersOpacityProperty);
        requireNonNull(opacitySlider).valueProperty().addListener((observable, oldValue, newValue) -> {
            final double newRounded = round(newValue.doubleValue()) / 100.0;
            final double oldRounded = round(oldValue.doubleValue()) / 100.0;
            if (newRounded != oldRounded) {
                othersOpacityProperty.set(newRounded);
                buildScene();
            }
        });
        this.othersOpacityProperty.addListener((observable, oldValue, newValue) -> {
            final double newVal = newValue.doubleValue();
            final double oldVal = oldValue.doubleValue();
            if (newVal != oldVal && newVal >= 0 && newVal <= 1.0) {
                opacitySlider.setValue(newVal * 100);
            }
        });
        this.othersOpacityProperty.setValue(DEFAULT_OTHERS_OPACITY);

        uniformSizeCheckBox.setSelected(true);
        uniformSize = true;
        requireNonNull(uniformSizeCheckBox).selectedProperty().addListener((observable, oldValue, newValue) -> {
            uniformSize = newValue;
            buildScene();
        });

        requireNonNull(clearAllLabelsButton).setOnAction(getClearAllLabelsButtonListener());
        requireNonNull(cellNucleusCheckBox).selectedProperty().addListener(getCellNucleusTickListener());
        requireNonNull(cellBodyCheckBox).selectedProperty().addListener(getCellBodyTickListener());
        requireNonNull(multiRadioBtn).selectedProperty().addListener(getMulticellModeListener());

        this.contextMenuStage = requireNonNull(contextMenuStage);
        this.contextMenuController = requireNonNull(contextMenuController);

        requireNonNull(searchResultsUpdateService).setOnSucceeded(event -> updateLocalSearchResults());
        this.searchResultsList = requireNonNull(searchResultsList);
    }

    /**
     * Creates the orientation indicator and transforms
     * <p>
     * (for new model as of 1/5/2016)
     *
     * @return the group containing the orientation indicator texts
     */
    private Group createOrientationIndicator() {
        indicatorRotation = new Rotate();
        // top level group
        // had rotation to make it match main rotation
        final Group orientationIndicator = new Group();
        // has rotation to make it match biological orientation
        final Group middleTransformGroup = new Group();

        // set up the orientation indicator in bottom right corner
        Text t = makeNoteBillboardText("P     A");
        t.setTranslateX(-10);
        middleTransformGroup.getChildren().add(t);

        t = makeNoteBillboardText("D     V");
        t.setTranslateX(-42);
        t.setTranslateY(32);
        t.setRotate(90);
        middleTransformGroup.getChildren().add(t);

        t = makeNoteBillboardText("L    R");
        t.setTranslateX(5);
        t.setTranslateZ(10);
        t.getTransforms().add(new Rotate(90, new Point3D(0, 1, 0)));
        middleTransformGroup.getChildren().add(t);

        // rotation to match lateral orientation in image
        middleTransformGroup.getTransforms().add(new Rotate(-30, 0, 0));

        // xy relocates z shrinks apparent by moving away from camera? improves resolution?
        middleTransformGroup.getTransforms().add(new Scale(3, 3, 3));

        orientationIndicator.getTransforms().add(new Translate(270, 200, 800));
        orientationIndicator.getTransforms().addAll(rotateZ, rotateY, rotateX);
        orientationIndicator.getChildren().add(middleTransformGroup);
        middleTransformGroup.getTransforms().add(indicatorRotation);
        return orientationIndicator;
    }

    private double computeInterpolatedValue(int timevalue, double[] keyFrames, double[] keyValues) {
        if (timevalue <= keyFrames[0]) {
            return keyValues[0];
        }
        if (timevalue >= keyFrames[keyFrames.length - 1]) {
            return keyValues[keyValues.length - 1];
        }
        int i;
        for (i = 0; i < keyFrames.length; i++) {
            if (keyFrames[i] == timevalue) {
                return (keyValues[i]);
            }
            if (keyFrames[i] > timevalue) {
                break;
            }
        }
        // interpolate btw values at i and i-1
        double alpha = ((timevalue - keyFrames[i - 1]) / (keyFrames[i] - keyFrames[i - 1]));
        double value = keyValues[i] * alpha + keyValues[i - 1] * (1 - alpha);
        return value;
    }

    /**
     * Inserts a transient label into the sprites pane for the specified entity if the entity is an 'other' entity
     * that is less than 10% opaque.
     *
     * @param name
     *         the name that appears on the transient label
     * @param entity
     *         The entity that the label should appear on
     */
    private void insertTransientLabel(String name, Shape3D entity) {
        final double opacity = othersOpacityProperty.get();
        if ((entity != null && entity.getMaterial() != colorHash.getOthersMaterial(opacity))
                || opacity > 0.25) {
            if (!currentLabels.contains(name) && entity != null) {
                final Bounds b = entity.getBoundsInParent();
                if (b != null) {
                    final String funcName = getFunctionalNameByLineageName(name);
                    if (funcName != null) {
                        name = funcName;
                    }
                    transientLabelText = makeNoteSpriteText(name);
                    transientLabelText.setWrappingWidth(-1);
                    transientLabelText.setFill(web(TRANSIENT_LABEL_COLOR_HEX));
                    transientLabelText.setOnMouseEntered(Event::consume);
                    transientLabelText.setOnMouseClicked(Event::consume);
                    final Point2D p = CameraHelper.project(
                            camera,
                            new Point3D(
                                    (b.getMinX() + b.getMaxX()) / 2,
                                    (b.getMinY() + b.getMaxY()) / 2,
                                    (b.getMaxZ() + b.getMinZ()) / 2));
                    double x = p.getX();
                    double y = p.getY();
                    double vOffset = b.getHeight() / 2;
                    double hOffset = b.getWidth() / 2;
                    x += hOffset;
                    y -= (vOffset + LABEL_SPRITE_Y_OFFSET);
                    transientLabelText.getTransforms().add(new Translate(x, y));
                    spritesPane.getChildren().add(transientLabelText);
                }
            }
        }
    }

    /**
     * Removes transient label from sprites pane.
     */
    private void removeTransientLabel() {
        spritesPane.getChildren().remove(transientLabelText);
    }

    @SuppressWarnings("unchecked")
    public void handleMouseEvent(MouseEvent me) {
        final EventType<MouseEvent> type = (EventType<MouseEvent>) me.getEventType();
        if (type == MOUSE_ENTERED_TARGET
                || type == MOUSE_ENTERED
                || type == MOUSE_RELEASED
                || type == MOUSE_MOVED) {
            handleMouseReleasedOrEntered();
        } else if (type == MOUSE_CLICKED
                && me.isStillSincePress()) {
            handleMouseClicked(me);
        } else if (type == MOUSE_DRAGGED) {
            handleMouseDragged(me);
        } else if (type == MOUSE_PRESSED) {
            handleMousePressed(me);
        }
    }

    private void handleMouseDragged(final MouseEvent event) {
        hideContextPopups();

        spritesPane.setCursor(CLOSED_HAND);

        mouseOldX = mousePosX;
        mouseOldY = mousePosY;
        mouseOldZ = mousePosZ;
        mousePosX = event.getSceneX();
        mousePosY = event.getSceneY();
        mouseDeltaX = (mousePosX - mouseOldX);
        mouseDeltaY = (mousePosY - mouseOldY);

        mouseDeltaX /= 2;
        mouseDeltaY /= 2;

        angleOfRotation = rotationAngleFromMouseMovement();
        mousePosZ = computeZCoord(mousePosX, mousePosY, angleOfRotation);

        if (event.isSecondaryButtonDown() || event.isMetaDown() || event.isControlDown()) {
            final double translateX = xform.getTranslateX() - mouseDeltaX;
            final double translateY = xform.getTranslateY() - mouseDeltaY;
            xform.setTranslateX(translateX);
            xform.setTranslateY(translateY);
            translateXProperty.set(translateX);
            translateYProperty.set(translateY);
            repositionSprites();
            repositionNoteBillboardFronts();

        } else {
            if (event.isPrimaryButtonDown()) {
                // how to get Z COORDINATE??

                if (quaternion != null) {
                    // double[] vectorToOldMousePos = vectorBWPoints(newOriginX,
                    // newOriginY, newOriginZ, mouseOldX, mouseOldY, mouseOldZ);
                    // double[] vectorToNewMousePos = vectorBWPoints(newOriginX,
                    // newOriginY, newOriginZ, mousePosX, mousePosY, mousePosZ);

					/*
                     * double[] vectorToOldMousePos = vectorBWPoints(mouseOldX,
					 * mouseOldY, mouseOldZ, newOriginX, newOriginY, newOriginZ);
					 * double[] vectorToNewMousePos = vectorBWPoints(mousePosX,
					 * mousePosY, mousePosZ, newOriginX, newOriginY, newOriginZ);
					 */
                    double[] vectorToOldMousePos = vectorBWPoints(mouseOldX, mouseOldY, mouseOldZ, 0, 0, 0);
                    double[] vectorToNewMousePos = vectorBWPoints(mousePosX, mousePosY, mousePosZ, 0, 0, 0);

                    if (vectorToOldMousePos.length == 3 && vectorToNewMousePos.length == 3) {
                        // System.out.println("from origin to old mouse pos: <" +
                        // vectorToOldMousePos[0] + ", " + vectorToOldMousePos[1] +
                        // ", " + vectorToOldMousePos[2] + ">");
                        // System.out.println("from origin to old mouse pos: <" +
                        // vectorToNewMousePos[0] + ", " + vectorToNewMousePos[1] +
                        // ", " + vectorToNewMousePos[2] + ">");
                        // System.out.println(" ");

                        // compute cross product
                        double[] cross = crossProduct(vectorToNewMousePos, vectorToOldMousePos);
                        if (cross.length == 3) {
                            // System.out.println("cross product: <" + cross[0] + ",
                            // " + cross[1] + ", " + cross[2] + ">");
                            quaternion.updateOnRotate(angleOfRotation, cross[0], cross[1], cross[2]);

                            ArrayList<Double> eulerAngles = quaternion.toEulerRotation();

                            if (eulerAngles.size() == 3) {
                                // rotateX.setAngle(eulerAngles.get(2));
                                // rotateY.setAngle(eulerAngles.get(0));
                            }
                        }
                    }
                }

                double modifier = 10.0;
                double modifierFactor = 0.1;

                rotateXAngleProperty.set((
                        (rotateXAngleProperty.get() + mouseDeltaY * modifierFactor * modifier * 2.0)
                                % 360 + 540) % 360 - 180);
                rotateYAngleProperty.set((
                        (rotateYAngleProperty.get() + mouseDeltaX * modifierFactor * modifier * 2.0)
                                % 360 + 540) % 360 - 180);

                repositionSprites();
                repositionNoteBillboardFronts();
            }
        }
    }

    private void handleMouseReleasedOrEntered() {
        spritesPane.setCursor(DEFAULT);
    }

    private void handleMouseClicked(final MouseEvent event) {
        spritesPane.setCursor(HAND);

        hideContextPopups();

        final Node node = event.getPickResult().getIntersectedNode();

        // Nucleus
        if (node instanceof Sphere) {
            Sphere picked = (Sphere) node;
            selectedIndex.set(getPickedSphereIndex(picked));
            String name = normalizeName(cellNames[selectedIndex.get()]);
            selectedNameProperty.set(name);
            cellClickedProperty.set(true);

            if (event.getButton() == SECONDARY
                    || (event.getButton() == PRIMARY
                    && (event.isMetaDown() || event.isControlDown()))) {
                showContextMenu(
                        name,
                        event.getScreenX(),
                        event.getScreenY(),
                        false);
            } else if (event.getButton() == PRIMARY) {
                if (othersOpacityProperty.get() > VISIBILITY_CUTOFF) {
                    if (allLabels.contains(name)) {
                        removeLabelFor(name);
                    } else {
                        if (!allLabels.contains(name)) {
                            allLabels.add(name);
                            currentLabels.add(name);
                            final Shape3D entity = getEntityWithName(name);
                            insertLabelFor(name, entity);
                            highlightActiveCellLabel(entity);
                        }
                    }
                }
            }
        }

        // Structure
        else if (node instanceof MeshView) {
            boolean found = false;
            MeshView curr;
            for (int i = 0; i < currentSceneElementMeshes.size(); i++) {
                curr = currentSceneElementMeshes.get(i);
                if (curr.equals(node)) {
                    SceneElement clickedSceneElement = currentSceneElements.get(i);
                    String name = normalizeName(clickedSceneElement.getSceneName());
                    selectedNameProperty.set(name);
                    found = true;

                    if (event.getButton() == SECONDARY
                            || (event.getButton() == PRIMARY && (event.isMetaDown() || event.isControlDown()))) {
                        if (sceneElementsList.isStructureSceneName(name)) {
                            showContextMenu(
                                    name,
                                    event.getScreenX(),
                                    event.getScreenY(),
                                    true);
                        }
                    } else if (event.getButton() == PRIMARY) {
                        if (othersOpacityProperty.get() > VISIBILITY_CUTOFF) {
                            if (allLabels.contains(name)) {
                                removeLabelFor(name);
                            } else {
                                allLabels.add(name);
                                currentLabels.add(name);
                                final Shape3D entity = getEntityWithName(name);
                                insertLabelFor(name, entity);
                                highlightActiveCellLabel(entity);
                            }
                        }
                    }
                    break;
                }
            }

            // Note structure
            if (!found) {
                currentNoteMeshMap.keySet()
                        .stream()
                        .filter(note -> currentNoteMeshMap.get(note).equals(node))
                        .forEachOrdered(note -> selectedNameProperty.set(note.getTagName()));
            }

        } else {
            selectedIndex.set(-1);
            selectedNameProperty.set("");
        }
    }

    private double[] vectorBWPoints(double px, double py, double pz, double qx, double qy, double qz) {
        double[] vector = new double[3];
        double vx, vy, vz;
        vx = qx - px;
        vy = qy - py;
        vz = qz - pz;
        vector[0] = vx;
        vector[1] = vy;
        vector[2] = vz;
        return vector;
    }

    private double computeZCoord(double xCoord, double yCoord, double angleOfRotation) {
        // http://stackoverflow.com/questions/14954317/know-coordinate-of-z-from-xy-value-and-angle
        // --> law of cosines: https://en.wikipedia.org/wiki/Law_of_cosines
        // http://answers.ros.org/question/42803/convert-coordinates-2d-to-3d-point-theoretical-question/
        return sqrt(pow(xCoord, 2) + pow(yCoord, 2) - (2 * xCoord * yCoord * Math.cos(angleOfRotation)));
    }

    private double rotationAngleFromMouseMovement() {
        // http://math.stackexchange.com/questions/59/calculating-an-angle-from-2-points-in-space
        double rotationAngleRadians = Math.acos(
                ((mouseOldX * mousePosX) + (mouseOldY * mousePosY) + (mouseOldZ * mousePosZ))
                        / sqrt((pow(mouseOldX, 2) + pow(mouseOldY, 2) + pow(mouseOldZ, 2))
                        * (pow(mousePosX, 2) + pow(mousePosY, 2) + pow(mousePosZ, 2))));
        return rotationAngleRadians;
    }

    // http://mathworld.wolfram.com/CrossProduct.html
    private double[] crossProduct(double[] u, double[] v) {
        if (u.length != 3 || v.length != 3) {
            return null;
        }
        double[] cross = new double[3];
        cross[0] = (u[1] * v[2]) - (u[2] * v[1]);
        cross[1] = (u[2] * v[0]) - (u[0] * v[2]);
        cross[2] = (u[0] * v[1]) - (u[1] * v[0]);
        return cross;
    }

    private String normalizeName(String name) {
        if (name.contains("(")) {
            name = name.substring(0, name.indexOf("("));
        }
        return name.trim();
    }

    private void handleMousePressed(MouseEvent event) {
        mousePosX = event.getSceneX();
        mousePosY = event.getSceneY();
    }

    private void showContextMenu(
            final String name,
            final double sceneX,
            final double sceneY,
            final boolean isStructure) {

        contextMenuController.setName(name);
        contextMenuController.setColorButtonText(isStructure);

        if (getFunctionalNameByLineageName(name) == null) {
            contextMenuController.disableTerminalCaseFunctions(true);
        } else {
            contextMenuController.disableTerminalCaseFunctions(false);
        }

        contextMenuController.setColorButtonListener(event -> {
            contextMenuStage.hide();
            if (isStructure) {
                searchLayer.addStructureRuleBySceneName(name, WHITE).showEditStage(parentStage);
            } else {
                searchLayer.addColorRule(LINEAGE, name, WHITE, CELL_NUCLEUS, CELL_BODY).showEditStage(parentStage);
            }
        });

        contextMenuController.setColorNeighborsButtonListener(event -> {
            contextMenuStage.hide();
            // color neighboring cell bodies, multicellular structures, as well as nuclei
            searchLayer.addColorRule(NEIGHBOR, name, WHITE, CELL_NUCLEUS, CELL_BODY).showEditStage(parentStage);
        });

        contextMenuStage.setX(sceneX);
        contextMenuStage.setY(sceneY);

        contextMenuStage.show();
        ((Stage) contextMenuStage.getScene().getWindow()).toFront();
    }

    private void repositionNoteBillboardFronts() {
        for (Node billboard : billboardFrontEntityMap.keySet()) {
            final Node entity = billboardFrontEntityMap.get(billboard);
            if (entity != null) {
                final Bounds b = entity.getBoundsInParent();
                if (b != null) {
                    billboard.getTransforms().clear();
                    double x = b.getMaxX();
                    double y = b.getMaxY() + b.getHeight() / 2;
                    double z = b.getMaxZ();
                    billboard.getTransforms().addAll(
                            new Translate(x, y, z),
                            new Scale(BILLBOARD_SCALE, BILLBOARD_SCALE));
                }
            }
        }
    }

    /**
     * Repositions sprites (labels and note sprites) by projecting the sphere's 3D coordinate onto the front of the
     * subscene
     */
    private void repositionSprites() {
        if (entitySpriteMap != null) {
            for (Node entity : entitySpriteMap.keySet()) {
                alignTextWithEntity(entitySpriteMap.get(entity), entity, false);
            }
        }
        if (entityLabelMap != null) {
            for (Node entity : entityLabelMap.keySet()) {
                alignTextWithEntity(entityLabelMap.get(entity), entity, true);
            }
        }
    }

    /**
     * Aligns a note graphic to its entity. The graphic is either a {@link Text} or a {@link VBox}. The graphic is
     * removed if it ends up outside the bounds of the subscene window after a transformation, and only reinserted if
     * its bounds are within the window again.
     *
     * @param noteOrLabelGraphic
     *         graphical representation of a note/notes (could be a {@link Text} or a {@link VBox})
     * @param node
     *         entity that the note graphic should attach to
     * @param isLabel
     *         true if a label is being aligned, false otherwise
     */
    private void alignTextWithEntity(final Node noteOrLabelGraphic, final Node node, final boolean isLabel) {
        if (node != null) {
            final Bounds b = node.getBoundsInParent();
            if (b != null) {
                final Point2D p = CameraHelper.project(
                        camera,
                        new Point3D(
                                (b.getMinX() + b.getMaxX()) / 2.0,
                                (b.getMinY() + b.getMaxY()) / 2.0,
                                (b.getMaxZ() + b.getMinZ()) / 2.0));
                double x = p.getX();
                double y = p.getY();

                double vOffset = b.getHeight() / 2;
                double hOffset = b.getWidth() / 2;

                if (isLabel) {
                    x += hOffset;
                    y -= (vOffset + LABEL_SPRITE_Y_OFFSET);
                } else {
                    x += hOffset;
                    y += vOffset + LABEL_SPRITE_Y_OFFSET;
                }
                noteOrLabelGraphic.getTransforms().clear();
                noteOrLabelGraphic.getTransforms().add(new Translate(x, y));
            }
        }
    }

    private int getIndexByCellName(final String name) {
        for (int i = 0; i < cellNames.length; i++) {
            if (cellNames[i].equals(name)) {
                return i;
            }
        }
        return -1;
    }

    private int getPickedSphereIndex(final Sphere picked) {
        for (int i = 0; i < cellNames.length; i++) {
            if (spheres[i].equals(picked)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Calls the service to retrieve subscene data at current timeProperty point then render entities, notes, and
     * labels
     */
    private void buildScene() {
        // Spool thread for actual rendering to subscene
        renderService.restart();
    }

    private void getSceneData() {
        final int requestedTime = timeProperty.get();
        cellNames = lineageData.getNames(requestedTime);
        positions = lineageData.getPositions(requestedTime);
        diameters = lineageData.getDiameters(requestedTime);
        otherCells.clear();

        totalNucleiProperty.set(cellNames.length);

        spheres = new Sphere[cellNames.length];
        if (defaultEmbryoFlag) {
            meshes = new MeshView[meshNames.length];
        }

        if (defaultEmbryoFlag) {
            // start scene element list, find scene elements present at current time, build meshes
            // empty meshes and scene element references from last rendering
            // same for story elements
            if (sceneElementsList != null) {
                meshNames = sceneElementsList.getSceneElementNamesAtTime(requestedTime);
            }

            if (!currentSceneElementMeshes.isEmpty()) {
                currentSceneElementMeshes.clear();
                currentSceneElements.clear();
            }

            sceneElementsAtCurrentTime = sceneElementsList.getSceneElementsAtTime(requestedTime);
            for (SceneElement se : sceneElementsAtCurrentTime) {
                final MeshView mesh = se.buildGeometry(requestedTime - 1);
                if (mesh != null) {
                    mesh.getTransforms().addAll(rotateX, rotateY, rotateZ);
                    // TODO is this right?
                    mesh.getTransforms().add(new Translate(-offsetX, -offsetY, -offsetZ * zScale));
                    // add rendered mesh to meshes list
                    currentSceneElementMeshes.add(mesh);
                    // add scene element to rendered scene element reference for on-click responsiveness
                    currentSceneElements.add(se);
                }
            }
            // End scene element mesh loading/building
        }

        // Label stuff
        entityLabelMap.clear();
        currentLabels.clear();

        for (String label : allLabels) {
            if (defaultEmbryoFlag) {
                for (SceneElement currentSceneElement : currentSceneElements) {
                    if (!currentLabels.contains(label)
                            && label.equalsIgnoreCase(normalizeName(currentSceneElement.getSceneName()))) {
                        currentLabels.add(label);
                        break;
                    }
                }
            }

            for (String cell : cellNames) {
                if (!currentLabels.contains(label) && cell.equalsIgnoreCase(label)) {
                    currentLabels.add(label);
                    break;
                }
            }
        }
        // End label stuff

        // Story stuff
        // Notes are indexed starting from 1 (or 1+offset shown to user)
        if (storiesLayer != null) {
            currentNotes.clear();
            currentNoteMeshMap.clear();
            currentGraphicNoteMap.clear();
            entitySpriteMap.clear();
            billboardFrontEntityMap.clear();

            currentNotes = storiesLayer.getNotesAtTime(requestedTime);

            for (Note note : currentNotes) {
                // Revert to overlay display if we have invalid
                // display/attachment
                // type combination
                if (note.hasLocationError() || note.hasEntityNameError()) {
                    note.setTagDisplay(OVERLAY);
                }

                if (defaultEmbryoFlag) {
                    // make mesh views for scene elements from note resources
                    if (note.hasSceneElements()) {
                        for (SceneElement se : note.getSceneElements()) {
                            MeshView mesh = se.buildGeometry(requestedTime);

                            if (mesh != null) {
                                mesh.setMaterial(colorHash.getNoteSceneElementMaterial());
                                mesh.getTransforms().addAll(rotateX, rotateY, rotateZ);
                                mesh.getTransforms().add(new Translate(-offsetX, -offsetY, -offsetZ * zScale));
                                currentNoteMeshMap.put(note, mesh);
                            }
                        }
                    }
                }
            }
        }
        // End story stuff

        // SearchLayer stuff
        if (localSearchResults.isEmpty()) {
            isCellSearchedFlags = new boolean[cellNames.length];
            isMeshSearchedFlags = new boolean[meshNames.length];
        } else {
            consultSearchResultsList();
        }
        // End search stuff
    }

    private void updateLocalSearchResults() {
        if (searchResultsList == null) {
            return;
        }
        localSearchResults.clear();
        for (String name : searchResultsList) {
            if (name.contains(" (")) {
                localSearchResults.add(name.substring(0, name.indexOf(" (")).trim());
            } else {
                localSearchResults.add(name);
            }
        }
        rebuildSubsceneFlag.set(true);
    }

    private void refreshScene() {
        // clear note billboards, cell spheres and meshes
        rootEntitiesGroup.getChildren().clear();
        rootEntitiesGroup.getChildren().add(xform);

        // clear note sprites and overlays
        overlayVBox.getChildren().clear();

        final Iterator<Node> iter = spritesPane.getChildren().iterator();
        while (iter.hasNext()) {
            Node node = iter.next();
            if (node instanceof Text) {
                iter.remove();
            } else if (node instanceof VBox && node != overlayVBox) {
                iter.remove();
            }
        }

        double newrotate = computeInterpolatedValue(timeProperty.get(), keyFramesRotate, keyValuesRotate);
        indicatorRotation.setAngle(-newrotate);
        indicatorRotation.setAxis(new Point3D(1, 0, 0));
    }

    private void addEntitiesToScene() {
        final List<Shape3D> entities = new ArrayList<>();
        final List<Node> notes = new ArrayList<>();

        // add spheres
        addCellGeometries(entities);

        // add scene element meshes (from notes and from scene elements list)
        if (defaultEmbryoFlag) {
            addSceneElementGeometries(entities);
        }

        entities.sort(opacityComparator);
        rootEntitiesGroup.getChildren().addAll(entities);

        // add notes
        insertOverlayTitles();

        if (!currentNotes.isEmpty()) {
            addNoteGeometries(notes);
        }

        // add labels
        Shape3D activeEntity = null;
        for (String name : currentLabels) {
            insertLabelFor(name, getEntityWithName(name));

            if (name.equalsIgnoreCase(selectedNameProperty.get())) {
                activeEntity = getEntityWithName(name);
            }
        }
        if (activeEntity != null) {
            highlightActiveCellLabel(activeEntity);
        }

        if (!notes.isEmpty()) {
            rootEntitiesGroup.getChildren().addAll(notes);
        }

        repositionSprites();
        repositionNoteBillboardFronts();

        removeOutOfBoundsSprites();
    }

    private void removeOutOfBoundsSprites() {
        final Bounds paneBounds = spritesPane.localToScreen(spritesPane.getBoundsInLocal());
        final Iterator<Node> iter = spritesPane.getChildren().iterator();
        while (iter.hasNext()) {
            Node node = iter.next();
            if (node != subscene) {
                final Bounds spriteBounds = node.localToScreen(node.getBoundsInLocal());
                if (spriteBounds.getMinX() < paneBounds.getMinX() - 10
                        && spriteBounds.getMinY() < paneBounds.getMinY() - 10
                        && spriteBounds.getMaxX() > paneBounds.getMaxX() + 10
                        && spriteBounds.getMaxY() > paneBounds.getMaxY() + 10) {
                    iter.remove();
                }
            }
        }
    }

    private void addSceneElementGeometries(final List<Shape3D> list) {
        if (defaultEmbryoFlag) {
            // add scene elements from note resources
            list.addAll(currentNoteMeshMap.keySet()
                    .stream()
                    .map(currentNoteMeshMap::get)
                    .collect(toList()));

            // Consult rules/search results
            if (!currentSceneElements.isEmpty()) {
                SceneElement sceneElement;
                MeshView meshView;
                for (int i = 0; i < currentSceneElements.size(); i++) {
                    sceneElement = currentSceneElements.get(i);
                    meshView = currentSceneElementMeshes.get(i);

                    // in search mode
                    if (isInSearchMode) {
                        // note: in highlighting, lim4_nerve_ring is parallel with an AB
                        // lineage name in meshNames and sceneElements respectively
                        if (cellBodyTicked && isMeshSearchedFlags[i]) {
                            meshView.setMaterial(colorHash.getHighlightMaterial());
                        } else {
                            meshView.setMaterial(colorHash.getTranslucentMaterial());
                        }
                    } else {
                        // in regular viewing mode
                        final List<String> structureCells = sceneElement.getAllCells();
                        final List<Color> colors = new ArrayList<>();

                        // 12/28/2016 meshes with no cells default to others opacity here because only structure
                        // rules can color them
                        if (structureCells.isEmpty()) {

                            // check if any rules apply to this no-cell structure
                            boolean ruleApplies = false;
                            for (Rule rule : rulesList) {
                                if (rule.appliesToStructureWithSceneName(sceneElement.getSceneName())) {
                                    ruleApplies = true;
                                    colors.add(rule.getColor());
                                }
                            }

                            // if no rules for this structure, set to others opacity
                            if (!ruleApplies) {
                                meshView.setMaterial(colorHash.getOthersMaterial(othersOpacityProperty.get()));
                            }
                        } else {
                            // process rules that apply to it
                            for (Rule rule : rulesList) {
                                /*
                                 * cell nuc, cell body rules should not tag multicellular structures that contain
								 * themselves
								 * so as to avoid ambiguity. To color multicellular structures, users must add explicit
								 * structures rules
								 */
                                // this is the check for whether this is an explicit structure rule
                                if (rule.appliesToStructureWithSceneName(sceneElement.getSceneName())) {
                                    colors.add(rule.getColor());
                                }
//								else if (!(structureCells.size() > 1) && rule.appliesToCellBody(structureCells.get(0)
// )) {
//									colors.add(rule.getColor());
//								}

                                // commented out 12/28/2016 --> this condition will color a mutlicellular structure
                                // if a single cell in struct has a rule
                                else {
                                    colors.addAll(structureCells
                                            .stream()
                                            .filter(rule::appliesToCellBody)
                                            .map(name -> rule.getColor())
                                            .collect(toList()));
                                }
                            }
                        }
                        colors.sort(colorComparator);
                        // if any rules applied
                        if (!colors.isEmpty()) {
                            meshView.setMaterial(colorHash.getMaterial(colors));
                        } else {
                            meshView.setMaterial(colorHash.getOthersMaterial(othersOpacityProperty.get()));
                        }
                    }

                    final String sceneName = sceneElement.getSceneName();
                    meshView.setOnMouseEntered(event -> {
                        spritesPane.setCursor(HAND);
                        // make label appear
                        final String name = normalizeName(sceneName);
                        if (!currentLabels.contains(name.toLowerCase())) {
                            insertTransientLabel(name, getEntityWithName(name));
                        }
                    });
                    meshView.setOnMouseExited(event -> {
                        spritesPane.setCursor(DEFAULT);
                        // make label disappear
                        removeTransientLabel();
                    });
                    list.add(meshView);
                }
            }
        }
    }

    private void addCellGeometries(final List<Shape3D> list) {
        final Material othersMaterial = colorHash.getOthersMaterial(othersOpacityProperty.get());
        for (int i = 0; i < cellNames.length; i++) {
            double radius;
            if (!uniformSize) {
                radius = SIZE_SCALE * diameters[i] / 2;
            } else {
                radius = SIZE_SCALE * UNIFORM_RADIUS;
            }
            final Sphere sphere = new Sphere(radius);
            Material material;
            // if in search, do highlighting
            if (isInSearchMode) {
                if (isCellSearchedFlags[i]) {
                    material = colorHash.getHighlightMaterial();
                } else {
                    material = colorHash.getTranslucentMaterial();
                }
            } else {
                final List<Color> colors = new ArrayList<>();
                // consult active list of rules
                for (Rule rule : rulesList) {
                    if (rule.appliesToCellNucleus(cellNames[i])) {
                        colors.add(web(rule.getColor().toString()));
                    }
                }
                material = colorHash.getMaterial(colors);
                if (colors.isEmpty()) {
                    material = othersMaterial;
                }
            }
            sphere.setMaterial(material);

            sphere.getTransforms().addAll(rotateX, rotateY, rotateZ);
            sphere.getTransforms().add(new Translate(
                    positions[i][X_COR_INDEX] * xScale,
                    positions[i][Y_COR_INDEX] * yScale,
                    positions[i][Z_COR_INDEX] * zScale));

            spheres[i] = sphere;

            final int index = i;
            sphere.setOnMouseEntered(event -> {
                spritesPane.setCursor(HAND);
                // make label appear
                String name = cellNames[index];
                if (!currentLabels.contains(name.toLowerCase())) {
                    // get cell body version of sphere, if there is one
                    insertTransientLabel(name, getEntityWithName(name));
                }
            });
            sphere.setOnMouseExited(event -> {
                spritesPane.setCursor(DEFAULT);
                // make label disappear
                removeTransientLabel();
            });

            list.add(sphere);
        }
    }

    private void removeLabelFor(String name) {
        allLabels.remove(name);
        currentLabels.remove(name);
        final Node entity = getEntityWithName(name);
        if (entity != null) {
            removeLabelFrom(entity);
        }
    }

    private void removeLabelFrom(Node entity) {
        if (entity != null) {
            spritesPane.getChildren().remove(entityLabelMap.get(entity));
            entityLabelMap.remove(entity);
        }
    }

    private void insertLabelFor(final String name, final Node entity) {
        // if label is already in scene, make all labels white and highlight that one
        final Text label = entityLabelMap.get(entity);
        if (label != null) {
            for (Node shape : entityLabelMap.keySet()) {
                entityLabelMap.get(shape).setFill(web(SPRITE_COLOR_HEX));
            }
            label.setFill(web(ACTIVE_LABEL_COLOR_HEX));
            return;
        }

        // otherwise, create a highlight new label
        final String funcName = getFunctionalNameByLineageName(name);
        Text text;
        if (funcName != null) {
            text = makeNoteSpriteText(funcName);
        } else {
            text = makeNoteSpriteText(name);
        }

        final String tempName = name;
        text.setOnMouseClicked(event -> removeLabelFor(tempName));
        text.setWrappingWidth(-1);

        entityLabelMap.put(entity, text);
        spritesPane.getChildren().add(text);
        alignTextWithEntity(text, entity, true);
    }

    private void highlightActiveCellLabel(Shape3D entity) {
        for (Node shape3D : entityLabelMap.keySet()) {
            entityLabelMap.get(shape3D).setFill(web(SPRITE_COLOR_HEX));
        }

        if (entity != null && entityLabelMap.get(entity) != null) {
            entityLabelMap.get(entity).setFill(web(ACTIVE_LABEL_COLOR_HEX));
        }
    }

    /**
     * @return The {@link Shape3D} entity with input name. Priority is given to
     * meshes (if a mesh and a cell have the same name, then the mesh is
     * returned).
     */
    private Shape3D getEntityWithName(final String name) {
        if (defaultEmbryoFlag) {
            // mesh view label
            for (int i = 0; i < currentSceneElements.size(); i++) {
                if (normalizeName(currentSceneElements.get(i).getSceneName()).equalsIgnoreCase(name)
                        && currentSceneElementMeshes.get(i) != null) {
                    return currentSceneElementMeshes.get(i);
                }
            }
        }

        // sphere label
        for (int i = 0; i < cellNames.length; i++) {
            if (spheres[i] != null && cellNames[i].equalsIgnoreCase(name)) {
                return spheres[i];
            }
        }

        return null;
    }

    /**
     * Inserts note geometries into the subscene.
     *
     * @param list
     *         the list of nodes that billboards are added to, which are added to to the subscene. Note overlays and
     *         sprites are added to the pane that contains the subscene.
     */
    private void addNoteGeometries(final List<Node> list) {
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
                    // add inivisible location marker to scene at location
                    // specified by note
                    Sphere marker = createLocationMarker(note.getX(), note.getY(), note.getZ());
                    rootEntitiesGroup.getChildren().add(marker);
                    entitySpriteMap.put(marker, box);
                    // add vbox to sprites pane
                    spritesPane.getChildren().add(box);
                }

                // cell attachment
                else if (note.attachedToCell()) {
                    for (int i = 0; i < cellNames.length; i++) {
                        if (cellNames[i].equalsIgnoreCase(note.getCellName()) && spheres[i] != null) {
                            // if another note is already attached to the same
                            // sphere,
                            // create a vbox for note positioning
                            if (!entitySpriteMap.containsKey(spheres[i])) {
                                VBox box = new VBox(3);
                                box.getChildren().add(text);
                                entitySpriteMap.put(spheres[i], box);
                                spritesPane.getChildren().add(box);
                            } else {
                                entitySpriteMap.get(spheres[i]).getChildren().add(text);
                            }

                            break;
                        }
                    }
                } else if (defaultEmbryoFlag) {
                    // structure attachment
                    if (note.attachedToStructure()) {
                        for (int i = 0; i < currentSceneElements.size(); i++) {
                            if (currentSceneElements.get(i).getSceneName().equalsIgnoreCase(note.getCellName())) {
                                MeshView mesh = currentSceneElementMeshes.get(i);
                                if (!entitySpriteMap.containsKey(mesh)) {
                                    VBox box = new VBox(3);
                                    box.getChildren().add(text);
                                    entitySpriteMap.put(mesh, box);
                                    spritesPane.getChildren().add(box);
                                } else {
                                    entitySpriteMap.get(mesh).getChildren().add(text);
                                }
                            }
                        }
                    }
                }
            }

            // BILLBOARD_FRONT
            else if (note.isBillboardFront()) {
                // location attachment
                if (note.attachedToLocation()) {
                    Sphere marker = createLocationMarker(note.getX(), note.getY(), note.getZ());
                    rootEntitiesGroup.getChildren().add(marker);
                    billboardFrontEntityMap.put(text, marker);
                }
                // cell attachment
                else if (note.attachedToCell()) {
                    for (int i = 0; i < cellNames.length; i++) {
                        if (cellNames[i].equalsIgnoreCase(note.getCellName()) && spheres[i] != null) {
                            billboardFrontEntityMap.put(text, spheres[i]);
                        }
                    }
                }
                // structure attachment
                else if (defaultEmbryoFlag) {
                    if (note.attachedToStructure()) {
                        for (int i = 0; i < currentSceneElements.size(); i++) {
                            if (currentSceneElements.get(i).getSceneName().equalsIgnoreCase(note.getCellName())) {
                                billboardFrontEntityMap.put(text, currentSceneElementMeshes.get(i));
                            }
                        }
                    }
                }
            }

            // BILLBOARD
            else if (note.isBillboard()) {
                // location attachment
                if (note.attachedToLocation()) {
                    text.getTransforms().addAll(rotateX, rotateY, rotateZ);
                    text.getTransforms().addAll(
                            new Translate(note.getX(), note.getY(), note.getZ()),
                            new Scale(BILLBOARD_SCALE, BILLBOARD_SCALE));
                }
                // cell attachment
                else if (note.attachedToCell()) {
                    for (int i = 0; i < cellNames.length; i++) {
                        if (cellNames[i].equalsIgnoreCase(note.getCellName()) && spheres[i] != null) {
                            double offset = 5;
                            if (!uniformSize) {
                                offset = spheres[i].getRadius() + 2;
                            }

                            text.getTransforms().addAll(spheres[i].getTransforms());
                            text.getTransforms().addAll(
                                    new Translate(offset, offset),
                                    new Scale(BILLBOARD_SCALE, BILLBOARD_SCALE));
                        }
                    }
                } else if (defaultEmbryoFlag) {
                    // structure attachment
                    if (note.attachedToStructure()) {
                        for (int i = 0; i < currentSceneElements.size(); i++) {
                            if (currentSceneElements.get(i).getSceneName().equalsIgnoreCase(note.getCellName())) {
                                text.getTransforms().addAll(currentSceneElementMeshes.get(i).getTransforms());
                                double offset = 5;
                                text.getTransforms().addAll(
                                        new Translate(offset, offset),
                                        new Scale(BILLBOARD_SCALE, BILLBOARD_SCALE));
                            }
                        }
                    }
                }
            }

            // add graphic to appropriate place (scene, overlay box, or on top of scene)
            final Display display = note.getTagDisplay();
            if (display != null) {
                switch (display) {
                    case SPRITE:
                        break;
                    case BILLBOARD_FRONT: // fall to billboard case
                    case BILLBOARD:
                        list.add(text);
                        break;
                    case OVERLAY: // fall to default case
                    case BLANK: // fall to default case
                    default:
                        overlayVBox.getChildren().add(text);
                        break;
                }
            }
        }
    }

    private void insertOverlayTitles() {
        final Text infoPaneTitle = makeNoteOverlayText("Active Story:");
        if (storiesLayer.getActiveStory() != null) {
            overlayVBox.getChildren().addAll(
                    infoPaneTitle,
                    makeNoteOverlayText(storiesLayer.getActiveStory().getName()));
        } else {
            overlayVBox.getChildren().addAll(
                    infoPaneTitle,
                    makeNoteOverlayText("none"));
        }
    }

    private Text makeNoteOverlayText(final String title) {
        final Text text = new Text(title);
        text.setFill(web(SPRITE_COLOR_HEX));
        text.setFontSmoothingType(LCD);
        text.setWrappingWidth(overlayVBox.getWidth());
        text.setFont(getSpriteAndOverlayFont());
        return text;
    }

    private Text makeNoteSpriteText(String title) {
        Text text = makeNoteOverlayText(title);
        text.setWrappingWidth(220);
        return text;
    }

    private Text makeNoteBillboardText(String title) {
        Text text = new Text(title);
        text.setWrappingWidth(100);
        text.setFont(getBillboardFont());
        text.setSmooth(false);
        text.setStrokeWidth(2);
        text.setFontSmoothingType(LCD);
        text.setCacheHint(QUALITY);
        text.setFill(web(SPRITE_COLOR_HEX));
        return text;
    }

    private Sphere createLocationMarker(double x, double y, double z) {
        Sphere sphere = new Sphere(1);
        sphere.getTransforms().addAll(rotateX, rotateY, rotateZ);
        sphere.getTransforms().add(new Translate(x * xScale, y * yScale, z * zScale));
        // make marker transparent
        sphere.setMaterial(colorHash.getOthersMaterial(0));
        return sphere;
    }

    private Text makeNoteGraphic(Note note) {
        String title = note.getTagName();
        if (note.isExpandedInScene()) {
            title += ": " + note.getTagContents();
        } else {
            title += "\n[more...]";
        }
        Text node = null;
        if (note.getTagDisplay() != null) {
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

                case OVERLAY: // fall to default case

                case BLANK: // fall to default case

                default:
                    node = makeNoteOverlayText(title);
                    break;

            }
        }
        return node;
    }

    private void buildCamera() {
        camera = new PerspectiveCamera(true);
        xform = new Xform();
        xform.reset();

        rootEntitiesGroup.getChildren().add(xform);
        xform.getChildren().add(camera);

        camera.setNearClip(CAMERA_NEAR_CLIP);
        camera.setFarClip(CAMERA_FAR_CLIP);
        camera.setTranslateZ(CAMERA_INITIAL_DISTANCE);

        subscene.setCamera(camera);
    }

    /**
     * Consults the local search results list (containing only lineage names, no functional names) and sets the flags
     * for cell and mesh highlighting. If the sphere or mesh view should be highlighted in the current active search,
     * then the flag at its index it set to true.
     */
    private void consultSearchResultsList() {
        isCellSearchedFlags = new boolean[cellNames.length];
        if (defaultEmbryoFlag) {
            isMeshSearchedFlags = new boolean[meshNames.length];
        }
        // cells
        for (int i = 0; i < cellNames.length; i++) {
            isCellSearchedFlags[i] = localSearchResults.contains(cellNames[i]);
        }
        // meshes
        if (defaultEmbryoFlag) {
            SceneElement sceneElement;
            for (int i = 0; i < meshNames.length; i++) {
                sceneElement = sceneElementsAtCurrentTime.get(i);

                // ** NOT IN THIS VERSION
                /*
                 * commented out 12/28/2016 --> multicellular search on Find Cells tab shouldn't highlight the
				 * multicellular structures themselves
				 */
                if (sceneElement.isMulticellular()) {
                    isMeshSearchedFlags[i] = false;
                    for (String cell : sceneElement.getAllCells()) {
                        if (localSearchResults.contains(cell)) {
                            isMeshSearchedFlags[i] = true;
                            break;
                        }
                    }
                } else {
                    isMeshSearchedFlags[i] = localSearchResults.contains(meshNames[i]);
                }

				/* Find Cells search should never highlight multicellular structures --> 12/28/2016
                 * THIS CONDITION IS FOR THE VERSION WHICH DISAMBIGUATES BETWEEN SINGLE CELL AND STRUCTURE RULES
				 * SO HIGHLIGHTING BEHAVES THE SAME
				 */
//				if (sceneElement.isMulticellular()) {
//					isMeshSearchedFlags[i] = false;
//				}


				/* It probably never makes sense to include this because structures with no cells shouldn't be
                 * highlighted via a cells search but in case it's ever needed, here's the condition
				 */
//				else if (sceneElement.isNoCellStructure()) {
//					if (sceneElement.getSceneName().startsWith(searchField.getText())) {
//						isMeshSearchedFlags[i] = true;
//					}
//				}

            }
        }
    }

    public boolean captureImagesForMovie() {
        movieFiles.clear();
        count = -1;

        Stage fileChooserStage = new Stage();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Save Location");
        fileChooser.getExtensionFilters().add(new ExtensionFilter("MOV File", "*.mov"));

        File fakeFile = fileChooser.showSaveDialog(fileChooserStage);

        if (fakeFile == null) {
            System.out.println("null file");
            return false;
        }

        // save the name from the file chooser for later MOV file
        movieName = fakeFile.getName();
        moviePath = fakeFile.getAbsolutePath();

        // make a temp directory for the frames at the given save location
        String path = fakeFile.getAbsolutePath();
        if (path.lastIndexOf("/") < 0) {
            path = path.substring(0, path.lastIndexOf("\\") + 1) + "tempFrameDir";
        } else {
            path = path.substring(0, path.lastIndexOf("/") + 1) + "tempFrameDir";
        }

        frameDir = new File(path);

        try {
            frameDir.mkdir();
        } catch (SecurityException se) {
            return false;
        }

        String frameDirPath = frameDir.getAbsolutePath() + "/";

        captureVideo.set(true);
        timer = new Timer();

        timer.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        if (captureVideo.get()) {
                            runLater(() -> {
                                final WritableImage screenCapture = subscene.snapshot(new SnapshotParameters(), null);
                                try {
                                    final File file = new File(frameDirPath + "movieFrame" + count++ + ".JPEG");
                                    if (file != null) {
                                        RenderedImage renderedImage = SwingFXUtils.fromFXImage(screenCapture, null);
                                        write(renderedImage, "JPEG", file);
                                        movieFiles.addElement(file);
                                    }
                                } catch (Exception e) {
                                    System.out.println("Could not write frame of movie to file.");
                                    e.printStackTrace();
                                }
                            });

                        } else {
                            timer.cancel();
                        }
                    }
                },
                0,
                1000);
        return true;
    }

    public void convertImagesToMovie() {
        // make our files into JavaPicture
        javaPictures.clear();

        for (File movieFile : movieFiles) {
            JavaPicture jp = new JavaPicture();

            jp.loadImage(movieFile);

            javaPictures.addElement(jp);
        }

        if (javaPictures.size() > 0) {
            new JpegImagesToMovie((int) subscene.getWidth(), (int) subscene.getHeight(), 2, movieName, javaPictures);

            // move the movie to the originally specified location
            File movJustMade = new File(movieName);
            movJustMade.renameTo(new File(moviePath));

            // remove the .movtemp.jpg file
            File movtempjpg = new File(".movtemp.jpg");
            if (movtempjpg != null) {
                movtempjpg.delete();
            }
        }

        // remove all of the images in the frame directory
        if (frameDir != null && frameDir.isDirectory()) {
            File[] frames = frameDir.listFiles();
            for (File frame : frames) {
                frame.delete();
            }
            frameDir.delete();
        }

    }

    /**
     * Saves a snapshot of the screen
     */
    public void stillscreenCapture() {
        final Stage fileChooserStage = new Stage();
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Save Location");
        fileChooser.getExtensionFilters().add(new ExtensionFilter("PNG File", "*.png"));

        final WritableImage screenCapture = subscene.snapshot(new SnapshotParameters(), null);

        //write the image to a file
        try {
            final File file = fileChooser.showSaveDialog(fileChooserStage);

            if (file != null) {
                final RenderedImage renderedImage = SwingFXUtils.fromFXImage(screenCapture, null);
                write(renderedImage, "png", file);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void printCellNames() {
        for (int i = 0; i < cellNames.length; i++) {
            System.out.println(cellNames[i] + CS + spheres[i]);
        }
    }

    public void printMeshNames() {
        if (defaultEmbryoFlag) {
            for (int i = 0; i < meshNames.length; i++) {
                System.out.println(meshNames[i] + CS + meshes[i]);
            }
        }
    }

    /**
     * Sets transparent anchor pane overlay for sprite notes display
     *
     * @param parentPane
     *         The {@link AnchorPane} in which labels and sprites reside
     */
    public void setNotesPane(final AnchorPane parentPane) {
        if (parentPane != null) {
            spritesPane = parentPane;
            overlayVBox = new VBox(5);
            overlayVBox.setPrefWidth(190);
            overlayVBox.setMaxWidth(overlayVBox.getPrefWidth());
            overlayVBox.setMinWidth(overlayVBox.getPrefWidth());
            setTopAnchor(overlayVBox, 5.0);
            setRightAnchor(overlayVBox, 5.0);
            spritesPane.getChildren().add(overlayVBox);
        }
    }

    /**
     * Hides the context menu
     */
    private void hideContextPopups() {
        contextMenuStage.hide();
    }

    private ChangeListener<Number> getTranslateXListener() {
        return (observable, oldValue, newValue) -> {
            final double value = newValue.doubleValue();
            if (xform.getTranslateX() != value) {
                xform.setTranslateX(value);
            }
        };
    }

    private ChangeListener<Number> getTranslateYListener() {
        return (observable, oldValue, newValue) -> {
            final double value = newValue.doubleValue();
            if (xform.getTranslateY() != value) {
                xform.setTranslateY(value);
            }
        };
    }

    private ChangeListener<Number> getRotateXAngleListener() {
        return (observable, oldValue, newValue) -> {
            double newAngle = newValue.doubleValue();
            rotateX.setAngle(newAngle);
        };
    }

    private ChangeListener<Number> getRotateYAngleListener() {
        return (observable, oldValue, newValue) -> {
            double newAngle = newValue.doubleValue();
            rotateY.setAngle(newAngle);
        };
    }

    private ChangeListener<Number> getRotateZAngleListener() {
        return (observable, oldValue, newValue) -> {
            double newAngle = newValue.doubleValue();
            rotateZ.setAngle(newAngle);
        };
    }

    private EventHandler<ActionEvent> getZoomInButtonListener() {
        return event -> {
            hideContextPopups();
            double z = zoomProperty.get();
            /*
			 * Workaround to avoid JavaFX bug --> stop zoomProperty at 0
			 * As of July 8, 2016
			 * Noted by: Braden Katzman
			 *
			 * JavaFX has a bug when zoomProperty gets below 0. The camera flips around and faces the scene instead of
			 * passing through it
			 * The API does not recognize that the camera orientation has changed and thus the back of back face
			 * culled shapes appear, surrounded w/ artifacts.
			 */
            if (z >= 0.25) {
                z -= 0.25;
            } else if (z < 0) {
                z = 0;
            }
            zoomProperty.set(z);
        };
    }

    private EventHandler<ActionEvent> getZoomOutButtonListener() {
        return event -> {
            hideContextPopups();
            zoomProperty.set(zoomProperty.get() + 0.25);
        };
    }

    private EventHandler<ActionEvent> getBackwardButtonListener() {
        return event -> {
            hideContextPopups();
            if (!playingMovieProperty.get()) {
                timeProperty.set(timeProperty.get() - 1);
            }
        };
    }

    private EventHandler<ActionEvent> getForwardButtonListener() {
        return event -> {
            hideContextPopups();
            if (!playingMovieProperty.get()) {
                timeProperty.set(timeProperty.get() + 1);
            }
        };
    }

    private EventHandler<ActionEvent> getClearAllLabelsButtonListener() {
        return event -> {
            allLabels.clear();
            currentLabels.clear();
            buildScene();
        };
    }

    /**
     * This method returns the {@link ChangeListener} that listens for the {@link BooleanProperty} that changes when
     * 'cell nucleus' is ticked/unticked in the search tab. On change, the scene refreshes and cell bodies are
     * highlighted/unhighlighted accordingly.
     *
     * @return The listener.
     */
    private ChangeListener<Boolean> getCellNucleusTickListener() {
        return (observable, oldValue, newValue) -> {
            cellNucleusTicked = newValue;
            buildScene();
        };
    }

    /**
     * This method returns the {@link ChangeListener} that listens for the {@link BooleanProperty} that changes when
     * 'cell body' is ticked/unticked in the search tab. On change, the scene refreshes and cell bodies are
     * highlighted/unhighlighted accordingly.
     *
     * @return The listener.
     */
    private ChangeListener<Boolean> getCellBodyTickListener() {
        return (observable, oldValue, newValue) -> {
            cellBodyTicked = newValue;
            buildScene();
        };
    }

    private ChangeListener<Boolean> getMulticellModeListener() {
        return (observable, oldValue, newValue) -> {
        };
    }

    /**
     * The getter for the {@link EventHandler} for the {@link MouseEvent} that is fired upon clicking on a note. The
     * handler expands the note on click.
     *
     * @return The event handler.
     */
    private EventHandler<MouseEvent> getNoteClickHandler() {
        return event -> {
            if (event.isStillSincePress()) {
                final Node result = event.getPickResult().getIntersectedNode();
                if (result instanceof Text) {
                    final Text picked = (Text) result;
                    final Note note = currentGraphicNoteMap.get(picked);
                    if (note != null) {
                        note.setExpandedInScene(!note.isExpandedInScene());
                        if (note.isExpandedInScene()) {
                            picked.setText(note.getTagName() + ": " + note.getTagContents());
                        } else {
                            picked.setText(note.getTagName() + "\n[more...]");
                        }
                    }
                }
            }
        };
    }

    /**
     * This service spools a thread that
     * <p>
     * 1) retrieves the data for cells, cell bodies, and multicellular
     * structures for the current timeProperty
     * <p>
     * 2) clears the notes, labels, and entities in the subscene
     * <p>
     * 3) adds the current notes, labels, and entities to the subscene
     */
    private final class RenderService extends Service<Void> {
        @Override
        protected Task<Void> createTask() {
            return new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    runLater(() -> {
                        refreshScene();
                        getSceneData();
                        addEntitiesToScene();
                    });
                    return null;
                }
            };
        }
    }

    /**
     * This JavaFX {@link Service} of type Void spools a thread to play the subscene movie. It waits the timeProperty
     * in milliseconds defined in the variable WAIT_TIME_MILLI (defined in the parent class) before rendering the next
     * timeProperty frame.
     */
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
                        runLater(() -> timeProperty.set(timeProperty.get() + 1));
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

    /**
     * This class is the {@link ChangeListener} that listens changes in the
     * height or width of the modelAnchorPane in which the subscene lives. When
     * the size changes, front-facing billboards and sprites (notes and labels)
     * are repositioned to align with their appropriate positions (whether it is
     * a location to an entity).
     */
    private final class SubsceneSizeListener implements ChangeListener<Number> {
        @Override
        public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
            repositionSprites();
            repositionNoteBillboardFronts();
        }
    }

    /**
     * This class is the Comparator for Shape3Ds that compares based on opacity. This is used for z-buffering for
     * semi-opaque materials. Entities with opaque materials should be rendered last (added first to the
     * rootEntitiesGroup group.
     */
    private final class OpacityComparator implements Comparator<Shape3D> {
        @Override
        public int compare(Shape3D o1, Shape3D o2) {
            double op1 = colorHash.getMaterialOpacity(o1.getMaterial());
            double op2 = colorHash.getMaterialOpacity(o2.getMaterial());
            if (op1 < op2) {
                return 1;
            } else if (op1 > op2) {
                return -1;
            } else {
                return 0;
            }
        }
    }
}