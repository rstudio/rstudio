/*
 * Copyright 2007 Google Inc.
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
import com.google.gwt.dev.util.log.AbstractTreeLogger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

/**
 * Tree logger built on an SWT tree item.
 */
public final class TreeItemLogger extends AbstractTreeLogger {

  /**
   * Represents an individual log event.
   */
  public static class LogEvent {
    public final String exceptionDetail;

    public final String exceptionName;

    public final HelpInfo helpInfo;

    public final int index;

    public final boolean isBranchCommit;

    public final TreeItemLogger logger;

    public final String message;

    public final TreeLogger.Type type;

    public LogEvent(TreeItemLogger logger, boolean isBranchCommit, int index,
        Type type, String message, Throwable caught, HelpInfo helpInfo) {
      this.exceptionDetail = AbstractTreeLogger.getStackTraceAsString(caught);
      this.exceptionName = AbstractTreeLogger.getExceptionName(caught);
      this.logger = logger;
      this.isBranchCommit = isBranchCommit;
      this.index = index;
      this.type = type;
      this.message = message;
      this.helpInfo = helpInfo;
    }

    @Override
    public String toString() {
      String s = "";
      s += "[logger " + logger.toString();
      s += ", " + (isBranchCommit ? "BRANCH" : "LOG");
      s += ", index " + index;
      s += ", type " + type.toString();
      s += ", msg '" + message + "'";
      s += "]";
      return s;
    }

    /**
     * Can only be called by the UI thread.
     */
    public void uiFlush(Tree tree) {
      // Get or create the tree item associated with this logger.
      //
      TreeItem treeItem = createTreeItem(tree);

      if (treeItem == null) {
        // The logger associated with this log event is dead, so it should
        // show no ui at all.
        //
        return;
      }

      // Style all ancestors.
      //
      uiStyleChildAndAncestors(type, treeItem);
    }

    /**
     * Creates a tree item in a way that is sensitive to the log event and its
     * position in the tree.
     */
    private TreeItem createTreeItem(Tree tree) {
      TreeItem treeItem = null;

      if (isBranchCommit) {
        // A child logger is committing.
        // 'logger' is the logger that needs a tree item.
        // We can be sure that the child logger's parent is non-null
        // and that either (1) it is has a TreeItem meaning it is not a
        // top-level entry or (2) it is a top-level entry and gets attached to
        // the Tree.
        //
        TreeItemLogger parentLogger = (TreeItemLogger) logger.getParentLogger();
        if (parentLogger.lazyTreeItem == null) {
          // Is top level.
          //
          treeItem = new TreeItem(tree, SWT.NONE);
          logger.lazyTreeItem = treeItem;
        } else if (!parentLogger.lazyTreeItem.isDisposed()) {
          // Is not top level, but still valid to write to.
          //
          treeItem = new TreeItem(parentLogger.lazyTreeItem, SWT.NONE);
          logger.lazyTreeItem = treeItem;
        } else {
          // The tree item associated with this logger's parent has been
          // disposed, so we simply ignore all pending updates related to it.
          // We also mark that logger dead to avoid adding log events for it.
          //
          parentLogger.markLoggerDead();
          return null;
        }
      } else {
        // Create a regular log item on 'logger'.
        // The logger may be the root logger, in which case we create TreeItems
        // directly underneath Tree, or it may be a branched logger, in which
        // case we create TreeItems underneath the branched logger's TreeItem
        // (which cannot be null because of the careful ordering of log events).
        //
        if (logger.lazyTreeItem == null) {
          // Is top level.
          //
          treeItem = new TreeItem(tree, SWT.NONE);
        } else if (!logger.lazyTreeItem.isDisposed()) {
          // Is not top level, but still valid to write to.
          //
          treeItem = new TreeItem(logger.lazyTreeItem, SWT.NONE);
        } else {
          // The tree item associated with this logger's parent has been
          // disposed, so we simply ignore all pending updates related to it.
          // We also mark that logger dead to avoid adding log events for it.
          //
          logger.markLoggerDead();
          return null;
        }
      }

      // Set the text of the new tree item.
      //
      String label = message;
      if (label == null) {
        assert (exceptionName != null);
        label = exceptionName;
      }
      treeItem.setText(label);

      if (helpInfo != null) {
        URL url = helpInfo.getURL();
        if (url != null) {
          TreeItem helpItem = new TreeItem(treeItem, SWT.NONE);
          helpItem.setImage(imageLink);
          helpItem.setText("More info: " + url.toString());
          helpItem.setForeground(helpItem.getDisplay().getSystemColor(
              SWT.COLOR_BLUE));
          helpItem.setData(helpInfo);
          treeItem.setExpanded(true);
        }
      }

      // This LogEvent object becomes the tree item's custom data.
      //
      treeItem.setData(this);

      return treeItem;
    }

