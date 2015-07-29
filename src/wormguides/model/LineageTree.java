package wormguides.model;

import java.util.ArrayList;
import java.util.Arrays;
import javafx.scene.control.TreeItem;

public class LineageTree {
	
	private ArrayList<String> treeBaseNames;
	
	private String[] allCellNames;
	private TreeItem<String> root;
	private TreeItem<String> ab;
	private TreeItem<String> ms;
	private TreeItem<String> e;
	private TreeItem<String> c;
	private TreeItem<String> d;
	
	@SuppressWarnings("unchecked")
	public LineageTree(String[] allCellNames) {
		this.allCellNames = allCellNames;
		
		// names of the cell added to tree upon initialization
		String[] baseNames = {"p0", "ab", "aba", "abal", "abar",
								"abp", "abpl", "abpr", "p1",
								"ems", "ms", "e", "p2", "c",
								"p3", "d", "p4", "z2", "z3"};
		treeBaseNames = new ArrayList<String>(Arrays.asList(baseNames));
		
		// add zygote (beginning lineage)
		root = new TreeItem<String>("P0");
		
		// first layer
		ab = makeTreeItem("AB");
		TreeItem<String> p1 = makeTreeItem("P1");
		root.getChildren().addAll(ab, p1);
		
		// second layer
		TreeItem<String> aba = makeTreeItem("ABa");
		TreeItem<String> abp = makeTreeItem("ABp");
		ab.getChildren().addAll(aba, abp);
		
		TreeItem<String> ems = makeTreeItem("EMS");
		TreeItem<String> p2 = makeTreeItem("P2");
		p1.getChildren().addAll(ems, p2);
		
		// third layer
		TreeItem<String> abal = makeTreeItem("ABal");
		TreeItem<String> abar = makeTreeItem("ABar");
		aba.getChildren().addAll(abal, abar);
		
		TreeItem<String> abpl = makeTreeItem("ABpl");
		TreeItem<String> abpr = makeTreeItem("ABpr");
		abp.getChildren().addAll(abpl, abpr);
		
		TreeItem<String> ms = makeTreeItem("MS");
		e = makeTreeItem("E");
		ems.getChildren().addAll(ms, e);
		
		c = makeTreeItem("C");
		TreeItem<String> p3 = makeTreeItem("P3");
		p2.getChildren().addAll(c, p3);
		
		// fourth layer (rightmost branch)
		d = makeTreeItem("D");
		TreeItem<String> p4 = makeTreeItem("P4");
		p3.getChildren().addAll(d, p4);
		
		// firth layer (rightmost branch)
		TreeItem<String> z2 = makeTreeItem("Z2");
		TreeItem<String> z3 = makeTreeItem("Z3");
		p4.getChildren().addAll(z2, z3);
		
		addAllCells();
	}
	
	private void addAllCells() {
		for (String name : allCellNames) {
			if (treeBaseNames.contains(name.toLowerCase()))
				continue;
			if (name.toLowerCase().startsWith("nuc"))
				continue;
			addCell(name);
		}
	}
	
	private void addCell(String newName) {
		String startingLetter = newName.substring(0,1).toLowerCase();
		TreeItem<String> startingNode = null;
		TreeItem<String> parent = null;
		switch (startingLetter) {
			case "a":	startingNode = ab;
						break;
			case "m":	startingNode = ms;
						break;
			case "e":	startingNode = e;
						break;
			case "c":	startingNode = c;
						break;
			case "d":	startingNode = d;
						break;
		}
		if (startingNode != null) {
			parent = addCellHelper(newName, startingNode);
			parent.getChildren().add(makeTreeItem(newName));
		}
	}
	
	// returns prent node of cell to be added (specified
	// by newName)
	private TreeItem<String> addCellHelper(String newName, TreeItem<String> node) {
		String currName = node.getValue().toLowerCase();
		newName = newName.toLowerCase();
		
		if (newName.length() == currName.length()+1 && newName.startsWith(currName))
			return node;
		
		for (TreeItem<String> child : node.getChildren()) {
			String childName = child.getValue().toLowerCase();
			if (newName.startsWith(childName))
				return addCellHelper(newName, child);
		}
		
		return null;
	}
	
	private TreeItem<String> makeTreeItem(String name) {
		return new TreeItem<String>(name);
	}
	
	public isDescendant(String desc, String ances) {
		
	}
	
	public TreeItem<String> getRoot() {
		return root;
	}
	
}
