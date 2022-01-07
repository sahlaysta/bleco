package com.github.sahlaysta.bleco.ui;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Arrays;
import java.util.List;

import javax.swing.JLayeredPane;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.github.sahlaysta.bleco.dict.Entry;
import com.github.sahlaysta.bleco.dict.SearchResult;

/* Bleco GUI's search panel with
 * a text box and a search result box */
final class GUISearcher extends JLayeredPane {
	private static final long serialVersionUID = 1L;
	
	//Constructor
	final GUI gui;
	GUISearcher(GUI gui) {
		this.gui = gui;
		init();
	}
	
	
	//Entry popup
	GUISearcherEntryPopup ep;
	
	//Components
	GUIJTextField searchField;
	JPanel searchFieldPanel;
	JList<?> searchResultList;
	GUISearcherListModel model;
	JScrollPane searchResultListScroll;
	private void init() {
		ep = new GUISearcherEntryPopup(gui);
		
		//Initialize main panel
		setLayout(new BorderLayout());
		JPanel mainPanel = new JPanel();
		setLayer(mainPanel, JLayeredPane.DEFAULT_LAYER);
		add(mainPanel);
		mainPanel.setLayout(new BorderLayout());
		
		//Searcher components
		searchField = new GUIJTextField();
		model = new GUISearcherListModel();
		searchResultList = new JList<>(model);
		
		//Resize main panel on window resize (JLayeredPane)
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				mainPanel.setSize(gui.getContentPane().getSize());
				gui.revalidate();
				gui.repaint();
			}
		});
		
		//Search text field layout
		searchFieldPanel = new JPanel();
		searchFieldPanel.setLayout(new GridLayout(1,1));
		searchFieldPanel.add(searchField);
		searchFieldPanel.setBorder(new EmptyBorder(7,5,5,5));
		searchFieldPanel.setBackground(
			searchResultList.getBackground());
		mainPanel.add(searchFieldPanel, BorderLayout.NORTH);
		
		//Search result list
		searchResultList.setSelectionMode(
			ListSelectionModel.SINGLE_SELECTION);
		searchResultList.setFocusable(false);
		searchResultListScroll
			= new JScrollPane(searchResultList);
		searchResultListScroll.setBorder(null);
		searchResultListScroll.setBorder(
			new EmptyBorder(0,5,0,5));
		searchResultListScroll.setBackground(
			searchResultList.getBackground());
		mainPanel.add(searchResultListScroll, BorderLayout.CENTER);

		//Search result list cell renderer
		GUISearcherListCellRenderer cellRenderer
			= new GUISearcherListCellRenderer(this);
		searchResultList.setCellRenderer(cellRenderer);
		
		//Search list double click open
		searchResultList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() >= 2)
					openSelectedEntry();
			}
		});
		

		//Update search results when search field is edited
		searchField.getDocument()
		.addDocumentListener(new DocumentListener() {
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
		
		
		//key press events
		Toolkit.getDefaultToolkit()
		.addAWTEventListener(e -> {
			if (!(e instanceof KeyEvent))
				return;
			if (e.getSource() != searchField)
				return;
			KeyEvent ke = (KeyEvent)e;
			if (ke.getID() != KeyEvent.KEY_PRESSED)
				return;
			int m = ke.getModifiersEx();
			keyEvent(
				ke.getKeyCode(),
				m == KeyEvent.CTRL_DOWN_MASK,
				m == KeyEvent.SHIFT_DOWN_MASK);
		}, AWTEvent.KEY_EVENT_MASK);
		
		//close opened entry when clicked anywhere
		MouseListener ml = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				closeOpenedEntry();
			}
		};
		GUIUtil.forEachComponent(
			this,
			c -> c.addMouseListener(ml));
	}
	
	//Update search results to Jlist
	List<SearchResult> searchResult;
	boolean engSearch = false;
	static final List<?> EMPTY_SEARCH
		= Arrays.asList("Enter a search term...");
	static final List<?> NO_RESULTS
		= Arrays.asList("No results found...");
	void updateSearch() {
		updateSearch(searchField.getText());
	}
	void updateSearch(String search) {
		searchResult
			= engSearch
				? gui.dict.englishSearch(search)
				: gui.dict.search(search);
		if (searchResult != null) {
			searchResultList.setEnabled(true);
			model.set(searchResult);
		} else {
			searchResultList.setEnabled(false);
			model.set(
				search == null || search.length() == 0
					? EMPTY_SEARCH
					: NO_RESULTS);
		}
		setSelection(-1);
	}
	
	//increment or decrement list selection
	void moveSelection(int change) {
		int index = searchResultList.getSelectedIndex();
		int newIndex;
		if (index < 0 || index >= searchResult.size()) {
			newIndex = 0;
		} else {
			newIndex = index + change;
			if (newIndex < 0)
				newIndex = 0;
		}
		setSelection(newIndex);
	}
	
	//set the jlist selection
	void setSelection(int index) {
		if (index < 0) {
			searchResultList.clearSelection();
		} else {
			searchResultList.setSelectedIndex(index);
			searchResultList.ensureIndexIsVisible(index);
		}
	}
	
	//keyevent shortcuts
	void keyEvent(int keyCode, boolean ctrl, boolean shift) {
		switch (keyCode) {
		case KeyEvent.VK_DOWN:
			gui.controller.next();
			break;
		case KeyEvent.VK_UP:
			gui.controller.previous();
			break;
		case KeyEvent.VK_TAB:
			if (shift)
				gui.controller.previous();
			else
				gui.controller.next();
			break;
		case KeyEvent.VK_ENTER:
			gui.controller.open();
			break;
		case KeyEvent.VK_BACK_SPACE:
			gui.controller.close();
			break;
		case KeyEvent.VK_PAGE_DOWN:
			moveSelection(5);
			break;
		case KeyEvent.VK_PAGE_UP:
			moveSelection(-5);
			break;
		}
	}
	
	//returns the selected entry in the jlist
	Entry getSelectedEntry() {
		if (searchResult == null)
			return null;
		int index = searchResultList.getSelectedIndex();
		if (index < 0 || index >= searchResult.size())
			return null;
		return searchResult.get(index).entry;
	}
	
	//Entry popup
	void openSelectedEntry() {
		Entry entry = getSelectedEntry();
		if (entry == null)
			setSelection(0);
		else
			ep.open(entry);
	}
	void closeOpenedEntry() {
		ep.close();
	}
	
	//Set font
	void setFont(String font, int fontSize) {
		gui.prefs.setFont(font);
		gui.prefs.setFontSize(fontSize);
		searchField.setFont(
			new Font(font, Font.PLAIN, fontSize));
		searchResultList.setFont(
			new Font(font, Font.PLAIN, fontSize + 2));
		revalidate();
		repaint();
	}
}