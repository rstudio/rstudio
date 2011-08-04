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
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.impl.StandardLinkerContext;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.ModuleDefLoader;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.javac.CompilationStateBuilder;
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
import com.google.gwt.dev.util.BrowserInfo;
import com.google.gwt.dev.util.arg.ArgHandlerEnableGeneratorResultCaching;
import com.google.gwt.dev.util.arg.ArgHandlerGenDir;
import com.google.gwt.dev.util.arg.ArgHandlerLogLevel;
import com.google.gwt.dev.util.arg.OptionDisableUpdateCheck;
import com.google.gwt.dev.util.arg.OptionGenDir;
import com.google.gwt.dev.util.arg.OptionLogLevel;
import com.google.gwt.dev.util.log.speedtracer.DevModeEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;
import com.google.gwt.util.tools.ArgHandlerFlag;
import com.google.gwt.util.tools.ArgHandlerString;

import java.io.File;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Semaphore;

/**
 * The main executable class for the hosted mode shell. This class must not have
 * any GUI dependencies.
 */
public abstract class DevModeBase implements DoneCallback {
  /**
   * Implementation of BrowserWidgetHost that supports the abstract UI
   * interface.
   */
  public class UiBrowserWidgetHostImpl implements BrowserWidgetHost {

    @Override
    public ModuleHandle createModuleLogger(String moduleName, String userAgent, String url,
        String tabKey, String sessionKey, BrowserChannelServer serverChannel, byte[] userAgentIcon) {
      if (sessionKey == null) {
        // if we don't have a unique session key, make one up
        sessionKey = randomString();
      }
      TreeLogger.Type maxLevel = options.getLogLevel();
      String agentTag = BrowserInfo.getShortName(userAgent);
      String remoteSocket = serverChannel.getRemoteEndpoint();
      ModuleHandle module =
          ui.getModuleLogger(userAgent, remoteSocket, url, tabKey, moduleName, sessionKey,
              agentTag, userAgentIcon, maxLevel);
      return module;
    }

    @Override
    public ModuleSpaceHost createModuleSpaceHost(ModuleHandle module, String moduleName)
        throws UnableToCompleteException {
      Event moduleSpaceHostCreateEvent =
          SpeedTracerLogger.start(DevModeEventType.MODULE_SPACE_HOST_CREATE, "Module Name",
              moduleName);
      // TODO(jat): add support for closing an active module
      TreeLogger logger = module.getLogger();
      try {
        // Try to find an existing loaded version of the module def.
        ModuleDef moduleDef = loadModule(logger, moduleName, true);
        assert (moduleDef != null);

        ArchivePreloader.preloadArchives(logger, moduleDef);
        
        CompilationState compilationState =
            moduleDef.getCompilationState(logger, !options.isStrict());
        ShellModuleSpaceHost host =
            doCreateShellModuleSpaceHost(logger, compilationState, moduleDef);
        return host;
      } catch (RuntimeException e) {
        logger.log(TreeLogger.ERROR, "Exception initializing module", e);
        module.unload();
        throw e;
      } finally {
        moduleSpaceHostCreateEvent.end();
      }
    }
  }

  /**
   * Handles the -bindAddress command line flag.
   */
  protected static class ArgHandlerBindAddress extends ArgHandlerString {

    private static final String BIND_ADDRESS_TAG = "-bindAddress";
    private static final String DEFAULT_BIND_ADDRESS = "127.0.0.1";

    private final OptionBindAddress options;

    public ArgHandlerBindAddress(OptionBindAddress options) {
      this.options = options;
    }

    @Override
    public String[] getDefaultArgs() {
      return new String[]{BIND_ADDRESS_TAG, DEFAULT_BIND_ADDRESS};
    }

    @Override
    public String getPurpose() {
      return "Specifies the bind address for the code server and web server " + "(defaults to "
          + DEFAULT_BIND_ADDRESS + ")";
    }

    @Override
    public String getTag() {
      return BIND_ADDRESS_TAG;
    }

    @Override
    public String[] getTagArgs() {
      return new String[]{"host-name-or-address"};
    }

    @Override
    public boolean setString(String value) {
      try {
        InetAddress address = InetAddress.getByName(value);
        options.setBindAddress(value);
        if (address.isAnyLocalAddress()) {
          // replace a wildcard address with our machine's local address
          // this isn't fully accurate, as there is no guarantee we will get
          // the right one on a multihomed host
          options.setConnectAddress(InetAddress.getLocalHost().getHostAddress());
        } else {
          options.setConnectAddress(value);
        }
        return true;
      } catch (UnknownHostException e) {
        System.err.println("-bindAddress host \"" + value + "\" unknown");
        return false;
      }
    }
  }

