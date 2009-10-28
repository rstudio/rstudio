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
package com.google.gwt.dev.shell;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.shell.log.SwingLoggerPanel;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 */
@SuppressWarnings("deprecation")
public class ShellMainWindow extends JPanel {

  private SwingLoggerPanel logWindow;

  public ShellMainWindow(TreeLogger.Type maxLevel, File logFile) {
    super(new BorderLayout());
    // TODO(jat): add back when we have real options
    if (false) {
      JPanel panel = new JPanel(new GridLayout(2, 1));
      JPanel optionPanel = new JPanel();
      optionPanel.setBorder(BorderFactory.createTitledBorder("Options"));
      optionPanel.add(new JLabel("Miscellaneous options here"));
      panel.add(optionPanel);
      JPanel launchPanel = new JPanel();
      launchPanel.setBorder(BorderFactory.createTitledBorder("Launch GWT Module"));
      launchPanel.add(new JLabel(
          "Selections for launching a new module on a selected browser"));
      panel.add(launchPanel);
      add(panel, BorderLayout.NORTH);
    }
    logWindow = new SwingLoggerPanel(maxLevel, logFile);
    add(logWindow);
  }

  /**
   * @return TreeLogger instance
   */
  public TreeLogger getLogger() {
    return logWindow.getLogger();
  }
}
