package wormguides.model;

import java.util.ArrayList;
import java.util.Comparator;

/*
 * All times known to data structures begin at 1
 * Frames are indexed from 0
 */

public class TableLineageData implements LineageData {

	private ArrayList<Frame> timeFrames;
	private ArrayList<String> allCellNames;
	private boolean isSulston;
	private double[] xyzScale;

	public TableLineageData() {
		this(new ArrayList<String>(), 1., 1., 1.);
	}

	public TableLineageData(ArrayList<String> allCellNames, double X_SCALE, double Y_SCALE, double Z_SCALE) {
		timeFrames = new ArrayList<Frame>();
		this.allCellNames = allCellNames;
		this.xyzScale = new double[3];
		xyzScale[0] = X_SCALE;
		xyzScale[1] = Y_SCALE;
		xyzScale[2] = Z_SCALE;
	}

	@Override
	public void shiftAllPositions(int x, int y, int z) {
		for (int i = 0; i < timeFrames.size(); i++) {
			timeFrames.get(i).shiftPositions(x, y, z);
		}
	}

	@Override
	public ArrayList<String> getAllCellNames() {
		allCellNames.sort(new Comparator<String>() {
			@Override
			public int compare(String s0, String s1) {
				return s0.compareTo(s1);
			}
		});
		return allCellNames;
	}

	@Override
	public String[] getNames(int time) {
		time--;
		if (time >= getTotalTimePoints() || time < 0)
			return new String[1];

		return timeFrames.get(time).getNames();
	}

	@Override
	public Double[][] getPositions(int time) {
		time--;
		if (time >= getTotalTimePoints() || time < 0)
			return new Double[1][3];

		return timeFrames.get(time).getPositions();
	}
	
	@Override
	public Double[] getDiameters(int time) {
		time--;
		if (time >= getTotalTimePoints() || time < 0)
			return new Double[1];

		return timeFrames.get(time).getDiameters();
	}
	
//	@Override
//	public Integer[][] getPositions(int time) {
//		time--;
//		if (time >= getTotalTimePoints() || time < 0)
//			return new Integer[1][3];
//
//		return timeFrames.get(time).getPositions();
//	}

//	@Override
//	public Integer[] getDiameters(int time) {
//		time--;
//		if (time >= getTotalTimePoints() || time < 0)
//			return new Integer[1];
//
//		return timeFrames.get(time).getDiameters();
//	}

	@Override
	public int getTotalTimePoints() {
		return timeFrames.size();
	}

	public void addFrame() {
		Frame frame = new Frame();
		timeFrames.add(frame);
	}
	
	public void addNucleus(int time, String name, double x, double y, double z, double diameter) {
		if (time <= getTotalTimePoints()) {
			int index = time - 1;
			Frame frame = timeFrames.get(index);
			frame.addName(name);
			Double[] position = new Double[] { x, y, z };
			frame.addPosition(position);
			frame.addDiameter(diameter);

			if (!allCellNames.contains(name))
				allCellNames.add(name);
		}
	}

//	public void addNucleus(int time, String name, int x, int y, int z, int diameter) {
//		if (time <= getTotalTimePoints()) {
//			int index = time - 1;
//			Frame frame = timeFrames.get(index);
//			frame.addName(name);
//			Integer[] position = new Integer[] { x, y, z };
//			frame.addPosition(position);
//			frame.addDiameter(diameter);
//
//			if (!allCellNames.contains(name))
//				allCellNames.add(name);
//		}
//	}

	@Override
	public int getFirstOccurrenceOf(String name) {
		int time = Integer.MIN_VALUE;
		name = name.trim();

		outer: for (int i = 0; i < timeFrames.size(); i++) {
			for (String cell : timeFrames.get(i).getNames()) {
				if (cell.equalsIgnoreCase(name)) {
					time = i + 1;
					break outer;
				}
			}
		}
		return time;
	}