  /**
   * Handles the -blacklist command line argument.
   */
  protected static class ArgHandlerBlacklist extends ArgHandlerString {
    public ArgHandlerBlacklist() {
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
      return new String[]{"blacklist-string"};
    }

    @Override
    public boolean setString(String blacklistStr) {
      return BrowserWidgetHostChecker.blacklistRegexes(blacklistStr);
    }
  }

  /**
   * Handles the -codeServerPort command line flag.
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
      return new String[]{CODE_SERVER_PORT_TAG, DEFAULT_PORT};
    }

    @Override
    public String getPurpose() {
      return "Specifies the TCP port for the code server (defaults to " + DEFAULT_PORT + ")";
    }

    @Override
    public String getTag() {
      return CODE_SERVER_PORT_TAG;
    }

    @Override
    public String[] getTagArgs() {
      return new String[]{"port-number | \"auto\""};
    }

    @Override
    public boolean setString(String value) {
      if (value.equals("auto")) {
        options.setCodeServerPort(0);
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
      return new String[]{"directory"};
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
      return new String[]{getTag(), "8888"};
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
      return new String[]{"port-number | \"auto\""};
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
   * Handles the -remoteUI command line flag.
   */
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
      return new String[]{"port-number:client-id-string | host-string:port-number:client-id-string"};
    }

    @Override
    public boolean isUndocumented() {
      return true;
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

  /**
   * Handles the -whitelist command line flag.
   */
  protected static class ArgHandlerWhitelist extends ArgHandlerString {
    public ArgHandlerWhitelist() {
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
      return new String[]{"whitelist-string"};
    }

    @Override
    public boolean setString(String whitelistStr) {
      return BrowserWidgetHostChecker.whitelistRegexes(whitelistStr);
    }
  }

  /**
   * Base options for dev mode.
   */
  protected interface HostedModeBaseOptions extends JJSOptions, OptionLogDir, OptionLogLevel,
      OptionGenDir, OptionNoServer, OptionPort, OptionCodeServerPort, OptionStartupURLs,
      OptionRemoteUI, OptionBindAddress, OptionDisableUpdateCheck {
  }

  /**
   * Concrete class to implement all hosted mode base options.
   */
  @SuppressWarnings("serial")
  protected static class HostedModeBaseOptionsImpl extends PrecompileTaskOptionsImpl implements
      HostedModeBaseOptions {

    private String bindAddress;
    private int codeServerPort;
    private String connectAddress;
    private boolean isNoServer;
    private File logDir;
    private int port;
    private String remoteUIClientId;
    private String remoteUIHost;
    private int remoteUIHostPort;
    private final List<String> startupURLs = new ArrayList<String>();

    @Override
    public void addStartupURL(String url) {
      startupURLs.add(url);
    }

    @Override
    public boolean alsoLogToFile() {
      return logDir != null;
    }

    @Override
    public String getBindAddress() {
      return bindAddress;
    }

    @Override
    public String getClientId() {
      return remoteUIClientId;
    }

    @Override
    public int getCodeServerPort() {
      return codeServerPort;
    }

    @Override
    public String getConnectAddress() {
      return connectAddress;
    }

    @Override
    public File getLogDir() {
      return logDir;
    }

    @Override
    public File getLogFile(String sublog) {
      if (logDir == null) {
        return null;
      }
      return new File(logDir, sublog);
    }

    @Override
    public int getPort() {
      return port;
    }

    @Override
    public String getRemoteUIHost() {
      return remoteUIHost;
    }

    @Override
    public int getRemoteUIHostPort() {
      return remoteUIHostPort;
    }

    @Override
    public List<String> getStartupURLs() {
      return Collections.unmodifiableList(startupURLs);
    }

    @Override
    public boolean isNoServer() {
      return isNoServer;
    }

    @Override
    public void setBindAddress(String bindAddress) {
      this.bindAddress = bindAddress;
    }

    @Override
    public void setClientId(String clientId) {
      this.remoteUIClientId = clientId;
    }

    @Override
    public void setCodeServerPort(int port) {
      codeServerPort = port;
    }

    @Override
    public void setConnectAddress(String connectAddress) {
      this.connectAddress = connectAddress;
    }

    @Override
    public void setLogFile(String filename) {
      logDir = new File(filename);
    }

    @Override
    public void setNoServer(boolean isNoServer) {
      this.isNoServer = isNoServer;
    }

    @Override
    public void setPort(int port) {
      this.port = port;
    }

    @Override
    public void setRemoteUIHost(String remoteUIHost) {
      this.remoteUIHost = remoteUIHost;
    }

    @Override
    public void setRemoteUIHostPort(int remoteUIHostPort) {
      this.remoteUIHostPort = remoteUIHostPort;
    }

    @Override
    public boolean useRemoteUI() {
      return remoteUIHost != null;
    }
  }

  /**
   * Controls what local address to bind to.
   */
  protected interface OptionBindAddress {
    String getBindAddress();

    String getConnectAddress();

    void setBindAddress(String bindAddress);

    void setConnectAddress(String connectAddress);
  }

  /**
   * Controls what port the code server listens on.
   */
  protected interface OptionCodeServerPort {
    int getCodeServerPort();

    void setCodeServerPort(int codeServerPort);
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

  /**
   * The base dev mode argument processor.
   */
  protected abstract static class ArgProcessor extends ArgProcessorBase {
    public ArgProcessor(HostedModeBaseOptions options, boolean forceServer) {
      if (!forceServer) {
        registerHandler(new ArgHandlerNoServerFlag(options));
      }
      registerHandler(new ArgHandlerPort(options));
      registerHandler(new ArgHandlerWhitelist());
      registerHandler(new ArgHandlerBlacklist());
      registerHandler(new ArgHandlerEnableGeneratorResultCaching());
      registerHandler(new ArgHandlerLogDir(options));
      registerHandler(new ArgHandlerLogLevel(options));
      registerHandler(new ArgHandlerGenDir(options));
      registerHandler(new ArgHandlerBindAddress(options));
      registerHandler(new ArgHandlerCodeServerPort(options));
      registerHandler(new ArgHandlerRemoteUI(options));
    }
  }

  private static final boolean generatorResultCachingDisabled =
      (System.getProperty("gwt.disableGeneratorResultCaching") != null);

  private static final Random RNG = new Random();

  public static String normalizeURL(String unknownUrlText, boolean isHttps, int port, String host) {
    if (unknownUrlText.contains("://")) {
      // Assume it's a full url.
      return unknownUrlText;
    }

    // Assume it's a trailing url path.
    if (unknownUrlText.length() > 0 && unknownUrlText.charAt(0) == '/') {
      unknownUrlText = unknownUrlText.substring(1);
    }

    String protocol = "http";
    String portString = ":" + port;
    if (isHttps) {
      protocol += "s";
      if (port == 443) {
        portString = "";
      }
    } else if (port == 80) {
      portString = "";
    }

    return protocol + "://" + host + portString + "/" + unknownUrlText;
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

  protected TreeLogger.Type baseLogLevelForUI = null;

  protected String bindAddress;

  protected int codeServerPort;

  protected String connectAddress;

  protected boolean isHttps;

  protected BrowserListener listener;

  protected final HostedModeBaseOptions options;

  protected DevModeUI ui = null;

  private final Semaphore blockUntilDone = new Semaphore(0);

  private BrowserWidgetHost browserHost = new UiBrowserWidgetHostImpl();

  private boolean headlessMode = false;

  private Map<String, RebindCache> rebindCaches = null;

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

  /**
   * Gets the base log level recommended by the UI for INFO-level messages. This
   * method can only be called once {@link #createUI()} has been called. Please
   * do not depend on this method, as it is subject to change.
   * 
   * @return the log level to use for INFO-level messages
   */
  public TreeLogger.Type getBaseLogLevelForUI() {
    if (baseLogLevelForUI == null) {
      throw new IllegalStateException("The ui must be created before calling this method.");
    }

    return baseLogLevelForUI;
  }

  public final int getPort() {
    return options.getPort();
  }

  public TreeLogger getTopLogger() {
    return topLogger;
  }

  /**
   * Callback for the UI to indicate it is done.
   */
  @Override
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

      boolean success = startUp();

      // The web server is running now, so launch browsers for startup urls.
      ui.moduleLoadComplete(success);

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
  protected final ShellModuleSpaceHost doCreateShellModuleSpaceHost(TreeLogger logger,
      CompilationState compilationState, ModuleDef moduleDef) throws UnableToCompleteException {
    ArtifactAcceptor artifactAcceptor = createArtifactAcceptor(logger, moduleDef);
    return new ShellModuleSpaceHost(logger, compilationState, moduleDef, options.getGenDir(),
        artifactAcceptor, getRebindCache(moduleDef.getName()));
  }

  protected abstract void doShutDownServer();

  /**
   * Perform any slower startup tasks, such as loading modules. This is separate
   * from {@link #doStartup()} so that the UI can be updated as soon as possible
   * and the web server can be started earlier.
   * 
   * @return false if startup failed
   */
  protected boolean doSlowStartup() {
    // do nothing by default
    return true;
  }

  protected abstract boolean doStartup();

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
  protected boolean doStartup(File persistentCacheDir) {
    bindAddress = options.getBindAddress();
    connectAddress = options.getConnectAddress();

    // Create the main app window.
    ui.initialize(options.getLogLevel());
    topLogger = ui.getTopLogger();

    CompilationStateBuilder.init(getTopLogger(), persistentCacheDir);

    // Set done callback
    ui.setCallback(DoneEvent.getType(), this);

    // Check for updates
    if (!options.isUpdateCheckDisabled()) {
      final TreeLogger logger = getTopLogger();
      final CheckForUpdates updateChecker = CheckForUpdates.createUpdateChecker(logger);
      if (updateChecker != null) {
        Thread checkerThread = new Thread("GWT Update Checker") {
          @Override
          public void run() {
            CheckForUpdates.logUpdateAvailable(logger, updateChecker
                .check(checkForUpdatesInterval()));
          }
        };
        checkerThread.setDaemon(true);
        checkerThread.start();
      }
    }

    // Accept connections from OOPHM clients
    ensureCodeServerListener();

    return true;
  }

  protected abstract int doStartUpServer();

  protected void ensureCodeServerListener() {
    if (listener == null) {
      codeServerPort = options.getCodeServerPort();
      listener =
          new BrowserListener(getTopLogger(), bindAddress, codeServerPort, new OophmSessionHandler(
              getTopLogger(), browserHost));
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
    return connectAddress;
  }

  /**
   * Add any plausible HTML files which might be used as startup URLs. Found
   * URLs should be added to {@code options.addStartupUrl(url)}.
   */
  protected void inferStartupUrls() {
    // do nothing by default
  }

  /**
   * By default we will open the application window.
   * 
   * @return true if we are running in headless mode
   */
  protected final boolean isHeadless() {
    return headlessMode;
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
    TreeLogger linkLogger =
        logger.branch(TreeLogger.DEBUG, "Linking module '" + module.getName() + "'");

    // Create a new active linker stack for the fresh link.
    StandardLinkerContext linkerStack = new StandardLinkerContext(linkLogger, module, options);
    ArtifactSet artifacts = linkerStack.getArtifactsForPublicResources(logger, module);
    artifacts = linkerStack.invokeLegacyLinkers(linkLogger, artifacts);
    artifacts = linkerStack.invokeFinalLink(linkLogger, artifacts);
    produceOutput(linkLogger, linkerStack, artifacts, module, false);
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
  protected ModuleDef loadModule(TreeLogger logger, String moduleName, boolean refresh)
      throws UnableToCompleteException {
    ModuleDef moduleDef = ModuleDefLoader.loadFromClassPath(logger, moduleName, refresh);
    assert (moduleDef != null) : "Required module state is absent";
    return moduleDef;
  }

  protected URL processUrl(String url) throws UnableToCompleteException {
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
      String hostedParam =
          BrowserListener.getDevModeURLParams(connectAddress, listener.getSocketPort());
      if (query == null) {
        query = hostedParam;
      } else {
        query += '&' + hostedParam;
      }
      path += '?' + query;
      if (hash != null) {
        path += '#' + hash;
      }
      parsedUrl = new URL(parsedUrl.getProtocol(), parsedUrl.getHost(), parsedUrl.getPort(), path);
      url = parsedUrl.toExternalForm();
    } catch (MalformedURLException e) {
      getTopLogger().log(TreeLogger.ERROR, "Invalid URL " + url, e);
      throw new UnableToCompleteException();
    }
    return parsedUrl;
  }

  protected abstract void produceOutput(TreeLogger logger, StandardLinkerContext linkerStack,
      ArtifactSet artifacts, ModuleDef module, boolean isRelink) throws UnableToCompleteException;

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

    Event startupEvent = SpeedTracerLogger.start(DevModeEventType.STARTUP);
    try {
      // See if there was a UI specified by command-line args
      ui = createUI();

      started = true;

      if (!doStartup()) {
        /*
         * TODO (amitmanjhi): Adding this redundant logging to narrow down a
         * failure. Remove soon.
         */
        getTopLogger().log(TreeLogger.ERROR, "shell failed in doStartup method");
        return false;
      }

      if (!options.isNoServer()) {
        int resultPort = doStartUpServer();
        if (resultPort < 0) {
          /*
           * TODO (amitmanjhi): Adding this redundant logging to narrow down a
           * failure. Remove soon.
           */
          getTopLogger().log(TreeLogger.ERROR, "shell failed in doStartupServer method");
          return false;
        }
        options.setPort(resultPort);
        getTopLogger().log(TreeLogger.TRACE, "Started web server on port " + resultPort);
      }

      if (options.getStartupURLs().isEmpty()) {
        // if no URLs were supplied, try and find plausible ones
        inferStartupUrls();
      }

      if (options.getStartupURLs().isEmpty()) {
        // TODO(jat): we could walk public resources to find plausible URLs
        // after the module(s) are loaded
        warnAboutNoStartupUrls();
      }

      setStartupUrls(getTopLogger());

      if (!doSlowStartup()) {
        /*
         * TODO (amitmanjhi): Adding this redundant logging to narrow down a
         * failure. Remove soon.
         */
        getTopLogger().log(TreeLogger.ERROR, "shell failed in doSlowStartup method");
        return false;
      }

      return true;
    } finally {
      startupEvent.end();
    }
  }

  /**
   * Log a warning explaining that no startup URLs were specified and no
   * plausible startup URLs were found.
   */
  protected abstract void warnAboutNoStartupUrls();

  private ArtifactAcceptor createArtifactAcceptor(TreeLogger logger, final ModuleDef module)
      throws UnableToCompleteException {
    final StandardLinkerContext linkerContext = link(logger, module);
    return new ArtifactAcceptor() {
      @Override
      public void accept(TreeLogger relinkLogger, ArtifactSet newArtifacts)
          throws UnableToCompleteException {
        relink(relinkLogger, linkerContext, module, newArtifacts);
      }
    };
  }

  /**
   * Create the UI and set the base log level for the UI.
   */
  private DevModeUI createUI() {
    DevModeUI newUI = null;

    Event createUIEvent = SpeedTracerLogger.start(DevModeEventType.CREATE_UI);

    if (headlessMode) {
      newUI = new HeadlessUI(options);
    } else {
      if (options.useRemoteUI()) {
        try {
          newUI =
              new RemoteUI(options.getRemoteUIHost(), options.getRemoteUIHostPort(), options
                  .getClientId());
          baseLogLevelForUI = TreeLogger.Type.TRACE;
        } catch (Throwable t) {
          System.err.println("Could not connect to remote UI listening at "
              + options.getRemoteUIHost() + ":" + options.getRemoteUIHostPort()
              + ". Using default UI instead.");
        }
      }
    }

    if (newUI == null) {
      newUI = new SwingUI(options);
    }

    if (baseLogLevelForUI == null) {
      baseLogLevelForUI = TreeLogger.Type.INFO;
    }

    createUIEvent.end();
    return newUI;
  }

  private RebindCache getRebindCache(String moduleName) {

    if (generatorResultCachingDisabled) {
      return null;
    }

    if (rebindCaches == null) {
      rebindCaches = new HashMap<String, RebindCache>();
    }

    RebindCache cache = rebindCaches.get(moduleName);
    if (cache == null) {
      cache = new RebindCache();
      rebindCaches.put(moduleName, cache);
    }
    return cache;
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
  private void relink(TreeLogger logger, StandardLinkerContext linkerContext, ModuleDef module,
      ArtifactSet newlyGeneratedArtifacts) throws UnableToCompleteException {
    TreeLogger linkLogger =
        logger.branch(TreeLogger.DEBUG, "Relinking module '" + module.getName() + "'");

    ArtifactSet artifacts = linkerContext.invokeRelink(linkLogger, newlyGeneratedArtifacts);
    produceOutput(linkLogger, linkerContext, artifacts, module, true);
  }

  /**
   * Set the set of startup URLs. This is done before launching to allow the UI
   * to better present the options to the user, but note that the UI should not
   * attempt to launch the URLs until
   * {@link DevModeUI#moduleLoadComplete(boolean)} is called, and should not
   * automatically launch any URLs if they
   * 
   * @param logger TreeLogger instance to use
   */
  private void setStartupUrls(final TreeLogger logger) {
    ensureCodeServerListener();
    Map<String, URL> startupUrls = new HashMap<String, URL>();
    for (String prenormalized : options.getStartupURLs()) {
      String startupURL = normalizeURL(prenormalized, isHttps, getPort(), getHost());
      logger.log(TreeLogger.DEBUG, "URL " + prenormalized + " normalized as " + startupURL, null);
      try {
        URL url = processUrl(startupURL);
        startupUrls.put(prenormalized, url);
      } catch (UnableToCompleteException e) {
        logger.log(TreeLogger.ERROR, "Unable to process startup URL " + startupURL, null);
      }
    }
    ui.setStartupUrls(startupUrls);
  }
}
