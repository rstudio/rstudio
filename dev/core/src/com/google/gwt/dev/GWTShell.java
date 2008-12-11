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
import com.google.gwt.dev.GWTCompiler.GWTCompilerOptionsImpl;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.shell.ArtifactAcceptor;
import com.google.gwt.dev.shell.WorkDirs;
import com.google.gwt.dev.shell.tomcat.EmbeddedTomcatServer;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.arg.ArgHandlerOutDir;
import com.google.gwt.util.tools.ArgHandlerExtra;

import java.io.File;

/**
 * The main executable class for the hosted mode shell.
 * 
 * @deprecated use {@link HostedMode} instead
 */
@Deprecated
public class GWTShell extends HostedModeBase {

  /**
   * Handles the list of startup urls that can be passed at the end of the
   * command line.
   */
  protected class ArgHandlerStartupURLsExtra extends ArgHandlerExtra {

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
   * The GWTShell argument processor.
   */
  protected class ArgProcessor extends HostedModeBase.ArgProcessor {
    public ArgProcessor(boolean forceServer, boolean noURLs) {
      if (!forceServer) {
        registerHandler(new ArgHandlerNoServerFlag());
      }

      if (!noURLs) {
        registerHandler(new ArgHandlerStartupURLsExtra());
      }
      registerHandler(new ArgHandlerOutDir(options));
    }

    @Override
    protected String getName() {
      return GWTShell.class.getName();
    }
  }

  /**
   * Concrete class to implement all compiler options.
   */
  static class ShellOptionsImpl extends GWTCompilerOptionsImpl implements
      HostedModeBaseOptions, WorkDirs {
    public File getCompilerOutputDir(ModuleDef moduleDef) {
      return new File(getOutDir(), moduleDef.getName());
    }

    public File getShellBaseWorkDir(ModuleDef moduleDef) {
      return new File(new File(getWorkDir(), moduleDef.getName()), "shell");
    }

    public File getShellPublicGenDir(ModuleDef moduleDef) {
      return new File(getShellBaseWorkDir(moduleDef), "public");
    }

    @Override
    public File getWorkDir() {
      return new File(getOutDir(), ".gwt-tmp");
    }
  }

  public static void main(String[] args) {
    System.err.println("WARNING: '" + GWTShell.class.getName()
        + "' is deprecated and will be removed in a future release.");
    System.err.println("Use '" + HostedMode.class.getName() + "' instead.");
    /*
     * NOTE: main always exits with a call to System.exit to terminate any
     * non-daemon threads that were started in Generators. Typically, this is to
     * shutdown AWT related threads, since the contract for their termination is
     * still implementation-dependent.
     */
    GWTShell shellMain = new GWTShell();
    if (shellMain.new ArgProcessor(false, false).processArgs(args)) {
      shellMain.run();
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
  protected final ShellOptionsImpl options = (ShellOptionsImpl) super.options;

  public LegacyCompilerOptions getCompilerOptions() {
    return new GWTCompilerOptionsImpl(options);
  }

  public void setCompilerOptions(CompilerOptions options) {
    this.options.copyFrom(options);
  }

  @Override
  protected void compile(TreeLogger logger) throws UnableToCompleteException {
    throw new UnsupportedOperationException();
  }

  /**
   * Compiles a logical module def. The caller can modify the specified module
   * def programmatically in some cases (this is needed for JUnit support, for
   * example).
   */
  @Override
  protected void compile(TreeLogger logger, ModuleDef moduleDef)
      throws UnableToCompleteException {
    LegacyCompilerOptions newOptions = new GWTCompilerOptionsImpl(options);
    new GWTCompiler(newOptions).run(logger, moduleDef);
  }

  @Override
  protected HostedModeBaseOptions createOptions() {
    return new ShellOptionsImpl();
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

  @Override
  protected boolean initModule(String moduleName) {
    /*
     * Not used in legacy mode due to GWTShellServlet playing this role.
     * 
     * TODO: something smarter here and actually make GWTShellServlet less
     * magic?
     */
    return false;
  }

  @Override
  protected void shutDownServer() {
    // Stop the HTTP server.
    //
    EmbeddedTomcatServer.stop();
  }

  protected int startUpServer() {
    // TODO(bruce): make tomcat work in terms of the modular launcher
    String whyFailed = EmbeddedTomcatServer.start(getTopLogger(), getPort(),
        options);

    // TODO(bruce): test that we can remove this old approach in favor of
    // a better, logger-based error reporting
    if (whyFailed != null) {
      System.err.println(whyFailed);
      return -1;
    }
    return EmbeddedTomcatServer.getPort();
  }
}
