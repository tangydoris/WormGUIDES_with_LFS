package wormguides.model;
/*
 * Reference class for Scene Elements over life of embryo
 * Data structure which contains SceneElements
 *
 * Created: 0ct. 30, 2015
 * Author: Braden Katzman
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.Vector;

public class SceneElementsList {
	private ArrayList<SceneElement> SceneElementsList;
	private final String configFile;

	//this will eventually be constructed using a .txt file that contains the Scene Element information for the embryo
	public SceneElementsList(String configFile) {
		this.SceneElementsList = new ArrayList<SceneElement>();
		this.configFile = configFile;
		buildListFromConfig();
	}
	
	private void buildListFromConfig() {
		File obj = new File(this.configFile);
		try {
			Scanner scanner = new Scanner(obj);
			if (scanner.hasNextLine()) { //headings
				@SuppressWarnings("unused")
				String titleBar = scanner.nextLine(); //skip column headings line
			}
			else {
				System.out.println("Invalid file: '" + configFile);
				scanner.close();
				return;
			}
			
			//if reached we have a valid csv file
			while(scanner.hasNextLine()) {
				String line = scanner.nextLine();
				
				String[] splits =  line.split(",", 8);
				
				//BUIILD SCENE ELEMENT
				
				//vector of cell names
				Vector<String> cellNames = new Vector<String>();
				StringTokenizer st = new StringTokenizer(splits[1]);
				while (st.hasMoreTokens()) {
					cellNames.add(st.nextToken());
				}
				
				SceneElement se = new SceneElement(splits[0], cellNames,
						splits[2], splits[3], splits[4],
						Integer.parseInt(splits[5]), Integer.parseInt(splits[6]),
						splits[7]);
				
				//add scene element to list
				SceneElementsList.add(se);
			}
			scanner.close();
		}
		catch (FileNotFoundException e) {
			System.out.println("The config file '" + configFile + "' wasn't found on the system.");
			//e.printStackTrace(); 
		}
	}

	public ArrayList<SceneElement> getSceneElementsAtTime(int time) {
		ArrayList<SceneElement> sceneElements = new ArrayList<SceneElement>();
		for (int i = 0; i < SceneElementsList.size(); i++) {
			SceneElement curr = SceneElementsList.get(i);
			if (curr.getStartTime() <= time && curr.getEndTime() >= time) {
				sceneElements.add(curr);
			}
		}
		return sceneElements;
	}
}