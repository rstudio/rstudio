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
package com.google.gwt.dev;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.shell.WrapLayout;
import com.google.gwt.dev.shell.log.SwingLoggerPanel;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JPanel;

/**
 */
public class WebServerPanel extends JPanel {

  /**
   * Callback interface for when the server should be restarted.
   */
  public interface RestartAction {
    void restartServer(TreeLogger logger);
  }

  private SwingLoggerPanel logWindow;

  public WebServerPanel(int serverPort, TreeLogger.Type maxLevel,
      File logFile) {
    this(serverPort, maxLevel, logFile, null);
  }

  /**
   * @param serverPort the server port number
   */
  public WebServerPanel(int serverPort, TreeLogger.Type maxLevel,
      File logFile, final RestartAction restartServerAction) {
    super(new BorderLayout());
    logWindow = new SwingLoggerPanel(maxLevel, logFile);
    if (restartServerAction != null) {
      JPanel panel = new JPanel(new WrapLayout());
      JButton restartButton = new JButton("Restart Server");
      restartButton.setMnemonic(KeyEvent.VK_R);
      restartButton.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          restartServerAction.restartServer(getLogger());
        }
      });
      panel.add(restartButton);
      add(panel, BorderLayout.NORTH);
    }
    add(logWindow);
  }

  public TreeLogger getLogger() {
    return logWindow.getLogger();
  }
}
