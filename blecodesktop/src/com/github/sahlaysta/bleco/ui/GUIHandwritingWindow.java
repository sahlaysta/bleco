package com.github.sahlaysta.bleco.ui;

import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;

import com.github.sahlaysta.bleco.Main;

import hanzidict.sourceforge.net.handwriting.hanzilookup.HanziLookup;

// uses hanzilookup by Jordan Kiang
final class GUIHandwritingWindow extends JDialog {
	private static final long serialVersionUID = 1L;

	//Constructor
	final GUI gui;
	GUIHandwritingWindow(GUI gui) {
		super(gui, false);
		this.gui = gui;
		setTitle("Handwriting");
		setResizable(false);
		Point p = gui.prefs.getHandwritingWindowLocation();
		if (p != null)
			setLocation(p);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				gui.menuBar.handwWindow.setSelected(false);
				gui.controller.hw = null;
				dispose();
			}
		});
		init();
	}
	
	
	//Components
	HanziLookup hanziLookup;
	private void init() {
		//Load hanzilookup
		try {
			hanziLookup = new HanziLookup(
				Main.getResource(
					"strokes-extended.dat"),
					gui.searcher.searchField.getFont());
		} catch (Throwable e) {
			GUIMessageBox.showErrorMsg(
				gui, "HanziLookup load fail", e);
			return;
		}
		
		//Layout
		add(hanziLookup);
		pack();
		
		/* Change "Enter character" text
		 * to "Draw character" */
		GUIUtil.forEachComponent(
			hanziLookup,
			c -> {
				if (c instanceof JComponent) {
					Border b = ((JComponent) c).getBorder();
					if (!(b instanceof TitledBorder))
						return;
					TitledBorder tb = (TitledBorder)b;
					if ("Enter character".equals(tb.getTitle()))
					tb.setTitle("Draw character");
				}
			});
		
		//make everything non-focusable
		setFocusableWindowState(false);
		GUIUtil.forEachComponent(
			this,
			c -> c.setFocusable(false));
		
		//Input entered characters
		hanziLookup.addCharacterReceiver(e->
			inputChinese(e.getSelectedCharacter()));
		
		//Listen to window moved
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentMoved(ComponentEvent e) {
				gui.prefs.setHandwritingWindowLocation(getLocation());
			}
		});
	}
	
	/* Inputs the selected Chinese character into
	 * the search field */
	private final void inputChinese(char ch) {
		JTextComponent jtc = gui.searcher.searchField;
		if (!jtc.hasFocus())
			return;
		int selStart = jtc.getSelectionStart(),
			selEnd = jtc.getSelectionEnd();
		try {
			((AbstractDocument)jtc.getDocument())
				.replace(
					selStart,
					selEnd - selStart,
					Character.toString(ch),
					null);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}
}