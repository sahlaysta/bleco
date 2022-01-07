package com.github.sahlaysta.bleco.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.github.sahlaysta.bleco.dict.Entry;
import com.github.sahlaysta.bleco.dict.SearchResult;

/* custom jlist cell renderer optimization, avoid rendering cells
 * that aren't onscreen on the scroll (jscrollpane) */
final class GUISearcherListCellRenderer extends DefaultListCellRenderer {
	private static final long serialVersionUID = 1L;
	
	//Constructor
	private final GUISearcher searcher;
	private final JPanel header;
	private final JLabel headerLabel;
	GUISearcherListCellRenderer(GUISearcher searcher) {
		this.searcher = searcher;
		
		//listen to search change
		searcher.searchField.getDocument()
		.addDocumentListener(new DocumentListener() {
			void edited() {
				updateIndexRange();
				
				//reset list width every search
				searcher.searchResultList.setFixedCellWidth(-1);
			}
			@Override
			public void changedUpdate(DocumentEvent e) {
				edited();
			}
			@Override
			public void insertUpdate(DocumentEvent e) {
				edited();
			}
			@Override
			public void removeUpdate(DocumentEvent e) {
				edited();
			}
		});
		
		//listen to component move/resize
		searcher.searchResultListScroll
		.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentMoved(ComponentEvent e) {
				updateIndexRange();
			}
			@Override
			public void componentResized(ComponentEvent e) {
				updateIndexRange();
			}
		});
		
		//listen to scrollbar change
		searcher.searchResultListScroll.getVerticalScrollBar()
		.addAdjustmentListener(e -> updateIndexRange());
		
		//add defaultlistcellrenderer to sentence split header
		header = new JPanel();
		header.setLayout(new BorderLayout());
		headerLabel = new JLabel();
		header.add(headerLabel, BorderLayout.NORTH);
	}
	
	
	//get visible index range in jscrollpane
	private static final int VISIBLE_INDEX_RANGE_LENIENCE = 20;
	int startIndex, endIndex;
	void updateIndexRange() {
		startIndex = searcher.searchResultList.locationToIndex(
			new Point(
				0,
				searcher.searchResultListScroll
					.getVerticalScrollBar().getValue()));
		endIndex = searcher.searchResultList.locationToIndex(
			new Point(
				0,
				searcher.searchResultListScroll
					.getVerticalScrollBar().getValue()
					+ searcher.searchResultListScroll
						.getSize().height));
		
		startIndex -= VISIBLE_INDEX_RANGE_LENIENCE;
		endIndex += VISIBLE_INDEX_RANGE_LENIENCE;
		
		searcher.searchResultListScroll.repaint();
		searcher.searchResultListScroll.revalidate();
	}
	
	
	//Cell render
	@Override
	public Component getListCellRendererComponent(
			JList<? extends Object> list,
			Object value,
			int index,
			boolean isSelected,
			boolean cellHasFocus) {
		
		
		//check if cell is in scroll range
		boolean inRange = !(index < startIndex || index > endIndex);
		if (!inRange)
			value = " ";
		

		/* disable blue selection highlight for
		 * "Enter a search" and "No results found" */
		if (!list.isEnabled()) {
			isSelected = false;
			cellHasFocus = false;
		}
		
		
		//put sentence split header
		boolean putHeader
			= value instanceof SearchResult
			&& ((SearchResult)value).isFirstOfSplitGroup;
		
		
		//highlight english search
		if (!putHeader
			&& !isSelected
			&& value instanceof SearchResult
			&& ((SearchResult)value).type
				== SearchResult.ENGLISH_SEARCH) {
			value = entryHtml((SearchResult)value);
		}
		
		
		//call defaultlistcellrenderer
		Component comp =
			super.getListCellRendererComponent(
				list,
				value,
				index,
				isSelected,
				cellHasFocus);
		if (!inRange)
			return comp;
		if (putHeader)
			return getHeader(comp, ((SearchResult)value).entry);

		
		if (value instanceof SearchResult) {
			SearchResult sr = (SearchResult)value;
			
			/* show sentence split header on the first
			 * match result of every match group */
			if (sr.isFirstOfSplitGroup)
				comp = getHeader(comp, sr.entry);
		}
		
		
		/* set the width of the list to the width
		 * of the widest seen cell component */
		int cellWidth = list.getFixedCellWidth();
		int compWidth = comp.getPreferredSize().width;
		if (compWidth > cellWidth)
			list.setFixedCellWidth(compWidth);
		
		return comp;
	}
	
	//create search result header
	private Component getHeader(Component comp, Entry entry) {
		header.add(comp, BorderLayout.CENTER);
		headerLabel.setText(
			Entry.getCharacterType() == Entry.SIMPLIFIED_CHINESE
				? entry.simplified
				: entry.traditional);
		return header;
	}
	
	
	//highlight english searches
	private static final Color HIGHLIGHT_COLOR = Color.decode("#c2d0ff");
	private static final String HIGHLIGHT_START
		= "<span bgcolor=\"rgb("
			+ HIGHLIGHT_COLOR.getRed()
			+ ", " + HIGHLIGHT_COLOR.getGreen()
			+ ", " + HIGHLIGHT_COLOR.getBlue()
			+ ")\">";
	private static final String HIGHLIGHT_END = "</span>";
	private String entryHtml(SearchResult sr) {
		Entry entry = sr.entry;
		String[] defs = new String[entry.definitions.length];
		for (int i = 0; i < defs.length; i++) {
			defs[i] =
				i == sr.definition
					? html(
						entry.definitions[i],
						sr.beginIndex,
						sr.endIndex + 1)
					: html(
						entry.definitions[i],
						-1,
						-1);
		}
		
		//lazy
		return
			"<html>"
			+ new Entry(
				entry.simplified,
				entry.traditional,
				entry.pinyin,
				entry.formattedPinyin,
				defs,
				entry.normalizedDefinitions,
				entry.exampleSentences)
			+ "</html>";
	}
	
	private static String html(String str, int startIndex, int endIndex) {
		int len = str.length();
		StringBuilder sb = new StringBuilder(len);
		//iterate string codepoints
		for (int i = 0; i < len; ) {
			//html highlight
			if (i == startIndex)
				sb.append(HIGHLIGHT_START);
			
			//escape html characters
			int codePoint = str.codePointAt(i);
			switch (codePoint) {
			case '\"': case '\'': case '<':
			case '>': case '&':
				sb.append("&#");
				sb.append(codePoint);
				sb.append(';');
				i++;
				continue;
			}
			sb.appendCodePoint(codePoint);
			i++;
			
			//html highlight
			if (i == endIndex)
				sb.append(HIGHLIGHT_END);
		}
		return sb.toString();
	}
}