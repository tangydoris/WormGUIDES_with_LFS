package wormguides.model;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Hashtable;

import javax.imageio.ImageIO;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.Material;
import javafx.scene.paint.PhongMaterial;

public class ColorHash extends Hashtable<ColorRule, Material> {
	
	private final Material defaultMaterial;
	
	public ColorHash() {
		super();
		
		// make default white material
		Color[] white = {Color.WHITE};
		defaultMaterial = makeMaterial(white);
		
		// Defaults upon startup
		addRule("aba", Color.RED.brighter());
		addRule("abp", Color.BLUE.brighter());
		addRule("p", Color.GREEN.brighter());
		addRule("ems", Color.YELLOW.brighter());
		//addRule("", Color.WHITE);
		
		addRule("abal", Color.GREY);
		//addRule("abal", Color.PURPLE);
		System.out.println(toString());
		//System.out.println("constructor done");
	}
	
	public void addRule(String cellName, Color color) {
		cellName = cellName.toLowerCase();
		System.out.println("adding rule for "+cellName);
		
		// Iterate through hash to see if there is already a rule
		// for cell name
		boolean found = false;
		boolean replace = false;
		ColorRule newRule = null;
		ColorRule oldRule = null;
		for (ColorRule rule : keySet()) {
			// this MAY cause some issues later on with string matching
			// just the prefixes...we will see
			if (!cellName.isEmpty() && cellName.startsWith(rule.getName())) {
				if (!found) {
					found = true;
					newRule = new ColorRule(cellName, rule.getColors());
					if (cellName.equals(rule.getName())) {
						replace = true;
						oldRule = rule;
					}
				}
				else
					newRule.addColor(rule.getColors());
			}
		}
		
		if (found) {
			if (replace)
				remove(oldRule);
			//System.out.println("make material for "+cellName);
			newRule.addColor(color);
			Material material = makeMaterial(newRule.getColors());
			put(newRule, material);
		}
		else {
			ColorRule rule = new ColorRule(cellName, color);
			//System.out.println("make material for "+cellName);
			Material material = makeMaterial(rule.getColors());
			put(rule, material);
		}
	}
	
	private Material makeMaterial(Color[] colors) {
		WritableImage wImage = new WritableImage(120, 120);
		PixelWriter writer = wImage.getPixelWriter();
		
		// for more than two colors, we want segments
		int segmentLength = (int) wImage.getHeight()/colors.length;
		Color color = Color.BLACK;
		for (int i = 0; i < colors.length; i++) {
			for (int j = i*segmentLength; j < (i+1)*segmentLength; j++) {
				for (int k = 0; k < wImage.getWidth(); k++) {
					 if (j < (i+1)*segmentLength)
						 color = colors[i];
					 //color = Color.web(UNSELECTED_COLOR_HEX, 0.5d);
					 writer.setColor(k, j, color);
				}
			}
		}
		// Rotate image so there is no wrapping error at the poles
		File file = new File("test2.png");
		RenderedImage renderedImage = SwingFXUtils.fromFXImage(wImage, null);
		try {
			ImageIO.write(
			        renderedImage, 
			        "png",
			        file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		PhongMaterial material = new PhongMaterial();
		material.setDiffuseMap(wImage);
		return material;
	}
	
	public Material getMaterial(String cellName) {
		//System.out.println("getting material for "+cellName);
		cellName = cellName.toLowerCase();
		String longestMatch = "";
		ColorRule ruleNeeded = null;
		for (ColorRule rule : keySet()) {
			// may have to change this matching later
			if (cellName.startsWith(rule.getName())) {
				String currentName = rule.getName();
				if (longestMatch.isEmpty()) {
					longestMatch = currentName;
					ruleNeeded = rule;
				}
				else {
					if (currentName.length() > longestMatch.length()) {
						ruleNeeded = rule;
						longestMatch = currentName;
					}
				}
			}
		}
		
		if (ruleNeeded != null)
			return get(ruleNeeded);
		
		return defaultMaterial;
	}
	
	public String toString() {
		String out = "";
		for (ColorRule rule : keySet()) {
			out += rule.toString()+"\n";
		}
		return out;
	}
	
}