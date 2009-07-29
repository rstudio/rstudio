/**
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
package com.google.gwt.dev;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.dev.shell.log.SwingLoggerPanel;
import com.google.gwt.dev.util.log.AbstractTreeLogger;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

/**
 */
public class ModulePanel extends JPanel {

  /**
   * A tab component with a close button, derived from Swing
   * TabComponentsDemoProject.
   */
  private class ClosedTabComponent extends JPanel {

    public ClosedTabComponent() {
      super(new FlowLayout(FlowLayout.LEFT, 0, 0));
      setOpaque(false);
      JButton button = new JButton(closeIcon);
      button.setBorderPainted(false);
      button.setPreferredSize(new Dimension(closeIcon.getIconWidth(),
          closeIcon.getIconHeight()));
      button.setToolTipText("Close this tab");
      add(button);
      button.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          synchronized (tabs) {
            tabs.remove(ModulePanel.this);
          }
        }
      });
    }
  }

  private static ImageIcon firefoxIcon = GWTShell.loadImageIcon("firefox24.png");

  private static ImageIcon ieIcon = GWTShell.loadImageIcon("ie24.png");

  private static ImageIcon safariIcon = GWTShell.loadImageIcon("safari24.png");

  private static ImageIcon closeIcon = GWTShell.loadImageIcon("close.png");

  private SwingLoggerPanel loggerPanel;

  private final JTabbedPane tabs;

  private JPanel topPanel;

  public ModulePanel(Type maxLevel, String moduleName, String userAgent,
      String remoteSocket, final JTabbedPane tabs) {
    super(new BorderLayout());
    this.tabs = tabs;
    topPanel = new JPanel();
    topPanel.add(new JLabel(moduleName));
    JButton compileButton = new JButton("Compile (not yet implemented)");
    compileButton.setEnabled(false);
    compileButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        JOptionPane.showMessageDialog(null, "Compiling not implemented yet", 
            "Alert: Not Implemented", JOptionPane.INFORMATION_MESSAGE);
      }
    });
    topPanel.add(compileButton);
    add(topPanel, BorderLayout.NORTH);
    loggerPanel = new SwingLoggerPanel(maxLevel);
    add(loggerPanel);
    AbstractTreeLogger logger = loggerPanel.getLogger();
    ImageIcon browserIcon = null;
    String lcAgent = userAgent.toLowerCase();
    if (lcAgent.contains("msie")) {
      browserIcon = ieIcon;
    } else if (lcAgent.contains("webkit") || lcAgent.contains("safari")) {
      browserIcon = safariIcon;
    } else if (lcAgent.contains("firefox")) {
      browserIcon = firefoxIcon;
    }
    String shortModuleName = moduleName;
    int lastDot = shortModuleName.lastIndexOf('.');
    if (lastDot >= 0) {
      shortModuleName = shortModuleName.substring(lastDot + 1);
    }
    synchronized (tabs) {
      tabs.addTab(shortModuleName, browserIcon, this, moduleName + " from "
          + remoteSocket + " on " + userAgent);
    }
    TreeLogger branch = logger.branch(TreeLogger.INFO, "Request for module "
        + moduleName);
    branch.log(TreeLogger.INFO, "User agent: " + userAgent);
    branch.log(TreeLogger.INFO, "Remote host: " + remoteSocket);
  }

  public void disconnect() {
    topPanel.add(new ClosedTabComponent());
    synchronized (tabs) {
      int index = tabs.indexOfComponent(this);
      if (index > -1) {
        tabs.setTitleAt(index, "Disconnected");
        tabs.setIconAt(index, null);
      }
    }
    loggerPanel.disconnected();
  }

  public AbstractTreeLogger getLogger() {
    return loggerPanel.getLogger();
  }
}
