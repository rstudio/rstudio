/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.dev.util.log;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.shell.LowLevel;
import com.google.gwt.dev.util.log.TreeItemLogger.LogEvent;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TreeEvent;
import org.eclipse.swt.events.TreeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import java.io.PrintWriter;
import java.io.StringWriter;

public class TreeLoggerWidget extends Composite implements TreeListener, SelectionListener {

  private static AbstractTreeLogger singletonWindowLogger;

  /**
   * Useful for debugging, this method creates a standalone window in a background thread and
   * returns a logger you can use to write to it.
   */
  public static synchronized AbstractTreeLogger getAsDetachedWindow(final String caption,
      final int width, final int height, final boolean autoScroll) {

    if (singletonWindowLogger != null) {
      // Already set.
      //
      return singletonWindowLogger;
    }

    final AbstractTreeLogger[] loggerHolder = new AbstractTreeLogger[1];
    Thread logWindowThread = new Thread() {

      public void run() {
        // Create a shell.
        //
        Shell shell = new Shell(Display.getCurrent());
        shell.setText(caption);
        FillLayout fillLayout = new FillLayout();
        fillLayout.marginWidth = 0;
        fillLayout.marginHeight = 0;
        shell.setLayout(fillLayout);

        // Create a logger in it.
        //
        synchronized (loggerHolder) {
          final TreeLoggerWidget treeLoggerWidget = new TreeLoggerWidget(shell);
          treeLoggerWidget.setAutoScroll(autoScroll);
          loggerHolder[0] = treeLoggerWidget.getLogger();
        }

        // Set the shell's icon and size then show it.
        //
        shell.setImage(LowLevel.loadImage("gwt.ico"));
        shell.setSize(width, height);
        shell.open();

        // Pump the event loop until the shell is closed.
        //
        Display display = Display.getCurrent();
        while (!shell.isDisposed()) {
          try {
            if (!display.readAndDispatch()) {
              display.sleep();
            }
          } catch (Throwable e) {
            // Ignored -- this method is only intended for test harnesses.
          }
        }
      }
    };

    // Start the thread and wait until the loggerHolder has something.
    //
    logWindowThread.setName("TreeLogger Window");
    logWindowThread.start();
    while (singletonWindowLogger == null) {
      Thread.yield();
      if (logWindowThread.isAlive()) {
        synchronized (loggerHolder) {
          singletonWindowLogger = loggerHolder[0];
        }
      } else {
        throw new IllegalStateException("Log window thread died unexpectedly");
      }
    }

    return singletonWindowLogger;
  }

