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

import com.google.gwt.core.ext.TreeLogger.HelpInfo;
import com.google.gwt.dev.shell.BrowserWidget;
import com.google.gwt.dev.shell.log.TreeItemLogger.LogEvent;
import com.google.gwt.dev.util.log.AbstractTreeLogger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TreeEvent;
import org.eclipse.swt.events.TreeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;

/**
 * SWT widget containing a tree logger.
 */
public class TreeLoggerWidget extends Composite implements TreeListener,
    SelectionListener {

  private boolean autoScroll;

  private final Text details;

  private final TreeItemLogger logger;

  private final Tree tree;

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
    tree.addMouseListener(new MouseAdapter() {
      public void mouseDoubleClick(MouseEvent e) {
        openHelpOnSelection(tree);
      }
    });
    tree.setFocus();
    tree.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        switch (e.keyCode) {
          case 'c':
            if (e.stateMask == SWT.CTRL) {
              copyTreeSelectionToClipboard(tree);
            }
            break;
          case '\r':
          case '\n':
            openHelpOnSelection(tree);
        }
      }
    });

    logger = new TreeItemLogger();

    // The detail
    details = new Text(sash, SWT.MULTI | SWT.WRAP | SWT.READ_ONLY | SWT.BORDER
        | SWT.V_SCROLL);
    final Color detailsBgColor = new Color(null, 255, 255, 255);
    details.setBackground(detailsBgColor);
    details.addDisposeListener(new DisposeListener() {
      public void widgetDisposed(DisposeEvent arg0) {
        detailsBgColor.dispose();
      }
    });

    sash.setWeights(new int[] {80, 20});

    initLogFlushTimer(parent.getDisplay());
  }

  public void collapseAll() {
    TreeItem[] items = tree.getItems();
    for (int i = 0, n = items.length; i < n; ++i) {
      TreeItem item = items[i];
      collapseAll(item);
    }
    if (items.length > 0) {
      tree.setSelection(new TreeItem[] {items[0]});
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
      tree.setSelection(new TreeItem[] {items[0]});
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

  public boolean getAutoScroll() {
    return autoScroll;
  }

  public AbstractTreeLogger getLogger() {
    return logger;
  }

  public void removeAll() {
    tree.removeAll();
    details.setText("");
  }

  public void setAutoScroll(boolean autoScroll) {
    this.autoScroll = autoScroll;
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

  protected void appendTreeItemText(PrintWriter result, TreeItem[] items,
      int depth) {
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

    final Object[] cbText = new Object[] {sw.toString()};
    final Transfer[] cbFormat = new Transfer[] {TextTransfer.getInstance()};
    cb.setContents(cbText, cbFormat);
  }

  protected void openHelpOnSelection(Tree tree) {
    TreeItem[] selected = tree.getSelection();
    for (TreeItem item : selected) {
      Object itemData = item.getData();
      if (itemData instanceof HelpInfo) {
        HelpInfo helpInfo = (HelpInfo) itemData;
        URL url = helpInfo.getURL();
        if (url != null) {
          BrowserWidget.launchExternalBrowser(logger, url.toString());
        }
      }
    }
  }

  private void initLogFlushTimer(final Display display) {
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
              tree.setSelection(new TreeItem[] {lastItem});
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
    if (logEvent != null && logEvent.exceptionDetail != null) {
      sb.append(logEvent.exceptionDetail);
    }

    details.setText(sb.toString());
  }
}
