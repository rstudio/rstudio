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
import com.google.gwt.dev.WebServerPanel.RestartAction;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.shell.BrowserListener;
import com.google.gwt.dev.shell.BrowserWidget;
import com.google.gwt.dev.shell.BrowserWidgetHost;
import com.google.gwt.dev.shell.ModuleSpaceHost;
import com.google.gwt.dev.shell.OophmSessionHandler;
import com.google.gwt.dev.shell.ShellMainWindow;
import com.google.gwt.dev.shell.ShellModuleSpaceHost;
import com.google.gwt.dev.util.log.AbstractTreeLogger;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import com.google.gwt.util.tools.ArgHandlerString;

import java.awt.Cursor;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.IdentityHashMap;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;

/**
 * The main executable class for hosted mode shells based on SWT.
 */
abstract class OophmHostedModeBase extends HostedModeBase {

  /**
   * Handles the -portHosted command line flag.
   */
  private static class ArgHandlerPortHosted extends ArgHandlerString {

    private final OptionPortHosted options;

    public ArgHandlerPortHosted(OptionPortHosted options) {
      this.options = options;
    }

    @Override
    public String[] getDefaultArgs() {
      return new String[] {"-portHosted", "9997"};
    }

    @Override
    public String getPurpose() {
      return "Listens on the specified port for hosted mode connections";
    }

    @Override
    public String getTag() {
      return "-portHosted";
    }

    @Override
    public String[] getTagArgs() {
      return new String[] {"port-number | \"auto\""};
    }

    @Override
    public boolean setString(String value) {
      if (value.equals("auto")) {
        options.setPortHosted(0);
      } else {
        try {
          options.setPortHosted(Integer.parseInt(value));
        } catch (NumberFormatException e) {
          System.err.println("A port must be an integer or \"auto\"");
          return false;
        }
      }
      return true;
    }
  }

  abstract static class ArgProcessor extends HostedModeBase.ArgProcessor {
    public ArgProcessor(OophmHostedModeBaseOptions options, boolean forceServer) {
      super(options, forceServer);
      registerHandler(new ArgHandlerPortHosted(options));
    }
  }

  interface OophmHostedModeBaseOptions extends HostedModeBaseOptions,
      OptionPortHosted {
  }

  /**
   * Concrete class to implement all shell options.
   */
  static class OophmHostedModeBaseOptionsImpl extends HostedModeBaseOptionsImpl
      implements OophmHostedModeBaseOptions {
    private int portHosted;

    public int getPortHosted() {
      return portHosted;
    }

    public void setPortHosted(int port) {
      portHosted = port;
    }
  }

  interface OptionPortHosted {
    int getPortHosted();

    void setPortHosted(int portHosted);
  }

  private class OophmBrowserWidgetHostImpl extends BrowserWidgetHostImpl {
    private final Map<ModuleSpaceHost, ModulePanel> moduleTabs = new IdentityHashMap<ModuleSpaceHost, ModulePanel>();

    @Override
    public ModuleSpaceHost createModuleSpaceHost(TreeLogger logger,
        BrowserWidget widget, String moduleName)
        throws UnableToCompleteException {
      throw new UnsupportedOperationException();
    }

    public ModuleSpaceHost createModuleSpaceHost(TreeLogger mainLogger,
        String moduleName, String userAgent, String url, String sessionKey,
        String remoteSocket) throws UnableToCompleteException {
      TreeLogger logger = mainLogger;
      TreeLogger.Type maxLevel = TreeLogger.INFO;
      if (mainLogger instanceof AbstractTreeLogger) {
        maxLevel = ((AbstractTreeLogger) mainLogger).getMaxDetail();
      }
      // TODO(jat): collect different sessions into the same tab, with a
      //    dropdown to select individual module's logs
      ModulePanel tab;
      if (!isHeadless()) {
        tab = new ModulePanel(maxLevel, moduleName, userAgent, remoteSocket,
            tabs);
        logger = tab.getLogger();

        // Switch to a wait cursor.
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
      } else {
        tab = null;
      }

      try {
        // Try to find an existing loaded version of the module def.
        ModuleDef moduleDef = loadModule(logger, moduleName, true);
        assert (moduleDef != null);

        // Create a sandbox for the module.
        // TODO(jat): consider multiple instances of the same module open at
        // once
        TypeOracle typeOracle = moduleDef.getTypeOracle(logger);
        ShellModuleSpaceHost host = doCreateShellModuleSpaceHost(logger,
            typeOracle, moduleDef);

        if (tab != null) {
          moduleTabs.put(host, tab);
        }
        return host;
      } catch (RuntimeException e) {
        logger.log(TreeLogger.ERROR, "Exception initializing module", e);
        throw e;
      } finally {
        if (!isHeadless()) {
          frame.setCursor(Cursor.getDefaultCursor());
        }
      }
    }

