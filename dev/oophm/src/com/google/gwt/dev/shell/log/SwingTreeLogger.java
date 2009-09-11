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
import com.google.gwt.dev.shell.Icons;
import com.google.gwt.dev.util.log.AbstractTreeLogger;

import org.jdesktop.swingworker.SwingWorker;

import java.awt.Color;
import java.net.URL;
import java.text.NumberFormat;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

/**
 * Tree logger built on an Swing tree item.
 */
public final class SwingTreeLogger extends AbstractTreeLogger {

  /**
   * Represents an individual log event.
   */
  public static class LogEvent {

    private static final Color DEBUG_COLOR = Color.decode("0x007777");
    private static final Color SPAM_COLOR = Color.decode("0x005500");
    private static final Color WARN_COLOR = Color.decode("0x888800");

    private static final Date firstLog = new Date();
    private static NumberFormat minHr = NumberFormat.getIntegerInstance();
    private static NumberFormat seconds = NumberFormat.getNumberInstance();

    static {
      seconds.setMinimumFractionDigits(3);
      seconds.setMaximumFractionDigits(3);
      seconds.setMinimumIntegerDigits(2);
      minHr.setMinimumIntegerDigits(2);
    }

    public final SwingTreeLogger childLogger;

    public final String exceptionDetail;

    public final String exceptionName;

    public final HelpInfo helpInfo;

    public final int index;

    public final boolean isBranchCommit;

    public final String message;

    public final Date timestamp;

    public final TreeLogger.Type type;

    public LogEvent(SwingTreeLogger logger, boolean isBranchCommit, int index,
        Type type, String message, Throwable caught, HelpInfo helpInfo) {
      this.childLogger = logger;
      this.isBranchCommit = isBranchCommit;
      this.index = index;
      this.type = type;
      this.message = message;
      this.helpInfo = helpInfo;
      this.timestamp = new Date();
      this.exceptionDetail = AbstractTreeLogger.getStackTraceAsString(caught);
      this.exceptionName = AbstractTreeLogger.getExceptionName(caught);
    }

    /**
     * @return full text of log event.
     */
    public String getFullText() {
      StringBuffer sb = new StringBuffer();

      formatTimestamp(timestamp.getTime() - firstLog.getTime(), sb);
      sb.append("  ");

      // Show the message type.
      //
      if (type != null) {
        sb.append("[");
        sb.append(type.getLabel());
        sb.append("] ");
      }

      // Show the item text.
      //
      sb.append(htmlEscape(message));
      sb.append("\n");

      // Show the exception info for anything other than "UnableToComplete".
      //
      if (exceptionDetail != null) {
        sb.append("<pre>" + htmlEscape(exceptionDetail) + "</pre>");
      }
      if (helpInfo != null) {
        URL url = helpInfo.getURL();
        if (url != null) {
          sb.append("\nMore info: <a href=\"");
          sb.append(url.toString());
          sb.append("\">");
          sb.append(url.toString());
          sb.append("</a>");
          sb.append("\n");
        }
      }
      return sb.toString();
    }

    /**
     * Set the properties of a label to match this log entry.
     * 
     * @param treeLabel label to set properties for.
     */
    public void setDisplayProperties(JLabel treeLabel) {
      Icon image = null;
      if (type == TreeLogger.ERROR) {
        treeLabel.setForeground(Color.RED);
        image = Icons.getLogItemError();
      } else if (type == TreeLogger.WARN) {
        treeLabel.setForeground(WARN_COLOR);
        image = Icons.getLogItemWarning();
      } else if (type == TreeLogger.INFO) {
        treeLabel.setForeground(Color.BLACK);
        image = Icons.getLogItemInfo();
      } else if (type == TreeLogger.TRACE) {
        treeLabel.setForeground(Color.DARK_GRAY);
        image = Icons.getLogItemTrace();
      } else if (type == TreeLogger.DEBUG) {
        treeLabel.setForeground(DEBUG_COLOR);
        image = Icons.getLogItemDebug();
      } else if (type == TreeLogger.SPAM) {
        treeLabel.setForeground(SPAM_COLOR);
        image = Icons.getLogItemSpam();
      } else {
        // Messages without icons, ie ALL
        treeLabel.setForeground(Color.BLACK);
      }
      treeLabel.setIcon(image);
      StringBuffer sb = new StringBuffer();

      formatTimestamp(timestamp.getTime() - firstLog.getTime(), sb);
      sb.append("  ");

      // Show the message type.
      //
      if (type != null) {
        sb.append("[");
        sb.append(type.getLabel());
        sb.append("] ");
      }

      // Show the item text.
      //
      sb.append(message);

      // Show the exception info for anything other than "UnableToComplete".
      //
      if (exceptionName != null) {
        sb.append(" -- exception: " + exceptionName);
      }
      treeLabel.setText(sb.toString());
    }

