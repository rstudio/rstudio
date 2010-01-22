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
import com.google.gwt.dev.shell.jetty.JettyLauncher;
import com.google.gwt.dev.ui.RestartServerCallback;
import com.google.gwt.dev.ui.RestartServerEvent;
import com.google.gwt.dev.util.InstalledHelpInfo;
import com.google.gwt.dev.util.OutputFileSetOnDirectory;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.arg.ArgHandlerExtraDir;
import com.google.gwt.dev.util.arg.ArgHandlerModuleName;
import com.google.gwt.dev.util.arg.ArgHandlerWarDir;
import com.google.gwt.dev.util.arg.ArgHandlerWorkDirOptional;
import com.google.gwt.util.tools.ArgHandlerString;
import com.google.gwt.util.tools.Utility;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.BindException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * The main executable class for the hosted mode shell. NOTE: the public API for
 * this class is to be determined. Consider this class as having <b>no</b>
 * public API other than {@link #main(String[])}.
 */
public class DevMode extends DevModeBase implements RestartServerCallback {

  /**
   * Handles the -server command line flag.
   */
  protected static class ArgHandlerServer extends ArgHandlerString {
    private HostedModeOptions options;

    public ArgHandlerServer(HostedModeOptions options) {
      this.options = options;
    }

    @Override
    public String[] getDefaultArgs() {
      if (options.isNoServer()) {
        return null;
      } else {
        return new String[] {getTag(), JettyLauncher.class.getName()};
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
      Throwable t;
      try {
        Class<?> clazz = Class.forName(sclClassName, true,
            Thread.currentThread().getContextClassLoader());
        Class<? extends ServletContainerLauncher> sclClass = clazz.asSubclass(ServletContainerLauncher.class);
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

  static class ArgProcessor extends DevModeBase.ArgProcessor {
    public ArgProcessor(HostedModeOptions options) {
      super(options, false);
      registerHandler(new ArgHandlerServer(options));
      registerHandler(new ArgHandlerStartupURLs(options));
      registerHandler(new ArgHandlerWarDir(options));
      registerHandler(new ArgHandlerExtraDir(options));
      registerHandler(new ArgHandlerWorkDirOptional(options));
      registerHandler(new ArgHandlerModuleName(options) {
        @Override
        public String getPurpose() {
          return super.getPurpose() + " to host";
        }
      });
    }

    @Override
    protected String getName() {
      return DevMode.class.getName();
    }
  }

  interface HostedModeOptions extends HostedModeBaseOptions, CompilerOptions {
    ServletContainerLauncher getServletContainerLauncher();

    String getServletContainerLauncherArgs();

    void setServletContainerLauncher(ServletContainerLauncher scl);

    void setServletContainerLauncherArgs(String args);
  }

  /**
   * Concrete class to implement all hosted mode options.
   */
  static class HostedModeOptionsImpl extends HostedModeBaseOptionsImpl
      implements HostedModeOptions {
    private File extraDir;
    private int localWorkers;
    private File outDir;
    private ServletContainerLauncher scl;
    private String sclArgs;
    private File warDir;

    public File getExtraDir() {
      return extraDir;
    }

    public int getLocalWorkers() {
      return localWorkers;
    }

    @Deprecated
    public File getOutDir() {
      return outDir;
    }

    public ServletContainerLauncher getServletContainerLauncher() {
      return scl;
    }

    public String getServletContainerLauncherArgs() {
      return sclArgs;
    }

    @Override
    public File getShellBaseWorkDir(ModuleDef moduleDef) {
      return new File(new File(getWorkDir(), moduleDef.getName()), "shell");
    }

    public File getWarDir() {
      return warDir;
    }

    public void setExtraDir(File extraDir) {
      this.extraDir = extraDir;
    }

    public void setLocalWorkers(int localWorkers) {
      this.localWorkers = localWorkers;
    }

    @Deprecated
    public void setOutDir(File outDir) {
      this.outDir = outDir;
    }

    public void setServletContainerLauncher(ServletContainerLauncher scl) {
      this.scl = scl;
    }

    public void setServletContainerLauncherArgs(String args) {
      sclArgs = args;
    }

    public void setWarDir(File warDir) {
      this.warDir = warDir;
    }
  }

  /**
   * The pattern for files usable as startup URLs.
   */
  private static final Pattern STARTUP_FILE_PATTERN = Pattern.compile(
      ".*\\.(html|jsp)", Pattern.CASE_INSENSITIVE);

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

  /**
   * Hiding super field because it's actually the same object, just with a
   * stronger type.
   */
  @SuppressWarnings("hiding")
  protected final HostedModeOptionsImpl options = (HostedModeOptionsImpl) super.options;

  /**
   * The server that was started.
   */
  private ServletContainer server;

  private final Map<String, ModuleDef> startupModules = new HashMap<String, ModuleDef>();

  /**
   * Tracks whether we created a temp workdir that we need to destroy.
   */
  private boolean tempWorkDir = false;

  /**
   * Default constructor for testing; no public API yet.
   */
  DevMode() {
  }

  /**
   * Called by the UI on a restart server event.
   */
  public void onRestartServer(TreeLogger logger) {
    try {
      server.refresh();
    } catch (UnableToCompleteException e) {
      // ignore, problem already logged
    }
  }

  @Override
  protected HostedModeBaseOptions createOptions() {
    return new HostedModeOptionsImpl();
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

    ServletValidator servletValidator = null;
    File webXml = new File(options.getWarDir(), "WEB-INF/web.xml");
    if (!options.isNoServer() && webXml.exists()) {
      servletValidator = ServletValidator.create(getTopLogger(), webXml);
    }

    TreeLogger branch = getTopLogger().branch(TreeLogger.TRACE,
        "Loading modules");
    for (String moduleName : options.getModuleNames()) {
      TreeLogger moduleBranch = branch.branch(TreeLogger.TRACE, moduleName);
      try {
        ModuleDef module = loadModule(moduleBranch, moduleName, false);
        // Create a hard reference to the module to avoid gc-ing it until we
        // actually load the module from the browser.
        startupModules.put(module.getName(), module);
        Util.recursiveDelete(options.getShellBaseWorkDir(module), false);
        if (!options.isNoServer()) {
          validateServletTags(moduleBranch, servletValidator, module, webXml);
        }
        TreeLogger loadLogger = moduleBranch.branch(TreeLogger.DEBUG,
            "Bootstrap link for command-line module '" + moduleName + "'");
        link(loadLogger, module);
      } catch (UnableToCompleteException e) {
        // Already logged.
        return false;
      }
    }
    return true;
  }

  @Override
  protected int doStartUpServer() {
    boolean clearCallback = true;
    try {
      ui.setCallback(RestartServerEvent.getType(), this);

      ServletContainerLauncher scl = options.getServletContainerLauncher();

      TreeLogger serverLogger = ui.getWebServerLogger(getWebServerName(), 
          scl.getIconBytes());

      String sclArgs = options.getServletContainerLauncherArgs();
      if (sclArgs != null) {
        if (!scl.processArguments(serverLogger, sclArgs)) {
          return -1;
        }
      }

      /*
       * TODO: This is a hack to pass the base log level to the SCL. We'll have
       * to figure out a better way to do this for SCLs in general.
       */
      if (scl instanceof JettyLauncher) {
        JettyLauncher jetty = (JettyLauncher) scl;
        jetty.setBaseRequestLogLevel(getBaseLogLevelForUI());
      }
      scl.setBindAddress(bindAddress);

      serverLogger.log(TreeLogger.TRACE, "Starting HTTP on port " + getPort(),
          null);
      server = scl.start(serverLogger, getPort(), options.getWarDir());
      assert (server != null);
      clearCallback = false;
      return server.getPort();
    } catch (BindException e) {
      System.err.println("Port " + bindAddress + ':' + getPort()
          + " is already is use; you probably still have another session active");
    } catch (Exception e) {
      System.err.println("Unable to start embedded HTTP server");
      e.printStackTrace();
    } finally {
      if (clearCallback) {
        // Clear the callback if we failed to start the server
        ui.setCallback(RestartServerEvent.getType(), null);
      }
    }
    return -1;
  }

  protected String getWebServerName() {
    return options.getServletContainerLauncher().getName();
  }

  @Override
  protected void inferStartupUrls() {
    // Look for launchable files directly under war
    File warDir = options.getWarDir();
    for (File htmlFile : warDir.listFiles(new FilenameFilter() {
      public boolean accept(File dir, String name) {
        return STARTUP_FILE_PATTERN.matcher(name).matches();
      }
    })) {
      options.addStartupURL(htmlFile.getName());
    }
  }

  @Override
  protected ModuleDef loadModule(TreeLogger logger, String moduleName,
      boolean refresh) throws UnableToCompleteException {
    if (startupModules.containsKey(moduleName)) {
      // First load of a startup module; remove from list, no need to refresh.
      return startupModules.remove(moduleName);
    }
    return super.loadModule(logger, moduleName, refresh);
  }

  @Override
  protected synchronized void produceOutput(TreeLogger logger,
      StandardLinkerContext linkerStack, ArtifactSet artifacts,
      ModuleDef module, boolean isRelink) throws UnableToCompleteException {
    OutputFileSetOnDirectory outFileSet = new OutputFileSetOnDirectory(
        options.getWarDir(), module.getName() + "/");
    linkerStack.produceOutput(logger, artifacts, false, outFileSet);
    outFileSet.close();

    if (options.getExtraDir() != null) {
      OutputFileSetOnDirectory extraFileSet = new OutputFileSetOnDirectory(
          options.getExtraDir(), module.getName() + "/");
      linkerStack.produceOutput(logger, artifacts, true, extraFileSet);
      extraFileSet.close();
    }
  }

  private void validateServletTags(TreeLogger logger,
      ServletValidator servletValidator, ModuleDef module, File webXml) {
    TreeLogger servletLogger = logger.branch(TreeLogger.DEBUG,
        "Validating <servlet> tags for module '" + module.getName() + "'",
        null, new InstalledHelpInfo("servletMappings.html"));
    String[] servletPaths = module.getServletPaths();
    if (servletValidator == null && servletPaths.length > 0) {
      servletLogger.log(
          TreeLogger.WARN,
          "Module declares "
              + servletPaths.length
              + " <servlet> declaration(s), but a valid 'web.xml' was not found at '"
              + webXml.getAbsolutePath() + "'");
    } else {
      for (String servletPath : servletPaths) {
        String servletClass = module.findServletForPath(servletPath);
        assert (servletClass != null);
        // Prefix module name to convert module mapping to global mapping.
        servletPath = "/" + module.getName() + servletPath;
        servletValidator.validate(servletLogger, servletClass, servletPath);
      }
    }
  }
}