    /**
     * Can only be called by the UI thread.
     */
    private void uiStyleChildAndAncestors(TreeLogger.Type type, TreeItem child) {
      Display display = child.getDisplay();
      Color color;

      Image image = null;
      if (type == TreeLogger.ERROR) {
        color = display.getSystemColor(SWT.COLOR_RED);
        image = imageError;
      } else if (type == TreeLogger.WARN) {
        color = display.getSystemColor(SWT.COLOR_DARK_YELLOW);
        image = imageWarning;
      } else if (type == TreeLogger.INFO) {
        color = display.getSystemColor(SWT.COLOR_BLACK);
        image = imageInfo;
      } else if (type == TreeLogger.TRACE) {
        color = display.getSystemColor(SWT.COLOR_DARK_GRAY);
        image = imageTrace;
      } else if (type == TreeLogger.DEBUG) {
        color = display.getSystemColor(SWT.COLOR_DARK_CYAN);
        image = imageDebug;
      } else {
        // if (type == TreeLogger.SPAM)
        color = display.getSystemColor(SWT.COLOR_DARK_GREEN);
        image = imageSpam;
      }

      if (image != null) {
        child.setImage(image);
      }

      // Set this item's color.
      //
      child.setForeground(color);

      // For types needing attention, set all parents to the warning color.
      //
      if (type.needsAttention()) {
        /*
         * Originally, this code would expand TreeItems from this child up to
         * its top level ancestor. However, on Mac and Linux, the TreeItems fail
         * to expand if its parent is not already expanded. It appears to be an
         * interaction between SWT and GTK, specifically bug
         * https://bugs.eclipse.org/bugs/show_bug.cgi?id=97757. The following
         * loop stores the ancestors and the second loop will set the expanded
         * attribute on from the top level ancestor down to this child.
         */
        Stack<TreeItem> parents = new Stack<TreeItem>();

        boolean propagateColor = true;
        TreeItem parent = child.getParentItem();
        while (parent != null) {
          parents.push(parent);

          LogEvent parentEvent = (LogEvent) parent.getData();
          if (propagateColor) {
            if (parentEvent.type.isLowerPriorityThan(type)) {
              parent.setForeground(color);
            } else {
              propagateColor = false;
            }
          }

          parent = parent.getParentItem();
        }

        while (!parents.isEmpty()) {
          parent = parents.pop();
          parent.setExpanded(true);
        }
      }
    }
  }
  /**
   * One object that is shared across all logger instances in the same tree.
   * This class is the synchronization choke point that prevents the ui thread
   * from flushing events while other threads are adding them, and it also
   * provides tree-wide shared objects such as log item images.
   */
  private static class PendingUpdates {
    private List<LogEvent> updates = new LinkedList<LogEvent>();

    private final Object updatesLock = new Object();

    public void add(LogEvent update) {
      synchronized (updatesLock) {
        updates.add(update);
      }
    }

