/*
 * Copyright 2009 Google Inc.
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
import com.google.gwt.dev.shell.log.SwingLoggerPanel;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

/**
 * Test app to visually inspect SwingTreeLogger's behavior.
 */
public class SwingTreeLoggerTestApp {

  /**
   * @param args ignored
   */
  public static void main(String[] args) {
    JFrame frame = new JFrame("SwingTreeLogger test");
    SwingLoggerPanel loggerPanel = new SwingLoggerPanel(TreeLogger.INFO, null);
    frame.getContentPane().add(loggerPanel);
    frame.setSize(950, 700);
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setVisible(true);
    TreeLogger logger = loggerPanel.getLogger();
    logger.log(TreeLogger.INFO, "info 1");
    TreeLogger branch = logger.branch(TreeLogger.INFO, "info branch");
    branch.log(TreeLogger.DEBUG, "debug 1");
    branch.log(TreeLogger.ERROR, "error 1");
    TreeLogger dbgBranch = logger.branch(TreeLogger.DEBUG, "debug branch");
    dbgBranch.log(TreeLogger.SPAM, "spam 1");
    dbgBranch.log(TreeLogger.WARN, "warn 1");
    logger.log(TreeLogger.INFO, "info 2");
  }
}
