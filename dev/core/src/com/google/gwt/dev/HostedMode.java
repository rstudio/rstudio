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
import com.google.gwt.core.ext.linker.EmittedArtifact;
import com.google.gwt.core.ext.linker.impl.StandardLinkerContext;
import com.google.gwt.dev.Compiler.CompilerOptionsImpl;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.ModuleDefLoader;
import com.google.gwt.dev.shell.ArtifactAcceptor;
import com.google.gwt.dev.shell.ServletContainer;
import com.google.gwt.dev.shell.ServletContainerLauncher;
import com.google.gwt.dev.shell.jetty.JettyLauncher;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.arg.ArgHandlerExtraDir;
import com.google.gwt.dev.util.arg.ArgHandlerModuleName;
import com.google.gwt.dev.util.arg.ArgHandlerWarDir;
import com.google.gwt.dev.util.arg.ArgHandlerWorkDirOptional;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import com.google.gwt.util.tools.ArgHandlerString;
import com.google.gwt.util.tools.Utility;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.BindException;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * The main executable class for the hosted mode shell.
 */
public class HostedMode extends HostedModeBase {

  /**
   * Handles the -server command line flag.
   */
  protected class ArgHandlerServer extends ArgHandlerString {
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
      return new String[] {"serverLauncherClass"};
    }