    @Override
    public String toString() {
      String s = "";
      s += "[logger " + childLogger.toString();
      s += ", " + (isBranchCommit ? "BRANCH" : "LOG");
      s += ", index " + index;
      s += ", type " + type.toString();
      s += ", msg '" + message + "'";
      s += "]";
      return s;
    }

    private void formatTimestamp(long ts, StringBuffer sb) {
      sb.append(minHr.format(ts / (1000 * 60 * 60)));
      sb.append(':');
      sb.append(minHr.format((ts / (1000 * 60)) % 60));
      sb.append(':');
      sb.append(seconds.format((ts % 60000) / 1000.0));
    }

    private String htmlEscape(String str) {
      // TODO(jat): call real implementation instead
      return str.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace(
          "\n", "<br>");
    }
  }

  // package protected so SwingLoggerPanel can access
  DefaultMutableTreeNode treeNode;

  private SwingLoggerPanel panel;

  /**
   * Constructs the top-level TreeItemLogger.
   * 
   * @param panel
   */
  public SwingTreeLogger(SwingLoggerPanel panel) {
    this.panel = panel;
    // treeNode gets replaced for branches in doCommitBranch
    treeNode = (DefaultMutableTreeNode) panel.treeModel.getRoot();
  }

  @Override
  protected AbstractTreeLogger doBranch() {
    SwingTreeLogger newLogger = new SwingTreeLogger(panel);
    return newLogger;
  }

  @Override
  protected void doCommitBranch(AbstractTreeLogger childBeingCommitted,
      Type type, String msg, Throwable caught, HelpInfo helpInfo) {
    SwingTreeLogger commitChild = (SwingTreeLogger) childBeingCommitted;
    LogEvent logEvent = new LogEvent(commitChild, true,
        commitChild.getBranchedIndex(), type, msg, caught, helpInfo);
    commitChild.treeNode = new DefaultMutableTreeNode(logEvent);
    addUpdate(logEvent);
  }

  @Override
  protected void doLog(int index, Type type, String msg, Throwable caught,
      HelpInfo helpInfo) {
    addUpdate(new LogEvent(this, false, index, type, msg, caught, helpInfo));
  }

  /**
   * @param logEvent
   */
  private void addUpdate(final LogEvent logEvent) {
    new SwingWorker<LogEvent, Void>() {
      @Override
      protected LogEvent doInBackground() throws Exception {
        return logEvent;
      }

      @Override
      protected void done() {
        LogEvent event;
        try {
          // TODO(jat): apply filter criteria
          event = get();
          // TODO(jat): do more of this work in doInBackground()?
          SwingTreeLogger logger = event.childLogger;
          DefaultMutableTreeNode node;
          DefaultMutableTreeNode parent;
          int idx;
          if (event.isBranchCommit) {
            SwingTreeLogger parentLogger = (SwingTreeLogger) logger.getParentLogger();
            parent = parentLogger.treeNode;
            idx = logger.indexWithinMyParent;
            node = logger.treeNode;
          } else {
            parent = logger.treeNode;
            idx = event.index;
            node = new DefaultMutableTreeNode(event);
          }
          int insertIndex = findInsertionPoint(parent, idx);
          panel.treeModel.insertNodeInto(node, parent, insertIndex);
          if (parent == panel.treeModel.getRoot()
              && parent.getChildCount() == 1) {
            panel.treeModel.reload();
          }
          if (event.type.needsAttention()) {
            panel.tree.makeVisible(new TreePath(node.getPath()));
          }
        } catch (InterruptedException e) {
          // TODO(jat): Auto-generated catch block
          e.printStackTrace();
        } catch (ExecutionException e) {
          // TODO(jat): Auto-generated catch block
          e.printStackTrace();
        }
      }

      private int findInsertionPoint(DefaultMutableTreeNode parent, int index) {
        int high = parent.getChildCount() - 1;
        if (high < 0) {
          return 0;
        }
        int low = 0;
        while (low <= high) {
          final int mid = low + ((high - low) >> 1);
          DefaultMutableTreeNode midChild = (DefaultMutableTreeNode) parent.getChildAt(mid);
          final Object userObject = midChild.getUserObject();
          int compIdx = -1;
          if (userObject instanceof LogEvent) {
            LogEvent event = (LogEvent) userObject;
            compIdx = event.index;
          }
          if (compIdx < index) {
            low = mid + 1;
          } else if (compIdx > index) {
            high = mid - 1;
          } else {
            return mid;
          }
        }
        return low;
      }
    }.execute();
  }
}