    /**
     * Flushes any pending log entries.
     * 
     * @return <code>true</code> if any new entries were written
     */
    public synchronized boolean uiFlush(Tree tree) {
      // Move the list to flush into a local copy then release the udpate
      // lock so log events can keep coming in while we flush.
      //
      List<LogEvent> toFlush = null;
      synchronized (updatesLock) {
        if (updates.isEmpty()) {
          // Nothing to do.
          //
          return false;
        }
        toFlush = updates;
        updates = new LinkedList<LogEvent>();
      }

      for (Iterator<LogEvent> iter = toFlush.iterator(); iter.hasNext();) {
        LogEvent update = iter.next();
        // Loggers can be die while flushing, so we have to be sure never
        // to try to flush an entry to a dead logger.
        //
        if (!update.logger.isLoggerDead()) {
          update.uiFlush(tree);
        }
      }

      return true;
    }
  }

  // These don't get disposed, but they do last for the entire process, so
  // not a big deal.
  //
  private static final Image imageDebug = tryLoadImage("log-item-debug.gif");
  private static final Image imageError = tryLoadImage("log-item-error.gif");
  private static final Image imageInfo = tryLoadImage("log-item-info.gif");
  private static final Image imageLink = tryLoadImage("log-link.gif");
  private static final Image imageSpam = tryLoadImage("log-item-spam.gif");
  private static final Image imageTrace = tryLoadImage("log-item-trace.gif");
  private static final Image imageWarning = tryLoadImage("log-item-warning.gif");

  private static Image tryLoadImage(String simpleName) {
    InputStream is = TreeItemLogger.class.getResourceAsStream(simpleName);
    if (is != null) {
      try {
        Image image = new Image(null, is);
        return image;
      } finally {
        try {
          is.close();
        } catch (IOException e) {
        }
      }
    } else {
      // Bad image.
      //
      return null;
    }
  }

  private boolean dead;

  private TreeItem lazyTreeItem;

  private final PendingUpdates sharedPendingUpdates;

  /**
   * Constructs the top-level TreeItemLogger.
   */
  public TreeItemLogger() {
    sharedPendingUpdates = new PendingUpdates();
  }

  /**
   * Constructs an internal logger.
   */
  private TreeItemLogger(PendingUpdates sharedPendingUpdates) {
    // Inherit the one and only update list from my parent.
    this.sharedPendingUpdates = sharedPendingUpdates;
  }

  public void markLoggerDead() {
    // Cannot kill the root logger, even if attempted.
    //
    if (getParentLogger() != null) {
      dead = true;
    }
  }

  /**
   * Flushes log records to the UI; must only be called by the UI thread.
   * 
   * @return <code>true</code> if any new entries were written
   */
  public boolean uiFlush(Tree tree) {
    return sharedPendingUpdates.uiFlush(tree);
  }

  @Override
  protected AbstractTreeLogger doBranch() {
    return new TreeItemLogger(sharedPendingUpdates);
  }

  @Override
  protected void doCommitBranch(AbstractTreeLogger childBeingCommitted,
      Type type, String msg, Throwable caught, HelpInfo helpInfo) {
    if (isLoggerDead()) {
      return;
    }

    TreeItemLogger commitChild = (TreeItemLogger) childBeingCommitted;
    sharedPendingUpdates.add(new LogEvent(commitChild, true,
        commitChild.getBranchedIndex(), type, msg, caught, helpInfo));
  }

  @Override
  protected void doLog(int index, Type type, String msg, Throwable caught,
      HelpInfo helpInfo) {
    if (isLoggerDead()) {
      return;
    }

    sharedPendingUpdates.add(new LogEvent(this, false, index, type, msg,
        caught, helpInfo));
  }

  /**
   * Used for an extra check to avoid creating log events for dead loggers. A
   * dead logger is one that can no longer interact with the UI.
   */
  private boolean isLoggerDead() {
    // Deadness was cached.
    //
    if (dead) {
      return true;
    }

    // Check upward in the parent chain for any dead parent.
    //
    TreeItemLogger parentLogger = (TreeItemLogger) getParentLogger();
    if (parentLogger == null) {
      // This is the root logger, which cannot die.
      //
      return false;
    }

    // Otherwise, this logger is dead if its parent is dead (recursively).
    //
    if (parentLogger.isLoggerDead()) {
      // This logger is dead because my parent is dead.
      //
      markLoggerDead();
      return true;
    }

    // I'm not quite dead yet.
    //
    return false;
  }
}
