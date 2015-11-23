package wormguides.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;
import wormguides.ColorComparator;
import wormguides.Xform;
import wormguides.model.ColorHash;
import wormguides.model.ColorRule;
import wormguides.model.LineageData;
import wormguides.model.SceneElement;
import wormguides.model.SceneElementsList;
//import wormguides.model.StoryList;
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
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.paint.Color;
import javafx.scene.paint.Material;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;

public class Window3DSubScene{

	private LineageData data;

	private SubScene subscene;

	// transformation stuff
	private Group root;
	private PerspectiveCamera camera;
	private Xform cameraXform;
	private double mousePosX, mousePosY;
	private double mouseOldX, mouseOldY;
	private double mouseDeltaX, mouseDeltaY;
	private int newOriginX, newOriginY, newOriginZ;

	// housekeeping stuff
	private IntegerProperty time;
	private IntegerProperty totalNuclei;
	private int endTime;
	private Sphere[] cells;
	private MeshView[] meshes;
	private String[] names;
	private String[] meshNames;
	private boolean[] searchedCells;
	private boolean[] searchedMeshes;
	private Integer[][] positions;
	private Integer[] diameters;
	private DoubleProperty zoom;
	private double zScale;

	// switching timepoints stuff
	private BooleanProperty playingMovie;
	private PlayService playService;
	private RenderService renderService;

	// subscene click cell selection stuff
	private IntegerProperty selectedIndex;
	private StringProperty selectedName;
	private StringProperty selectedCellMeshName; //for our rendered cell meshes

	// searched highlighting stuff
	private boolean inSearch;
	private ObservableList<String> searchResultsList;
	private ArrayList<String> localSearchResults;

	// color rules stuff
	private ColorHash colorHash;
	private ObservableList<ColorRule> rulesList;

	private Service<Void> searchResultsUpdateService;

	// specific boolean listener for gene search results
	private BooleanProperty geneResultsUpdated;

	// opacity value for "other" cells (with no rule attached)
	private DoubleProperty othersOpacity;
	private HashMap<Double, Material> opacityMaterialHash;
	private ArrayList<String> otherCells;

	// rotation stuff
	private final Rotate rotateX;
	private final Rotate rotateY;
	private final Rotate rotateZ;

	//Scene Elements stuff
	private SceneElementsList sceneElementsList;
	ArrayList<SceneElement> sceneElementsAtTime;
	private ArrayList<MeshView> currentSceneElementMeshes;
	private ArrayList<SceneElement> renderedSceneElementRef; //reference of successfully rendered scene elements for click responsiveness

//------------------------NO STORY ELEMENTS IN LATEST DISTRIBUTION---------------	
//	//Story elements stuff
//	private StoryList storyList;
//	ArrayList<SceneElement> storyElementsAtTime;
//	private ArrayList<MeshView> currentStoryElementMeshes;
//	
//	//Billboard stuff
//	ArrayList<Text> billboardsAtTime;
//-------------------------------------------------------------------------------

