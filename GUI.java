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
import javax.swing.JOptionPane;
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
	
	static final class Entry {
		//Entry tuple class
		final CCCEDICTEntry entry;
		final String styledPinyin, toStringSimplified, toStringTraditional;
		Entry(CCCEDICTEntry entry) {
			this.entry = entry;
			this.styledPinyin = CCCEDICTPinyin.toFormattedPinyin(entry);
			this.toStringSimplified =
					entry.simplified + " "
					+ styledPinyin + " - "
					+ styleDefinition(entry.definitions.get(0), true);
			this.toStringTraditional =
					entry.traditional + " "
					+ styledPinyin + " - "
					+ styleDefinition(entry.definitions.get(0), false);
		}
		private static String styleDefinition(String definition, boolean simplified) {
			/*
			 * Split the '|' separator in a definition String.
			 * 
			 * Example:
			 * definition = "Linnei township in Yunlin county 雲林縣|云林县, Taiwan"
			 * output (simplified == true) = "Linnei township in Yunlin county 云林县, Taiwan"
			 * output (simplified == false) = "Linnei township in Yunlin county 雲林縣, Taiwan"
			 */
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < definition.length(); i++) {
				char c = definition.charAt(i);
				if (c == '|') {
					if (simplified) {
						int sbLength = sb.length();
						int traditionalStartIndex = sbLength - 1;
						while (sb.charAt(traditionalStartIndex--) > 125) {
							if (traditionalStartIndex < 0) {
								traditionalStartIndex=-2;
								break;
							}
						}
						sb.replace(traditionalStartIndex += 2, sbLength, "");
						continue;
					} else {
						do {
							if (i >= definition.length() - 1) {
								i = definition.length();
								break;
							} 
						} while (definition.charAt(++i) > 125);
						i--;
						continue;
					}
				}
				if (c == '[') {
					while (definition.charAt(++i) != ']');
					continue;
				}
				sb.append(c);
			}
			return sb.toString();
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
				dictionary[i] = new Entry(entry);
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
				int i = 0;
				for (SearchEntry searchEntry: searchEntries) {
					if (searchTerm.equals(searchEntry.pinyin)
							|| searchTerm.equals(searchEntry.pinyinTone)
							|| searchTerm.equals(searchEntry.simplified)
							|| searchTerm.equals(searchEntry.traditional)) {
						add.add(dictionary[i]);
					}
					else if (searchMatches(searchTerm, searchEntry.pinyin)
							|| searchMatches(searchTerm, searchEntry.pinyinTone)
							|| searchMatches(searchTerm, searchEntry.simplified)
							|| searchMatches(searchTerm, searchEntry.traditional)) {
						add2.add(dictionary[i]);
					}
					else if (pinyinMatches(searchTerm, i, true)
							|| pinyinMatchesBackwards(searchTerm, i, true)
							|| pinyinMatches(searchTerm, i, false)
							|| pinyinMatchesBackwards(searchTerm, i, false)) {
						add3.add(dictionary[i]);
					}
					i++;
				}
			} else { //wildcard search
				int i = 0;
				for (SearchEntry searchEntry: searchEntries) {
					if (wildcardMatch(searchTerm, searchEntry.simplified)
						|| wildcardMatch(searchTerm, searchEntry.traditional))
						add.add(dictionary[i]);
					i++;
				}
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
			switch (characterOption) {
			case CHARACTER_SIMPLIFIED: {
				for (Entry e: add)
					listModel.addElement(e.toStringSimplified);
				for (Entry e: add2)
					listModel.addElement(e.toStringSimplified);
				for (Entry e: add3)
					listModel.addElement(e.toStringSimplified);
				break;
			}
			case CHARACTER_TRADITIONAL: {
				for (Entry e: add)
					listModel.addElement(e.toStringTraditional);
				for (Entry e: add2)
					listModel.addElement(e.toStringTraditional);
				for (Entry e: add3)
					listModel.addElement(e.toStringTraditional);
				break;
			}
			}
			if (listModel.getSize() == 0) {
				jList.setEnabled(false);
				listModel.addElement("No results found");
			}
		}
		
		///// Search result methods //////////
		private final boolean searchMatches(String searchTerm, String entryString) {
			/*
			 * For 'Auto-fill' search functionality
			 * Example:
			 * pinyinOfTheEntryBeingCompared = "huashu";
			 * "huas" will match the entry
			 * "hua" also matches it
			 * "hu" also
			 */
			if (entryString.length() <= searchTerm.length())
				return false;
			return entryString.substring(0, searchTerm.length()).equals(searchTerm);
		}
		private final boolean pinyinMatches(String searchTerm, int entryIndex, boolean simplified) {
			/*
			 * Search functionality for search results that start with a Chinese character.
			 * Example:
			 * "滑ban" matches "滑板" (pronounced huaban)
			 */
			String chinese = simplified ? searchEntries[entryIndex].simplified : searchEntries[entryIndex].traditional;
			for (int i = searchTerm.length() - 1; i > 0; i--) {
				if (i >= chinese.length())
					continue;
				if (searchTerm.substring(0, i).equals(chinese.substring(0, i))) {
					String pinyinSearchTerm = searchTerm.substring(i);
					String pinyinSearchEntry = searchEntries[entryIndex].pinyinRaw.substring(
							1 + nthIndexOfChar(searchEntries[entryIndex].pinyinRaw, i, ' '))
							.replace(" ", "");
					if (searchMatches(pinyinSearchTerm, pinyinSearchEntry)
							|| pinyinSearchTerm.equals(pinyinSearchEntry))
						return true;
					String formatted = SearchEntry.formatPinyin(pinyinSearchEntry);
					return searchMatches(pinyinSearchTerm, formatted)
							|| pinyinSearchTerm.contentEquals(formatted);
				}
			}
			return false;
		}
		private final boolean pinyinMatchesBackwards(String searchTerm, int entryIndex, boolean simplified) {
			/*
			 * Search functionality for search results that end with a Chinese character.
			 * Example:
			 * "hua板" matches "滑板" (pronounced huaban)
			 */
			String chinese = simplified ? searchEntries[entryIndex].simplified : searchEntries[entryIndex].traditional;
			for (int i = 0; i < searchTerm.length(); i++) {
				String substring = searchTerm.substring(i);
				for (int ii = 1; ii < chinese.length(); ii++) {
					if (substring.equals(chinese.substring(ii))) {
						String pinyinSearchTerm = searchTerm.substring(0, i);
						String pinyinSearchEntry = searchEntries[entryIndex].pinyinRaw.substring(0, 
								1 + nthIndexOfChar(searchEntries[entryIndex].pinyinRaw, ii, ' '))
								.replace(" ", "");

						if (searchMatches(pinyinSearchTerm, pinyinSearchEntry)
								|| pinyinSearchTerm.equals(pinyinSearchEntry))
							return true;
						String formatted = SearchEntry.formatPinyin(pinyinSearchEntry);
						return searchMatches(pinyinSearchTerm, formatted)
								|| pinyinSearchTerm.equals(formatted);
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
		private boolean wildcardMatch(String wildCardSearch, String chinese) {
			/*
			 * Matches wild card search results.
			 * Example:
			 * string = "中*";
			 * The string will match any dictionary entry with a string length of 2, and whose first character equals 中
			 */
			int length = wildCardSearch.length();
			if (length != chinese.length())
				return false;
			for (int i = 0; i < length; i++) {
				final char c = wildCardSearch.charAt(i);
				if (c != chinese.charAt(i) && c != '*')
					return false;
			}
			return true;
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
	
	//About option click
	@Override
	void aboutOption_click() {
		JOptionPane.showMessageDialog(
				this,
				"Bleco written by porog\n"
						+ "https://github.com/sahlaysta/bleco\n\n"
						+ "CC-CEDICT belongs to MDBG: https://cc-cedict.org/wiki/\n\n"
						+ "HanziLookup belongs to Jordan Kiang: https://www.kiang.org/jordan/software/hanzilookup/",
				"About",
				JOptionPane.INFORMATION_MESSAGE);
	}
}
