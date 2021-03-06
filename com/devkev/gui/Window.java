package com.devkev.gui;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.HeadlessException;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

import com.devkev.devscript.raw.ApplicationInput;
import com.devkev.devscript.raw.ApplicationListener;
import com.devkev.devscript.raw.Output;
import com.devkev.devscript.raw.Process;

public class Window {
	
	JFrame window;
	int fontSize = 12;
	JMenuBar bar;
	JTextPane textArea;
	JScrollPane pane;
	Process p;
	
	ApplicationInput input;
	volatile boolean waitForEnter = false;
	volatile int inputStart = 0;
	
	private static Font font;
	private File openedFile = null; //Null means, creating a new file when saving.
	private static final String TITLE = "Devscript 1.9.0 Editor (Alpha)";
	private ArrayList<String> history = new ArrayList<String>();
	private int historyIndex = 0;
	public int maxHistorySize = 50;
	
	JLayeredPane layerPane;
	JPanel previewContainer;
	JLabel commandPreview;
	
	public Window() {
		try {
	        UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
	    } catch (Exception evt) {}
		font = new Font("Consolas", Font.PLAIN, 13);
		
		window = new JFrame(TITLE + " - unsaved");
		window.setVisible(true);
		window.setResizable(true);
		window.setLayout(null);
		window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		window.addComponentListener(new ComponentListener() {
			public void componentResized(ComponentEvent e) {
				if(textArea == null) return;
				pane.setSize(window.getRootPane().getWidth() - 10, window.getRootPane().getHeight()-bar.getHeight() - 10);
				layerPane.setSize(window.getRootPane().getWidth() - 10, window.getRootPane().getHeight()-bar.getHeight() - 10);
				pane.updateUI();
			}
			public void componentShown(ComponentEvent e) {}
			public void componentMoved(ComponentEvent e) {}
			public void componentHidden(ComponentEvent e) {}
		});
		window.setMinimumSize(new Dimension(180, 180));
		
		p = new Process(true);
		p.addOutput(new Output() {
			public void warning(String message) {
				console.append("[WARN]" + message + "\n");
				consolePane.getVerticalScrollBar().setValue(consolePane.getVerticalScrollBar().getMaximum());
			}
			public void log(String message, boolean newline) {
				console.append(message + (newline ? "\n" : ""));
				consolePane.getVerticalScrollBar().setValue(consolePane.getVerticalScrollBar().getMaximum());
			}
			public void error(String message) {
				console.append(message + "\n");
				consolePane.getVerticalScrollBar().setValue(consolePane.getVerticalScrollBar().getMaximum());
			}
		});
		input = new ApplicationInput() {
			@Override
			public void awaitInput() {
				waitForEnter = true;
				console.setEnabled(true);
				inputStart = console.getText().length();
				console.setCaretPosition(inputStart);
				console.setCaretColor(Color.blue);
			}
		};
		p.setInput(input);
		p.setApplicationListener(new ApplicationListener() {
			public void done(int exitCode) {
				runWindow.setTitle("Finished with Exit Code " + exitCode);
				window.setEnabled(true);
				console.setEnabled(false);
			}
		});
		
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
			@Override
			public boolean dispatchKeyEvent(KeyEvent e) {
				if(e.getKeyCode() == KeyEvent.VK_ENTER && input.inputRequested() && waitForEnter) {
					input.flush(console.getText().substring(inputStart, console.getCaretPosition()) + "\n");
					waitForEnter = false;
					console.setCaretColor(Color.black);
				}
				return false;
//				if(window.isEnabled() && e.isControlDown() && e.getKeyCode() == KeyEvent.VK_SPACE) {
//					String commandSnippet = textArea.getText();
//					for(int i = textArea.getCaretPosition(); i >= 0; i--) {
//						char current = textArea.getText().charAt(i);
//						System.out.println("CHecking: '" + current + "'");
//						if(current == ' ' || current == '\n' || current == '\t' || current == '\r' || Alphabet.partOf(current) || i == 0) {
//							System.out.println((i+1) + " " + textArea.getCaretPosition());
//							commandSnippet = textArea.getText().substring(i == 0 ? i : i+1, textArea.getCaretPosition()).trim();
//							System.out.println("Snippet: " + commandSnippet);
//							break;
//						}
//					}
//					if(!commandSnippet.isEmpty()) {
//						commandPreview.setVisible(true);
//						commandPreview.setLocation(textArea.getCaret().getMagicCaretPosition().x + 5, textArea.getCaret().getMagicCaretPosition().y + 25);
//						
//						StringBuilder html = new StringBuilder("<html><body>");
//						for(GeneratedLibrary lib : p.getLibraries()) {
//							for(Command c : lib.commands) {
//								if(c.name.length() >= commandSnippet.length()) {
//									if(c.name.substring(0, commandSnippet.length()).equals(commandSnippet)) {
//										String args = "";
//										for(DataType s : c.arguments) {
//											args += "[" + s.type + "] ";
//										}
//										html.append(c.name + " " + args + "<br>");
//									}
//								}
//							}
//						}
//						html.append("</body></html>");
//						System.out.println(html);
//						commandPreview.setText(html.toString());
//					}
//				}
//				return false;
			}
		});
		
		bar = new JMenuBar();
		
		JMenu m = new JMenu("File");
		
		JMenuItem newFile = new JMenuItem("New...");
		newFile.setAccelerator(KeyStroke.getKeyStroke("control N"));
		newFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textArea.setText("");
				openedFile = null;
				window.setTitle(TITLE + " - unsaved");
			}
		});
		m.add(newFile);
		
		JMenuItem loadFile = new JMenuItem(getFormattedBarText("Open..."));
		loadFile.setAccelerator(KeyStroke.getKeyStroke("control O"));
		loadFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				window.setEnabled(false);
				JFileChooser chooser = new JFileChooser();
				int res = chooser.showOpenDialog(new JFrame());
				if(res == JFileChooser.APPROVE_OPTION) {
					openDocument(chooser.getSelectedFile());
				} else {
					window.setEnabled(true);
				}
			}
		});
		m.add(loadFile);
		JMenuItem saveFile = new JMenuItem("Save File...");
		saveFile.setAccelerator(KeyStroke.getKeyStroke("control S"));
		saveFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(openedFile == null) {
					if(textArea.getText().isEmpty()) return;
					window.setEnabled(false);
					JFileChooser chooser = new JFileChooser();
					chooser.setDialogTitle("Save New File");
					int res = chooser.showSaveDialog(new JFrame());
					if(res == JFileChooser.APPROVE_OPTION) saveDocument(chooser.getSelectedFile());
					window.setEnabled(true);
				} else saveDocument(openedFile);
				window.setTitle(TITLE + " - saved");
			}
		});
		m.add(saveFile);
		m.addSeparator();
		JMenu examples = new JMenu("Examples");
		
		JMenuItem selectionSort = new JMenuItem("Selection Sort");
		selectionSort.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				openDocument(null);
				textArea.setText("#Selection sort example by DevKev#\r\n" + 
						"\r\n" + 
						"unsorted = [3 4 7 8 9 543 23 7 5 32 1 56 7 65 34 13 5 7  2];\r\n" + 
						"sorted = [];\r\n" + 
						"record = 99999;\r\n" + 
						"index = -1;\r\n" + 
						"\r\n" + 
						"loop" + 
						" {\r\n" + 
						"	for i (length $unsorted) \r\n" + 
						"	{\r\n" + 
						"		if ($unsorted[$i] lt $record) \r\n" + 
						"		{\r\n" + 
						"			record = $unsorted[$i];\r\n" + 
						"			index = $i;\r\n" + 
						"		};\r\n" + 
						"	};\r\n" + 
						"\r\n" + 
						"	push $unsorted[$index] $sorted;\r\n" + 
						"	pop $unsorted $index;\r\n" + 
						"	record = 99999;\r\n" + 
						"	\r\n" + 
						"	if ((length $unsorted) == 0) \r\n" + 
						"	{\r\n" + 
						"		break;\r\n" + 
						"	};\r\n" + 
						"};\r\n" + 
						"\r\n" + 
						"println \"Done\";\r\n" + 
						"println $sorted;");
			}
		});
		examples.add(selectionSort);
		
		JMenuItem calculator = new JMenuItem("Calculator");
		calculator.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				openDocument(null);
				textArea.setText("#Simple calculator with input#\r\n" + 
						"#Should also be an example for functions#\r\n" + 
						"\r\n" + 
						"calculate = {\r\n" + 
						"	num1 = $0; #Better names#\r\n" + 
						"	num2 = $1;\r\n" + 
						"	operator = $2;\r\n" + 
						"\r\n" + 
						"	ifnot (($num1 typeof num) or ($num2 typeof num))\r\n" + 
						"	{\r\n" + 
						"		println \"Error\";\r\n" + 
						"		return;\r\n" + 
						"	};\r\n" + 
						"\r\n" + 
						"	if ($operator == +)\r\n" + 
						"	{\r\n" + 
						"		return ($num1 + $num2);\r\n" + 
						"	};\r\n" + 
						"	if ($operator == -) \r\n" + 
						"	{	\r\n" + 
						"		return ($num1 - $num2);\r\n" + 
						"	};\r\n" + 
						"	println \"Error, unknown operator: \" $operator;\r\n" + 
						"};\r\n" + 
						"\r\n" + 
						"print \"Enter first number: \";\r\n" + 
						"num1 = (input);\r\n" + 
						"print \"Enter second number: \";\r\n" + 
						"num2 = (input);\r\n" + 
						"print \"Enter operator (+, -): \";\r\n" + 
						"operator = (input);\r\n" + 
						"\r\n" + 
						"println \"Calculating...\";\r\n" + 
						"println (call $calculate $num1 $num2 $operator);");
			}
		});
		examples.add(calculator);
		
		m.add(examples);
		bar.add(m);
		
		JMenu m2 = new JMenu("Edit");
		JMenuItem undo = new JMenuItem(getFormattedBarText("Undo"));
		undo.setAccelerator(KeyStroke.getKeyStroke("control Z"));
		undo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(historyIndex > 0) {
					historyIndex--;
					textArea.setText(history.get(historyIndex));
					if(historyIndex == 0) undo.setEnabled(false);
				}
			}
		});
		undo.setEnabled(false);
		m2.add(undo);
		m2.addSeparator();
		JMenuItem selectAll = new JMenuItem(getFormattedBarText("Select All"));
		selectAll.setAccelerator(KeyStroke.getKeyStroke("control A"));
		selectAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textArea.selectAll();
			}
		});
		JMenuItem copyToClipboard = new JMenuItem(getFormattedBarText("Copy"));
		copyToClipboard.setAccelerator(KeyStroke.getKeyStroke("control C"));
		copyToClipboard.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(textArea.getSelectedText()),null);
			}
		});
		JMenuItem paste = new JMenuItem(getFormattedBarText("Paste"));
		paste.setAccelerator(KeyStroke.getKeyStroke("control V"));
		paste.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					textArea.setText((String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor));
				} catch (HeadlessException | UnsupportedFlavorException | IOException e1) {
					e1.printStackTrace();
				}
			}
		});
		m2.add(selectAll);
		m2.add(copyToClipboard);
		m2.add(paste);
		bar.add(m2);
		
		JMenu m3 = new JMenu("Run");
		JMenuItem run = new JMenuItem(getFormattedBarText("Run in Shell"));
		run.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
		run.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(p.isRunning()) return;
				console.setText("");
				runWindow.toFront();
				runWindow.setTitle("Running...");
				runWindow.setVisible(true);	
				window.setEnabled(false);
				p.execute(textArea.getText(), true);
			}
		});
		m3.add(run);
		bar.add(m3);
		
		JMenu m4 = new JMenu("Help");
		JMenuItem commandCC = new JMenuItem("Command Cheatsheet");
		commandCC.setToolTipText("Runs the 'help' command");
		commandCC.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(p.isRunning()) return;
				console.setText("");
				runWindow.toFront();
				runWindow.setVisible(true);
				p.execute("help", false);
			}});
		m4.add(commandCC);
		JMenuItem help = new JMenuItem("Tutorial");
		help.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ProcessBuilder pb = new ProcessBuilder("Notepad.exe", "README.md");
				try {
					pb.start();
				} catch (IOException e1) {
					e1.printStackTrace();
				}				
			}
		});
		m4.add(help);
		JMenuItem license = new JMenuItem("License");
		license.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ProcessBuilder pb = new ProcessBuilder("Notepad.exe", "LICENSE");
				try {
					pb.start();
				} catch (IOException e1) {
					e1.printStackTrace();
				}	
			}
		});
		JMenuItem changelog = new JMenuItem("Changelog");
		changelog.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ProcessBuilder pb = new ProcessBuilder("Notepad.exe", "changelog.txt");
				try {
					pb.start();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});
		m4.add(changelog);
		m4.add(license);
		m4.addSeparator();
		JMenuItem about = new JMenuItem("About / GitHub");
		about.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {
					Desktop.getDesktop().browse(new URI("https://github.com/DevKevYT/devscript"));
				} catch (IOException | URISyntaxException e) {
					e.printStackTrace();
				}
			}
		});
		m4.add(about);
		m4.addSeparator();
		JMenuItem ty = new JMenuItem("Thank you! <3");
		ty.setEnabled(false);
		m4.add(ty);
		bar.add(m4);
		
		window.setJMenuBar(bar);
		
		textArea = new JTextPane();
		textArea.setFont(font);
		textArea.setLayout(null);
		textArea.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {
				if(!e.isControlDown()) window.setTitle(TITLE + " - unsaved");
				commandPreview.setVisible(false);
			}
			public void keyReleased(KeyEvent e) {
				if(e.isControlDown()) return;
				history.add(textArea.getText());
				if(history.size() >= 20) history.remove(0);
				historyIndex = history.size()-1;
				undo.setEnabled(true);
			}
			public void keyPressed(KeyEvent e) {}
		});
		pane = new JScrollPane(textArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		pane.setBounds(5, 5, window.getRootPane().getWidth() - 10, window.getRootPane().getHeight()-bar.getHeight() - 10);
		pane.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		
		commandPreview = new JLabel("<html><body>println [STRING]<br>print [STRING]</body></html>");
		commandPreview.setBorder(BorderFactory.createLineBorder(Color.black, 1));
		commandPreview.setOpaque(true);
		commandPreview.setVisible(false);
		commandPreview.setForeground(Color.darkGray);
		commandPreview.setFont(new Font("Arial", Font.BOLD, 11));
		commandPreview.setBounds(0,  0,  200, 60);
		
		layerPane = new JLayeredPane();
		layerPane.add(pane);
		layerPane.add(commandPreview);
		layerPane.setLayer(commandPreview, 999);
		window.add(layerPane);
		
		initRunWindow();
		window.pack();
		window.setSize(500, 500);
	}
	
	public void setScript(String script) {
		appendToPane(textArea, script, Color.black);
	}
	
	public void openDocument(File file) {
		try {
			history.clear();
			textArea.setText("");
			if(file != null) {
				BufferedReader reader = new BufferedReader(new FileReader(file));
				String line = reader.readLine();
				while(line != null) {
					appendToPane(textArea, line + "\n", Color.black);
					line = reader.readLine();
				}
				reader.close();
			}
			window.setEnabled(true);
			window.toFront();
			openedFile = file;
			window.setTitle(TITLE);
		} catch (Exception e1) {
			e1.printStackTrace();
			window.setEnabled(true);
		}
	}
	
	public void saveDocument(File file) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			writer.write(textArea.getText());
			writer.close();
			openedFile = file;
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	JFrame saveDialog;
	JButton approveSave;
	JButton cancelSave;
	
	public void initSaveNotification() {
		saveDialog = new JFrame("Save changes to ");
	}
	
	JFrame runWindow;
	JTextArea console;
	JScrollPane consolePane;
	
	public void initRunWindow() {
		runWindow = new JFrame();
		runWindow.setVisible(false);
		runWindow.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		runWindow.addWindowListener(new WindowListener() {
			public void windowClosed(WindowEvent arg0) {}
			public void windowOpened(WindowEvent arg0) {}
			public void windowIconified(WindowEvent arg0) {}
			public void windowDeiconified(WindowEvent arg0) {}
			public void windowDeactivated(WindowEvent arg0) {}
			public void windowClosing(WindowEvent arg0) {
				if(p.isRunning()) {
					System.out.println("Closing running instance");
					p.kill(p.getMain(), "Interrupted by program");
				}
				window.setEnabled(true);
				inputStart = 0;
				input.flush(null);
				console.setText("");
			}
			public void windowActivated(WindowEvent arg0) {}
		});
		runWindow.addComponentListener(new ComponentListener() {
			@Override
			public void componentResized(ComponentEvent e) {
				console.setSize(window.getRootPane().getWidth() - 10, window.getRootPane().getHeight()-bar.getHeight() - 10);
				console.updateUI();
			}
			public void componentShown(ComponentEvent e) {}
			public void componentMoved(ComponentEvent e) {}
			public void componentHidden(ComponentEvent e) {}
		});
		
		console = new JTextArea();
		console.setFont(font);
		console.addCaretListener(new CaretListener() {
			@Override
			public void caretUpdate(CaretEvent e) {
				if(input.inputRequested() && console.getCaretPosition() < inputStart) {
					console.setCaretPosition(inputStart);
				}
			}
		});
		console.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {}
			public void keyReleased(KeyEvent e) {}
			public void keyPressed(KeyEvent e) {
				if(!input.inputRequested()) return;
				if(console.getCaretPosition() == inputStart && e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
					e.consume();
				}
			}
		});
		
		console.setEnabled(false);
		console.setDisabledTextColor(Color.DARK_GRAY);
		consolePane = new JScrollPane(console, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		consolePane.setBounds(5, 5, runWindow.getRootPane().getWidth()-10,  runWindow.getRootPane().getHeight()-10);
		consolePane.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		consolePane.setAutoscrolls(true);
		runWindow.add(consolePane);
		
		runWindow.pack();
		runWindow.setBounds((int) (Toolkit.getDefaultToolkit().getScreenSize().width *.5f-250), (int) (Toolkit.getDefaultToolkit().getScreenSize().height *.5f-100), 500, 200);
	}
	
	private String getFormattedBarText(String text) {
		return String.format("%s%20s", text, "");
	}
	
    private void appendToPane(JTextPane tp, String msg, Color c) {
        StyleContext sc = StyleContext.getDefaultStyleContext();
        AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, c);

        aset = sc.addAttribute(aset, StyleConstants.FontFamily, "Consolas");
        aset = sc.addAttribute(aset, StyleConstants.Alignment, StyleConstants.ALIGN_JUSTIFIED);

        int len = tp.getDocument().getLength();
        tp.setCaretPosition(len);
        tp.setCharacterAttributes(aset, false);
        tp.replaceSelection(msg);
        try {
        	tp.setCaretPosition(tp.getText().length()-1); //WTF
        } catch(Exception e) {}
    }
}