	public Window3DSubScene(double width, double height, LineageData data) {
		root = new Group();
		this.data = data;

		time = new SimpleIntegerProperty(START_TIME);
		time.addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable,
					Number oldValue, Number newValue) {
				//System.out.println("time changed, building scene");
				buildScene(time.get());
			}
		});

		zoom = new SimpleDoubleProperty(1.0);
		zScale = Z_SCALE;

		cells = new Sphere[1];
		meshes = new MeshView[1];
		names = new String[1];
		meshNames = new String[1];
		positions = new Integer[1][3];
		diameters = new Integer[1];
		searchedCells = new boolean[1];
		searchedMeshes = new boolean[1];

		selectedIndex = new SimpleIntegerProperty();
		selectedIndex.set(-1);

		selectedName = new SimpleStringProperty();
		selectedName.set("");
		selectedName.addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable,
					String oldValue, String newValue) {
				int selected = getIndexByName(newValue);
				if (selected != -1)
					selectedIndex.set(selected);
			}
		});
		
		selectedCellMeshName = new SimpleStringProperty();
		selectedCellMeshName.set("");
		selectedName.addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable,
						String oldValue, String newValue) {
				//unused but required for initialization
				//bypassed use of index like above by keeping parallel scene element reference array
			}
		});

		inSearch = false;

		totalNuclei = new SimpleIntegerProperty();
		totalNuclei.set(0);

		endTime = data.getTotalTimePoints();

		createSubScene(width, height);

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
				if (newValue) {
					playService.restart();
				}
				else {
					playService.cancel();
				}
			}
		});

		renderService = new RenderService();

		zoom.addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable,
					Number oldValue, Number newValue) {
				cameraXform.setScale(zoom.get());
			}
		});

		localSearchResults = new ArrayList<String>();

		searchResultsUpdateService = null;

		geneResultsUpdated = new SimpleBooleanProperty();

		othersOpacity = new SimpleDoubleProperty(1.0);
		opacityMaterialHash = new HashMap<Double, Material>();
		opacityMaterialHash.put(othersOpacity.get(), new PhongMaterial(Color.WHITE));
		otherCells = new ArrayList<String>();

		rotateX = new Rotate(0, 0, newOriginY, newOriginZ, Rotate.X_AXIS);
		rotateY = new Rotate(0, newOriginX, 0, newOriginZ, Rotate.Y_AXIS);
		rotateZ = new Rotate(0, newOriginX, newOriginY, 0, Rotate.Z_AXIS);

		// Initialize scene element events
		String configFile = "src/wormguides/model/CellShapesConfig.csv";
		sceneElementsList = new SceneElementsList();
		sceneElementsList.buildListFromConfig();
		currentSceneElementMeshes = new ArrayList<MeshView>();
		renderedSceneElementRef = new ArrayList<SceneElement>();
		// End scene element initialization
		
