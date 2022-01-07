package com.github.sahlaysta.bleco.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JLayeredPane;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;

import com.github.sahlaysta.bleco.dict.Entry;
import com.github.sahlaysta.bleco.dict.ExampleSentence;
import com.github.sahlaysta.bleco.dict.Match;
import com.github.sahlaysta.bleco.dict.SearchResult;

// displays entry popup over the searcher
final class GUISearcherEntryPopup {
	
	//Constructor
	final GUI gui;
	GUISearcherEntryPopup(GUI gui) {
		this.gui = gui;
	}
	
	
	//entry pane
	JPanel entryPane;
	
	//Show entry popup
	void open(Entry entry) {
		if (entryPane != null)
			return;
		
		//Popup pane layout
		JLayeredPane jlp = gui.searcher;
		entryPane = new JPanel();
		entryPane.setOpaque(false);
		entryPane.setLayout(new GridLayout(1,1));
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.setBackground(new Color(0, 0, 0, 100));
		panel.setBorder(new EmptyBorder(40, 40, 40, 40));
		entryPane.add(panel);
		jlp.setLayer(entryPane, JLayeredPane.PALETTE_LAYER);
		jlp.add(entryPane);
		
		setThingsEnabled(false);
		
		//Html text pane
		JTextPane textPane = new JTextPane();
		textPane.setPreferredSize(new Dimension(200, 100));
		textPane.setEditable(false);
		textPane.setContentType("text/html");
		createEntryHtml(entry, textPane);
		textPane.setCaretPosition(0);
		JScrollPane jScrollPane = new JScrollPane(textPane);
		
		panel.add(jScrollPane, BorderLayout.CENTER);
		
		
		//keyboard controls
		//Backspace, ESC, Enter, and Tab keys close entry
		textPane.setFocusTraversalKeysEnabled(false);
		textPane.getActionMap()
		.put("blecoentryclose", new AbstractAction("blecoentryclose") {
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e) {
				if (entryPane != null)
					close();
			}
		});
		for (int keyCode: new int[] { 
				KeyEvent.VK_BACK_SPACE,
				KeyEvent.VK_ESCAPE,
				KeyEvent.VK_ENTER,
				KeyEvent.VK_TAB} ) {
			textPane.getInputMap().put(
					KeyStroke.getKeyStroke(keyCode, 0, false),
					"blecoentryclose");
		}
		
