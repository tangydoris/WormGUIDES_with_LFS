package wormguides.view;

import wormguides.model.ColorHash;
import wormguides.model.ColorRule;
import wormguides.model.LineageData;
import wormguides.model.MulticellularStructureRule;
import wormguides.model.Rule;
import wormguides.ColorComparator;

import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;
import java.lang.reflect.Field;
import javafx.util.Duration;
import java.lang.Math;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.Paint;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.transform.Scale;
import javafx.scene.layout.Pane;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;

public class SulstonTreePane extends ScrollPane {
	LineageData data;
	HashMap<String, Integer> nameXUseMap;
	HashMap<String, Integer> nameYStartUseMap;
	Set<String> hiddenNodes;
	TreeItem<String> lineageTreeRoot;

	ColorHash colorHash;
	int maxX=0; //global to class to keep track of current x layout position
	ObservableList<Rule> rules;
	Pane mainPane;
	Group zoomGroup;
	//Node content;
	Scale scaleTransform;

	int ttduration=0;

	EventHandler<MouseEvent> handler = new EventHandler<MouseEvent>() {
		@Override
		public void handle(MouseEvent event) {
			//handleMouseEvent(event);
			String sourceName =((Node)event.getSource()).getId();
			if(hiddenNodes.contains(sourceName)){
				hiddenNodes.remove(sourceName);
			}else{
				hiddenNodes.add(sourceName);
			}
			updateDrawing();
		}
	};

	public SulstonTreePane() {

	}



	public SulstonTreePane(LineageData data, TreeItem<String> lineageTreeRoot, ObservableList<Rule> rules, ColorHash colorHash) {
		super();
		this.hiddenNodes=new TreeSet<String>();
		this.rules=rules;
		this.colorHash=colorHash;
		this.lineageTreeRoot=lineageTreeRoot;

		rules.addListener(new ListChangeListener<Rule>() {
			@Override
			public void onChanged(
					ListChangeListener.Change<? extends Rule> change) {

				while (change.next()) {
					for (Rule rule : change.getAddedSubList()) {
						rule.getRuleChangedProperty().addListener(new ChangeListener<Boolean>() {
							@Override
							public void changed(ObservableValue<? extends Boolean> observable,
									Boolean oldValue, Boolean newValue) {
								if (newValue)
									updateColoring();
								//buildScene(time.get());
							}
						});
						updateColoring();
						//buildScene(time.get());
					}

					for (Rule rule : change.getRemoved()) {
						updateColoring();
						//buildScene(time.get());
					}
				}
			}
		});


		nameXUseMap= new HashMap<String, Integer>();
		nameYStartUseMap= new HashMap<String, Integer>();

		Pane canvas=new Pane();
		this.data=data;
		mainPane=canvas;

		//zooming
		scaleTransform=new Scale( 2,2,0,0);
		Group contentGroup=new Group();
		zoomGroup=new Group();
		contentGroup.getChildren().add(zoomGroup);
		zoomGroup.getChildren().add(canvas);
		zoomGroup.getTransforms().add(scaleTransform);

		//canvas.getTransforms().add(scaleTransform);

		System.out.println("in right constructor");
		//canvas.setStyle("-fx-background-color: white;");
		// Circle circle = new Circle(50,Color.BLUE);
		//circle.relocate(20, 20);
		//Rectangle rectangle = new Rectangle(100,100,Color.RED);
		//rectangle.relocate(70,70);
		//  canvas.getChildren().addAll(circle,rectangle);
		canvas.setVisible(true);
		//    this.getChildren().add(canvas);


		this.getChildren().add(contentGroup);

		//ScrollPane sp=new ScrollPane();

		//	sp.setContent(contentGroup);
		this.setPannable(true);
		//   this.getChildren().add(sp);

		addLines(lineageTreeRoot,canvas);
		// canvas.setPrefSize(maxX,400);

		//   this.setPrefSize(maxX,400);

		//add controls for zoom
		Text tp=new Text(15,20,"+");
		Text tm=new Text(30,20,"-");
		tp.setFont(new Font(25));
		tm.setFont(new Font(35));

		contentGroup.getChildren().add(tp);
		contentGroup.getChildren().add(tm);
		//this.getChildren().add(tp);
		//this.getChildren().add(tm);
		tm.setOnMousePressed(new EventHandler<MouseEvent>(){
			@Override
			public void handle(MouseEvent event) {
				scaleTransform.setX(scaleTransform.getX()*.75);
				scaleTransform.setY(scaleTransform.getY()*.75);}
		});
		tp.setOnMousePressed(new EventHandler<MouseEvent>(){
			@Override
			public void handle(MouseEvent event) {
				scaleTransform.setX(scaleTransform.getX()*1.3333);
				scaleTransform.setY(scaleTransform.getY()*1.3333);}
		});
		Pane yetanotherlevel=new Pane();
		yetanotherlevel.getChildren().add(contentGroup);
		//		  this.setContent(contentGroup);
		this.setContent(yetanotherlevel);

		bindLocation(tm, (ScrollPane)this, yetanotherlevel);
		bindLocation(tp, (ScrollPane)this, yetanotherlevel);

	}

