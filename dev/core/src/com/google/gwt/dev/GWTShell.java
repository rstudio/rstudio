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
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.ModuleDefLoader;
import com.google.gwt.dev.jjs.JJSOptions;
import com.google.gwt.dev.shell.BrowserWidget;
import com.google.gwt.dev.shell.BrowserWidgetHost;
import com.google.gwt.dev.shell.BrowserWidgetHostChecker;
import com.google.gwt.dev.shell.LowLevel;
import com.google.gwt.dev.shell.ModuleSpaceHost;
import com.google.gwt.dev.shell.PlatformSpecific;
import com.google.gwt.dev.shell.ShellMainWindow;
import com.google.gwt.dev.shell.ShellModuleSpaceHost;
import com.google.gwt.dev.shell.tomcat.EmbeddedTomcatServer;
import com.google.gwt.dev.util.PerfLogger;
import com.google.gwt.dev.util.arg.ArgHandlerGenDir;
import com.google.gwt.dev.util.arg.ArgHandlerLogLevel;
import com.google.gwt.dev.util.arg.ArgHandlerScriptStyle;
import com.google.gwt.dev.util.log.AbstractTreeLogger;
import com.google.gwt.util.tools.ArgHandlerDisableAggressiveOptimization;
import com.google.gwt.util.tools.ArgHandlerEnableAssertions;
import com.google.gwt.util.tools.ArgHandlerExtra;
import com.google.gwt.util.tools.ArgHandlerFlag;
import com.google.gwt.util.tools.ArgHandlerOutDir;
import com.google.gwt.util.tools.ArgHandlerString;
import com.google.gwt.util.tools.ToolBase;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.internal.Library;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
          String msg = "A port must be an integer or \"auto\"";
          getTopLogger().log(TreeLogger.ERROR, msg, null);
          return false;
        }
      }
      return true;
    }
  }

  /**
   * handles the -saveJsni command line flag.
   */
  protected class ArgHandlerSaveJsni extends ArgHandlerFlag {
    @Override
    public String getPurpose() {
      return "Save generated JSNI source in the supplied gen directory (if any)";
    }

    @Override
    public String getTag() {
      return "-saveJsni";
    }

    @Override
    public boolean setFlag() {
      saveJsni = true;
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

  private class BrowserWidgetHostImpl implements BrowserWidgetHost {
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
        final String moduleName) throws UnableToCompleteException {
      TreeLogger logger = getLogger();

      // Switch to a wait cursor.
      //
      Shell widgetShell = widget.getShell();
      try {
        Cursor waitCursor = display.getSystemCursor(SWT.CURSOR_WAIT);
        widgetShell.setCursor(waitCursor);

        // Try to find an existing loaded version of the module def.
        //
        ModuleDef moduleDef = loadModule(moduleName, logger);
        assert (moduleDef != null);

        // Create a sandbox for the module.
        //
        File shellDir = new File(outDir, GWT_SHELL_PATH + File.separator
            + moduleName);

        TypeOracle typeOracle = moduleDef.getTypeOracle(logger);
        ShellModuleSpaceHost host = doCreateShellModuleSpaceHost(logger,
            typeOracle, moduleDef, genDir, shellDir);
        return host;
      } finally {
        Cursor normalCursor = display.getSystemCursor(SWT.CURSOR_ARROW);
        widgetShell.setCursor(normalCursor);
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
      return GWTShell.this.openNewBrowserWindow();
    }

    /**
     * Load a module.
     * 
     * @param moduleName name of the module to load
     * @param logger TreeLogger to use
     * @return the loaded module
     * @throws UnableToCompleteException
     */
    private ModuleDef loadModule(final String moduleName, TreeLogger logger)
        throws UnableToCompleteException {
      ModuleDef moduleDef = doLoadModule(logger, moduleName);
      assert (moduleDef != null) : "Required module state is absent";
      return moduleDef;
    }
  }

  public static final String GWT_SHELL_PATH = ".gwt-tmp" + File.separator
      + "shell";

  private static Image[] icons;

  static {
    // Correct menu on Mac OS X
    Display.setAppName("GWT");
  }

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
    // the enture URL up to the first slash not prefixed by a slash or colon.
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

  /**
   * Well-known place to get the GWT icons.
   */
  public static Image[] getIcons() {
    // Make sure icon images are loaded.
    //
    if (icons == null) {
      icons = new Image[] {
          LowLevel.loadImage("icon16.png"), LowLevel.loadImage("icon24.png"),
          LowLevel.loadImage("icon32.png"), LowLevel.loadImage("icon48.png"),
          LowLevel.loadImage("icon128.png")};
    }
    return icons;
  }

  public static void main(String[] args) {
    /*
     * NOTE: main always exits with a call to System.exit to terminate any
     * non-daemon threads that were started in Generators. Typically, this is to
     * shutdown AWT related threads, since the contract for their termination is
     * still implementation-dependent.
     */
    BootStrapPlatform.init();
    GWTShell shellMain = new GWTShell();
    if (shellMain.processArgs(args)) {
      shellMain.run();
    }
    System.exit(0);
  }

  /**
   * Use the default display; constructing a new one would make instantiating
   * multiple GWTShells fail with a mysterious exception.
   */
  protected final Display display = Display.getDefault();

  protected File outDir;

  private BrowserWidgetHostImpl browserHost = new BrowserWidgetHostImpl();

  private final List<Shell> browserShells = new ArrayList<Shell>();

  private File genDir;

  private boolean headlessMode = false;

  private final JJSOptions jjsOptions = new JJSOptions();

  private TreeLogger.Type logLevel;

  private ShellMainWindow mainWnd;

  private int port;

  private boolean runTomcat = true;

  private boolean saveJsni = false;

  private boolean started;

  private final List<String> startupUrls = new ArrayList<String>();

  public GWTShell() {
    this(false, false);
  }

  protected GWTShell(boolean forceServer, boolean noURLs) {
    registerHandler(getArgHandlerPort());

    if (!forceServer) {
      registerHandler(new ArgHandlerNoServerFlag());
    }

    registerHandler(new ArgHandlerWhitelist());
    registerHandler(new ArgHandlerBlacklist());

    registerHandler(new ArgHandlerLogLevel() {
      @Override
      protected Type getDefaultLogLevel() {
        return doGetDefaultLogLevel();
      }

      @Override
      public void setLogLevel(Type level) {
        logLevel = level;
      }
    });

    registerHandler(new ArgHandlerGenDir() {
      @Override
      public void setDir(File dir) {
        genDir = dir;
      }
    });

    registerHandler(new ArgHandlerSaveJsni());

    if (!noURLs) {
      registerHandler(new ArgHandlerStartupURLs());
    }

    registerHandler(new ArgHandlerOutDir() {
      @Override
      public void setDir(File dir) {
        outDir = dir;
      }
    });

    registerHandler(new ArgHandlerScriptStyle(jjsOptions));

    registerHandler(new ArgHandlerEnableAssertions(jjsOptions));

    registerHandler(new ArgHandlerDisableAggressiveOptimization() {
      @Override
      public boolean setFlag() {
        jjsOptions.setAggressivelyOptimize(false);
        return true;
      }
    });
  }

  public void addStartupURL(String url) {
    startupUrls.add(url);
  }

  public void closeAllBrowserWindows() {
    while (!browserShells.isEmpty()) {
      browserShells.get(0).dispose();
    }
  }

  public File getGenDir() {
    return genDir;
  }

  public Type getLogLevel() {
    return logLevel;
  }

  public File getOutDir() {
    return outDir;
  }

  public int getPort() {
    return port;
  }

  public TreeLogger getTopLogger() {
    return mainWnd.getLogger();
  }

  public boolean hasBrowserWindowsOpen() {
    if (browserShells.isEmpty()) {
      return false;
    } else {
      return true;
    }
  }

  /**
   * Launch the arguments as Urls in separate windows.
   */
  public void launchStartupUrls(final TreeLogger logger) {
    if (startupUrls != null) {
      // Launch a browser window for each startup url.
      //
      String startupURL = "";
      try {
        for (String prenormalized : startupUrls) {
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
   * Called directly by ShellMainWindow and indirectly via BrowserWidgetHost.
   */
  public BrowserWidget openNewBrowserWindow() throws UnableToCompleteException {
    boolean succeeded = false;
    Shell s = createTrackedBrowserShell();
    try {
      BrowserWidget bw = PlatformSpecific.createBrowserWidget(getTopLogger(),
          s, browserHost);

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

  /**
   * Sets up all the major aspects of running the shell graphically, including
   * creating the main window and optionally starting the embedded Tomcat
   * server.
   */
  public void run() {
    try {
      // Set any platform specific system properties.
      BootStrapPlatform.applyPlatformHacks();

      if (!startUp()) {
        // Failed to initalize.
        return;
      }

      // Eager AWT initialization for OS X to ensure safe coexistence with SWT.
      BootStrapPlatform.maybeInitializeAWT();

      // Tomcat's running now, so launch browsers for startup urls now.
      launchStartupUrls(getTopLogger());

      pumpEventLoop();

      shutDown();

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void setCompilerOptions(JJSOptions options) {
    jjsOptions.copyFrom(options);
  }

  public void setGenDir(File genDir) {
    this.genDir = genDir;
  }

  public void setLogLevel(Type level) {
    this.logLevel = level;
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
    GWTCompiler compiler = new GWTCompiler(moduleDef.getCacheManager());
    compiler.setCompilerOptions(jjsOptions);
    compiler.setGenDir(genDir);
    compiler.setOutDir(outDir);
    compiler.setModuleName(moduleDef.getName());
    compiler.setLogLevel(logLevel);
    compiler.distill(logger, moduleDef);
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
    return new ShellModuleSpaceHost(logger, typeOracle, moduleDef, genDir,
        shellDir, saveJsni);
  }

  /**
   * Can be override to change the default log level in subclasses. JUnit does
   * this for example.
   */
  protected Type doGetDefaultLogLevel() {
    return Type.INFO;
  }

  /**
   * Loads a named module. This method can be overridden if the module def needs
   * to be tweaked (or even created) programmatically -- JUnit integration does
   * this, for example.
   */
  protected ModuleDef doLoadModule(TreeLogger logger, final String moduleName)
      throws UnableToCompleteException {
    return ModuleDefLoader.loadFromClassPath(logger, moduleName);
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

  protected BrowserWidgetHost getBrowserHost() {
    return browserHost;
  }

  protected void initializeLogger() {
    final AbstractTreeLogger logger = mainWnd.getLogger();
    logger.setMaxDetail(logLevel);
  }

  /**
   * By default we will open the application window.
   * 
   * @return true if we are running in headless mode
   */
  protected boolean isHeadless() {
    return headlessMode;
  }

  protected boolean notDone() {
    if (!mainWnd.isDisposed()) {
      return true;
    }
    if (!browserShells.isEmpty()) {
      return true;
    }
    return false;
  }

  /**
   * 
   */
  protected void pumpEventLoop() {
    TreeLogger logger = getTopLogger();

    // Run the event loop. When there are no open shells, quit.
    //
    while (notDone()) {
      try {
        if (!display.readAndDispatch()) {
          sleep();
        }
      } catch (Throwable e) {
        String msg = e.getMessage();
        msg = (msg != null ? msg : e.getClass().getName());
        logger.log(TreeLogger.ERROR, msg, e);
      }
    }
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

  protected void sleep() {
    display.sleep();
  }

  protected boolean startUp() {
    if (started) {
      throw new IllegalStateException("Startup code has already been run");
    }

    started = true;

    loadRequiredNativeLibs();

    // Create the main app window.
    // When it is up and running, it will start the Tomcat server if desired.
    //
    openAppWindow();

    // Initialize the logger.
    //
    initializeLogger();

    if (runTomcat) {
      // Start the HTTP server.
      // Use a new thread so that logging that occurs during startup is
      // displayed immediately.
      //
      final int serverPort = getPort();

      PerfLogger.start("GWTShell.startup (Tomcat launch)");
      String whyFailed = EmbeddedTomcatServer.start(getTopLogger(), serverPort,
          outDir);
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

    shell.setImages(getIcons());

    return shell;
  }

  private void loadRequiredNativeLibs() {
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

  private void openAppWindow() {
    final Shell shell = new Shell(display);

    FillLayout fillLayout = new FillLayout();
    fillLayout.marginWidth = 0;
    fillLayout.marginHeight = 0;
    shell.setLayout(fillLayout);

    shell.setImages(getIcons());

    boolean checkForUpdates = doShouldCheckForUpdates();

    mainWnd = new ShellMainWindow(this, shell, runTomcat ? getPort() : 0,
        checkForUpdates);

    shell.setSize(700, 600);
    if (!isHeadless()) {
      shell.open();
    }
  }
}
