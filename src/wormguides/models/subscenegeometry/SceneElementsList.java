/*
 * Bao Lab 2016
 */

package wormguides.models.subscenegeometry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import wormguides.MainApp;

import static java.lang.Integer.MIN_VALUE;
import static java.lang.Integer.parseInt;
import static java.util.Collections.sort;

import static wormguides.loaders.GeometryLoader.doesResourceExist;

/**
 * Record of {@link SceneElement}s over the life of the embryo
 */
public class SceneElementsList {

    private final String CELL_CONFIG_FILE_NAME = "CellShapesConfig.csv";
    private final String ASTERISK = "*";

    private final List<SceneElement> elementsList;
    private final Map<String, List<String>> nameCellsMap;
    private final Map<String, String> nameCommentsMap;

    // this will eventually be constructed using a .txt file that contains the
    // Scene Element information for the embryo
    public SceneElementsList() {
        elementsList = new ArrayList<>();
        nameCellsMap = new HashMap<>();
        nameCommentsMap = new HashMap<>();
        buildListFromConfig();
    }

    /**
     * @param name
     *         the name to check
     *
     * @return true if the name is the scene name of a structure, false otherwise
     */
    public boolean isStructureSceneName(String name) {
        name = name.trim().toLowerCase();
        for (SceneElement se : elementsList) {
            if (se.getSceneName().toLowerCase().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private void buildListFromConfig() {
        final URL url = MainApp.class.getResource("/wormguides/models/shapes_file/" + CELL_CONFIG_FILE_NAME);
        if (url != null) {
            try (InputStream stream = url.openStream()) {
                processStream(stream);
                processCells();
            } catch (IOException e) {
                System.out.println("The config file '" + CELL_CONFIG_FILE_NAME + "' wasn't found on the system.");
            }
        }
    }

    private void processStream(final InputStream stream) {
        try (final InputStreamReader streamReader = new InputStreamReader(stream);
             final BufferedReader reader = new BufferedReader(streamReader)) {

            // skip csv file heading
            reader.readLine();

            String line;
            // process each line
            while ((line = reader.readLine()) != null) {
                final String[] splits = line.split(",", 8);

                // BUIILD SCENE ELEMENT
                // vector of cell names
                final List<String> cellNames = new ArrayList<>();
                final StringTokenizer st = new StringTokenizer(splits[1]);
                while (st.hasMoreTokens()) {
                    cellNames.add(st.nextToken());
                }

                try {
                    final String resourceLocation = splits[4];
                    final int startTime = parseInt(splits[5]);
                    final int endTime = parseInt(splits[6]);
                    // check to see if resource exists in the shape files archive
                    // only create a scene element if it does
                    if (doesResourceExist(resourceLocation, startTime, endTime)) {
                        final SceneElement element = new SceneElement(
                                splits[0],
                                cellNames,
                                splits[2],
                                splits[3],
                                resourceLocation,
                                startTime,
                                endTime,
                                splits[7]);
                        addSceneElement(element);
                        if (!element.getComments().isEmpty()) {
                            nameCommentsMap.put(element.getSceneName().toLowerCase(), element.getComments());
                        }
                    }
                } catch (NumberFormatException e) {
                    System.out.println("error in reading scene element time for line " + line);
                }
            }

            reader.close();
        } catch (IOException e) {
            System.out.println("Invalid file: '" + CELL_CONFIG_FILE_NAME);
        }
    }

    /*
     * the start of our recursive algorithm to unpack entries in cell names
     * which reference other scene elements' cell list
     */
    private void processCells() {
        if (elementsList == null) {
            return;
        }

        for (SceneElement se : elementsList) {
            List<String> cells = se.getAllCells();
            for (int i = 0; i < cells.size(); i++) {
                if (cells.get(i).startsWith(ASTERISK)) {
                    se.setNewCellNames(unpackCells(cells));
                }
            }
        }
    }

    private List<String> unpackCells(final List<String> cells) {
        final List<String> unpackedCells = new ArrayList<>();
        for (String cell : cells) {
            // if cell starts with ASTERISK, recurse. else, add cell
            if (cell.startsWith(ASTERISK)) {
                // find the matching resource location
                elementsList.stream()
                        .filter(se -> se.getResourceLocation().endsWith(cell.substring(1)))
                        .forEachOrdered(se -> {
                            // recursively unpack matching location's cell list
                            unpackedCells.addAll(unpackCells(se.getAllCells()));
                        });
            } else {
                // only add cell name entry if not already added
                if (!unpackedCells.contains(cell)) {
                    unpackedCells.add(cell);
                }
            }
        }
        return unpackedCells;
    }

    /**
     * Returns the biological time (without frame offset) of the first occurrence of element with scene name, name
     */
    public int getFirstOccurrenceOf(String name) {
        int time = MIN_VALUE;
        for (SceneElement element : elementsList) {
            if (element.getSceneName().equalsIgnoreCase(name)) {
                time = element.getStartTime();
            }
        }
        return time + 1;
    }

    /**
     * Returns the biological time (without frame offset) of the last occurrence of element with scene name, name
     */
    public int getLastOccurrenceOf(String name) {
        int time = MIN_VALUE;
        for (SceneElement element : elementsList) {
            if (element.getSceneName().equalsIgnoreCase(name)) {
                time = element.getEndTime();
            }
        }
        return time + 1;
    }

    /**
     * Adds a scene element to the data store.
     *
     * @param element
     *         the scene element to add
     */
    public void addSceneElement(final SceneElement element) {
        if (element != null) {
            elementsList.add(element);
        }
    }

    public String[] getSceneElementNamesAtTime(final int time) {
        // Add lineage names of all structures at time
        final List<String> list = new ArrayList<>();
        elementsList.stream().filter(se -> se.existsAtTime(time)).forEachOrdered(se -> {
            if (se.isMulticellular()) {
                list.add(se.getSceneName());
            } else {
                list.add(se.getAllCells().get(0));
            }
        });
        return list.toArray(new String[list.size()]);
    }

    public List<SceneElement> getSceneElementsAtTime(final int time) {
        final List<SceneElement> sceneElements = elementsList.stream()
                .filter(se -> se.existsAtTime(time))
                .collect(Collectors.toCollection(ArrayList::new));
        return sceneElements;
    }

    public List<String> getAllSceneNames() {
        final Set<String> namesSet = new HashSet<>();
        elementsList.stream()
                .forEachOrdered(se -> namesSet.add(se.getSceneName()));
        final List<String> namesSorted = new ArrayList<>(namesSet);
        sort(namesSorted);
        return namesSorted;
    }

    public List<String> getAllMulticellSceneNames() {
        final Set<String> namesSet = new HashSet<>();
        elementsList.stream()
                .filter(SceneElement::isMulticellular)
                .forEachOrdered(se -> namesSet.add(se.getSceneName()));
        final List<String> namesSorted = new ArrayList<>(namesSet);
        sort(namesSorted);
        return namesSorted;
    }

    public List<SceneElement> getMulticellSceneElements() {
        final List<SceneElement> elements = new ArrayList<>();
        elementsList.stream()
                .filter(se -> se.isMulticellular() && !elements.contains(se))
                .forEachOrdered(elements::add);
        return elements;
    }

    public boolean isMulticellStructureName(String name) {
        name = name.trim();
        for (String cellName : getAllMulticellSceneNames()) {
            if (cellName.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    public String getCommentByName(final String name) {
        final String comment = nameCommentsMap.get(name.trim().toLowerCase());
        if (comment == null) {
            return "";
        }
        return comment;
    }

    public Map<String, String> getNameToCommentsMap() {
        return nameCommentsMap;
    }

    public Map<String, List<String>> getNameToCellsMap() {
        return nameCellsMap;
    }

    public List<SceneElement> getElementsList() {
        return elementsList;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Scene elements list:\n");
        for (SceneElement se : elementsList) {
            sb.append(se.getSceneName()).append("\n");
        }
        return sb.toString();
    }
}