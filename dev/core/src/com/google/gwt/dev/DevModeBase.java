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
import com.google.gwt.core.ext.TreeLogger.HelpInfo;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.impl.StandardLinkerContext;
import com.google.gwt.dev.Precompile.PrecompileOptionsImpl;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.ModuleDefLoader;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.jjs.JJSOptions;
import com.google.gwt.dev.shell.ArtifactAcceptor;
import com.google.gwt.dev.shell.BrowserChannelServer;
import com.google.gwt.dev.shell.BrowserListener;
import com.google.gwt.dev.shell.BrowserWidgetHost;
import com.google.gwt.dev.shell.BrowserWidgetHostChecker;
import com.google.gwt.dev.shell.CheckForUpdates;
import com.google.gwt.dev.shell.ModuleSpaceHost;
import com.google.gwt.dev.shell.OophmSessionHandler;
import com.google.gwt.dev.shell.ShellModuleSpaceHost;
import com.google.gwt.dev.shell.remoteui.RemoteUI;
import com.google.gwt.dev.ui.DevModeUI;
import com.google.gwt.dev.ui.DoneCallback;
import com.google.gwt.dev.ui.DoneEvent;
import com.google.gwt.dev.ui.DevModeUI.ModuleHandle;
import com.google.gwt.dev.util.BrowserInfo;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.arg.ArgHandlerGenDir;
import com.google.gwt.dev.util.arg.ArgHandlerLogLevel;
import com.google.gwt.dev.util.arg.OptionGenDir;
import com.google.gwt.dev.util.arg.OptionLogLevel;
import com.google.gwt.util.tools.ArgHandlerFlag;
import com.google.gwt.util.tools.ArgHandlerString;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The main executable class for the hosted mode shell. This class must not have
 * any GUI dependencies.
 */
abstract class DevModeBase implements DoneCallback {

  /**
   * Implementation of BrowserWidgetHost that supports the abstract UI
   * interface.
   */
  public class UiBrowserWidgetHostImpl implements BrowserWidgetHost {

    public ModuleSpaceHost createModuleSpaceHost(TreeLogger mainLogger,
        String moduleName, String userAgent, String url, String tabKey,
        String sessionKey, BrowserChannelServer serverChannel,
        byte[] userAgentIcon) throws UnableToCompleteException {
      if (sessionKey == null) {
        // if we don't have a unique session key, make one up
        sessionKey = randomString();
      }
      TreeLogger logger = mainLogger;
      TreeLogger.Type maxLevel = options.getLogLevel();
      String agentTag = BrowserInfo.getShortName(userAgent);
      String remoteSocket = serverChannel.getRemoteEndpoint();
      ModuleHandle module = ui.loadModule(userAgent, remoteSocket, url, tabKey,
          moduleName, sessionKey, agentTag, userAgentIcon, maxLevel);
      // TODO(jat): add support for closing an active module
      logger = module.getLogger();
      try {
        // Try to find an existing loaded version of the module def.
        ModuleDef moduleDef = loadModule(logger, moduleName, true);
        assert (moduleDef != null);
        // Release the hard reference to the module if it is present.
        startupModules.remove(moduleDef.getName());

        ShellModuleSpaceHost host = doCreateShellModuleSpaceHost(logger,
            moduleDef.getCompilationState(logger), moduleDef);

        loadedModules.put(host, module);
        return host;
      } catch (RuntimeException e) {
        logger.log(TreeLogger.ERROR, "Exception initializing module", e);
        ui.unloadModule(module);
        throw e;
      }
    }

    public TreeLogger getLogger() {
      return getTopLogger();
    }

    public void unloadModule(ModuleSpaceHost moduleSpaceHost) {
      ModuleHandle module = loadedModules.remove(moduleSpaceHost);
      if (module != null) {
        ui.unloadModule(module);
      }
    }
  }

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
   * Handles the -logdir command line option.
   */
  protected static class ArgHandlerLogDir extends ArgHandlerString {
    private final OptionLogDir options;

    public ArgHandlerLogDir(OptionLogDir options) {
      this.options = options;
    }

    @Override
    public String getPurpose() {
      return "Logs to a file in the given directory, as well as graphically";
    }

    @Override
    public String getTag() {
      return "-logdir";
    }

