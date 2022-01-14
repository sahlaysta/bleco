package com.github.sahlaysta.bleco.ui;

import java.awt.event.KeyEvent;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

//The menu bar component at the top of Bleco GUI
final class GUIJMenuBar extends JMenuBar {
	private static final long serialVersionUID = 1L;
	
	//Constructor
	final GUI gui;
	GUIJMenuBar(GUI gui) {
		this.gui = gui;
		init();
	}
	
	//Menu + menu items
	JMenu options;
		JCheckBoxMenuItem simplified;
		JCheckBoxMenuItem traditional;
		JCheckBoxMenuItem engSearch;
		JMenuItem font;
	JMenu entry;
		JMenuItem next;
		JMenuItem previous;
		JMenuItem open;
		JMenuItem close;
		JMenuItem copySelEntry;
	JMenu handw;
		JCheckBoxMenuItem handwWindow;
	JMenu help;
		JMenuItem about;
	private void init() {
		GUIController c = gui.controller;
		
		options = new JMenu("Options");
			simplified = jcbmi("Simplified", c::simplified, "ctrl S");
			traditional = jcbmi("Traditional", c::traditional, "ctrl T");
			engSearch = jcbmi("Search English only", c::engSearch, "ctrl E");
			font = jmi("Font", c::font, null);
			mAdd(options, simplified, traditional, null, engSearch, null, font);
		add(options);
			
		entry = new JMenu("Entry");
			next = jmi("Next", c::next, "DOWN");
			previous = jmi("Previous", c::previous, "UP");
			open = jmi("Open", c::open, "ENTER");
			close = jmi("Close", c::close, "BACK_SPACE");
			copySelEntry = jmi("Copy selected entry", c::copy, "ctrl B");
			mAdd(entry, next, previous, open, close, null, copySelEntry);
		add(entry);
			
		handw = new JMenu("Handwriting");
			handwWindow = jcbmi("Handwriting window", c::handw, null);
			mAdd(handw, handwWindow);
		add(handw);
			
		help = new JMenu("Help");
			about = jmi("About", c::about, "F1");
			mAdd(help, about);
		add(help);
	}
	
	
	//helper methods
	private static JMenuItem
	jmi(String text, Runnable r, String shortcut) {
		return initJMenuItem(new JMenuItem(), text, r, shortcut);
	}
	private static JCheckBoxMenuItem
	jcbmi(String text, Runnable r, String shortcut) {
		return (JCheckBoxMenuItem)initJMenuItem(
				new JCheckBoxMenuItem(), text, r, shortcut);
	}
	
	private static JMenuItem initJMenuItem(
			JMenuItem jmi, String text, Runnable r, String shortcut) {
		jmi.setText(text);
		if (r != null)
			jmi.addActionListener(e -> r.run());
		if (shortcut != null)
			setShortcut(jmi, shortcut);
		return jmi;
	}
	
	private static void setShortcut(
			JMenuItem jmi, String shortcut) {
		KeyStroke ks = KeyStroke.getKeyStroke(shortcut);
		jmi.setAccelerator(ks);

		/* If the shortcut has no modifier
		 * (no ctrl, shift, etc.) and is not a function
		 * key (F1, F2, etc.), makes its
		 * key press non-functional */
		if (ks.getModifiers() == 0
				&& ks.getKeyCode() < KeyEvent.VK_F1) {
			jmi.getInputMap(
				JMenuItem.WHEN_IN_FOCUSED_WINDOW)
					.put(ks, "none");
		}
	}
	
	private static void mAdd(JMenu menu, JMenuItem... items) {
		for (JMenuItem item: items) {
			if (item == null)
				menu.addSeparator();
			else
				menu.add(item);
		}
	}
}