package com.github.sahlaysta.bleco;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.undo.UndoManager;

import com.github.sahlaysta.bleco.Dictionary.Entry;
import com.github.sahlaysta.bleco.Dictionary.ExampleSentence;
import com.github.sahlaysta.bleco.Dictionary.Match;

/** GUI window of Bleco */
public final class GUI extends JFrame {
	private static final long serialVersionUID = 1L;
	/** Main code block that creates
	 * Bleco GUI instance */
	public static void main(String[] args) throws IOException {
		new GUI().setVisible(true);
	}
	
	/** Constructs GUI and loads the dictionary
	 * showing a brief "Loading dictionary" screen */
	public GUI() {
		super("Bleco");
		setMinimumSize(new Dimension(240, 180));
		setIconImage(new ImageIcon(
				GUI.class.getResource("resources/icon.png")).getImage());
		try {
			UIManager.setLookAndFeel(
					UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		Preferences.getWindowProperties().apply(this);
		Preferences.subscribe(this);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				Preferences.setSearchQuery(searchField.getText());
				Preferences.setFont(font);
				Preferences.setFontSize(fontSize);
				Preferences.setHandwritingWindowOpen(
						handwritingWindow != null);
				closeHandwritingWindow();
				Preferences.save();
				System.exit(0);
			}
		});
		
