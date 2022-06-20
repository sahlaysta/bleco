package sahlaysta.bleco.ui;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Container;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ContainerEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;
import javax.swing.text.TextAction;
import javax.swing.undo.UndoManager;

//A better custom jtextfield
final class GUIJTextField extends JTextField {
	private static final long serialVersionUID = 1L;

	//Constructor
	final GUI gui;
	GUIJTextField(GUI gui) {
		this.gui = gui;
		configUiManager(gui);
	}
	
	//setText clears undomanager
	@Override
	public void setText(String t) {
		super.setText(t);
		UndoManager um = getUndoManager(this);
		if (um != null)
			um.discardAllEdits();
	}
	
	
	//configure UIManager global text field
	private static boolean configured = false;
	private static void configUiManager(GUI gui) {
		if (configured)
			return;
		configured = true;
		
		//shortcuts
		GUIShortcuts gs = gui.shortcuts;
		
		/* make it so that clicking out of a
		 * popup menu doesnt disable the
		 * next click action */
		UIManager.put("PopupMenu.consumeEventOnClose", Boolean.FALSE);
		
		
		//give all jtextcomponents an undomanager
		Toolkit.getDefaultToolkit()
		.addAWTEventListener(e -> {
			//get containerevent
			if (!(e instanceof ContainerEvent))
				return;
			if (!(e.getSource() instanceof Container))
				return;
			ContainerEvent ce = (ContainerEvent)e;
			
			//only 'component added' events
			if (ce.getID() != ContainerEvent.COMPONENT_ADDED)
				return;
			
			//get jtextcomponent
			Component child = ce.getChild();
			if (!(child instanceof JTextComponent))
				return;
			JTextComponent jtc = (JTextComponent)child;
			
			//return if already has an undo manager
			if (getUndoManager(jtc) != null)
				return;
			
			//add undo manager
			jtc.getDocument()
			.addUndoableEditListener(new UndoManager());
		}, AWTEvent.CONTAINER_EVENT_MASK);
		
		
		//text field right click menu with cut, copy, paste etc.
		Toolkit.getDefaultToolkit()
		.addAWTEventListener(e -> {
			//get jtextcomponent and mouseevent
			if (!(e instanceof MouseEvent))
				return;
			Object o = e.getSource();
			if (!(o instanceof JTextComponent))
				return;
			MouseEvent me = (MouseEvent)e;
			JTextComponent jtc = (JTextComponent)o;
			
			//must be mouse popup trigger
			if (!me.isPopupTrigger())
				return;
			
			//get jtextcomponent's undomanager
			final UndoManager um = getUndoManager(jtc);
			
			//create jpopupmenu
			JPopupMenu jpm = new JPopupMenu();
			JMenuItem cut = new JMenuItem("Cut");
			JMenuItem copy = new JMenuItem("Copy");
			JMenuItem paste = new JMenuItem("Paste");
			JMenuItem selAll = new JMenuItem("Select all");
			JMenuItem undo = new JMenuItem("Undo");
			JMenuItem redo = new JMenuItem("Redo");
			ActionListener reqFoc = e2 -> jtc.requestFocus();
			cut.addActionListener(e2 -> jtc.cut());
			copy.addActionListener(e2 -> jtc.copy());
			paste.addActionListener(e2 -> jtc.paste());
			selAll.addActionListener(e2 -> jtc.selectAll());
			undo.addActionListener(e2 -> {
				if (um != null && um.canUndo())
					um.undo();
			});
			redo.addActionListener(e2 -> {
				if (um != null && um.canRedo())
					um.redo();
			});
			cut.addActionListener(reqFoc);
			copy.addActionListener(reqFoc);
			paste.addActionListener(reqFoc);
			selAll.addActionListener(reqFoc);
			undo.addActionListener(reqFoc);
			redo.addActionListener(reqFoc);
			gs.setAccelerator(cut, gs.cut[0]);
			gs.setAccelerator(copy, gs.copy[0]);
			gs.setAccelerator(paste, gs.paste[0]);
			gs.setAccelerator(selAll, gs.selectAll[0]);
			gs.setAccelerator(undo, gs.undo[0]);
			gs.setAccelerator(redo, gs.redo[0]);
			if (isUneditable(jtc)) {
				cut.setEnabled(false);
				paste.setEnabled(false);
				undo.setEnabled(false);
				redo.setEnabled(false);
			}
			if (um == null) {
				undo.setEnabled(false);
				redo.setEnabled(false);
			}
			jpm.add(cut);
			jpm.add(copy);
			jpm.add(paste);
			jpm.add(selAll);
			jpm.addSeparator();
			jpm.add(undo);
			jpm.add(redo);
			
			//show popup menu
			jpm.show(jtc, me.getX(), me.getY());
		}, AWTEvent.MOUSE_EVENT_MASK);

		
		//get global text field action map and input map
		ActionMap am = (ActionMap)UIManager.get("TextField.actionMap");
		InputMap im = (InputMap)UIManager.get("TextField.focusInputMap");
		
		/* Disable the beep sound when you
		 * backspace in a text field while
		 * it is empty / the caret position
		 * is at the beginning */
		Object backspaceActionKey = DefaultEditorKit.deletePrevCharAction;
		Action defaultBckspaceAct = am.get(backspaceActionKey);
		am.put(backspaceActionKey, new AbstractAction() {
			static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e) {
				//get jtextcomponent
				Object o = e.getSource();
				if (!(o instanceof JTextComponent)) {
					defaultBckspaceAct.actionPerformed(e);
					return;
				}
				JTextComponent jtc = (JTextComponent)o;
				
				//check caret position at beginning
				if (jtc.getSelectionStart() == 0
					&& jtc.getSelectionEnd() == 0)
					return;
				
				defaultBckspaceAct.actionPerformed(e);
			}
		});
		
