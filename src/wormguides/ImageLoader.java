package wormguides;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class ImageLoader {
	
	private ImageView forward, backward, play, pause;
	private Image edit, eye; 
	//private Image close;
	JarFile jarFile;
	
	public ImageLoader(String jarPath) {
		try {
			this.jarFile = new JarFile(new File(jarPath));

			Enumeration<JarEntry> entries = jarFile.entries();
			//int time = 0;
			
			JarEntry entry;
			while (entries.hasMoreElements()){
				entry = entries.nextElement();
				if (entry.getName().startsWith(ENTRY_PREFIX)) {
					processImage(entry);
				}
			}

			jarFile.close();
			
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	public void processImage(JarEntry entry) throws IOException {
		InputStream input = jarFile.getInputStream(entry);
		Image image = new Image(input);
		switch (entry.getName()) {
			case EDIT_PNG:
				this.edit = image;
				return;
			case EYE_PNG:
				this.eye = image;
				return;
			/*
			case CLOSE_PNG:
				this.close = image;
				return;
			 */
		}
		ImageView icon = new ImageView(image);
		switch (entry.getName()) {
			case BACKWARD_PNG:
				this.backward = icon;
				break;
			case FORWARD_PNG:
				this.forward = icon;
				break;
			case PLAY_PNG:
				this.play = icon;
				break;
			case PAUSE_PNG:
				this.pause = icon;
				break;
		}
	}

	public ImageView getForwardIcon() {
		return this.forward;
	}
	
	public ImageView getBackwardIcon() {
		return this.backward;
	}
	
	public ImageView getPlayIcon() {
		return this.play;
	}
	
	public ImageView getPauseIcon() {
		return this.pause;
	}
	
	public ImageView getEditIcon() {
		return new ImageView(edit);
	}
	
	public ImageView getEyeIcon() {
		return new ImageView(eye);
	}
	
	/*
	public ImageView getCloseIcon() {
		return new ImageView(close);
	}
	*/
	
	private static final String ENTRY_PREFIX = "wormguides/view/icons/",
			BACKWARD_PNG = ENTRY_PREFIX+"backward.png",
			FORWARD_PNG = ENTRY_PREFIX+"forward.png",
			PAUSE_PNG = ENTRY_PREFIX+"pause.png",
			PLAY_PNG = ENTRY_PREFIX+"play.png",
			EDIT_PNG = ENTRY_PREFIX+"edit.png",
			EYE_PNG = ENTRY_PREFIX+"eye.png";
			//CLOSE_PNG = ENTRY_PREFIX+"close.png";
}