		//Load Dictionary and show brief "Loading dictionary" screen
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentShown(ComponentEvent e) {
				removeComponentListener(this);
				
				//Loading label
				JLabel loadingLabel = new JLabel(
						"Loading dictionary...",
						SwingConstants.CENTER);
				loadingLabel.setEnabled(false);
				loadingLabel.setFont(loadingLabel.getFont().deriveFont(20f));
				add(loadingLabel);
				revalidate();
				repaint();
				
				//Load resources
				new SwingWorker<Object, Object>() {
					@Override
					protected Object doInBackground() throws Exception {
						loadBleco();
						return null;
					}
					@Override
					protected void done() {
						initComponents();
					}
				}.execute();
			}
		});
	}
	/* Loads Bleco Dictionary */
	private final void loadBleco() {
		Throwable loadErr = null;
		String loadErrMsg = null;
		
		try { //load Dictionary
			Dictionary.load(GUI.class.getResourceAsStream(
					"resources/dict.bleco"));
		} catch (Throwable e) {
			loadErr = e;
			loadErrMsg = "Error loading dictionary";
		}
		
		if (loadErr != null) { //Show load error if exists
			loadErr.printStackTrace();
			JOptionPane.showMessageDialog(
					this,
					loadErrMsg + "\n" + loadErr.toString(),
					"Error",
					JOptionPane.ERROR_MESSAGE);
			System.exit(0);
		}
	}
	
	
	//GUI Components
	/** Main JLayeredPane with search field and JList */
	private JLayeredPane jlp;
	/** The search text field of Bleco */
	private JTextField searchField;
	/** The JList that displays search results */
	private JList<?> searchResultList;
	/** The scroll pane of the search result list */
	private JScrollPane searchResultListScroll;
	/** The data list model of search results */
	private DefaultListModel<String> dlm;
	/** The font of the GUI */
	private String font;
	/** The font size of the GUI */
	private int fontSize;
	/** The Chinese character handwriting window */
	private JDialog handwritingWindow;
	/** Option to switch to simplified Chinese characters */
	private JCheckBoxMenuItem simplified;
	/** Option to switch to traditional Chinese characters */
	private JCheckBoxMenuItem traditional;
	/** Option to show/close the handwriting window */
	private JCheckBoxMenuItem handwritingWindowMenuItem;
	/** Initialize the layout and components of Bleco */
	@SuppressWarnings("unchecked")
	private final void initComponents() {
		for (Component c: getContentPane().getComponents())
			remove(c);

		//Main layout
		JPanel masterPanel = new JPanel();
		masterPanel.setLayout(new BorderLayout());
		masterPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
		setLayout(new GridLayout(1, 1));
		
		jlp = new JLayeredPane();
		jlp.setLayout(new BorderLayout());
		jlp.setLayer(masterPanel, JLayeredPane.DEFAULT_LAYER);
		jlp.add(masterPanel);
		getContentPane().add(jlp, BorderLayout.CENTER);
		
		//Resize master panel on window resize (JLayeredPane)
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				masterPanel.setSize(getContentPane().getSize());
				revalidate();
				repaint();
			}
		});
		
		//Menubar
		JMenuBar menuBar = new JMenuBar();
		
		JMenu options = new JMenu("Options"); //Options JMenu
		simplified = new JCheckBoxMenuItem("Simplified");
		traditional = new JCheckBoxMenuItem("Traditional");
		JMenuItem fontOption = new JMenuItem("Font");
		if (Dictionary.getCharacterType() == Dictionary.SIMPLIFIED_CHINESE)
			simplified.setSelected(true);
		else
			traditional.setSelected(true);
		simplified.addActionListener(e->{
			traditional.setSelected(false);
			simplified.setSelected(true);
			Dictionary.setCharacterType(Dictionary.SIMPLIFIED_CHINESE);
			updateSearch();
		});
		traditional.addActionListener(e->{
			simplified.setSelected(false);
			traditional.setSelected(true);
			Dictionary.setCharacterType(Dictionary.TRADITIONAL_CHINESE);
			updateSearch();
		});
		fontOption.addActionListener(e -> showFontPicker());
		options.add(simplified);
		options.add(traditional);
		options.addSeparator();
		options.add(fontOption);
		
		JMenu handwriting = new JMenu("Handwriting"); //Handwriting JMenu
		handwritingWindowMenuItem
			= new JCheckBoxMenuItem("Handwriting window");
		if (Preferences.getHandwritingWindowOpen())
			handwritingWindowMenuItem.setSelected(true);
		handwritingWindowMenuItem.addActionListener(e->{
			if (handwritingWindowMenuItem.isSelected())
				showHandwritingWindow();
			else
				closeHandwritingWindow();
		});
		handwriting.add(handwritingWindowMenuItem);
		
		JMenu help = new JMenu("Help"); //Help JMenu
		JMenuItem about = new JMenuItem("About");
		about.addActionListener(e->{
			JOptionPane.showMessageDialog(
				this,
				"Bleco v1.1a written by porog\n"
					+ "https://github.com/sahlaysta/bleco\n\n"
					+ "CC-CEDICT belongs to MDBG:"
					+ "https://cc-cedict.org/wiki/\n"
					+ "Example sentences belong to Tatoeba:"
					+ "https://tatoeba.org/en/\n"
					+ "HanziLookup belongs to Jordan Kiang:"
					+ "https://www.kiang.org/jordan/software/hanzilookup/\n",
				"About",
				JOptionPane.INFORMATION_MESSAGE);
		});
		about.setAccelerator(
				KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0, false));
		help.add(about);
		
		JMenu entryOptions = new JMenu("Entry"); //Entry JMenu
		JMenuItem next = new JMenuItem("Next"),
				previous = new JMenuItem("Previous"),
				open = new JMenuItem("Open"),
				close = new JMenuItem("Close"),
				copyEntry = new JMenuItem("Copy selected entry");
		final ActionListener
			nextAction = e->moveSelectedIndex(1),
			prevAction = e->moveSelectedIndex(-1),
			openAction = e->openSelectedEntry(),
			closeAction = e->closeEntry(),
			copyEntryAction = e->copySelectedEntry();
		next.addActionListener(nextAction);
		previous.addActionListener(prevAction);
		open.addActionListener(openAction);
		close.addActionListener(closeAction);
		copyEntry.addActionListener(copyEntryAction);
		KeyStroke nextKeyStroke = KeyStroke.getKeyStroke(
				KeyEvent.VK_DOWN, 0, false);
		KeyStroke prevKeyStroke = KeyStroke.getKeyStroke(
				KeyEvent.VK_UP, 0, false);
		KeyStroke openKeyStroke = KeyStroke.getKeyStroke(
				KeyEvent.VK_ENTER, 0, false);
		KeyStroke closeKeyStroke = KeyStroke.getKeyStroke(
				KeyEvent.VK_BACK_SPACE, 0, false);
		KeyStroke copyEntryKeyStroke = KeyStroke.getKeyStroke(
				KeyEvent.VK_B, KeyEvent.CTRL_DOWN_MASK, false);
		next.setAccelerator(nextKeyStroke);
		next.getInputMap(JMenuItem.WHEN_IN_FOCUSED_WINDOW)
			.put(nextKeyStroke, "none");
		previous.setAccelerator(prevKeyStroke);
		previous.getInputMap(JMenuItem.WHEN_IN_FOCUSED_WINDOW)
			.put(prevKeyStroke, "none");
		open.setAccelerator(openKeyStroke);
		open.getInputMap(JMenuItem.WHEN_IN_FOCUSED_WINDOW)
			.put(openKeyStroke, "none");
		close.setAccelerator(closeKeyStroke);
		close.getInputMap(JMenuItem.WHEN_IN_FOCUSED_WINDOW)
			.put(closeKeyStroke, "none");
		copyEntry.setAccelerator(copyEntryKeyStroke);
		entryOptions.add(next);
		entryOptions.add(previous);
		entryOptions.add(open);
		entryOptions.add(close);
		entryOptions.add(copyEntry);
		
		menuBar.add(options);
		menuBar.add(entryOptions);
		menuBar.add(handwriting);
		menuBar.add(help);
		setJMenuBar(menuBar);
		
		
		//Search field and JList layout
		searchField = new JTextField();
		dlm = new DefaultListModel<>();
		searchResultList = new JList<>(dlm);
		searchResultList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JPanel searchFieldPanel = new JPanel();
		searchFieldPanel.setLayout(new GridLayout(1,1));
		searchFieldPanel.add(searchField);
		searchFieldPanel.setBorder(new EmptyBorder(7,5,5,5));
		searchFieldPanel.setBackground(searchResultList.getBackground());
		searchResultListScroll = new JScrollPane(searchResultList);
		searchResultListScroll.setBorder(new EmptyBorder(0,5,0,5));
		searchResultListScroll.setBackground(searchResultList.getBackground());
		masterPanel.add(searchFieldPanel, BorderLayout.NORTH);
		masterPanel.add(searchResultListScroll, BorderLayout.CENTER);

		//search field preference value
		searchField.setText(Preferences.getSearchQuery());
		
		/* Update the search result list whenever the search
		 * field is changed */
		searchField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void changedUpdate(DocumentEvent e) {
				updateSearch();
			}
			@Override
			public void insertUpdate(DocumentEvent e) {
				updateSearch();
			}
			@Override
			public void removeUpdate(DocumentEvent e) {
				updateSearch();
			}
		});
		
		/* Disable the beep sound when you backspace in the
		 * text field while it is empty / caret is at the beginning */
		searchField.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {
				consumeEmptyBackspace(e);
			}
			@Override
			public void keyReleased(KeyEvent e) {
				consumeEmptyBackspace(e);
			}
			@Override
			public void keyTyped(KeyEvent e) {
				consumeEmptyBackspace(e);
			}
			private final void consumeEmptyBackspace(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE
						&& searchField.getSelectionStart() == 0
						&& searchField.getSelectionEnd() == 0)
					e.consume();
			}
		});
		
		//Close opened entry when click outside of entry pane
		searchField.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				if (entryPane == null)
					return;
				if (e.getOppositeComponent() instanceof JList)
					closeEntry();
				else
					entryPane.requestFocus();
			}
		});
		MouseListener ml = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				closeEntry();
			}
		};
		searchField.addMouseListener(ml);
		searchResultList.addMouseListener(ml);
		getContentPane().addMouseListener(ml);
		searchResultListScroll.addMouseListener(ml);
		searchResultListScroll.getVerticalScrollBar().addMouseListener(ml);
		for (Component c: searchResultListScroll.getVerticalScrollBar()
				.getComponents())
			c.addMouseListener(ml);
		searchResultListScroll.getHorizontalScrollBar().addMouseListener(ml);
		for (Component c: searchResultListScroll.getHorizontalScrollBar()
				.getComponents())
			c.addMouseListener(ml);
		
		// Search list up, down, and enter key press events
		AtomicLong keyPress = new AtomicLong();
		searchField.setFocusTraversalKeysEnabled(false);
		searchField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.isAltDown()
					|| e.isAltGraphDown()
					|| e.isControlDown()
					|| e.isMetaDown())
					return;
				switch (e.getKeyCode()) {
				case KeyEvent.VK_UP:
					keyPress.set(1);
					break;
				case KeyEvent.VK_DOWN:
					keyPress.set(2);
					break;
				case KeyEvent.VK_ENTER:
					keyPress.set(3);
					break;
				case KeyEvent.VK_TAB:
					keyPress.set(e.isShiftDown() ? 1 : 2);
					break;
				case KeyEvent.VK_PAGE_UP:
					keyPress.set(4);
					break;
				case KeyEvent.VK_PAGE_DOWN:
					keyPress.set(5);
					break;
				}
			}
		});
		Timer timer = new Timer(30, e -> {
			int oldVal = keyPress.intValue();
			keyPress.set(0);
			switch (oldVal) {
			case 0:
				break;
			case 1:
				prevAction.actionPerformed(null);
				break;
			case 2:
				nextAction.actionPerformed(null);
				break;
			case 3:
				openAction.actionPerformed(null);
				break;
			case 4:
				moveSelectedIndex(-5);
				break;
			case 5:
				moveSelectedIndex(5);
				break;
			}
		});
		timer.start();
		
		// Search list double click open
		searchResultList.addMouseListener(new MouseAdapter() {
			int firstClickIndex = -1;
			@Override
			public void mouseClicked(MouseEvent e) {
				if (!searchResultList.isEnabled() ||
					!SwingUtilities.isLeftMouseButton(e))
					return;
				int clickCount = e.getClickCount();
				if (clickCount == 1) {
					firstClickIndex
						= searchResultList.locationToIndex(e.getPoint());
				} else if (clickCount >= 2) {
					int secondClickIndex
						= searchResultList.locationToIndex(e.getPoint());
					if (firstClickIndex == secondClickIndex)
						openEntry(searchResult.get(firstClickIndex));
				}
			}
		});
		
		//Always transfer focus to search field
		searchResultList.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				searchField.requestFocus();
			}
		});
		
		//Operate "Show all" button
		searchResultList.addListSelectionListener(new GUI.ShowAllSelected());
		searchResultList.setCellRenderer(
				new GUI.BlueShowAll(searchResultList.getCellRenderer()));
		
		//searchField: undo with Ctrl-Z and redo with Ctrl-Y
		UndoManager um = new UndoManager();
		searchField.getDocument().addUndoableEditListener(um);
		searchField.getActionMap()
		.put("ctrlzundo", new AbstractAction("ctrlzundo") {
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e) {
				if (um.canUndo())
					um.undo();
			}
		});
		searchField.getActionMap()
		.put("ctrlyundo", new AbstractAction("ctrlyundo") {
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e) {
				if (um.canRedo())
					um.redo();
			}
		});
		searchField.getInputMap().put(
				KeyStroke.getKeyStroke(
						KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK),
				"ctrlzundo");
		searchField.getInputMap().put(
				KeyStroke.getKeyStroke(
						KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK),
				"ctrlyundo");
		
		//searchField: rightclick popup menu with cut, copy, paste etc
		JPopupMenu rightClickMenu = new JPopupMenu();
		JMenuItem cut = new JMenuItem("Cut"),
				copy = new JMenuItem("Copy"),
				paste = new JMenuItem("Paste"),
				selectAll = new JMenuItem("Select all"),
				undo = new JMenuItem("Undo"),
				redo = new JMenuItem("Redo");
		cut.addActionListener(e -> searchField.cut());
		copy.addActionListener(e -> searchField.copy());
		paste.addActionListener(e -> searchField.paste());
		selectAll.addActionListener(e -> searchField.selectAll());
		undo.addActionListener(e -> {
			if (um.canUndo())
				um.undo();
		});
		redo.addActionListener(e -> {
			if (um.canRedo())
				um.redo();
		});
		cut.setAccelerator(KeyStroke.getKeyStroke(
						KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK));
		copy.setAccelerator(KeyStroke.getKeyStroke(
				KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK));
		paste.setAccelerator(KeyStroke.getKeyStroke(
				KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK));
		selectAll.setAccelerator(KeyStroke.getKeyStroke(
				KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK));
		undo.setAccelerator(KeyStroke.getKeyStroke(
				KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK));
		redo.setAccelerator(KeyStroke.getKeyStroke(
				KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK));
		rightClickMenu.add(cut);
		rightClickMenu.add(copy);
		rightClickMenu.add(paste);
		rightClickMenu.add(selectAll);
		rightClickMenu.addSeparator();
		rightClickMenu.add(undo);
		rightClickMenu.add(redo);
		searchField.setComponentPopupMenu(rightClickMenu);
		
		//Last step: refresh layout
		setFont(Preferences.getFont(), Preferences.getFontSize());
		updateSearch();
		revalidate();
		repaint();
		searchField.requestFocus();
		if (Preferences.getHandwritingWindowOpen())
			showHandwritingWindow();
	}
	private final void openSelectedEntry() {
		if (!searchResultList.isEnabled())
			return;
		int index = searchResultList.getSelectedIndex();
		if (index < 0) {
			searchResultList.setSelectedIndex(0);
			searchResultList.ensureIndexIsVisible(0);
			return;
		}
		openEntry(searchResult.get(index));
	}
	private final void moveSelectedIndex(int change) {
		/* Increment or decrement the selected index
		 * in the search result list */
		if (!searchResultList.isEnabled()
			|| entryPane != null)
			return;
		
		int index = searchResultList.getSelectedIndex();
		if (change < 0 && index < 0)
			return;
		
		int newIndex = index + change;
		if (newIndex < 0)
			newIndex = 0;
		searchResultList.setSelectedIndex(newIndex);
		searchResultList.ensureIndexIsVisible(newIndex);
	}
	private final void copySelectedEntry() {
		/* Copy the Chinese text of the
		 * selected entry to the clipboard */
		if (entryPane != null || !searchResultList.isEnabled())
			return;
		int index = searchResultList.getSelectedIndex();
		if (index < 0)
			return;
		Entry selectedEntry = searchResult.get(index);
		String text =
			Dictionary.getCharacterType()
			== Dictionary.SIMPLIFIED_CHINESE
				? selectedEntry.simplified
				: selectedEntry.traditional;
		Toolkit.getDefaultToolkit().getSystemClipboard()
        	.setContents(new StringSelection(text), null);
	}
	
	private final void setFont(String font, int fontSize) {
		/** Sets the GUI font */
		this.font = font;
		this.fontSize = fontSize;
		searchField.setFont(new Font(font, Font.PLAIN, fontSize));
		searchResultList.setFont(new Font(font, Font.PLAIN, fontSize + 2));
		if (handwritingWindow != null)
			handwritingWindow.setFont(new Font(font, Font.PLAIN, fontSize));
		revalidate();
		repaint();
	}
	
	
	//Searching
	/* Get the Dictionary search results
	 * of the entered search in the text field
	 * and shows it to the list */
	List<Entry> searchResult;
	private static final int SEARCH_CAP = 50;
	private static final String SHOW_ALL = "Show all...";
	private boolean hasShownAll;
	private final void updateSearch() {
		hasShownAll = false;
		String searchQuery = searchField.getText();
		searchResult = Dictionary.search(searchQuery);
		
		dlm.clear();
		if (searchResult == null) {
			searchResultList.setEnabled(false);
			dlm.addElement(
					searchQuery.length() > 0
					? "No results found..."
					: "Enter a search term...");
		} else {
			searchResultList.setEnabled(true);
			for (int i = 0; i < searchResult.size(); i++) {
				dlm.addElement(searchResult.get(i).toString());
				if (i > SEARCH_CAP) {
					dlm.addElement(SHOW_ALL);
					break;
				}
			}
		}
	}
	private final class ShowAllSelected implements ListSelectionListener {
		@Override
		public void valueChanged(ListSelectionEvent e) {
			if (hasShownAll)
				return;
			int index = searchResultList.getSelectedIndex();
			if (index < 0)
				return;
			if (dlm.getElementAt(index).equals(SHOW_ALL)) {
				hasShownAll = true;
				dlm.removeElementAt(SEARCH_CAP + 2);
				for (int i = SEARCH_CAP + 2; i < searchResult.size(); i++)
					dlm.addElement(searchResult.get(i).toString());
				searchResultList.setSelectedIndex(SEARCH_CAP + 2);
			}
		}
	}
	@SuppressWarnings({"rawtypes", "unchecked"})
	private final class BlueShowAll implements ListCellRenderer {
		private final ListCellRenderer lcr;
		private BlueShowAll(ListCellRenderer lcr) {
			this.lcr = lcr;
		}
		@Override
		public Component getListCellRendererComponent(
				JList list, Object value, int index,
				boolean isSelected, boolean cellHasFocus) {
			Component c = lcr.getListCellRendererComponent(
					list, value, index, isSelected, cellHasFocus);
			if (value == SHOW_ALL)
				c.setForeground(Color.BLUE);
			return c;
		}
	}
	
	
	//Entry subwindow
	private JComponent entryPane;
	private final void openEntry(Entry entry) {
		if (entryPane != null)
			return;
		
		//Popup pane layout
		entryPane = new JPanel();
		entryPane.setOpaque(false);
		entryPane.setLayout(new GridLayout(1,1));
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.setBackground(new Color(0, 0, 0, 100));
		panel.setBorder(new EmptyBorder(50, 50, 50, 50));
		entryPane.add(panel);
		jlp.setLayer(entryPane, JLayeredPane.PALETTE_LAYER);
		jlp.add(entryPane);
		
		//While entry is open
		simplified.setEnabled(false);
		traditional.setEnabled(false);
		searchResultListScroll.setWheelScrollingEnabled(false);
		
		//Html text pane
		JTextPane textPane = new JTextPane();
		textPane.setPreferredSize(new Dimension(200, 100));
		textPane.setEditable(false);
		textPane.setContentType("text/html");
		createEntryHtml(entry, textPane);
		textPane.setCaretPosition(0);
		JScrollPane jScrollPane = new JScrollPane(textPane);
		
		panel.add(jScrollPane, BorderLayout.CENTER);
		
		//Backspace, ESC, Enter, and Tab keys close entry
		textPane.setFocusTraversalKeysEnabled(false);
		textPane.getActionMap()
		.put("blecoentryclose", new AbstractAction("blecoentryclose") {
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e) {
				if (entryPane != null)
					closeEntry();
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
		
		revalidate();
		repaint();
		textPane.requestFocus();
	}
	private final void closeEntry() {
		if (entryPane == null)
			return;
		jlp.remove(entryPane);
		entryPane = null;
		revalidate();
		repaint();
		
		//Reenable
		simplified.setEnabled(true);
		traditional.setEnabled(true);
		searchResultListScroll.setWheelScrollingEnabled(true);
	}
	private final void createEntryHtml(Entry entry, JTextPane jTextPane) {
		boolean smpl =
				Dictionary.getCharacterType()
				== Dictionary.SIMPLIFIED_CHINESE;
		
		//Create JTextPane HTML for Entry
		StringBuilder html = new StringBuilder();
		String fontName = searchField.getFont().getFamily();
		
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
					if (es.chinese.contains(
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
				html.append(es.english);
				html.append("</font><font face=\"");
				html.append(fontName);
				
				//The Chinese example sentence
				html.append("\"size=\"5\"><br><a href=\"a\"style=\""
						+ "text-decoration: none;\"><span style=\""
						+ "color:rgb(24, 54, 204);\">");
				String chinese = es.chinese;
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
			Match match
				= Dictionary
					.findChineseWord(
						clickedSentence,
						clickedIndexInSentence);
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
				List<Entry> searchEntries = Dictionary.search(selectedText);
				if (searchEntries == null)
					return;
				for (Entry searchEntry: searchEntries) {
					if (searchEntry.simplified.equals(selectedText)
							|| searchEntry.traditional.equals(selectedText)) {
						selectedEntry = searchEntry;
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
	private static final void
	showEntryContextMenu(Entry entry, Component invoker, MouseEvent e) {
		/* Create a small popup context menu for entry with
		 * pronunciation and definitions */
		JPopupMenu jPopupMenu = new JPopupMenu();
		
		//Add entry simplified/traditional
		String entryText = Dictionary.getCharacterType()
				== Dictionary.SIMPLIFIED_CHINESE
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
		copy.addActionListener(e2 ->
			Toolkit.getDefaultToolkit()
				.getSystemClipboard()
					.setContents(
						new StringSelection(entryText),
						null));
		jPopupMenu.add(copy);
		
		//Show the popup menu at the mouse
		jPopupMenu.show(invoker, e.getX() + 5, e.getY() + 15);
	}

	//Font picker
	private final void showFontPicker() {
		String originalFont = font;
		int originalFontSize = fontSize;
		
		//Create the JDialog font picker
		JDialog jDialog = new JDialog(this, true);
		jDialog.setTitle("Font");
		jDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		jDialog.setResizable(false);
		jDialog.setLayout(new GridLayout(1,1));
		
		//Layout
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());
		mainPanel.setBorder(new EmptyBorder(5, 5, 0, 0));
		
		//Font options panel
		JPanel optionsPanel = new JPanel();
		optionsPanel.setBorder(new EmptyBorder(0, 0, 0, 5));
		JComboBox<String> jComboBox = new JComboBox<>();
		jComboBox.setPreferredSize(new Dimension(100, 0));
		JTextField jTextField = new JTextField(fontSize + "");
		optionsPanel.setLayout(new GridLayout(2,2,5,5));
		optionsPanel.add(new JLabel("Font:"));
		optionsPanel.add(jComboBox);
		optionsPanel.add(new JLabel("Font size:"));
		optionsPanel.add(jTextField);
		
		//Buttons panel
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		JButton okButton = new JButton("OK");
		JButton cancelButton = new JButton("Cancel");
		buttonPanel.add(okButton);
		buttonPanel.add(cancelButton);
		
		//Add panels
		mainPanel.add(optionsPanel, BorderLayout.CENTER);
		mainPanel.add(buttonPanel, BorderLayout.SOUTH);
		
		//Configure combo box
		for (String s: GraphicsEnvironment.getLocalGraphicsEnvironment()
				.getAvailableFontFamilyNames())
			jComboBox.addItem(s);
		jComboBox.setSelectedItem(searchField.getFont().getFamily());
		
		//Button action listeners
		ActionListener acceptAction = e -> {
			
			//Get the text field's entered font size
			int newFontSize;
			try {
				newFontSize = Integer.parseInt(jTextField.getText());
			} catch (NumberFormatException e2) { //error message window
				JOptionPane.showMessageDialog(
						this,
						"Invalid input: " + jTextField.getText(),
						"Error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
			
			if (newFontSize < 0) { //if bad font size
				JOptionPane.showMessageDialog(
						this,
						"Font size cannot be less than zero",
						"Error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
			
			//Set font and close
			setFont((String)jComboBox.getSelectedItem(), newFontSize);
		};
		ActionListener acceptAndCloseAction = e -> {
			acceptAction.actionPerformed(null);
			jDialog.dispose();
		};
		okButton.addActionListener(acceptAndCloseAction);
		jTextField.addActionListener(acceptAction);
		jComboBox.addActionListener(acceptAction);
		cancelButton.addActionListener(e -> {
			setFont(originalFont, originalFontSize);
			jDialog.dispose();
		});
		
		//Show the JDIalog
		jDialog.add(mainPanel);
		jDialog.pack();
		jDialog.setLocationRelativeTo(this);
		jDialog.setVisible(true);
	}
	
	//HandwritingWindow
	private WindowProperties handwritingWindowWindowProperties;
	private final void showHandwritingWindow() {
		handwritingWindow = new HandwritingWindow(
				this,
				searchField,
				new Font(font, Font.PLAIN, fontSize));
		handwritingWindow.setDefaultCloseOperation(
				JDialog.DO_NOTHING_ON_CLOSE);
		handwritingWindow.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				closeHandwritingWindow();
			}
		});
		handwritingWindow.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				refreshHandwritingWindowProperties();
			}
			@Override
			public void componentMoved(ComponentEvent e) {
				refreshHandwritingWindowProperties();
			}
		});
		handwritingWindow.setVisible(true);
	}
	private final void closeHandwritingWindow() {
		if (handwritingWindowWindowProperties != null) {
			Preferences.setHandwritingWindowX(
					handwritingWindowWindowProperties.x);
			Preferences.setHandwritingWindowY(
					handwritingWindowWindowProperties.y);
		}
		if (handwritingWindow != null) {
			handwritingWindow.dispose();
			handwritingWindow = null;
		}
		handwritingWindowMenuItem.setSelected(false);
	}
	private final void refreshHandwritingWindowProperties() {
		Point p = handwritingWindow.getLocation();
		Dimension d = handwritingWindow.getSize();
		handwritingWindowWindowProperties
			= new WindowProperties(
					false,
					p.x,
					p.y,
					d.width,
					d.height);
	}
}