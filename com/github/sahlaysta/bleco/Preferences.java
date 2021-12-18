package com.github.sahlaysta.bleco;

import java.awt.Frame;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;

import com.github.sahlaysta.json.JsonObject;
import com.github.sahlaysta.json.JsonParser;

//Loads and saves JSON preferences file
final class Preferences {
	//Invis constructor
	private Preferences() {}

	/* Static block to set JSON_FILE to the
	 * .prefs file, which is the path of
	 * the .jar file with ".prefs"
	 * added to the end */
	private static final File JSON_FILE;
	static {
		File prefsFile = null;
		try {
			String runningJarPath = new File(
					GUI.class
					.getProtectionDomain()
					.getCodeSource()
					.getLocation()
					.toURI()
				).toString();
			prefsFile = new File(runningJarPath + ".prefs");
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		JSON_FILE = prefsFile;
	}
	
	//Preference variable fields
	private static WindowProperties windowProperties;
	private static String searchQuery;
	private static String font;
	private static int fontSize;
	private static boolean handwritingWindowOpen;
	private static int handwritingWindowX, handwritingWindowY;
	static final WindowProperties getWindowProperties() {
		return windowProperties;
	}
	static final void setSearchQuery(String searchQuery) {
		Preferences.searchQuery = searchQuery;
	}
	static final String getSearchQuery() {
		return searchQuery;
	}
	static final void setFont(String font) {
		Preferences.font = font;
	}
	static final String getFont() {
		return font;
	}
	static final void setFontSize(int fontSize) {
		Preferences.fontSize = fontSize;
	}
	static final int getFontSize() {
		return fontSize;
	}
	static final boolean getHandwritingWindowOpen() {
		return handwritingWindowOpen;
	}
	static final void
	setHandwritingWindowOpen(boolean handwritingWindowOpen) {
		Preferences.handwritingWindowOpen = handwritingWindowOpen;
	}
	static final int getHandwritingWindowX() {
		return handwritingWindowX;
	}
	static final void
	setHandwritingWindowX(int handwritingWindowX) {
		Preferences.handwritingWindowX = handwritingWindowX;
	}
	static final int getHandwritingWindowY() {
		return handwritingWindowY;
	}
	static final void
	setHandwritingWindowY(int handwritingWindowY) {
		Preferences.handwritingWindowY = handwritingWindowY;
	}
	

	/* Parse the preference variable fields from the
	 * preferences JSON file */
	static {
		try {
			JsonObject jo
				= JsonParser.parse(JSON_FILE).toJsonObject();
			JsonObject jsonWindowProperties
				= jo.get("windowProperties").toJsonObject();
			boolean windowMaximized =
					jsonWindowProperties.get("maximized").toBoolean();
			int windowX = jsonWindowProperties.get("x")
					.toBigDecimal().intValue();
			int windowY = jsonWindowProperties.get("y")
					.toBigDecimal().intValue();
			int windowWidth = jsonWindowProperties.get("width")
					.toBigDecimal().intValue();
			int windowHeight = jsonWindowProperties.get("height")
					.toBigDecimal().intValue();
			
			windowProperties = new WindowProperties(
					windowMaximized, windowX, windowY,
					windowWidth, windowHeight);
			searchQuery = jo.get("searchQuery").toString();
			Dictionary.setCharacterType(
					jo.get("characterPreference")
						.toBigDecimal().intValue());
			font = jo.get("font").toString();
			fontSize = jo.get("fontSize").toBigDecimal().intValue();
			handwritingWindowOpen = jo.get("handwritingWindowOpen")
				.toBoolean();
			handwritingWindowX = jo.get("handwritingWindowX")
					.toBigDecimal().intValue();
			handwritingWindowY = jo.get("handwritingWindowY")
					.toBigDecimal().intValue();
		} catch (Exception e) {
			if (!(e instanceof FileNotFoundException)) {
				System.out.println("Exception loading preferences:");
				e.printStackTrace();
			}
			
			//Default preference values
			windowProperties = new WindowProperties(
					false, 200, 200, 400, 300);
			searchQuery = "";
			font = "Dialog";
			fontSize = 13;
			handwritingWindowOpen = false;
			handwritingWindowX = 430;
			handwritingWindowY = 200;
		}
	}
	
	/* Listens to changes in Frame window resizing and
	 * movement to update WindowProperties */
	static final void subscribe(Frame frame) {
		frame.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				windowProperties =
					WindowProperties.fromFrame(
						frame, windowProperties);
			}
			@Override
			public void componentMoved(ComponentEvent e) {
				windowProperties =
					WindowProperties.fromFrame(
						frame, windowProperties);
			}
		});
	}
	
	/* Save the preferences to the json file */
	static final void save() {
		JsonObject jo = new JsonObject();
		JsonObject jsonWindowProperties = new JsonObject();
		jsonWindowProperties.put("maximized", windowProperties.maximized);
		jsonWindowProperties.put("x", windowProperties.x);
		jsonWindowProperties.put("y", windowProperties.y);
		jsonWindowProperties.put("width", windowProperties.width);
		jsonWindowProperties.put("height", windowProperties.height);
		jo.put("windowProperties", jsonWindowProperties);
		jo.put("searchQuery", searchQuery);
		jo.put("font", font);
		jo.put("fontSize", fontSize);
		jo.put("characterPreference", Dictionary.getCharacterType());
		jo.put("handwritingWindowOpen", handwritingWindowOpen);
		jo.put("handwritingWindowX", handwritingWindowX);
		jo.put("handwritingWindowY", handwritingWindowY);
		try {
			JsonParser.write(jo, JSON_FILE);
		} catch (IOException e) {
			System.out.println("Exception saving preferences:");
			e.printStackTrace();
		}
	}
}