//-------------------------NO STORY ELEMENTS IN LATEST DISTRIBUTION---------------------	
//		//initialize story elements
//		String configFile2 = "StoryListConfig.csv";
//		storyList = new StoryList(configFile2);
//		storyList.buildStories();
//		currentStoryElementMeshes = new ArrayList<MeshView>();
//		
//		//initialize billboards
//		billboardsAtTime = new ArrayList<Text>();
//--------------------------------------------------------------------------------------	
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

	public StringProperty getSelectedName() {
		return selectedName;
	}
	
	public StringProperty getSelectedCellMeshName() {
		return selectedCellMeshName;
	}

	public IntegerProperty getTotalNucleiProperty() {
		return totalNuclei;
	}

	public BooleanProperty getPlayingMovieProperty() {
		return playingMovie;
	}

	private SubScene createSubScene(Double width, Double height) {
		subscene = new SubScene(root, width, height, true, SceneAntialiasing.DISABLED);

		subscene.setFill(Color.web(FILL_COLOR_HEX));
		subscene.setCursor(Cursor.HAND);

		subscene.setOnMouseDragged(new EventHandler<MouseEvent>() {
			public void handle(MouseEvent me) {
				subscene.setCursor(Cursor.CLOSED_HAND);

				mouseOldX = mousePosX;
                mouseOldY = mousePosY;
                mousePosX = me.getSceneX();
                mousePosY = me.getSceneY();
                mouseDeltaX = (mousePosX - mouseOldX);
                mouseDeltaY = (mousePosY - mouseOldY);
                mouseDeltaX /= 4;
                mouseDeltaY /= 4;

				if (me.isPrimaryButtonDown()) {
					mouseDeltaX /= 2;
	                mouseDeltaY /= 2;
	                rotateAllCells(mouseDeltaX, mouseDeltaY);
				}

				else if (me.isSecondaryButtonDown()) {
					double tx = cameraXform.t.getTx()-mouseDeltaX;
					double ty = cameraXform.t.getTy()-mouseDeltaY;

					if (tx>0 && tx<450)
						cameraXform.t.setX(tx);
					if (ty>0 && ty<450)
						cameraXform.t.setY(ty);
				}
			}
		});

		subscene.setOnMouseReleased(new EventHandler<MouseEvent>() {
			public void handle(MouseEvent me) {
				subscene.setCursor(Cursor.HAND);
			}
		});

		subscene.setOnMouseClicked(new EventHandler<MouseEvent>() {
			public void handle(MouseEvent me) {
				PickResult result = me.getPickResult();
				Node node = result.getIntersectedNode();
				//System.out.println("3d coord: "+result.getIntersectedPoint());
				if (node instanceof Sphere) {
					selectedIndex.set(getPickedSphereIndex((Sphere)node));
					selectedName.set(names[selectedIndex.get()]);
				}
				else if (node instanceof MeshView) {
					for (int i = 0; i < currentSceneElementMeshes.size(); i++) {
						MeshView curr = currentSceneElementMeshes.get(i);
						if (curr.equals(node)) {
							SceneElement clickedSceneElement = renderedSceneElementRef.get(i);
							selectedCellMeshName.set(clickedSceneElement.getAllCellNames().get(0));
							selectedName.set(selectedCellMeshName.get());
						}
					}
				}
				else
					selectedIndex.set(-1);
			}
		});

		subscene.setOnMousePressed(new EventHandler<MouseEvent>() {
			public void handle(MouseEvent me) {
				mousePosX = me.getSceneX();
				mousePosY = me.getSceneY();
			}
		});

		buildCamera();

		return subscene;
	}

	private void rotateAllCells(double x, double y) {
		rotateX.setAngle(rotateX.getAngle()+y);
		rotateY.setAngle(rotateY.getAngle()-x);
	}

	private int getIndexByName(String name) {
		for (int i = 0; i < names.length; i++) {
			if (names[i].equals(name))
				return i;
		}
		return -1;
	}

	private int getPickedSphereIndex(Sphere picked) {
		for (int i = 0; i < names.length; i++) {
			if (cells[i].equals(picked)) {
				return i;
			}
		}
		return -1;
	}

	// Builds subscene for a given timepoint
	private void buildScene(int time) {
		// Frame is indexed 1 less than the time requested
		time--;

		names = data.getNames(time);
		meshNames = sceneElementsList.getSceneElementNamesAtTime(time);
		positions = data.getPositions(time);
		diameters = data.getDiameters(time);
		totalNuclei.set(names.length);
		cells = new Sphere[names.length];
		meshes = new MeshView[meshNames.length];
		
		// Start scene element list, find scene elements present at time, build and meshes
		//empty meshes and scene element references from last rendering. Same for story elements
		if (!currentSceneElementMeshes.isEmpty()) {
			currentSceneElementMeshes.clear();
			renderedSceneElementRef.clear();
		}
		
		sceneElementsAtTime = sceneElementsList.getSceneElementsAtTime(time);
		for (int i = 0; i < sceneElementsAtTime.size(); i++) {
			//add meshes from each scene element
			SceneElement curr = sceneElementsAtTime.get(i);
			MeshView mesh = curr.buildGeometry(time);
			
			if (mesh != null) { //null mesh when file not found thrown
				mesh.getTransforms().addAll(rotateX, rotateY, rotateZ);
				
				//add rendered mesh to meshes list
				currentSceneElementMeshes.add(mesh);
				
				//add scene element to rendered scene element reference for on click responsiveness
				renderedSceneElementRef.add(curr);
			}
		}	
		// End scene element mesh loading/building
		
		
//-------------------------NO STORY ELEMENTS IN LATEST DISTRIBUTION---------------------		
		//clear billboards
//		if(!billboardsAtTime.isEmpty()) {
//			billboardsAtTime.clear();
//		}
//		
//		//repeat process for story elements and billboards
//		if(!currentStoryElementMeshes.isEmpty()) {
//			currentStoryElementMeshes.clear();
//		}
//		
//		storyElementsAtTime = storyList.getSceneElementsAtTime(time);
//		for (int i = 0; i < storyElementsAtTime.size(); i++) {
//			//add meshes from each scene element in story
//			SceneElement se = storyElementsAtTime.get(i);
//			if (se.getBillboardFlag()) {
//				Text t = se.buildBillboard();
//				if (t != null) {
//					t.getTransforms().addAll(rotateX, rotateY);
//					billboardsAtTime.add(t);
//				}
//				
//			} else {
//				MeshView mesh = se.buildGeometry(time);
//				if (mesh != null) {
//					mesh.getTransforms().addAll(rotateX, rotateY);
//					currentStoryElementMeshes.add(mesh);
//				}
//			}	
//		}
//----------------------------------------------------------------------------------------
		

		if (localSearchResults.isEmpty()) {
			searchedCells = new boolean[names.length];
			searchedMeshes = new boolean[meshNames.length];
		}
		else
			consultSearchResultsList();

		renderService.restart();
	}

	private void updateLocalSearchResults() {
		if (searchResultsList==null)
			return;

		localSearchResults.clear();

		for (String name : searchResultsList) {
			if (name.indexOf("(")!=-1)
				localSearchResults.add(name.substring(0, name.indexOf(" ")));
			else
				localSearchResults.add(name);
		}

		buildScene(time.get());
	}

	private void refreshScene() {
		root.getChildren().clear();
		root.getChildren().add(cameraXform);
	}

	private void addCellsToScene() {
		ArrayList<Sphere> renderFirstSpheres = new ArrayList<Sphere>();
 		ArrayList<Sphere> renderSecondSpheres = new ArrayList<Sphere>();
 		
 		ArrayList<MeshView> renderFirstMeshes = new ArrayList<MeshView>();
 		ArrayList<MeshView> renderSecondMeshes = new ArrayList<MeshView>();
 		
 		// for sphere rendering
		for (int i = 0; i < names.length; i ++) {
			boolean isOther = true;
			double radius = SIZE_SCALE*diameters[i]/2;
			Sphere sphere = new Sphere(radius);

			Material material = new PhongMaterial();
 			// if in search, do highlighting
 			if (inSearch) {
 				if (searchedCells[i]) {
					renderFirstSpheres.add(sphere);
					material = colorHash.getHighlightMaterial();
 				}
 				else {
					renderSecondSpheres.add(sphere);
					material = colorHash.getTranslucentMaterial();
 				}
 			}
 			// not in search mode
 			else {
 				TreeSet<Color> colors = new TreeSet<Color>(new ColorComparator());
 				for (ColorRule rule : rulesList) {
 					// just need to consult rule's active list
 					if (rule.appliesTo(names[i]))
 						colors.add(rule.getColor());
 				}
 				material = colorHash.getMaterial(colors);

 				if (colors.isEmpty()) {
 					renderSecondSpheres.add(sphere);
 					material = opacityMaterialHash.get(othersOpacity.get());
 					//othersOpacity.set(1.0);
 				}
 				else {
 					renderFirstSpheres.add(sphere);
 					isOther = false;
 				}
 			}

 			sphere.setMaterial(material);
 			
 			// Mesh inherits color from attached cell
 			// consult names array and scene elements for matches
 			if (!currentSceneElementMeshes.isEmpty()) { //process only if meshes at this time point
 				for (int j = 0; j < renderedSceneElementRef.size(); j++) {
 					if (renderedSceneElementRef.get(j).getAllCellNames().contains(names[i])) {
 						//if match - set mesh color to current color of cell
 						currentSceneElementMeshes.get(j).setMaterial(material);
 						currentSceneElementMeshes.get(j).setCullFace(CullFace.NONE);
 						if (isOther)
 							renderSecondMeshes.add(currentSceneElementMeshes.get(j));
 						else
 							renderFirstMeshes.add(currentSceneElementMeshes.get(j));
	 				}
	 			}
 			}
 			// End color inheritance

 			double x = positions[i][X_COR_INDEX];
	        double y = positions[i][Y_COR_INDEX];
	        double z = positions[i][Z_COR_INDEX]*zScale;
	        sphere.getTransforms().addAll(rotateX, rotateY, rotateZ);
	        translateSphere(sphere, x, y, z);
	        
	        cells[i] = sphere;
	        
		}

		refreshScene();
		
		// render opaque entities first
		root.getChildren().addAll(renderFirstSpheres);
		root.getChildren().addAll(renderFirstMeshes);
		
		// render transparent entities last
		root.getChildren().addAll(renderSecondSpheres);
		root.getChildren().addAll(renderSecondMeshes);

//------------------------NO STORY ELEMENTS IN LATEST DISTRIBUTION----------
//		//add story scene element meshes to scene
//		if(!currentStoryElementMeshes.isEmpty()) {
//			root.getChildren().addAll(currentStoryElementMeshes);
//		}
//
//		//add billboards to scene
//		if(!billboardsAtTime.isEmpty()) {
//			root.getChildren().addAll(billboardsAtTime);
//		}
//--------------------------------------------------------------------------
	}

	private void translateSphere(Node sphere, double x, double y, double z) {
		Translate t = new Translate(x, y, z);
		sphere.getTransforms().add(t);
	}

	private void buildCamera() {
		this.camera = new PerspectiveCamera(true);
		this.cameraXform = new Xform();
		cameraXform.reset();

		root.getChildren().add(cameraXform);
		cameraXform.getChildren().add(camera);

        camera.setNearClip(CAMERA_NEAR_CLIP);
        camera.setFarClip(CAMERA_FAR_CLIP);
        camera.setTranslateZ(CAMERA_INITIAL_DISTANCE);

        cameraXform.setScaleX(X_SCALE);
        cameraXform.setScaleY(Y_SCALE);

        setNewOrigin();

        subscene.setCamera(camera);
	}

	private void setNewOrigin() {
		// Find average X Y positions of initial timepoint
		Integer[][] positions = data.getPositions(START_TIME);
		int numCells = positions.length;
		int sumX = 0;
		int sumY = 0;
		int sumZ = 0;
		for (int i = 0; i < numCells; i++) {
			sumX += positions[i][X_COR_INDEX];
			sumY += positions[i][Y_COR_INDEX];
			sumZ += positions[i][Z_COR_INDEX];
		}
		this.newOriginX = Math.round(sumX/numCells);
		this.newOriginY = Math.round(sumY/numCells);
		this.newOriginZ = (int) Math.round(Z_SCALE*sumZ/numCells);

		// Set new origin to average X Y positions
		cameraXform.setTranslate(newOriginX, newOriginY, newOriginZ);
		System.out.println("origin xyz: "+newOriginX+" "+newOriginY+" "+newOriginZ);
	}

	public void setSearchResultsList(ObservableList<String> list) {
		searchResultsList = list;
	}

	public void setResultsUpdateService(Service<Void> service) {
		searchResultsUpdateService = service;
		searchResultsUpdateService.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
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
		searchedCells = new boolean[names.length];
		searchedMeshes = new boolean[meshNames.length];
		
		// look for searched cells
		for (int i=0; i<names.length; i++) {
			if (localSearchResults.contains(names[i]))
				searchedCells[i] = true;
			else
				searchedCells[i] = false;
		}
		
		// look for searched meshes
		for (int i=0; i>meshNames.length; i++) {
			if (localSearchResults.contains(meshNames[i]))
				searchedMeshes[i] = true;
			else
				searchedMeshes[i] = false;
		}
	}

	public void printCellNames() {
		for (int i = 0; i < names.length; i++)
			System.out.println(names[i]+CS+cells[i]);
	}
	
	public void printMeshNames() {
		for (int i = 0; i < meshNames.length; i++)
			System.out.println(meshNames[i]+CS+meshes[i]);
	}

	// sets everything associated with color rules
	public void setRulesList(ObservableList<ColorRule> rulesList) {
		this.rulesList = rulesList;
		colorHash = new ColorHash(rulesList);

		this.rulesList.addListener(new ListChangeListener<ColorRule>() {
			@Override
			public void onChanged(
					ListChangeListener.Change<? extends ColorRule> change) {
				while (change.next()) {
					for (ColorRule rule : change.getAddedSubList()) {
						rule.getRuleChangedProperty().addListener(new ChangeListener<Boolean>() {
							@Override
							public void changed(
									ObservableValue<? extends Boolean> observable,
									Boolean oldValue, Boolean newValue) {
								if (newValue) {
									//System.out.println("rule changed, building scene");
									buildScene(time.get());
								}
							}
						});
					}
					buildScene(time.get());
				}
			}
		});
	}

	public ArrayList<ColorRule> getRulesList() {
		ArrayList<ColorRule> list = new ArrayList<ColorRule>();
		for (ColorRule rule : rulesList)
			list.add(rule);
		return list;
	}

	public ObservableList<ColorRule> getObservableRulesList() {
		return rulesList;
	}

	public void setRulesList(ArrayList<ColorRule> list) {
		rulesList.clear();
		rulesList.setAll(list);
	}

	public int getTime() {
		return time.get();
	}

	public void setTime(int t) {
		if (t > 0 && t < endTime)
			time.set(t);
	}
	
	public void setRotations(double rx, double ry, double rz) {
		rx = Math.toDegrees(rx);
		ry = Math.toDegrees(ry);
		rx = Math.toDegrees(rz);
		
		rotateX.setAngle(rx+180);
		rotateY.setAngle(ry);
		rotateZ.setAngle(rz);
	}

	public double getRotationX() {
		if (cells[0]!=null) {
			Transform transform = cells[0].getLocalToSceneTransform();
			double roll = Math.atan2(-transform.getMyx(), transform.getMxx());
			return roll;  
		}
		else
			return 0;
	}

	public double getRotationY() {
		if (cells[0]!=null) {
			Transform transform = cells[0].getLocalToSceneTransform();
			double pitch = Math.atan2(-transform.getMzy(), transform.getMzz());
			return pitch;
		}
		else
			return 0;
	}
	
	public double getRotationZ() {
		if (cells[0]!=null) {
			Transform transform = cells[0].getLocalToSceneTransform();
			double yaw = Math.atan2(transform.getMzx(), Math.sqrt((transform.getMzy()*transform.getMzy()
														+(transform.getMzz()*transform.getMzz()))));
			return yaw;
		}
		else
			return 0;
	}

	public double getTranslationX() {
		return cameraXform.t.getTx()-newOriginX;
	}
	
	public void setTranslationX(double tx) {
		double newTx = tx+newOriginX;
		if (newTx>0 && newTx<450)
			cameraXform.t.setX(newTx);
	}
	
	public double getTranslationY() {
		return cameraXform.t.getTy()-newOriginY;
	}
	
	public void setTranslationY(double ty) {
		double newTy = ty+newOriginY;
		if (newTy>0 && newTy<450)
			cameraXform.t.setY(newTy);
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

				if (!opacityMaterialHash.containsKey(othersOpacity.get())) {
					int darkness = (int) (Math.round(othersOpacity.get()*255));
					String colorString = "#";
					StringBuilder sb = new StringBuilder();
					sb.append(Integer.toHexString(darkness));

					if (sb.length()<2)
						sb.insert(0, "0");

					for (int i=0; i<3; i++)
						colorString += sb.toString();
					
					opacityMaterialHash.put(othersOpacity.get(), new PhongMaterial(
										Color.web(colorString, othersOpacity.get())));
				}

				buildScene(time.get());
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
					buildScene(time.get());
				}
				else
					inSearch = true;
			}
		};
	}

	public int getEndTime() {
		return endTime;
	}

	public int getStartTime() {
		return START_TIME;
	}

	public EventHandler<ActionEvent> getZoomInButtonListener() {
		return new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				double z = zoom.get();
				if (z<=5 && z>0.25)
					zoom.set(z-.25);
			}
		};
	}

	public EventHandler<ActionEvent> getZoomOutButtonListener() {
		return new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				double z = zoom.get();
				if (z<5 && z>=0.25)
					zoom.set(z+.25);
			}
		};
	}

	public EventHandler<ActionEvent> getBackwardButtonListener() {
		return new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
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
				if (!playingMovie.get()) {
					int t = time.get();
					if (t>=1 && t<getEndTime()-1)
						time.set(t+1);
				}
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
							refreshScene();
							otherCells.clear();
							addCellsToScene();
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
								if (time.get()<endTime-1)
									time.set(time.get()+1);
								else
									time.set(endTime-1);
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

	private static final String CS = ", ";

	private static final String FILL_COLOR_HEX = "#272727";

	private static final long WAIT_TIME_MILLI = 550;

	private static final double CAMERA_INITIAL_DISTANCE = -800;

    private static final double CAMERA_NEAR_CLIP = 1, // 0.01,
    							CAMERA_FAR_CLIP = 1000; // 10000;

    private static final int START_TIME = 1;

    private static final int X_COR_INDEX = 0,
    						Y_COR_INDEX = 1,
    						Z_COR_INDEX = 2;

    private static final double Z_SCALE = 5,
					    		X_SCALE = 1,
					    		Y_SCALE = 1;

    private static final double SIZE_SCALE = .9;

}
