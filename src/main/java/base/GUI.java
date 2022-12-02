package base;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.security.auth.login.LoginException;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;

import org.jetbrains.annotations.Nullable;

import base.Bot.ShutdownReason;
import engines.base.CentralTimer;
import engines.base.ConsoleCommandListener;
import engines.base.ScrollEngine;
import engines.data.ConfigManager;
import engines.logging.ConsoleEngine;
import net.dv8tion.jda.api.entities.Guild;
import net.miginfocom.swing.MigLayout;

public class GUI extends JFrame implements FocusListener {
	
	public static GUI INSTANCE;
	
	private static final long serialVersionUID = 5923282583431103590L;
    
    public final ConcurrentHashMap<Argument, String> argument = new ConcurrentHashMap<>();
    public final JTextArea console = new JTextArea();
    public final JTextField consoleIn = new JTextField();
    public final JTextField databaseIP = new JTextField();
    public final JTextField databaseName = new JTextField();
    public final JTextField botToken = new JTextField();
    public final JTextField databasePort = new JTextField();
    public final JTextField username = new JTextField();
    public final JPasswordField password = new JPasswordField();
    
	private final JLabel greenLED = new JLabel();
	private final JLabel redLED = new JLabel();
	private final JButton startButton = new JButton("Start");
	private final JButton stopButton = new JButton("Stop");
	private final JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
	private final JTable infoTable = new JTable();
	private final JTable commandTable = new JTable();
	private final JButton showPassword = new JButton("");
	private final JCheckBox shutdownWindowBox = new JCheckBox("");
	private final JLabel sdWLabel = new JLabel("Custom shutdown reason:");
    private final Font default_font;
    private final Font console_font;
	
	private ImageIcon greenLEDOn;
	private ImageIcon greenLEDOff;
	private ImageIcon redLEDOn;
	private ImageIcon redLEDOff;
	private ImageIcon eyeIconRaw;
	private ImageIcon windowIcon;
    
