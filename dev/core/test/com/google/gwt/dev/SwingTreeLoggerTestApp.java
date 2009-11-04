/**
 * 
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