    @Override
    public boolean setString(String arg) {
      // Supercedes -noserver.
      setRunTomcat(true);
      return setServer(console, arg);
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

  class ArgProcessor extends HostedModeBase.ArgProcessor {
    public ArgProcessor() {
      registerHandler(new ArgHandlerServer());
      registerHandler(new ArgHandlerNoServerFlag());
      registerHandler(new ArgHandlerStartupURLs());
      registerHandler(new ArgHandlerWarDir(options));
      registerHandler(new ArgHandlerExtraDir(options));
      registerHandler(new ArgHandlerWorkDirOptional(options) {
        @Override
        public String[] getDefaultArgs() {
          return new String[] {"-workDir", "work"};
        }
      });
      registerHandler(new ArgHandlerModuleName(options));
    }

    @Override
    protected String getName() {
      return GWTShell.class.getName();
    }
  }

  /**
   * Concrete class to implement all compiler options.
   */
  static class HostedModeOptionsImpl extends CompilerOptionsImpl implements
      HostedModeBaseOptions {
    public File getShellBaseWorkDir(ModuleDef moduleDef) {
      return new File(new File(getWorkDir(), moduleDef.getName()), "shell");
    }
  }

  public static void main(String[] args) {
    /*
     * NOTE: main always exits with a call to System.exit to terminate any
     * non-daemon threads that were started in Generators. Typically, this is to
     * shutdown AWT related threads, since the contract for their termination is
     * still implementation-dependent.
     */
    HostedMode shellMain = new HostedMode();
    if (shellMain.new ArgProcessor().processArgs(args)) {
      // Exit w/ success code.
      System.exit(0);
    }
    // Exit w/ non-success code.
    System.exit(-1);
  }

  protected final PrintWriterTreeLogger console = new PrintWriterTreeLogger(
      new PrintWriter(System.err, true));

  /**
   * Hiding super field because it's actually the same object, just with a
   * stronger type.
   */
  @SuppressWarnings("hiding")
  protected final HostedModeOptionsImpl options = (HostedModeOptionsImpl) super.options;

  /**
   * The servlet launcher to use (defaults to embedded Jetty).
   */
  private ServletContainerLauncher launcher = new JettyLauncher();

  private final Map<ModuleDef, StandardLinkerContext> linkerMap = new IdentityHashMap<ModuleDef, StandardLinkerContext>();

  /**
   * The server that was started.
   */
  private ServletContainer server;

  /**
   * Tracks whether we created a temp workdir that we need to destroy.
   */
  private boolean tempWorkDir = false;

  {
    console.setMaxDetail(TreeLogger.WARN);
  }

  public boolean setServer(TreeLogger logger, String serverClassName) {
    Throwable t;
    try {
      Class<?> clazz = Class.forName(serverClassName, true,
          Thread.currentThread().getContextClassLoader());
      Class<? extends ServletContainerLauncher> sclClass = clazz.asSubclass(ServletContainerLauncher.class);
      launcher = sclClass.newInstance();
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
    logger.log(TreeLogger.ERROR, "Unable to load server class '"
        + serverClassName + "'", t);
    return false;
  }

  protected void compile(TreeLogger logger, ModuleDef moduleDef)
      throws UnableToCompleteException {
    CompilerOptions newOptions = new CompilerOptionsImpl(options);
    newOptions.addModuleName(moduleDef.getName());
    new Compiler(newOptions).run(logger);
  }

  @Override
  protected HostedModeBaseOptions createOptions() {
    return new HostedModeOptionsImpl();
  }

  @Override
  protected ArtifactAcceptor doCreateArtifactAcceptor(final ModuleDef module) {
    final File moduleOutDir = new File(options.getWarDir(), module.getName());
    return new ArtifactAcceptor() {
      public void accept(TreeLogger logger, ArtifactSet newlyGeneratedArtifacts)
          throws UnableToCompleteException {
        StandardLinkerContext linkerStack = linkerMap.get(module);
        ArtifactSet artifacts = linkerStack.invokeRelink(logger,
            newlyGeneratedArtifacts);
        // TODO: extras
        linkerStack.produceOutputDirectory(logger, artifacts, moduleOutDir,
            null);
      }
    };
  }

  @Override
  protected void shutDownServer() {
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
  protected int startUpServer() {
    tempWorkDir = options.getWorkDir() == null;
    if (tempWorkDir) {
      try {
        options.setWorkDir(Utility.makeTemporaryDirectory(null, "gwtc"));
      } catch (IOException e) {
        System.err.println("Unable to create hosted mode work directory");
        e.printStackTrace();
        return -1;
      }
    }

    for (String moduleName : options.getModuleNames()) {
      TreeLogger linkLogger = getTopLogger().branch(TreeLogger.DEBUG,
          "Prelinking module " + moduleName);
      try {
        ModuleDef module = ModuleDefLoader.loadFromClassPath(linkLogger,
            moduleName);

        // TODO: Validate servlet tags.
        String[] servletPaths = module.getServletPaths();
        if (servletPaths.length > 0) {
          linkLogger.log(TreeLogger.WARN,
              "Ignoring legacy <servlet> tag(s) in module '" + moduleName
                  + "'; add servlet tags to your web.xml instead");
        }

        File moduleOutDir = new File(options.getWarDir(), moduleName);
        StandardLinkerContext linkerStack = new StandardLinkerContext(
            linkLogger, module, options);
        linkerMap.put(module, linkerStack);

        // TODO: remove all public files initially, only conditionally emit.
        ArtifactSet artifacts = linkerStack.invokeLink(linkLogger);
        for (EmittedArtifact artifact : artifacts.find(EmittedArtifact.class)) {
          TreeLogger artifactLogger = linkLogger.branch(TreeLogger.DEBUG,
              "Emitting resource " + artifact.getPartialPath(), null);

          if (!artifact.isPrivate()) {
            File outFile = new File(moduleOutDir, artifact.getPartialPath());
            // if (!outFile.exists()) {
            Util.copy(artifactLogger, artifact.getContents(artifactLogger),
                outFile);
            outFile.setLastModified(artifact.getLastModified());
            // }
          }
        }
      } catch (UnableToCompleteException e) {
        // Already logged.
        return -1;
      }
    }

    try {
      TreeLogger serverLogger = getTopLogger().branch(TreeLogger.INFO,
          "Starting HTTP on port " + getPort(), null);
      server = launcher.start(serverLogger, getPort(), options.getWarDir());
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
}
