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
import com.google.gwt.dev.Compiler.CompilerOptionsImpl;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.shell.ArtifactAcceptor;
import com.google.gwt.dev.shell.jetty.JettyLauncher;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.arg.ArgHandlerExtraDir;
import com.google.gwt.dev.util.arg.ArgHandlerLocalWorkers;
import com.google.gwt.dev.util.arg.ArgHandlerModuleName;
import com.google.gwt.dev.util.arg.ArgHandlerWarDir;
import com.google.gwt.dev.util.arg.ArgHandlerWorkDirOptional;
import com.google.gwt.util.tools.ArgHandlerString;
import com.google.gwt.util.tools.Utility;

import java.io.File;
import java.io.IOException;
import java.net.BindException;
import java.util.HashMap;
import java.util.Map;

/**
 * The main executable class for the hosted mode shell. NOTE: the public API for
 * this class is to be determined. Consider this class as having <b>no</b>
 * public API other than {@link #main(String[])}.
 */
public class HostedMode extends HostedModeBase {

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
      return "Prevents the embedded Tomcat server from running, even if a port is specified";
    }

    @Override
    public String getTag() {
      return "-server";
    }

    @Override
    public String[] getTagArgs() {
      return new String[] {"servletContainerLauncher"};
    }

    @Override
    public boolean setString(String sclClassName) {
      // Supercedes -noserver.
      options.setNoServer(false);
      Throwable t;
      try {
        Class<?> clazz = Class.forName(sclClassName, true,
            Thread.currentThread().getContextClassLoader());
        Class<? extends ServletContainerLauncher> sclClass = clazz.asSubclass(ServletContainerLauncher.class);
        options.setServletContainerLauncher(sclClass.newInstance());
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

  static class ArgProcessor extends HostedModeBase.ArgProcessor {
    public ArgProcessor(HostedModeOptions options) {
      super(options, false);
      registerHandler(new ArgHandlerServer(options));
      registerHandler(new ArgHandlerStartupURLs(options));
      registerHandler(new ArgHandlerWarDir(options));
      registerHandler(new ArgHandlerExtraDir(options));
      registerHandler(new ArgHandlerWorkDirOptional(options));
      registerHandler(new ArgHandlerLocalWorkers(options));
      registerHandler(new ArgHandlerModuleName(options) {
        @Override
        public String getPurpose() {
          return super.getPurpose() + " to host";
        }
      });
    }

    @Override
    protected String getName() {
      return HostedMode.class.getName();
    }
  }

  interface HostedModeOptions extends HostedModeBaseOptions, CompilerOptions {
    ServletContainerLauncher getServletContainerLauncher();

    void setServletContainerLauncher(ServletContainerLauncher scl);
  }

  /**
   * Concrete class to implement all hosted mode options.
   */
  static class HostedModeOptionsImpl extends HostedModeBaseOptionsImpl
      implements HostedModeOptions {
    private File extraDir;
    private int localWorkers;
    private ServletContainerLauncher scl;
    private File warDir;

    public File getExtraDir() {
      return extraDir;
    }

    public int getLocalWorkers() {
      return localWorkers;
    }

    public ServletContainerLauncher getServletContainerLauncher() {
      return scl;
    }

    public File getShellBaseWorkDir(ModuleDef moduleDef) {
      return new File(new File(getWorkDir(), moduleDef.getName()), "shell");
    }

    public File getShellPublicGenDir(ModuleDef moduleDef) {
      return new File(getShellBaseWorkDir(moduleDef), "public");
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

    public void setServletContainerLauncher(ServletContainerLauncher scl) {
      this.scl = scl;
    }

    public void setWarDir(File warDir) {
      this.warDir = warDir;
    }
  }

  public static void main(String[] args) {
    /*
     * NOTE: main always exits with a call to System.exit to terminate any
     * non-daemon threads that were started in Generators. Typically, this is to
     * shutdown AWT related threads, since the contract for their termination is
     * still implementation-dependent.
     */
    HostedMode hostedMode = new HostedMode();
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
   * Maps each active linker stack by module.
   */
  private final Map<String, StandardLinkerContext> linkerStacks = new HashMap<String, StandardLinkerContext>();

  /**
   * The set of specified modules by name; the keys represent the renamed name
   * of each module rather than the canonical name.
   */
  private Map<String, ModuleDef> modulesByName = new HashMap<String, ModuleDef>();

  /**
   * The server that was started.
   */
  private ServletContainer server;

  /**
   * Tracks whether we created a temp workdir that we need to destroy.
   */
  private boolean tempWorkDir = false;

  /**
   * Default constructor for testing; no public API yet.
   */
  HostedMode() {
  }

  @Override
  protected void compile(TreeLogger logger) throws UnableToCompleteException {
    CompilerOptions newOptions = new CompilerOptionsImpl(options);
    new Compiler(newOptions).run(logger);
  }

  protected void compile(TreeLogger logger, ModuleDef moduleDef)
      throws UnableToCompleteException {
    throw new UnsupportedOperationException();
  }

  @Override
  protected HostedModeBaseOptions createOptions() {
    return new HostedModeOptionsImpl();
  }

  @Override
  protected ArtifactAcceptor doCreateArtifactAcceptor(final ModuleDef module) {
    return new ArtifactAcceptor() {
      public void accept(TreeLogger logger, ArtifactSet newlyGeneratedArtifacts)
          throws UnableToCompleteException {
        relink(logger, module, newlyGeneratedArtifacts);
      }
    };
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
  protected boolean doStartup() {
    if (!super.doStartup()) {
      return false;
    }
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

    for (String moduleName : options.getModuleNames()) {
      TreeLogger loadLogger = getTopLogger().branch(TreeLogger.DEBUG,
          "Loading module " + moduleName);
      try {
        ModuleDef module = loadModule(loadLogger, moduleName, false);

        // TODO: Validate servlet tags.
        String[] servletPaths = module.getServletPaths();
        if (servletPaths.length > 0) {
          loadLogger.log(TreeLogger.WARN,
              "Ignoring legacy <servlet> tag(s) in module '" + moduleName
                  + "'; add servlet tags to your web.xml instead");
        }

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
    try {
      TreeLogger serverLogger = getTopLogger().branch(TreeLogger.INFO,
          "Starting HTTP on port " + getPort(), null);
      server = options.getServletContainerLauncher().start(serverLogger,
          getPort(), options.getWarDir());
      assert (server != null);
      return server.getPort();
    } catch (BindException e) {
      System.err.println("Port "
          + getPort()
          + " is already is use; you probably still have another session active");
    } catch (Exception e) {
      System.err.println("Unable to start embedded HTTP server");
      e.printStackTrace();
    }
    return -1;
  }

  @Override
  protected String getHost() {
    if (server != null) {
      return server.getHost();
    }
    return super.getHost();
  }

  @Override
  protected boolean initModule(String moduleName) {
    ModuleDef module = modulesByName.get(moduleName);
    if (module == null) {
      getTopLogger().log(
          TreeLogger.WARN,
          "Unknown module requested '"
              + moduleName
              + "'; all active GWT modules must be specified in the command line arguments");
      return false;
    }
    try {
      boolean shouldRefreshPage = false;
      if (module.isGwtXmlFileStale()) {
        shouldRefreshPage = true;
        module = loadModule(getTopLogger(), module.getCanonicalName(), false);
      }
      link(getTopLogger(), module);
      return shouldRefreshPage;
    } catch (UnableToCompleteException e) {
      // Already logged.
      return false;
    }
  }

  /*
   * Overridden to keep our map up to date.
   */
  @Override
  protected ModuleDef loadModule(TreeLogger logger, String moduleName,
      boolean refresh) throws UnableToCompleteException {
    ModuleDef module = super.loadModule(logger, moduleName, refresh);
    modulesByName.put(module.getName(), module);
    return module;
  }

  /**
   * Perform an initial hosted mode link, without overwriting newer or
   * unmodified files in the output folder.
   * 
   * @param logger the logger to use
   * @param module the module to link
   * @param includePublicFiles if <code>true</code>, include public files in
   *          the link, otherwise do not include them
   * @throws UnableToCompleteException
   */
  private void link(TreeLogger logger, ModuleDef module)
      throws UnableToCompleteException {
    // TODO: move the module-specific computations to a helper function.
    File moduleOutDir = new File(options.getWarDir(), module.getName());
    File moduleExtraDir = (options.getExtraDir() == null) ? null : new File(
        options.getExtraDir(), module.getName());

    // Create a new active linker stack for the fresh link.
    StandardLinkerContext linkerStack = new StandardLinkerContext(logger,
        module, options);
    linkerStacks.put(module.getName(), linkerStack);

    ArtifactSet artifacts = linkerStack.invokeLink(logger);
    linkerStack.produceOutputDirectory(logger, artifacts, moduleOutDir,
        moduleExtraDir);
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
  private void relink(TreeLogger logger, ModuleDef module,
      ArtifactSet newlyGeneratedArtifacts) throws UnableToCompleteException {
    // TODO: move the module-specific computations to a helper function.
    File moduleOutDir = new File(options.getWarDir(), module.getName());
    File moduleExtraDir = (options.getExtraDir() == null) ? null : new File(
        options.getExtraDir(), module.getName());

    // Find the existing linker stack.
    StandardLinkerContext linkerStack = linkerStacks.get(module.getName());
    assert linkerStack != null;

    ArtifactSet artifacts = linkerStack.invokeRelink(logger,
        newlyGeneratedArtifacts);
    linkerStack.produceOutputDirectory(logger, artifacts, moduleOutDir,
        moduleExtraDir);
  }
}
