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

import java.awt.Color;
import java.awt.EventQueue;
import java.net.URL;
import java.text.NumberFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * Tree logger built on an Swing tree item.
 */
public final class SwingTreeLogger extends AbstractTreeLogger {

  /**
   * Represents an individual log event.
   */
  public static class LogEvent {

    private static final Color DEBUG_COLOR = Color.decode("0x007777");
    private static final Date firstLog = new Date();
    private static final Map<Type, Color> logColors = new HashMap<Type, Color>();
    private static final Map<Type, Icon> logIcons = new HashMap<Type, Icon>();
    private static NumberFormat minHr = NumberFormat.getIntegerInstance();
    private static NumberFormat seconds = NumberFormat.getNumberInstance();
    private static final Color SPAM_COLOR = Color.decode("0x005500");
    private static final Color WARN_COLOR = Color.decode("0x888800");

    static {
      seconds.setMinimumFractionDigits(3);
      seconds.setMaximumFractionDigits(3);
      seconds.setMinimumIntegerDigits(2);
      minHr.setMinimumIntegerDigits(2);
      logColors.put(ERROR, Color.RED);
      logIcons.put(ERROR, Icons.getLogItemError());
      logColors.put(WARN, WARN_COLOR);
      logIcons.put(WARN, Icons.getLogItemWarning());
      logColors.put(INFO, Color.BLACK);
      logIcons.put(INFO, Icons.getLogItemInfo());
      logColors.put(TRACE, Color.DARK_GRAY);
      logIcons.put(TRACE, Icons.getLogItemTrace());
      logColors.put(DEBUG, DEBUG_COLOR);
      logIcons.put(DEBUG, Icons.getLogItemDebug());
      logColors.put(SPAM, SPAM_COLOR);
      logIcons.put(SPAM, Icons.getLogItemSpam());
    }

    /**
     * Logger for this event.
     */
    public final SwingTreeLogger childLogger;

    /**
     * Detail message for the exception (ie, the stack trace).
     */
    public final String exceptionDetail;

    /**
     * The name of the exception, or null if none.
     */
    public final String exceptionName;

    /**
     * Extra info for this message, or null if none.
     */
    public final HelpInfo helpInfo;

    /**
     * Index within the parent logger.
     */
    public final int index;

    /**
     * True if this is a branch commit.
     */
    public final boolean isBranchCommit;

    /**
     * Log message.
     */
    public final String message;

    /**
     * Timestamp of when the message was logged.
     */
    public final Date timestamp;

    /**
     * Log level of this message.
     */
    public final TreeLogger.Type type;

    /**
     * Maintains the highest priority of any child events.
     */
    private Type inheritedPriority;

    /**
     * Create a log event.
     *
     * @param logger
     * @param isBranchCommit
     * @param index
     * @param type
     * @param message
     * @param caught
     * @param helpInfo
     */
    public LogEvent(SwingTreeLogger logger, boolean isBranchCommit, int index,
        Type type, String message, Throwable caught, HelpInfo helpInfo) {
      this.childLogger = logger;
      this.isBranchCommit = isBranchCommit;
      this.index = index;
      this.type = type;
      this.inheritedPriority = type;
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
        String anchorText = helpInfo.getAnchorText();
        if (anchorText == null && url != null) {
          anchorText = url.toExternalForm();
        }
        String prefix = helpInfo.getPrefix();
        if (url != null) {
          sb.append("<p>" + prefix + "<a href=\"");
          sb.append(url.toString());
          sb.append("\">");
          sb.append(anchorText);
          sb.append("</a>");
          sb.append("\n");
        }
      }
      return sb.toString();
    }

    /**
     * @return the inherited priority, which will be the highest priority of
     *         this event or any child.
     */
    public Type getInheritedPriority() {
      return inheritedPriority;
    }

