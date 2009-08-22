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
import com.google.gwt.dev.Precompile.PrecompileOptionsImpl;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.ModuleDefLoader;
import com.google.gwt.dev.jjs.JJSOptions;
import com.google.gwt.dev.shell.ArtifactAcceptor;
import com.google.gwt.dev.shell.BrowserWidget;
import com.google.gwt.dev.shell.BrowserWidgetHost;
import com.google.gwt.dev.shell.BrowserWidgetHostChecker;
import com.google.gwt.dev.shell.BrowserWindowController;
import com.google.gwt.dev.shell.CheckForUpdates;
import com.google.gwt.dev.shell.ModuleSpaceHost;
import com.google.gwt.dev.shell.ShellModuleSpaceHost;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.arg.ArgHandlerDisableAggressiveOptimization;
import com.google.gwt.dev.util.arg.ArgHandlerDisableCastChecking;
import com.google.gwt.dev.util.arg.ArgHandlerDisableClassMetadata;
import com.google.gwt.dev.util.arg.ArgHandlerDraftCompile;
import com.google.gwt.dev.util.arg.ArgHandlerEnableAssertions;
import com.google.gwt.dev.util.arg.ArgHandlerGenDir;
import com.google.gwt.dev.util.arg.ArgHandlerLogLevel;
import com.google.gwt.dev.util.arg.ArgHandlerScriptStyle;
import com.google.gwt.dev.util.arg.OptionGenDir;
import com.google.gwt.dev.util.arg.OptionLogLevel;
import com.google.gwt.util.tools.ArgHandlerFlag;
import com.google.gwt.util.tools.ArgHandlerString;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The main executable class for the hosted mode shell. This class must not have
 * any GUI dependencies.
 * 
 * TODO: remove BrowserWidget references (which reference SWT via inheritance,
 * though it doesn't appear to cause any harm currently.
 */
abstract class HostedModeBase implements BrowserWindowController {

  /**
   * Handles the -blacklist command line argument.
   */
  protected static class ArgHandlerBlacklist extends ArgHandlerString {
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
   * Handles the -noserver command line flag.
   */
  protected static class ArgHandlerNoServerFlag extends ArgHandlerFlag {
    private final OptionNoServer options;

    public ArgHandlerNoServerFlag(OptionNoServer options) {
      this.options = options;
    }

    @Override
    public String getPurpose() {
      return "Prevents the embedded web server from running";
    }

    @Override
    public String getTag() {
      return "-noserver";
    }

    @Override
    public boolean setFlag() {
      options.setNoServer(true);
      return true;
    }
  }

  /**
   * Handles the -port command line flag.
   */
  protected static class ArgHandlerPort extends ArgHandlerString {

    private final OptionPort options;

    public ArgHandlerPort(OptionPort options) {
      this.options = options;
    }

    @Override
    public String[] getDefaultArgs() {
      return new String[] {getTag(), "8888"};
    }

    @Override
    public String getPurpose() {
      return "Specifies the TCP port for the embedded web server (defaults to 8888)";
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
        options.setPort(0);
      } else {
        try {
          options.setPort(Integer.parseInt(value));
        } catch (NumberFormatException e) {
          System.err.println("A port must be an integer or \"auto\"");
          return false;
        }
      }
      return true;
    }
  }

  /**
   * Handles the -whitelist command line flag.
   */
  protected static class ArgHandlerWhitelist extends ArgHandlerString {
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

  protected abstract class BrowserWidgetHostImpl implements BrowserWidgetHost {

    public void compile() throws UnableToCompleteException {
      if (isLegacyMode()) {
        throw new UnsupportedOperationException();
      }
      HostedModeBase.this.compile(getLogger());
    }

    @Deprecated
    public void compile(String[] moduleNames) throws UnableToCompleteException {
      if (!isLegacyMode()) {
        throw new UnsupportedOperationException();
      }
      for (int i = 0; i < moduleNames.length; i++) {
        String moduleName = moduleNames[i];
        ModuleDef moduleDef = loadModule(getLogger(), moduleName, true);
        HostedModeBase.this.compile(getLogger(), moduleDef);
      }
    }

    public abstract ModuleSpaceHost createModuleSpaceHost(TreeLogger logger,
        BrowserWidget widget, String moduleName)
        throws UnableToCompleteException;

    public TreeLogger getLogger() {
      return getTopLogger();
    }

    public boolean initModule(String moduleName) {
      return HostedModeBase.this.initModule(moduleName);
    }

    @Deprecated
    public boolean isLegacyMode() {
      return HostedModeBase.this instanceof GWTShell;
    }

    public String normalizeURL(String whatTheUserTyped) {
      return HostedModeBase.this.normalizeURL(whatTheUserTyped);
    }

    public BrowserWidget openNewBrowserWindow()
        throws UnableToCompleteException {
      return HostedModeBase.this.openNewBrowserWindow();
    }
  }

  protected interface HostedModeBaseOptions extends JJSOptions, OptionLogLevel,
      OptionGenDir, OptionNoServer, OptionPort, OptionStartupURLs {

    /**
     * The base shell work directory.
     */
    File getShellBaseWorkDir(ModuleDef moduleDef);
  }

  /**
   * Concrete class to implement all hosted mode base options.
   */
  protected static class HostedModeBaseOptionsImpl extends
      PrecompileOptionsImpl implements HostedModeBaseOptions {

    private boolean isNoServer;
    private int port;
    private final List<String> startupURLs = new ArrayList<String>();

    public void addStartupURL(String url) {
      startupURLs.add(url);
    }

    public int getPort() {
      return port;
    }

    public File getShellBaseWorkDir(ModuleDef moduleDef) {
      return new File(new File(getWorkDir(), moduleDef.getName()), "shell");
    }

    public List<String> getStartupURLs() {
      return Collections.unmodifiableList(startupURLs);
    }

    public boolean isNoServer() {
      return isNoServer;
    }

    public void setNoServer(boolean isNoServer) {
      this.isNoServer = isNoServer;
    }

    public void setPort(int port) {
      this.port = port;
    }
  }

  /**
   * Controls whether to run a server or not.
   * 
   */
  protected interface OptionNoServer {
    boolean isNoServer();

    void setNoServer(boolean isNoServer);
  }

  /**
   * Controls what port to use.
   * 
   */
  protected interface OptionPort {
    int getPort();

    void setPort(int port);
  }

  /**
   * Controls the startup URLs.
   */
  protected interface OptionStartupURLs {
    void addStartupURL(String url);

    List<String> getStartupURLs();
  }

  abstract static class ArgProcessor extends ArgProcessorBase {
    public ArgProcessor(HostedModeBaseOptions options, boolean forceServer) {
      if (!forceServer) {
        registerHandler(new ArgHandlerNoServerFlag(options));
      }
      registerHandler(new ArgHandlerPort(options));
      registerHandler(new ArgHandlerWhitelist());
      registerHandler(new ArgHandlerBlacklist());
      registerHandler(new ArgHandlerLogLevel(options));
      registerHandler(new ArgHandlerGenDir(options));
      registerHandler(new ArgHandlerScriptStyle(options));
      registerHandler(new ArgHandlerEnableAssertions(options));
      registerHandler(new ArgHandlerDisableAggressiveOptimization(options));
      registerHandler(new ArgHandlerDisableClassMetadata(options));
      registerHandler(new ArgHandlerDisableCastChecking(options));
      registerHandler(new ArgHandlerDraftCompile(options));
    }
  }

  public static String normalizeURL(String unknownUrlText, int port, String host) {
    if (unknownUrlText.indexOf(":") != -1) {
      // Assume it's a full url.
      return unknownUrlText;
    }

    // Assume it's a trailing url path.
    if (unknownUrlText.length() > 0 && unknownUrlText.charAt(0) == '/') {
      unknownUrlText = unknownUrlText.substring(1);
    }

    if (port != 80) {
      // CHECKSTYLE_OFF: Not really an assembled error message, so no space
      // after ':'.
      return "http://" + host + ":" + port + "/" + unknownUrlText;
      // CHECKSTYLE_ON
    } else {
      return "http://" + host + "/" + unknownUrlText;
    }
  }

  protected final HostedModeBaseOptions options;

  /**
   * Cheat on the first load's refresh by assuming the module loaded by
   * {@link com.google.gwt.dev.shell.GWTShellServlet} is still fresh. This
   * prevents a double-refresh on startup. Subsequent refreshes will trigger a
   * real refresh.
   */
  private Set<String> alreadySeenModules = new HashSet<String>();

  private boolean headlessMode = false;

  private boolean started;

  public HostedModeBase() {
    // Set any platform specific system properties.
    BootStrapPlatform.initHostedMode();
    BootStrapPlatform.applyPlatformHacks();
    options = createOptions();
  }

  public final void addStartupURL(String url) {
    options.addStartupURL(url);
  }

  public abstract void closeAllBrowserWindows();

  public final int getPort() {
    return options.getPort();
  }

  public abstract TreeLogger getTopLogger();

  public abstract boolean hasBrowserWindowsOpen();

  /**
   * Launch the arguments as Urls in separate windows.
   */
  public abstract void launchStartupUrls(final TreeLogger logger);

  public final String normalizeURL(String unknownUrlText) {
    return normalizeURL(unknownUrlText, getPort(), getHost());
  }

  /**
   * Sets up all the major aspects of running the shell graphically, including
   * creating the main window and optionally starting the embedded Tomcat
   * server.
   */
  public final void run() {
    try {
      if (startUp()) {
        // Eager AWT init for OS X to ensure safe coexistence with SWT.
        BootStrapPlatform.initGui();

        // Tomcat's running now, so launch browsers for startup urls now.
        launchStartupUrls(getTopLogger());
      }

      pumpEventLoop();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      shutDown();
    }
  }

  public final void setPort(int port) {
    options.setPort(port);
  }

  public final void setRunTomcat(boolean run) {
    options.setNoServer(!run);
  }

  /**
   * Derived classes can override to lengthen ping delay.
   */
  protected long checkForUpdatesInterval() {
    return CheckForUpdates.ONE_MINUTE;
  }

  /**
   * Compiles all modules.
   * 
   * @throws UnableToCompleteException
   */
  protected abstract void compile(TreeLogger logger)
      throws UnableToCompleteException;

  /**
   * Compiles a module (legacy only).
   */
  @Deprecated
  protected abstract void compile(TreeLogger logger, ModuleDef moduleDef)
      throws UnableToCompleteException;

  protected abstract HostedModeBaseOptions createOptions();

  protected abstract ArtifactAcceptor doCreateArtifactAcceptor(ModuleDef module);

  /**
   * Creates an instance of ShellModuleSpaceHost (or a derived class) using the
   * specified constituent parts. This method is made to be overridden for
   * subclasses that need to change the behavior of ShellModuleSpaceHost.
   * 
   * @param logger TreeLogger to use
   * @param typeOracle
   * @param moduleDef
   * @return ShellModuleSpaceHost instance
   */
  protected final ShellModuleSpaceHost doCreateShellModuleSpaceHost(
      TreeLogger logger, TypeOracle typeOracle, ModuleDef moduleDef) {
    // Clear out the shell temp directory.
    Util.recursiveDelete(options.getShellBaseWorkDir(moduleDef), true);
    return new ShellModuleSpaceHost(logger, typeOracle, moduleDef,
        options.getGenDir(), new File(options.getShellBaseWorkDir(moduleDef),
            "gen"), doCreateArtifactAcceptor(moduleDef));
  }

  protected abstract void doShutDownServer();

  protected boolean doStartup() {
    loadRequiredNativeLibs();

    // Create the main app window.
    openAppWindow();

    // Initialize the logger.
    //
    initializeLogger();
    
    // Check for updates
    final TreeLogger logger = getTopLogger();
    final CheckForUpdates updateChecker
        = CheckForUpdates.createUpdateChecker(logger);
    if (updateChecker != null) {
      Thread checkerThread = new Thread("GWT Update Checker") {
        @Override
        public void run() {
          CheckForUpdates.logUpdateAvailable(logger,
              updateChecker.check(checkForUpdatesInterval()));
        }
      };
      checkerThread.setDaemon(true);
      checkerThread.start();
    }
    return true;
  }

  protected abstract int doStartUpServer();

  protected String getHost() {
    return "localhost";
  }

  protected abstract void initializeLogger();

  /**
   * Called from a selection script as it begins to load in hosted mode. This
   * triggers a hosted mode link, which might actually update the running
   * selection script.
   * 
   * @param moduleName the module to link
   * @return <code>true</code> if the selection script was overwritten; this
   *         will trigger a full-page refresh by the calling (out of date)
   *         selection script
   */
  protected abstract boolean initModule(String moduleName);

  /**
   * By default we will open the application window.
   * 
   * @return true if we are running in headless mode
   */
  protected final boolean isHeadless() {
    return headlessMode;
  }

  /**
   * Load a module.
   * 
   * @param moduleName name of the module to load
   * @param logger TreeLogger to use
   * @param refresh if <code>true</code>, refresh the module from disk
   * @return the loaded module
   * @throws UnableToCompleteException
   */
  protected ModuleDef loadModule(TreeLogger logger, String moduleName,
      boolean refresh) throws UnableToCompleteException {
    refresh &= alreadySeenModules.contains(moduleName);
    ModuleDef moduleDef = ModuleDefLoader.loadFromClassPath(logger, moduleName,
        refresh);
    alreadySeenModules.add(moduleName);
    assert (moduleDef != null) : "Required module state is absent";
    return moduleDef;
  }

  protected abstract void loadRequiredNativeLibs();

  protected abstract boolean notDone();

  protected abstract void openAppWindow();

  protected abstract void processEvents() throws Exception;

  protected final void pumpEventLoop() {
    TreeLogger logger = getTopLogger();

    // Run the event loop. When there are no open shells, quit.
    //
    while (notDone()) {
      try {
        processEvents();
      } catch (Throwable e) {
        String msg = e.getMessage();
        msg = (msg != null ? msg : e.getClass().getName());
        logger.log(TreeLogger.ERROR, msg, e);
      }
    }
  }

  protected final void setHeadless(boolean headlessMode) {
    this.headlessMode = headlessMode;
  }

  protected final void shutDown() {
    if (options.isNoServer()) {
      return;
    }
    doShutDownServer();
  }

  protected final boolean startUp() {
    if (started) {
      throw new IllegalStateException("Startup code has already been run");
    }

    started = true;

    if (!doStartup()) {
      return false;
    }

    if (!options.isNoServer()) {
      int resultPort = doStartUpServer();
      if (resultPort < 0) {
        return false;
      }
      options.setPort(resultPort);
      getTopLogger().log(TreeLogger.INFO, "Started web server on port "
          + resultPort);
    }

    return true;
  }
}