	private void bindLocation(Text n,ScrollPane s, Pane scontent){
		n.layoutYProperty().bind(
				// to vertical scroll shift (which ranges from 0 to 1)
				s.vvalueProperty()
				// multiplied by (scrollableAreaHeight - visibleViewportHeight)
				.multiply(
						scontent.heightProperty()
						.subtract(
								new ScrollPaneViewPortHeightBinding(s))));

		n.layoutXProperty().bind(
				// to vertical scroll shift (which ranges from 0 to 1)
				s.hvalueProperty()
				// multiplied by (scrollableAreaHeight - visibleViewportHeight)
				.multiply(
						scontent.widthProperty()
						.subtract(
								new ScrollPaneViewPortWidthBinding(s))));


	}

	//lifted code to create control zoom overlays
	// we need this class because Bounds object doesn't support binding 
	private static class ScrollPaneViewPortHeightBinding extends DoubleBinding {

		private final ScrollPane root;

		public ScrollPaneViewPortHeightBinding(ScrollPane root) {
			this.root = root;
			super.bind(root.viewportBoundsProperty());
		}

		@Override
		protected double computeValue() {
			return root.getViewportBounds().getHeight();
		}
	}

	private static class ScrollPaneViewPortWidthBinding extends DoubleBinding {

		private final ScrollPane root;

		public ScrollPaneViewPortWidthBinding(ScrollPane root) {
			this.root = root;
			super.bind(root.viewportBoundsProperty());
		}

		@Override
		protected double computeValue() {
			return root.getViewportBounds().getWidth();
		}
	}

	private void updateDrawing(){
		//clear drawing
		System.out.println("update drawing");
		mainPane.getChildren().clear();
		maxX=0;
		//update drawing
		addLines(lineageTreeRoot, mainPane);
	}



	public void updateColoring() {
		//iterate over all drawn lines and recompute their color
		ObservableList<Node> contentnodes= mainPane.getChildren();
		Paint newcolors=null;
		for(Node currentnode: contentnodes){
			if (Line.class.isInstance(currentnode)){
				Line currline=(Line)currentnode;
				//Color newcolors=ColorsThatApplyToCell(currentnode.getId());
				Paint lnewcolors=PaintThatApplyToCell(currentnode.getId());
				
				//note this is relying on using last color to set colors for 
				//division lines that return null because are tagged with both
				if(!(lnewcolors==null))
					currline.setStroke(lnewcolors);
				else currline.setStroke(Color.BLACK);//first for now
				//else if (!(newcolors==null))
				//	currline.setStroke(newcolors);
				// hash method
				//Material getMaterial(TreeSet<Color> colorSet) {
			}
		}
	}

