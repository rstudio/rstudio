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
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.shell.BrowserWidget;
import com.google.gwt.dev.shell.BrowserWidgetHost;
import com.google.gwt.dev.shell.ModuleSpaceHost;
import com.google.gwt.dev.shell.ShellMainWindow;
import com.google.gwt.dev.shell.ShellModuleSpaceHost;
import com.google.gwt.dev.util.log.AbstractTreeLogger;
import com.google.gwt.util.tools.ToolBase;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.internal.Library;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * The main executable class for hosted mode shells based on SWT.
 */
abstract class SwtHostedModeBase extends HostedModeBase {

  private class SwtBrowserWidgetHostImpl extends BrowserWidgetHostImpl {

    @Override
    public ModuleSpaceHost createModuleSpaceHost(TreeLogger logger,
        BrowserWidget widget, String moduleName)
        throws UnableToCompleteException {
      // Switch to a wait cursor.
      Shell widgetShell = widget.getShell();
      try {
        Cursor waitCursor = display.getSystemCursor(SWT.CURSOR_WAIT);
        widgetShell.setCursor(waitCursor);

        // Try to find an existing loaded version of the module def.
        //
        ModuleDef moduleDef = loadModule(logger, moduleName, true);
        assert (moduleDef != null);

        TypeOracle typeOracle = moduleDef.getTypeOracle(logger);
        ShellModuleSpaceHost host = doCreateShellModuleSpaceHost(
            getTopLogger(), typeOracle, moduleDef);
        return host;
      } finally {
        Cursor normalCursor = display.getSystemCursor(SWT.CURSOR_ARROW);
        widgetShell.setCursor(normalCursor);
      }
    }

    public ModuleSpaceHost createModuleSpaceHost(TreeLogger logger,
        String moduleName, String userAgent, String url, String tabKey,
        String sessionKey, String remoteEndpoint)
        throws UnableToCompleteException {
      throw new UnsupportedOperationException();
    }

