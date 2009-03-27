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
import com.google.gwt.util.tools.ToolBase;

import java.io.File;

/**
 * The main executable class for the hosted mode shell.
 * 
 * @deprecated Use {@link HostedMode} instead
 */
@Deprecated
public class GWTShell extends SwtHostedModeBase {

  /**
   * Handles the list of startup urls that can be passed at the end of the
   * command line.
   */
  protected static class ArgHandlerStartupURLsExtra extends ArgHandlerExtra {

    private final OptionStartupURLs options;

    public ArgHandlerStartupURLsExtra(OptionStartupURLs options) {
      this.options = options;
    }

    @Override
    public boolean addExtraArg(String arg) {
      options.addStartupURL(arg);
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
  protected static class ArgProcessor extends HostedModeBase.ArgProcessor {
    public ArgProcessor(ShellOptionsImpl options, boolean forceServer,
        boolean noURLs) {
      super(options, forceServer);
      if (!noURLs) {
        registerHandler(new ArgHandlerStartupURLsExtra(options));
      }
      registerHandler(new ArgHandlerOutDir(options));
    }

    @Override
    protected String getName() {
      return GWTShell.class.getName();
    }
  }

  /**
   * Concrete class to implement all shell options.
   */
  static class ShellOptionsImpl extends HostedModeBaseOptionsImpl implements
      HostedModeBaseOptions, WorkDirs, LegacyCompilerOptions {
    private int localWorkers;
    private File outDir;

    public File getCompilerOutputDir(ModuleDef moduleDef) {
      return new File(getOutDir(), moduleDef.getName());
    }

    public int getLocalWorkers() {
      return localWorkers;
    }

    public File getOutDir() {
      return outDir;
    }

    public File getShellPublicGenDir(ModuleDef moduleDef) {
      return new File(getShellBaseWorkDir(moduleDef), "public");
    }

    @Override
    public File getWorkDir() {
      if (System.getProperty("com.google.gwt.shell.outdir") != null) {
        return new File(System.getProperty("com.google.gwt.shell.outdir"));
      }
      return new File(getOutDir(), ".gwt-tmp");
    }

    public void setLocalWorkers(int localWorkers) {
      this.localWorkers = localWorkers;
    }

    public void setOutDir(File outDir) {
      this.outDir = outDir;
    }
  }

  public static void main(String[] args) {
    ToolBase.legacyWarn(GWTShell.class, HostedMode.class);

    /*
     * NOTE: main always exits with a call to System.exit to terminate any
     * non-daemon threads that were started in Generators. Typically, this is to
     * shutdown AWT related threads, since the contract for their termination is
     * still implementation-dependent.
     */
    GWTShell gwtShell = new GWTShell();
    ArgProcessor argProcessor = new ArgProcessor(gwtShell.options, false, false);

    // deprecated old property way to set outputs
    if (System.getProperty("com.google.gwt.shell.outdir") != null) {
      gwtShell.options.setOutDir(new File(System.getProperty("com.google.gwt.shell.outdir")));
      gwtShell.options.setWorkDir(new File(System.getProperty("com.google.gwt.shell.outdir")));
    }

    if (argProcessor.processArgs(args)) {
      gwtShell.run();
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

  public WebServerRestart hasWebServer() {
    return WebServerRestart.NONE;
  }

  public void restartServer(TreeLogger logger) throws UnableToCompleteException {
    // Unimplemented.
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
    if (!new GWTCompiler(newOptions).run(logger, moduleDef)) {
      throw new UnableToCompleteException();
    }
  }

  @Override
  protected HostedModeBaseOptions createOptions() {
    return new ShellOptionsImpl();
  }

  @Override
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
  protected void doShutDownServer() {
    // Stop the HTTP server.
    //
    EmbeddedTomcatServer.stop();
  }

  @Override
  protected int doStartUpServer() {
    // TODO(bruce): make tomcat work in terms of the modular launcher
    String whyFailed = EmbeddedTomcatServer.start(getTopLogger(), getPort(),
        options, shouldAutoGenerateResources());

    // TODO(bruce): test that we can remove this old approach in favor of
    // a better, logger-based error reporting
    if (whyFailed != null) {
      System.err.println(whyFailed);
      return -1;
    }
    return EmbeddedTomcatServer.getPort();
  }

  @Override
  protected String getTitleText() {
    return "Google Web Toolkit Development Shell";
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

  /**
   * Whether this shell should auto-generate GWT resources when it recognizes
   * requests for them. By default this is true. Subclasses can disable such
   * auto-generation and make this servlet appear to be like any arbitrary web
   * server that knows nothing about GWT.
   */
  protected boolean shouldAutoGenerateResources() {
    return true;
  }
}
