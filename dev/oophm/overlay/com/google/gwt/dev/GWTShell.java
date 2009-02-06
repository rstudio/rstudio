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
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.EmittedArtifact;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.GWTCompiler.GWTCompilerOptionsImpl;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.ModuleDefLoader;
import com.google.gwt.dev.shell.ArtifactAcceptor;
import com.google.gwt.dev.shell.BrowserListener;
import com.google.gwt.dev.shell.BrowserWidget;
import com.google.gwt.dev.shell.BrowserWidgetHost;
import com.google.gwt.dev.shell.ModuleSpaceHost;
import com.google.gwt.dev.shell.OophmSessionHandler;
import com.google.gwt.dev.shell.ShellMainWindow;
import com.google.gwt.dev.shell.ShellModuleSpaceHost;
import com.google.gwt.dev.shell.WorkDirs;
import com.google.gwt.dev.shell.tomcat.EmbeddedTomcatServer;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.arg.ArgHandlerOutDir;
import com.google.gwt.dev.util.log.AbstractTreeLogger;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import com.google.gwt.util.tools.ArgHandlerExtra;
import com.google.gwt.util.tools.ArgHandlerString;

import java.awt.Cursor;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;

/**
 * The main executable class for the hosted mode shell.
 */
@SuppressWarnings("deprecation")
@Deprecated
public class GWTShell extends HostedModeBase {

  /**
   * Handles the -portHosted command line flag.
   */
  protected static class ArgHandlerPortHosted extends ArgHandlerString {

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

  /**
   * Handles the list of startup urls that can be passed at the end of the
   * command line.
   */
  protected static class ArgHandlerStartupURLsExtra extends ArgHandlerExtra {

    private final OptionStartupURLs options;

    public ArgHandlerStartupURLsExtra(OptionStartupURLs options) {
      this.options = options;
    }

    @Override
    public boolean addExtraArg(String arg) {
      options.addStartupURL(arg);
      return true;
    }

    @Override
    public String getPurpose() {
      return "Automatically launches the specified URL";
    }

    @Override
    public String[] getTagArgs() {
      return new String[] {"url"};
    }
  }

  /**
   * The GWTShell argument processor.
   */
  protected static class ArgProcessor extends HostedModeBase.ArgProcessor {
    public ArgProcessor(ShellOptionsImpl options, boolean forceServer,
        boolean noURLs) {
      super(options, forceServer);
      if (!noURLs) {
        registerHandler(new ArgHandlerStartupURLsExtra(options));
      }
      registerHandler(new ArgHandlerOutDir(options));
      registerHandler(new ArgHandlerPortHosted(options));
    }

    @Override
    protected String getName() {
      return GWTShell.class.getName();
    }
  }

  interface OptionPortHosted {
    int getPortHosted();

    void setPortHosted(int portHosted);
  }

  /**
   * Concrete class to implement all shell options.
   */
  static class ShellOptionsImpl extends HostedModeBaseOptionsImpl implements
      HostedModeBaseOptions, WorkDirs, LegacyCompilerOptions, OptionPortHosted {
    private int localWorkers;
    private File outDir;
    private int portHosted;

    public File getCompilerOutputDir(ModuleDef moduleDef) {
      return new File(getOutDir(), moduleDef.getName());
    }

    public int getLocalWorkers() {
      return localWorkers;
    }

    public File getOutDir() {
      return outDir;
    }

    public int getPortHosted() {
      return portHosted;
    }

    public File getShellPublicGenDir(ModuleDef moduleDef) {
      return new File(getShellBaseWorkDir(moduleDef), "public");
    }

    @Override
    public File getWorkDir() {
      return new File(getOutDir(), ".gwt-tmp");
    }

    public void setLocalWorkers(int localWorkers) {
      this.localWorkers = localWorkers;
    }

    public void setOutDir(File outDir) {
      this.outDir = outDir;
    }

    public void setPortHosted(int port) {
      portHosted = port;
    }
  }

  private class BrowserWidgetHostImpl implements BrowserWidgetHost {
    private TreeLogger logger;
    private Map<ModuleSpaceHost, ModulePanel> moduleTabs = new IdentityHashMap<ModuleSpaceHost, ModulePanel>();

    public BrowserWidgetHostImpl() {
    }

