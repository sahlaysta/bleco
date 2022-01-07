package com.github.sahlaysta.bleco.ui;

import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.UIManager;

import com.github.sahlaysta.bleco.Main;
import com.github.sahlaysta.bleco.dict.Dictionary;

/** GUI class of Bleco
 * @author porog */
public final class GUI extends JFrame {
	private static final long serialVersionUID = 1L;
	
	//Bleco Dictionary
	Dictionary dict;
	
	//Components
	GUIJMenuBar menuBar;
	GUIController controller;
	GUISearcher searcher;
	GUIPreferences prefs;
	
	/** Creates and initializes Bleco GUI */
	public GUI() {
		super("Bleco");
		try {
			UIManager.setLookAndFeel(
				UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		setMinimumSize(new Dimension(240, 180));
		setIconImage(new ImageIcon(
			Main.getResourceBytes("icon.png")).getImage());
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				prefs.trySave();
				GUI.this.dispose();
				System.exit(0);
			}
		});
		
		init();
	}
	
	private void init() {
		loadDictionary();
		
		controller = new GUIController(this);

		menuBar = new GUIJMenuBar(this);
		setJMenuBar(menuBar);
		
		searcher = new GUISearcher(this);
		add(searcher);
		
		prefs = new GUIPreferences(this);
		prefs.tryLoad();
	}
	
	private void loadDictionary() {
		dict = new Dictionary();
		try {
			dict.load(Main.getResource("dict.bleco"));
		} catch (Throwable e) {
			e.printStackTrace();
			GUIMessageBox.showErrorMsg(
				this, "Bleco dictionary load fail", e);
			System.exit(0);
		}
	}
}