		//ctrl Z and ctrl Y to undo / redo
		String u = "ctrlzundo";
		String r = "ctrlyredo";
		am.put(u, new AbstractAction(u) {
			static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e) {
				//get jtextcomponent
				Object o = e.getSource();
				if (!(o instanceof JTextComponent))
					return;
				JTextComponent jtc = (JTextComponent)o;
				
				//ensure jtextcomponent is editable
				if (isUneditable(jtc))
					return;
				
				//get undomanager
				UndoManager um = getUndoManager(jtc);
				if (um == null)
					return;
				
				//undo
				if (um.canUndo())
					um.undo();
			}
		});
		am.put(r, new AbstractAction(r) {
			static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e) {
				//get jtextcomponent
				Object o = e.getSource();
				if (!(o instanceof JTextComponent))
					return;
				JTextComponent jtc = (JTextComponent)o;
				
				//ensure jtextcomponent is editable
				if (isUneditable(jtc))
					return;
				
				//get undomanager
				UndoManager um = getUndoManager(jtc);
				if (um == null)
					return;
				
				//redo
				if (um.canRedo())
					um.redo();
			}
		});
		for (KeyStroke ks: gs.undo)
			im.put(ks, u);
		for (KeyStroke ks: gs.redo)
			im.put(ks, r);

		
		/* (since jtextfield doesnt do this by default)
		 * If the text field contains selected text
		 * and the left arrow key is pressed, move
		 * the caret to the beginning of the
		 * selection. And if the right arrow key
		 * is pressed, move to the end */
		am.put(DefaultEditorKit.forwardAction, new TextAction(
				"Caret Forward") {
			static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e) {
				JTextComponent jtc = getFocusedComponent();
				int len = jtc.getDocument().getLength();
				int selEnd = jtc.getSelectionEnd();
				int newPos
					= selEnd == jtc.getSelectionStart()
						? selEnd + 1
						: selEnd;
				jtc.setCaretPosition(newPos > len ? len : newPos);
			}
		});
		am.put(DefaultEditorKit.backwardAction, new TextAction(
				"Caret Backward") {
			static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e) {
				JTextComponent jtc = getFocusedComponent();
				int selStart = jtc.getSelectionStart();
				int newPos
					= selStart == jtc.getSelectionEnd()
						? selStart - 1
						: selStart;
				jtc.setCaretPosition(newPos >= 0 ? newPos : 0);
			}
		});
	}
	
	
	
	
	//JTextComponent Static methods
	
	//get an undomanager from a jtextcomponent
	static UndoManager getUndoManager(JTextComponent jtc) {
		if (jtc == null)
			return null;
		Document doc = jtc.getDocument();
		if (doc instanceof AbstractDocument) {
			UndoableEditListener[] uels
				= ((AbstractDocument)doc)
					.getUndoableEditListeners();
			if (uels == null)
				return null;
			for (UndoableEditListener uel: uels)
				if (uel instanceof UndoManager)
					return (UndoManager)uel;
		}
		return null;
	}
	
	
	//uneditable jtextcomponent with functioning caret
	
	static final List<JTextComponent> UNEDITABLE_JTEXTCOMPONENTS = new ArrayList<>();
	
	/* configures a jtextcomponent so that the caret still
	 * functions but the contents cannot be edited */
	static void setUneditable(JTextComponent jtc) {
		if (jtc == null)
			return;
		Document doc = jtc.getDocument();
		if (!(doc instanceof AbstractDocument))
			return;
		((AbstractDocument)doc).setDocumentFilter(EMPT_DOC_FILTER);
		UNEDITABLE_JTEXTCOMPONENTS.add(jtc);
	}
	
	/* remove jtextcomponent from the list to prevent
	 * memory leak */
	static void disposeUneditable(Container c) {
		if (c == null)
			return;
		for (Component c2: GUIUtil.getAllComponents(c)) {
			if (!(c2 instanceof JTextComponent))
				continue;
			JTextComponent jtc = (JTextComponent)c2;
			UNEDITABLE_JTEXTCOMPONENTS.remove(jtc);
		}
	}
	
	//check if a jtextcomponent's contents are not editable
	private static boolean isUneditable(JTextComponent jtc) {
		if (jtc == null)
			return false;
		for (JTextComponent jtc2: UNEDITABLE_JTEXTCOMPONENTS)
			if (jtc == jtc2)
				return true;
		return !(jtc.isEnabled() && jtc.isEditable());
	}
	
	//document filter that ignores all inputs
	private static final DocumentFilter EMPT_DOC_FILTER = new DocumentFilter() {
		@Override
		public void insertString(
			FilterBypass fb,
			int offset,
			String string,
			AttributeSet attr) {}
		@Override
		public void remove(
			FilterBypass fb,
			int offset,
			int length) {}
		@Override
		public void replace(
			FilterBypass fb,
			int offset,
			int length,
			String text,
			AttributeSet attr) {}
	};
}