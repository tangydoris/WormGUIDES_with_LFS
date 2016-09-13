package wormguides.loaders;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javafx.collections.ObservableList;

import wormguides.models.Note;
import wormguides.models.Story;

/*
 * Used for loading/saving stories
 */
public class StoryFileUtil {

    private static final String CS = ",", BR = "\n", NAME = "Tag Name", CONTENTS = "Tag Contents",
            DISPLAY = "Tag Display", ATTACHMENT = "Attachment Type", LOCATION = "xyz Locatoin", CELLS = "Cells",
            MARKER = "Marker", SOURCE = "Imaging Source", RESOURCE = "Resource Location", START = "Start Time",
            END = "End Time", COMMENTS = "Comments", AUTHOR = "Author", DATE = "Date", COLOR = "Color Scheme Url";

	public static Story loadFromCSVFile(ObservableList<Story> stories, File file, int offset) {
		StoriesLoader.loadFromFile(file, stories, offset);
		return null;
	}

	public static File saveToCSVFile(Story story, File file, int offset) throws IOException {
		BufferedWriter out = null;
		try {
			FileWriter fstream = new FileWriter(file, false); // false means do
																// not append
			out = new BufferedWriter(fstream);

			// do writing
			// headers
			out.append(NAME).append(CS).append(CONTENTS).append(CS).append(DISPLAY).append(CS).append(ATTACHMENT)
					.append(CS).append(LOCATION).append(CS).append(CELLS).append(CS).append(MARKER).append(CS)
					.append(SOURCE).append(CS).append(RESOURCE).append(CS).append(START).append(CS).append(END)
					.append(CS).append(COMMENTS).append(CS).append(AUTHOR).append(CS).append(DATE).append(CS)
					.append(COLOR);
			out.append(BR);

			// story
			String[] storyLine = new String[StoriesLoader.NUMBER_OF_CSV_FIELDS];
			storyLine[StoriesLoader.STORY_NAME_INDEX] = story.getName();
			storyLine[StoriesLoader.STORY_DESCRIPTION_INDEX] = story.getDescription();
			storyLine[StoriesLoader.STORY_AUTHOR_INDEX] = story.getAuthor();
			storyLine[StoriesLoader.STORY_DATE_INDEX] = story.getDate();
			storyLine[StoriesLoader.STORY_COLOR_URL_INDEX] = story.getColorURL();
			out.append(convertToCSV(storyLine)).append(BR);

			// notes
			for (Note note : story.getNotes()) {
				String[] noteLine = new String[StoriesLoader.NUMBER_OF_CSV_FIELDS];
				noteLine[StoriesLoader.NAME_INDEX] = note.getTagName();
				noteLine[StoriesLoader.CONTENTS_INDEX] = note.getTagContents();
				noteLine[StoriesLoader.DISPLAY_INDEX] = note.getTagDisplay().toString();
				noteLine[StoriesLoader.TYPE_INDEX] = note.getAttachmentType().toString();
				noteLine[StoriesLoader.LOCATION_INDEX] = note.getLocationString();
				noteLine[StoriesLoader.CELLNAME_INDEX] = note.getCellName();
				noteLine[StoriesLoader.IMG_SOURCE_INDEX] = note.getImgSource();
				noteLine[StoriesLoader.MARKER_INDEX] = note.getMarker();
				noteLine[StoriesLoader.RESOURCE_LOCATION_INDEX] = note.getResourceLocation();
				// if time is no specified, do not use the Integer.MIN_VALUE
				// value
				// leave it blank
				int start = note.getStartTime();
				int end = note.getEndTime();
				if (start != Integer.MIN_VALUE && end != Integer.MIN_VALUE) {
					noteLine[StoriesLoader.START_TIME_INDEX] = Integer.toString(start + offset);
					noteLine[StoriesLoader.END_TIME_INDEX] = Integer.toString(end + offset);
				}
				noteLine[StoriesLoader.COMMENTS_INDEX] = note.getComments();
				out.append(convertToCSV(noteLine)).append(BR);
			}
		} catch (IOException e) {
			System.err.println("Error: " + e.getMessage());
		} finally {
			if (out != null) {
				out.close();
			}
		}
		return file;
	}

	private static String convertToCSV(String[] line) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < line.length; i++) {
			if (line[i] == null)
				sb.append("");
			else if (line[i].contains(CS))
				sb.append("\"").append(line[i]).append("\"");
			else
				sb.append(line[i]);

			if (i < line.length - 1)
				sb.append(CS);
		}
		return sb.toString();
	}

}