		//Up arrow and down arrow keys scroll the JScrollPane
		String scrollUp = "blecoentryscrollup";
		String scrollDown = "blecoentryscrolldown";
		int scrollIncrement = 20;
		textPane.getActionMap()
		.put(scrollUp, new AbstractAction(scrollUp) {
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e) {
				JScrollBar vsb = jScrollPane.getVerticalScrollBar();
				vsb.setValue(vsb.getValue() - scrollIncrement);
			}
		});
		textPane.getInputMap().put(
			KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0, false),
			scrollUp);
		textPane.getActionMap()
		.put(scrollDown, new AbstractAction(scrollDown) {
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e) {
				JScrollBar vsb = jScrollPane.getVerticalScrollBar();
				vsb.setValue(vsb.getValue() + scrollIncrement);
			}
		});
		textPane.getInputMap().put(
			KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0, false),
			scrollDown);
		
		//Show
		gui.revalidate();
		gui.repaint();
		textPane.requestFocus();
	}
	
	//Close entry popup
	void close() {
		if (entryPane == null)
			return;
		gui.searcher.remove(entryPane);
		entryPane = null;
		setThingsEnabled(true);
		gui.searcher.searchField.requestFocus();
		gui.revalidate();
		gui.repaint();
	}
	
	//disable while entry popup is open
	private void setThingsEnabled(boolean enabled) {
		gui.menuBar.simplified.setEnabled(enabled);
		gui.menuBar.traditional.setEnabled(enabled);
		gui.menuBar.engSearch.setEnabled(enabled);
		gui.menuBar.font.setEnabled(enabled);
		gui.searcher.searchResultListScroll
		.setWheelScrollingEnabled(enabled);
	}
	
	//create entry popup html
	private void createEntryHtml(Entry entry, JTextPane jTextPane) {
		boolean smpl =
			Entry.getCharacterType()
			== Entry.SIMPLIFIED_CHINESE;
		
		//Create JTextPane HTML for Entry
		StringBuilder html = new StringBuilder();
		String fontName = gui.searcher.searchField.getFont().getFamily();
		
		//Title
		html.append("<font face=\"");
		html.append(fontName);
		html.append("\"size=\"6\">");
		if (entry.simplified.equals(entry.traditional)) {
			html.append(entry.simplified);
		} else {
			html.append(smpl ? entry.simplified : entry.traditional);
			html.append("<br></font><font face=\"");
			html.append(fontName);
			html.append("\"size=\"5\">");
			html.append(!smpl ? entry.simplified : entry.traditional);
		}
		html.append("<br></font><hr>");
		
		//Subtitle (entry pronunciation)
		html.append("<font face=\"");
		html.append(fontName);
		html.append("\"size=\"5\">");
		html.append(entry.formattedPinyin);
		html.append("<br></font>");
		
		//Body (entry English definitions)
		html.append("<font face=\"");
		html.append(fontName);
		html.append("\"size=\"4\">");
		for (String definition: entry.formattedDefinitions()) {
			html.append("- ");
			html.append(definition);
			html.append("<br>");
		}
		
		//Body 2 (Example sentences)
		if (entry.exampleSentences != null) {
			
			/* if simplified chinese, show simplified chinese
			 * example sentences before traditional ones
			 * and vice versa */
			List<ExampleSentence> sortedExampleSentences
				= new ArrayList<>(entry.exampleSentences.length);
			if (!entry.simplified.equals(entry.traditional)) {
				int top = 0;
				for (ExampleSentence es: entry.exampleSentences) {
					if (es.chineseSentence.contains(
						smpl
							? entry.simplified
							: entry.traditional))
						sortedExampleSentences.add(top++, es);
					else
						sortedExampleSentences.add(es);
				}
			} else {
				for (ExampleSentence es: entry.exampleSentences)
					sortedExampleSentences.add(es);
			}
			
			//add example sentences to html
			for (ExampleSentence es: sortedExampleSentences) {
				
				//The English translation of the example sentence
				html.append("<font face=\"");
				html.append(fontName);
				html.append("\"size=\"4\"><br>");
				html.append(es.englishSentence);
				html.append("</font><font face=\"");
				html.append(fontName);
				
				//The Chinese example sentence
				html.append("\"size=\"5\"><br><a href=\"a\"style=\""
					+ "text-decoration: none;\"><span style=\""
					+ "color:rgb(24, 54, 204);\">");
				String chinese = es.chineseSentence;
				/* Add "<span>" and "</span>" before and after each
				 * character in the string. it improves the
				 * accuracy of java's getIndexAtPoint method */
				for (int ii = 0; ii < chinese.length(); ) {
					//iterate string codepoints
					int codePoint = chinese.codePointAt(ii);
					
					/* the if statement:
					 * highlight each occurrence of this
					 * dictionary entry in the example sentence
					 * to red */
					boolean smplOccurrence
						= codePoint == entry.simplified.codePointAt(0)
						&& chinese.indexOf(entry.simplified, ii) == ii;
					boolean tradOccurrence
						= !smplOccurrence
						&& codePoint == entry.traditional.codePointAt(0)
						&& chinese.indexOf(entry.traditional, ii) == ii;
					if (smplOccurrence || tradOccurrence) {
						html.append("<span style=\"color:red;\">");
						String occurrence
							= smplOccurrence
								? entry.simplified
								: entry.traditional;
						for (int iii = 0; iii < occurrence.length(); ) {
							//iterate string codepoints
							String codePointStr
								= occurrence.substring(
									iii,
									iii + Character.charCount(
										occurrence
											.codePointAt(iii)));
							html.append("<span>");
							html.append(codePointStr);
							html.append("</span>");
							iii += codePointStr.length();
						}
						html.append("</span>");
						ii += occurrence.length();
					} else {
						String codePointStr
							= chinese.substring(
								ii,
								ii + Character.charCount(codePoint));
						html.append("<span>");
						html.append(codePointStr);
						html.append("</span>");
						ii += codePointStr.length();
					}
				}
				html.append("</span></a><br></font>");
			}
		}

		jTextPane.setText(html.toString());
		
		//Mouse click listeners
		/* Auto select clicked word on Chinese sentence click
		 * e.g. clicking the 们 in 我们 selects the
		 * whole word, 我们
		 * */
		jTextPane.addHyperlinkListener(e -> {
			if (e.getEventType() != HyperlinkEvent.EventType.ACTIVATED
				|| !(e.getInputEvent() instanceof MouseEvent))
				return;
			MouseEvent me = (MouseEvent) e.getInputEvent();
			int clickedIndex = jTextPane.getAccessibleContext()
				.getAccessibleText().getIndexAtPoint(me.getPoint());
			
			// Find the substring of the clicked sentence
			int startIndex = -1, endIndex = -1;
			String text = null;
			try {
				text = jTextPane.getDocument()
					.getText(0, jTextPane.getDocument().getLength());
			} catch (BadLocationException e2) {
				e2.printStackTrace();
				return;
			}
			//get start index (any space character before the clicked index)
			for (int i = 0; i < clickedIndex; ) { //iterate string codepoints
				int codePoint = text.codePointAt(i);
				if (codePoint == ' ')
					startIndex = i + 1;
				i += Character.charCount(codePoint);
			}
			//get end index (any space character after the clicked index)
			for (int i = clickedIndex; i < text.length(); ) {
				int codePoint = text.codePointAt(i);
				if (codePoint == ' ') {
					endIndex = i;
					break;
				}
				i += Character.charCount(codePoint);
			}
			
			//Find the Chinese word of the clicked character
			String clickedSentence = text.substring(startIndex, endIndex);
			int clickedIndexInSentence = clickedIndex - startIndex;
			if (clickedIndexInSentence >= clickedSentence.length()) {
				// bad index
				jTextPane.setSelectionStart(clickedIndex);
				jTextPane.setSelectionEnd(clickedIndex + 1);
				return;
			}
			Match match = gui.dict.findChineseWord(
				clickedSentence, clickedIndexInSentence);
			if (match == null) {
				// no match
				jTextPane.setSelectionStart(clickedIndex);
				jTextPane.setSelectionEnd(clickedIndex + 1);
				return;
			}
			
			//Select the match
			int selStart = startIndex + match.index;
			int selEnd = startIndex + match.index + match.str.length();
			jTextPane.setSelectionStart(selStart);
			jTextPane.setSelectionEnd(selEnd);
			
			//Show entry popup context menu
			showEntryContextMenu(match.entry, jTextPane, me);
		});
		
		/* When Chinese text is selected, show a small popup
		 * context menu showing its pronunciation and
		 * definitions */
		jTextPane.addMouseListener(new MouseAdapter() {
			//listen to changes in selected text by mouse
			@Override
			public void mouseReleased(MouseEvent e) {
				hasSelChanged(e);
			}
			
			private boolean first = true;
			private int prevSelStart, prevSelEnd;
			private void hasSelChanged(MouseEvent e) {
				int selStart = jTextPane.getSelectionStart();
				int selEnd = jTextPane.getSelectionEnd();
				if (first ||
					(prevSelStart != selStart
					|| prevSelEnd != selEnd))
					selChanged(e, selStart, selEnd);
				prevSelStart = selStart;
				prevSelEnd = selEnd;
				if (first)
					first = false;
			}
			
			private void selChanged(MouseEvent e, int selStart, int selEnd) {
				if (selStart == selEnd)
					return;
				
				//Get selected text
				String selectedText = null;
				try {
					selectedText = jTextPane.getDocument()
						.getText(selStart, selEnd - selStart);
				} catch (BadLocationException e2) {
					e2.printStackTrace();
				}
				if (selectedText == null || selectedText.isEmpty())
					return;
				
				//Get Dictionary entry from selected text
				Entry selectedEntry = null;
				List<SearchResult> results = gui.dict.search(selectedText);
				if (results == null)
					return;
				for (SearchResult sr: results) {
					if (sr.entry.simplified.equals(selectedText)
						|| sr.entry.traditional.equals(selectedText)) {
						selectedEntry = sr.entry;
						break;
					}
				}
				if (selectedEntry == null)
					return;
				
				//Show entry popup context menu
				showEntryContextMenu(selectedEntry, jTextPane, e);
			}
		});
	}
	private static void
	showEntryContextMenu(Entry entry, Component invoker, MouseEvent e) {
		/* Create a small popup context menu for entry with
		 * pronunciation and definitions */
		JPopupMenu jPopupMenu = new JPopupMenu();
		
		//Add entry simplified/traditional
		String entryText = Entry.getCharacterType()
			== Entry.SIMPLIFIED_CHINESE
				? entry.simplified
				: entry.traditional;
		jPopupMenu.add(entryText + " - " + entry.formattedPinyin);
		
		//Add entry definitions (3 max)
		int addCount = 0;
		for (String definition: entry.formattedDefinitions()) {
			jPopupMenu.add("- " + definition);
			if (++addCount >= 3)
				break;
		}
		
		//Ctrl C to copy to clipboard
		JMenuItem copy = new JMenuItem("Copy");
		copy.setAccelerator(KeyStroke.getKeyStroke(
			KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK));
		copy.addActionListener(e2 -> {
			String text = ((JTextComponent)invoker).getSelectedText();
			Toolkit.getDefaultToolkit()
				.getSystemClipboard()
					.setContents(
						new StringSelection(text),
						null);
			});
		jPopupMenu.add(copy);
		
		//Show the popup menu at the mouse
		jPopupMenu.show(invoker, e.getX() + 5, e.getY() + 15);
	}
}