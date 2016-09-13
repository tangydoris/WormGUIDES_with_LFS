package wormguides.loaders;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import javafx.collections.ObservableList;

import wormguides.models.Note;
import wormguides.models.Note.AttachmentTypeEnumException;
import wormguides.models.Note.LocationStringFormatException;
import wormguides.models.Note.TagDisplayEnumException;
import wormguides.models.Story;

/*
 * Used to load stories from the internal stories config file
 */
public class StoriesLoader {

    public static final int NUMBER_OF_CSV_FIELDS = 15;
    public static final int STORY_NAME_INDEX = 0, STORY_DESCRIPTION_INDEX = 1, STORY_AUTHOR_INDEX = 12,
            STORY_DATE_INDEX = 13, STORY_COLOR_URL_INDEX = 14;
    public static final int NAME_INDEX = 0, CONTENTS_INDEX = 1, DISPLAY_INDEX = 2, TYPE_INDEX = 3, LOCATION_INDEX = 4,
            CELLNAME_INDEX = 5, MARKER_INDEX = 6, IMG_SOURCE_INDEX = 7, RESOURCE_LOCATION_INDEX = 8,
            START_TIME_INDEX = 9, END_TIME_INDEX = 10, COMMENTS_INDEX = 11;
    private static final String StoryListConfig = "/wormguides/models/story_file/StoryListConfig.csv";

	public static void loadFromFile(File file, ObservableList<Story> stories, int offset) {
		if (file == null)
			return;

		try {
			InputStream stream = new FileInputStream(file);
			processStream(stream, stories, offset);
		} catch (FileNotFoundException e) {
			System.out.println("The file '" + file.getName() + "' was not found in system.");
		}
	}

	public static void loadConfigFile(ObservableList<Story> stories, int offset) {
		URL url = StoriesLoader.class.getResource(StoryListConfig);

		try {
			if (url != null) {
				InputStream stream = url.openStream();
				processStream(stream, stories, offset);
			}
		} catch (FileNotFoundException e) {
			System.out.println("The config file '" + StoryListConfig + "' was not found in the system.");
		} catch (IOException e) {
			System.out.println("The config file '" + StoryListConfig + "' was not found inn the system.");
		}
	}

	private static void processStream(InputStream stream, ObservableList<Story> stories, int offset) {
		int storyCounter = stories.size() - 1; // used for accessing the current
												// story for adding scene
												// elements

		try {
			InputStreamReader streamReader = new InputStreamReader(stream);
			BufferedReader reader = new BufferedReader(streamReader);

			String line;

			// Skip heading line
			reader.readLine();

			while ((line = reader.readLine()) != null) {
				String[] split = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1);

				if (split.length != NUMBER_OF_CSV_FIELDS) {
					System.out.println("Missing fields in CSV file.");
					continue;
				}

				// get rid of quotes in story description/note contents since
				// field might have contained commas
				String contents = split[CONTENTS_INDEX];
				if (contents.startsWith("\"") && contents.endsWith("\""))
					split[CONTENTS_INDEX] = contents.substring(1, contents.length() - 1);

				if (isStory(split)) {
					Story story = new Story(split[STORY_NAME_INDEX], split[STORY_DESCRIPTION_INDEX],
							split[STORY_AUTHOR_INDEX], split[STORY_DATE_INDEX], split[STORY_COLOR_URL_INDEX]);
					stories.add(story);
					storyCounter++;
				} else {
					Story story = stories.get(storyCounter);
					Note note = new Note(story, split[NAME_INDEX], split[CONTENTS_INDEX]);
					story.addNote(note);

					try {
						note.setTagDisplay(split[DISPLAY_INDEX]);
						note.setAttachmentType(split[TYPE_INDEX]);
						note.setLocation(split[LOCATION_INDEX]);
						note.setCellName(split[CELLNAME_INDEX]);

						note.setImagingSource(split[IMG_SOURCE_INDEX]);
						note.setResourceLocation(split[RESOURCE_LOCATION_INDEX]);

						String startTime = split[START_TIME_INDEX];
						String endTime = split[END_TIME_INDEX];
						if (!startTime.isEmpty() && !endTime.isEmpty()) {
							note.setStartTime(Integer.parseInt(startTime) - offset);
							note.setEndTime(Integer.parseInt(endTime) - offset);
						}

						note.setComments(split[COMMENTS_INDEX]);

					} catch (ArrayIndexOutOfBoundsException e) {
						System.out.println(e.toString());
						System.out.println(line);
					} catch (TagDisplayEnumException e) {
						System.out.println(e.toString());
						System.out.println(line);
						e.printStackTrace();
					} catch (AttachmentTypeEnumException e) {
						System.out.println(e.toString());
						System.out.println(line);
					} catch (LocationStringFormatException e) {
						System.out.println(e.toString());
						System.out.println(line);
					} catch (NumberFormatException e) {
						System.out.println(e.toString());
						System.out.println(line);
					}
				}
			}

			reader.close();

		} catch (ArrayIndexOutOfBoundsException e) {
			System.out.println("Unable to process file '" + StoryListConfig + "'.");
		} catch (NumberFormatException e) {
			System.out.println("Number Format Error in file '" + StoryListConfig + "'.");
		} catch (IOException e) {
			System.out.println("The config file '" + StoryListConfig + "' wasn't found on the system.");
		}
	}

	private static boolean isStory(String[] csvLine) {
		try {
			if (csvLine[DISPLAY_INDEX].isEmpty() && csvLine[TYPE_INDEX].isEmpty())
				return true;
		} catch (ArrayIndexOutOfBoundsException e) {
			return false;
		}
		return false;
	}
}
