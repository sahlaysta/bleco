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
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingWorker;
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
	GUISearcherList searchResultList;
	JPanel searchFieldPanel;
	boolean forceEngSearch = false;
	private void init() {
		ep = new GUISearcherEntryPopup(gui);
		
		//Initialize main panel
		setLayout(new BorderLayout());
		JPanel mainPanel = new JPanel();
		setLayer(mainPanel, JLayeredPane.DEFAULT_LAYER);
		add(mainPanel);
		mainPanel.setLayout(new BorderLayout());
		
		//Searcher components
		searchField = new GUIJTextField(gui);
		searchResultList = new GUISearcherList(gui);
		
		//Match background color
		mainPanel.setBackground(searchField.getBackground());
		
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
		searchFieldPanel.setBackground(searchField.getBackground());
		mainPanel.add(searchFieldPanel, BorderLayout.NORTH);
		
		//Search result list layout
		searchResultList.setFocusable(false);
		searchResultList.setBackground(searchField.getBackground());
		searchResultList.setBorder(new EmptyBorder(0,5,0,5));
		mainPanel.add(searchResultList);
		
		//Search list double click open
		searchResultList.slc.addMouseListener(new MouseAdapter() {
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
			keyEvent(ke);
		}, AWTEvent.KEY_EVENT_MASK);
		
		//close opened entry when clicked anywhere outside
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
	
	
	//Update search results to list
	private final WorkerManager wm = new WorkerManager();
	private static final List<?> EMPTY_SEARCH = Arrays.asList("Enter a search term...");
	private static final List<?> NO_RESULTS = Arrays.asList("No results found...");
	void updateSearch() {
		updateSearch(searchField.getText());
	}
	void updateSearch(String search) {
		wm.queue(new SearchWorker(wm, search));
	}
	private List<SearchResult> searchDict(String search) {
		return
			forceEngSearch
				? gui.dict.englishSearch(search)
				: gui.dict.search(search);
	}
	private void setSearch(String search, List<SearchResult> searchResult) {
		//display search results to search result list
		if (searchResult != null) {
			searchResultList.setEnabled(true);
			searchResultList.setSearchResult(searchResult);
		} else {
			searchResultList.setEnabled(false);
			searchResultList.setSearchResult(
				search == null || search.length() == 0
					? EMPTY_SEARCH
					: NO_RESULTS);
		}
	}
	
	//swingworker queue
	//rough rewrite of https://stackoverflow.com/a/22425451/
	private static final class WorkerManager {
		//queue multiple swingworkers
		final Queue<QueueableWorker> q = new LinkedList<>();
		volatile boolean executing = false;
		
		//queue operations
		synchronized void queue(QueueableWorker qw) {
			q.add(qw);
			if (!executing)
				executeNext();
		}
		synchronized void executeNext() {
			QueueableWorker qw = q.poll();
			if (qw != null) {
				setExecuting(true);
				qw.execute();
			}
		}
		void setExecuting(boolean executing) {
			this.executing = executing;
		}
	}
	
	//convenience abstraction over swingworker class
	private static abstract class QueueableWorker
			extends SwingWorker<Object, Object> {
		final WorkerManager wm;
		QueueableWorker(WorkerManager wm) {
			this.wm = wm;
		}
		abstract void workerDone();
		abstract void doWork();
		@Override
		final protected Object doInBackground() throws Exception {
			doWork();
			return null;
		}
		@Override
		final protected void done() {
			workerDone();
			wm.setExecuting(false);
			wm.executeNext();
		}
	}

	//search implementation of queueableworker
	private final class SearchWorker extends QueueableWorker {
		//constructor
		final String search;
		SearchWorker(WorkerManager wm, String search) {
			super(wm);
			this.search = search;
		}
		
		//swingworker operations
		volatile List<SearchResult> searchResult;
		@Override
		protected void doWork() {
			//perform dictionary search
			searchResult = searchDict(search);
		}
		@Override
		void workerDone() {
			setSearch(search, searchResult);
		}
	}
	
	
	
	//Gui searcher operations
	
	private void keyEvent(KeyEvent e) {
		//searcher key shortcuts
		KeyStroke ks = KeyStroke.getKeyStrokeForEvent(e);
		if (keyEq(gui.shortcuts.openEntry, ks))
			openSelectedEntry();
		else if (keyEq(gui.shortcuts.next, ks))
			moveSelection(1);
		else if (keyEq(gui.shortcuts.prev, ks))
			moveSelection(-1);
		else if (keyEq(gui.shortcuts.next5, ks))
			moveSelection(5);
		else if (keyEq(gui.shortcuts.prev5, ks))
			moveSelection(-5);
		else if (keyEq(gui.shortcuts.copySelEntry, ks))
			gui.controller.copy();
	}
	private static boolean keyEq(KeyStroke[] sh, KeyStroke ks) {
		for (KeyStroke i: sh)
			if (i.equals(ks))
				return true;
		return false;
	}
	
	//increment or decrement list selection
	void moveSelection(int change) {
		int index = searchResultList.getSelectedIndex();
		int newIndex;
		if (index < 0) {
			newIndex = 0;
		} else {
			newIndex = index + change;
			if (newIndex < 0)
				newIndex = 0;
		}
		setSelection(newIndex);
	}
	
	//set the list selection
	void setSelection(int index) {
		if (index < 0)
			searchResultList.clearSelection();
		else
			searchResultList.setSelectedIndex(index);
	}
	
	//returns the selected entry in the list
	Entry getSelectedEntry() {
		return searchResultList.getSelectedEntry();
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