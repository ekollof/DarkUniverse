/*
 * DarkUniverseView.java
 */
package darkuniverseapp;

import java.awt.Color;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.ListSelectionEvent;
import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.TaskMonitor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.Timer;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import wox.serial.Easy;
import net.sourceforge.jsorter.*;

/**
 * The application's main frame.
 */
public class DarkUniverseView extends FrameView {

	Vector Powers;
	Player player = new Player();
	SwingSorter sort = new SwingSorter();
	DefaultListModel pwrModel = new DefaultListModel();

	public DarkUniverseView(SingleFrameApplication app) {
		super(app);


		initComponents();

		PowerList.setModel(new DefaultListModel());
		pwrModel = (DefaultListModel) PowerList.getModel();

		try {
			// Load character data if present
			this.loadCharacter();
		} catch (FileNotFoundException ex) {
			Logger.getLogger(DarkUniverseView.class.getName()).log(Level.WARNING, null, ex);
		} catch (IOException ex) {
			Logger.getLogger(DarkUniverseView.class.getName()).log(Level.SEVERE, null, ex);
		} catch (ClassNotFoundException ex) {
			Logger.getLogger(DarkUniverseView.class.getName()).log(Level.SEVERE, null, ex);
		}

		// Add selectionlistener for Powerlist
		PowerList.addListSelectionListener(
			new ListSelectionListener() {
				public void valueChanged(ListSelectionEvent e) {

					if (e.getValueIsAdjusting() == false) {
						if (PowerList.getSelectedIndex() == -1) {
							// No selection
							PowerDeleteBtn.setEnabled(false);
							PowerSaveBtn.setEnabled(true);
						} else {
							PowerDeleteBtn.setEnabled(true);
							ComboBoxModel pwrCompleteModel = PowerCompleted.getModel();
							Power p = new Power();

							p = fetchPower(pwrModel.get(PowerList.getSelectedIndex()).toString());

							if (p.equals(null)) {
								statusMessageLabel.setText("Power not found!");
							}

							PowerName.setText(p.pwrname);

							PowerBase.setText(p.pwrbase);
							PowerLevel.setText("" + p.pwrlevel);
							PowerDescription.setText(p.pwrdesc);
							pwrCompleteModel.setSelectedItem(p.pwrcomplete);
							PowerLevelNameLbl.setText(p.pwrcat);
						}
					}

					return;
				}
			}
		);

		// status bar initialization - message timeout, idle icon and busy animation, etc
		ResourceMap resourceMap = getResourceMap();
		int messageTimeout = resourceMap.getInteger("StatusBar.messageTimeout");
		messageTimer = new Timer(messageTimeout, new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				statusMessageLabel.setText("");
			}
		});
		messageTimer.setRepeats(false);
		int busyAnimationRate = resourceMap.getInteger("StatusBar.busyAnimationRate");
		for (int i = 0; i < busyIcons.length; i++) {
			busyIcons[i] = resourceMap.getIcon("StatusBar.busyIcons[" + i + "]");
		}
		busyIconTimer = new Timer(busyAnimationRate, new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				busyIconIndex = (busyIconIndex + 1) % busyIcons.length;
				statusAnimationLabel.setIcon(busyIcons[busyIconIndex]);
			}
		});
		idleIcon = resourceMap.getIcon("StatusBar.idleIcon");
		statusAnimationLabel.setIcon(idleIcon);
		progressBar.setVisible(false);

		// connecting action tasks to status bar via TaskMonitor
		TaskMonitor taskMonitor = new TaskMonitor(getApplication().getContext());
		taskMonitor.addPropertyChangeListener(new java.beans.PropertyChangeListener() {

			public void propertyChange(java.beans.PropertyChangeEvent evt) {
				String propertyName = evt.getPropertyName();
				if ("started".equals(propertyName)) {
					if (!busyIconTimer.isRunning()) {
						statusAnimationLabel.setIcon(busyIcons[0]);
						busyIconIndex = 0;
						busyIconTimer.start();
					}
					progressBar.setVisible(true);
					progressBar.setIndeterminate(true);
				} else if ("done".equals(propertyName)) {
					busyIconTimer.stop();
					statusAnimationLabel.setIcon(idleIcon);
					progressBar.setVisible(false);
					progressBar.setValue(0);
				} else if ("message".equals(propertyName)) {
					String text = (String) (evt.getNewValue());
					statusMessageLabel.setText((text == null) ? "" : text);
					messageTimer.restart();
				} else if ("progress".equals(propertyName)) {
					int value = (Integer) (evt.getNewValue());
					progressBar.setVisible(true);
					progressBar.setIndeterminate(false);
					progressBar.setValue(value);
				}
			}
		});
	}

	@Action
	public void showAboutBox() {
		if (aboutBox == null) {
			JFrame mainFrame = DarkUniverseApp.getApplication().getMainFrame();
			aboutBox = new DarkUniverseAboutBox(mainFrame);
			aboutBox.setLocationRelativeTo(mainFrame);
		}
		DarkUniverseApp.getApplication().show(aboutBox);
	}

	@Action
	public void getCharacterName() {
		String s = (String) JOptionPane.showInputDialog(null, "Please enter the new username",
			"User Name", JOptionPane.PLAIN_MESSAGE, null, null, PlayerNameLbl.getText());
		PlayerNameLbl.setText(s);
		player.playername = s;
	}

	@Action
	public void showDiceFrame() {
		if (duDiceFrame == null) {
			JFrame mainFrame = DarkUniverseApp.getApplication().getMainFrame();
			duDiceFrame = new DiceFrame(mainFrame, false);
			duDiceFrame.setLocationRelativeTo(mainFrame);
		}
		DarkUniverseApp.getApplication().show(duDiceFrame);
		return;
	}

	@Action
	public void addKarma() {
		// Add karma
		int karma = Integer.parseInt(Karma.getText());
		karma = karma + Integer.parseInt(KarmaMod.getText());
		Karma.setText("" + karma);
		return;
	}

	@Action
	public void subKarma() {
		// subtract karma
		player.karma = Integer.parseInt(Karma.getText());
		player.karma = player.karma - Integer.parseInt(KarmaMod.getText());
		Karma.setText("" + player.karma);
		return;
	}

	@Action
	public void addRes() {
		// Add resources
		player.resources = Integer.parseInt(Resources.getText());
		player.resources = player.resources + Integer.parseInt(ResMod.getText());
		Resources.setText("" + player.resources);
		return;
	}

	@Action
	public void subRes() {
		// subtract resources
		player.resources = Integer.parseInt(Resources.getText());
		player.resources = player.resources - Integer.parseInt(ResMod.getText());
		Resources.setText("" + player.resources);
		return;
	}

	@Action
	public void saveCharacter() throws FileNotFoundException, IOException {
		if (WantXML.isSelected() == true) {
			this.saveCharacterXML();
		}
		if (WantXML.isSelected() == false) {
			this.saveCharacterSerial();
		}
	}

	@Action
	public void loadCharacter() throws FileNotFoundException, IOException, ClassNotFoundException {
		System.out.println("XML=" + WantXML.isSelected());
		if (WantXML.isSelected() == true) {
			this.loadCharacterXML();
		}
		if (WantXML.isSelected() == false) {
			this.loadCharacterSerial();
		}
	}

	@Action
	public void saveCharacterData() {
		// Load all values in the class.
		player.playername = this.PlayerNameLbl.getText();
		player.karma = Integer.parseInt(Karma.getText());
		player.resources = Integer.parseInt(Resources.getText());
		player.initiative = Integer.parseInt(Initiative.getText());
		player.playernotes = PlayerNotes.getText();
		player.health = Integer.parseInt(Health.getText());

		// FASERIP
		player.fighting = Integer.parseInt(Fighting.getText());
		player.agility = Integer.parseInt(Agility.getText());
		player.strength = Integer.parseInt(Strength.getText());
		player.endurance = Integer.parseInt(Endurance.getText());
		player.reason = Integer.parseInt(Reason.getText());
		player.intuition = Integer.parseInt(Intuition.getText());
		player.psycho = Integer.parseInt(Psycho.getText());

		// Powers
		player.Powers = Powers;

		return;
	}

	@Action
	public void saveCharacterSerial() throws FileNotFoundException, IOException {
		FileOutputStream f_out = null;
		ObjectOutputStream obj_out = null;
		File uFile = new File("user.data");
		uFile.delete();

		this.saveCharacterData();

		f_out = new FileOutputStream("user.data");
		obj_out = new ObjectOutputStream(f_out);
		obj_out.writeObject(player);

		statusMessageLabel.setText("Character saved.");
		return;
	}

	@Action
	public void saveCharacterXML() throws IOException {
		File uFile = new File("user.xml");

		uFile.delete();

		Easy.save(player, "user.xml");
		statusMessageLabel.setText("Character saved (XML).");
		return;
	}

	@Action
	public void loadCharacterData() {
		// We have the data, now update the fields. Update this as stuff gets added.
		if (player != null) {
			PlayerNameLbl.setText(player.playername);
			Karma.setText("" + player.karma);
			Resources.setText("" + player.resources);
			Initiative.setText("" + player.initiative);
			PlayerNotes.setText(player.playernotes);
			Health.setText("" + player.health);

			// Dump FASERIP
			Fighting.setText("" + player.fighting);
			Agility.setText("" + player.agility);
			Strength.setText("" + player.strength);
			Endurance.setText("" + player.endurance);
			Reason.setText("" + player.reason);
			Intuition.setText("" + player.intuition);
			Psycho.setText("" + player.psycho);

			Powers = player.Powers;
		}

		if (Powers.equals(null)) {
			Powers = new Vector();
		}

		// Populate the powerlist
		for (Object o: Powers) {
			int index = PowerList.getSelectedIndex();
			int size = pwrModel.getSize();

			Power p = new Power();

			p = (Power) o;

			// No selection or last position is selected, add new one and select
			if (index == -1 || (index + 1 == size)) {
				pwrModel.addElement(p.pwrname);
				PowerList.setSelectedIndex(size);
			} else {
				// Otherwise, insert it and select
				pwrModel.insertElementAt(p.pwrname, index + 1);
				PowerList.setSelectedIndex(index + 1);
				PowerList.ensureIndexIsVisible(index + 1);
			}
		}

		PowerList.setSelectedIndex(0);
		
		
		sort.sortListModel(pwrModel);

		return;
	}

	@Action
	public void loadCharacterSerial() throws FileNotFoundException, IOException, ClassNotFoundException {

		FileInputStream f_in = null;
		ObjectInputStream obj_in = null;
		Object o = null;

		f_in = new FileInputStream("user.data");
		obj_in = new ObjectInputStream(f_in);
		o = obj_in.readObject();
		if (o instanceof Player) {
			this.player = (Player) o;
		} else {
			statusMessageLabel.setText("Could not load character data.");
			return;
		}
		loadCharacterData();
		return;
	}

	@Action
	public void loadCharacterXML() throws FileNotFoundException {

		try {
			player = (Player) Easy.load("user.xml");
		} finally {
			loadCharacterData();
			return;
		}
	}

	@Action
	public void importXmlData() {
		JFileChooser fc = new JFileChooser();
		FileNameExtensionFilter ff = new FileNameExtensionFilter("User data", "xml");

		String userData;

		System.out.println("Trying to open file chooser.");

		fc.addChoosableFileFilter(ff);
		
		int res = fc.showOpenDialog(null);
		if (res == JFileChooser.APPROVE_OPTION) {
			userData = fc.getSelectedFile().getPath();
		} else {
			return;
		}

		try {
			Powers.clear(); // Reset this
		} catch (NullPointerException e) {
			Powers = new Vector(); // Init
		}
		
		try {
			player = (Player) Easy.load(userData);
		} finally {
			loadCharacterData();
			return;
		}
	}

	@Action
	public void pwrSaveBtnClick() {
		int index = PowerList.getSelectedIndex();
		int size;

		Power p = new Power();
		

		p.pwrname = PowerName.getText();
		p.pwrbase = PowerBase.getText();
		p.pwrlevel = Integer.parseInt(PowerLevel.getText());
		p.pwrcat = lvlToName(p.pwrlevel);
		p.pwrdesc = PowerDescription.getText();
		p.pwrcomplete = PowerCompleted.getModel().getSelectedItem().toString();

		if (powerExists(p.pwrname)) {

			// Find power, edit values.
			Power gp = new Power();

			for (Object o: Powers) {
				gp = (Power) o;
				if (gp.pwrname.equals(p.pwrname)) {
					delPower(p.pwrname);
					Powers.add(p);
				}
			}

			// Check if it is a base power, then update all child
			// powers that have it as a base.
			// TODO: Make it recurse when base powers have base powers.
			if (isBasePower(p.pwrname)) {
				for (Object o: Powers) {
					gp = (Power) o;
					if (gp.pwrbase.equals(p.pwrname)) { // Got one!
						System.out.println("Found child power: " + gp.pwrname);
						gp.pwrlevel = p.pwrlevel;
					}
				}
			}

		} else {
			// Add power to Vector, and add it to list.
			try {
				Powers.add(p);
			} catch (NullPointerException e) {
				Powers = new Vector();
				Powers.add(p);
			}

			size = pwrModel.getSize();

			// No selection or last position is selected, add new one and select
			if (index == -1 || (index + 1 == size)) {
				pwrModel.addElement(p.pwrname);
				PowerList.setSelectedIndex(size);
			} else {
				// Otherwise, insert it and select
				pwrModel.insertElementAt(p.pwrname, index + 1);
				PowerList.setSelectedIndex(index + 1);
				PowerList.ensureIndexIsVisible(index + 1);
				sort.sortListModel(pwrModel);
			}
		}
		return;
	}

	@Action
	public void pwrDeleteBtnClick() {

		String pwrname = pwrModel.get(PowerList.getSelectedIndex()).toString();
		ListSelectionModel lsm = PowerList.getSelectionModel();
		int first = lsm.getMinSelectionIndex();
		int last = lsm.getMaxSelectionIndex();
		pwrModel.removeRange(first, last);

		int size = pwrModel.size();

		if (size == 0) { // List is empty
			PowerDeleteBtn.setEnabled(false);
			return; // No further action.
		} else {

			// Last item was deleted
			if (first == pwrModel.getSize()) {
				first--;
			}
			PowerList.setSelectedIndex(first);
			sort.sortListModel(pwrModel); 

		}

		// Delete power from powers list
		delPower(pwrname);

		return;
	}

	public Power fetchPower(String pwrname) {

		Power p = new Power();

		for (Object q: Powers) {
			p = (Power) q;
			if (p.pwrname.equals(pwrname)) {
				return p;
			}
		}
		return null;
	}

	public Boolean isBasePower(String pwrname) {
		
		Power p = new Power();

		for (Object o: Powers) {
			p = (Power) o;
			if (p.pwrname.equals(pwrname)) {
				if (p.pwrbase.equals("None") || p.pwrbase.isEmpty())
					return true;
			}
		}
		return false;
	}

	public void delPower(String pwrname) {
		Power p = new Power();
		int index = 0;

		for (Object q: Powers) {
			p = (Power) q;
			if (p.pwrname.equals(pwrname)) {
				Powers.remove(index);
				return;
			}
			index++;
 		}
	}

	public Boolean powerExists(String pwrname) {

		Power p = new Power();

		// Check if it's empty, if the object was never instantiated, 
		// then construct.
		try {
			if (Powers.isEmpty() == true) {
				return false;
			}
		} catch (NullPointerException e) {
			Powers = new Vector();
			return false;
		}


		for (Object q: Powers) {
			p = (Power) q;
			if (p.pwrname.equals(pwrname)) {
				return true;
			}
		}
		return false;
	}

	public String lvlToName(Integer power) {

		if (power == 0) {
			return "Sh";
		}
		if (power >= 351) {
			return "ShZ";
		}

		if (power >= 176) {
			return "ShY";
		}

		if (power >= 126) {
			return "ShX";
		}

		if (power >= 88) {
			return "Un";
		}

		if (power >= 63) {
			return "Mn";
		}

		if (power >= 46) {
			return "Am";
		}

		if (power >= 37) {
			return "In";
		}

		if (power >= 26) {
			return "Rm";
		}

		if (power >= 16) {
			return "Ex";
		}

		if (power >= 8) {
			return "Gd";
		}

		if (power >= 5) {
			return "Ty";
		}

		if (power >= 3) {
			return "Pr";
		}

		if (power >= 1) {
			return "Fe";
		}

		return "Unk";
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
	@SuppressWarnings("unchecked")
        // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
        private void initComponents() {

                mainPanel = new javax.swing.JPanel();
                NameChgBtn = new javax.swing.JButton();
                PlayerNameLbl = new javax.swing.JLabel();
                DiceShowBtn = new javax.swing.JButton();
                TabbedPane = new javax.swing.JTabbedPane();
                jPanel1 = new javax.swing.JPanel();
                jPanel4 = new javax.swing.JPanel();
                Fighting = new javax.swing.JTextField();
                jLabel3 = new javax.swing.JLabel();
                jLabel9 = new javax.swing.JLabel();
                jLabel5 = new javax.swing.JLabel();
                Reason = new javax.swing.JTextField();
                jLabel7 = new javax.swing.JLabel();
                jLabel6 = new javax.swing.JLabel();
                Psycho = new javax.swing.JTextField();
                Agility = new javax.swing.JTextField();
                Intuition = new javax.swing.JTextField();
                Endurance = new javax.swing.JTextField();
                jLabel8 = new javax.swing.JLabel();
                Strength = new javax.swing.JTextField();
                jLabel4 = new javax.swing.JLabel();
                jLabel10 = new javax.swing.JLabel();
                Initiative = new javax.swing.JTextField();
                jLabel16 = new javax.swing.JLabel();
                Health = new javax.swing.JTextField();
                FCatLbl = new javax.swing.JLabel();
                ACatLbl = new javax.swing.JLabel();
                SCatLbl = new javax.swing.JLabel();
                ECatLbl = new javax.swing.JLabel();
                RCatLbl = new javax.swing.JLabel();
                ICatLbl = new javax.swing.JLabel();
                PCatLbl = new javax.swing.JLabel();
                jPanel5 = new javax.swing.JPanel();
                jPanel6 = new javax.swing.JPanel();
                jScrollPane2 = new javax.swing.JScrollPane();
                PowerList = new javax.swing.JList();
                jLabel13 = new javax.swing.JLabel();
                jScrollPane3 = new javax.swing.JScrollPane();
                PowerDescription = new javax.swing.JTextArea();
                jLabel14 = new javax.swing.JLabel();
                PowerBase = new javax.swing.JTextField();
                PwrNameLbl = new javax.swing.JLabel();
                PowerCompleted = new javax.swing.JComboBox();
                PowerSaveBtn = new javax.swing.JButton();
                jLabel12 = new javax.swing.JLabel();
                PowerName = new javax.swing.JTextField();
                PowerLevelNameLbl = new javax.swing.JLabel();
                PowerLevel = new javax.swing.JTextField();
                jLabel15 = new javax.swing.JLabel();
                PowerDeleteBtn = new javax.swing.JButton();
                jPanel2 = new javax.swing.JPanel();
                jPanel3 = new javax.swing.JPanel();
                Karma = new javax.swing.JTextField();
                jLabel1 = new javax.swing.JLabel();
                jLabel2 = new javax.swing.JLabel();
                KarmaMod = new javax.swing.JTextField();
                ResAddBtn = new javax.swing.JButton();
                KarmaAddBtn = new javax.swing.JButton();
                Resources = new javax.swing.JTextField();
                ResMod = new javax.swing.JTextField();
                jScrollPane1 = new javax.swing.JScrollPane();
                PlayerNotes = new javax.swing.JEditorPane();
                CharSaveBtn = new javax.swing.JButton();
                PlayerNameLbl1 = new javax.swing.JLabel();
                WantXML = new javax.swing.JCheckBox();
                menuBar = new javax.swing.JMenuBar();
                javax.swing.JMenu fileMenu = new javax.swing.JMenu();
                jMenuItem1 = new javax.swing.JMenuItem();
                jMenuItem2 = new javax.swing.JMenuItem();
                javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
                javax.swing.JMenu helpMenu = new javax.swing.JMenu();
                javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();
                statusPanel = new javax.swing.JPanel();
                javax.swing.JSeparator statusPanelSeparator = new javax.swing.JSeparator();
                statusMessageLabel = new javax.swing.JLabel();
                statusAnimationLabel = new javax.swing.JLabel();
                progressBar = new javax.swing.JProgressBar();

                mainPanel.setName("mainPanel"); // NOI18N
                mainPanel.setOpaque(false);

                javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(darkuniverseapp.DarkUniverseApp.class).getContext().getActionMap(DarkUniverseView.class, this);
                NameChgBtn.setAction(actionMap.get("getCharacterName")); // NOI18N
                org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(darkuniverseapp.DarkUniverseApp.class).getContext().getResourceMap(DarkUniverseView.class);
                NameChgBtn.setText(resourceMap.getString("NameChgBtn.text")); // NOI18N
                NameChgBtn.setToolTipText(resourceMap.getString("NameChgBtn.toolTipText")); // NOI18N
                NameChgBtn.setName("NameChgBtn"); // NOI18N

                PlayerNameLbl.setFont(PlayerNameLbl.getFont().deriveFont(PlayerNameLbl.getFont().getSize()+4f));
                PlayerNameLbl.setText(resourceMap.getString("PlayerNameLbl.text")); // NOI18N
                PlayerNameLbl.setName("PlayerNameLbl"); // NOI18N

                DiceShowBtn.setAction(actionMap.get("showDiceFrame")); // NOI18N
                DiceShowBtn.setText(resourceMap.getString("DiceShowBtn.text")); // NOI18N
                DiceShowBtn.setName("DiceShowBtn"); // NOI18N

                TabbedPane.setName("TabbedPane"); // NOI18N

                jPanel1.setName("jPanel1"); // NOI18N

                jPanel4.setName("jPanel4"); // NOI18N

                Fighting.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
                Fighting.setText(resourceMap.getString("Fighting.text")); // NOI18N
                Fighting.setName("Fighting"); // NOI18N
                Fighting.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                FightingActionPerformed(evt);
                        }
                });
                Fighting.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
                        public void propertyChange(java.beans.PropertyChangeEvent evt) {
                                FightingPropertyChange(evt);
                        }
                });

                jLabel3.setText(resourceMap.getString("jLabel3.text")); // NOI18N
                jLabel3.setName("jLabel3"); // NOI18N

                jLabel9.setText(resourceMap.getString("jLabel9.text")); // NOI18N
                jLabel9.setName("jLabel9"); // NOI18N

                jLabel5.setText(resourceMap.getString("jLabel5.text")); // NOI18N
                jLabel5.setName("jLabel5"); // NOI18N

                Reason.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
                Reason.setText(resourceMap.getString("Reason.text")); // NOI18N
                Reason.setName("Reason"); // NOI18N
                Reason.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                ReasonActionPerformed(evt);
                        }
                });
                Reason.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
                        public void propertyChange(java.beans.PropertyChangeEvent evt) {
                                ReasonPropertyChange(evt);
                        }
                });

                jLabel7.setText(resourceMap.getString("jLabel7.text")); // NOI18N
                jLabel7.setName("jLabel7"); // NOI18N

                jLabel6.setText(resourceMap.getString("jLabel6.text")); // NOI18N
                jLabel6.setName("jLabel6"); // NOI18N

                Psycho.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
                Psycho.setText(resourceMap.getString("Psycho.text")); // NOI18N
                Psycho.setName("Psycho"); // NOI18N
                Psycho.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                PsychoActionPerformed(evt);
                        }
                });
                Psycho.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
                        public void propertyChange(java.beans.PropertyChangeEvent evt) {
                                PsychoPropertyChange(evt);
                        }
                });

                Agility.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
                Agility.setText(resourceMap.getString("Agility.text")); // NOI18N
                Agility.setName("Agility"); // NOI18N
                Agility.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                AgilityActionPerformed(evt);
                        }
                });
                Agility.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
                        public void propertyChange(java.beans.PropertyChangeEvent evt) {
                                AgilityPropertyChange(evt);
                        }
                });

                Intuition.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
                Intuition.setText(resourceMap.getString("Intuition.text")); // NOI18N
                Intuition.setName("Intuition"); // NOI18N
                Intuition.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                IntuitionActionPerformed(evt);
                        }
                });
                Intuition.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
                        public void propertyChange(java.beans.PropertyChangeEvent evt) {
                                IntuitionPropertyChange(evt);
                        }
                });

                Endurance.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
                Endurance.setText(resourceMap.getString("Endurance.text")); // NOI18N
                Endurance.setName("Endurance"); // NOI18N
                Endurance.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                EnduranceActionPerformed(evt);
                        }
                });
                Endurance.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
                        public void propertyChange(java.beans.PropertyChangeEvent evt) {
                                EndurancePropertyChange(evt);
                        }
                });

                jLabel8.setText(resourceMap.getString("jLabel8.text")); // NOI18N
                jLabel8.setName("jLabel8"); // NOI18N

                Strength.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
                Strength.setText(resourceMap.getString("Strength.text")); // NOI18N
                Strength.setName("Strength"); // NOI18N
                Strength.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                StrengthActionPerformed(evt);
                        }
                });
                Strength.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
                        public void propertyChange(java.beans.PropertyChangeEvent evt) {
                                StrengthPropertyChange(evt);
                        }
                });

                jLabel4.setText(resourceMap.getString("jLabel4.text")); // NOI18N
                jLabel4.setName("jLabel4"); // NOI18N

                jLabel10.setText(resourceMap.getString("jLabel10.text")); // NOI18N
                jLabel10.setName("jLabel10"); // NOI18N

                Initiative.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
                Initiative.setText(resourceMap.getString("Initiative.text")); // NOI18N
                Initiative.setName("Initiative"); // NOI18N

                jLabel16.setText(resourceMap.getString("jLabel16.text")); // NOI18N
                jLabel16.setName("jLabel16"); // NOI18N

                Health.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
                Health.setText(resourceMap.getString("Health.text")); // NOI18N
                Health.setName("Health"); // NOI18N

                FCatLbl.setText(resourceMap.getString("FCatLbl.text")); // NOI18N
                FCatLbl.setName("FCatLbl"); // NOI18N

                ACatLbl.setText(resourceMap.getString("ACatLbl.text")); // NOI18N
                ACatLbl.setName("ACatLbl"); // NOI18N

                SCatLbl.setText(resourceMap.getString("SCatLbl.text")); // NOI18N
                SCatLbl.setName("SCatLbl"); // NOI18N

                ECatLbl.setText(resourceMap.getString("ECatLbl.text")); // NOI18N
                ECatLbl.setName("ECatLbl"); // NOI18N

                RCatLbl.setText(resourceMap.getString("RCatLbl.text")); // NOI18N
                RCatLbl.setName("RCatLbl"); // NOI18N

                ICatLbl.setText(resourceMap.getString("ICatLbl.text")); // NOI18N
                ICatLbl.setName("ICatLbl"); // NOI18N

                PCatLbl.setText(resourceMap.getString("PCatLbl.text")); // NOI18N
                PCatLbl.setName("PCatLbl"); // NOI18N

                javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
                jPanel4.setLayout(jPanel4Layout);
                jPanel4Layout.setHorizontalGroup(
                        jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel4Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jLabel5)
                                        .addComponent(jLabel6)
                                        .addComponent(jLabel4)
                                        .addComponent(jLabel3)
                                        .addComponent(jLabel7)
                                        .addComponent(jLabel8)
                                        .addComponent(jLabel9))
                                .addGap(21, 21, 21)
                                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addComponent(FCatLbl, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(ACatLbl, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(SCatLbl, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(ECatLbl, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(RCatLbl, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(ICatLbl, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(PCatLbl, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(Psycho)
                                        .addComponent(Intuition)
                                        .addComponent(Reason)
                                        .addComponent(Endurance)
                                        .addComponent(Strength)
                                        .addComponent(Agility)
                                        .addComponent(Fighting, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(32, 32, 32)
                                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jLabel10)
                                        .addComponent(jLabel16))
                                .addGap(13, 13, 13)
                                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(Initiative)
                                        .addComponent(Health, javax.swing.GroupLayout.DEFAULT_SIZE, 49, Short.MAX_VALUE))
                                .addContainerGap(40, Short.MAX_VALUE))
                );
                jPanel4Layout.setVerticalGroup(
                        jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel4Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel3)
                                        .addComponent(Fighting, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel16)
                                        .addComponent(Health, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(FCatLbl))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel4)
                                        .addComponent(Agility, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel10)
                                        .addComponent(Initiative, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(ACatLbl))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel5)
                                        .addComponent(Strength, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(SCatLbl))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel6)
                                        .addComponent(Endurance, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(ECatLbl))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel7)
                                        .addComponent(Reason, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(RCatLbl))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel8)
                                        .addComponent(Intuition, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(ICatLbl))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel9)
                                        .addComponent(Psycho, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(PCatLbl))
                                .addContainerGap())
                );

                javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
                jPanel1.setLayout(jPanel1Layout);
                jPanel1Layout.setHorizontalGroup(
                        jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(307, Short.MAX_VALUE))
                );
                jPanel1Layout.setVerticalGroup(
                        jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, 268, Short.MAX_VALUE)
                                .addGap(127, 127, 127))
                );

                TabbedPane.addTab(resourceMap.getString("jPanel1.TabConstraints.tabTitle"), jPanel1); // NOI18N

                jPanel5.setName("jPanel5"); // NOI18N

                jPanel6.setName("jPanel6"); // NOI18N

                jScrollPane2.setName("jScrollPane2"); // NOI18N

                PowerList.setToolTipText(resourceMap.getString("PowerList.toolTipText")); // NOI18N
                PowerList.setName("PowerList"); // NOI18N
                jScrollPane2.setViewportView(PowerList);

                jLabel13.setText(resourceMap.getString("jLabel13.text")); // NOI18N
                jLabel13.setName("jLabel13"); // NOI18N

                jScrollPane3.setName("jScrollPane3"); // NOI18N

                PowerDescription.setColumns(20);
                PowerDescription.setLineWrap(true);
                PowerDescription.setRows(5);
                PowerDescription.setWrapStyleWord(true);
                PowerDescription.setName("PowerDescription"); // NOI18N
                jScrollPane3.setViewportView(PowerDescription);

                jLabel14.setText(resourceMap.getString("jLabel14.text")); // NOI18N
                jLabel14.setName("jLabel14"); // NOI18N

                PowerBase.setText(resourceMap.getString("PowerBase.text")); // NOI18N
                PowerBase.setName("PowerBase"); // NOI18N

                PwrNameLbl.setText(resourceMap.getString("PwrNameLbl.text")); // NOI18N
                PwrNameLbl.setName("PwrNameLbl"); // NOI18N

                PowerCompleted.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "1/3", "2/3", "3/3", "Completed" }));
                PowerCompleted.setName("PowerCompleted"); // NOI18N

                PowerSaveBtn.setAction(actionMap.get("pwrSaveBtnClick")); // NOI18N
                PowerSaveBtn.setText(resourceMap.getString("PowerSaveBtn.text")); // NOI18N
                PowerSaveBtn.setName("PowerSaveBtn"); // NOI18N

                jLabel12.setText(resourceMap.getString("jLabel12.text")); // NOI18N
                jLabel12.setName("jLabel12"); // NOI18N

                PowerName.setText(resourceMap.getString("PowerName.text")); // NOI18N
                PowerName.setName("PowerName"); // NOI18N

                PowerLevelNameLbl.setText(resourceMap.getString("PowerLevelNameLbl.text")); // NOI18N
                PowerLevelNameLbl.setName("PowerLevelNameLbl"); // NOI18N

                PowerLevel.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
                PowerLevel.setText(resourceMap.getString("PowerLevel.text")); // NOI18N
                PowerLevel.setName("PowerLevel"); // NOI18N
                PowerLevel.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                PowerLevelActionPerformed(evt);
                        }
                });
                PowerLevel.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
                        public void propertyChange(java.beans.PropertyChangeEvent evt) {
                                PowerLevelPropertyChange(evt);
                        }
                });

                jLabel15.setText(resourceMap.getString("jLabel15.text")); // NOI18N
                jLabel15.setName("jLabel15"); // NOI18N

                PowerDeleteBtn.setAction(actionMap.get("pwrDeleteBtnClick")); // NOI18N
                PowerDeleteBtn.setText(resourceMap.getString("PowerDeleteBtn.text")); // NOI18N
                PowerDeleteBtn.setName("PowerDeleteBtn"); // NOI18N

                javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
                jPanel6.setLayout(jPanel6Layout);
                jPanel6Layout.setHorizontalGroup(
                        jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel6Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 266, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 349, Short.MAX_VALUE)
                                        .addComponent(jLabel15)
                                        .addGroup(jPanel6Layout.createSequentialGroup()
                                                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(jLabel13)
                                                        .addComponent(PwrNameLbl)
                                                        .addComponent(jLabel12)
                                                        .addComponent(jLabel14))
                                                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addGroup(jPanel6Layout.createSequentialGroup()
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                        .addComponent(PowerCompleted, 0, 262, Short.MAX_VALUE)
                                                                        .addComponent(PowerBase, javax.swing.GroupLayout.DEFAULT_SIZE, 262, Short.MAX_VALUE)
                                                                        .addComponent(PowerName, javax.swing.GroupLayout.DEFAULT_SIZE, 262, Short.MAX_VALUE)))
                                                        .addGroup(jPanel6Layout.createSequentialGroup()
                                                                .addGap(22, 22, 22)
                                                                .addComponent(PowerLevelNameLbl, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(PowerLevel, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                .addComponent(PowerSaveBtn)
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(PowerDeleteBtn)))))
                                .addContainerGap())
                );
                jPanel6Layout.setVerticalGroup(
                        jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel6Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(jPanel6Layout.createSequentialGroup()
                                                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                        .addComponent(PwrNameLbl)
                                                        .addComponent(PowerName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                        .addComponent(jLabel12)
                                                        .addComponent(PowerBase, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                        .addComponent(jLabel13)
                                                        .addComponent(PowerSaveBtn)
                                                        .addComponent(PowerLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(PowerLevelNameLbl)
                                                        .addComponent(PowerDeleteBtn))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                        .addComponent(jLabel14)
                                                        .addComponent(PowerCompleted, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jLabel15)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 144, Short.MAX_VALUE))
                                        .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 302, Short.MAX_VALUE))
                                .addContainerGap())
                );

                javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
                jPanel5.setLayout(jPanel5Layout);
                jPanel5Layout.setHorizontalGroup(
                        jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel5Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(56, 56, 56))
                );
                jPanel5Layout.setVerticalGroup(
                        jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel5Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(74, 74, 74))
                );

                TabbedPane.addTab(resourceMap.getString("jPanel5.TabConstraints.tabTitle"), jPanel5); // NOI18N

                jPanel2.setName("jPanel2"); // NOI18N

                jPanel3.setName("jPanel3"); // NOI18N

                Karma.setFont(Karma.getFont());
                Karma.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
                Karma.setText(resourceMap.getString("Karma.text")); // NOI18N
                Karma.setName("Karma"); // NOI18N

                jLabel1.setFont(jLabel1.getFont());
                jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
                jLabel1.setAlignmentX(0.5F);
                jLabel1.setName("jLabel1"); // NOI18N

                jLabel2.setFont(jLabel2.getFont());
                jLabel2.setText(resourceMap.getString("jLabel2.text")); // NOI18N
                jLabel2.setAlignmentX(0.5F);
                jLabel2.setName("jLabel2"); // NOI18N

                KarmaMod.setFont(KarmaMod.getFont());
                KarmaMod.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
                KarmaMod.setText(resourceMap.getString("KarmaMod.text")); // NOI18N
                KarmaMod.setName("KarmaMod"); // NOI18N

                ResAddBtn.setAction(actionMap.get("addRes")); // NOI18N
                ResAddBtn.setFont(ResAddBtn.getFont());
                ResAddBtn.setText(resourceMap.getString("ResAddBtn.text")); // NOI18N
                ResAddBtn.setAlignmentX(0.5F);
                ResAddBtn.setName("ResAddBtn"); // NOI18N

                KarmaAddBtn.setAction(actionMap.get("addKarma")); // NOI18N
                KarmaAddBtn.setFont(KarmaAddBtn.getFont());
                KarmaAddBtn.setText(resourceMap.getString("KarmaAddBtn.text")); // NOI18N
                KarmaAddBtn.setAlignmentX(0.5F);
                KarmaAddBtn.setName("KarmaAddBtn"); // NOI18N

                Resources.setFont(Resources.getFont());
                Resources.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
                Resources.setText(resourceMap.getString("Resources.text")); // NOI18N
                Resources.setName("Resources"); // NOI18N

                ResMod.setFont(ResMod.getFont());
                ResMod.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
                ResMod.setText(resourceMap.getString("ResMod.text")); // NOI18N
                ResMod.setName("ResMod"); // NOI18N

                javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
                jPanel3.setLayout(jPanel3Layout);
                jPanel3Layout.setHorizontalGroup(
                        jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel3Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addGroup(jPanel3Layout.createSequentialGroup()
                                                .addComponent(jLabel1)
                                                .addGap(42, 42, 42)
                                                .addComponent(Karma, javax.swing.GroupLayout.PREFERRED_SIZE, 127, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGroup(jPanel3Layout.createSequentialGroup()
                                                .addComponent(jLabel2)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(Resources, javax.swing.GroupLayout.PREFERRED_SIZE, 127, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(ResMod)
                                        .addComponent(KarmaMod, javax.swing.GroupLayout.DEFAULT_SIZE, 66, Short.MAX_VALUE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(ResAddBtn, javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addComponent(KarmaAddBtn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGap(67, 67, 67))
                );
                jPanel3Layout.setVerticalGroup(
                        jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel3Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel1)
                                        .addComponent(Karma, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(KarmaMod, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(KarmaAddBtn))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel2)
                                        .addComponent(ResAddBtn)
                                        .addComponent(Resources, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(ResMod, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addContainerGap())
                );

                javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
                jPanel2.setLayout(jPanel2Layout);
                jPanel2Layout.setHorizontalGroup(
                        jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(255, Short.MAX_VALUE))
                );
                jPanel2Layout.setVerticalGroup(
                        jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(288, Short.MAX_VALUE))
                );

                TabbedPane.addTab(resourceMap.getString("jPanel2.TabConstraints.tabTitle"), jPanel2); // NOI18N

                jScrollPane1.setName("jScrollPane1"); // NOI18N

                PlayerNotes.setName("PlayerNotes"); // NOI18N
                jScrollPane1.setViewportView(PlayerNotes);

                TabbedPane.addTab(resourceMap.getString("jScrollPane1.TabConstraints.tabTitle"), jScrollPane1); // NOI18N

                TabbedPane.setSelectedComponent(jPanel1);

                CharSaveBtn.setAction(actionMap.get("saveCharacter")); // NOI18N
                CharSaveBtn.setText(resourceMap.getString("CharSaveBtn.text")); // NOI18N
                CharSaveBtn.setName("CharSaveBtn"); // NOI18N

                PlayerNameLbl1.setFont(PlayerNameLbl1.getFont().deriveFont(PlayerNameLbl1.getFont().getSize()+4f));
                PlayerNameLbl1.setText(resourceMap.getString("PlayerNameLbl1.text")); // NOI18N
                PlayerNameLbl1.setName("PlayerNameLbl1"); // NOI18N

                WantXML.setText(resourceMap.getString("WantXML.text")); // NOI18N
                WantXML.setName("WantXML"); // NOI18N

                javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
                mainPanel.setLayout(mainPanelLayout);
                mainPanelLayout.setHorizontalGroup(
                        mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(mainPanelLayout.createSequentialGroup()
                                .addGap(24, 24, 24)
                                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(TabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 690, Short.MAX_VALUE)
                                        .addGroup(mainPanelLayout.createSequentialGroup()
                                                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                        .addGroup(mainPanelLayout.createSequentialGroup()
                                                                .addComponent(PlayerNameLbl1)
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(PlayerNameLbl, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                                        .addGroup(mainPanelLayout.createSequentialGroup()
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(NameChgBtn)
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(DiceShowBtn)
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(CharSaveBtn)))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(WantXML)))
                                .addContainerGap())
                );
                mainPanelLayout.setVerticalGroup(
                        mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(mainPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(PlayerNameLbl, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(PlayerNameLbl1, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(NameChgBtn)
                                        .addComponent(DiceShowBtn)
                                        .addComponent(CharSaveBtn)
                                        .addComponent(WantXML))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(TabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 416, Short.MAX_VALUE))
                );

                menuBar.setName("menuBar"); // NOI18N

                fileMenu.setText(resourceMap.getString("fileMenu.text")); // NOI18N
                fileMenu.setName("fileMenu"); // NOI18N
                fileMenu.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                fileMenuActionPerformed(evt);
                        }
                });

                jMenuItem1.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
                jMenuItem1.setText(resourceMap.getString("jMenuItem1.text")); // NOI18N
                jMenuItem1.setName("jMenuItem1"); // NOI18N
                fileMenu.add(jMenuItem1);

                jMenuItem2.setAction(actionMap.get("importXmlData")); // NOI18N
                jMenuItem2.setText(resourceMap.getString("jMenuItem2.text")); // NOI18N
                jMenuItem2.setName("jMenuItem2"); // NOI18N
                fileMenu.add(jMenuItem2);

                exitMenuItem.setAction(actionMap.get("quit")); // NOI18N
                exitMenuItem.setName("exitMenuItem"); // NOI18N
                fileMenu.add(exitMenuItem);

                menuBar.add(fileMenu);

                helpMenu.setText(resourceMap.getString("helpMenu.text")); // NOI18N
                helpMenu.setName("helpMenu"); // NOI18N

                aboutMenuItem.setAction(actionMap.get("showAboutBox")); // NOI18N
                aboutMenuItem.setName("aboutMenuItem"); // NOI18N
                helpMenu.add(aboutMenuItem);

                menuBar.add(helpMenu);

                statusPanel.setName("statusPanel"); // NOI18N

                statusPanelSeparator.setName("statusPanelSeparator"); // NOI18N

                statusMessageLabel.setName("statusMessageLabel"); // NOI18N

                statusAnimationLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
                statusAnimationLabel.setName("statusAnimationLabel"); // NOI18N

                progressBar.setName("progressBar"); // NOI18N

                javax.swing.GroupLayout statusPanelLayout = new javax.swing.GroupLayout(statusPanel);
                statusPanel.setLayout(statusPanelLayout);
                statusPanelLayout.setHorizontalGroup(
                        statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(statusPanelSeparator, javax.swing.GroupLayout.DEFAULT_SIZE, 714, Short.MAX_VALUE)
                        .addGroup(statusPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(statusMessageLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 528, Short.MAX_VALUE)
                                .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(statusAnimationLabel)
                                .addContainerGap())
                );
                statusPanelLayout.setVerticalGroup(
                        statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(statusPanelLayout.createSequentialGroup()
                                .addComponent(statusPanelSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(statusMessageLabel)
                                        .addComponent(statusAnimationLabel)
                                        .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(3, 3, 3))
                );

                setComponent(mainPanel);
                setMenuBar(menuBar);
                setStatusBar(statusPanel);
        }// </editor-fold>//GEN-END:initComponents

	private void FightingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_FightingActionPerformed
		FCatLbl.setText(lvlToName(Integer.parseInt(Fighting.getText())));
	}//GEN-LAST:event_FightingActionPerformed

	private void FightingPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_FightingPropertyChange
		FCatLbl.setText(lvlToName(Integer.parseInt(Fighting.getText())));
	}//GEN-LAST:event_FightingPropertyChange

	private void AgilityActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_AgilityActionPerformed
		ACatLbl.setText(lvlToName(Integer.parseInt(Agility.getText())));
	}//GEN-LAST:event_AgilityActionPerformed

	private void StrengthActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_StrengthActionPerformed
		SCatLbl.setText(lvlToName(Integer.parseInt(Strength.getText())));
	}//GEN-LAST:event_StrengthActionPerformed

	private void EnduranceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_EnduranceActionPerformed
		ECatLbl.setText(lvlToName(Integer.parseInt(Endurance.getText())));
	}//GEN-LAST:event_EnduranceActionPerformed

	private void ReasonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ReasonActionPerformed
		RCatLbl.setText(lvlToName(Integer.parseInt(Reason.getText())));
	}//GEN-LAST:event_ReasonActionPerformed

	private void IntuitionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_IntuitionActionPerformed
		ICatLbl.setText(lvlToName(Integer.parseInt(Intuition.getText())));
	}//GEN-LAST:event_IntuitionActionPerformed

	private void PsychoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_PsychoActionPerformed
		PCatLbl.setText(lvlToName(Integer.parseInt(Psycho.getText())));
	}//GEN-LAST:event_PsychoActionPerformed

	private void AgilityPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_AgilityPropertyChange
		ACatLbl.setText(lvlToName(Integer.parseInt(Agility.getText())));
	}//GEN-LAST:event_AgilityPropertyChange

	private void StrengthPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_StrengthPropertyChange
		SCatLbl.setText(lvlToName(Integer.parseInt(Strength.getText())));
	}//GEN-LAST:event_StrengthPropertyChange

	private void EndurancePropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_EndurancePropertyChange
		ECatLbl.setText(lvlToName(Integer.parseInt(Endurance.getText())));
	}//GEN-LAST:event_EndurancePropertyChange

	private void ReasonPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_ReasonPropertyChange
		RCatLbl.setText(lvlToName(Integer.parseInt(Reason.getText())));
	}//GEN-LAST:event_ReasonPropertyChange

	private void IntuitionPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_IntuitionPropertyChange
		ICatLbl.setText(lvlToName(Integer.parseInt(Intuition.getText())));
	}//GEN-LAST:event_IntuitionPropertyChange

	private void PsychoPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_PsychoPropertyChange
		PCatLbl.setText(lvlToName(Integer.parseInt(Psycho.getText())));
	}//GEN-LAST:event_PsychoPropertyChange

	private void fileMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fileMenuActionPerformed
		// Nothing
	}//GEN-LAST:event_fileMenuActionPerformed

	private void PowerLevelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_PowerLevelActionPerformed
		PowerLevelNameLbl.setText(lvlToName(Integer.parseInt(PowerLevel.getText())));
	}//GEN-LAST:event_PowerLevelActionPerformed

	private void PowerLevelPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_PowerLevelPropertyChange
		PowerLevelNameLbl.setText(lvlToName(Integer.parseInt(PowerLevel.getText())));
	}//GEN-LAST:event_PowerLevelPropertyChange

        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JLabel ACatLbl;
        private javax.swing.JTextField Agility;
        private javax.swing.JButton CharSaveBtn;
        private javax.swing.JButton DiceShowBtn;
        private javax.swing.JLabel ECatLbl;
        private javax.swing.JTextField Endurance;
        private javax.swing.JLabel FCatLbl;
        private javax.swing.JTextField Fighting;
        private javax.swing.JTextField Health;
        private javax.swing.JLabel ICatLbl;
        private javax.swing.JTextField Initiative;
        private javax.swing.JTextField Intuition;
        private javax.swing.JTextField Karma;
        private javax.swing.JButton KarmaAddBtn;
        private javax.swing.JTextField KarmaMod;
        private javax.swing.JButton NameChgBtn;
        private javax.swing.JLabel PCatLbl;
        private javax.swing.JLabel PlayerNameLbl;
        private javax.swing.JLabel PlayerNameLbl1;
        private javax.swing.JEditorPane PlayerNotes;
        private javax.swing.JTextField PowerBase;
        private javax.swing.JComboBox PowerCompleted;
        private javax.swing.JButton PowerDeleteBtn;
        private javax.swing.JTextArea PowerDescription;
        private javax.swing.JTextField PowerLevel;
        private javax.swing.JLabel PowerLevelNameLbl;
        private javax.swing.JList PowerList;
        private javax.swing.JTextField PowerName;
        private javax.swing.JButton PowerSaveBtn;
        private javax.swing.JTextField Psycho;
        private javax.swing.JLabel PwrNameLbl;
        private javax.swing.JLabel RCatLbl;
        private javax.swing.JTextField Reason;
        private javax.swing.JButton ResAddBtn;
        private javax.swing.JTextField ResMod;
        private javax.swing.JTextField Resources;
        private javax.swing.JLabel SCatLbl;
        private javax.swing.JTextField Strength;
        private javax.swing.JTabbedPane TabbedPane;
        private javax.swing.JCheckBox WantXML;
        private javax.swing.JLabel jLabel1;
        private javax.swing.JLabel jLabel10;
        private javax.swing.JLabel jLabel12;
        private javax.swing.JLabel jLabel13;
        private javax.swing.JLabel jLabel14;
        private javax.swing.JLabel jLabel15;
        private javax.swing.JLabel jLabel16;
        private javax.swing.JLabel jLabel2;
        private javax.swing.JLabel jLabel3;
        private javax.swing.JLabel jLabel4;
        private javax.swing.JLabel jLabel5;
        private javax.swing.JLabel jLabel6;
        private javax.swing.JLabel jLabel7;
        private javax.swing.JLabel jLabel8;
        private javax.swing.JLabel jLabel9;
        private javax.swing.JMenuItem jMenuItem1;
        private javax.swing.JMenuItem jMenuItem2;
        private javax.swing.JPanel jPanel1;
        private javax.swing.JPanel jPanel2;
        private javax.swing.JPanel jPanel3;
        private javax.swing.JPanel jPanel4;
        private javax.swing.JPanel jPanel5;
        private javax.swing.JPanel jPanel6;
        private javax.swing.JScrollPane jScrollPane1;
        private javax.swing.JScrollPane jScrollPane2;
        private javax.swing.JScrollPane jScrollPane3;
        private javax.swing.JPanel mainPanel;
        private javax.swing.JMenuBar menuBar;
        private javax.swing.JProgressBar progressBar;
        private javax.swing.JLabel statusAnimationLabel;
        private javax.swing.JLabel statusMessageLabel;
        private javax.swing.JPanel statusPanel;
        // End of variables declaration//GEN-END:variables
	private final Timer messageTimer;
	private final Timer busyIconTimer;
	private final Icon idleIcon;
	private final Icon[] busyIcons = new Icon[15];
	private int busyIconIndex = 0;
	private JDialog aboutBox;
	private JDialog duDiceFrame;
}
