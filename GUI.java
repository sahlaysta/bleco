package bleco;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

//CC-CEDICT parser by me: https://github.com/sahlaysta/cc-cedict-parser
import com.github.sahlaysta.cccedict.CCCEDICTEntry;
import com.github.sahlaysta.cccedict.CCCEDICTParser;
import com.github.sahlaysta.cccedict.CCCEDICTPinyin;

import bleco.Preferences.WindowProperties;
//HanziLookup Swing component from: https://www.kiang.org/jordan/software/hanzilookup/
import bleco.handwriting.hanzilookup.HanziLookup;
import bleco.handwriting.hanzilookup.HanziLookup.CharacterSelectionEvent;
import bleco.handwriting.hanzilookup.HanziLookup.CharacterSelectionListener;

final class GUI extends AbstractGUI {
	private static final long serialVersionUID = 1L;
	public static void main(String[] args) {
		new GUI().setVisible(true);
	}
	
	class Entry {
		CCCEDICTEntry entry;
		String styledPinyin;
		@Override
		public String toString() {
			return (characterOption == CHARACTER_SIMPLIFIED ? entry.simplified : entry.traditional)
					+ ' ' + styledPinyin + " - " + entry.definitions.get(0);
		}
	}
	Preferences preferences;
	HandwritingWindow handwritingWindow;
	Entry[] dictionary;
	SearchEntry[] searchEntries;
	WindowProperties windowProperties;
	public GUI() {
		initComponent();
		setMinimumSize(new Dimension(230, 150));
		//Apply preferences
		preferences = new Preferences();
		setCharacterOption(preferences.getCharacterOption());
		characterOption_changed(preferences.getCharacterOption());
		preferences.applyWindowProperties(this);
		
		try { //load CC-CEDICT
			List<CCCEDICTEntry> cccedict = CCCEDICTParser.parse(getClass().getResourceAsStream("cedict_ts.u8"));
			dictionary = new Entry[cccedict.size()];
			searchEntries = new SearchEntry[dictionary.length];
			int i = 0;
			for (CCCEDICTEntry entry: cccedict) {
				(dictionary[i] = new Entry()).entry = entry;
				dictionary[i].styledPinyin = CCCEDICTPinyin.toFormattedPinyin(entry);
				searchEntries[i] = SearchEntry.fromCCCEDICTEntry(entry);
				i++;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try { //load Chinese Handwriting window
			HanziLookup hanziLookup = new HanziLookup(getClass().getResourceAsStream("handwriting/strokes-extended.dat"), jTextField.getFont().deriveFont(15f));
			handwritingWindow = new HandwritingWindow(this, jTextField, hanziLookup, preferences.getHandwritingWindowLocation());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if (preferences.getHandwritingWindowOpen()) {
			handwritingWindow.setVisible(true);
			handwritingOptionCheckbox.setSelected(true);
		}
		
		//Save preferences on close
		addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                try {
					preferences.save(GUI.this);
				} catch (IOException e2) {
					System.out.println("Failed to save prefs");
				}
            }
        });
		
		//Update windowProperties on window move/resize
		ComponentAdapter ca = new ComponentAdapter() {
			public void componentMoved(ComponentEvent e) {
				updateWindowProperties();
			}
		    public void componentResized(ComponentEvent e) {
		        updateWindowProperties();
		    }
		    void updateWindowProperties() {
		    	WindowProperties newProperties = new WindowProperties();
		    	if (windowProperties != null && (newProperties.maximized = (getExtendedState() == JFrame.MAXIMIZED_BOTH))) {
		    		newProperties.x = windowProperties.x;
					newProperties.y = windowProperties.y;
					newProperties.width = windowProperties.width;
					newProperties.height = windowProperties.height;
				} else {
					Point p = getLocation();
					Dimension d = getSize();
					newProperties.x = p.x;
					newProperties.y = p.y;
					newProperties.width = d.width;
					newProperties.height = d.height;
				}
		    	
		    	windowProperties = newProperties;
		    }
		};
		ca.componentMoved(null);
		addComponentListener(ca);
		
		//Saved search field entry
		String searchFieldEntry = preferences.getSearchFieldEntry();
		jTextField.setText(searchFieldEntry);
		jTextField.setCaretPosition(searchFieldEntry.length());
	}
	