	private void addLines(TreeItem<String> lineageTreeRoot, Pane mainPane){
		int something = recursiveDraw(mainPane, 400, 10, 10, lineageTreeRoot, 10);
	}

	// abortive attempt to paint lines with stripes by recovering the material used in striping shapes
	// retrieval and coloring works but I think the texture needs to be rotated and so am holding off on figuring this out
	private  Paint PaintThatApplyToCell(String cellname) {
		//TreeSet<Color> colors = new TreeSet<Color>();
		TreeSet<Color> colors = new TreeSet<Color>(new ColorComparator());
		//iterate over rulesList
		for (Rule rule: rules) {

			if (rule instanceof MulticellularStructureRule) {
				//nothing
			}

			else if(rule instanceof ColorRule) {
				//iterate over cells and check if cells apply
				if(((ColorRule)rule).appliesToBody(cellname)) {
					colors.add(rule.getColor());
				}

			}
		}
		//translate color list to material from material cache
		if(!(colors.isEmpty())){
			PhongMaterial m=(PhongMaterial)colorHash.getMaterial(colors);
			Image i=m.getDiffuseMap();

			if (!(i==null)){
				ImagePattern ip=new ImagePattern(i,0,0,21,21,false);

				return ip;}
		}
		return null; //colors;
	}

	private Color ColorsThatApplyToCell(String cellname) {
		//TreeSet<Color> colors = new TreeSet<Color>();

		//iterate over rulesList
		for (Rule rule: rules) {

			if (rule instanceof MulticellularStructureRule) {
				//nothing
			}

			else if(rule instanceof ColorRule) {
				//iterate over cells and check if cells apply
				if(((ColorRule)rule).appliesToBody(cellname)) {
					//colors.add(rule.getColor());
					return rule.getColor();
				}

			}
		}
		return null; //colors;
	}


