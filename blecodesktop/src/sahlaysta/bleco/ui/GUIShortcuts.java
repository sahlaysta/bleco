package sahlaysta.bleco.ui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.plaf.InputMapUIResource;

//utility class that manages look and feel keyboard shortcuts
final class GUIShortcuts {
	
	//constructor
	final GUI gui;
	GUIShortcuts(GUI gui) {
		this.gui = gui;
		init();
	}
	
	
	//define gui shortcuts
	private static KeyStroke[] k(String... s) {
		KeyStroke[] result = new KeyStroke[s.length];
		for (int i = 0; i < result.length; i++)
			result[i] = KeyStroke.getKeyStroke(s[i]);
		return result;
	}
	
	KeyStroke[]
		//the default keystrokes
			
		cut = k("ctrl X"),
		copy = k("ctrl C"),
		paste = k("ctrl V"),
		selectAll = k("ctrl A"),
		undo = k("ctrl Z"),
		redo = k("ctrl Y"),
		
		openEntry = k("ENTER"),
		exitEntry = k("BACK_SPACE", "ESCAPE", "ENTER", "TAB"),
		down = k("DOWN"),
		up = k("UP"),
		next5 = k("PAGE_DOWN"),
		prev5 = k("PAGE_UP"),
		next = k("DOWN", "TAB"),
		prev = k("UP", "shift TAB"),
		
		simplified = k("ctrl S"),
		traditional = k("ctrl T"),
		forceEng = k("ctrl E"),
		copySelEntry = k("ctrl B"),
		
		about = k("F1");
	
	
	
	//banned accelerators for input maps to avoid in menuitems
	private KeyStroke[] bannedAccelerators;
	
	private void init() {
		bannedAccelerators = getBannedAccelerators();
		refreshMenuAccelerators();
	}
	
	void setAccelerator(JMenuItem jmi, KeyStroke ks) {
		if (ks == null)
			return;
		
		jmi.setAccelerator(ks);
		
		//nullify, these are handled by the searcher component
		if (jmi == gui.menuBar.next
			|| jmi == gui.menuBar.previous
			|| jmi == gui.menuBar.open
			|| jmi == gui.menuBar.close) {
			jmi.getInputMap(
				JMenuItem.WHEN_IN_FOCUSED_WINDOW)
					.put(ks, "none");
			return;
		}
		
		//nullify if banned accelerator
		for (KeyStroke bannedAcc: bannedAccelerators) {
			if (bannedAcc.equals(ks)) {
				jmi.getInputMap(
					JMenuItem.WHEN_IN_FOCUSED_WINDOW)
						.put(ks, "none");
				break;
			}
		}
	}
	
	//find and get banned accelerators
	private KeyStroke[] getBannedAccelerators() {
		List<KeyStroke> list = new ArrayList<>();
		
		//get input map from root pane, the enter key presses
		Object[] rootPaneInputMap = (Object[])UIManager.get(
			"RootPane.defaultButtonWindowKeyBindings");
		for (int i = 0; i < rootPaneInputMap.length; i += 2) {
			list.add(KeyStroke.getKeyStroke((String)rootPaneInputMap[i]));
		}
		
		//get input map from toolbar, the arrow key presses
		for (KeyStroke ks: ((InputMapUIResource)UIManager
				.get("ToolBar.ancestorInputMap")).allKeys()) {
			list.add(ks);
		}
		
		return list.toArray(new KeyStroke[list.size()]);
	}
	
	
	//set menu accelerators
	void refreshMenuAccelerators() {
		setAccelerator(gui.menuBar.simplified, simplified[0]);
		setAccelerator(gui.menuBar.traditional, traditional[0]);
		setAccelerator(gui.menuBar.engSearch, forceEng[0]);
		setAccelerator(gui.menuBar.copySelEntry, copySelEntry[0]);
		setAccelerator(gui.menuBar.about, about[0]);
		setAccelerator(gui.menuBar.next, next[0]);
		setAccelerator(gui.menuBar.previous, prev[0]);
		setAccelerator(gui.menuBar.open, openEntry[0]);
		setAccelerator(gui.menuBar.close, exitEntry[0]);
	}
}