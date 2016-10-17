/*
 * Bao Lab 2016
 */

package connectome;

import java.util.ArrayList;
import java.util.List;

import static connectome.ConnectomeLoader.loadConnectome;
import static partslist.PartsList.getFunctionalNameByLineageName;
import static partslist.PartsList.getLineageNameByFunctionalName;
import static partslist.PartsList.isFunctionalName;
import static partslist.PartsList.isLineageName;

/**
 * Underlying model of the all neuronal connections. It holds a list of {@link NeuronalSynapse}s that define the
 * wiring between two terminal cells
 */
public class Connectome {

    // synapse types as strings for search logic
    private final static String s_presynapticDescription = "S presynaptic";
    private final static String r_postsynapticDescription = "R postsynaptic";
    private final static String ej_electricalDescription = "EJ electrical";
    private final static String nmj_neuromuscularDescrpition = "Nmj neuromuscular";

    private List<NeuronalSynapse> synapses;

    public Connectome() {
        synapses = loadConnectome();
    }

    public List<NeuronalSynapse> getConnectomeList() {
        return synapses;
    }

    public List<String> getAllConnectomeCellNames() {
        // iterate through synapses arraylist and add all cell names
        List<String> allConnectomeCellNames = new ArrayList<>();
        for (NeuronalSynapse ns : synapses) {
            allConnectomeCellNames.add(getLineageNameByFunctionalName(ns.getCell1()));
            allConnectomeCellNames.add(getLineageNameByFunctionalName(ns.getCell2()));
        }
        return allConnectomeCellNames;
    }

    public List<String> getConnectedCells(String centralCell) {
        // find all cells that are connected to the central cell
        List<String> connectedCells = new ArrayList<>();
        for (NeuronalSynapse ns : synapses) {
            if (ns.getCell1().equals(centralCell)) {
                connectedCells.add(ns.getCell2());
            } else if (ns.getCell2().equals(centralCell)) {
                connectedCells.add(ns.getCell1());
            }
        }
        //make sure self isn't in list
        if (connectedCells.contains(centralCell)) {
            connectedCells.remove(centralCell);
        }
        return connectedCells;
    }

    /**
     * Provides name translation from systematic to functional
     *
     * @param queryCell
     *         the cell to be checked
     *
     * @return the resultant translated or untranslated cell name
     */
    public String checkQueryCell(String queryCell) {
        if (isLineageName(queryCell)) {
            queryCell = getFunctionalNameByLineageName(queryCell).toLowerCase();
        }
        return queryCell;
    }

    /**
     * @param queryCell
     *         the cell to query in the synapses
     *
     * @return boolean corresponding to whether the query is in the synapses
     * or not
     */
    public boolean containsCell(String queryCell) {
        queryCell = checkQueryCell(queryCell);
        for (NeuronalSynapse ns : synapses) {
            if (ns.getCell1().toLowerCase().equals(queryCell)
                    || ns.getCell2().toLowerCase().equals(queryCell)) {
                return true;
            }
        }
        return false;
    }
    
    public List<NeuronalSynapse> getSynapseList() {
    	return this.synapses;
    }

    /**
     * Search function which takes cell and filters results based on filter toggles filter toggles = 4 SynapseTypes
     *
     * @param queryCell
     *         lineage name of the cell to be searched for
     * @param isPresynapticTicked
     *         true if the presynaptic search box is ticked, false otherwise
     * @param isPostsynapticTicked
     *         true if the postsynaptic search box is ticked, false otherwise
     * @param isElectricalTicked
     *         true if the electrical search box is ticked, false otherwise
     * @param isNeuromuscularTicked
     *         true if the neuromuscular search box is ticked, false otherwise
     * @param areLineageNamesReturned
     *         true if lineage names should be returned, false otherwise
     *
     * @return the list of connections to the query cell
     */
    public List<String> queryConnectivity(
            String queryCell,
            boolean isPresynapticTicked,
            boolean isPostsynapticTicked,
            boolean isElectricalTicked,
            boolean isNeuromuscularTicked,
            boolean areLineageNamesReturned) {

        // query only works for lineage names
        if (isFunctionalName(queryCell)) {
            queryCell = getLineageNameByFunctionalName(queryCell);
        }

        queryCell = checkQueryCell(queryCell);

        List<String> searchResults = new ArrayList<>();

        // error check
        if (queryCell == null) {
            return searchResults;
        }

        // //iterate over synapses
        for (NeuronalSynapse ns : synapses) {
            // check if synapse contains query cell
            if (ns.getCell1().toLowerCase().contains(queryCell)
                    || ns.getCell2().toLowerCase().contains(queryCell)) {

                String cell1 = ns.getCell1();
                String cell2 = ns.getCell2();

                // process type code
                String synapseTypeDescription = ns.getSynapseType().getDescription();

                // find synapse type code for connection, compare to toggle
                // ticks
                switch (synapseTypeDescription) {
                    case s_presynapticDescription:
                        if (isPresynapticTicked) {
                            // don't add duplicates
                            if (!searchResults.contains(cell1)) {
                                searchResults.add(cell1);
                            }

                            if (!searchResults.contains(cell2)) {
                                searchResults.add(cell2);
                            }
                        }
                        break;
                    case r_postsynapticDescription:
                        if (isPostsynapticTicked) {
                            // don't add duplicates
                            if (!searchResults.contains(cell1)) {
                                searchResults.add(cell1);
                            }

                            if (!searchResults.contains(cell2)) {
                                searchResults.add(cell2);
                            }
                        }
                        break;
                    case ej_electricalDescription:
                        if (isElectricalTicked) {
                            // don't add duplicates
                            if (!searchResults.contains(cell1)) {
                                searchResults.add(cell1);
                            }

                            if (!searchResults.contains(cell2)) {
                                searchResults.add(cell2);
                            }
                        }
                        break;
                    case nmj_neuromuscularDescrpition:
                        if (isNeuromuscularTicked) {
                            // don't add duplicates
                            if (!searchResults.contains(cell1)) {
                                searchResults.add(cell1);
                            }

                            if (!searchResults.contains(cell2)) {
                                searchResults.add(cell2);
                            }
                        }
                        break;
                }
            }
        }

        // Return lineage names instead of functional names if flag is true
        if (areLineageNamesReturned) {
            List<String> lineageNameResults = new ArrayList<>();
            for (String result : searchResults) {
                String lineageName = getLineageNameByFunctionalName(result);

                if (lineageName != null) {
                    lineageNameResults.add(lineageName);
                }
            }
            return lineageNameResults;
        }

        // check if queryCell in results, remove
        if (searchResults.contains(queryCell.toUpperCase())) {
            searchResults.remove(queryCell.toUpperCase());
        }

        return searchResults;
    }
}