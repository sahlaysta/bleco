package com.github.sahlaysta.bleco;

import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;

import com.github.sahlaysta.bleco.handwriting.hanzilookup.HanziLookup;

final class HandwritingWindow extends JDialog {
	private static final long serialVersionUID = 1L;
	private HanziLookup hanziLookup;
	private JTextComponent jtc;
	public HandwritingWindow(Frame frame, JTextComponent jtc, Font font) {
		super(frame, false);
		setTitle("Handwriting");
		setResizable(false);
		setLocation(Preferences.getHandwritingWindowX(),
				Preferences.getHandwritingWindowY());
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		this.jtc = jtc;
		
		//Load HandwritingWindow and show brief loading screen
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentShown(ComponentEvent e) {
				removeComponentListener(this);
				
				//Loading label
				JLabel loadingLabel = new JLabel(
						"Loading handwriting...",
						SwingConstants.CENTER);
				loadingLabel.setEnabled(false);
				loadingLabel.setFont(loadingLabel.getFont().deriveFont(20f));
				add(loadingLabel);
				pack();
				revalidate();
				repaint();

				//Load HandWriting window resources
				new SwingWorker<Object, Object>() {
					@Override
					protected Object doInBackground() throws Exception {
						hanziLookup = new HanziLookup(
								GUI.class.getResourceAsStream(
										"resources/strokes-extended.dat"),
								font);
						return null;
					}
					@Override
					protected void done() {
						remove(loadingLabel);
						initComponents();
					}
				}.execute();
			}
		});
	}
	private final void initComponents() {
		add(hanziLookup);
		pack();
		
		Set<Component> components = getAllComponents(this);
		for (Component c: components) {
			//Always put focus on the JTextComponent
			c.addFocusListener(new FocusAdapter() {
				@Override
				public void focusGained(FocusEvent e) {
					jtc.requestFocus();
				}
			});
			
			//Modify "Enter character" text to "Draw character"
			if (c instanceof JComponent) {
				Border b = ((JComponent)c).getBorder();
				if (!(b instanceof TitledBorder))
					continue;
				TitledBorder tb = (TitledBorder)b;
				if ("Enter character".equals(tb.getTitle()))
					tb.setTitle("Draw character");
			}
		}
		
		hanziLookup.addCharacterReceiver(e->
			inputChinese(e.getSelectedCharacter()));

		revalidate();
		repaint();
		jtc.requestFocus();
	}
	private final
	Set<Component> getAllComponents(Container container) {
		/* Gets all Components and sub Components of
		 * any Container object */
		return addAllSubComponents(container, new HashSet<>());
	}
	private final Set<Component> addAllSubComponents(
			Container container,
			Set<Component> components) {
		components.add(container);
		for (Component c: container.getComponents()) {
			components.add(c);
			if (c instanceof Container)
				addAllSubComponents((Container)c, components);
		}
		return components;
	}
	
	//Sets the HanziLookup character font
	@Override
	public void setFont(Font font) {
		hanziLookup.setFont(font);
	}
	
	
	/* Inputs the selected Chinese character into
	 * the registered JTextComponent */
	private final void inputChinese(char ch) {
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