package sahlaysta.bleco.ui;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

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
			simplified = jcbmi("Simplified", c::simplified);
			traditional = jcbmi("Traditional", c::traditional);
			engSearch = jcbmi("Search English only", c::engSearch);
			font = jmi("Font", c::font);
			mAdd(options, simplified, traditional, null, engSearch, null, font);
		add(options);
			
		entry = new JMenu("Entry");
			next = jmi("Next", c::next);
			previous = jmi("Previous", c::previous);
			open = jmi("Open", c::open);
			close = jmi("Close", c::close);
			copySelEntry = jmi("Copy selected entry", c::copy);
			mAdd(entry, next, previous, open, close, null, copySelEntry);
		add(entry);
			
		handw = new JMenu("Handwriting");
			handwWindow = jcbmi("Handwriting window", c::handw);
			mAdd(handw, handwWindow);
		add(handw);
			
		help = new JMenu("Help");
			about = jmi("About", c::about);
			mAdd(help, about);
		add(help);
	}
	
	
	//helper methods
	private static JMenuItem jmi(String text, Runnable r) {
		return initJMenuItem(new JMenuItem(), text, r);
	}
	private static JCheckBoxMenuItem jcbmi(String text, Runnable r) {
		return (JCheckBoxMenuItem)initJMenuItem(new JCheckBoxMenuItem(), text, r);
	}
	
	private static JMenuItem initJMenuItem(JMenuItem jmi, String text, Runnable r) {
		jmi.setText(text);
		if (r != null)
			jmi.addActionListener(e -> r.run());
		return jmi;
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