    public void unloadModule(ModuleSpaceHost moduleSpaceHost) {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * All of these classes must extend BrowserWidget. The first one that loads
   * will be used, so it is important that only the correct one be on the
   * classpath.
   */
  private static final String[] browserClassNames = new String[] {
      "com.google.gwt.dev.shell.ie.BrowserWidgetIE6",
      "com.google.gwt.dev.shell.moz.BrowserWidgetMoz",
      "com.google.gwt.dev.shell.mac.BrowserWidgetSaf"};

  static {
    // Force ToolBase to clinit, which causes SWT stuff to happen.
    new ToolBase() {
    };
    // Correct menu on Mac OS X
    Display.setAppName("GWT");
  }

  private static BrowserWidget createBrowserWidget(TreeLogger logger,
      Composite parent, BrowserWidgetHost host)
      throws UnableToCompleteException {
    Throwable caught = null;
    try {
      for (int i = 0; i < browserClassNames.length; i++) {
        Class<? extends BrowserWidget> clazz = null;
        try {
          clazz = Class.forName(browserClassNames[i]).asSubclass(
              BrowserWidget.class);
          Constructor<? extends BrowserWidget> ctor = clazz.getDeclaredConstructor(new Class[] {
              Shell.class, BrowserWidgetHost.class});
          BrowserWidget bw = ctor.newInstance(new Object[] {parent, host});
          return bw;
        } catch (ClassNotFoundException e) {
          caught = e;
        }
      }
      logger.log(TreeLogger.ERROR,
          "No instantiable browser widget class could be found", caught);
      throw new UnableToCompleteException();
    } catch (SecurityException e) {
      caught = e;
    } catch (NoSuchMethodException e) {
      caught = e;
    } catch (IllegalArgumentException e) {
      caught = e;
    } catch (InstantiationException e) {
      caught = e;
    } catch (IllegalAccessException e) {
      caught = e;
    } catch (InvocationTargetException e) {
      caught = e.getTargetException();
    } catch (ClassCastException e) {
      caught = e;
    }
    logger.log(TreeLogger.ERROR,
        "The browser widget class could not be instantiated", caught);
    throw new UnableToCompleteException();
  }

  private BrowserWidgetHostImpl browserHost = new SwtBrowserWidgetHostImpl();

  private final List<Shell> browserShells = new ArrayList<Shell>();

  /**
   * Use the default display; constructing a new one would make instantiating
   * multiple GWTShells fail with a mysterious exception.
   */
  private final Display display = Display.getDefault();

  private ShellMainWindow mainWnd;

  public SwtHostedModeBase() {
    super();
  }

  @Override
  public final void closeAllBrowserWindows() {
    while (!browserShells.isEmpty()) {
      browserShells.get(0).dispose();
    }
  }

  @Override
  public TreeLogger getTopLogger() {
    return mainWnd.getLogger();
  }

  @Override
  public final boolean hasBrowserWindowsOpen() {
    if (browserShells.isEmpty()) {
      return false;
    } else {
      return true;
    }
  }

  /**
   * Launch the arguments as Urls in separate windows.
   */
  @Override
  public void launchStartupUrls(final TreeLogger logger) {
    // Launch a browser window for each startup url.
    String startupURL = "";
    try {
      for (String prenormalized : options.getStartupURLs()) {
        startupURL = normalizeURL(prenormalized);
        logger.log(TreeLogger.TRACE, "Starting URL: " + startupURL, null);
        BrowserWidget bw = openNewBrowserWindow();
        bw.go(startupURL);
      }
    } catch (UnableToCompleteException e) {
      logger.log(TreeLogger.ERROR,
          "Unable to open new window for startup URL: " + startupURL, null);
    }
  }

  /**
   * Called directly by ShellMainWindow and indirectly via BrowserWidgetHost.
   */
  public final BrowserWidget openNewBrowserWindow()
      throws UnableToCompleteException {
    boolean succeeded = false;
    Shell s = createTrackedBrowserShell();
    try {
      BrowserWidget bw = createBrowserWidget(getTopLogger(), s, browserHost);

      if (mainWnd != null) {
        Rectangle r = mainWnd.getShell().getBounds();
        int n = browserShells.size() + 1;
        s.setBounds(r.x + n * 50, r.y + n * 50, 800, 600);
      } else {
        s.setSize(800, 600);
      }

      if (!isHeadless()) {
        s.open();
      }

      bw.onFirstShown();
      succeeded = true;
      return bw;
    } finally {
      if (!succeeded) {
        s.dispose();
      }
    }
  }

  protected final BrowserWidgetHost getBrowserHost() {
    return browserHost;
  }

  protected abstract String getTitleText();

  @Override
  protected void initializeLogger() {
    final AbstractTreeLogger logger = mainWnd.getLogger();
    logger.setMaxDetail(options.getLogLevel());
  }

  @Override
  protected void loadRequiredNativeLibs() {
    String libName = null;
    try {
      libName = "swt";
      Library.loadLibrary(libName);
    } catch (UnsatisfiedLinkError e) {
      StringBuffer sb = new StringBuffer();
      sb.append("Unable to load required native library '" + libName + "'");
      sb.append("\n\tPlease specify the JVM startup argument ");
      sb.append("\"-Djava.library.path\"");
      throw new RuntimeException(sb.toString(), e);
    }
  }

  @Override
  protected boolean notDone() {
    if (!mainWnd.isDisposed()) {
      return true;
    }
    if (!browserShells.isEmpty()) {
      return true;
    }
    return false;
  }

  @Override
  protected void openAppWindow() {
    final Shell shell = new Shell(display);

    FillLayout fillLayout = new FillLayout();
    fillLayout.marginWidth = 0;
    fillLayout.marginHeight = 0;
    shell.setLayout(fillLayout);

    shell.setImages(ShellMainWindow.getIcons());

    mainWnd = new ShellMainWindow(this, shell, getTitleText(),
        options.isNoServer() ? 0 : getPort(), 
        options.alsoLogToFile() ? options.getLogFile("hosted.log") : null,
        options.getLogLevel());

    shell.setSize(700, 600);
    if (!isHeadless()) {
      shell.open();
    }
  }

  @Override
  protected void processEvents() throws Exception {
    if (!display.readAndDispatch()) {
      sleep();
    }
  }

  protected void sleep() {
    display.sleep();
  }

  private Shell createTrackedBrowserShell() {
    final Shell shell = new Shell(display);
    FillLayout fillLayout = new FillLayout();
    fillLayout.marginWidth = 0;
    fillLayout.marginHeight = 0;
    shell.setLayout(fillLayout);
    browserShells.add(shell);
    shell.addDisposeListener(new DisposeListener() {
      public void widgetDisposed(DisposeEvent e) {
        if (e.widget == shell) {
          browserShells.remove(shell);
        }
      }
    });

    shell.setImages(ShellMainWindow.getIcons());

    return shell;
  }
}
