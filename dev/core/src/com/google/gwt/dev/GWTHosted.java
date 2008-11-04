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
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.ModuleDefLoader;
import com.google.gwt.dev.shell.ArtifactAcceptor;
import com.google.gwt.dev.shell.GWTShellServletFilter;
import com.google.gwt.dev.shell.ServletContainer;
import com.google.gwt.dev.shell.jetty.JettyLauncher;
import com.google.gwt.dev.util.PerfLogger;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import com.google.gwt.util.tools.ArgHandlerExtra;
import com.google.gwt.util.tools.ArgHandlerString;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

/**
 * The main executable class for the hosted mode shell.
 */
public class GWTHosted extends GWTShell {

  /**
   * Handles the set of modules that can be passed at the end of the command
   * line.
   */
  protected class ArgHandlerModulesExtra extends ArgHandlerExtra {

    private final PrintWriterTreeLogger console = new PrintWriterTreeLogger(
        new PrintWriter(System.err));
    {
      console.setMaxDetail(TreeLogger.WARN);
    }

    @Override
    public boolean addExtraArg(String arg) {
      return addModule(console, arg);
    }

    @Override
    public String getPurpose() {
      return "Specifies the set of modules to host";
    }

    @Override
    public String[] getTagArgs() {
      return new String[] {"module"};
    }
  }

  /**
   * Handles a startup url that can be passed on the command line.
   */
  protected class ArgHandlerStartupURLs extends ArgHandlerString {

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
      addStartupURL(arg);
      return true;
    }
  }

  public static void main(String[] args) {
    /*
     * NOTE: main always exits with a call to System.exit to terminate any
     * non-daemon threads that were started in Generators. Typically, this is to
     * shutdown AWT related threads, since the contract for their termination is
     * still implementation-dependent.
     */
    BootStrapPlatform.init();
    GWTHosted shellMain = new GWTHosted();
    if (shellMain.processArgs(args)) {
      shellMain.run();
    }
    System.exit(0);
  }

  private Set<ModuleDef> modules = new HashSet<ModuleDef>();

  private ServletContainer server;

  private GWTShellServletFilter servletFilter;

  public GWTHosted() {
    super(false, true);
    registerHandler(new ArgHandlerStartupURLs());
    registerHandler(new ArgHandlerModulesExtra());
  }

  public boolean addModule(TreeLogger logger, String moduleName) {
    try {
      ModuleDef moduleDef = ModuleDefLoader.loadFromClassPath(logger,
          moduleName);
      modules.add(moduleDef);
      return true;
    } catch (UnableToCompleteException e) {
      System.err.println("Unable to load module '" + moduleName + "'");
      return false;
    }
  }

  @Override
  protected ArtifactAcceptor doCreateArtifactAcceptor(final ModuleDef module) {
    return new ArtifactAcceptor() {
      public void accept(TreeLogger logger, ArtifactSet newlyGeneratedArtifacts)
          throws UnableToCompleteException {
        servletFilter.relink(logger, module, newlyGeneratedArtifacts);
      }
    };
  }

  @Override
  protected void shutDown() {
    if (server != null) {
      try {
        server.stop();
      } catch (UnableToCompleteException e) {
        // Already logged.
      }
      server = null;
    }
  }

  @Override
  protected int startUpServer() {
    PerfLogger.start("GWTShell.startup (Jetty launch)");
    JettyLauncher launcher = new JettyLauncher();
    try {
      TreeLogger serverLogger = getTopLogger().branch(TreeLogger.INFO,
          "Starting HTTP on port " + getPort(), null);
      ModuleDef[] moduleArray = modules.toArray(new ModuleDef[modules.size()]);
      for (ModuleDef moduleDef : moduleArray) {
        String[] servletPaths = moduleDef.getServletPaths();
        if (servletPaths.length > 0) {
          serverLogger.log(TreeLogger.WARN,
              "Ignoring legacy <servlet> tag(s) in module '"
                  + moduleDef.getName()
                  + "'; add servlet tags to your web.xml instead");
        }
      }
      servletFilter = new GWTShellServletFilter(serverLogger, options,
          moduleArray);
      server = launcher.start(serverLogger, getPort(), options.getOutDir(),
          servletFilter);
    } catch (UnableToCompleteException e) {
      PerfLogger.end();
      return -1;
    }
    assert (server != null);

    PerfLogger.end();
    return server.getPort();
  }

}
