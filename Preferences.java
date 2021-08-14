package bleco;

import java.awt.Dimension;
import java.awt.Point;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;

import javax.swing.JFrame;

//Google GSON: https://github.com/google/gson
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

final class Preferences {
	static final File jsonFile = getJsonFile();
	final JsonObject preferences;
	
	//Preference fields
	private int characterOption;
	private String searchFieldEntry;
	private Point handwritingWindowLocation;
	private boolean handwritingWindowOpen;
	private WindowProperties windowProperties;
	public Preferences() {
		preferences = readJson();
		if (preferences == null)
			defaultValues();
		else {
			try {
				//Load characterOption
				characterOption = preferences.get("characterOption").getAsInt();
				
				//Load searchFieldEntry
				searchFieldEntry = preferences.get("searchFieldEntry").getAsString();
				
				//Load windowProperties
				windowProperties = new WindowProperties();
				JsonObject wpObj = preferences.get("windowProperties").getAsJsonObject();
				windowProperties.x = wpObj.get("x").getAsInt();
				windowProperties.y = wpObj.get("y").getAsInt();
				windowProperties.width = wpObj.get("width").getAsInt();
				windowProperties.height = wpObj.get("height").getAsInt();
				windowProperties.maximized = wpObj.get("maximized").getAsBoolean();
				handwritingWindowOpen = wpObj.get("handwritingOpen").getAsBoolean();
				handwritingWindowLocation = new Point(
						wpObj.get("handwritingX").getAsInt(),
						wpObj.get("handwritingY").getAsInt());
			} catch (Exception e) {
				defaultValues();
			}
		}
	}
	private void defaultValues() {
		characterOption = 0;
		searchFieldEntry = "";
		handwritingWindowLocation = null;
		handwritingWindowOpen = false;
		windowProperties = null;
	}
	
	//Public getters
	public int getCharacterOption() {
		return characterOption;
	}
	public String getSearchFieldEntry() {
		return searchFieldEntry;
	}
	public void applyWindowProperties(JFrame jFrame) {
		if (windowProperties == null)
			return;
		jFrame.setLocation(windowProperties.x, windowProperties.y);
		jFrame.setSize(new Dimension(windowProperties.width, windowProperties.height));
		if (windowProperties.maximized)
			jFrame.setExtendedState(jFrame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
	}
	public boolean getHandwritingWindowOpen() {
		return handwritingWindowOpen;
	}
	public Point getHandwritingWindowLocation() {
		return handwritingWindowLocation;
	}
	
	//Save method
	public void save(GUI gui) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(jsonFile));
		bw.write("{\r\n\t\"characterOption\": ");
		bw.write(Integer.toString(gui.characterOption));
		bw.write(",\r\n\t\"searchFieldEntry\": \"");
		bw.write(escapeJson(gui.jTextField.getText()));
		bw.write("\",\r\n\t\"windowProperties\": {\r\n\t\t\"x\": ");
		bw.write(Integer.toString(gui.windowProperties.x));
		bw.write(",\r\n\t\t\"y\": ");
		bw.write(Integer.toString(gui.windowProperties.y));
		bw.write(",\r\n\t\t\"width\": ");
		bw.write(Integer.toString(gui.windowProperties.width));
		bw.write(",\r\n\t\t\"height\": ");
		bw.write(Integer.toString(gui.windowProperties.height));
		bw.write(",\r\n\t\t\"maximized\": ");
		bw.write(Boolean.toString(gui.windowProperties.maximized));
		bw.write(",\r\n\t\t\"handwritingOpen\": ");
		bw.write(Boolean.toString(gui.handwritingWindow.isVisible()));
		Point p = gui.handwritingWindow.getLocation();
		bw.write(",\r\n\t\t\"handwritingX\": ");
		bw.write(Integer.toString(p.x));
		bw.write(",\r\n\t\t\"handwritingY\": ");
		bw.write(Integer.toString(p.y));
		bw.write("\r\n\t}\r\n}");
		bw.close();
	}

	static final String escapeJson(CharSequence cs) {
		/*
		 * Escape a String to Json
		 * Example:
		 * string = "Hello world";
		 * output = "\u0048\u0065\u006c\u006c\u006f\u0020\u0077\u006f\u0072\u006c\u0064"
		 */
		char[] chars = new char[cs.length() * 6];
		for (int i = 0; i < cs.length(); i++) {
			String hex = "\\u" + String.format("%04x", (int) cs.charAt(i));
			int ii = 0;
			for (int iii = (i * 6); iii < (i * 6) + 6; iii++)
				chars[iii] = hex.charAt(ii++);
		}
		return new String(chars);
	}
	
	//Static
	static final class WindowProperties {
		int x, y, width, height;
		boolean maximized;
	}
	static File getJsonFile() { //get filepath of bleco.jar.prefs
		try {
			return new File(new File(Preferences.class.getProtectionDomain().getCodeSource().getLocation()
				    .toURI()).toString() + ".prefs");
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return null;
		}
	}
	static JsonObject readJson() {
		try {
			JsonElement output = JsonParser.parseReader(new FileReader(jsonFile));
			if (!output.isJsonObject()) return null;
			return output.getAsJsonObject();
		} catch (JsonIOException | JsonSyntaxException e) {
			System.out.println("Bad json syntax");
			return null;
		} catch (FileNotFoundException e) {
			return null;
		}
	}
}
