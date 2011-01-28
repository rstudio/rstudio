/*
 * Copyright 2008 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.shell.log;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.dev.BootStrapPlatform;
import com.google.gwt.dev.shell.CloseButton;
import com.google.gwt.dev.shell.CloseButton.Callback;
import com.google.gwt.dev.shell.WrapLayout;
import com.google.gwt.dev.shell.log.SwingTreeLogger.LogEvent;
import com.google.gwt.dev.util.BrowserLauncher;
import com.google.gwt.dev.util.log.AbstractTreeLogger;
import com.google.gwt.dev.util.log.CompositeTreeLogger;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.HeadlessException;
import java.awt.Point;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.text.html.HTMLDocument;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

/**
 * Swing widget containing a tree logger.
 * 
 * <p>
 * This class should not be serialized.
 * </p>
 */
public class SwingLoggerPanel extends JPanel implements TreeSelectionListener,
    HyperlinkListener {

  /**
   * Callback interface for optional close button behavior.
   */
  public interface CloseHandler {
    
    /**
     * Called when the close button has been clicked on the tree logger
     * and any confirmation needed has been handled.
     * 
     * @param loggerPanel SwingTreeLogger instance being closed
     */
    void onCloseRequest(SwingLoggerPanel loggerPanel);
  }

  private class FindBox extends JPanel {

    private Popup findPopup;
    private String lastSearch;

    private ArrayList<DefaultMutableTreeNode> matches;

    private int matchNumber;
    private JTextField searchField;
    private JLabel searchStatus;

    public FindBox() {
      super(new BorderLayout());
      JPanel top = new JPanel(new FlowLayout());
      searchField = new JTextField(20);
      top.add(searchField);
      JButton nextButton = new JButton("+");
      top.add(nextButton);
      nextButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          nextMatch();
        }
      });
      JButton prevButton = new JButton("-");
      top.add(prevButton);
      prevButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          prevMatch();
        }
      });
      CloseButton closeButton = new CloseButton("Close this search box");
      closeButton.setCallback(new Callback() {
        public void onCloseRequest() {
          hideFindBox();
        }
      });
      top.add(closeButton);
      KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
      getInputMap(WHEN_IN_FOCUSED_WINDOW).put(key, "find-cancel");
      key = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
      getInputMap(WHEN_IN_FOCUSED_WINDOW).put(key, "find-search");
      getActionMap().put("find-search", new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          lastSearch = searchField.getText();
          matches = doFind(lastSearch);
          matchNumber = 0;
          updateSearchResult();
        }
      });
      AbstractAction closeFindBox = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          hideFindBox();
        }
      };
      getActionMap().put("find-cancel", closeFindBox);
      add(top, BorderLayout.NORTH);
      searchStatus = new JLabel("Type search text and press Enter");
      searchStatus.setBorder(BorderFactory.createEmptyBorder(0, 2, 2, 0));
      add(searchStatus, BorderLayout.SOUTH);
    }

    public void hideBox() {
      if (findPopup != null) {
        findPopup.hide();
        findPopup = null;
      }
    }

    public void nextMatch() {
      if (matches != null && matches.size() > 0) {
        matchNumber = (matchNumber + 1) % matches.size();
        updateSearchResult();
      }
    }

    public void prevMatch() {
      if (matches != null) {
        int n = matches.size();
        if (n > 0) {
          matchNumber = (matchNumber + n - 1) % n;
          updateSearchResult();
        }
      }
    }

    public void showBox() {
      Point loggerOrigin = details.getLocationOnScreen();
      Dimension dim = details.getSize();
      if (findPopup != null) {
        findPopup.hide();
      }
      // have to display once to get the correct size
      int width = findBox.getWidth();
      boolean needsRelocate = (width <= 0);
      int x = loggerOrigin.x + dim.width - width;
      int y = loggerOrigin.y + dim.height - findBox.getHeight();
      PopupFactory popupFactory = PopupFactory.getSharedInstance();
      // TODO(jat): need to track window resize?
      findPopup = popupFactory.getPopup(SwingLoggerPanel.this, findBox, x, y);
      findPopup.show();
      if (needsRelocate) {
        x = loggerOrigin.x + dim.width - findBox.getWidth();
        y = loggerOrigin.y + dim.height - findBox.getHeight();
        findPopup.hide();
        findPopup = popupFactory.getPopup(SwingLoggerPanel.this, findBox, x, y);
        findPopup.show();
      }
      searchField.requestFocusInWindow();
    }

    /**
     * 
     */
    private void updateSearchResult() {
      int n = matches.size();
      if (n == 0) {
        searchStatus.setText("No matches");
      } else {
        searchStatus.setText(String.valueOf(matchNumber + 1) + " of "
            + n + " matches");
        showFindResult(matches.get(matchNumber), lastSearch);
      }
    }
  }

  private static class TreeRenderer extends DefaultTreeCellRenderer {
    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value,
        boolean sel, boolean expanded, boolean leaf, int row,
        boolean componentHasFocus) {
      super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row,
          componentHasFocus);
      DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
      Object userObject = node.getUserObject();
      if (userObject instanceof LogEvent) {
        LogEvent event = (LogEvent) userObject;
        event.setDisplayProperties(this);
      }
      return this;
    }
  }

  /**
   * The mask to use for Ctrl -- mapped to Command on Mac.
   */
  private static int ctrlKeyDown;

  private static final Color DISCONNECTED_COLOR = Color.decode("0xFFDDDD");

  static {
    ctrlKeyDown = BootStrapPlatform.isMac() ?  InputEvent.ALT_DOWN_MASK
        : InputEvent.CTRL_DOWN_MASK;
  }

  // package protected for SwingTreeLogger to access
  Type levelFilter;

  String regexFilter;

  final JTree tree;

  DefaultTreeModel treeModel;

  private CloseHandler closeHandler;

  private CloseButton closeLogger;

  private final JEditorPane details;

  private boolean disconnected = false;

  private FindBox findBox;

  private JComboBox levelComboBox;

  private final TreeLogger logger;

  private JTextField regexField;
  
  private DefaultMutableTreeNode root;

  private JPanel topPanel;
  
  private JScrollPane treeView;

  /**
   * Create a Swing-based logger panel, with a tree section and a detail
   * section.
   * 
   * @param maxLevel
   * @param logFile
   */
  public SwingLoggerPanel(TreeLogger.Type maxLevel, File logFile) {
    super(new BorderLayout());
    regexFilter = "";
    levelFilter = maxLevel;
    // TODO(jat): how to make the topPanel properly layout items
    // when the window is resized
    topPanel = new JPanel(new BorderLayout());
    JPanel logButtons = new JPanel(new WrapLayout());
    JButton expandButton = new JButton("Expand All");
    expandButton.setMnemonic(KeyEvent.VK_E);
    expandButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        expandAll();
      }
    });
    logButtons.add(expandButton);
    JButton collapseButton = new JButton("Collapse All");
    collapseButton.setMnemonic(KeyEvent.VK_O);
    collapseButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        collapseAll();
      }
    });
    logButtons.add(collapseButton);
    topPanel.add(logButtons, BorderLayout.CENTER);
    // TODO(jat): temporarily avoid showing parts that aren't implemented.
    if (false) {
      logButtons.add(new JLabel("Filter Log Messages: "));
      levelComboBox = new JComboBox();
      for (TreeLogger.Type type : TreeLogger.Type.instances()) {
        if (type.compareTo(maxLevel) > 0) {
          break;
        }
        levelComboBox.addItem(type);
      }
      levelComboBox.setEditable(false);
      levelComboBox.setSelectedIndex(levelComboBox.getItemCount() - 1);
      topPanel.add(levelComboBox);
      levelComboBox.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          setLevelFilter((TreeLogger.Type) levelComboBox.getSelectedItem());
        }
      });
      regexField = new JTextField(20);
      logButtons.add(regexField);
      JButton applyRegexButton = new JButton("Apply Regex");
      applyRegexButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          setRegexFilter(regexField.getText());
        }
      });
      logButtons.add(applyRegexButton);
      JButton clearRegexButton = new JButton("Clear Regex");
      clearRegexButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          regexField.setText("");
          setRegexFilter("");
        }
      });
      logButtons.add(clearRegexButton);
    }
    closeLogger = new CloseButton("Close this log window");
    closeLogger.setCallback(new Callback() {
      // TODO(jat): add support for closing active session when SWT is removed
      public void onCloseRequest() {
        if (disconnected && closeHandler != null) {
          closeHandler.onCloseRequest(SwingLoggerPanel.this);
        }
      }
    });
    closeLogger.setEnabled(false);
    closeLogger.setVisible(false);
    topPanel.add(closeLogger, BorderLayout.EAST);
    add(topPanel, BorderLayout.NORTH);
    root = new DefaultMutableTreeNode();
    treeModel = new DefaultTreeModel(root);
    tree = new JTree(treeModel);
    tree.setRootVisible(false);
    tree.setEditable(false);
    tree.setExpandsSelectedPaths(true);
    tree.setShowsRootHandles(true);
    tree.setCellRenderer(new TreeRenderer());
    tree.getSelectionModel().setSelectionMode(
        TreeSelectionModel.SINGLE_TREE_SELECTION);
    tree.addTreeSelectionListener(this);
    treeView = new JScrollPane(tree);
    // TODO(jat): better way to do this
    details = new JEditorPane() {
      @Override
      public boolean getScrollableTracksViewportWidth() {
        return true;
      }
    };
    details.setEditable(false);
    details.setContentType("text/html");
    details.setForeground(Color.BLACK);
    details.addHyperlinkListener(this);
    // font trick from http://explodingpixels.wordpress.com/2008/10/28/make-jeditorpane-use-the-system-font/
    Font font = UIManager.getFont("Label.font");
    String bodyRule = "body { font-family: " + font.getFamily() + "; "
        + "font-size: " + font.getSize() + "pt; }";
    ((HTMLDocument) details.getDocument()).getStyleSheet().addRule(bodyRule);
    JScrollPane msgView = new JScrollPane(details);
    JSplitPane splitter = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    splitter.setTopComponent(treeView);
    splitter.setBottomComponent(msgView);
    Dimension minSize = new Dimension(100, 50);
    msgView.setMinimumSize(minSize);
    treeView.setMinimumSize(minSize);
    splitter.setDividerLocation(0.80);
    add(splitter);
    
    AbstractTreeLogger uiLogger = new SwingTreeLogger(this);
    uiLogger.setMaxDetail(maxLevel);
    TreeLogger bestLogger = uiLogger;
    if (logFile != null) {
      try {
        PrintWriterTreeLogger fileLogger = new PrintWriterTreeLogger(logFile);
        fileLogger.setMaxDetail(maxLevel);
        bestLogger = new CompositeTreeLogger(bestLogger, fileLogger);
      } catch (IOException ex) {
        bestLogger.log(TreeLogger.ERROR, "Can't log to file "
            + logFile.getAbsolutePath(), ex);
      }
    }
    logger = bestLogger;
    KeyStroke key = getCommandKeyStroke(KeyEvent.VK_F, false);
    getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(key, "find");
    getActionMap().put("find", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        showFindBox();
      }
    });
    key = getCommandKeyStroke(KeyEvent.VK_C, false);
    tree.getInputMap().put(key, "copy");
    tree.getActionMap().put("copy", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        treeCopy();
      }
    });
    findBox = new FindBox();
    key = getCommandKeyStroke(KeyEvent.VK_G, false);
    tree.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(key, "findnext");
    tree.getActionMap().put("findnext", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        findBox.nextMatch();
      }
    });
    key = getCommandKeyStroke(KeyEvent.VK_G, true);
    tree.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(key, "findprev");
    tree.getActionMap().put("findprev", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        findBox.prevMatch();
      }
    });
  }

  /**
   * Collapse all tree nodes.
   */
  @SuppressWarnings("unchecked")
  public void collapseAll() {
    Enumeration<DefaultMutableTreeNode> children = root.postorderEnumeration();
    while (children.hasMoreElements()) {
      DefaultMutableTreeNode node = children.nextElement();
      if (node != root) {
        tree.collapsePath(new TreePath(node.getPath()));
      }
    }
    tree.invalidate();
  }

  /**
   * Show that the client connected to this logger has disconnected.
   */
  public void disconnected() {
    disconnected  = true;
    tree.setBackground(DISCONNECTED_COLOR);
    tree.repaint();
  }

  /**
   * Expand all tree nodes.
   */
  @SuppressWarnings("unchecked")
  public void expandAll() {
    Enumeration<DefaultMutableTreeNode> children = root.postorderEnumeration();
    while (children.hasMoreElements()) {
      DefaultMutableTreeNode node = children.nextElement();
      if (node != root) {
        tree.expandPath(new TreePath(node.getPath()));
      }
    }
    tree.invalidate();
  }

  /**
   * @return the TreeLogger for this panel
   */
  public TreeLogger getLogger() {
    return logger;
  }

  public void hyperlinkUpdate(HyperlinkEvent event) {
    EventType eventType = event.getEventType();
    if (eventType == HyperlinkEvent.EventType.ACTIVATED) {
      URL url = event.getURL();
      try {
        BrowserLauncher.browse(url.toExternalForm());
        return;
      } catch (Exception e) {
        // if anything fails, fall-through to failsafe implementation
      }
      // As a last resort, just use the details pane to display the HTML, but
      // this is rather poor.
      try {
        details.setPage(url);
      } catch (IOException e) {
        logger.log(TreeLogger.ERROR, "Unable to follow link to " + url, e);
      }
    }
  }

  /**
   * @param node
   */
  public void notifyChange(DefaultMutableTreeNode node) {
    treeModel.nodeChanged(node);
  }

  @Override
  public void removeAll() {
    tree.removeAll();
    details.setText("");
  }

  /**
   * Sets a callback for handling a close request, which also makes the close
   * button visible.
   * 
   * @param handler
   */
  public void setCloseHandler(CloseHandler handler) {
    closeHandler = handler;
    closeLogger.setEnabled(true);
    closeLogger.setVisible(true);
  }

  public void valueChanged(TreeSelectionEvent e) {
    if (e.isAddedPath()) {
      TreePath path = e.getPath();
      Object treeNode = path.getLastPathComponent();
      if (treeNode == null) {
        // handle the case of no selection
        details.setText("");
        return;
      }
      Object userObject = ((DefaultMutableTreeNode) treeNode).getUserObject();
      String text = userObject.toString();
      if (userObject instanceof LogEvent) {
        LogEvent event = (LogEvent) userObject;
        text = event.getFullText();
      }
      details.setText(text);
    }
  }

  protected void alert(String msg) {
    JOptionPane.showMessageDialog(null, msg, "Alert: Not Implemented",
        JOptionPane.INFORMATION_MESSAGE);
  }

  /**
   * Ask the user for confirmation to close the current logger.
   * 
   * @return true if the user confirmed the request
   */
  protected boolean confirmClose() {
    int response = JOptionPane.showConfirmDialog(null,
        "Close the logger for the currently displayed module",
        "Close this Logger", JOptionPane.OK_CANCEL_OPTION,
        JOptionPane.WARNING_MESSAGE);
    return response != JOptionPane.YES_OPTION;
  }

  protected ArrayList<DefaultMutableTreeNode> doFind(String search) {
    @SuppressWarnings("unchecked")
    Enumeration<DefaultMutableTreeNode> children = root.preorderEnumeration();
    ArrayList<DefaultMutableTreeNode> matches = new ArrayList<DefaultMutableTreeNode>();
    while (children.hasMoreElements()) {
      DefaultMutableTreeNode node = children.nextElement();
      if (node != root && nodeMatches(node, search)) {
        matches.add(node);
        // Make sure our this entry is visible by expanding up to parent
        TreeNode[] nodePath = node.getPath();
        if (nodePath.length > 1) {
          TreeNode[] parentPath = new TreeNode[nodePath.length - 1];
          System.arraycopy(nodePath, 0, parentPath, 0, parentPath.length);
          tree.expandPath(new TreePath(parentPath));
        }
      }
    }
    tree.invalidate();
    return matches;
  }

  protected void hideFindBox() {
    findBox.hideBox();
  }

  protected void setLevelFilter(Type selectedLevel) {
    levelFilter = selectedLevel;
    // TODO(jat): filter current tree
    alert("Filtering not implemented yet");
  }

  protected void setRegexFilter(String regex) {
    regexFilter = regex;
    // TODO(jat): filter current tree
    alert("Regex filtering not implemented yet");
  }

  protected void showFindBox() {
    findBox.showBox();
  }

  protected void treeCopy() {
    DefaultMutableTreeNode node = (DefaultMutableTreeNode)
        tree.getLastSelectedPathComponent();
    if (node == null) {
      return;
    }
    // is it better to use SwingUtilities2.canAccessSystemClipboard() here? 
    Clipboard clipboard;
    try {
      clipboard = tree.getToolkit().getSystemClipboard();
    } catch (SecurityException e) {
      return;
    } catch (HeadlessException e) {
      return;
    }
    if (clipboard == null) {
      return;
    }
    StringBuilder text = new StringBuilder();
    treeLogTraverse(text, node, 0);
    StringSelection selection = new StringSelection(text.toString());
    clipboard.setContents(selection, selection);
  }

  /**
   * Returns a keystroke which adds the appropriate modifier for a command key:
   * Command on mac, Ctrl everywhere else.
   * 
   * @param key virtual key defined in {@code KeyEvent#VK_*}
   * @param shift true if the Ctrl/Command key must be shifted
   * @return KeyStroke of the Ctrl/Command-key
   */
  private KeyStroke getCommandKeyStroke(int key, boolean shift) {
    int mask = ctrlKeyDown;
    if (shift) {
      mask |= InputEvent.SHIFT_DOWN_MASK;
    }
    return KeyStroke.getKeyStroke(key, mask);
  }

  private String htmlUnescape(String str) {
    // TODO(jat): real implementation, needs to correspond to
    // SwingTreeLogger.htmlEscape()
    return str.replace("&lt;", "<").replace("&gt;", ">").replace("&amp;",
        "&").replace("<br>", "\n");
  }

  private boolean nodeMatches(DefaultMutableTreeNode node, String search) {
    Object userObject = node.getUserObject();
    if (userObject instanceof LogEvent) {
      LogEvent event = (LogEvent) userObject;
      String text = htmlUnescape(event.getFullText());
      // TODO(jat): should this be more than a substring match, such as regex?
      if (text.contains(search)) {
        return true;
      }
    }
    return false;
  }

  /**
   * @param search the search string, currently ignored.
   */
  private void showFindResult(DefaultMutableTreeNode node, String search) {
    // TODO(jat): highlight search string
    TreePath path = new TreePath(node.getPath());
    tree.scrollPathToVisible(path);
    tree.setSelectionPath(path);
  }

  private void treeLogTraverse(StringBuilder buf, TreeNode node,
      int indent) {
    for (int i = 0; i < indent; ++i) {
      buf.append(' ');
    }
    if (node instanceof DefaultMutableTreeNode) {
      DefaultMutableTreeNode mutableNode = (DefaultMutableTreeNode) node;
      Object userObject = mutableNode.getUserObject();
      if (userObject instanceof LogEvent) {
        LogEvent event = (LogEvent) userObject;
        buf.append(htmlUnescape(event.getFullText()));
        if (event.isBranchCommit) {
          SwingTreeLogger childLogger = event.childLogger;
          DefaultMutableTreeNode parent = childLogger.treeNode;
          for (int i = 0; i < parent.getChildCount(); ++i) {
            treeLogTraverse(buf, parent.getChildAt(i), indent + 2);
          }
        }
      } else {
        buf.append(userObject.toString());
        buf.append('\n');
      }
    } else {
      buf.append(node.toString());
      buf.append('\n');
    }
  }
}
