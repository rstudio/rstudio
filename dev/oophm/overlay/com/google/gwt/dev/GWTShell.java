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
import com.google.gwt.dev.shell.BrowserWidgetHostChecker;
import com.google.gwt.dev.shell.ModuleSpaceHost;
import com.google.gwt.dev.shell.OophmSessionHandler;
import com.google.gwt.dev.shell.ShellMainWindow;
import com.google.gwt.dev.shell.ShellModuleSpaceHost;
import com.google.gwt.dev.shell.WorkDirs;
import com.google.gwt.dev.shell.tomcat.EmbeddedTomcatServer;
import com.google.gwt.dev.util.PerfLogger;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.arg.ArgHandlerDisableAggressiveOptimization;
import com.google.gwt.dev.util.arg.ArgHandlerEnableAssertions;
import com.google.gwt.dev.util.arg.ArgHandlerExtraDir;
import com.google.gwt.dev.util.arg.ArgHandlerGenDir;
import com.google.gwt.dev.util.arg.ArgHandlerLogLevel;
import com.google.gwt.dev.util.arg.ArgHandlerOutDir;
import com.google.gwt.dev.util.arg.ArgHandlerScriptStyle;
import com.google.gwt.dev.util.arg.ArgHandlerWorkDirOptional;
import com.google.gwt.dev.util.log.AbstractTreeLogger;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import com.google.gwt.util.tools.ArgHandlerExtra;
import com.google.gwt.util.tools.ArgHandlerFlag;
import com.google.gwt.util.tools.ArgHandlerString;
import com.google.gwt.util.tools.ToolBase;

import java.awt.Cursor;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;

/**
 * The main executable class for the hosted mode shell.
 */
public class GWTShell extends ToolBase {

  /**
   * Handles the -blacklist command line argument.
   */
  protected class ArgHandlerBlacklist extends ArgHandlerString {

    @Override
    public String[] getDefaultArgs() {
      return new String[] {"-blacklist", ""};
    }

    @Override
    public String getPurpose() {
      return "Prevents the user browsing URLs that match the specified regexes (comma or space separated)";
    }

    @Override
    public String getTag() {
      return "-blacklist";
    }

    @Override
    public String[] getTagArgs() {
      return new String[] {"blacklist-string"};
    }

    @Override
    public boolean setString(String blacklistStr) {
      return BrowserWidgetHostChecker.blacklistRegexes(blacklistStr);
    }
  }

  /**
   * handles the -noserver command line flag.
   */
  protected class ArgHandlerNoServerFlag extends ArgHandlerFlag {
    @Override
    public String getPurpose() {
      return "Prevents the embedded Tomcat server from running, even if a port is specified";
    }

    @Override
    public String getTag() {
      return "-noserver";
    }

    @Override
    public boolean setFlag() {
      runTomcat = false;
      return true;
    }
  }

  /**
   * Handles the -port command line flag.
   */
  protected class ArgHandlerPort extends ArgHandlerString {

    @Override
    public String[] getDefaultArgs() {
      return new String[] {"-port", "8888"};
    }

    @Override
    public String getPurpose() {
      return "Runs an embedded Tomcat instance on the specified port (defaults to 8888)";
    }

    @Override
    public String getTag() {
      return "-port";
    }

    @Override
    public String[] getTagArgs() {
      return new String[] {"port-number | \"auto\""};
    }

    @Override
    public boolean setString(String value) {
      if (value.equals("auto")) {
        port = 0;
      } else {
        try {
          port = Integer.parseInt(value);
        } catch (NumberFormatException e) {
          System.err.println("A port must be an integer or \"auto\"");
          return false;
        }
      }
      return true;
    }
  }

