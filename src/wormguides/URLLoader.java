package wormguides;

import java.util.ArrayList;

import javafx.collections.ObservableList;
import javafx.scene.paint.Color;
import wormguides.model.ColorRule;
import wormguides.view.Window3DSubScene;

public class URLLoader {
	
	private Window3DSubScene window3D;
	private ObservableList<ColorRule> rulesList;
	
	public URLLoader(Window3DSubScene window3D) {
		this.window3D = window3D;
		rulesList = window3D.getObservableColorRulesList();
	}
	
	public void parseURL(String url) {
		// TODO add error message
		if (window3D==null)
			return;
		
		if (!url.contains("testurlscript?/"))
			return;
		
		String[] args = url.split("/");
		ArrayList<String> ruleArgs = new ArrayList<String>();
		ArrayList<String> viewArgs = new ArrayList<String>();
		
		// add rules and view parameters to their ArrayList's
		int i = 0;
		while (i < args.length) {
			if (args[i].equals("set")) {
				// do not need the 'set' String
				i++;
				// iterate through set parameters until we hit the view parameters
				while (!args[i].equals("view")) {
					ruleArgs.add(args[i].trim());
					i++;
				}
				
				// iterate through view parameters
				// do not need the 'view' String
				i++;
				while (!args[i].equals("iOS") && !args[i].equals("Android") 
											&& !args[i].equals("browser")) {
					viewArgs.add(args[i]);
					i++;
				}
			}
			else
				i++;
		}
		
		// process arguments
		parseRules(ruleArgs);
		parseViewArgs(viewArgs);
	}
	
	private void parseRules(ArrayList<String> rules) {
		rulesList.clear();
		for (String rule : rules) {
			ArrayList<String> types = new ArrayList<String>();
			StringBuilder sb = new StringBuilder(rule);
			boolean noTypeSpecified = true;
			
			try {
				if (sb.indexOf("-s") > -1) {
					noTypeSpecified = false;
					types.add("-s");
					int i = sb.indexOf("-s");
					sb.replace(i, i+2, "");
				}
				if (sb.indexOf("-n") > -1) {
					noTypeSpecified = false;
					types.add("-n");
					int i = sb.indexOf("-n");
					sb.replace(i, i+2, "");
				}
				if (sb.indexOf("-d") > -1) {
					noTypeSpecified = false;
					types.add("-d");
					int i = sb.indexOf("-d");
					sb.replace(i, i+2, "");
				}
				if (sb.indexOf("-g") > -1) {
					noTypeSpecified = false;
					types.add("-g");
					int i = sb.indexOf("-g");
					sb.replace(i, i+2, "");
				}
				
				String colorString = sb.substring(sb.indexOf("+")+6, sb.length());
				
				ArrayList<SearchOption> options = new ArrayList<SearchOption>();
				if (sb.indexOf("%3C") > -1) {
					options.add(SearchOption.ANCESTOR);
					int i = sb.indexOf("%3C");
					sb.replace(i, i+3, "");
				}
				if (sb.indexOf("$") > -1) {
					options.add(SearchOption.CELL);
					options.add(SearchOption.CELLBODY);
					int i = sb.indexOf("$");
					sb.replace(i, i+1, "");
				}
				if (rule.contains("%3E")) {
					options.add(SearchOption.DESCENDANT);
					int i = sb.indexOf("%3E");
					sb.replace(i, i+3, "");
				}
				
				// extract name from what's left of rule
				String name = sb.substring(0, sb.indexOf("+"));
				
				if (types.contains("-s"))
					Search.addColorRule(SearchType.SYSTEMATIC, name, Color.web(colorString), options);
				
				if (types.contains("-n"))
					Search.addColorRule(SearchType.FUNCTIONAL, name, Color.web(colorString), options);
				
				if (types.contains("-d"))
					Search.addColorRule(SearchType.DESCRIPTION, name, Color.web(colorString), options);
				
				if (types.contains("-g"))
					Search.addColorRule(SearchType.GENE, name, Color.web(colorString), options);
				
				// if no type present, default is systematic
				if (noTypeSpecified) {
					SearchType type = SearchType.SYSTEMATIC;
					if (isGeneFormat(name))
						type = SearchType.GENE;
					Search.addColorRule(type, name, Color.web(colorString), options);
				}
			}
			catch (StringIndexOutOfBoundsException e) {
				System.out.println("invalid color rule format");
				e.printStackTrace();
			}
		}
	}
	
	private boolean isGeneFormat(String name) {
		if (name.indexOf("-") < 0)
			return false;
		
		String[] tokens = name.split("-");
		if (tokens.length != 2)
			return false;
		try {
			Integer.parseInt(tokens[1]);
		} catch (NumberFormatException nfe) {
			return false;
		}
		
		return true;
	}
	
	private void parseViewArgs(ArrayList<String> viewArgs) {
		// manipulate viewArgs arraylist so that rx ry and rz are grouped together
		// to facilitate loading rotations in x and y
		for (int i = 0; i < viewArgs.size(); i++) {
			if (viewArgs.get(i).startsWith("rX")) {
				String ry = viewArgs.get(i+1);
				String rz = viewArgs.get(i+2);
				viewArgs.set(i, viewArgs.get(i)+","+ry+","+rz);
				break;
			}
		}
		
		for (String arg : viewArgs) {
			if (arg.startsWith("rX")) {
				String[] tokens = arg.split(",");
				try {
					double rx = Double.parseDouble(tokens[0].split("=")[1]);
					double ry = Double.parseDouble(tokens[1].split("=")[1]);
					double rz = Double.parseDouble(tokens[2].split("=")[1]);
					window3D.setRotations(rx, ry, rz);
				} catch (NumberFormatException nfe) {
					System.out.println("error in parsing time variable");
					nfe.printStackTrace();
				}
				
				continue;
			}
			String[] tokens = arg.split("=");
			if (tokens.length!=0) {
				switch (tokens[0]) {
					case "time":	try {
										window3D.setTime(Integer.parseInt(tokens[1])+1);
									} catch (NumberFormatException nfe) {
										System.out.println("error in parsing time variable");
										nfe.printStackTrace();
									}
									break;
									
					case "tX":		try {
										window3D.setTranslationX(Double.parseDouble(tokens[1]));
									} catch (NumberFormatException nfe) {
										System.out.println("error in parsing translation variable");
										nfe.printStackTrace();
									}
									break;
					
					case "tY":		try {
										window3D.setTranslationY(Double.parseDouble(tokens[1]));
									} catch (NumberFormatException nfe) {
										System.out.println("error in parsing translation variable");
										nfe.printStackTrace();
									}
									break;
					
					case "scale":	try {
										window3D.setScale(Double.parseDouble(tokens[1]));
									} catch (NumberFormatException nfe) {
										System.out.println("error in parsing scale variable");
										nfe.printStackTrace();
									}
									break;
					
					case "dim":		try {
										window3D.setOthersVisibility(Double.parseDouble(tokens[1]));
									} catch (NumberFormatException nfe) {
										System.out.println("error in parsing dim variable");
										nfe.printStackTrace();
									}
									break;
				}
			}
		}
	}
}
