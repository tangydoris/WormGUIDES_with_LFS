/*
 * Bao Lab 2016
 */

package wormguides.models;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import partslist.PartsList;

/**
 * The list of cell deaths represented in internal memory and a DOM for external window viewing
 */
public class CellDeaths {
    private final static String CellDeathsFile = "/wormguides/models/cell_deaths/CellDeaths.csv";
    private static List<String> cellDeaths;

    static {
        cellDeaths = new ArrayList<String>();

        URL url = PartsList.class.getResource(CellDeathsFile);
        try (InputStream input = url.openStream();
             InputStreamReader isr = new InputStreamReader(input);
             BufferedReader br = new BufferedReader(isr)) {

            String line;
            while ((line = br.readLine()) != null) {
                cellDeaths.add(line.toLowerCase());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static boolean containsCell(String cell) {
        return cellDeaths != null && cellDeaths.contains(cell.toLowerCase());
    }
    
    public static Object[] getCellDeathsAsArray() {
    	return cellDeaths.toArray();
    }
}