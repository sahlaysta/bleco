package sahlaysta.bleco.ui;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;

import sahlaysta.bleco.dict.Entry;

//Swing menu bar actions controller
final class GUIController {
	
	//Constructor
	final GUI gui;
	GUIController(GUI gui) {
		this.gui = gui;
	}
	
	
	//Menu bar activate events
	
	//---Options---//
	//Simplified toggle
	void simplified() {
		gui.menuBar.simplified.setSelected(true);
		gui.menuBar.traditional.setSelected(false);
		Entry.setCharacterType(Entry.SIMPLIFIED_CHINESE);
		gui.searcher.updateSearch();
	}
	//Traditional toggle
	void traditional() {
		gui.menuBar.simplified.setSelected(false);
		gui.menuBar.traditional.setSelected(true);
		Entry.setCharacterType(Entry.TRADITIONAL_CHINESE);
		gui.searcher.updateSearch();
	}
	//Force English search
	void engSearch() {
		boolean toggle = gui.menuBar.engSearch.isSelected();
		gui.searcher.forceEngSearch = toggle;
		gui.searcher.updateSearch();
		gui.prefs.setEngSearch(toggle);
	}
	//Font picker
	void font() {
		GUIFontPicker.show(gui);
	}
	
	//---Entry---//
	//Next entry
	void next() {
		gui.searcher.moveSelection(1);
	}
	//Previous entry
	void previous() {
		gui.searcher.moveSelection(-1);
	}
	//Open selected entry
	void open() {
		gui.searcher.openSelectedEntry();
	}
	//Close opened entry
	void close() {
		gui.searcher.closeOpenedEntry();
	}
	//Copy selected entry
	void copy() {
		Entry entry = gui.searcher.getSelectedEntry();
		if (entry == null)
			return;
		String text
			= Entry.getCharacterType() == Entry.SIMPLIFIED_CHINESE
				? entry.simplified
				: entry.traditional;
		Toolkit.getDefaultToolkit().getSystemClipboard()
		.setContents(new StringSelection(text), null);
	}
	
	//---Handwriting---//
	GUIHandwritingWindow hw;
	//Toggle the handwriting window
	void handw() {
		boolean toggle = gui.menuBar.handwWindow.isSelected();
		if (toggle) {
			if (hw == null) {
				hw = new GUIHandwritingWindow(gui);
				hw.setVisible(true);
			}
		} else {
			if (hw != null) {
				hw.dispose();
				hw = null;
			}
		}
	}
	
	//---Help---//
	//Show "About" dialog
	void about() {
		GUIMessageBox.showAboutMsg(gui);
	}
}