	@Override
	public int getLastOccurrenceOf(String name) {
		name = name.trim();
		int time = getFirstOccurrenceOf(name);

		if (time >= 1) {
			outer: for (int i = time; i < timeFrames.size(); i++) {
				for (String cell : timeFrames.get(i).getNames()) {
					if (cell.equalsIgnoreCase(name))
						continue outer;
				}

				time = i - 1;
				break;
			}
		}

		return time + 1;
	}

	@Override
	public boolean isCellName(String name) {
		name = name.trim();
		for (String cell : allCellNames) {
			if (cell.equalsIgnoreCase(name))
				return true;
		}
		return false;
	}

	@Override
	public String toString() {
		String out = "";
		int totalFrames = getTotalTimePoints();
		for (int i = 0; i < totalFrames; i++)
			out += (i + 1) + Frame.NEWLINE + timeFrames.get(i).toString() + Frame.NEWLINE;

		return out;
	}
	
	@Override
	public boolean isSulstonMode() {
		return isSulston; //default embryo
	}
	
	@Override
	public void setIsSulstonModeFlag(boolean isSulston) {
		this.isSulston = isSulston;
	}

	public class Frame {
		private ArrayList<String> names;
//		private ArrayList<Integer[]> positions;
		private ArrayList<Double[]> positions;
//		private ArrayList<Integer> diameters;
		private ArrayList<Double> diameters;

		private String[] namesArray;
//		private Integer[][] positionsArray;
//		private Integer[] diametersArray;
		private Double[][] positionsArray;
		private Double[] diametersArray;

		private Frame() {
			names = new ArrayList<String>();
//			positions = new ArrayList<Integer[]>();
			positions = new ArrayList<Double[]>();
//			diameters = new ArrayList<Integer>();
			diameters = new ArrayList<Double>();
		}

		/**
		 * Shifts all the positions in this Frame by a specified x-, y- and
		 * z-offset.
		 * 
		 * @param x
		 *            Amount of offset the x-coordinates by
		 * @param y
		 *            Amount of offset the y-coordinates by
		 * @param z
		 *            Amount of offset the z-coordinates by
		 */
		private void shiftPositions(int x, int y, int z) {
			for (int i = 0; i < positions.size(); i++) {
//				Integer[] pos = positions.get(i);
				Double[] pos = positions.get(i);
//				positions.set(i, new Integer[] { pos[0] - x, pos[1] - y, pos[2] - z });
				positions.set(i, new Double[] { pos[0] - x, pos[1] - y, pos[2] - z });
			}
		}

		private void addName(String name) {
			names.add(name);
		}

		private void addPosition(Double[] position) {
			positions.add(position);
		}
		
		private void addDiameter(Double diameter) {
			diameters.add(diameter);
		}
		
//		private void addPosition(Integer[] position) {
//			positions.add(position);
//		}

//		private void addDiameter(Integer diameter) {
//			diameters.add(diameter);
//		}

		private String[] getNames() {
			if (namesArray == null)
				namesArray = names.toArray(new String[names.size()]);
			return namesArray;
		}

//		private Integer[][] getPositions() {
//			positionsArray = positions.toArray(new Integer[positions.size()][3]);
//			return positionsArray;
//		}
		
		private Double[][] getPositions() {
			positionsArray = positions.toArray(new Double[positions.size()][3]);
			return positionsArray;
		}

		private Double[] getDiameters() {
			if (diametersArray == null)
				diametersArray = diameters.toArray(new Double[diameters.size()]);
			return diametersArray;
		}
		
//		private Integer[] getDiameters() {
//			if (diametersArray == null)
//				diametersArray = diameters.toArray(new Integer[diameters.size()]);
//			return diametersArray;
//		}

		@Override
		public String toString() {
			String out = "";
			String[] names = getNames();
			for (int i = 0; i < names.length; i++) {
				out += names[i] + NEWLINE;
			}
			return out;
		}

		private final static String NEWLINE = "\n";
	}

	@Override
	public double[] getXYZScale() {
		return this.xyzScale;
	}

}