  /**
   * Handles the -portHosted command line flag.
   */
  protected class ArgHandlerPortHosted extends ArgHandlerString {

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
        portHosted = 0;
      } else {
        try {
          portHosted = Integer.parseInt(value);
        } catch (NumberFormatException e) {
          String msg = "A port must be an integer or \"auto\"";
          getTopLogger().log(TreeLogger.ERROR, msg, null);
          return false;
        }
      }
      return true;
    }
  }

  /**
   * Handles the list of startup urls that can be passed on the command line.
   */
  protected class ArgHandlerStartupURLs extends ArgHandlerExtra {

    @Override
    public boolean addExtraArg(String arg) {
      addStartupURL(arg);
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
   * Handles the -whitelist command line flag.
   */
  protected class ArgHandlerWhitelist extends ArgHandlerString {

    @Override
    public String[] getDefaultArgs() {
      return new String[] {"-whitelist", ""};
    }

    @Override
    public String getPurpose() {
      return "Allows the user to browse URLs that match the specified regexes (comma or space separated)";
    }

    @Override
    public String getTag() {
      return "-whitelist";
    }

    @Override
    public String[] getTagArgs() {
      return new String[] {"whitelist-string"};
    }

    @Override
    public boolean setString(String whitelistStr) {
      return BrowserWidgetHostChecker.whitelistRegexes(whitelistStr);
    }
  }

  /**
   * Concrete class to implement all compiler options.
   */
  static class ShellOptionsImpl extends GWTCompilerOptionsImpl implements
      ShellOptions, WorkDirs {
    public File getCompilerOutputDir(ModuleDef moduleDef) {
      return new File(getOutDir(), moduleDef.getDeployTo());
    }

    public File getShellPublicGenDir(ModuleDef moduleDef) {
      return new File(getShellBaseWorkDir(moduleDef), "public");
    }

    /**
     * The base shell work directory.
     */
    protected File getShellBaseWorkDir(ModuleDef moduleDef) {
      return new File(new File(getWorkDir(), moduleDef.getName()), "shell");
    }

    /**
     * Where generated files go by default until we are sure they are public;
     * then they are copied into {@link #getShellPublicGenDir(ModuleDef)}.
     */
    protected File getShellPrivateGenDir(ModuleDef moduleDef) {
      return new File(getShellBaseWorkDir(moduleDef), "gen");
    }
  }

  private class BrowserWidgetHostImpl implements BrowserWidgetHost {
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
      TreeLogger.Type maxLevel = TreeLogger.INFO;
      if (mainLogger instanceof AbstractTreeLogger) {
        maxLevel = ((AbstractTreeLogger) mainLogger).getMaxDetail();
      }

      TreeLogger logger;
      ModulePanel tab;
      if (!headlessMode) {
        tab = new ModulePanel(maxLevel, moduleName, userAgent, remoteSocket,
            tabs);
        logger = tab.getLogger();

        // Switch to a wait cursor.
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
      } else {
        tab = null;
        logger = mainLogger;
      }

      try {
        // Try to find an existing loaded version of the module def.
        ModuleDef moduleDef = loadModule(moduleName, logger);
        assert (moduleDef != null);

        // Create a sandbox for the module.
        // TODO(jat): consider multiple instances of the same module open at
        // once
        File shellDir = new File(outDir, GWT_SHELL_PATH + File.separator
            + moduleName);

        TypeOracle typeOracle = moduleDef.getTypeOracle(logger);
        ShellModuleSpaceHost host = doCreateShellModuleSpaceHost(logger,
            typeOracle, moduleDef, genDir, shellDir);

        if (tab != null) {
          moduleTabs.put(host, tab);
        }
        return host;

      } finally {
        if (!headlessMode) {
          frame.setCursor(Cursor.getDefaultCursor());
        }
      }
    }

    public TreeLogger getLogger() {
      return getTopLogger();
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
    GWTShell shellMain = new GWTShell();
    if (shellMain.processArgs(args)) {
      shellMain.run();
    }
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

  protected File outDir;

  protected final ShellOptionsImpl options = new ShellOptionsImpl();

  /**
   * Cheat on the first load's refresh by assuming the module loaded by
   * {@link com.google.gwt.dev.shell.GWTShellServlet} is still fresh. This
   * prevents a double-refresh on startup. Subsequent refreshes will trigger a
   * real refresh.
   */
  private Set<String> alreadySeenModules = new HashSet<String>();

  private BrowserWidgetHostImpl browserHost = new BrowserWidgetHostImpl();

  private JFrame frame;

  private File genDir;

  private boolean headlessMode = false;

  private ShellMainWindow mainWnd;

  private int port;

  private int portHosted;

  private boolean runTomcat = true;

  private boolean started;

  private final List<String> startupUrls = new ArrayList<String>();

  private JTabbedPane tabs;

  private AbstractTreeLogger topLogger;

  private WebServerPanel webServerLog;

  public GWTShell() {
    this(false, false);
  }

  protected GWTShell(boolean forceServer, boolean noURLs) {
    // Set any platform specific system properties.
    BootStrapPlatform.initHostedMode();
    BootStrapPlatform.applyPlatformHacks();

    registerHandler(getArgHandlerPort());

    registerHandler(getArgHandlerPortHosted());

    if (!forceServer) {
      registerHandler(new ArgHandlerNoServerFlag());
    }

    registerHandler(new ArgHandlerWhitelist());
    registerHandler(new ArgHandlerBlacklist());

    registerHandler(new ArgHandlerLogLevel(options));

    registerHandler(new ArgHandlerGenDir(options));
    registerHandler(new ArgHandlerWorkDirOptional(options));

    if (!noURLs) {
      registerHandler(new ArgHandlerStartupURLs());
    }

    registerHandler(new ArgHandlerExtraDir(options));
    registerHandler(new ArgHandlerOutDir(options));

    registerHandler(new ArgHandlerScriptStyle(options));
    registerHandler(new ArgHandlerEnableAssertions(options));
    registerHandler(new ArgHandlerDisableAggressiveOptimization(options));
  }

  public void addStartupURL(String url) {
    startupUrls.add(url);
  }

  public File getGenDir() {
    return genDir;
  }

  public Type getLogLevel() {
    return options.getLogLevel();
  }

  public File getOutDir() {
    return outDir;
  }

  public int getPort() {
    return port;
  }

  public TreeLogger getTopLogger() {
    return topLogger;
  }

  /**
   * Launch the arguments as Urls in separate windows.
   */
  public void launchStartupUrls(final TreeLogger logger) {
    if (startupUrls != null) {
      // Launch a browser window for each startup url.
      String startupURL = "";
      try {
        for (String prenormalized : startupUrls) {
          startupURL = normalizeURL(prenormalized);
          logger.log(TreeLogger.INFO, "Starting URL: " + startupURL, null);
          launchURL(startupURL);
        }
      } catch (UnableToCompleteException e) {
        logger.log(TreeLogger.ERROR,
            "Unable to open new window for startup URL: " + startupURL, null);
      }
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

  public String normalizeURL(String unknownUrlText) {
    if (unknownUrlText.indexOf(":") != -1) {
      // Assume it's a full url.
      return unknownUrlText;
    }

    // Assume it's a trailing url path.
    //
    if (unknownUrlText.length() > 0 && unknownUrlText.charAt(0) == '/') {
      unknownUrlText = unknownUrlText.substring(1);
    }

    int prt = getPort();
    if (prt != 80 && prt != 0) {
      // CHECKSTYLE_OFF: Not really an assembled error message, so no space
      // after ':'.
      return "http://localhost:" + prt + "/" + unknownUrlText;
      // CHECKSTYLE_ON
    } else {
      return "http://localhost/" + unknownUrlText;
    }
  }

  /**
   * Sets up all the major aspects of running the shell graphically, including
   * creating the main window and optionally starting the embedded Tomcat
   * server.
   */
  public void run() {
    try {
      if (!startUp()) {
        // Failed to initialize.
        return;
      }

      // Perform any platform-specific hacks to initialize the GUI.
      BootStrapPlatform.initGui();

      // Tomcat's running now, so launch browsers for startup urls now.
      launchStartupUrls(getTopLogger());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void setCompilerOptions(CompilerOptions options) {
    this.options.copyFrom(options);
  }

  public void setGenDir(File genDir) {
    this.genDir = genDir;
  }

  public void setLogLevel(Type level) {
    options.setLogLevel(level);
  }

  public void setOutDir(File outDir) {
    this.outDir = outDir;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public void setRunTomcat(boolean run) {
    runTomcat = run;
  }

  /**
   * Compiles a logical module def. The caller can modify the specified module
   * def programmatically in some cases (this is needed for JUnit support, for
   * example).
   */
  protected void compile(TreeLogger logger, ModuleDef moduleDef)
      throws UnableToCompleteException {
    CompilerOptions newOptions = new GWTCompilerOptionsImpl(options);
    newOptions.setModuleName(moduleDef.getName());
    new GWTCompiler(newOptions).run(logger);
  }

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
   * Creates an instance of ShellModuleSpaceHost (or a derived class) using the
   * specified constituent parts. This method is made to be overridden for
   * subclasses that need to change the behavior of ShellModuleSpaceHost.
   * 
   * @param logger TreeLogger to use
   * @param typeOracle
   * @param moduleDef
   * @param genDir
   * @return ShellModuleSpaceHost instance
   */
  protected ShellModuleSpaceHost doCreateShellModuleSpaceHost(
      TreeLogger logger, TypeOracle typeOracle, ModuleDef moduleDef,
      File genDir, File shellDir) {
    // Clear out the shell temp directory.
    Util.recursiveDelete(options.getShellBaseWorkDir(moduleDef), true);
    return new ShellModuleSpaceHost(logger, typeOracle, moduleDef,
        options.getGenDir(), options.getShellPrivateGenDir(moduleDef),
        doCreateArtifactAcceptor(moduleDef));
  }

  /**
   * Can be override to change the default log level in subclasses. JUnit does
   * this for example.
   */
  protected Type doGetDefaultLogLevel() {
    return Type.INFO;
  }

  /**
   * Derived classes can override to prevent automatic update checking.
   */
  protected boolean doShouldCheckForUpdates() {
    return true;
  }

  /**
   * Derived classes can override to set a default port.
   */
  protected ArgHandlerPort getArgHandlerPort() {
    return new ArgHandlerPort();
  }

  /**
   * Derived classes can override to set a default port.
   */
  protected ArgHandlerPortHosted getArgHandlerPortHosted() {
    return new ArgHandlerPortHosted();
  }

  protected BrowserWidgetHost getBrowserHost() {
    return browserHost;
  }

  protected void initializeLogger() {
    if (mainWnd != null) {
      topLogger = mainWnd.getLogger();
    } else {
      topLogger = new PrintWriterTreeLogger(new PrintWriter(System.out));
    }
    topLogger.setMaxDetail(options.getLogLevel());
  }

  /**
   * By default we will open the application window.
   * 
   * @return true if we are running in headless mode
   */
  protected boolean isHeadless() {
    return headlessMode;
  }

  protected void setHeadless(boolean headlessMode) {
    this.headlessMode = headlessMode;
  }

  /**
   * 
   */
  protected void shutDown() {
    if (!runTomcat) {
      return;
    }

    // Stop the HTTP server.
    //
    EmbeddedTomcatServer.stop();
  }

  protected boolean startUp() {
    if (started) {
      throw new IllegalStateException("Startup code has already been run");
    }

    started = true;

    // Create the main app window.
    // When it is up and running, it will start the Tomcat server if desired.
    if (!headlessMode) {
      openAppWindow();
    }

    // Initialize the logger.
    initializeLogger();

    // Accept connections from OOPHM clients
    startOophmListener();

    if (runTomcat) {
      // Start the HTTP server.
      // Use a new thread so that logging that occurs during startup is
      // displayed immediately.
      //
      final int serverPort = getPort();

      PerfLogger.start("GWTShell.startup (Tomcat launch)");
      String whyFailed = EmbeddedTomcatServer.start(headlessMode
          ? getTopLogger() : webServerLog.getLogger(), serverPort, options);
      PerfLogger.end();

      if (whyFailed != null) {
        System.err.println(whyFailed);
        return false;
      }

      // Record what port Tomcat is actually running on.
      port = EmbeddedTomcatServer.getPort();
    }

    return true;
  }

  @SuppressWarnings("unused")
  // TODO(jat): implement and hook into UI, this is just copied from trunk
  private void compile() {
    // // first, compile
    // Set<String> keySet = new HashSet<String>();
    // for (Map.Entry<?, ModuleSpace> entry : loadedModules.entrySet()) {
    // ModuleSpace module = entry.getValue();
    // keySet.add(module.getModuleName());
    // }
    // String[] moduleNames = Util.toStringArray(keySet);
    // if (moduleNames.length == 0) {
    // // A latent problem with a module.
    // //
    // openWebModeButton.setEnabled(false);
    // return;
    // }
    // try {
    // Cursor waitCursor = getDisplay().getSystemCursor(SWT.CURSOR_WAIT);
    // getShell().setCursor(waitCursor);
    // getHost().compile(moduleNames);
    // } catch (UnableToCompleteException e) {
    // // Already logged by callee.
    // //
    // MessageBox msgBox = new MessageBox(getShell(), SWT.OK
    // | SWT.ICON_ERROR);
    // msgBox.setText("Compilation Failed");
    // msgBox.setMessage("Compilation failed. Please see the log in the
    // development shell for details.");
    // msgBox.open();
    // return;
    // } finally {
    // // Restore the cursor.
    // //
    // Cursor normalCursor = getDisplay().getSystemCursor(SWT.CURSOR_ARROW);
    // getShell().setCursor(normalCursor);
    // }
    //
    // String locationText = location.getText();
    //
    // launchExternalBrowser(logger, locationText);
  }

  private void openAppWindow() {
    ImageIcon gwtIcon = loadImageIcon("icon24.png");
    frame = new JFrame("GWT Hosted Mode");
    tabs = new JTabbedPane();
    boolean checkForUpdates = doShouldCheckForUpdates();
    mainWnd = new ShellMainWindow(this, checkForUpdates, options.getLogLevel());
    tabs.addTab("Hosted Mode", gwtIcon, mainWnd, "GWT Hosted-mode");
    if (runTomcat) {
      ImageIcon tomcatIcon = loadImageIcon("tomcat24.png");
      webServerLog = new WebServerPanel(getPort(), options.getLogLevel());
      tabs.addTab("Tomcat", tomcatIcon, webServerLog);
    }
    frame.getContentPane().add(tabs);
    frame.setSize(950, 700);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setIconImage(loadImageIcon("icon16.png").getImage());
    frame.setVisible(true);
  }

  private void startOophmListener() {
    listener = new BrowserListener(getTopLogger(), portHosted,
        new OophmSessionHandler(browserHost));
    listener.start();
  }
}