    public void unloadModule(ModuleSpaceHost moduleSpaceHost) {
      ModulePanel tab = moduleTabs.remove(moduleSpaceHost);
      if (tab != null) {
        tab.disconnect();
      }
    }
  }

  protected static final String PACKAGE_PATH = OophmHostedModeBase.class.getPackage().getName().replace(
      '.', '/').concat("/shell/");

  /**
   * Loads an image from the classpath in this package.
   */
  static ImageIcon loadImageIcon(String name) {
    return loadImageIcon(name, true);
  }

  /**
   * Loads an image from the classpath, optionally prepending this package.
   * 
   * @param name name of an image file.
   * @param prependPackage true if {@link #PACKAGE_PATH} should be prepended to
   *          this name.
   */
  static ImageIcon loadImageIcon(String name, boolean prependPackage) {
    ClassLoader cl = OophmHostedModeBase.class.getClassLoader();
    if (prependPackage) {
      name = PACKAGE_PATH + name;
    }
    
    URL url = (name == null) ? null : cl.getResource(name);
    if (url != null) {
      ImageIcon image = new ImageIcon(url);
      return image;
    } else {
      // Bad image.
      return new ImageIcon();
    }
  }

  protected BrowserListener listener;

  /**
   * Hiding super field because it's actually the same object, just with a
   * stronger type.
   */
  @SuppressWarnings("hiding")
  protected final OophmHostedModeBaseOptionsImpl options = (OophmHostedModeBaseOptionsImpl) super.options;

  // TODO(jat): clean up access to this field
  protected WebServerPanel webServerLog;

  private BrowserWidgetHostImpl browserHost = new OophmBrowserWidgetHostImpl();

  private JFrame frame;

  private volatile boolean mainWindowClosed;

  private ShellMainWindow mainWnd;

  private JTabbedPane tabs;

  private AbstractTreeLogger topLogger;

  public OophmHostedModeBase() {
    super();
  }

  @Override
  public void closeAllBrowserWindows() {
  }

  @Override
  public TreeLogger getTopLogger() {
    return topLogger;
  }

  @Override
  public boolean hasBrowserWindowsOpen() {
    return false;
  }

  /**
   * Launch the arguments as Urls in separate windows.
   */
  @Override
  public void launchStartupUrls(final TreeLogger logger) {
    ensureOophmListener();
    String startupURL = "";
    try {
      for (String prenormalized : options.getStartupURLs()) {
        startupURL = normalizeURL(prenormalized);
        logger.log(TreeLogger.INFO, "Starting URL: " + startupURL, null);
        launchURL(startupURL);
      }
    } catch (UnableToCompleteException e) {
      logger.log(TreeLogger.ERROR,
          "Unable to open new window for startup URL: " + startupURL, null);
    }
  }