    /**
     * Set the properties of a label to match this log entry.
     *
     * @param treeLabel label to set properties for.
     */
    public void setDisplayProperties(JLabel treeLabel) {
      Icon image = logIcons.get(type);
      Color color = logColors.get(inheritedPriority);
      if (color == null) {
        color = Color.BLACK;
      }
      treeLabel.setForeground(color);
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

    /**
     * Update this log event's inherited priority, which is the highest priority
     * of this event and any child events.
     *
     * @param childPriority
     * @return true if the priority was upgraded
     */
    public boolean updateInheritedPriority(Type childPriority) {
      if (this.inheritedPriority.isLowerPriorityThan(childPriority)) {
        this.inheritedPriority = childPriority;
        return true;
      }
      return false;
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
  final DefaultMutableTreeNode treeNode;

  private SwingLoggerPanel panel;

  /**
   * Constructs the top-level TreeItemLogger.
   *
   * @param panel
   */
  public SwingTreeLogger(SwingLoggerPanel panel) {
    this(panel, (DefaultMutableTreeNode) panel.treeModel.getRoot());
  }

  /**
   * Used to create a branch treelogger, supplying a tree node to use rather
   * than the panel's.
   *
   * @param panel
   * @param treeNode
   */
  private SwingTreeLogger(SwingLoggerPanel panel,
      DefaultMutableTreeNode treeNode) {
    this.panel = panel;
    this.treeNode = treeNode;
  }

  @Override
  protected AbstractTreeLogger doBranch() {
    SwingTreeLogger newLogger = new SwingTreeLogger(panel,
        new DefaultMutableTreeNode(null));
    return newLogger;
  }

  @Override
  protected void doCommitBranch(AbstractTreeLogger childBeingCommitted,
      Type type, String msg, Throwable caught, HelpInfo helpInfo) {
    SwingTreeLogger commitChild = (SwingTreeLogger) childBeingCommitted;
    assert commitChild.treeNode.getUserObject() == null;
    addUpdate(new LogEvent(commitChild, true, commitChild.getBranchedIndex(),
        type, msg, caught, helpInfo));
  }

  @Override
  protected void doLog(int index, Type type, String msg, Throwable caught,
      HelpInfo helpInfo) {
    addUpdate(new LogEvent(this, false, index, type, msg, caught, helpInfo));
  }

  /**
   * Add a log event to be processed on the event thread.
   *
   * @param logEvent LogEvent to process
   */
  private void addUpdate(final LogEvent logEvent) {
    // TODO(jat): investigate not running all of this on the event thread
    EventQueue.invokeLater(new Runnable() {
      @Override
      public void run() {
        // TODO(jat): apply filter criteria
        SwingTreeLogger logger = logEvent.childLogger;
        DefaultMutableTreeNode node;
        DefaultMutableTreeNode parentNode;
        int idx;
        if (logEvent.isBranchCommit) {
          SwingTreeLogger parentLogger = (SwingTreeLogger) logger.getParentLogger();
          logger.treeNode.setUserObject(logEvent);
          parentNode = parentLogger.treeNode;
          idx = logger.getBranchedIndex();
          node = logger.treeNode;
        } else {
          parentNode = logger.treeNode;
          idx = logEvent.index;
          node = new DefaultMutableTreeNode(logEvent);
        }
        int insertIndex = findInsertionPoint(parentNode, idx);
        panel.treeModel.insertNodeInto(node, parentNode, insertIndex);
        if (parentNode == panel.treeModel.getRoot()
            && parentNode.getChildCount() == 1) {
          panel.treeModel.reload();
        }
        // Propagate our priority to our ancestors
        Type priority = logEvent.getInheritedPriority();
        while (parentNode != panel.treeModel.getRoot()) {
          LogEvent parentEvent = (LogEvent) parentNode.getUserObject();
          if (!parentEvent.updateInheritedPriority(priority)) {
            break;
          }
          parentNode = ((DefaultMutableTreeNode) parentNode.getParent());
        }
      }

      private int findInsertionPoint(DefaultMutableTreeNode parentNode,
          int index) {
        int high = parentNode.getChildCount() - 1;
        if (high < 0) {
          return 0;
        }
        int low = 0;
        while (low <= high) {
          final int mid = low + ((high - low) >> 1);
          DefaultMutableTreeNode midChild = (DefaultMutableTreeNode) parentNode.getChildAt(mid);
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
    });
  }
}