	//recursively draws each cell in Tree 
	//not sure what rootstart is
	// note returns the midpoint of the sublineage just drawn
	private int recursiveDraw(Pane mainPane, int h, int x, int ystart, TreeItem<String> cell, int rootStart) {
		// iCellsDrawn.add(c);

		boolean done = false;
		String cellName=cell.getValue();

		if (hiddenNodes.contains(cellName))
			done=true;

		int startTime = data.getFirstOccurrenceOf(cellName);
		int lastTime = data.getLastOccurrenceOf(cellName);
		//        if (c.iEndTime > lateTime) {
		//           done = true;
		//          lastTime = lateTime;
		//     }  
		//   dubious hard coded constants i dont understand
		//double ysc=1;
		int xsc=5;//=XScale minimal spacing between branches, inter branch gap seems to be some multiple of this?
		//int iTimeIndex=1;
		int iXmax=20; //left margin 
		int iYmin=20;

		int length = (int)((lastTime - startTime));
		//if (length<1) length=5;

		int yStartUse = (int)((startTime+iYmin)) ;
		nameYStartUseMap.put(cellName, new Integer(yStartUse));

		//compute color
		//TreeSet<Color> lcolors=
		//Color lcolor=ColorsThatApplyToCell(cellName);
		Paint lcolor=PaintThatApplyToCell(cellName);


		if (cell.isLeaf() || done) {
			if (x < iXmax) x = iXmax + xsc;
			//terminal case line drawn
			maxX=Math.max(x,maxX);


			Line lcell=new Line(x,yStartUse,x,yStartUse+length);
			// if (!lcolors.isEmpty())
			if(!(lcolor==null))
				lcell.setStroke(lcolor); //first for now
			Tooltip t = new Tooltip(cellName);
			hackTooltipStartTiming(t,ttduration);
			Tooltip.install(lcell, t);

			lcell.setId(cellName);
			lcell.setOnMousePressed(handler);
			if(done){ //this is a collapsed node not a terminal cell
				System.out.println("done rendering");
				Circle circle = new Circle(2,Color.BLACK);
				circle.relocate(x-2, yStartUse+length-2);
				t = new Tooltip("Expand "+cellName);
				hackTooltipStartTiming(t,ttduration);
				Tooltip.install(circle, t);
				circle.setId(cellName);
				mainPane.getChildren().add(circle);
				circle.setOnMousePressed(handler);
			}
			mainPane.getChildren().add(lcell);
			if (x > iXmax) iXmax = x;
			nameXUseMap.put(cellName, new Integer(x));
			//c.xUse = x;

			//fillInHash(c, cHash);
			//g.fillOval(c.xUse-2, c.yStartUse-2, 4, 4);

			return x;
		} else {

			//note left right not working here or relying on presort
			ObservableList<TreeItem<String>> childrenlist=cell.getChildren();

			TreeItem<String> cLeft = childrenlist.get(0);
			TreeItem<String> cRite = childrenlist.get(1);
			// int nl = LineageTree.getChildCount(cLeft.getValue());///2;
			// if (nl == 0) nl = 1;
			int x1 = recursiveDraw(mainPane, h, x, yStartUse + length, cLeft,  rootStart);
			nameXUseMap.put(cLeft.getValue(),new Integer(x1));
			int xx = maxX+xsc; 
			int x2 = recursiveDraw(mainPane, h, xx, yStartUse + length, cRite,  rootStart);
			nameXUseMap.put(cRite.getValue(), new Integer(x2));

			Integer leftXUse=nameXUseMap.get(cLeft.getValue());
			Integer rightXUse=nameXUseMap.get(cRite.getValue());
			Integer leftYUse=nameYStartUseMap.get(cLeft.getValue());
			Integer rightYUse=nameYStartUseMap.get(cRite.getValue());
			//division line
			//if(length>0){
				Line lcell=new Line(leftXUse.intValue(),leftYUse.intValue(),rightXUse.intValue(),leftYUse.intValue());
				if(!(lcolor==null))
					lcell.setStroke(lcolor); //first for now
				lcell.setId(cLeft.getValue()+cRite.getValue()); //name division lines with child names
				mainPane.getChildren().add(lcell);
				x = (x1 + x2)/2;
				length=leftYUse.intValue()-yStartUse;

				//nonerminal case line drawn
				lcell=new Line(x,yStartUse,x,leftYUse.intValue());
				if(!(lcolor==null))
					lcell.setStroke(lcolor); //first for now

				lcell.setOnMousePressed(handler);// handler for collapse
				lcell.setId(cellName);
				Tooltip t = new Tooltip(cellName);
				hackTooltipStartTiming(t,ttduration);
				Tooltip.install(lcell, t); 
				mainPane.getChildren().add(lcell);
		//	}
			return x;
		}
	}

	// stolen from web to hack these tooltips to come up faster
	public static void hackTooltipStartTiming(Tooltip tooltip,int duration) {
		try {
			Field fieldBehavior = tooltip.getClass().getDeclaredField("BEHAVIOR");
			fieldBehavior.setAccessible(true);
			Object objBehavior = fieldBehavior.get(tooltip);

			Field fieldTimer = objBehavior.getClass().getDeclaredField("activationTimer");
			fieldTimer.setAccessible(true);
			Timeline objTimer = (Timeline) fieldTimer.get(objBehavior);

			objTimer.getKeyFrames().clear();
			objTimer.getKeyFrames().add(new KeyFrame(new Duration(duration)));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}




	/* public final int
     NAME = 4
    ,TIME = 0
    ,PLANE = 3
    ,X = 1
    ,Y = 2
    ,DIA = 5
    ,PREV = 12 // index for this cell in the previous nuclei file
    ,START0 = 10 // y location where tree drawing starts
    ,START1 = 20 // y location where root cell is placed
    //,BORDERS = 60 // combined unused space at top and bottom (I think)
    ,BORDERS = 90 // combined unused space at top and bottom (I think)
    ,LINEWIDTH = 5
    ;

	*/
}