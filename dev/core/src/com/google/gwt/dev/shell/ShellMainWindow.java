// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.shell;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.GWTShell;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.log.AbstractTreeLogger;
import com.google.gwt.dev.util.log.TreeLoggerWidget;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolItem;

public class ShellMainWindow extends Composite implements DisposeListener,
    ShellListener {

  private class Toolbar extends HeaderBarBase {

    public Toolbar(Composite parent) {
      super(parent);

      fNewWindow = newItem("new-window.gif", "&Hosted Browser",
        "Opens a new hosted mode browser window for debugging");
      fNewWindow.addSelectionListener(new SelectionAdapter() {
        public void widgetSelected(SelectionEvent event) {
          String startupUrl = fServerWindow.normalizeURL("/");
          try {
            BrowserWidget bw = fServerWindow.openNewBrowserWindow();
            bw.go(startupUrl);
          } catch (UnableToCompleteException e) {
            getLogger().log(TreeLogger.ERROR,
              "Unable to open a new hosted browser window", e);
          }
        }
      });

      newSeparator();

      fCollapseAll = newItem("collapse.gif", "&Collapse All",
        "Collapses all log entries");
      fCollapseAll.addSelectionListener(new SelectionAdapter() {
        public void widgetSelected(SelectionEvent e) {
          fLogPane.collapseAll();
        }
      });

      fExpandAll = newItem("expand.gif", "&Expand All",
        "Expands all log entries");
      fExpandAll.addSelectionListener(new SelectionAdapter() {
        public void widgetSelected(SelectionEvent e) {
          fLogPane.expandAll();
        }
      });

      fClearLog = newItem("clear-log.gif", "Clear &Log",
        "Removes all log entries");
      fClearLog.addSelectionListener(new SelectionAdapter() {
        public void widgetSelected(SelectionEvent e) {
          fLogPane.removeAll();
        }
      });

      newSeparator();

      fAbout = newItem("about.gif", "    &About    ", "About...");
      fAbout.addSelectionListener(new SelectionAdapter() {
        public void widgetSelected(SelectionEvent e) {
          String aboutHtml = Util.getFileFromInstallPath("about.html");
          if (aboutHtml != null) {
            String serial = verify("TwysxNpVumPBvFyBoxzLy");
            StringBuffer sb = new StringBuffer();
            sb
              .append("<div style='overflow:hidden;width:100%;white-space:nowrap;font-size:1px'><br/><br/><br/><br/><font style='background-color:gray;color:lightgrey'>");
            for (int i = 0; i < 100; ++i)
              sb.append(serial);
            sb.append("</font></div>");
            serial = sb.toString();
            int pos;
            while ((pos = aboutHtml.indexOf("<hr/>")) >= 0) {
              aboutHtml = aboutHtml.substring(0, pos) + serial
                + aboutHtml.substring(pos + 5);
            }
            while ((pos = aboutHtml.indexOf("<body>")) >= 0) {
              aboutHtml = aboutHtml.substring(0, pos)
                + "<body oncontextmenu='return false'>"
                + aboutHtml.substring(pos + 6);
            }
          } else {
            aboutHtml = "Could not locate 'about.html' in installation directory.";
          }
          BrowserDialog browserDialog = new BrowserDialog(getShell(),
            getLogger(), aboutHtml);
          browserDialog.open(true);
        }
      });
    }

    private ToolItem fAbout;
    private ToolItem fClearLog;
    private ToolItem fCollapseAll;
    private ToolItem fExpandAll;
    private ToolItem fNewWindow;
  }

  public ShellMainWindow(GWTShell serverWindow, final Shell parent,
      int serverPort, boolean checkForUpdates) {
    super(parent, SWT.NONE);

    fServerWindow = serverWindow;

    fColorWhite = new Color(null, 255, 255, 255);

    addDisposeListener(this);
    parent.addShellListener(this);

    setLayout(new FillLayout());
    if (serverPort > 0) {
      parent.setText("Google Web Toolkit Development Shell / Port " + serverPort);
    } else {
      parent.setText("Google Web Toolkit Development Shell");
    }

    GridLayout gridLayout = new GridLayout(1, true);
    gridLayout.marginWidth = 0;
    gridLayout.marginHeight = 0;
    gridLayout.horizontalSpacing = 0;
    gridLayout.verticalSpacing = 0;
    setLayout(gridLayout);

    // Create the toolbar.
    //
    {
      fToolbar = new Toolbar(this);
      GridData data = new GridData();
      data.grabExcessHorizontalSpace = true;
      data.horizontalAlignment = GridData.FILL;
      fToolbar.setLayoutData(data);
    }

    // Create the log pane.
    //
    {
      fLogPane = new TreeLoggerWidget(this);
      GridData data = new GridData();
      data.grabExcessHorizontalSpace = true;
      data.grabExcessVerticalSpace = true;
      data.horizontalAlignment = GridData.FILL;
      data.verticalAlignment = GridData.FILL;
      fLogPane.setLayoutData(data);
    }

    // check for updates
    if (checkForUpdates) {
      try {
        final CheckForUpdates updateChecker = PlatformSpecific
          .createUpdateChecker();
        if (updateChecker != null) {
          final CheckForUpdates.UpdateAvailableCallback callback = new CheckForUpdates.UpdateAvailableCallback() {
            public void onUpdateAvailable(final String html) {
              // Do this on the main thread.
              //
              parent.getDisplay().asyncExec(new Runnable() {
                public void run() {
                  new BrowserDialog(parent, getLogger(), html).open(true);
                }

              });
            }
          };

          // Run the update checker on a background thread.
          //
          Thread checkerThread = new Thread() {
            public void run() {
              updateChecker.check(callback);
            }
          };

          checkerThread.setDaemon(true);
          checkerThread.start();
        }
      } catch (Throwable e) {
        // Always silently ignore any errors.
      }
    }
  }

  public AbstractTreeLogger getLogger() {
    return fLogPane.getLogger();
  }

  public void shellActivated(ShellEvent e) {
  }

  public void shellClosed(ShellEvent e) {
	  if (fServerWindow.hasBrowserWindowsOpen()) {
		  boolean closeWindows=true;
		  if(System.getProperty("gwt.shell.endquick")==null) {
			  closeWindows = DialogBase.confirmAction((Shell) e.widget,
						  "Closing the development shell will close " +
						  "all hosted mode browsers.  Continue?", "Confirm close");
		  } 
		if (closeWindows) {
			  fServerWindow.closeAllBrowserWindows();
			  e.doit = true;
		  } else {
			  e.doit = false;
		  }
	  }
  }

  public void shellDeactivated(ShellEvent e) {
  }

  public void shellDeiconified(ShellEvent e) {
  }

  public void shellIconified(ShellEvent e) {
  }

  public void widgetDisposed(DisposeEvent e) {
    fColorWhite.dispose();
  }

  private static String verify(String hash) {
    char[] in = hash.toCharArray();
    char[] ou = new char[in.length];
    for (int i = 0, c = 0; i < in.length; ++i) {
      if (in[i] < 'a')
        c += in[i] - 'A';
      else
        c += in[i] - 'a' - 26;

      if (c == 0)
        ou[i] = ' ';
      else
        ou[i] = (char) ('@' + c);
    }
    return String.valueOf(ou);
  }

  private Color fColorWhite;
  private TreeLoggerWidget fLogPane;
  private GWTShell fServerWindow;
  private Toolbar fToolbar;
}
