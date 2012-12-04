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
import com.google.gwt.core.ext.linker.EmittedArtifact.Visibility;
import com.google.gwt.core.ext.linker.impl.StandardLinkerContext;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.shell.WorkDirs;
import com.google.gwt.dev.shell.tomcat.EmbeddedTomcatServer;
import com.google.gwt.dev.util.OutputFileSetOnDirectory;
import com.google.gwt.dev.util.arg.ArgHandlerDisableUpdateCheck;
import com.google.gwt.dev.util.arg.ArgHandlerOutDir;
import com.google.gwt.util.tools.ArgHandlerExtra;

import java.io.File;
import java.util.Locale;
import java.util.Set;

/**
 * The main executable class for the hosted mode shell.
 */
@Deprecated
public class GWTShell extends DevModeBase {

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
  protected static class ArgProcessor extends DevModeBase.ArgProcessor {
    public ArgProcessor(ShellOptionsImpl options, boolean forceServer,
        boolean noURLs) {
      super(options, forceServer);
      if (!noURLs) {
        registerHandler(new ArgHandlerStartupURLsExtra(options));
      }
      registerHandler(new ArgHandlerOutDir(options));
      registerHandler(new ArgHandlerDisableUpdateCheck(options));
    }

    @Override
    protected String getName() {
      return GWTShell.class.getName();
    }
  }

  /**
   * Concrete class to implement all shell options.
   */
  protected static class ShellOptionsImpl extends HostedModeBaseOptionsImpl
      implements WorkDirs, LegacyCompilerOptions {
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
      File moduleWorkDir = new File(getWorkDir(), moduleDef.getName());
      return new File(moduleWorkDir, "public");
    }

    @Override
    public File getWorkDir() {
      return new File(getOutDir(), ".gwt-tmp");
    }

    public void setLocalWorkers(int localWorkers) {
      this.localWorkers = localWorkers;
    }

    public void setOutDir(File outDir) {
      this.outDir = outDir;
    }
  }

  public static String checkHost(String hostUnderConsideration,
      Set<String> hosts) {
    hostUnderConsideration = hostUnderConsideration.toLowerCase(Locale.ENGLISH);
    for (String rule : hosts) {
      // match on lowercased regex
      if (hostUnderConsideration.matches(".*" + rule + ".*")) {
        return rule;
      }
    }
    return null;
  }

  public static String computeHostRegex(String url) {
    // the entire URL up to the first slash not prefixed by a slash or colon.
    String raw = url.split("(?<![:/])/")[0];
    // escape the dots and put a begin line specifier on the result
    return "^" + raw.replaceAll("[.]", "[.]");
  }

  public static String formatRules(Set<String> invalidHttpHosts) {
    StringBuffer out = new StringBuffer();
    for (String rule : invalidHttpHosts) {
      out.append(rule);
      out.append(" ");
    }
    return out.toString();
  }

  public static void main(String[] args) {
    /*
     * NOTE: main always exits with a call to System.exit to terminate any
     * non-daemon threads that were started in Generators. Typically, this is to
     * shutdown AWT related threads, since the contract for their termination is
     * still implementation-dependent.
     */
    GWTShell gwtShell = new GWTShell();
    ArgProcessor argProcessor = new ArgProcessor(gwtShell.options, false, false);
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

  protected File outDir;

  @SuppressWarnings("unused")
  public void restartServer(TreeLogger logger) throws UnableToCompleteException {
    // Unimplemented.
  }

  @Override
  protected HostedModeBaseOptions createOptions() {
    return new ShellOptionsImpl();
  }

  @Override
  protected void doShutDownServer() {
    // Stop the HTTP server.
    //
    EmbeddedTomcatServer.stop();
  }

  @Override
  protected boolean doStartup() {
    File persistentCacheDir = new File(options.getWorkDir(), "gwt-unitCache");
    return super.doStartup(persistentCacheDir);
  }

  @Override
  protected int doStartUpServer() {
    // TODO(jat): find a safe way to get an icon for Tomcat
    TreeLogger logger = ui.getWebServerLogger("Tomcat", null);
    // TODO(bruce): make tomcat work in terms of the modular launcher
    String whyFailed = EmbeddedTomcatServer.start(isHeadless() ? getTopLogger()
        : logger, getPort(), options, shouldAutoGenerateResources());

    if (whyFailed != null) {
      getTopLogger().log(TreeLogger.ERROR, "Starting Tomcat: " + whyFailed);
      return -1;
    }
    return EmbeddedTomcatServer.getPort();
  }

  @Override
  protected synchronized void produceOutput(TreeLogger logger,
      StandardLinkerContext linkerStack, ArtifactSet artifacts,
      ModuleDef module, boolean isRelink) throws UnableToCompleteException {
    /*
     * Legacy: in GWTShell we only copy generated artifacts into the public gen
     * folder. Public files and "autogen" files have special handling (that
     * needs to die).
     */
    if (isRelink) {
      File outputDir = options.getShellPublicGenDir(module);
      outputDir.mkdirs();
      OutputFileSetOnDirectory outFileSet = new OutputFileSetOnDirectory(
          outputDir, "");
      linkerStack.produceOutput(logger, artifacts, Visibility.Public,
          outFileSet);
      outFileSet.close();
    }
  }

  protected boolean shouldAutoGenerateResources() {
    return true;
  }

  @Override
  protected void warnAboutNoStartupUrls() {
    getTopLogger().log(TreeLogger.WARN,
        "No startup URLs were supplied -- add them to the end of the GWTShell"
        + " command line");
  }
}