    @Override
    public String[] getTagArgs() {
      return new String[] {"directory"};
    }

    @Override
    public boolean setString(String value) {
      options.setLogFile(value);
      return true;
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
        options.setPort(getFreeSocketPort());
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
   * Handles the -portHosted command line flag.
   */
  protected static class ArgHandlerCodeServerPort extends ArgHandlerString {

    private static final String CODE_SERVER_PORT_TAG = "-codeServerPort";
    private static final String DEFAULT_PORT = "9997";
    
    private final OptionCodeServerPort options;

    public ArgHandlerCodeServerPort(OptionCodeServerPort options) {
      this.options = options;
    }

    @Override
    public String[] getDefaultArgs() {
      return new String[] {CODE_SERVER_PORT_TAG, DEFAULT_PORT};
    }

    @Override
    public String getPurpose() {
      return "Specifies the TCP port for the code server (defaults to " + 
        DEFAULT_PORT + ")";
    }

    @Override
    public String getTag() {
      return CODE_SERVER_PORT_TAG;
    }

    @Override
    public String[] getTagArgs() {
      return new String[] {"port-number | \"auto\""};
    }

    @Override
    public boolean setString(String value) {
      if (value.equals("auto")) {
        options.setCodeServerPort(getFreeSocketPort());
      } else {
        try {
          options.setCodeServerPort(Integer.parseInt(value));
        } catch (NumberFormatException e) {
          System.err.println("A port must be an integer or \"auto\"");
          return false;
        }
      }
      return true;
    }
  }

  protected static class ArgHandlerRemoteUI extends ArgHandlerString {

    private final HostedModeBaseOptions options;

    public ArgHandlerRemoteUI(HostedModeBaseOptions options) {
      this.options = options;
    }

    @Override
    public String getPurpose() {
      return "Sends Development Mode UI event information to the specified host and port.";
    }

    @Override
    public String getTag() {
      return "-remoteUI";
    }

    @Override
    public String[] getTagArgs() {
      return new String[] {"port-number:client-id-string | host-string:port-number:client-id-string"};
    }

    @Override
    public boolean setString(String str) {
      String[] split = str.split(":");
      String hostStr = "localhost";
      String portStr = null;
      String clientId;

      if (split.length == 3) {
        hostStr = split[0];
        portStr = split[1];
        clientId = split[2];
      } else if (split.length == 2) {
        portStr = split[0];
        clientId = split[1];
      } else {
        return false;
      }

      options.setRemoteUIHost(hostStr);
      options.setClientId(clientId);

      try {
        options.setRemoteUIHostPort(Integer.parseInt(portStr));
      } catch (NumberFormatException nfe) {
        System.err.println("A port must be an integer");
        return false;
      }

      return true;
    }
  }

  protected static final Map<String, ModuleDef> startupModules = new HashMap<String, ModuleDef>();
  
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

  protected interface HostedModeBaseOptions extends JJSOptions, OptionLogDir,
      OptionLogLevel, OptionGenDir, OptionNoServer, OptionPort,
      OptionCodeServerPort, OptionStartupURLs, OptionRemoteUI {

    /**
     * The base shell work directory.
     * 
     * @param moduleDef
     * @return working directory base
     */
    File getShellBaseWorkDir(ModuleDef moduleDef);
  }

  /**
   * Concrete class to implement all hosted mode base options.
   */
  @SuppressWarnings("serial")
  protected static class HostedModeBaseOptionsImpl extends
      PrecompileOptionsImpl implements HostedModeBaseOptions {

    private boolean isNoServer;
    private File logDir;
    private int port;
    private int portHosted;
    private String remoteUIClientId;
    private String remoteUIHost;
    private int remoteUIHostPort;
    private final List<String> startupURLs = new ArrayList<String>();

    public void addStartupURL(String url) {
      startupURLs.add(url);
    }

    public boolean alsoLogToFile() {
      return logDir != null;
    }

    public String getClientId() {
      return remoteUIClientId;
    }

    public File getLogDir() {
      return logDir;
    }

    public File getLogFile(String sublog) {
      if (logDir == null) {
        return null;
      }
      return new File(logDir, sublog);
    }

    public int getPort() {
      return port;
    }

    public int getCodeServerPort() {
      return portHosted;
    }

    public String getRemoteUIHost() {
      return remoteUIHost;
    }

    public int getRemoteUIHostPort() {
      return remoteUIHostPort;
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

    public void setClientId(String clientId) {
      this.remoteUIClientId = clientId;
    }

    public void setLogFile(String filename) {
      logDir = new File(filename);
    }

    public void setNoServer(boolean isNoServer) {
      this.isNoServer = isNoServer;
    }

    public void setPort(int port) {
      this.port = port;
    }

    public void setCodeServerPort(int port) {
      portHosted = port;
    }

    public void setRemoteUIHost(String remoteUIHost) {
      this.remoteUIHost = remoteUIHost;
    }

    public void setRemoteUIHostPort(int remoteUIHostPort) {
      this.remoteUIHostPort = remoteUIHostPort;
    }

    public boolean useRemoteUI() {
      return remoteUIHost != null;
    }
  }

  /**
   * Controls whether and where to log data to file.
   * 
   */
  protected interface OptionLogDir {
    boolean alsoLogToFile();

    File getLogDir();

    File getLogFile(String subfile);

    void setLogFile(String filename);
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

  protected interface OptionCodeServerPort {
    int getCodeServerPort();

    void setCodeServerPort(int codeServerPort);
  }

  /**
   * Controls the UI that should be used to display the dev mode server's data.
   */
  protected interface OptionRemoteUI {
    String getClientId();

    String getRemoteUIHost();

    int getRemoteUIHostPort();

    void setClientId(String clientId);

    void setRemoteUIHost(String remoteUIHost);

    void setRemoteUIHostPort(int remoteUIHostPort);

    boolean useRemoteUI();
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
      registerHandler(new ArgHandlerLogDir(options));
      registerHandler(new ArgHandlerLogLevel(options));
      registerHandler(new ArgHandlerGenDir(options));
      registerHandler(new ArgHandlerCodeServerPort(options));
      registerHandler(new ArgHandlerRemoteUI(options));
    }
  }

  private static final Random RNG = new Random();

  private static final AtomicLong uniqueId = new AtomicLong();

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

  /**
   * Returns a free port. The returned port should not be returned again unless
   * the ephemeral port range is exhausted.
   */
  protected static int getFreeSocketPort() {
    ServerSocket socket = null;
    try {
      socket = new ServerSocket(0);
      return socket.getLocalPort();
    } catch (IOException e) {
    } finally {
      if (socket != null) {
        try {
          socket.close();
        } catch (IOException e) {
        }
      }
    }
    return -1;
  }

  /**
   * Produce a random string that has low probability of collisions.
   * 
   * <p>
   * In this case, we use 16 characters, each drawn from a pool of 94, so the
   * number of possible values is 94^16, leading to an expected number of values
   * used before a collision occurs as sqrt(pi/2) * 94^8 (treated the same as a
   * birthday attack), or a little under 10^16.
   * 
   * <p>
   * This algorithm is also implemented in hosted.html, though it is not
   * technically important that they match.
   * 
   * @return a random string
   */
  protected static String randomString() {
    StringBuilder buf = new StringBuilder(16);
    for (int i = 0; i < 16; ++i) {
      buf.append((char) RNG.nextInt('~' - '!' + 1) + '!');
    }
    return buf.toString();
  }

  protected int codeServerPort;

  protected BrowserListener listener;

  protected final HostedModeBaseOptions options;

  protected DevModeUI ui = null;

  /**
   * Cheat on the first load's refresh by assuming the module loaded by
   * {@link com.google.gwt.dev.shell.GWTShellServlet} is still fresh. This
   * prevents a double-refresh on startup. Subsequent refreshes will trigger a
   * real refresh.
   */
  private Set<String> alreadySeenModules = new HashSet<String>();

  private final Semaphore blockUntilDone = new Semaphore(0);

  private BrowserWidgetHost browserHost = new UiBrowserWidgetHostImpl();

  private boolean headlessMode = false;

  private final Map<ModuleSpaceHost, ModuleHandle> loadedModules = new IdentityHashMap<ModuleSpaceHost, ModuleHandle>();

  private boolean started;

  private TreeLogger topLogger;

  public DevModeBase() {
    // Set any platform specific system properties.
    BootStrapPlatform.initHostedMode();
    BootStrapPlatform.applyPlatformHacks();
    options = createOptions();
  }

  public final void addStartupURL(String url) {
    options.addStartupURL(url);
  }

  public final int getPort() {
    return options.getPort();
  }

  public TreeLogger getTopLogger() {
    return topLogger;
  }

  /**
   * Launch the arguments as Urls in separate windows.
   * 
   * @param logger TreeLogger instance to use
   */
  public void launchStartupUrls(final TreeLogger logger) {
    ensureCodeServerListener();
    String startupURL = "";
    try {
      for (String prenormalized : options.getStartupURLs()) {
        startupURL = normalizeURL(prenormalized, getPort(), getHost());
        logger.log(TreeLogger.TRACE, "Starting URL: " + startupURL, null);
        launchURL(startupURL);
      }
    } catch (UnableToCompleteException e) {
      logger.log(TreeLogger.ERROR,
          "Unable to open new window for startup URL: " + startupURL, null);
    }
  }

  /**
   * Callback for the UI to indicate it is done.
   */
  public void onDone() {
    setDone();
  }

  /**
   * Sets up all the major aspects of running the shell graphically, including
   * creating the main window and optionally starting an embedded web server.
   */
  public final void run() {
    try {
      // Eager AWT init for OS X to ensure safe coexistence with SWT.
      BootStrapPlatform.initGui();

      if (startUp() && !options.useRemoteUI()) {
        // The web server is running now, so launch browsers for startup urls.
        launchStartupUrls(getTopLogger());
      }

      blockUntilDone.acquire();
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

  protected abstract HostedModeBaseOptions createOptions();

  /**
   * Creates an instance of ShellModuleSpaceHost (or a derived class) using the
   * specified constituent parts. This method is made to be overridden for
   * subclasses that need to change the behavior of ShellModuleSpaceHost.
   * 
   * @param logger TreeLogger to use
   * @param compilationState
   * @param moduleDef
   * @return ShellModuleSpaceHost instance
   */
  protected final ShellModuleSpaceHost doCreateShellModuleSpaceHost(
      TreeLogger logger, CompilationState compilationState, ModuleDef moduleDef)
      throws UnableToCompleteException {
    // Clear out the shell temp directory.
    File shellBaseWorkDir = options.getShellBaseWorkDir(moduleDef);
    File sessionWorkDir = new File(shellBaseWorkDir,
        String.valueOf(uniqueId.getAndIncrement()));
    Util.recursiveDelete(sessionWorkDir, false);
    ArtifactAcceptor artifactAcceptor = createArtifactAcceptor(logger,
        moduleDef);
    return new ShellModuleSpaceHost(logger, compilationState, moduleDef,
        options.getGenDir(), new File(sessionWorkDir, "gen"), artifactAcceptor);
  }

  protected abstract void doShutDownServer();

  /**
   * Perform any startup tasks, including initializing the UI (if any) and the
   * logger, updates checker, and the development mode code server.
   * 
   * <p>
   * Subclasses that override this method should be careful what facilities are
   * used before the super implementation is called.
   * 
   * @return true if startup was successful
   */
  protected boolean doStartup() {
    // Create the main app window.
    ui.initialize(options.getLogLevel());
    topLogger = ui.getTopLogger();

    // Set done callback
    ui.setCallback(DoneEvent.getType(), this);

    // Check for updates
    final TreeLogger logger = getTopLogger();
    final CheckForUpdates updateChecker = CheckForUpdates.createUpdateChecker(logger);
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

    // Accept connections from OOPHM clients
    ensureCodeServerListener();

    return true;
  }

  protected abstract int doStartUpServer();

  protected void ensureCodeServerListener() {
    if (listener == null) {
      codeServerPort = options.getCodeServerPort();
      listener = new BrowserListener(getTopLogger(), codeServerPort,
          new OophmSessionHandler(browserHost));
      listener.start();
      try {
        // save the port we actually used if it was auto
        codeServerPort = listener.getSocketPort();
      } catch (UnableToCompleteException e) {
        // ignore errors listening, we will catch them later
      }
    }
  }

  protected String getHost() {
    return "localhost";
  }

  /**
   * By default we will open the application window.
   * 
   * @return true if we are running in headless mode
   */
  protected final boolean isHeadless() {
    return headlessMode;
  }

  protected void launchURL(String url) throws UnableToCompleteException {
    /*
     * TODO(jat): properly support launching arbitrary browsers -- need some UI
     * API tweaks to support that.
     */
    URL parsedUrl = null;
    try {
      parsedUrl = new URL(url);
      String path = parsedUrl.getPath();
      String query = parsedUrl.getQuery();
      String hash = parsedUrl.getRef();
      String hostedParam = BrowserListener.getDevModeURLParams(listener.getEndpointIdentifier());
      if (query == null) {
        query = hostedParam;
      } else {
        query += '&' + hostedParam;
      }
      path += '?' + query;
      if (hash != null) {
        path += '#' + hash;
      }
      parsedUrl = new URL(parsedUrl.getProtocol(), parsedUrl.getHost(),
          parsedUrl.getPort(), path);
      url = parsedUrl.toExternalForm();
    } catch (MalformedURLException e) {
      getTopLogger().log(TreeLogger.ERROR, "Invalid URL " + url, e);
      throw new UnableToCompleteException();
    }
    
    final URL helpInfoUrl = parsedUrl;
    getTopLogger().log(TreeLogger.INFO,
        "Waiting for browser connection to " + url, null, new HelpInfo() {
          @Override
          public String getAnchorText() {
            return "Launch default browser";
          }

          @Override
          public String getPrefix() {
            return "";
          }

          @Override
          public URL getURL() {
            return helpInfoUrl;
          }
        });
  }

  /**
   * Perform an initial hosted mode link, without overwriting newer or
   * unmodified files in the output folder.
   * 
   * @param logger the logger to use
   * @param module the module to link
   * @throws UnableToCompleteException
   */
  protected final StandardLinkerContext link(TreeLogger logger, ModuleDef module)
      throws UnableToCompleteException {
    TreeLogger linkLogger = logger.branch(TreeLogger.DEBUG, "Linking module '"
        + module.getName() + "'");

    // Create a new active linker stack for the fresh link.
    StandardLinkerContext linkerStack = new StandardLinkerContext(linkLogger,
        module, options);
    ArtifactSet artifacts = linkerStack.invokeLink(linkLogger);
    produceOutput(linkLogger, linkerStack, artifacts, module);
    return linkerStack;
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

  protected abstract void produceOutput(TreeLogger logger,
      StandardLinkerContext linkerStack, ArtifactSet artifacts, ModuleDef module)
      throws UnableToCompleteException;

  protected final void setDone() {
    blockUntilDone.release();
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

    // See if there was a UI specified by command-line args
    ui = createUI();

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
      getTopLogger().log(TreeLogger.TRACE,
          "Started web server on port " + resultPort);
    }

    return true;
  }

  private ArtifactAcceptor createArtifactAcceptor(TreeLogger logger,
      final ModuleDef module) throws UnableToCompleteException {
    final StandardLinkerContext linkerContext = link(logger, module);
    return new ArtifactAcceptor() {
      public void accept(TreeLogger relinkLogger, ArtifactSet newArtifacts)
          throws UnableToCompleteException {
        relink(relinkLogger, linkerContext, module, newArtifacts);
      }
    };
  }

  private DevModeUI createUI() {
    if (headlessMode) {
      return new HeadlessUI(options);
    } else {
      if (options.useRemoteUI()) {
        return new RemoteUI(options.getRemoteUIHost(),
            options.getRemoteUIHostPort(), options.getClientId(),
            options.getPort(), options.getCodeServerPort());
      }
    }

    return new SwingUI(options);
  }

  /**
   * Perform hosted mode relink when new artifacts are generated, without
   * overwriting newer or unmodified files in the output folder.
   * 
   * @param logger the logger to use
   * @param module the module to link
   * @param newlyGeneratedArtifacts the set of new artifacts
   * @throws UnableToCompleteException
   */
  private void relink(TreeLogger logger, StandardLinkerContext linkerContext,
      ModuleDef module, ArtifactSet newlyGeneratedArtifacts)
      throws UnableToCompleteException {
    TreeLogger linkLogger = logger.branch(TreeLogger.DEBUG,
        "Relinking module '" + module.getName() + "'");

    ArtifactSet artifacts = linkerContext.invokeRelink(linkLogger,
        newlyGeneratedArtifacts);
    produceOutput(linkLogger, linkerContext, artifacts, module);
  }
}
