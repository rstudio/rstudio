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

import com.google.gwt.core.ext.ServletContainer;
import com.google.gwt.core.ext.ServletContainerLauncher;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.impl.StandardLinkerContext;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.resource.impl.ResourceOracleImpl;
import com.google.gwt.dev.shell.BrowserListener;
import com.google.gwt.dev.shell.CodeServerListener;
import com.google.gwt.dev.shell.OophmSessionHandler;
import com.google.gwt.dev.shell.SuperDevListener;
import com.google.gwt.dev.shell.jetty.JettyLauncher;
import com.google.gwt.dev.ui.RestartServerCallback;
import com.google.gwt.dev.ui.RestartServerEvent;
import com.google.gwt.dev.util.InstalledHelpInfo;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.arg.ArgHandlerDeployDir;
import com.google.gwt.dev.util.arg.ArgHandlerDisableUpdateCheck;
import com.google.gwt.dev.util.arg.ArgHandlerExtraDir;
import com.google.gwt.dev.util.arg.ArgHandlerGenerateJsInteropExports;
import com.google.gwt.dev.util.arg.ArgHandlerIncrementalCompile;
import com.google.gwt.dev.util.arg.ArgHandlerMethodNameDisplayMode;
import com.google.gwt.dev.util.arg.ArgHandlerModuleName;
import com.google.gwt.dev.util.arg.ArgHandlerModulePathPrefix;
import com.google.gwt.dev.util.arg.ArgHandlerScriptStyle;
import com.google.gwt.dev.util.arg.ArgHandlerSetProperties;
import com.google.gwt.dev.util.arg.ArgHandlerSourceLevel;
import com.google.gwt.dev.util.arg.ArgHandlerStrict;
import com.google.gwt.dev.util.arg.ArgHandlerWarDir;
import com.google.gwt.dev.util.arg.ArgHandlerWorkDirOptional;
import com.google.gwt.dev.util.arg.OptionModulePathPrefix;
import com.google.gwt.dev.util.log.speedtracer.DevModeEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;
import com.google.gwt.util.tools.ArgHandlerFlag;
import com.google.gwt.util.tools.ArgHandlerString;
import com.google.gwt.util.tools.Utility;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.BindException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * The main executable class for the hosted mode shell. NOTE: the public API for
 * this class is to be determined. Consider this class as having <b>no</b>
 * public API other than {@link #main(String[])}.
 */
public class DevMode extends DevModeBase implements RestartServerCallback {

  /**
   * Handles the -superDevMode command line flag.
   */
  public interface HostedModeOptions extends HostedModeBaseOptions, CompilerOptions,
      OptionSuperDevMode, OptionModulePathPrefix {
    ServletContainerLauncher getServletContainerLauncher();

    String getServletContainerLauncherArgs();

    void setServletContainerLauncher(ServletContainerLauncher scl);

    void setServletContainerLauncherArgs(String args);
  }

  /**
   * Runs the superdev-mode code server instead of classic one.
   */
  protected static class ArgHandlerSuperDevMode extends ArgHandlerFlag {
    private final HostedModeOptions options;

    public ArgHandlerSuperDevMode(HostedModeOptions options) {
      this.options = options;
      addTagValue("-superDevMode", true);
    }

    @Override
    public boolean getDefaultValue() {
      return true;
    }

    @Override
    public String getLabel() {
      return "superDevMode";
    }

    @Override
    public String getPurposeSnippet() {
      return "Runs Super Dev Mode instead of classic Development Mode.";
    }

    @Override
    public boolean setFlag(boolean value) {
      options.setSuperDevMode(value);
      // Superdev uses incremental by default
      if (options.isSuperDevMode()) {
        options.setIncrementalCompileEnabled(true);
      }
      return true;
    }
  }

  /**
   * Handles the -server command line flag.
   */
  protected static class ArgHandlerServer extends ArgHandlerString {

    private static final String DEFAULT_SCL = JettyLauncher.class.getName();

    private HostedModeOptions options;

    public ArgHandlerServer(HostedModeOptions options) {
      this.options = options;
    }

    @Override
    public String[] getDefaultArgs() {
      if (options.isNoServer()) {
        return null;
      } else {
        return new String[] {getTag(), DEFAULT_SCL};
      }
    }

    @Override
    public String getPurpose() {
      return "Specify a different embedded web server to run (must implement ServletContainerLauncher)";
    }

    @Override
    public String getTag() {
      return "-server";
    }

    @Override
    public String[] getTagArgs() {
      return new String[] {"servletContainerLauncher[:args]"};
    }

    @Override
    public boolean setString(String arg) {
      // Supercedes -noserver.
      options.setNoServer(false);
      String sclClassName;
      String sclArgs;
      int idx = arg.indexOf(':');
      if (idx >= 0) {
        sclArgs = arg.substring(idx + 1);
        sclClassName = arg.substring(0, idx);
      } else {
        sclArgs = null;
        sclClassName = arg;
      }
      if (sclClassName.length() == 0) {
        sclClassName = DEFAULT_SCL;
      }
      Throwable t;
      try {
        Class<?> clazz =
            Class.forName(sclClassName, true, Thread.currentThread().getContextClassLoader());
        Class<? extends ServletContainerLauncher> sclClass =
            clazz.asSubclass(ServletContainerLauncher.class);
        options.setServletContainerLauncher(sclClass.newInstance());
        options.setServletContainerLauncherArgs(sclArgs);
        return true;
      } catch (ClassCastException e) {
        t = e;
      } catch (ClassNotFoundException e) {
        t = e;
      } catch (InstantiationException e) {
        t = e;
      } catch (IllegalAccessException e) {
        t = e;
      }
      System.err.println("Unable to load server class '" + sclClassName + "'");
      t.printStackTrace();
      return false;
    }
  }

  /**
   * Handles a startup url that can be passed on the command line.
   */
  protected static class ArgHandlerStartupURLs extends ArgHandlerString {
    private final OptionStartupURLs options;

    public ArgHandlerStartupURLs(OptionStartupURLs options) {
      this.options = options;
    }

    @Override
    public String getPurpose() {
      return "Automatically launches the specified URL";
    }

    @Override
    public String getTag() {
      return "-startupUrl";
    }

    @Override
    public String[] getTagArgs() {
      return new String[] {"url"};
    }

    @Override
    public boolean setString(String arg) {
      options.addStartupURL(arg);
      return true;
    }
  }

  /**
   * The argument processor.
   */
  protected static class ArgProcessor extends DevModeBase.ArgProcessor {
    public ArgProcessor(HostedModeOptions options) {
      super(options, false);
      registerHandler(new ArgHandlerSuperDevMode(options));
      registerHandler(new ArgHandlerServer(options));
      registerHandler(new ArgHandlerStartupURLs(options));
      registerHandler(new ArgHandlerWarDir(options));
      registerHandler(new ArgHandlerDeployDir(options));
      registerHandler(new ArgHandlerExtraDir(options));
      registerHandler(new ArgHandlerModulePathPrefix(options));
      registerHandler(new ArgHandlerWorkDirOptional(options));
      registerHandler(new ArgHandlerDisableUpdateCheck(options));
      registerHandler(new ArgHandlerMethodNameDisplayMode(options));
      registerHandler(new ArgHandlerSourceLevel(options));
      registerHandler(new ArgHandlerGenerateJsInteropExports(options));
      registerHandler(new ArgHandlerIncrementalCompile(options));
      registerHandler(new ArgHandlerScriptStyle(options));
      registerHandler(new ArgHandlerStrict(options));
      registerHandler(new ArgHandlerModuleName(options) {
        @Override
        public String getPurpose() {
          return super.getPurpose() + " to host";
        }
      });
      registerHandler(new ArgHandlerSetProperties(options));
    }

    @Override
    protected String getName() {
      return DevMode.class.getName();
    }
  }

  /**
   * Concrete class to implement all hosted mode options.
   */
  @SuppressWarnings("serial")
  protected static class HostedModeOptionsImpl extends HostedModeBaseOptionsImpl implements
      HostedModeOptions {
    private File deployDir;
    private File extraDir;
    private int localWorkers;
    private ServletContainerLauncher scl;
    private String sclArgs;
    private boolean sdm = true;
    private File moduleBaseDir;
    private String modulePathPrefix = "";
    private File warDir;
    private boolean closureCompilerFormatEnabled;

    @Override
    public File getDeployDir() {
      return (deployDir == null) ? new File(warDir, "WEB-INF/deploy") : deployDir;
    }

    @Override
    public File getExtraDir() {
      return extraDir;
    }

    @Override
    public int getLocalWorkers() {
      return localWorkers;
    }

    @Override
    public File getSaveSourceOutput() {
      return null;
    }

    @Override
    public ServletContainerLauncher getServletContainerLauncher() {
      return scl;
    }

    @Override
    public File getModuleBaseDir() {
      return moduleBaseDir;
    }

    @Override
    public String getServletContainerLauncherArgs() {
      return sclArgs;
    }

    @Override
    public File getWarDir() {
      return warDir;
    }

    @Override
    public boolean isSuperDevMode() {
      return sdm;
    }

    @Override
    public void setDeployDir(File deployDir) {
      this.deployDir = deployDir;
    }

    @Override
    public void setExtraDir(File extraDir) {
      this.extraDir = extraDir;
    }

    @Override
    public void setLocalWorkers(int localWorkers) {
      this.localWorkers = localWorkers;
    }

    @Override
    public void setModulePathPrefix(String prefix) {
      if (!prefix.equals(modulePathPrefix)) {
        modulePathPrefix = prefix;
        updateModuleBaseDir();
      }
    }

    @Deprecated
    public void setOutDir(File outDir) {
      setWarDir(outDir);
    }

    @Override
    public void setSaveSourceOutput(File debugDir) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void setSuperDevMode(boolean sdm) {
      this.sdm = sdm;
    }

    @Override
    public void setServletContainerLauncher(ServletContainerLauncher scl) {
      this.scl = scl;
    }

    @Override
    public void setServletContainerLauncherArgs(String args) {
      sclArgs = args;
    }

    @Override
    public void setWarDir(File warDir) {
      this.warDir = warDir;
      updateModuleBaseDir();
    }

    private void updateModuleBaseDir() {
      this.moduleBaseDir = new File(warDir, modulePathPrefix);
    }

    @Override
    public boolean isClosureCompilerFormatEnabled() {
      return closureCompilerFormatEnabled;
    }

    @Override
    public void setClosureCompilerFormatEnabled(boolean enabled) {
      this.closureCompilerFormatEnabled = enabled;
    }
  }

  /**
   * Determines whether to start the code server or not.
   */
  protected interface OptionSuperDevMode {
    boolean isSuperDevMode();

    void setSuperDevMode(boolean sdm);
  }

  /**
   * The pattern for files usable as startup URLs.
   */
  private static final Pattern STARTUP_FILE_PATTERN = Pattern.compile(".*\\.(html|jsp)",
      Pattern.CASE_INSENSITIVE);

  /**
   * Startup development mode.
   *
   * @param args command line arguments
   */
  public static void main(String[] args) {
    /*
     * NOTE: main always exits with a call to System.exit to terminate any
     * non-daemon threads that were started in Generators. Typically, this is to
     * shutdown AWT related threads, since the contract for their termination is
     * still implementation-dependent.
     */
    DevMode hostedMode = new DevMode();
    if (new ArgProcessor(hostedMode.options).processArgs(args)) {
      hostedMode.run();
      // Exit w/ success code.
      System.exit(0);
    }
    // Exit w/ non-success code.
    System.exit(-1);
  }

  protected CodeServerListener listener;

  /**
   * Hiding super field because it's actually the same object, just with a stronger type.
   */
  @SuppressWarnings("hiding")
  protected final HostedModeOptions options = (HostedModeOptionsImpl) super.options;

  /**
   * The server that was started.
   */
  private ServletContainer server;

  private final Map<String, ModuleDef> startupModules = new LinkedHashMap<String, ModuleDef>();

  /**
   * Tracks whether we created a temp workdir that we need to destroy.
   */
  private boolean tempWorkDir = false;

  /**
   * Default constructor for testing; no public API yet.
   */
  protected DevMode() {
  }

  /**
   * Called by the UI on a restart server event.
   */
  @Override
  public void onRestartServer(TreeLogger logger) {
    try {
      server.refresh();
    } catch (UnableToCompleteException e) {
      // ignore, problem already logged
    }
  }

  @Override
  protected HostedModeOptions createOptions() {
    HostedModeOptionsImpl hostedModeOptions = new HostedModeOptionsImpl();
    hostedModeOptions.setIncrementalCompileEnabled(true);
    compilerContext = compilerContextBuilder.options(hostedModeOptions).build();
    return hostedModeOptions;
  }

  @Override
  protected void doShutDownServer() {
    if (server != null) {
      try {
        server.stop();
      } catch (UnableToCompleteException e) {
        // Already logged.
      }
      server = null;
    }

    if (tempWorkDir) {
      Util.recursiveDelete(options.getWorkDir(), false);
    }
  }

  @Override
  protected boolean doSlowStartup() {
    tempWorkDir = options.getWorkDir() == null;
    if (tempWorkDir) {
      try {
        options.setWorkDir(Utility.makeTemporaryDirectory(null, "gwtc"));
      } catch (IOException e) {
        System.err.println("Unable to create hosted mode work directory");
        e.printStackTrace();
        return false;
      }
    }

    TreeLogger branch = getTopLogger().branch(TreeLogger.TRACE, "Linking modules");
    Event slowStartupEvent = SpeedTracerLogger.start(DevModeEventType.SLOW_STARTUP);
    try {
      for (ModuleDef module : startupModules.values()) {
        TreeLogger loadLogger =
            branch.branch(TreeLogger.DEBUG, "Bootstrap link for command-line module '"
                + module.getCanonicalName() + "'");
        link(loadLogger, module);
      }
    } catch (UnableToCompleteException e) {
      // Already logged.
      return false;
    } finally {
      slowStartupEvent.end();
    }
    return true;
  }

  @Override
  protected boolean doStartup() {
    // Background scan the classpath to warm the cache.
    Thread scanThread = new Thread(new Runnable() {
      @Override
      public void run() {
        ResourceOracleImpl.preload(TreeLogger.NULL);
      }
    });
    scanThread.setDaemon(true);
    scanThread.setPriority((Thread.MIN_PRIORITY + Thread.NORM_PRIORITY) / 2);
    scanThread.start();

    File persistentCacheDir = null;
    if (options.getWarDir() != null && !options.getWarDir().getName().endsWith(".jar")) {
      persistentCacheDir = new File(options.getWarDir(), "../");
    }

    if (!super.doStartup(persistentCacheDir)) {
      return false;
    }

    ServletValidator servletValidator = null;
    ServletWriter servletWriter = null;
    File webXml = new File(options.getWarDir(), "WEB-INF/web.xml");
    if (!options.isNoServer()) {
      if (webXml.exists()) {
        servletValidator = ServletValidator.create(getTopLogger(), webXml);
      } else {
        servletWriter = new ServletWriter();
      }
    }

    TreeLogger branch = getTopLogger().branch(TreeLogger.TRACE, "Loading modules");
    try {
      for (String moduleName : options.getModuleNames()) {
        TreeLogger moduleBranch = branch.branch(TreeLogger.TRACE, moduleName);
        ModuleDef module = loadModule(moduleBranch, moduleName, false);
        // Create a hard reference to the module to avoid gc-ing it until we
        // actually load the module from the browser.
        startupModules.put(module.getName(), module);

        if (!options.isNoServer()) {
          validateServletTags(moduleBranch, servletValidator, servletWriter, module);
        }
      }
      if (servletWriter != null) {
        servletWriter.realize(webXml);
      }
    } catch (IOException e) {
      getTopLogger().log(TreeLogger.WARN, "Unable to generate '" + webXml.getAbsolutePath() + "'");
    } catch (UnableToCompleteException e) {
      // Already logged.
      return false;
    }
    return true;
  }

  @Override
  protected int doStartUpServer() {
    // Create the war directory if it doesn't exist
    File warDir = options.getWarDir();
    if (!warDir.exists() && !warDir.mkdirs()) {
      getTopLogger().log(TreeLogger.ERROR, "Unable to create war directory " + warDir);
      return -1;
    }

    Event jettyStartupEvent = SpeedTracerLogger.start(DevModeEventType.JETTY_STARTUP);
    boolean clearCallback = true;
    try {
      ui.setCallback(RestartServerEvent.getType(), this);

      ServletContainerLauncher scl = options.getServletContainerLauncher();

      TreeLogger serverLogger = ui.getWebServerLogger(getWebServerName(), scl.getIconBytes());

      String sclArgs = options.getServletContainerLauncherArgs();
      if (sclArgs != null) {
        if (!scl.processArguments(serverLogger, sclArgs)) {
          return -1;
        }
      }

      isHttps = scl.isSecure();

      // Tell the UI if the web server is secure
      if (isHttps) {
        ui.setWebServerSecure(serverLogger);
      }

      /*
       * TODO: This is a hack to pass the base log level to the SCL. We'll have
       * to figure out a better way to do this for SCLs in general.
       */
      if (scl instanceof JettyLauncher) {
        JettyLauncher jetty = (JettyLauncher) scl;
        jetty.setBaseRequestLogLevel(getBaseLogLevelForUI());
      }
      scl.setBindAddress(options.getBindAddress());

      if (serverLogger.isLoggable(TreeLogger.TRACE)) {
        serverLogger.log(TreeLogger.TRACE, "Starting HTTP on port " + getPort(), null);
      }
      server = scl.start(serverLogger, getPort(), options.getWarDir());
      assert (server != null);
      clearCallback = false;
      return server.getPort();
    } catch (BindException e) {
      System.err.println("Port " + options.getBindAddress() + ':' + getPort()
          + " is already in use; you probably still have another session active");
    } catch (Exception e) {
      System.err.println("Unable to start embedded HTTP server");
      e.printStackTrace();
    } finally {
      jettyStartupEvent.end();
      if (clearCallback) {
        // Clear the callback if we failed to start the server
        ui.setCallback(RestartServerEvent.getType(), null);
      }
    }
    return -1;
  }

  @Override
  protected void ensureCodeServerListener() {
    if (listener == null) {
      if (options.isSuperDevMode()) {
        listener = new SuperDevListener(getTopLogger(), options);
      } else {
        listener =
            new BrowserListener(getTopLogger(), options, new OophmSessionHandler(getTopLogger(),
                browserHost));
      }
      listener.start();
    }
  }

  protected String getWebServerName() {
    return options.getServletContainerLauncher().getName();
  }

  @Override
  protected void inferStartupUrls() {
    // Look for launchable files directly under war
    File warDir = options.getWarDir();
    if (!warDir.exists()) {
      // if the war directory doesn't exist, there are no startup files there
      return;
    }
    for (File htmlFile : warDir.listFiles(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return STARTUP_FILE_PATTERN.matcher(name).matches();
      }
    })) {
      options.addStartupURL(htmlFile.getName());
    }
  }

  @Override
  protected ModuleDef loadModule(TreeLogger logger, String moduleName, boolean refresh)
      throws UnableToCompleteException {
    if (startupModules.containsKey(moduleName)) {
      // First load of a startup module; remove from list, no need to refresh.
      return startupModules.remove(moduleName);
    }
    return super.loadModule(logger, moduleName, refresh);
  }

  @Override
  protected URL makeStartupUrl(String url) throws UnableToCompleteException {
    return listener.makeStartupUrl(url);
  }

  @Override
  protected synchronized void produceOutput(TreeLogger logger, StandardLinkerContext linkerStack,
      ArtifactSet artifacts, ModuleDef module, boolean isRelink) throws UnableToCompleteException {
    listener.writeCompilerOutput(linkerStack, artifacts, module, isRelink);
  }

  @Override
  protected void warnAboutNoStartupUrls() {
    getTopLogger().log(TreeLogger.WARN,
        "No startup URLs supplied and no plausible ones found -- use " + "-startupUrl");
  }

  private void validateServletTags(TreeLogger logger, ServletValidator servletValidator,
      ServletWriter servletWriter, ModuleDef module) {
    String[] servletPaths = module.getServletPaths();
    if (servletPaths.length == 0) {
      return;
    }

    TreeLogger servletLogger =
        logger.branch(TreeLogger.DEBUG, "Validating <servlet> tags for module '" + module.getName()
            + "'", null, new InstalledHelpInfo("servletMappings.html"));
    for (String servletPath : servletPaths) {
      String servletClass = module.findServletForPath(servletPath);
      assert (servletClass != null);
      // Prefix module name to convert module mapping to global mapping.
      servletPath = "/" + module.getName() + servletPath;
      if (servletValidator == null) {
        servletWriter.addMapping(servletClass, servletPath);
      } else {
        servletValidator.validate(servletLogger, servletClass, servletPath);
      }
    }
  }

}
