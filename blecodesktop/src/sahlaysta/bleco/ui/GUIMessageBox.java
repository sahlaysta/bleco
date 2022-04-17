package sahlaysta.bleco.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

//lazy utility class for java swing message boxes
final class GUIMessageBox {
	
	//methods
	static void showMsg(
			Component comp,
			String title,
			String message) {
		JOptionPane.showMessageDialog(
			comp,
			message,
			title,
			JOptionPane.INFORMATION_MESSAGE);
	}
	static void showErrorMsg(
			Component comp,
			String title,
			String message) {
		JOptionPane.showMessageDialog(
			comp,
			message,
			title,
			JOptionPane.ERROR_MESSAGE);
	}
	static void showErrorMsg(
			Component comp,
			String title,
			Throwable e) {
		JOptionPane.showMessageDialog(
			comp,
			stackTraceToString(e),
			title,
			JOptionPane.ERROR_MESSAGE);
	}
	private static String
	stackTraceToString(Throwable e) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		return sw.toString();
	}
	
	
	
	//Shows the about message dialog
	static void showAboutMsg(GUI gui) {
		//Initialize dialog
		JDialog jDialog = new JDialog(gui, true);
		jDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		jDialog.setTitle("About Bleco");
		jDialog.setLayout(new BorderLayout());
		
		JTabbedPane jtp = new JTabbedPane();
		
		//About pane
		jtp.add("About", createJta(ABOUT_MSG));
		
		//License pane
		jtp.add("License", createJta(LICENSE_MSG));
		
		jDialog.add(jtp, BorderLayout.CENTER);
		
		//OK button
		JPanel buttonPanel = new JPanel();
		JButton jButton = new JButton("OK");
		jButton.addActionListener(e -> jDialog.dispose());
		buttonPanel.setLayout(new GridBagLayout());
		buttonPanel.add(jButton, new GridBagConstraints());
		jDialog.add(buttonPanel, BorderLayout.SOUTH);
		jDialog.addWindowListener(new WindowAdapter() {
			//set focus to jbutton when opened
			@Override
			public void windowActivated(WindowEvent e) {
				jDialog.removeWindowListener(this);
				jButton.requestFocus();
			}
		});
		
		//Show jdialog
		jDialog.setSize(400, 250);
		jDialog.setLocationRelativeTo(gui);
		jDialog.setVisible(true);
	}
	private static JComponent createJta(String text) {
		//uneditable jtextarea
		JTextArea jta = new JTextArea();
		jta.setText(text);
		jta.setLineWrap(true);
		jta.setWrapStyleWord(true);
		jta.setCaretPosition(0);
		GUIJTextField.setUneditable(jta);
		JScrollPane jsp = new JScrollPane(jta);
		jsp.setBorder(null);
		return jsp;
	}

	
	
	//About message
	private static final String ABOUT_MSG = ""
		+ "Bleco v1.1h written by porog\n"
		+ "https://github.com/sahlaysta/bleco\n\n"
		+ "CC-CEDICT belongs to\nMDBG:\n"
		+ "https://cc-cedict.org/wiki/\n\n"
		+ "Example sentences are obtained from\nTatoeba:\n"
		+ "https://tatoeba.org/en/\n\n"
		+ "HanziLookup belongs to\nJordan Kiang:\n"
		+ "https://www.kiang.org/jordan/software/hanzilookup/";
	
	//License message
	private static final String LICENSE_MSG = ""
		+ "Bleco\n"
		+ "Copyright 2021-2022 sahlaysta"
		+ " https://github.com/sahlaysta/bleco\n"
		+ "Licensed under the Apache License, Version 2.0 (the \"License\");"
		+ " you may not use this Work except in compliance with the License."
		+ " You may obtain a copy of the License at\n"
		+ "http://www.apache.org/licenses/LICENSE-2.0\n"
		+ "Unless required by applicable law or agreed to in writing, software"
		+ " distributed under the License is distributed on an \"AS IS\" BASIS,"
		+ " WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied."
		+ " See the License for the specific language governing permissions and"
		+ " limitations under the License.\n\n"
		
		+ "CC-CEDICT\n"
		+ "Copyright (c) MDBG 2022"
		+ " mdbg.net\n"
		+ "Licensed under a Creative Commons Attribution-ShareAlike 4.0 International"
		+ " License. It more or less means that you are allowed to use this data for"
		+ " both non-commercial and commercial purposes provided that you: mention where"
		+ " you got the data from (attribution) and that in case you improve / add to the"
		+ " data you will share these changes under the same license (share alike).\n\n"
		
		+ "Tatoeba\n"
		+ "tatoeba.org\n"
		+ "Tatoeba's technical infrastructure uses the default Creative Commons"
		+ " Attribution 2.0 France license (CC-BY 2.0 FR) for the use of textual sentences."
		+ " You are responsible for your use, reuse, modification and dissemination of the"
		+ " content available on Tatoeba. Thus, if you circulate a sentence under license,"
		+ " it is your responsibility to circulate it with its license. For example, in the"
		+ " case of a CC-BY license, it is your responsibility to quote the author of the"
		+ " sentence.\n\n"
		
		+ "Jordan Kiang\n"
		+ "Copyright (C) 2005 Jordan Kiang"
		+ " kiang.org/jordan\n"
		+ "This program is free software; you can redistribute it and/or modify it under"
		+ " the terms of the GNU General Public License as published by the Free Software"
		+ " Foundation; either version 2 of the License, or (at your option) any later"
		+ " version.";
		
}