    public void compile(ModuleDef moduleDef) throws UnableToCompleteException {
      GWTShell.this.compile(getLogger(), moduleDef);
    }

    public void compile(String[] moduleNames) throws UnableToCompleteException {
      for (int i = 0; i < moduleNames.length; i++) {
        String moduleName = moduleNames[i];
        ModuleDef moduleDef = loadModule(moduleName, getLogger());
        compile(moduleDef);
      }
    }

    public ModuleSpaceHost createModuleSpaceHost(BrowserWidget widget,
        String moduleName) throws UnableToCompleteException {
      // TODO(jat): implement method createModuleSpaceHost
      return null;
    }

    public ModuleSpaceHost createModuleSpaceHost(TreeLogger mainLogger,
        String moduleName, String userAgent, String remoteSocket)
        throws UnableToCompleteException {
      logger = mainLogger;
      TreeLogger.Type maxLevel = TreeLogger.INFO;
      if (mainLogger instanceof AbstractTreeLogger) {
        maxLevel = ((AbstractTreeLogger) mainLogger).getMaxDetail();
      }

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
        ModuleDef moduleDef = loadModule(moduleName, logger);
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

    public TreeLogger getLogger() {
      return logger;
    }

    public String normalizeURL(String whatTheUserTyped) {
      return GWTShell.this.normalizeURL(whatTheUserTyped);
    }

    public BrowserWidget openNewBrowserWindow()
        throws UnableToCompleteException {
      // TODO(jat): is this ok?
      throw new UnableToCompleteException();
    }

    public void unloadModule(ModuleSpaceHost moduleSpaceHost) {
      ModulePanel tab = moduleTabs.remove(moduleSpaceHost);
      if (tab != null) {
        tab.disconnect();
      }
    }

    /**
     * Load a module.
     * 
     * @param moduleName name of the module to load
     * @param logger TreeLogger to use
     * @return the loaded module
     * @throws UnableToCompleteException
     */
    private ModuleDef loadModule(String moduleName, TreeLogger logger)
        throws UnableToCompleteException {
      // TODO(jat): consider multithreading issues dealing with ModuleDefs
      boolean assumeFresh = !alreadySeenModules.contains(moduleName);
      ModuleDef moduleDef = ModuleDefLoader.loadFromClassPath(logger,
          moduleName, !assumeFresh);
      alreadySeenModules.add(moduleName);
      assert (moduleDef != null) : "Required module state is absent";
      return moduleDef;
    }
  }

  public static final String GWT_SHELL_PATH = ".gwt-tmp" + File.separator
      + "shell";

  private static final String PACKAGE_PATH = GWTShell.class.getPackage().getName().replace(
      '.', '/').concat("/shell/");

  public static String checkHost(String hostUnderConsideration,
      Set<String> hosts) {
    hostUnderConsideration = hostUnderConsideration.toLowerCase();
    for (String rule : hosts) {
      // match on lowercased regex
      if (hostUnderConsideration.matches(".*" + rule + ".*")) {
        return rule;
      }
    }
    return null;
  }

  public static String computeHostRegex(String url) {
    // the entire URL up to the first slash not prefixed by a slash or colon.
    String raw = url.split("(?<![:/])/")[0];
    // escape the dots and put a begin line specifier on the result
    return "^" + raw.replaceAll("[.]", "[.]");
  }

  public static String formatRules(Set<String> invalidHttpHosts) {
    StringBuffer out = new StringBuffer();
    for (String rule : invalidHttpHosts) {
      out.append(rule);
      out.append(" ");
    }
    return out.toString();
  }

  public static void main(String[] args) {
    /*
     * NOTE: main always exits with a call to System.exit to terminate any
     * non-daemon threads that were started in Generators. Typically, this is to
     * shutdown AWT related threads, since the contract for their termination is
     * still implementation-dependent.
     */
    GWTShell gwtShell = new GWTShell();
    ArgProcessor argProcessor = new ArgProcessor(gwtShell.options, false, false);
    if (argProcessor.processArgs(args)) {
      gwtShell.run();
      // Exit w/ success code.
      System.exit(0);
    }
    // Exit w/ non-success code.
    System.exit(-1);
  }

