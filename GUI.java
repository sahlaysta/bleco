package bleco;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;

//CC-CEDICT parser by me: https://github.com/sahlaysta/cc-cedict-parser
import com.github.sahlaysta.cccedict.CCCEDICTEntry;

import bleco.CompiledDictionary.TatoebaParser.ExampleSentence;
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
	
	Preferences preferences;
	HandwritingWindow handwritingWindow;
	Entry[] dictionary;
	Index[][] dissectedExampleSentences;
	WindowProperties windowProperties;
	Font font_GoogleNoto;
	
	public GUI() {
		setMinimumSize(new Dimension(230, 150));
		preferences = new Preferences();
		preferences.applyWindowProperties(this);
		
		setLoading(true);

		//Load Google Noto Sans font
		try {
			font_GoogleNoto = Font.createFont(Font.TRUETYPE_FONT,
					getClass().getResourceAsStream("resources/NotoSansSC-Regular.otf"));
			GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font_GoogleNoto);
		} catch (FontFormatException | IOException e) {
			e.printStackTrace();
		}
		
		try { //load Chinese dictionary
			dictionary = CompiledDictionary.decompile(getClass().getResourceAsStream("resources/compiledDict.dat"));
			dissectedExampleSentences = CompiledDictionary.decompileDissectedExampleSentences(getClass().getResourceAsStream("resources/compiledExamples.dat"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		HanziLookup hanziLookup = null;
		try { //load Chinese Handwriting window
			Font font = new JTextField().getFont().deriveFont(15f);
			hanziLookup = new HanziLookup(getClass().getResourceAsStream("handwriting/strokes-extended.dat"), font);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		setLoading(false);
		
		initComponent();
		
		//Apply preferences
		setCharacterOption(preferences.getCharacterOption());
		characterOption_changed(preferences.getCharacterOption());
		
		//Create handwritingWindow
		handwritingWindow = new HandwritingWindow(this, jTextField, hanziLookup, preferences.getHandwritingWindowLocation());
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
				} catch (Exception e2) {
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
		
		//Preference search field entry
		String searchFieldEntry = preferences.getSearchFieldEntry();
		jTextField.setText(searchFieldEntry);
		jTextField.setCaretPosition(searchFieldEntry.length());
	}
	
	// Simplified / traditional preference change
	int characterOption;
	void characterOption_changed(int option) {
		characterOption = option;
		jTextField_changed(jTextField.getText()); //refresh jTextField
	}
	
	// Search function (Update JList on Text field edit)
	SearchWorker searchWorker;
	@Override
	void jTextField_changed(String text) {
		if (searchWorker != null && !searchWorker.isDone()) {
			try {
				searchWorker.cancel();
			} catch(ConcurrentModificationException e) {
				searchWorker = null;
			}
		}
		
		//SwingWorker for search results
		final String searchTerm = text.toLowerCase().replace(" ", "");
		(searchWorker = new SearchWorker(searchTerm)).execute();
	}
	// Search results SwingWorker ///////////////////////////////////////////////////////
	static final class SearchEntry { //Search entry tuple class
		final String pinyin, pinyinTone, pinyinRaw;
		SearchEntry(String pinyin, String pinyinTone, String pinyinRaw) {
			this.pinyin = pinyin;
			this.pinyinTone = pinyinTone;
			this.pinyinRaw = pinyinRaw;
		}
		static SearchEntry fromCCCEDICTEntry(CCCEDICTEntry entry) {
			return new SearchEntry(
					formatPinyin(entry.pronunciation).replace("u:", "v"),
					entry.pronunciation.replace(" ", "").toLowerCase().replace("u:", "v"),
					entry.pronunciation.replace("u:", "v")
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
		final List<Entry> add = new ArrayList<>(), add2 = new ArrayList<>(), add3 = new ArrayList<>(),
				add4 = new ArrayList<>(), add5 = new ArrayList<>(), add6 = new ArrayList<>();
		public Entry[] options;
		boolean canceled = false;
		SearchWorker(String searchTerm) {
			super();
			this.searchTerm = searchTerm;
		}
		public void cancel() {
			canceled = true;
			super.cancel(true);
		}
		@Override
		protected Object doInBackground() { //Match search results to add to JList
			if (!searchTerm.contains("*")) {
				for (Entry e: dictionary)
					if (searchTerm.equals(e.searchEntry.pinyin)
							|| searchTerm.equals(e.searchEntry.pinyinTone)
							|| searchTerm.equals(e.simplified)
							|| searchTerm.equals(e.traditional))
						if (Character.isLowerCase(e.styledPinyin.charAt(0)))
							add.add(e);
						else add4.add(e);
					else if (searchMatches(searchTerm, e.searchEntry.pinyin)
							|| searchMatches(searchTerm, e.searchEntry.pinyinTone)
							|| searchMatches(searchTerm, e.simplified)
							|| searchMatches(searchTerm, e.traditional))
						if (Character.isLowerCase(e.styledPinyin.charAt(0)))
							add2.add(e);
						else add5.add(e);
					else if (pinyinMatches(searchTerm, e, true)
							|| pinyinMatchesBackwards(searchTerm, e, true)
							|| pinyinMatches(searchTerm, e, false)
							|| pinyinMatchesBackwards(searchTerm, e, false))
						if (Character.isLowerCase(e.styledPinyin.charAt(0)))
							add3.add(e);
						else add6.add(e);
				if (getTotalSize() == 0) {
					for (Index ind: dissectChineseSentence(searchTerm)) {
						String chinese = searchTerm.substring(ind.start, ind.end);
						if (chinese.length() == 1)
							add.add(findHighestEntry(chinese));
						else
							add.add(findEntry(chinese));
					}
				}
			} else { //wildcard search
				for (Entry e: dictionary)
					if (wildcardMatch(searchTerm, e.simplified)
						|| wildcardMatch(searchTerm, e.traditional))
						add.add(e);
			}
			return null;
		}
		
		@Override
		protected void done() { //Add results to JList
			if (canceled)
				return;
			listModel.addElement("");
			jList.setSelectedIndex(1);
			listModel.clear();
			if (searchTerm.length() == 0) {
				listModel.addElement("Enter a search term...");
				jList.setEnabled(false);
				return;
			} else jList.setEnabled(true);
			switch (characterOption) {
			//Populate JList
			case CHARACTER_SIMPLIFIED: {
				for (Entry e: add)
					listModel.addElement(e.toStringSimplified);
				for (Entry e: add2)
					listModel.addElement(e.toStringSimplified);
				for (Entry e: add3)
					listModel.addElement(e.toStringSimplified);
				for (Entry e: add4)
					listModel.addElement(e.toStringSimplified);
				for (Entry e: add5)
					listModel.addElement(e.toStringSimplified);
				for (Entry e: add6)
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
				for (Entry e: add4)
					listModel.addElement(e.toStringTraditional);
				for (Entry e: add5)
					listModel.addElement(e.toStringTraditional);
				for (Entry e: add6)
					listModel.addElement(e.toStringTraditional);
				break;
			}
			}
			if (listModel.getSize() == 0) {
				jList.setEnabled(false);
				listModel.addElement("No results found");
			}
			
			//Create selections
			int i = 0;
			options = new Entry[getTotalSize()];
			for (Entry e: add)
				options[i++] = e;
			for (Entry e: add2)
				options[i++] = e;
			for (Entry e: add3)
				options[i++] = e;
			for (Entry e: add4)
				options[i++] = e;
			for (Entry e: add5)
				options[i++] = e;
			for (Entry e: add6)
				options[i++] = e;
		}
		int getTotalSize() {
			return add.size() + add2.size() + add3.size() + add4.size() + add5.size() + add6.size();
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
		private final boolean pinyinMatches(String searchTerm, Entry entry, boolean simplified) {
			/*
			 * Search functionality for search results that start with a Chinese character.
			 * Example:
			 * "滑ban" matches "滑板" (pronounced huaban)
			 */
			String chinese = simplified ? entry.simplified : entry.traditional;
			for (int i = searchTerm.length() - 1; i > 0; i--) {
				if (i >= chinese.length())
					continue;
				if (searchTerm.substring(0, i).equals(chinese.substring(0, i))) {
					String pinyinSearchTerm = searchTerm.substring(i);
					String pinyinSearchEntry = entry.searchEntry.pinyinRaw.substring(
							1 + nthIndexOfChar(entry.searchEntry.pinyinRaw, i, ' '))
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
		private final boolean pinyinMatchesBackwards(String searchTerm, Entry entry, boolean simplified) {
			/*
			 * Search functionality for search results that end with a Chinese character.
			 * Example:
			 * "hua板" matches "滑板" (pronounced huaban)
			 */
			String chinese = simplified ? entry.simplified : entry.traditional;
			for (int i = 0; i < searchTerm.length(); i++) {
				String substring = searchTerm.substring(i);
				for (int ii = 1; ii < chinese.length(); ii++) {
					if (substring.equals(chinese.substring(ii))) {
						String pinyinSearchTerm = searchTerm.substring(0, i);
						String pinyinSearchEntry = entry.searchEntry.pinyinRaw.substring(0, 
								1 + nthIndexOfChar(entry.searchEntry.pinyinRaw, ii, ' '))
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
	
	// Pinyin priority method //////////////////////////////////////////////////////////////////
	Entry findHighestEntry(String chinese) {
		/*
		 * If a Chinese character has multiple pronunciations,
		 * get the most common one.
		 * 
		 * Example: with "强", prefer "qiang" over "jiang"
		 */
		final String pinyin = getPinyin(chinese);
		for (Entry entry: dictionary) {
			if (entry.styledPinyin.equals(pinyin) && entry.simplified.equals(chinese))
				return entry;
			else if (entry.styledPinyin.equals(pinyin) && entry.traditional.equals(chinese))
				return entry;
		}
		return null;
	}
	static class CountMap<T> {
		private Map<T, Integer> map = new HashMap<>();
		public void add(T e) {
			map.put(e, 1 + get(e));
		}
		public int get(T e) {
			return map.getOrDefault(e, 0);
		}
		public T getHighest() {
			T result = null;
			int highest = Integer.MIN_VALUE;
			for (java.util.Map.Entry<T, Integer> set: map.entrySet()) {
				int value = set.getValue();
				if (value > highest) {
					highest = value;
					result = set.getKey();
				}
			}
			return result;
		}
	}
	final SearchWorker sw = new SearchWorker(null);
	String getPinyin(String chinese) {
		CountMap<String> cm = new CountMap<>();
		for (Entry e: dictionary) {
			int i = e.simplified.indexOf(chinese);
			if (i == -1)
				if ((i = e.traditional.indexOf(chinese)) == -1)
						continue;
			String pinyin;
			if (i == 0)
				pinyin = e.styledPinyin;
			else
				pinyin = e.styledPinyin.substring(1 + sw.nthIndexOfChar(e.styledPinyin, i, ' '));
			if ((i = pinyin.indexOf(' ')) != -1)
				pinyin = pinyin.substring(0, i);
			cm.add(pinyin);
		}
		return cm.getHighest();
	}
	
	//Handwriting window class /////////////////////////////////
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
					textField.setCaretPosition(1 + start);
				}
			});
		}
	}
	
	// Double-click Dictionary Entry window ////////////////////////////////////////////////////////////
	static final int MAX_EXAMPLE_SENTENCES = 10;
	@Override
	void jList_doubleClick(int index) {
		showEntryWindow(searchWorker.options[index]);
	}
	static final Insets ITEM_INSETS = new Insets(2, -20, 2, 2);
	static final KeyStroke CTRL_C = KeyStroke.getKeyStroke("control C");
	void showEntryWindow(Entry entry) {
		/*
		 * Show the entry window of a Dictionary entry
		 */
		final boolean simplified = characterOption == CHARACTER_SIMPLIFIED;
		JDialog window = new JDialog(this, true);
		window.setTitle(TITLE);
		JTextPane jTextPane = new JTextPane();
		jTextPane.setEditable(false);
		jTextPane.setContentType("text/html");
		jTextPane.setText(createHtml(entry, simplified));
		jTextPane.setCaretPosition(0);
		JScrollPane jScrollPane = new JScrollPane(jTextPane);
		jScrollPane.setBorder(new EmptyBorder(0,0,0,0));
		window.add(jScrollPane);
		window.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		MouseListener mouseListener = new MouseListener() {
			@Override public void mouseClicked(MouseEvent arg0) {}
			@Override public void mouseEntered(MouseEvent arg0) {}
			@Override public void mouseExited(MouseEvent arg0) {}
			@Override public void mousePressed(MouseEvent arg0) {}
			@Override public void mouseReleased(MouseEvent arg0) {
				/*
				 * Open context menu when a Chinese word
				 * is selected
				 */
				if (arg0.getButton() != MouseEvent.BUTTON1)
					return;
				String selection = jTextPane.getSelectedText();
				if (selection == null)
					return;
				
				// Try to match selection
				Entry match = null;
				if (selection.length() == 1) {
					match = findHighestEntry(selection);
					if (Character.isUpperCase(match.styledPinyin.charAt(0)))
							match = null;
				}
				if (match == null) {
					if (simplified) {
						for (Entry entry: dictionary) {
							if (entry.simplified.equals(selection)) {
								match = entry;
								break;
							}
						}
					}
					else {
						for (Entry entry: dictionary)
							if (entry.traditional.equals(selection)) {
								match = entry;
								break;
							}
					}
				}
				
				// Show popup menu
				if (match != null) {
					JPopupMenu menu = new JPopupMenu();
						JMenuItem pinyinItem = new JMenuItem(selection + ": " + match.styledPinyin);
							pinyinItem.setMargin(ITEM_INSETS);
							menu.add(pinyinItem);
						for (int i = 0; i < match.definitions.length && i < 3; i++) {
							JMenuItem definitionItem = new JMenuItem("- " + Entry.styleDefinition(match.definitions[i], simplified));
							definitionItem.setMargin(ITEM_INSETS);
							menu.add(definitionItem);
						}
						menu.addSeparator();
						JMenuItem copyItem = new JMenuItem("Copy");
							copyItem.setMargin(ITEM_INSETS);
							copyItem.setAccelerator(CTRL_C);
							copyItem.addActionListener(new ActionListener() {
								@Override
								public void actionPerformed(ActionEvent arg0) { //Copy selection to clipboard
									java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
							        .setContents(new java.awt.datatransfer.StringSelection(selection), null);
								}
							});
							menu.add(copyItem);
					menu.show(jTextPane, arg0.getX(), arg0.getY() + 25);
				}
			}
		};
		jTextPane.addMouseListener(mouseListener);
		jTextPane.addHyperlinkListener(new HyperlinkListener() {
			@Override
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if (e.getEventType() != HyperlinkEvent.EventType.ACTIVATED)
					return;
				Element elmnt = e.getSourceElement();
				jTextPane.setSelectionStart(elmnt.getStartOffset());
				jTextPane.setSelectionEnd(elmnt.getEndOffset());
				Rectangle mousePos = null;
				try {
					mousePos = jTextPane.modelToView(elmnt.getEndOffset());
				} catch (BadLocationException e2) {
					e2.printStackTrace();
				}
				MouseEvent me = new MouseEvent(jTextPane, -1, -1, -1, mousePos.x, mousePos.y, -1, -1, 1, false, MouseEvent.BUTTON1);
				mouseListener.mouseReleased(me);
			}
		});
		window.setSize(300, 250);
		window.setLocationRelativeTo(this);
		window.setVisible(true);
	}
	String createHtml(Entry entry, boolean simplified) {
		/*
		 * Create the JTextPane html
		 * to use for the Entry window
		 */
		final boolean differs = !entry.simplified.equals(entry.traditional);
		
		StringBuilder html = new StringBuilder();
		if (simplified) {
			html.append("<font face=\"" + font_GoogleNoto.getFamily() + "\"size=\"6\">" + entry.simplified + "<br></font>");
			if (differs)
				html.append("<font face=\"" + font_GoogleNoto.getFamily() + "\"size=\"5\">" + entry.traditional + "<br></font>");
		} else {
			html.append("<font face=\"" + font_GoogleNoto.getFamily() + "\"size=\"6\">" + entry.traditional + "<br></font>");
			if (differs)
				html.append("<font face=\"" + font_GoogleNoto.getFamily() + "\"size=\"5\">" + entry.simplified + "<br></font>");
		}
		html.append("<hr><font face=\"" + font_GoogleNoto.getFamily() + "\" size=\"5\">" + entry.styledPinyin + "<br></font>");
		html.append("<font face=\"" + font_GoogleNoto.getFamily() + "\" size=\"4\">");
		for (String definition: entry.definitions) {
			html.append("- " + Entry.styleDefinition(definition, simplified) + "<br>");
		}
		Set<String> addedSentences = new HashSet<>(); //prevent duplicates
		int i = 0;
		for (int ii: entry.exampleSentences) {
			if (i >= MAX_EXAMPLE_SENTENCES)
				break;
			ExampleSentence es = Entry.EXAMPLE_SENTENCES.get(ii);
			String chineseSentence = simplified ? es.simplified : es.traditional;
			if (addedSentences.contains(chineseSentence)) {
				i++;
				continue;
			}
			addedSentences.add(chineseSentence);
			html.append("<font face=\"" + jTextField.getFont().getFamily() + "\" size=\"4\">");
			html.append("<br>" + es.english);
			html.append("</font><font face=\"" + font_GoogleNoto.getFamily() + "\" size=\"5\">");
			html.append("<br>" + dissect(chineseSentence, dissectedExampleSentences[ii]) + "<br></font>");
			i++;
		}
		return html.toString();
	}
	static final class Index {
		final int start, end;
		Index(int start,int end){this.start=start;this.end=end;}
	}
	String dissect(String chinese, Index[] indexes) {
		StringBuilder sb = new StringBuilder();
		final int length = chinese.length();
		for (int i = 0; i < length;) {
			for (Index ind: indexes) {
				if (ind.start == i) {
					String word = chinese.substring(ind.start, ind.end);
					sb.append("<a href=\"");
					sb.append(word);
					sb.append("\">");
					sb.append(word);
					sb.append("</a>");
					i += ind.end - ind.start;
					continue;
				}
			}
			if (i >= length)
				break;
			sb.append(chinese.charAt(i++));
		}
		return sb.toString();
	}
	List<Index> dissectChineseSentence(String chinese) {
		/*
		 * Separate a Chinese sentence into
		 * dictionary entries.
		 * 
		 * Example:
		 * sentence = "他们是热爱和平的人。"
		 * 
		 * parts = "他们", "是", "热爱", "和平", "的", "人"
		 */
		List<Index> indexes = new ArrayList<>();
		for (int i = chinese.length(); i >= 0; i--)
			for (int ii = 0; ii < i; ii++) {
				String substr = chinese.substring(ii, i);
				Entry entry = findEntry(substr);
				if (entry != null) {
					int length = entry.simplified.length();
					indexes.add(0, new Index(ii, ii + length));
					i -= length - 1;
					break;
				}
			}
		return indexes;
	}
	Entry findEntry(String chinese) {
		for (Entry entry: dictionary) {
			if (entry.simplified.equals(chinese))
				return entry;
			else if (entry.traditional.equals(chinese))
				return entry;
		}
		return null;
	}
	static List<Integer> indexesOfSubstring(String str, String substr){
		List<Integer> result = new ArrayList<Integer>();
		int lastIndex = 0;
		while(lastIndex != -1) {
			lastIndex = str.indexOf(substr, lastIndex);
			if(lastIndex != -1){
		        result.add(lastIndex);
		        lastIndex += 1;
		    }
		}
		return result;
	}
	
	//About option click //////////////////////////////////////////////////////////////////////////////////////////
	@Override
	void aboutOption_click() {
		JOptionPane.showMessageDialog(
				this,
				"Bleco v1.0c-alpha written by porog\n"
						+ "https://github.com/sahlaysta/bleco\n\n"
						+ "CC-CEDICT belongs to MDBG: https://cc-cedict.org/wiki/\n"
						+ "Example sentences belong to Tatoeba: https://tatoeba.org/en/\n"
						+ "HanziLookup belongs to Jordan Kiang: https://www.kiang.org/jordan/software/hanzilookup/\n"
						+ "ZHConverter belongs to Google: https://code.google.com/archive/p/java-zhconverter/\n"
						+ "Google Noto Sans Font belongs to Google: https://www.google.com/get/noto/#sans-lgc",
				"About",
				JOptionPane.INFORMATION_MESSAGE);
	}
}
