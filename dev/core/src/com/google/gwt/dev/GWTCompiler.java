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
import com.google.gwt.dev.CompileTaskRunner.CompileTask;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.ModuleDefLoader;
import com.google.gwt.dev.jjs.JJSOptions;
import com.google.gwt.dev.jjs.PermutationResult;
import com.google.gwt.dev.shell.CheckForUpdates;
import com.google.gwt.dev.shell.CheckForUpdates.UpdateResult;
import com.google.gwt.dev.util.FileBackedObject;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.arg.ArgHandlerLocalWorkers;
import com.google.gwt.dev.util.arg.ArgHandlerOutDir;
import com.google.gwt.dev.util.arg.ArgHandlerWorkDirOptional;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;
import com.google.gwt.util.tools.ToolBase;
import com.google.gwt.util.tools.Utility;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.FutureTask;

/**
 * The main executable entry point for the GWT Java to JavaScript compiler.
 * 
 * @deprecated Use {@link Compiler} instead
 */
@Deprecated
public class GWTCompiler {

  static final class ArgProcessor extends PrecompileTaskArgProcessor {
    public ArgProcessor(LegacyCompilerOptions options) {
      super(options);

      registerHandler(new ArgHandlerOutDir(options));

      // Override the ArgHandlerWorkDirRequired in the super class.
      registerHandler(new ArgHandlerWorkDirOptional(options));

      registerHandler(new ArgHandlerLocalWorkers(options));
    }

    @Override
    protected String getName() {
      return GWTCompiler.class.getName();
    }
  }

  /**
   * Simple implementation of {@link LegacyCompilerOptions}.
   */
  public static class GWTCompilerOptionsImpl extends PrecompileTaskOptionsImpl
      implements LegacyCompilerOptions {

    private int localWorkers;
    private File outDir;

    public GWTCompilerOptionsImpl() {
    }

    public GWTCompilerOptionsImpl(LegacyCompilerOptions other) {
      copyFrom(other);
    }

    public void copyFrom(LegacyCompilerOptions other) {
      super.copyFrom(other);
      setLocalWorkers(other.getLocalWorkers());
      setOutDir(other.getOutDir());
    }

    @Override
    public int getLocalWorkers() {
      return localWorkers;
    }

    @Override
    public File getOutDir() {
      return outDir;
    }

    @Override
    public void setLocalWorkers(int localWorkers) {
      this.localWorkers = localWorkers;
    }

    @Override
    public void setOutDir(File outDir) {
      this.outDir = outDir;
    }
  }

  public static void main(String[] args) {
    ToolBase.legacyWarn(GWTCompiler.class, Compiler.class);
    SpeedTracerLogger.init();
    Event compileEvent = SpeedTracerLogger.start(CompilerEventType.COMPILE);

    /*
     * NOTE: main always exits with a call to System.exit to terminate any
     * non-daemon threads that were started in Generators. Typically, this is to
     * shutdown AWT related threads, since the contract for their termination is
     * still implementation-dependent.
     */
    final LegacyCompilerOptions options = new GWTCompilerOptionsImpl();
    boolean success = false;
    if (new ArgProcessor(options).processArgs(args)) {
      CompileTask task = new CompileTask() {
        @Override
        public boolean run(TreeLogger logger) throws UnableToCompleteException {
          FutureTask<UpdateResult> updater = null;
          if (!options.isUpdateCheckDisabled()) {
            updater = CheckForUpdates.checkForUpdatesInBackgroundThread(logger,
                CheckForUpdates.ONE_DAY);
          }
          boolean success = new GWTCompiler(options).run(logger);
          if (success) {
            CheckForUpdates.logUpdateAvailable(logger, updater);
          }
          return success;
        }
      };
      if (CompileTaskRunner.runWithAppropriateLogger(options, task)) {
        // Exit w/ success code.
        success = true;
      }
    }

    compileEvent.end();
    // Exit w/ non-success code.
    System.exit(success ? 0 : 1);
  }

  private final GWTCompilerOptionsImpl options;

  public GWTCompiler(LegacyCompilerOptions options) {
    this.options = new GWTCompilerOptionsImpl(options);
  }

  /**
   * Compiles the set of modules specified in the options.
   */
  public boolean run(TreeLogger logger) throws UnableToCompleteException {
    ModuleDef[] modules = new ModuleDef[options.getModuleNames().size()];
    int i = 0;
    for (String moduleName : options.getModuleNames()) {
      modules[i++] = ModuleDefLoader.loadFromClassPath(logger, moduleName, true);
    }
    return run(logger, modules);
  }

  /**
   * Compiles a specific set of modules.
   */
  public boolean run(TreeLogger logger, ModuleDef... modules)
      throws UnableToCompleteException {
    Event compileEvent = SpeedTracerLogger.start(CompilerEventType.COMPILE);
    boolean tempWorkDir = false;
    try {
      if (options.getWorkDir() == null) {
        options.setWorkDir(Utility.makeTemporaryDirectory(null, "gwtc"));
        tempWorkDir = true;
      }

      for (ModuleDef module : modules) {
        String moduleName = module.getName();

        if (options.isValidateOnly()) {
          if (!Precompile.validate(logger, options, module, options.getGenDir())) {
            return false;
          }
        } else {
          long compileStart = System.currentTimeMillis();
          logger = logger.branch(TreeLogger.INFO, "Compiling module "
              + moduleName);

          // Optimize early since permutation compiles will run in process.
          options.setOptimizePrecompile(true);
          Precompilation precompilation = Precompile.precompile(logger,
              options, module, options.getGenDir());

          if (precompilation == null) {
            return false;
          }
          Permutation[] allPerms = precompilation.getPermutations();
          List<FileBackedObject<PermutationResult>> resultFiles = CompilePerms.makeResultFiles(
              options.getCompilerWorkDir(moduleName), allPerms);
          CompilePerms.compile(logger, precompilation, allPerms,
              options.getLocalWorkers(), resultFiles);

          ArtifactSet generatedArtifacts = precompilation.getGeneratedArtifacts();
          JJSOptions precompileOptions = precompilation.getUnifiedAst().getOptions();

          precompilation = null; // No longer needed, so save the memory

          Link.legacyLink(logger.branch(TreeLogger.TRACE, "Linking into "
              + options.getOutDir().getPath()), module, generatedArtifacts,
              allPerms, resultFiles, options.getOutDir(), precompileOptions);

          long compileDone = System.currentTimeMillis();
          long delta = compileDone - compileStart;
          if (logger.isLoggable(TreeLogger.INFO)) {
            logger.log(TreeLogger.INFO, "Compilation succeeded -- "
                + String.format("%.3f", delta / 1000d) + "s");
          }
        }
      }
    } catch (IOException e) {
      logger.log(TreeLogger.ERROR, "Unable to create compiler work directory",
          e);
      return false;
    } finally {
      compileEvent.end();
      if (tempWorkDir) {
        Util.recursiveDelete(options.getWorkDir(), false);
      }
    }
    return true;
  }
}
