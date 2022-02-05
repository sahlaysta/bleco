package com.github.sahlaysta.bleco.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

// a small font chooser window
final class GUIFontPicker {
	
	static void show(GUI gui) {
		String originalFont = gui.prefs.getFont();
		int originalFontSize = gui.prefs.getFontSize();
		
		//Create the JDialog font picker
		JDialog jDialog = new JDialog(gui, true);
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
		JTextField jTextField = new JTextField(originalFontSize + "");
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
		jComboBox.setSelectedItem(
			gui.searcher.searchField.getFont().getFamily());
		
		//Button action listeners
		ActionListener acceptAction = e -> {
			
			//Get the text field's entered font size
			int newFontSize;
			try {
				newFontSize = Integer.parseInt(jTextField.getText());
			} catch (NumberFormatException e2) { //error message window
				GUIMessageBox.showErrorMsg(
					jDialog,
					"Error",
					"Invalid input: " + jTextField.getText());
				return;
			}
			
			if (newFontSize < 0) { //if bad font size
				GUIMessageBox.showErrorMsg(
					jDialog,
					"Error",
					"Font size cannot be less than zero");
				return;
			}
			
			//Set font and close
			gui.searcher.setFont(
				(String)jComboBox.getSelectedItem(), newFontSize);
		};
		ActionListener acceptAndCloseAction = e -> {
			acceptAction.actionPerformed(null);
			jDialog.dispose();
		};
		okButton.addActionListener(acceptAndCloseAction);
		jTextField.addActionListener(acceptAction);
		jComboBox.addActionListener(acceptAction);
		cancelButton.addActionListener(e -> {
			gui.searcher.setFont(
				originalFont, originalFontSize);
			jDialog.dispose();
		});
		
		//Show the JDialog
		jDialog.add(mainPanel);
		jDialog.pack();
		jDialog.setLocationRelativeTo(gui);
		jDialog.setVisible(true);
	}
}