  public void launchURL(String url) throws UnableToCompleteException {
    /*
     * TODO(jat): properly support launching arbitrary browsers; waiting on
     * Freeland's work with BrowserScanner and the trunk merge to get it.
     */
    try {
      URL parsedUrl = new URL(url);
      String path = parsedUrl.getPath();
      String query = parsedUrl.getQuery();
      String hash = parsedUrl.getRef();
      String hostedParam =  "gwt.hosted=" + listener.getEndpointIdentifier();
      if (query == null) {
        query = hostedParam;
      } else {
        query += '&' + hostedParam;
      }
      path += '?' + query;
      if (hash != null) {
        path += '#' + hash;
      }
      url = new URL(parsedUrl.getProtocol(), parsedUrl.getHost(),
          parsedUrl.getPort(), path).toExternalForm();
    } catch (MalformedURLException e) {
      getTopLogger().log(TreeLogger.ERROR, "Invalid URL " + url, e);
      throw new UnableToCompleteException();
    }
    TreeLogger branch = getTopLogger().branch(TreeLogger.INFO,
        "Launching firefox with " + url, null);
    try {
      Process browser = Runtime.getRuntime().exec("firefox " + url + "&");
      int exitCode = browser.waitFor();
      if (exitCode != 0) {
        branch.log(TreeLogger.ERROR, "Exit code " + exitCode, null);
      }
    } catch (IOException e) {
      branch.log(TreeLogger.ERROR, "Error starting browser", e);
    } catch (InterruptedException e) {
      branch.log(TreeLogger.ERROR, "Error starting browser", e);
    }
  }

  public BrowserWidget openNewBrowserWindow() throws UnableToCompleteException {
    throw new UnableToCompleteException();
  }

  /**
   * @throws UnableToCompleteException
   */
  @Override
  protected void compile(TreeLogger logger) throws UnableToCompleteException {
    throw new UnsupportedOperationException();
  }

  @Override
  protected boolean doStartup() {
    if (super.doStartup()) {
      // Accept connections from OOPHM clients
      ensureOophmListener();
      return true;
    }
    return false;
  }

  protected final BrowserWidgetHost getBrowserHost() {
    return browserHost;
  }

  /**
   * @return the icon to use for the web server tab
   */
  protected ImageIcon getWebServerIcon() {
    return null;
  }

  /**
   * @return the name of the web server tab
   */
  protected String getWebServerName() {
    return "Server";
  }

  @Override
  protected void initializeLogger() {
    if (mainWnd != null) {
      topLogger = mainWnd.getLogger();
    } else {
      topLogger = new PrintWriterTreeLogger(new PrintWriter(System.out));
    }
    topLogger.setMaxDetail(options.getLogLevel());
  }

  @Override
  protected boolean initModule(String moduleName) {
    /*
     * Not used in legacy mode due to GWTShellServlet playing this role.
     * 
     * TODO: something smarter here and actually make GWTShellServlet less
     * magic?
     */
    return false;
  }

  @Override
  protected void loadRequiredNativeLibs() {
    // no native libraries are needed with OOPHM
  }

  @Override
  protected synchronized boolean notDone() {
    return !mainWindowClosed;
  }

  @Override
  protected void openAppWindow() {
    ImageIcon gwtIcon = loadImageIcon("icon24.png");
    frame = new JFrame("GWT Hosted Mode");
    tabs = new JTabbedPane();
    mainWnd = new ShellMainWindow(options.getLogLevel());
    tabs.addTab("Hosted Mode", gwtIcon, mainWnd, "GWT Hosted-mode");
    if (!options.isNoServer()) {
      webServerLog = new WebServerPanel(getPort(), options.getLogLevel(),
          new RestartAction() {
            public void restartServer(TreeLogger logger) {
              try {
                OophmHostedModeBase.this.restartServer(logger);
              } catch (UnableToCompleteException e) {
                // Already logged why it failed
              }
            }
          });
      tabs.addTab(getWebServerName(), getWebServerIcon(), webServerLog);
    }
    frame.getContentPane().add(tabs);
    frame.setSize(950, 700);
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosed(WindowEvent e) {
        setMainWindowClosed();
      }
    });
    frame.setIconImage(loadImageIcon("icon16.png").getImage());
    frame.setVisible(true);
  }

  @Override
  protected void processEvents() throws Exception {
    Thread.sleep(10);
  }

  protected synchronized void setMainWindowClosed() {
    mainWindowClosed = true;
  }

  private void ensureOophmListener() {
    if (listener == null) {
      listener = new BrowserListener(getTopLogger(), options.getPortHosted(),
          new OophmSessionHandler(browserHost));
      listener.start();
    }
  }
}
