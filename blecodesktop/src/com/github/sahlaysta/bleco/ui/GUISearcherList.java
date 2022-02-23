package com.github.sahlaysta.bleco.ui;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelListener;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.border.EmptyBorder;

import com.github.sahlaysta.bleco.dict.Entry;
import com.github.sahlaysta.bleco.dict.SearchResult;

/* a fast custom list with a cell renderer that
 * paginates with vertical scrollbar */
final class GUISearcherList extends JComponent {
	private static final long serialVersionUID = 1L;
	
	//Constructor
	final GUI gui;
	GUISearcherList(GUI gui) {
		this.gui = gui;
		init();
	}
	
	
	//Components
	SearcherListComponent slc;
	private JScrollBar vjsb;//vertical scroll bar
	private JPanel vjsbPanel;//vertical scrollbar's panel
	private int vScrollVal;//vertical scroll value
	private JScrollBar hjsb;//horizontal scroll bar
	private JPanel hjsbPanel;//horizontal scrollbar's panel
	private int hScrollVal;//horizontal scroll value
	private int hScrollMax;//save widest horizontal scroll per search
	private int selIndex = -1;//list selection index
	private void init() {
		setOpaque(false);
		setDoubleBuffered(true);
		setBorder(null);
		setLayout(new BorderLayout());
		
		//set up layout
		slc = new SearcherListComponent();
		vjsb = new JScrollBar(JScrollBar.VERTICAL);
		hjsb = new JScrollBar(JScrollBar.HORIZONTAL);
		vjsbPanel = new JPanel();
		hjsbPanel = new JPanel();
		vScrollVal = 0;
		hScrollVal = 0;
		add(slc, BorderLayout.CENTER);
		
		//vertical scrollbar panel
		vjsbPanel.setLayout(new BorderLayout());
		vjsbPanel.setBorder(null);
		vjsbPanel.add(vjsb, BorderLayout.CENTER);
		add(vjsbPanel, BorderLayout.EAST);
		
		//horizontal scrollbar panel
		hjsbPanel.setLayout(new BorderLayout());
		hjsbPanel.setBorder(null);
		hjsbPanel.add(hjsb, BorderLayout.CENTER);
		add(hjsbPanel, BorderLayout.SOUTH);
		
		/* Set the border of the horizontal scrollbar when both
		 * the horizontal and vertical scrollbars are visible,
		 * so that there is space between them */
		Toolkit.getDefaultToolkit().addAWTEventListener(e -> {
			//ensure event id + scrollbar events
			if (!(e instanceof ComponentEvent))
				return;
			ComponentEvent ce = (ComponentEvent)e;
			Object o = e.getSource();
			if (o != hjsb && o != vjsb &&
					o != hjsbPanel && o != vjsbPanel) {
				return;
			}
			switch (ce.getID()) {
			case ComponentEvent.COMPONENT_MOVED:
			case ComponentEvent.COMPONENT_RESIZED:
			case ComponentEvent.COMPONENT_SHOWN:
			case ComponentEvent.COMPONENT_HIDDEN:
				break;
			default: return;
			}
			
			//refresh scrollbar border
			hjsbPanel.setBorder(
				hjsbPanel.isVisible() && vjsbPanel.isVisible()
					? new EmptyBorder(0,0,0,vjsbPanel.getWidth())
					: null);
			revalidate();
			repaint();
		}, AWTEvent.COMPONENT_EVENT_MASK);
		
		//update scroll changes
		vjsb.addAdjustmentListener(e -> {
			vScrollVal = e.getValue();
			revalidate();
			repaint();
		});
		hjsb.addAdjustmentListener(e -> {
			hScrollVal = e.getValue();
			revalidate();
			repaint();
		});
		
		/* automatic repositioning for when the vertical scrollbar is
		 * scrolled to the bottom and the end of the list is reached,
		 * and then the window is vertically resized to a bigger height */
		//get height amount moved on each resize
		final int[] h = new int[1];//wrapper array for an int
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				//update height
				Dimension newD = getSize();
				int heightMoved = newD.height - h[0];
				h[0] = newD.height;
				
				//offset y position by the resized height amount
				if ((vjsbPanel.isVisible()) &&
					(vjsb.getMaximum() ==
						(vjsb.getValue() +
						vjsb.getVisibleAmount()))) {
					vjsb.setValue(vjsb.getValue() - heightMoved);
				}
			}
		});
		
		//same as above but horizontally
		final int[] w = new int[1];//wrapper array for an int
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				//update width
				Dimension newW = getSize();
				int widthMoved = newW.width - w[0];
				w[0] = newW.width;
				
				//offset x position by the resized width amount
				if ((hjsbPanel.isVisible()) &&
					(hjsb.getMaximum() ==
						(hjsb.getValue() +
						hjsb.getVisibleAmount()))) {
					hjsb.setValue(hjsb.getValue() - widthMoved);
				}
			}
		});
		
		//make mouse wheel scrollable
		MouseWheelListener mwl = e -> {
			if (wheelScrollEnabled && vjsbPanel.isVisible()) {
				vjsb.setValue(
						
					e.getWheelRotation() > 0
					
						? vjsb.getValue() +
							(vjsb.getUnitIncrement() *
							e.getScrollAmount())
							
						: vjsb.getValue() -
							(vjsb.getUnitIncrement() *
							e.getScrollAmount()));
			}
		};
		vjsb.addMouseWheelListener(mwl);
		slc.addMouseWheelListener(mwl);
		
		setSearchResult(null);
	}
	
	
	
	//set search results
	private List<?> searchResult;
	private int searchResultLen;
	void setSearchResult(List<?> searchResult) {
		this.searchResult = searchResult == null
			? Collections.EMPTY_LIST : searchResult;
		searchResultLen = this.searchResult.size();
		
		//save and reapply scroll values
		int vs = vjsb.getValue();
		int hs = hjsb.getValue();
		refreshList();
		vjsb.setValue(vs);
		hjsb.setValue(hs);
		
		revalidate();
		repaint();
	}
	void refreshList() {
		calcCellHeights();
		slc.cachedTotalHeight = -1;
		hScrollMax = 0;
		vjsb.setValue(vScrollVal = 0);
		hjsb.setValue(hScrollVal = 0);
	}
	
	//set component fonts + calculate cell heights on font change
	@Override
	public void setFont(Font font) {
		super.setFont(font);
		if (slc != null)
			slc.setFont(font);
		if (ncr != null)
			ncr.setFont(font);
		if (hcr != null)
			hcr.setFont(font);
		calcCellHeights();
		revalidate();
		repaint();
	}
	
	//set component background colors
	@Override
	public void setBackground(Color bg) {
		super.setBackground(bg);
		if (ncr != null)
			ncr.setBackground(bg);
		if (vjsbPanel != null)
			vjsbPanel.setBackground(bg);
		if (hjsbPanel != null)
			hjsbPanel.setBackground(bg);
	}
	
	
	//component view to paint list cells
	final class SearcherListComponent extends JComponent {
		private static final long serialVersionUID = 1L;
		
		//render cells
		@Override
		protected void paintComponent(Graphics g) {
			boolean enabled = GUISearcherList.this.isEnabled();
			ncr.setEnabled(enabled);
			
			Dimension d = getSize();
			int compWidth = d.width;
			int compHeight = d.height;
			int x = 0 - hScrollVal;
			int y = 0 - vScrollVal;
			int minY = 0 - normalCellHeight;
			
			//paint cells
			for (int i = 0; i < searchResultLen; i++) {
				//loop search result
				Object val = searchResult.get(i);
				
				//dont render offscreen scroll cells
				if (y < minY) {
					//continue loop, update y axis
					if (hasHeader(val))
						y += headerCellHeight;
					y += normalCellHeight;
					continue;
				}
				
				//setup the cell
				boolean cellHasHeader = false;
				boolean isSelected = i == selIndex;
				if (val instanceof SearchResult) {
					SearchResult sr = (SearchResult)val;
					if (sr.isFirstOfSplitGroup) {
						//set up cell header
						hcr.setText(sr.entry.getName());
						cellHasHeader = true;
					}
					if (!isSelected && sr.type ==
							SearchResult.ENGLISH_SEARCH) {
						//render highlighted english search result
						val = highlightedEnglishSearchResultHtml(sr);
					}
				}
				ncr.setCell(val, enabled ? isSelected : false);
				
				//update maximum cell width
				int normalCellWidth = ncr.getPreferredSize().width;
				int headerCellWidth = hcr.getPreferredSize().width;
				if (normalCellWidth > hScrollMax)
					hScrollMax = normalCellWidth;
				if (headerCellWidth > hScrollMax)
					hScrollMax = headerCellWidth;
				
				//set width and height of cell
				int cellWidth
					= hScrollMax < compWidth
						? compWidth
						: hScrollMax;
				if (cellHasHeader)
					hcr.setSize(cellWidth, headerCellHeight);
				ncr.setSize(cellWidth, normalCellHeight);
				
				//paint cell, and header if has header
				if (cellHasHeader) {
					hcr.paintComponent(g, x, y);
					y += headerCellHeight;
				}
				ncr.paintComponent(g, x, y);
				y += normalCellHeight;
				
				//end cond
				if (y > compHeight)
					break;
			}
			
			//set scrollbar info
			int totalHeight = calcTotalHeight();
			vjsb.setMinimum(0);
			vjsb.setMaximum(totalHeight);
			vjsb.setVisibleAmount(compHeight);
			vjsbPanel.setVisible(totalHeight > compHeight);
			hjsb.setMinimum(0);
			hjsb.setMaximum(hScrollMax);
			hjsb.setVisibleAmount(compWidth);
			hjsbPanel.setVisible(hScrollMax > compWidth);
		}
		
		//calculate total height of all cells
		private int cachedTotalHeight = -1;
		private int calcTotalHeight() {
			if (cachedTotalHeight != -1)
				return cachedTotalHeight;
			int y = 0;
			for (Object obj: searchResult) {
				if (hasHeader(obj))
					y += headerCellHeight;
				y += normalCellHeight;
			}
			return cachedTotalHeight = y;
		}
		
		//true if a cell has a sentence split header
		private boolean hasHeader(Object val) {
			return val instanceof SearchResult
				&& ((SearchResult)val).isFirstOfSplitGroup;
		}
		
		
		//mouse usage
		//process clicks to list component
		{
			ML ml = new ML();
			addMouseListener(ml);
			addMouseMotionListener(new MML(ml));
		}
		
		/* timer to handle mouse
		 * down and release + mouse drag */
		private final class ML extends MouseAdapter {
			//atomic timer
			final MLTimer mlt = new MLTimer();
			final class MLTimer extends Timer {
				volatile int y;
				volatile TimerTask tt;
			}
			
			//operate mouse movement
			final class MLTT extends TimerTask {
				@Override
				public void run() {
					mouseUpd();
				}
			}
			
			static final int MIN_OFFSCREEN_JUMP = 8;
			void mouseUpd() {
				//ensure search exists
				if (searchResult == null || searchResultLen == 0)
					return;
				
				//synchronize get mouse y
				int y;
				synchronized(mlt) {
					y = mlt.y;
				}
				
				//mouse dragged offscreen
				/* (influence scroll speed by how far
				 * the mouse is offscreen) */
				int h = getHeight();
				if (y < 0) {
					vjsb.setValue(
						vjsb.getValue() +
						(y - MIN_OFFSCREEN_JUMP));
					
					/* set selection index to the
					 * topmost visible */
					int topIndex = getIndexAtY(1);
					if (topIndex >= 0)
						setSelectedIndex(topIndex);
					
					return;
				} else if (y > h) {
					vjsb.setValue(
						vjsb.getValue() +
						(y - h) + MIN_OFFSCREEN_JUMP);
					
					/* set selection index to the
					 * bottommost visible */
					int bottomIndex = getIndexAtY(h - 1);
					if (bottomIndex >= 0)
						setSelectedIndex(bottomIndex);
					
					return;
				}
				
				//update click to mouse index
				int mouseIndex = getIndexAtY(y);
				if (mouseIndex == -2) {
					//upper outbound mouse
					setSelectedIndex(0);
				} else if (mouseIndex == -1) {
					//lower outbound mouse
					setSelectedIndex(searchResultLen - 1);
				} else {
					//normal
					setSelectedIndex(mouseIndex);
				}
				
			}
			
			//set mouse y (drag)
			synchronized void setY(int y) {
				mlt.y = y;
			}
			
			//start mouse drag timer intervals
			synchronized void startMouseTimer(int y) {
				mlt.y = y;
				if (mlt.tt != null)
					mlt.tt.cancel();
				mlt.tt = new MLTT();
				mlt.scheduleAtFixedRate(mlt.tt, 0, 30);
			}
			
			//end timer
			synchronized void stopMouseTimer() {
				if (mlt.tt != null) {
					mlt.tt.cancel();
					mlt.tt = null;
				}
			}
			
			@Override
			public void mousePressed(MouseEvent e) {
				startMouseTimer(e.getY());
			}
			@Override
			public void mouseReleased(MouseEvent e) {
				stopMouseTimer();
			}
		}
		
		//handle mouse drag + timer
		private final class MML extends MouseMotionAdapter {
			final ML ml;
			MML(ML ml) {
				this.ml = ml;
			}
			@Override
			public void mouseDragged(MouseEvent e) {
				ml.setY(e.getY());
			}
		}
		
		
		//calculate the clicked index
		private int getIndexAtY(int y) {
			int vY = y + vScrollVal;
			if (vY < 0)
				return -2;
			int slcY = 0;
			for (int i = 0; i < searchResultLen; i++) {
				if (hasHeader(searchResult.get(i)))
					slcY += headerCellHeight;
				slcY += normalCellHeight;
				if (slcY >= vY)
					return i;
			}
			return -1;
		}
		
		//moves the scroll to the visible cell index
		private void ensureIndexIsVisible(int index) {
			if (index < 0 || index >= searchResultLen)
				return;
			
			//get the top and bottom y of the cell
			int compHeight = getHeight();
			int indexTopY = -1, indexBottomY = -1;
			int y = 0;
			for (int i = 0; i < searchResultLen; i++) {
				if (i == index)
					indexTopY = y;
				if (hasHeader(searchResult.get(i)))
					y += headerCellHeight;
				y += normalCellHeight;
				if (i == index) {
					indexBottomY = y;
					break;
				}
			}
			
			//scroll the cell to be visible
			if (indexTopY < vScrollVal) {
				vjsb.setValue(indexTopY);
				return;
			}
			if (indexBottomY > vScrollVal + compHeight) {
				vjsb.setValue(indexBottomY - compHeight);
				return;
			}
			
			//the cell is already visible
			revalidate();
			repaint();
		}
	}
	
	
	//cell renderering
	private final NormalCellRenderer ncr = new NormalCellRenderer();
	private final HeaderCellRenderer hcr = new HeaderCellRenderer();
	
	//class to make the protected paint method publicly visible
	private static abstract class PaintableJLabel extends JLabel {
		private static final long serialVersionUID = 1L;
		PaintableJLabel() {
			setOpaque(true);
			setBorder(new EmptyBorder(1, 1, 1, 1));
			setHorizontalAlignment(JLabel.LEFT);
		}
		void paintComponent(Graphics g, int x, int y) {
			Dimension d = getSize();
			Graphics tg = g.create(x, y, d.width, d.height);
			paintComponent(tg);
		}
	}
	
	//cell renderer for normal cells
	private static final class NormalCellRenderer extends PaintableJLabel {
		private static final long serialVersionUID = 1L;
		
		//constructor
		NormalCellRenderer() {
			//first call opposite cache
			selectedColor = true;
			setCellColors(false);
		}
		
		//sets cell contents
		void setCell(Object obj, boolean isSelected) {
			setText(String.valueOf(obj));
			setCellColors(isSelected);
		}
		
		//sets cell coloring
		final JList<?> colors = new JList<>();//jlist for colors
		boolean selectedColor;//cache isSelected colors
		void setCellColors(boolean isSelected) {
			if (isSelected == selectedColor)
				return;
			if (selectedColor = isSelected) {
				setBackground(colors.getSelectionBackground());
				setForeground(colors.getSelectionForeground());
			} else {
				setBackground(colors.getBackground());
				setForeground(colors.getForeground());
			}
		}
	}
	
	//cell renderer for header cells
	private static final class HeaderCellRenderer extends PaintableJLabel {
		private static final long serialVersionUID = 1L;
		
		//Font
		String font;
		int fontSize;
		@Override
		public void setFont(Font font) {
			super.setFont(font);
			this.font = font.getFamily();
			//give header cells smaller font size
			this.fontSize = font.getSize() - 3;
		}
		
		//html jlabel text to apply font size
		@Override
		public void setText(String text) {
			if (font == null) {
				super.setText(text);
				return;
			}
			super.setText(
				"<html><span style=\"font-size:'" + fontSize +
				"'\"><span style=\"font-family:'" + htmlEscape(font) +
				"'\">" + htmlEscape(text) + "</span></span></html>");
		}
		static String htmlEscape(String str) {
			//escape a string to html
			StringBuilder sb = new StringBuilder();
			for (int i = 0, len = str.length(); i < len; i++)
				sb.append("&#").append((int)str.charAt(i)).append(';');
			return sb.toString();
		}
	}
	
	
	//calculate cell heights
	private int normalCellHeight, headerCellHeight;
	private void calcCellHeights() {
		if (ncr == null || hcr == null)
			return;
		Font font = getFont();
		if (font == null)
			return;
		FontMetrics fm = getFontMetrics(getFont());
		normalCellHeight = fm.getHeight() + 2;
		headerCellHeight = fm.getHeight();
		vjsb.setUnitIncrement(normalCellHeight);
	}
	
	
	//get html text for highlighted search range of english searches
	private static final String HIGHLIGHT_COLOR_STR = "#c2d0ff";//light whiteish blue
	private static final Color HIGHLIGHT_COLOR = Color.decode(HIGHLIGHT_COLOR_STR);
	//html coloring
	private static final String HIGHLIGHT_START
		= "<span bgcolor=\"rgb("
			+ HIGHLIGHT_COLOR.getRed()
			+ ", " + HIGHLIGHT_COLOR.getGreen()
			+ ", " + HIGHLIGHT_COLOR.getBlue()
			+ ")\">";
	private static final String HIGHLIGHT_END = "</span>";
	private static String highlightedEnglishSearchResultHtml(SearchResult sr) {
		/* reconstitute the definitions from a dictionary Entry
		 * and add colored html spans between the matched search 
		 * begin and end indexes */
		Entry entry = sr.entry;
		String[] defs = new String[entry.definitions.length];
		for (int i = 0; i < defs.length; i++) {
			defs[i] =
				i == sr.definition
					? htmlifyAndHighlightString(
						entry.definitions[i],
						sr.beginIndex,
						sr.endIndex + 1)
					: htmlifyAndHighlightString(
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
	private static String
	htmlifyAndHighlightString(String str, int startIndex, int endIndex) {
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
			i += Character.charCount(codePoint);
			
			//html highlight
			if (i == endIndex)
				sb.append(HIGHLIGHT_END);
		}
		return sb.toString();
	}
	
	
	
	//Selection and controlling
	
	int getSelectedIndex() {
		return selIndex;
	}
	
	void clearSelection() {
		selIndex = -1;
		revalidate();
		repaint();
	}
	
	void setSelectedIndex(int index) {
		if (index < 0)
			selIndex = 0;
		else if (index >= searchResultLen)
			selIndex = searchResultLen - 1;
		else
			selIndex = index;
		slc.ensureIndexIsVisible(selIndex);
	}
	
	private boolean wheelScrollEnabled = true;
	void setWheelScrollingEnabled(boolean enabled) {
		wheelScrollEnabled = enabled;
	}
	
	Entry getSelectedEntry() {
		if (selIndex < 0 || selIndex >= searchResultLen)
			return null;
		Object o = searchResult.get(selIndex);
		return o instanceof SearchResult ? ((SearchResult)o).entry : null;
	}
}