  /**
   * Loads an image from the classpath in this package.
   */
  static ImageIcon loadImageIcon(String name) {
    ClassLoader cl = GWTShell.class.getClassLoader();
    URL url = cl.getResource(PACKAGE_PATH + name);
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
  protected final ShellOptionsImpl options = (ShellOptionsImpl) super.options;

  protected File outDir;

  /**
   * Cheat on the first load's refresh by assuming the module loaded by
   * {@link com.google.gwt.dev.shell.GWTShellServlet} is still fresh. This
   * prevents a double-refresh on startup. Subsequent refreshes will trigger a
   * real refresh.
   */
  private Set<String> alreadySeenModules = new HashSet<String>();

  private BrowserWidgetHostImpl browserHost = new BrowserWidgetHostImpl();

  private JFrame frame;

  private volatile boolean mainWindowClosed;

  private ShellMainWindow mainWnd;

  private JTabbedPane tabs;

  private AbstractTreeLogger topLogger;

  private WebServerPanel webServerLog;

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
    String separator;
    if (url.contains("?")) {
      separator = "&";
    } else {
      separator = "?";
    }
    url += separator + "gwt.hosted=" + listener.getEndpointIdentifier();
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

  @Override
  protected void compile(TreeLogger logger) throws UnableToCompleteException {
    throw new UnsupportedOperationException();
  }

  /**
   * Compiles a logical module def. The caller can modify the specified module
   * def programmatically in some cases (this is needed for JUnit support, for
   * example).
   */
  @Override
  protected void compile(TreeLogger logger, ModuleDef moduleDef)
      throws UnableToCompleteException {
    LegacyCompilerOptions newOptions = new GWTCompilerOptionsImpl(options);
    if (!new GWTCompiler(newOptions).run(logger, moduleDef)) {
      // TODO(jat): error dialog?
    }
  }

  @Override
  protected HostedModeBaseOptions createOptions() {
    return new ShellOptionsImpl();
  }

  @Override
  protected ArtifactAcceptor doCreateArtifactAcceptor(final ModuleDef module) {
    return new ArtifactAcceptor() {
      public void accept(TreeLogger logger, ArtifactSet artifacts)
          throws UnableToCompleteException {

        /*
         * Copied from StandardLinkerContext.produceOutputDirectory() for legacy
         * GWTShellServlet support.
         */
        for (EmittedArtifact artifact : artifacts.find(EmittedArtifact.class)) {
          if (!artifact.isPrivate()) {
            File outFile = new File(options.getShellPublicGenDir(module),
                artifact.getPartialPath());
            Util.copy(logger, artifact.getContents(logger), outFile);
          }
        }
      }
    };
  }

  /**
   * Can be override to change the default log level in subclasses. JUnit does
   * this for example.
   */
  protected Type doGetDefaultLogLevel() {
    return Type.INFO;
  }

  @Override
  protected void doShutDownServer() {
    // Stop the HTTP server.
    //
    EmbeddedTomcatServer.stop();
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

  @Override
  protected int doStartUpServer() {
    // TODO(bruce): make tomcat work in terms of the modular launcher
    String whyFailed = EmbeddedTomcatServer.start(isHeadless() ? getTopLogger()
        : webServerLog.getLogger(), getPort(), options);

    // TODO(bruce): test that we can remove this old approach in favor of
    // a better, logger-based error reporting
    if (whyFailed != null) {
      System.err.println(whyFailed);
      return -1;
    }
    return EmbeddedTomcatServer.getPort();
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
    boolean checkForUpdates = doShouldCheckForUpdates();
    mainWnd = new ShellMainWindow(this, checkForUpdates, options.getLogLevel());
    tabs.addTab("Hosted Mode", gwtIcon, mainWnd, "GWT Hosted-mode");
    if (!options.isNoServer()) {
      ImageIcon tomcatIcon = loadImageIcon("tomcat24.png");
      webServerLog = new WebServerPanel(getPort(), options.getLogLevel());
      tabs.addTab("Tomcat", tomcatIcon, webServerLog);
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

  private void ensureOophmListener() {
    if (listener == null) {
      listener = new BrowserListener(getTopLogger(), options.getPortHosted(),
          new OophmSessionHandler(browserHost));
      listener.start();
    }
  }

  private synchronized void setMainWindowClosed() {
    mainWindowClosed = true;
  }
}
