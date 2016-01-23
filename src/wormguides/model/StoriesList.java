package wormguides.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import wormguides.model.Note.AttachmentTypeEnumException;
import wormguides.model.Note.LocationStringFormatException;
import wormguides.model.Note.TagDisplayEnumException;
import wormguides.model.Note.TimeStringFormatException;

public class StoriesList {
	
	public ObservableList<Story> stories;
	public SceneElementsList elementsList;
	
	
	public StoriesList(SceneElementsList list) {
		stories = FXCollections.observableArrayList();
		elementsList = list;
		buildStories();
	}
	
	
	public void buildStories() {
		try {
			JarFile jarFile = new JarFile(new File("WormGUIDES.jar"));
			Enumeration<JarEntry> entries = jarFile.entries();
			JarEntry entry;
			
			while (entries.hasMoreElements()) {
				entry = entries.nextElement();
				
				if (entry.getName().equals("wormguides/model/story_file/"+STORY_CONFIG_FILE_NAME)) {
					InputStream stream = jarFile.getInputStream(entry);
					processStream(stream);
				}
			}
			
			jarFile.close();
		} catch (FileNotFoundException e) {
			System.out.println("The config file '" + STORY_CONFIG_FILE_NAME + "' wasn't found on the system.");
		} catch (IOException e) {
			System.out.println("The config file '" + STORY_CONFIG_FILE_NAME + "' wasn't found on the system.");
		}
	}
	
	
	public void processStream(InputStream stream) {
		int storyCounter = -1; //used for accessing the current story for adding scene elements

		try {
			InputStreamReader streamReader = new InputStreamReader(stream);
			BufferedReader reader = new BufferedReader(streamReader);
			
			String line;
			
			// Skip heading line
			reader.readLine();
			
			while ((line = reader.readLine()) != null) {
				String[] split =  line.split(",", NUMBER_OF_CSV_FIELDS); //split the line up by commas
				
				int len = split.length;

				if (len!=NUMBER_OF_CSV_FIELDS) {
					System.out.println("Missing fields in CSV file.");
					continue;
				}
				
				if (isStory(split)) {
					Story story = new Story(split[STORY_NAME_INDEX], split[STORY_DESCRIPTION_INDEX]);
					stories.add(story);
					storyCounter++;
				}
				else {
					Note note = new Note(split[NAME_INDEX], split[CONTENTS_INDEX]);
					try {
						note.setTagDisplay(split[DISPLAY_INDEX]);
						note.setAttachmentType(split[TYPE_INDEX]);
						note.setLocation(split[LOCATION_INDEX]);
						note.setCellName(split[CELLNAME_INDEX]);
						
						note.setImagingSource(split[IMG_SOURCE_INDEX]);
						note.setResourceLocation(split[RESOURCE_LOCATION_INDEX]);
						
						note.setStartTime(split[START_TIME_INDEX]);
						note.setEndTime(split[END_TIME_INDEX]);
						
						note.setComments(split[COMMENTS_INDEX]);
						
					} catch (ArrayIndexOutOfBoundsException e) {
						System.out.println(e.toString());
						System.out.println(line);
					} catch (TagDisplayEnumException e) {
						System.out.println(e.toString());
						System.out.println(line);
					} catch (AttachmentTypeEnumException e) {
						System.out.println(e.toString());
						System.out.println(line);
					} catch (LocationStringFormatException e) {
						System.out.println(e.toString());
						System.out.println(line);
					} catch (TimeStringFormatException e) {
						System.out.println(e.toString());
						System.out.println(line);
					} finally {
						Story story = stories.get(storyCounter);
						story.addNote(note);
						note.setParent(story);
					}
				}	
			}
			
			reader.close();
			
		} catch (ArrayIndexOutOfBoundsException e) {
			System.out.println("Unable to process file '" + STORY_CONFIG_FILE_NAME + "'.");
		} catch (NumberFormatException e) {
			System.out.println("Number Format Error in file '" + STORY_CONFIG_FILE_NAME + "'.");
		} catch (IOException e) {
			System.out.println("The config file '" + STORY_CONFIG_FILE_NAME + "' wasn't found on the system.");
		}
	}
	
	
	public boolean isStory(String[] csvLine) {
		try {
			if (csvLine[DISPLAY_INDEX].isEmpty())
				return true;
		} catch (ArrayIndexOutOfBoundsException e) {
			return false;
		}
		return false;
	}
	
	
	public ArrayList<Note> getNotesWithCell() {
		ArrayList<Note> notes = new ArrayList<Note>();
		for (Story story : stories) {
			if (story.isActive())
				notes.addAll(story.getNotesWithCell());
		}
		return notes;
	}
	
	
	public ArrayList<Note> getActiveNotes(int time) {
		ArrayList<Note> notes = new ArrayList<Note>();
		
		for (Story story : stories) {
			if (story.isActive())
				notes.addAll(story.getNotesAtTime(time));
		}
		
		return notes;
	}
	
	
	public ObservableList<Story> getStories() {
		return stories;
	}
	
	
	public String toString() {
		StringBuilder sb = new StringBuilder("Stories list:\n");
		for (int i=0; i<stories.size(); i++) {
			Story story = stories.get(i);
			sb.append(story.getName()).append(": ")
				.append(story.getNumberOfNotes()).append(" notes\n");
			for (Note note : story.getNotes()) {
				sb.append("\t").append(note.getTagName()).append(": times ")
					.append(note.getStartTime()).append(" ")
					.append(note.getEndTime()).append("\n");
			}
			if (i<stories.size()-1)
				sb.append("\n");
		}
		
		return sb.toString();
	}
	
	
	private final String STORY_CONFIG_FILE_NAME = "StoryListConfig.csv";
	private final int NUMBER_OF_CSV_FIELDS = 12;
	private final int STORY_NAME_INDEX = 0,
					STORY_DESCRIPTION_INDEX = 1;
	private final int NAME_INDEX = 0,
					CONTENTS_INDEX = 1,
					DISPLAY_INDEX = 2,
					TYPE_INDEX = 3,
					LOCATION_INDEX = 4,
					CELLNAME_INDEX = 5,
					MARKER_INDEX = 6,
					IMG_SOURCE_INDEX = 7,
					RESOURCE_LOCATION_INDEX = 8,
					START_TIME_INDEX = 9,
					END_TIME_INDEX = 10,
					COMMENTS_INDEX = 11;
	
}