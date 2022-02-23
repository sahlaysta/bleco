package com.github.sahlaysta.bleco.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.event.ActionListener;

import javax.accessibility.Accessible;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.ComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;

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
		
		//Font options panel layout
		JPanel optionsPanel = new JPanel();
		optionsPanel.setBorder(new EmptyBorder(0, 0, 0, 5));
		JComboBox<String> jComboBox = new WiderJComboBox<>();
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
				.getAvailableFontFamilyNames()) {
			jComboBox.addItem(s);
		}
		jComboBox.setSelectedItem(
			gui.searcher.searchField.getFont().getFamily());
		
		//Button action listeners
		ActionListener acceptAction = e -> {
			
			//Get the text field's entered font size
			int newFontSize;
			try {
				newFontSize = Integer.parseInt(jTextField.getText());
			//handle invalid
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
	
	
	//jcombobox with wide dropdown
	private static final class WiderJComboBox<T> extends JComboBox<T> {
		private static final long serialVersionUID = 1L;
		
		//constructor
		final JScrollBar vjsb;//vertical jscrollbar of jcombobox
		WiderJComboBox() {
			this.vjsb = findVerticalJScrollBar();
		}

		//safely get vertical jscrollbar of combobox
		JScrollBar findVerticalJScrollBar() {
			ComboBoxUI cbui = getUI();
			int len = cbui.getAccessibleChildrenCount(this);
			for (int i = 0; i < len; i++) {
				//ensure BasicComboPopup instance
				Accessible a = cbui.getAccessibleChild(this, i);
				if (!(a instanceof BasicComboPopup))
					continue;
				BasicComboPopup bcp = (BasicComboPopup)a;
				
				//get JScrollBar of BasicComboPopup
				for (Object o: GUIUtil.getAllComponents(bcp)) {
					//ensure is JScrollBar and is vertical
					if (!(o instanceof JScrollBar))
						continue;
					JScrollBar jsb = (JScrollBar)o;
					if (jsb.getOrientation() != JScrollBar.VERTICAL)
						continue;
					return jsb;
				}
			}
			return null;
		}
		
		//hook into jcombobox's size
		@Override
		public Dimension getSize() {
			Dimension d = super.getSize();
			int listWidth = getWidestItemWidth();
			if (listWidth > 0)
				d.width = listWidth;
			return d;
		}
		
		//find width of widest item
		int getWidestItemWidth() {
			int len = getItemCount();
			Font font = getFont();
			FontMetrics fm = getFontMetrics(font);
			int widest = 0;
			for (int i = 0; i < len; i++) {
				Object item = getItemAt(i);
				int lineWidth = fm.stringWidth(item.toString());
				widest = Math.max(widest, lineWidth);
			}
			return widest + 5 + getVerticalJScrollbarWidth();
		}
		
		//get vertical jscrollbar width safely
		int getVerticalJScrollbarWidth() {
			if (vjsb == null)
				return 0;
			int width = vjsb.getWidth();
			if (width > 0)
				return width;
			return vjsb.getPreferredSize().width;
		}
	}
}