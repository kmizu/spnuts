/*
 * VisualDebuggerView.java
 *
 * Copyright (c) 1997-2006 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution of
 * this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Element;
import javax.swing.text.Highlighter;
import javax.swing.UIManager;
import org.pnuts.awt.DialogOutputStream;
import pnuts.lang.Context;
import pnuts.lang.Runtime;
import pnuts.lang.SimpleNode;

public class VisualDebuggerView {
    public static final String imageSuffix = ".image";
    public static final String labelSuffix = ".label";
    public static final String actionSuffix = ".action";
    public static final String tipSuffix = ".tooltip";
    public static final String shortcutSuffix = ".shortcut";
    public static final String openAction = "open";
    public static final String stepAction = "step";
    public static final String stepUpAction = "stepUp";
    public static final String nextAction = "next";
    public static final String contAction = "cont";
    public static final String closeAction = "close";
    public static final String inspectAction = "inspect";
    public static final String clearAction = "clear";
    
    static final int DEFAULT_WIDTH = 500;
    static final int DEFAULT_HEIGHT = 600;
    static final Color DEFALT_CURRENT_POSITION_COLOR = Color.cyan;
    static final Color DEFALT_BREAK_POINT_COLOR = Color.orange;
    
    private static String actionNames[] = { openAction, stepAction,
    stepUpAction, nextAction, contAction, closeAction, clearAction,
    inspectAction };
    
    Action[] defaultActions = new Action[] {
        new MenuAction(VisualDebuggerModel.OPEN),
        new MenuAction(VisualDebuggerModel.STEP),
        new MenuAction(VisualDebuggerModel.STEP_UP),
        new MenuAction(VisualDebuggerModel.NEXT),
        new MenuAction(VisualDebuggerModel.CONT),
        new MenuAction(VisualDebuggerModel.CLOSE),
        new MenuAction(VisualDebuggerModel.CLEAR_BP),
        new MenuAction(VisualDebuggerModel.INSPECT) };
    
    private ResourceBundle resources;
    
    static {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    
    static ResourceBundle getDefaultResourceBundle(){
        try {
            return ResourceBundle.getBundle("pnuts.tools.dbg", Locale.getDefault());
        } catch (MissingResourceException mre){
            throw new RuntimeException("resource not found");
        }
    }
    
    private Color currentPositionColor;
    private Color breakPointColor;
    private Hashtable menuItems;
    private Hashtable commands;
    private boolean guiStarted = false;
    private Hashtable highlights; // source -> Highlighter
    private Hashtable tags; // source:line -> highlight tag
    private Object traceTag;
    private Element lineMap;
    private JFrame jfr;
    private JTextArea jta;
    private JToolBar toolbar;
    private JMenuBar jmb;
    private ContextView contextView;
    private JDialog inspector;
    private PrintWriter errorStream;
    private int windowWidth;
    private int windowHeight;
    protected VisualDebuggerModel model;
    
    public VisualDebuggerView() {
        this(getDefaultResourceBundle());
    }
    
    public VisualDebuggerView(ResourceBundle resourceBundle) {
        this.highlights = new Hashtable();
        this.tags = new Hashtable();
        this.commands = new Hashtable();
        for (int i = 0; i < actionNames.length; i++) {
            commands.put(actionNames[i], new MenuAction(i + 1));
        }
        this.resources = resourceBundle;
    }
    
    public VisualDebuggerModel getModel() {
        return model;
    }
    
    /**
     * Returns a JFrame
     *
     * The default behavior of this method creates and returns a JFrame object.
     * Subclasses may override this method to define a different way of getting JFrame.
     */
    protected JFrame getJFrame(){
	return new JFrame();
    }

    public void startGUI() {
        if (guiStarted) {
            return;
        }
        initializeLineColors();
        jfr = getJFrame();
        jfr.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                model.do_close();
                exitGUI();
            }
        });
        jta = createTextArea();
        menuItems = new Hashtable();
        jmb = createMenubar();
        if (jmb != null) {
            jfr.setJMenuBar(jmb);
        }
        Container contentPane = jfr.getContentPane();
        contentPane.setLayout(new BorderLayout());
        Component tb = createToolbar();
        if (tb != null) {
            contentPane.add("North", tb);
        }
        contentPane.add(new JScrollPane(jta));
        
        int w = getIntResource("width");
        if (w < 0) {
            w = DEFAULT_WIDTH;
        }
        int h = getIntResource("height");
        if (h < 0) {
            h = DEFAULT_HEIGHT;
        }
        jfr.setSize(w, h);
        jfr.setVisible(true);
        
        contextView = new ContextView(this);
        inspector = new JDialog(jfr);
        inspector.setLocation(jfr.getX() + jfr.getWidth() + 1, jfr.getY());
        inspector.getContentPane().add(contextView.getContainer());
        inspector.setSize((int) inspector.getPreferredSize().getWidth() + 20,
                (int)inspector.getPreferredSize().getHeight() + 20);
        
        errorStream = new PrintWriter(new DialogOutputStream(jfr), true);
        guiStarted = true;
    }
    
    void initializeLineColors() {
        int i = getIntResource("currentPositionColor");
        if (i < 0) {
            this.currentPositionColor = DEFALT_CURRENT_POSITION_COLOR;
        } else {
            this.currentPositionColor = new Color(i);
        }
        i = getIntResource("breakPointColor");
        if (i < 0) {
            this.breakPointColor = DEFALT_BREAK_POINT_COLOR;
        } else {
            this.breakPointColor = new Color(i);
        }
    }
    
    public void exitGUI() {
        jfr.dispose();
        jfr = null;
        jta = null;
        guiStarted = false;
    }
    
    /**
     * Returns the title string Subclasses may override this method to customize
     * the window title.
     *
     * @param source
     *            the script source
     * @return the title string
     */
    protected String getTitleString(Object source) {
        return (source == null) ? "?" : source.toString();
    }
    
    void update(Object source, int beginLine) {
        update(source, beginLine, null, null);
    }
    
    /**
     * Updates the view
     */
    public void update(Object source, int beginLine, SimpleNode node, Context c) {
        startGUI();
        c.setErrorWriter(errorStream);
        
        if (source == null) {
            if (node != null) {
                jta.setText(Runtime.unparse(node, null));
            } else {
                jta.setText("");
            }
        }
        
        contextView.setContext(c);
        try {
            Highlighter highlighter = null;
            if (source != null) {
                highlighter = (Highlighter) highlights.get(source);
                if (highlighter == null) {
                    highlighter = new DefaultHighlighter();
                    highlights.put(source, highlighter);
                }
            } else {
                highlighter = new DefaultHighlighter();
                jfr.setTitle(getTitleString(source));
            }
            boolean sourceChanged = false;
            
            if (source != null && !source.equals(model.getCurrentSource())) {
                sourceChanged = true;
                jfr.setTitle(getTitleString(source));
                StringWriter sw = new StringWriter();
                if (source instanceof URL) {
                    URL url = (URL) source;
                    InputStream in = url.openStream();
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(in));
                    try {
                        char[] buf = new char[512];
                        int n = 0;
                        while ((n = reader.read(buf)) != -1) {
                            sw.write(buf, 0, n);
                        }
                        jta.setText(sw.toString());
                    } finally {
                        reader.close();
                    }
                    
                } else if (source instanceof Runtime) {
                    jta.setText("");
                    return;
                } else if (source instanceof String) {
                    jta.setText((String) source);
                } else {
                    throw new RuntimeException("invalid source:" + source);
                }
            }
            
            if (source != null) {
                try {
                    if (sourceChanged) {
                        jta.setHighlighter(highlighter);
                    }
                    
                    lineMap = jta.getDocument().getDefaultRootElement();
                    
                    Vector lines = model.getBreakPoints(source);
                    if (lines != null) {
                        for (Enumeration e = lines.elements(); e
                                .hasMoreElements();) {
                            int line = ((Integer) e.nextElement()).intValue();
                            Element pos = lineMap.getElement(line - 1);
                            if (pos != null) {
                                int start = pos.getStartOffset();
                                int end = pos.getEndOffset() - 1;
                                
                                String key = source + ":" + (line - 1);
                                if (tags.get(key) == null){
                                    Object tag = highlighter
                                            .addHighlight(
                                            start,
                                            end,
                                            new DefaultHighlighter.DefaultHighlightPainter(
                                            breakPointColor));
                                    tags.put(key, tag);
                                }
                            }
                        }
                    }
                    if (beginLine > 0) {
                        Element pos = lineMap.getElement(beginLine - 1);
                        if (pos != null) {
                            int start = pos.getStartOffset();
                            int end = pos.getEndOffset() - 1;
                            
                            jta.select(start, start); // to make it visible
                            
                            if (traceTag != null && !sourceChanged) {
                                highlighter.changeHighlight(traceTag, start,
                                        end);
                            } else {
                                traceTag = highlighter
                                        .addHighlight(
                                        start,
                                        end,
                                        new DefaultHighlighter.DefaultHighlightPainter(
                                        currentPositionColor));
                            }
                        }
                    } else {
                        if (beginLine < 0) {
                            jta.setText("");
                        }
                    }
                } catch (BadLocationException ex) {
                    ex.printStackTrace();
                    return;
                }
                
            } else { // source == null
                if (beginLine > 0) {
                    lineMap = jta.getDocument().getDefaultRootElement();
                    
                    if (lineMap != null) {
                        Element pos = lineMap.getElement(beginLine - 1);
                        if (pos != null) {
                            int start = pos.getStartOffset();
                            int end = pos.getEndOffset() - 1;
                            try {
                                jta.select(start, start); // to make it visible
                                jta.setHighlighter(highlighter);
                                if (traceTag != null) {
                                    highlighter.changeHighlight(traceTag,
                                            start, end);
                                } else {
                                    traceTag = highlighter
                                            .addHighlight(
                                            start,
                                            end,
                                            new DefaultHighlighter.DefaultHighlightPainter(
                                            currentPositionColor));
                                }
                                
                            } catch (BadLocationException badLocation) {
                            }
                        }
                    }
                } else {
                    if (beginLine < 0) {
                        jta.setText("");
                    }
                }
            }
            jta.repaint();
            jfr.setVisible(true);
            
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
    
    int getElementIndex(int pos) {
        Element lineMap = jta.getDocument().getDefaultRootElement();
        return lineMap.getElementIndex(pos);
    }
    
    Element getElement(int pos) {
        Element lineMap = jta.getDocument().getDefaultRootElement();
        return lineMap.getElement(pos);
    }
    
    void showInspector() {
        inspector.setVisible(true);
    }
    
    void open() {
        synchronized (this) {
            notifyAll();
        }
        JFileChooser chooser = new JFileChooser(System.getProperty("user.dir"));
        int ret = chooser.showOpenDialog(jfr);
        if (ret == JFileChooser.APPROVE_OPTION) {
            final File file = chooser.getSelectedFile();
            
            Thread t = new Thread(new Runnable() {
                public void run() {
                    open(file.getPath());
                }
            });
            t.start();
        }
    }
    
    /**
     * Opens a local file in a window
     *
     * @param filename
     *            the file name
     */
    public void open(String filename) {
        File file = new File(filename);
        try {
            if (file.exists()) {
                update(Runtime.fileToURL(file), 0);
            } else {
                errorStream.println(filename + " is not found");
            }
        } catch (IOException e) {
        }
    }
    
    /**
     * Create the toolbar. By default this reads the resource file for the
     * definition of the toolbar.
     */
    private Component createToolbar() {
        String toolbarDef = getResourceString("toolbar");
        if (toolbarDef == null) {
            return null;
        }
        toolbar = new JToolBar();
        String[] toolKeys = tokenize(toolbarDef);
        for (int i = 0; i < toolKeys.length; i++) {
            if (toolKeys[i].equals("-")) {
                toolbar.add(Box.createHorizontalStrut(5));
            } else {
                toolbar.add(createToolbarButton(toolKeys[i]));
            }
        }
        toolbar.add(Box.createHorizontalGlue());
        return toolbar;
    }
    
    /**
     * Create a button to go inside of the toolbar. By default this will load an
     * image resource. The image filename is relative to the classpath
     * (including the '.' directory if its a part of the classpath), and may
     * either be in a JAR file or a separate file.
     *
     * @param key
     *            The key in the resource file to serve as the basis of lookups.
     */
    protected JButton createToolbarButton(String key) {
        URL url = getResource(key + imageSuffix);
        JButton b;
        if (url != null) {
            b = new JButton(new ImageIcon(url)) {
                public float getAlignmentY() {
                    return 0.5f;
                }
            };
        } else {
            b = new JButton(getResourceString(key + labelSuffix)) {
                public float getAlignmentY() {
                    return 0.5f;
                }
            };
        }
        b.setRequestFocusEnabled(false);
        b.setMargin(new Insets(1, 1, 1, 1));
        
        String astr = getResourceString(key + actionSuffix);
        if (astr == null) {
            astr = key;
        }
        Action a = getAction(astr);
        if (a != null) {
            b.setActionCommand(astr);
            b.addActionListener(a);
        } else {
            b.setEnabled(false);
        }
        
        String tip = getResourceString(key + tipSuffix);
        if (tip != null) {
            b.setToolTipText(tip);
        }
        
        return b;
    }
    
    String[] tokenize(String input) {
        Vector v = new Vector();
        StringTokenizer t = new StringTokenizer(input);
        String cmd[];
        
        while (t.hasMoreTokens()) {
            v.addElement(t.nextToken());
        }
        cmd = new String[v.size()];
        for (int i = 0; i < cmd.length; i++) {
            cmd[i] = (String) v.elementAt(i);
        }
        return cmd;
    }
    
    /**
     * Create the menubar for the app. By default this pulls the definition of
     * the menu from the associated resource file.
     *
     * @return a JMenuBar
     */
    protected JMenuBar createMenubar() {
        JMenuItem mi;
        JMenuBar mb = new JMenuBar();
        String mbdef = getResourceString("menubar");
        if (mbdef == null) {
            return null;
        }
        String[] menuKeys = tokenize(mbdef);
        for (int i = 0; i < menuKeys.length; i++) {
            JMenu m = createMenu(menuKeys[i]);
            if (m != null) {
                mb.add(m);
            }
        }
        return mb;
    }
    
    /**
     * Create a menu for the app. By default this pulls the definition of the
     * menu from the associated resource file.
     *
     * @param key
     *            name of a menu group
     */
    protected JMenu createMenu(String key) {
        String[] itemKeys = tokenize(getResourceString(key));
        JMenu menu = new JMenu(getResourceString(key + labelSuffix));
        for (int i = 0; i < itemKeys.length; i++) {
            if (itemKeys[i].equals("-")) {
                menu.addSeparator();
            } else {
                JMenuItem mi = createMenuItem(itemKeys[i]);
                menu.add(mi);
            }
        }
        return menu;
    }
    
    /**
     * Create a menu item for the specified command
     *
     * @param cmd
     *            the command name
     * @return the JMenuItem
     */
    protected JMenuItem createMenuItem(String cmd) {
        JMenuItem mi = new JMenuItem(getResourceString(cmd + labelSuffix));
        KeyStroke ks = KeyStroke.getKeyStroke(getResourceString(cmd
                + shortcutSuffix));
        if (ks != null) {
            mi.setAccelerator(ks);
        }
        String astr = getResourceString(cmd + actionSuffix);
        if (astr == null) {
            astr = cmd;
        }
        mi.setActionCommand(astr);
        Action a = getAction(astr);
        if (a != null) {
            mi.addActionListener(a);
            mi.setEnabled(a.isEnabled());
        } else {
            mi.setEnabled(false);
        }
        menuItems.put(cmd, mi);
        return mi;
    }
    
    int getIntResource(String nm) {
        String s = getResourceString(nm);
        if (s != null) {
            try {
                return Integer.decode(s).intValue();
            } catch (NumberFormatException e) {
            }
        }
        return -1;
    }
    
    protected String getResourceString(String nm) {
        String str;
        try {
            str = resources.getString(nm);
        } catch (MissingResourceException mre) {
            str = null;
        }
        return str;
    }
    
    protected URL getResource(String key) {
        String name = getResourceString(key);
        if (name != null) {
            URL url = VisualDebuggerView.class.getResource(name);
            return url;
        }
        return null;
    }
    
    Action getAction(String cmd) {
        return (Action) commands.get(cmd);
    }
    
    /**
     * Create an editor to represent the given document.
     */
    protected JTextArea createTextArea() {
        JTextArea jta = new JTextArea();
        jta.setFont(new Font("monospaced", Font.PLAIN, 12));
        jta.addMouseListener(new MouseHandler());
        jta.setEditable(false);
        return jta;
    }
    
    class MenuAction extends AbstractAction {
        private int id;
        
        MenuAction(int id) {
            this.id = id;
        }
        
        public void actionPerformed(ActionEvent e) {
            switch (id) {
                case VisualDebuggerModel.OPEN:
                    open();
                    break;
                case VisualDebuggerModel.STEP:
                    model.do_step(1);
                    break;
                case VisualDebuggerModel.STEP_UP:
                    model.do_stepup();
                    break;
                case VisualDebuggerModel.NEXT:
                    model.do_next(1);
                    break;
                case VisualDebuggerModel.CONT:
                    model.do_cont();
                    break;
                case VisualDebuggerModel.CLOSE:
                    model.do_close();
                    exitGUI();
                    break;
                case VisualDebuggerModel.CLEAR_BP: {
                    int start = -1;
                    int end = -1;
                    Highlighter.HighlightPainter p = null;
                    Highlighter.Highlight currentLineHighlight = (Highlighter.Highlight) traceTag;
                    if (currentLineHighlight != null) {
                        start = currentLineHighlight.getStartOffset();
                        end = currentLineHighlight.getEndOffset();
                        p = currentLineHighlight.getPainter();
                    }
                    model.clearBreakPoints();
                    highlights = new Hashtable();
                    tags = new Hashtable();
                    traceTag = null;
                    jta.setHighlighter(jta.getHighlighter());
                    highlights.put(model.getCurrentSource(), jta.getHighlighter());
                    if (currentLineHighlight != null) {
                        try {
                            traceTag=jta.getHighlighter().addHighlight(start, end, p);
                        } catch (BadLocationException e1) {
                            e1.printStackTrace();
                        }
                    }
                }
                break;
                case VisualDebuggerModel.INSPECT:
                    inspector.setVisible(!inspector.isVisible());
		    inspector.pack();
                    break;
            }
        }
    }
    
    class MouseHandler extends MouseAdapter {
        public void mousePressed(MouseEvent e) {
            maybeShowPopup(e);
        }
        
        public void mouseReleased(MouseEvent e) {
            maybeShowPopup(e);
        }
        
        private void maybeShowPopup(MouseEvent e) {
            if (!e.isPopupTrigger()) {
                return;
            }
            
            int x = e.getX();
            int y = e.getY();
            Point pt = new Point(x, y);
            int pos = jta.viewToModel(pt);
            if (pos > 0) {
                int line = getElementIndex(pos); // zero origin
                Element elem = getElement(line);
                
                try {
                    Object currentSource = model.getCurrentSource();
                    String key = currentSource + ":" + line;
                    Object tag = tags.get(key);
                    
                    Highlighter highlighter = jta.getHighlighter();
                    if (tag == null) {
                        tag = highlighter.addHighlight(elem.getStartOffset(),
                                elem.getEndOffset() - 1,
                                new DefaultHighlighter.DefaultHighlightPainter(
                                breakPointColor));
                        tags.put(key, tag);
                        
                        model.setBreakPoint(currentSource, line);
                    } else {
                        highlighter.removeHighlight(tag);
                        tags.remove(key);
                        model.removeBreakPoint(currentSource, line);
                    }
                } catch (BadLocationException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}
