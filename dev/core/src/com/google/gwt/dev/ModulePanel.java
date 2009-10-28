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
import com.google.gwt.dev.ModuleTabPanel.Session;
import com.google.gwt.dev.shell.log.SwingLoggerPanel;
import com.google.gwt.dev.shell.log.SwingLoggerPanel.CloseHandler;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * A panel which represents a single module session.
 */
public class ModulePanel extends JPanel implements Disconnectable {

  private SwingLoggerPanel loggerPanel;

  private Session session;
  
  private boolean disconnected;

  public ModulePanel(Type maxLevel, String moduleName,
      Session session, File logFile) {
    super(new BorderLayout());
    this.session = session;
    if (false) {
      JPanel topPanel = new JPanel();
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
    }
    loggerPanel = new SwingLoggerPanel(maxLevel, logFile);
    add(loggerPanel);
    session.addModule(moduleName, this);
  }

  /* (non-Javadoc)
   * @see com.google.gwt.dev.Disconnectable#disconnect()
   */
  public void disconnect() {
    setDisconnected();
  }

  public TreeLogger getLogger() {
    return loggerPanel.getLogger();
  }

  /* (non-Javadoc)
   * @see com.google.gwt.dev.Disconnectable#isDisconnected()
   */
  public boolean isDisconnected() {
    return disconnected;
  }
  
  /**
   * Called by ModuleTabPanel when the user forces the close.
   */
  void setDisconnected() {
    disconnected = true;
    loggerPanel.disconnected();
    // TODO(jat): allow closing open connections once we do away with SWT
    loggerPanel.setCloseHandler(new CloseHandler() {
      public void onCloseRequest(SwingLoggerPanel loggerPanelToClose) {
        session.disconnectModule(ModulePanel.this);
      }
    });
  }
}