	//Search entry tuple class
	static final class SearchEntry {
		final String pinyin, pinyinTone, pinyinRaw, simplified, traditional;
		SearchEntry(String pinyin, String pinyinTone, String pinyinRaw, String simplified, String traditional) {
			this.pinyin = pinyin;
			this.pinyinTone = pinyinTone;
			this.pinyinRaw = pinyinRaw;
			this.simplified = simplified;
			this.traditional = traditional;
		}
		static SearchEntry fromCCCEDICTEntry(CCCEDICTEntry entry) {
			return new SearchEntry(
					formatPinyin(entry.pronunciation).replace("u:", "v"),
					entry.pronunciation.replace(" ", "").toLowerCase().replace("u:", "v"),
					entry.pronunciation.replace("u:", "v"),
					entry.simplified.replace(" ", "").toLowerCase(),
					entry.traditional.replace(" ", "").toLowerCase()
					);
		}
		static final String formatPinyin(String pinyin) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < pinyin.length(); i++) {
				char c = pinyin.charAt(i);
				switch (c) {
				case ' ': break;
				case '1': break;
				case '2': break;
				case '3': break;
				case '4': break;
				case '5': break;
				default: sb.append(Character.toLowerCase(c)); break;
				}
			}
			return sb.toString();
		}
	}
	
	// Simplified / traditional preference change
	int characterOption;
	void characterOption_changed(int option) {
		characterOption = option;
		jTextField_changed(jTextField.getText());
	}
	
	//Update JList on Text field edit
	SearchWorker searchWorker;
	@Override
	void jTextField_changed(String text) {
		if (searchWorker != null && !searchWorker.isDone()) {
			try {
				searchWorker.cancel(true);
			} catch(ConcurrentModificationException e) {
				searchWorker = null;
			}
		}
		
		//SwingWorker for search results
		final String searchTerm = text.toLowerCase();
		(searchWorker = new SearchWorker(searchTerm)).execute();
	}
	// Search results SwingWorker ///////////////
	private final class SearchWorker extends SwingWorker<Object, Object> {
		final String searchTerm;
		final List<Entry> add = new ArrayList<>(), add2 = new ArrayList<>(), add3 = new ArrayList<>();
		SearchWorker(String searchTerm) {
			super();
			this.searchTerm = searchTerm;
		}
		@Override
		protected Object doInBackground() { //Match search results to add to JList
			if (!searchTerm.contains("*")) {
				for (int i = 0; i < searchEntries.length; i++) {
					if (searchTerm.equals(searchEntries[i].pinyin)
							|| searchTerm.equals(searchEntries[i].pinyinTone)
							|| searchTerm.equals(searchEntries[i].simplified)
							|| searchTerm.equals(searchEntries[i].traditional)) {
						add.add(dictionary[i]);
					}
					else if (searchMatches(searchTerm, searchEntries[i].pinyin)
							|| searchMatches(searchTerm, searchEntries[i].pinyinTone)
							|| searchMatches(searchTerm, searchEntries[i].simplified)
							|| searchMatches(searchTerm, searchEntries[i].traditional)) {
						add2.add(dictionary[i]);
					}
					else if (pinyinMatches(searchTerm, i)
							|| pinyinMatchesBackwards(searchTerm, i)) {
						add3.add(dictionary[i]);
					}
				}
			} else { //wildcard search
				
			}
			return null;
		}
		@Override
		protected void done() {
			listModel.clear();
			if (searchTerm.length() == 0) {
				listModel.addElement("Enter a search term...");
				jList.setEnabled(false);
				return;
			} else jList.setEnabled(true);
			for (Entry e: add)
				listModel.addElement(e.toString());
			for (Entry e: add2)
				listModel.addElement(e.toString());
			for (Entry e: add3)
				listModel.addElement(e.toString());
			if (listModel.getSize() == 0) {
				jList.setEnabled(false);
				listModel.addElement("No results found");
			}
		}
		
		///// Search result methods //////////
		private final boolean searchMatches(String searchTerm, String entryString) {
			if (entryString.length() <= searchTerm.length())
				return false;
			return entryString.substring(0, searchTerm.length()).equals(searchTerm);
		}
		private final boolean pinyinMatches(String searchTerm, int entryIndex) {
			for (int i = searchTerm.length() - 1; i > 0; i--) {
				if (i >= searchEntries[entryIndex].simplified.length())
					continue;
				if (searchTerm.substring(0, i).equals(searchEntries[entryIndex].simplified.substring(0, i))) {
					String pinyinSearchTerm = searchTerm.substring(i);
					String pinyinSearchEntry = searchEntries[entryIndex].pinyinRaw.substring(
							1 + nthIndexOfChar(searchEntries[entryIndex].pinyinRaw, i, ' '))
							.replace(" ", "");
					if (searchMatches(pinyinSearchTerm, pinyinSearchEntry)
							|| pinyinSearchTerm.equals(pinyinSearchEntry))
						return true;
					String formatted = pinyinSearchEntry
							.replace(" ", "")
							.replace("1", "")
							.replace("2", "")
							.replace("3", "")
							.replace("4", "")
							.replace("5", "");
					return searchMatches(pinyinSearchTerm, formatted)
							|| pinyinSearchTerm.contentEquals(formatted);
				}
			}
			return false;
		}
		private final boolean pinyinMatchesBackwards(String searchTerm, int entryIndex) {
			for (int i = 0; i < searchTerm.length(); i++) {
				String substring = searchTerm.substring(i);
				for (int ii = 1; ii < searchEntries[entryIndex].simplified.length(); ii++) {
					if (substring.equals(searchEntries[entryIndex].simplified.substring(ii))) {
						String pinyinSearchTerm = searchTerm.substring(0, i);
						String pinyinSearchEntry = searchEntries[entryIndex].pinyinRaw.substring(0, 
								1 + nthLastIndexOfChar(searchEntries[entryIndex].pinyinRaw, ii, ' '))
								.replace(" ", "");
						if (searchMatches(pinyinSearchTerm, pinyinSearchEntry)
								|| pinyinSearchTerm.equals(pinyinSearchEntry))
							return true;
						String formatted = pinyinSearchEntry
								.replace(" ", "")
								.replace("1", "")
								.replace("2", "")
								.replace("3", "")
								.replace("4", "")
								.replace("5", "");
						return searchMatches(pinyinSearchTerm, formatted)
								|| pinyinSearchTerm.contentEquals(formatted);
					}
				}
			}
			return false;
		}
		private final int nthIndexOfChar(String str, int n, char ch) {
			int count = 0;
			for (int i = 0; i < str.length(); i++)
				if (str.charAt(i) ==  ch)
					if (++count == n)
						return i;
			return -1;
		}
		private final int nthLastIndexOfChar(String str, int n, char ch) {
			int count = 0;
			for (int i = str.length() - 1; i >= 0; i--)
				if (str.charAt(i) ==  ch)
					if (++count == n)
						return i;
			return -1;
		}
	}
	
	//Handwriting window class
	@Override
	void handwritingOption_click(boolean checked) {
		handwritingWindow.setVisible(checked);
	}
	final class HandwritingWindow extends JDialog {
		private static final long serialVersionUID = 1L;
		HandwritingWindow(JFrame mainWindow, JTextField textField, HanziLookup hanziLookup, Point location) {
			super(mainWindow);
			setResizable(false);
			setLocationRelativeTo(null);
			if (location != null)
				setLocation(location);
			setSize(230, 400);
			add(hanziLookup);
			addWindowListener(new WindowAdapter() {
			    @Override
			    public void windowClosing(WindowEvent e) {
			    	setVisible(false);
			    	handwritingOptionCheckbox.setSelected(false);
			    }
			});
			hanziLookup.addCharacterReceiver(new CharacterSelectionListener() { //enter selected character to text field
				@Override
				public void characterSelected(CharacterSelectionEvent e) {
					final int start = textField.getSelectionStart(), end = textField.getSelectionEnd();
					if (start == end) { //put selected character at carot
						textField.setText(
						new StringBuilder(textField.getText())
							.insert(start, e.getSelectedCharacter())
							.toString()
						);
					} else { //replace selection with character
						textField.setText(
						new StringBuilder(textField.getText())
							.replace(start, end, "")
							.insert(start, e.getSelectedCharacter())
							.toString()
						);
					}
					textField.requestFocus();
				}
			});
		}
	}
}