    private long runtimeMeasuringTaskId;
    private OffsetDateTime startTime = null;
    private Duration additional = Duration.ZERO;
    private boolean invalidArguments, autostart = false;
	
	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
		} catch (Throwable e) {}
		try {
            new GUI(args);
        } catch (IOException | FontFormatException e) {
            e.printStackTrace();
        }
	}
	
	public GUI(String[] args) throws IOException, FontFormatException {
		INSTANCE = this;
		this.processArguments(args);
		
		ClassLoader loader = this.getClass().getClassLoader();
		greenLEDOn = new ImageIcon(loader.getResourceAsStream("gui/green_on.png").readAllBytes());
		greenLEDOff = new ImageIcon(loader.getResourceAsStream("gui/green_off.png").readAllBytes());
		redLEDOn = new ImageIcon(loader.getResourceAsStream("gui/red_on.png").readAllBytes());
		redLEDOff = new ImageIcon(loader.getResourceAsStream("gui/red_off.png").readAllBytes());
		eyeIconRaw = new ImageIcon(loader.getResourceAsStream("gui/eye_icon.png").readAllBytes());
		windowIcon = new ImageIcon(loader.getResourceAsStream("misc/self_avatar.png").readAllBytes());
        default_font = Font.createFont(Font.TRUETYPE_FONT, loader.getResourceAsStream("gui/default_font.ttf")).deriveFont(0, 12f);
        console_font = Font.createFont(Font.TRUETYPE_FONT, loader.getResourceAsStream("gui/console_font.ttf")).deriveFont(0, 12f);
		
		setIconImage(windowIcon.getImage());
		setSize(1200, 600);
		setTitle(Bot.NAME + " - " + Bot.VERSION);
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setLocationRelativeTo(null);
		
		getContentPane().setLayout(new MigLayout("", "[600,grow][125:125:125][75:75:75][140:140:140][30:30:30][30:30:30]", "[30:n][20:n][20:n][20:n][20:n][510,grow][20:n]"));
		
		JScrollPane consoleScrollPane = new JScrollPane(console);
		consoleScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		consoleScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        new ScrollEngine(consoleScrollPane);
		console.setEditable(false);
		console.setFont(console_font);
		getContentPane().add(consoleScrollPane, "flowx,cell 0 0 1 6,grow");

		greenLED.setIcon(greenLEDOff);
		getContentPane().add(greenLED, "cell 4 0,alignx center");
		
		redLED.setIcon(redLEDOn);
		getContentPane().add(redLED, "flowx,cell 5 0,alignx center");
		
		this.setupTextField(databaseIP, "Server IP", argument.get(Argument.DATABASE_IP));
		getContentPane().add(databaseIP, "cell 1 0,growx,aligny bottom");
		
		this.setupTextField(databasePort, "Port", argument.get(Argument.DATABASE_PORT));
		getContentPane().add(databasePort, "cell 2 0,growx,aligny bottom");
		
		this.setupTextField(databaseName, "Database name", argument.get(Argument.DATABASE_NAME));
		getContentPane().add(databaseName, "cell 3 0,growx,aligny bottom");
		
		this.setupTextField(username, "Username", argument.get(Argument.USERNAME));
		getContentPane().add(username, "cell 1 1 2 1,grow");
		
		password.setEchoChar((char) 0);
		password.setForeground(Color.GRAY);
		password.setFont(default_font);
		password.setName("Password");
		password.setText(password.getName());
		password.addFocusListener(new FocusListener() {		
			@Override
			public void focusLost(FocusEvent e) {
				JPasswordField field = (JPasswordField) e.getComponent();
				if (String.copyValueOf(field.getPassword()).equals("")) {
					field.setText(field.getName());
					field.setForeground(Color.GRAY);
					field.setEchoChar((char) 0);
				}
			}
			@Override
			public void focusGained(FocusEvent e) {
				JPasswordField field = (JPasswordField) e.getComponent();
				if (String.copyValueOf(field.getPassword()).equals(field.getName())) {
					field.setText("");
					field.setForeground(Color.BLACK);
					field.setEchoChar('*');
				}
			}
		});
		if (argument.get(Argument.PASSWORD) != null) {
            password.setText(argument.get(Argument.PASSWORD));
            password.setForeground(Color.BLACK);
            password.setEchoChar('*');
		}
		getContentPane().add(password, "cell 3 1 2 1,grow");
		
		showPassword.setSize(30, 20);
		showPassword.setMargin(new Insets(0,0,0,0));
		Image eyeIconRescaled = eyeIconRaw.getImage().getScaledInstance(15, 15, Image.SCALE_SMOOTH);
		showPassword.setIcon(new ImageIcon(eyeIconRescaled));
		showPassword.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				password.setEchoChar((char) 0);
	        }
			@Override
	        public void mouseReleased(MouseEvent e) {
				password.setEchoChar('*');
	        }
		});
		getContentPane().add(showPassword, "cell 5 1,alignx left");
		
		this.setupTextField(botToken, "Bot Token", argument.get(Argument.TOKEN));
		getContentPane().add(botToken, "cell 1 2 5 1,grow");
		
		startButton.addActionListener(e -> {
			this.startBot();
		});
		startButton.setFont(default_font);
		getContentPane().add(startButton, "flowx,cell 1 3 2 1,grow");
		
		stopButton.addActionListener(e -> {
			if (shutdownWindowBox.isSelected()) {
				new ShutdownWindow((reasons, additionalMessage) -> this.shutdownBot(reasons.get(0), true, additionalMessage));
			} else {
				this.shutdownBot(ShutdownReason.OFFLINE, true, null);
			}
		});
		stopButton.setEnabled(false);
		stopButton.setFont(default_font);
		getContentPane().add(stopButton, "cell 3 3 3 1,grow");
		
		sdWLabel.setFont(default_font);
		getContentPane().add(sdWLabel, "cell 3 4 2 1,alignx right,aligny center");
		
		getContentPane().add(shutdownWindowBox, "cell 5 4,alignx left,aligny center");
		
		infoTable.setShowGrid(false);
		infoTable.setFont(default_font);
		infoTable.setModel(new DefaultTableModel(
			new Object[][] {
				{"Name:", Bot.NAME},
				{"Version:", Bot.VERSION},
				{"ID:", Bot.ID},
				{"Runtime:", "00:00:00:00"},
				{"Errors:", 0},
				{"Executions:", 0},
				{"Servers:", 0},
				{"Users:", 0},
				{"J2C-Channels", 0},
				{"Push Cycle Period:", String.valueOf(ConfigManager.PUSH_CYCLE_PERIOD) + " min."},
				{"Total Pushs:", 0}
			},
			new String[] {
				"key", "value"
			}
		) {private static final long serialVersionUID = 4012626449837340333L;
			
		   public boolean isCellEditable(int row, int column) {
				return false;
		   }
		});
		infoTable.getColumnModel().getColumn(0).setResizable(false);
		tabbedPane.addTab("Info", null, infoTable, null);

		commandTable.setShowGrid(false);
		commandTable.setFont(default_font);
		commandTable.setModel(new DefaultTableModel(
			new Object[][] {
				{"stop", "[Send Message]"},
				{"restart", ""},
				{"exit", ""},
				{"warn", "[Guild ID]  [User ID]"},
				{"pushCache", ""},
				{"printCache", ""},
				{"clearCache", ""},
				{"printEventAwaiter", ""},
				{"clearEventAwaiter", ""}
			},
			new String[] {
				"key", "value"
			}
		) {private static final long serialVersionUID = 7041769283649777050L;

		public boolean isCellEditable(int row, int column) {
				return false;
		   }
		});
		commandTable.getColumnModel().getColumn(0).setResizable(false);
		
		tabbedPane.addTab("Commands", null, commandTable, null);
		tabbedPane.setFont(default_font);
		getContentPane().add(tabbedPane, "cell 1 5 5 2,grow");
		
		consoleIn.addActionListener(new ConsoleCommandListener());
		consoleIn.setFont(console_font);
		getContentPane().add(consoleIn, "cell 0 6,growx,aligny center");
		
		setVisible(true);
		
		if (this.invalidArguments) {
		    ConsoleEngine.getLogger(this).warn("Encountered a problem whilst parsing arguments!");
		}
		if (this.autostart) {
		    this.startBot();
		}
	}

	public void startBot() {
		if (Bot.isShutdown()) {
			try {
				new Bot(botToken.getText(), databaseIP.getText(), databasePort.getText(), databaseName.getText(), username.getText(), String.copyValueOf(password.getPassword()));
			} catch (LoginException | InterruptedException | IOException e) {
				ConsoleEngine.getLogger(Bot.class).error("Bot instanciation failed - Check token validity!");
				Bot.get().kill();
			} catch (IllegalArgumentException e) {
				ConsoleEngine.getLogger(Bot.class).error("Bot instanciation failed - " + e.getMessage());
				Bot.get().kill();
			}
		}
	}
	
	public void restartBot() {
	    if (!Bot.isShutdown()) {
	        Runtime.getRuntime().removeShutdownHook(Bot.get().getShutdownThread());
            Bot.get().shutdown(ShutdownReason.RESTART, false, null, () -> startBot());
        }
	}
	
	public void shutdownBot(ShutdownReason reason, boolean sendMessage, @Nullable String additionalMessage) {
		if (!Bot.isShutdown()) {
			Runtime.getRuntime().removeShutdownHook(Bot.get().getShutdownThread());
			Bot.get().shutdown(reason, sendMessage, additionalMessage, null);
		}
	}
    
    public void startRuntimeMeasuring() {
        startTime = OffsetDateTime.now();
        this.runtimeMeasuringTaskId = CentralTimer.get().schedule(new Runnable() {
            @Override
            public void run() {
                Duration diff = Duration.between(startTime, OffsetDateTime.now()).plus(additional);
                GUI.INSTANCE.setTableValue(3, ConfigManager.convertDurationToString(diff));
            }
        }, TimeUnit.MILLISECONDS,  0, TimeUnit.SECONDS, 1);
    }
    
    public void stopRuntimeMeasuring() {
        if (startTime != null) {
            additional = Duration.between(startTime, OffsetDateTime.now());
        }
        CentralTimer.get().cancel(this.runtimeMeasuringTaskId);
    }
	
	public void setBotRunning(boolean status) {
		if (status) {
			greenLED.setIcon(greenLEDOn);
			redLED.setIcon(redLEDOff);
		} else {
			redLED.setIcon(redLEDOn);
			greenLED.setIcon(greenLEDOff);
		}
		stopButton.setEnabled(status);
		startButton.setEnabled(!status);
	}
	
	public void increaseErrorCounter() {
		this.increaseCounter(4);
	}
	
	public void increaseExecutionsCounter() {
		this.increaseCounter(5);
	}
    
    public void increaseMemberCounter() {
        this.increaseCounter(7);
    }
    
    public void decreaseMemberCounter() {
        this.decreaseCounter(7);
    }
	
	public void increaseJ2CCounter() {
	    this.increaseCounter(8);
	}
	
	public void decreaseJ2CCounter() {
	    this.decreaseCounter(8);
	}
	
	public void increasePushCounter() {
		this.increaseCounter(10);
	}
	
	private void increaseCounter(int position) {
	    this.modifyCounter(position, 1);
	}
	
	private void decreaseCounter(int position) {
        this.modifyCounter(position, -1);
	}
	
	private void modifyCounter(int position, int value) {
        try {
            this.setTableValue(position, (int) this.getTableValue(position) + value);
        } catch (ClassCastException e) {
            ConsoleEngine.getLogger(this).debug("Couldn't cast table entry to counter for position: " + String.valueOf(position));
        }
	}
    
    public void updateStatistics() {
        int guildCount = 0;
        int userCount = 0;
        for (Guild guild : Bot.getAPI().getGuilds()) {
            guildCount++;
            userCount += guild.getMemberCount();
        }
        this.setTableValue(6, guildCount);
        this.setTableValue(7, userCount);
    }
    
    private void processArguments(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String[] input = args[i].split(" ");
            if (input.length < 2) {
                this.invalidArguments = true;
            } else {
                for (int a = 0; a < input.length; a++) {
                    if (input[a].startsWith("--")) {
                        Argument argumentType = null;
                        switch (input[a]) {
                            case "--dbip":
                                argumentType = Argument.DATABASE_IP;
                                break;
                            case "--dbport":
                                argumentType = Argument.DATABASE_PORT;
                                break;
                            case "--dbname":
                                argumentType = Argument.DATABASE_NAME;
                                break;
                            case "--user":
                                argumentType = Argument.USERNAME;
                                break;
                            case "--password":
                                argumentType = Argument.PASSWORD;
                                break;
                            case "--token":
                                argumentType = Argument.TOKEN;
                                break;
                            case "--autostart":
                                this.autostart = true;
                                break;
                            default:
                                this.invalidArguments = true;
                        }
                        if (argumentType != null && a+1 < input.length && !input[a+1].startsWith("--")) {
                            a += 1;
                            String value = input[a];
                            if (!value.isBlank()) {
                                if (value.startsWith(">")) {
                                    for (int e = a+1; e < input.length; e++) {
                                        a += 1;
                                        value += " " + input[e];
                                        if (value.endsWith("<")) {
                                            e = input.length;
                                        }
                                    }
                                    value = value.replace(">", "").replace("<", "");
                                }
                                if (argumentType == Argument.DATABASE_IP && value.contains(":")) {
                                    String[] split_value = value.split(":");
                                    this.argument.put(Argument.DATABASE_IP, split_value[0]);
                                    this.argument.put(Argument.DATABASE_PORT, split_value[1]);
                                } else {
                                    this.argument.put(argumentType, value);
                                }
                            }
                        }
                    } else {
                        this.invalidArguments = true;
                    }
                }
            }
        }
    }
    
    private void setupTextField(JTextField textField, String name, @Nullable String value) {
        textField.setForeground(Color.GRAY);
        textField.setFont(default_font);
        textField.setName(name);
        textField.setText(name);
        textField.addFocusListener(this);
        if (value != null) {
            textField.setText(value);
            textField.setForeground(Color.BLACK);
        }
    }
    
    public void setTableValue(int row, Object value) {
        infoTable.getModel().setValueAt(value, row, 1);
        infoTable.repaint();
    }
    
    public Object getTableValue(int row) {
        return infoTable.getModel().getValueAt(row, 1);
    }
	
	@Override
	public void focusGained(FocusEvent e) {
		JTextField textField = (JTextField) e.getComponent();
		if (textField.getText().equals(textField.getName())) {
			textField.setForeground(Color.BLACK);
			textField.setText("");
		}
	}

	@Override
	public void focusLost(FocusEvent e) {
		JTextField textField = (JTextField) e.getComponent();
		if (textField.getText().equals(textField.getName()) || textField.getText().equals("")) {
			textField.setForeground(Color.GRAY);
			textField.setText(textField.getName());
		}
	}
	
	private static enum Argument {
	    DATABASE_IP,
	    DATABASE_PORT,
	    DATABASE_NAME,
	    USERNAME,
	    PASSWORD,
	    TOKEN;
	}
}