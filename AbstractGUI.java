package bleco;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

abstract class AbstractGUI extends JFrame {
	private static final long serialVersionUID = 1L;
	private boolean themed; /* public field */ public boolean isThemed() { return themed; }
	abstract void jTextField_changed(String text);
	abstract void handwritingOption_click(boolean checked);
	abstract void characterOption_changed(int option);
	abstract void jList_doubleClick(int index);
	abstract void aboutOption_click();
	
	static final String TITLE = "Bleco";
	JTextField jTextField;
	DefaultListModel<String> listModel;
	JList<String> jList;
	JCheckBoxMenuItem simplifiedOption, traditionalOption, handwritingOptionCheckbox;
	public AbstractGUI() {
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setLocationRelativeTo(null);
		setSize(350, 250);
		setTitle(TITLE);
		setIconImage(new ImageIcon(getClass().getResource("icon.png")).getImage());
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			themed = true;
		} catch (
			ClassNotFoundException
			| InstantiationException
			| IllegalAccessException
			| UnsupportedLookAndFeelException e) {
			themed = false;
			e.printStackTrace();
		}
	}
	public void initComponent() {
		JMenuBar jMenuBar = new JMenuBar();
		JMenu optionsJMenu = new JMenu("Options");
			simplifiedOption = new JCheckBoxMenuItem("Simplified");
			traditionalOption = new JCheckBoxMenuItem("Traditional");
			simplifiedOption.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					characterOption_changed(CHARACTER_SIMPLIFIED);
					setCharacterOption(CHARACTER_SIMPLIFIED);
				}
			});
			traditionalOption.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					characterOption_changed(CHARACTER_TRADITIONAL);
					setCharacterOption(CHARACTER_TRADITIONAL);
				}
			});
			optionsJMenu.add(simplifiedOption);
			optionsJMenu.add(traditionalOption);
		JMenu handwritingJMenu = new JMenu("Handwriting");
			handwritingOptionCheckbox = new JCheckBoxMenuItem("Handwriting window");
			handwritingOptionCheckbox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					handwritingOption_click(
							handwritingOptionCheckbox.isSelected());
				}
			});
			handwritingJMenu.add(handwritingOptionCheckbox);
		JMenu helpJMenu = new JMenu("Help");
			JMenuItem aboutOption = new JMenuItem("About");
			aboutOption.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					aboutOption_click();
				}
			});
			helpJMenu.add(aboutOption);
		jMenuBar.add(optionsJMenu);
		jMenuBar.add(handwritingJMenu);
		jMenuBar.add(helpJMenu);
		setJMenuBar(jMenuBar);
		
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());
		JPanel textBoxPanel = new JPanel();
		jTextField = new JTextField();
		textBoxPanel.setLayout(new GridLayout(1,1));
		textBoxPanel.setBorder(new EmptyBorder(7,7,3,7));
		textBoxPanel.add(jTextField);
		mainPanel.add(textBoxPanel, BorderLayout.NORTH);
		listModel = new DefaultListModel<>();
		jList = new JList<>(listModel);
		jList.setBorder(new EmptyBorder(3,9,3,9));
		jList.setFont(jList.getFont().deriveFont(15f));
		jList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		jList.addMouseListener(new MouseAdapter() {
		    public void mouseClicked(MouseEvent evt) {
	            // Double-click detected
		        if (evt.getClickCount() == 2)
		            jList_doubleClick(jList.locationToIndex(evt.getPoint()));
		    }
		});
		JScrollPane jScrollPane = new JScrollPane(jList);
		jScrollPane.setBorder(new EmptyBorder(0,0,0,0));
		mainPanel.add(jScrollPane, BorderLayout.CENTER);
		add(mainPanel);

		textBoxPanel.setBackground(jList.getBackground()); //style color
		
		jTextField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void changedUpdate(DocumentEvent arg0) {
				jTextField_changed(jTextField.getText());
			}
			@Override
			public void insertUpdate(DocumentEvent arg0) {
				jTextField_changed(jTextField.getText());
			}
			@Override
			public void removeUpdate(DocumentEvent arg0) {
				jTextField_changed(jTextField.getText());
			}
		});
		
		
		//Set focus
		jTextField.requestFocus();
	}
	
	public static final int CHARACTER_SIMPLIFIED = 0, CHARACTER_TRADITIONAL = 1; //character enum
	public void setCharacterOption(int selection) {
		switch (selection) {
		case CHARACTER_SIMPLIFIED: {
			simplifiedOption.setSelected(true);
			traditionalOption.setSelected(false);
			break;
		}
		case CHARACTER_TRADITIONAL: {
			simplifiedOption.setSelected(false);
			traditionalOption.setSelected(true);
			break;
		}
		}
	}
	
	// Loading screen
	JPanel loadingPanel;
	private JPanel createLoadingPanel() {
		JPanel output = new JPanel();
		output.setBorder(new EmptyBorder(0,0,30,0));
		output.setLayout(new BorderLayout());
		output.setBackground(getBackground());
		JLabel loadingLabel = new JLabel("Loading dictionary...", SwingConstants.CENTER);
		Font font = loadingLabel.getFont().deriveFont(18f);
		loadingLabel.setFont(font);
		loadingLabel.setEnabled(false);
		output.add(loadingLabel, BorderLayout.CENTER);
		return output;
	}
	public void setLoading(boolean loading) {
		if (loading) {
			setVisible(true);
			loadingPanel = createLoadingPanel();
			add(loadingPanel);
			repaint();
		} else {
			remove(loadingPanel);
			loadingPanel = null;
		}
	}
}
