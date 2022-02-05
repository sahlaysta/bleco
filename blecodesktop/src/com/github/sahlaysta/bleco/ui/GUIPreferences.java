package com.github.sahlaysta.bleco.ui;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.math.BigDecimal;

import com.github.sahlaysta.bleco.dict.Entry;
import com.github.sahlaysta.bleco.ui.GUI;
import com.github.sahlaysta.json.JsonElement;
import com.github.sahlaysta.json.JsonObject;
import com.github.sahlaysta.json.JsonParser;

/* Reads and saves Bleco GUI preferences
 * such as window size and location
 * to the .prefs file */
final class GUIPreferences {

	//Constructor
	final GUI gui;
	GUIPreferences(GUI gui) {
		this.gui = gui;
		init();
	}
	
	//Window properties
	boolean maximized;
	int x, y, width, height;
	private void init() {
		// when the gui window is first opened
		gui.addWindowListener(new WindowAdapter() {
			@Override
			public void windowOpened(WindowEvent e) {
				gui.removeWindowListener(this);
				updateWindowProperties();
				listenWindowProperties();
			}
		});
	}
	private void updateWindowProperties() {
		Point p = gui.getLocation();
		Dimension d = gui.getSize();
		maximized = gui.getExtendedState() == Frame.MAXIMIZED_BOTH;
		if (maximized)
			return;
		x = p.x;
		y = p.y;
		width = d.width;
		height = d.height;
	}
	private void listenWindowProperties() {
		gui.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentMoved(ComponentEvent e) {
				updateWindowProperties();
			}
			@Override
			public void componentResized(ComponentEvent e) {
				updateWindowProperties();
			}
		});
	}
	
	/* Static block to set JSON_FILE to the
	 * .prefs file, which is the path of
	 * the running .jar file with ".prefs"
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
		} catch (Exception e) {
			e.printStackTrace();
		}
		JSON_FILE = prefsFile;
	}
	
	//Preferences map
	private JsonObject prefs = new JsonObject();
	//Preference variables
	//Font
	void setFont(String font) {
		prefs.put("font", font);
	}
	String getFont() {
		return prefs.get("font").toString();
	}
	//Font size
	void setFontSize(int fontSize) {
		prefs.put("fontSize", fontSize);
	}
	int getFontSize() {
		return prefs.get("fontSize").toBigDecimal().intValue();
	}
	//English search toggle
	void setEngSearch(boolean engSearch) {
		prefs.put("engSearch", engSearch);
	}
	boolean getEngSearch() {
		return prefs.get("engSearch").toBoolean();
	}
	//Handwriting window location
	void setHandwritingWindowLocation(Point p) {
		if (p == null)
			return;
		prefs.put("handwritingWindowX", p.x);
		prefs.put("handwritingWindowY", p.y);
	}
	Point getHandwritingWindowLocation() {
		JsonElement a = new JsonElement("");
		JsonElement jeX = prefs.getOrDefault(
			"handwritingWindowX", a);
		JsonElement jeY = prefs.getOrDefault(
			"handwritingWindowY", a);
		if (jeX == a || jeY == a)
			return null;
		if (!jeX.isBigDecimal() || !jeY.isBigDecimal())
			return null;
		return
			new Point(
				jeX.toBigDecimal().intValue(),
				jeY.toBigDecimal().intValue());
	}
	
	
	//Load preferences
	void tryLoad() {
		try {
			load();
			return;
		} catch (Exception e) {
			if (!(e instanceof FileNotFoundException)) {
				System.out.println("Failed to load prefs:");
				e.printStackTrace();
			}
		}
		
		//Default preferences
		gui.setSize(360, 260);
		gui.searcher.updateSearch();
		gui.controller.simplified();
		gui.searcher.setFont("Dialog", 13);
		gui.controller.engSearch();//false call
	}
	private void load() throws Exception {
		//Read json file
		prefs = JsonParser.parse(JSON_FILE).toJsonObject();
		
		//Preference values
		boolean windowMaximized;
		BigDecimal windowX, windowY, windowWidth, windowHeight;
		BigDecimal characterType;
		String search;
		String font;
		int fontSize;
		boolean engSearch;
		boolean handwritingWindowOpened;
		
		//Load all preferences
		JsonObject windowProperties
			= prefs.get("windowProperties")
				.toJsonObject();
		windowMaximized = windowProperties.get("maximized").toBoolean();
		windowX = windowProperties.get("x").toBigDecimal();
		windowY = windowProperties.get("y").toBigDecimal();
		windowWidth = windowProperties.get("width").toBigDecimal();
		windowHeight = windowProperties.get("height").toBigDecimal();
		characterType = prefs.get("characterType").toBigDecimal();
		search = prefs.get("search").toString();
		font = getFont();
		fontSize = getFontSize();
		engSearch = getEngSearch();
		handwritingWindowOpened = prefs.get("handwritingWindowOpened").toBoolean();
		
		//Apply all preferences
		gui.setLocation(windowX.intValue(), windowY.intValue());
		gui.setSize(windowWidth.intValue(), windowHeight.intValue());
		updateWindowProperties();
		if (windowMaximized)
			gui.setExtendedState(Frame.MAXIMIZED_BOTH);
		if (characterType.equals(BigDecimal.ONE))
			gui.controller.traditional();
		else
			gui.controller.simplified();
		if (search.isEmpty())
			gui.searcher.updateSearch();
		else
			gui.searcher.searchField.setText(search);
		gui.searcher.searchField.setCaretPosition(search.length());
		gui.searcher.setFont(font, fontSize);
		gui.menuBar.engSearch.setSelected(engSearch);
		gui.controller.engSearch();
		if (handwritingWindowOpened) {
			gui.menuBar.handwWindow.setSelected(true);
			gui.controller.handw();
		}
	}
	
	//Save preferences
	void trySave() {
		try {
			save();
		} catch (Exception e) {
			System.out.println("Failed to save prefs:");
			e.printStackTrace();
		}
	}
	private void save() throws Exception {
		//Preference values
		boolean windowMaximized;
		int windowX, windowY, windowWidth, windowHeight;
		int characterType;
		String search;
		boolean handwritingWindowOpened;
		
		//Define all preferences
		windowMaximized = maximized;
		windowX = x;
		windowY = y;
		windowWidth = width;
		windowHeight = height;
		characterType = Entry.getCharacterType();
		search = gui.searcher.searchField.getText();
		handwritingWindowOpened = gui.controller.hw != null;
		
		//Set all preferences
		JsonObject windowProperties = new JsonObject();
		prefs.put("windowProperties", windowProperties);
		windowProperties.put("maximized", windowMaximized);
		windowProperties.put("x", windowX);
		windowProperties.put("y", windowY);
		windowProperties.put("width", windowWidth);
		windowProperties.put("height", windowHeight);
		prefs.put("characterType", characterType);
		prefs.put("search", search);
		prefs.put("handwritingWindowOpened", handwritingWindowOpened);
		
		//Write json file
		JsonParser.write(prefs, JSON_FILE);
	}
}