package wormguides.loaders;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.StringTokenizer;


/**
 * 
 * Syntax rules for config file:
 * 	- Replace ',' with ';' to support StringTokenizer with ',' delimeter (.csv file)
 * 
 * @author bradenkatzman
 *
 */
public class ProductionInfoLoader {

    // production info file location
    private final static int NUMBER_OF_FIELDS = 11;
    private final static String productionInfoFilePath = "wormguides/models/production_info_file/"
            + "Production_Info.csv";
    private static final String productInfoLine = "Production Information,,,,,,,,,,";
    private static final String headerLine =
            "Cells,Image Series,Marker,Strain,Compressed Embryo?,Temporal Resolution,Segmentation,"
                    + "cytoshow link,Movie start time (min),isSulston?,Total Time Points";

	/**
	 * Tokenizes each line in the config file and creates a 2D array of the file
     *
     * @return the 2D array
	 */
	public static ArrayList<ArrayList<String>> buildProductionInfo() {

        URL url = ProductionInfoLoader.class.getResource("/wormguides/models/production_info_file/Production_Info.csv");
        ArrayList<ArrayList<String>> productionInfo = new ArrayList<>();
        ArrayList<String> cells = new ArrayList<>();
        ArrayList<String> imageSeries = new ArrayList<>();
        ArrayList<String> markers = new ArrayList<>();
        ArrayList<String> strains = new ArrayList<>();
        ArrayList<String> compressedEmbryo = new ArrayList<>();
        ArrayList<String> temporalResolutions = new ArrayList<>();
        ArrayList<String> segmentations = new ArrayList<>();
        ArrayList<String> cytoshowLinks = new ArrayList<>();
        ArrayList<String> movieStartTime = new ArrayList<>();
        ArrayList<String> isSulston = new ArrayList<>();
        ArrayList<String> totalTimePoints = new ArrayList<>();

		try {
			InputStream stream = url.openStream();
			InputStreamReader streamReader = new InputStreamReader(stream);
			BufferedReader reader = new BufferedReader(streamReader);

			String line;

			while ((line = reader.readLine()) != null) {
				// skip product info line and header line
				if (line.equals(productInfoLine)) {
					line = reader.readLine();

					if (line.equals(headerLine)) {
						line = reader.readLine();
					}

					if (line == null)
						break;
				}

				// make sure valid line
				if (line.length() <= 1)
					break;

				StringTokenizer tokenizer = new StringTokenizer(line, ",");
				// check if valid line
				if (tokenizer.countTokens() == NUMBER_OF_FIELDS) {
					cells.add(tokenizer.nextToken());
					imageSeries.add(tokenizer.nextToken());
					markers.add(tokenizer.nextToken());
					strains.add(tokenizer.nextToken());
					compressedEmbryo.add(tokenizer.nextToken());
					temporalResolutions.add(tokenizer.nextToken());
					segmentations.add(tokenizer.nextToken());
					cytoshowLinks.add(tokenizer.nextToken());
					movieStartTime.add(tokenizer.nextToken());
					isSulston.add(tokenizer.nextToken());
					totalTimePoints.add(tokenizer.nextToken());
				}
			}

			// add array lists
			productionInfo.add(cells);
			productionInfo.add(imageSeries);
			productionInfo.add(markers);
			productionInfo.add(strains);
			productionInfo.add(compressedEmbryo);
			productionInfo.add(temporalResolutions);
			productionInfo.add(segmentations);
			productionInfo.add(cytoshowLinks);
			productionInfo.add(movieStartTime);
			productionInfo.add(isSulston);
			productionInfo.add(totalTimePoints);

			return productionInfo;

		} catch (IOException e) {
			System.out.println("The production info file " + productionInfoFilePath + " wasn't found on the system.");
		}

		return productionInfo;
	}

}