  public TreeLoggerWidget(Composite parent) {
    super(parent, SWT.NONE);

    setLayout(new FillLayout());

    // The sash (aka "splitter").
    //
    SashForm sash = new SashForm(this, SWT.VERTICAL);

    // The tree.
    //
    tree = new Tree(sash, SWT.BORDER | SWT.SHADOW_IN);
    tree.setLinesVisible(false);
    tree.addSelectionListener(this);
    tree.setFocus();
    tree.addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent e) {
        if (e.keyCode == 'c' && e.stateMask == SWT.CTRL) {
          // Copy subtree to clipboard.
          //
          copyTreeSelectionToClipboard(tree);
        }
      }
    });

    logger = new TreeItemLogger();

    // The detail
    details = new Text(sash, SWT.MULTI | SWT.WRAP | SWT.READ_ONLY | SWT.BORDER | SWT.V_SCROLL);
    final Color detailsBgColor = new Color(null, 255, 255, 255);
    details.setBackground(detailsBgColor);
    details.addDisposeListener(new DisposeListener() {
      public void widgetDisposed(DisposeEvent arg0) {
        detailsBgColor.dispose();
      }
    });

    sash.setWeights(new int[]{80, 20});

    initLogFlushTimer(parent.getDisplay());
  }

  public void collapseAll() {
    TreeItem[] items = tree.getItems();
    for (int i = 0, n = items.length; i < n; ++i) {
      TreeItem item = items[i];
      collapseAll(item);
    }
    if (items.length > 0) {
      tree.setSelection(new TreeItem[]{items[0]});
    }
  }

  public void collapseAll(TreeItem from) {
    TreeItem[] items = from.getItems();
    for (int i = 0, n = items.length; i < n; ++i) {
      TreeItem item = items[i];
      collapseAll(item);
    }
    from.setExpanded(false);
  }

  public void expandAll() {
    TreeItem[] items = tree.getItems();
    for (int i = 0, n = items.length; i < n; ++i) {
      TreeItem item = items[i];
      expandAll(item);
    }
    if (items.length > 0) {
      tree.setSelection(new TreeItem[]{items[0]});
    }
  }

  public void expandAll(TreeItem from) {
    from.setExpanded(true);
    TreeItem[] items = from.getItems();
    for (int i = 0, n = items.length; i < n; ++i) {
      TreeItem item = items[i];
      expandAll(item);
    }
  }

  public AbstractTreeLogger getLogger() {
    return logger;
  }

  public void removeAll() {
    tree.removeAll();
    details.setText("");
  }

  public synchronized void treeCollapsed(TreeEvent treeEvent) {
  }

  public synchronized void treeExpanded(TreeEvent treeEvent) {
  }

  public void widgetDefaultSelected(SelectionEvent event) {
  }

  public void widgetSelected(SelectionEvent event) {
    syncDetailsPane((TreeItem) event.item);
  }

  private void syncDetailsPane(TreeItem item) {
    // Try to get a LogEvent from the item's custom data.
    //
    TreeItemLogger.LogEvent logEvent = null;
    Object testLogEvent = item.getData();
    if (testLogEvent instanceof TreeItemLogger.LogEvent) {
      logEvent = (LogEvent) testLogEvent;
    }

    // Build a detail string.
    //
    StringBuffer sb = new StringBuffer();

    // Show the message type.
    //
    if (logEvent != null && logEvent.type != null) {
      sb.append("[");
      sb.append(logEvent.type.getLabel());
      sb.append("] ");
    }

    // Show the item text.
    //
    sb.append(item.getText());
    sb.append("\n");

    // Show the exception info for anything other than "UnableToComplete".
    //
    if (logEvent != null && logEvent.caught != null) {
      if (!(logEvent.caught instanceof UnableToCompleteException)) {
        String stackTrace = AbstractTreeLogger.getStackTraceAsString(logEvent.caught);
        sb.append(stackTrace);
      }
    }

    details.setText(sb.toString());
  }

  protected void appendTreeItemText(PrintWriter result, TreeItem[] items, int depth) {
    for (int i = 0; i < items.length; i++) {
      TreeItem item = items[i];
      for (int j = 0; j < depth; j++) {
        result.print("   ");
      }
      result.println(item.getText());
      TreeItem[] children = item.getItems();
      if (children != null) {
        appendTreeItemText(result, children, depth + 1);
      }
    }
  }

  protected void copyTreeSelectionToClipboard(Tree tree) {
    TreeItem[] selected = tree.getSelection();
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw, false);
    if (selected != null) {
      appendTreeItemText(pw, selected, 0);
    }
    pw.close();
    Clipboard cb = new Clipboard(tree.getDisplay());

    final Object[] cbText = new Object[]{sw.toString()};
    final Transfer[] cbFormat = new Transfer[]{TextTransfer.getInstance()};
    cb.setContents(cbText, cbFormat);
  }

  private final void initLogFlushTimer(final Display display) {
    final int flushDelay = 1000;

    display.timerExec(flushDelay, new Runnable() {
      public void run() {
        if (tree.isDisposed()) {
          return;
        }

        if (logger.uiFlush(tree)) {
          // Sync to the end of the tree.
          //
          if (autoScroll) {
            TreeItem lastItem = findLastVisibleItem(tree);
            if (lastItem != null) {
              tree.setSelection(new TreeItem[]{lastItem});
              tree.showItem(lastItem);
              expandAllChildren(lastItem);
              syncDetailsPane(lastItem);
            }
          }
        }

        display.timerExec(flushDelay, this);
      }

      private void expandAllChildren(TreeItem item) {
        item.setExpanded(true);
        for (int i = 0, n = item.getItemCount(); i < n; ++i) {
          expandAllChildren(item.getItem(i));
        }
      }

      private TreeItem findLastVisibleItem(Tree tree) {
        int n = tree.getItemCount() - 1;
        if (n > 0) {
          TreeItem item = tree.getItem(n);
          if (item.getExpanded()) {
            return findLastVisibleItem(item);
          } else {
            return item;
          }
        } else {
          return null;
        }
      }

      private TreeItem findLastVisibleItem(TreeItem item) {
        int n = item.getItemCount() - 1;
        if (n > 0) {
          TreeItem child = item.getItem(n);
          if (child.getExpanded()) {
            return findLastVisibleItem(child);
          }
        }
        return item;
      }
    });
  }

  public void setAutoScroll(boolean autoScroll) {
    this.autoScroll = autoScroll;
  }

  public boolean getAutoScroll() {
    return autoScroll;
  }

  private boolean autoScroll;
  private final Text details;
  private final TreeItemLogger logger;
  private final Tree tree;
}
