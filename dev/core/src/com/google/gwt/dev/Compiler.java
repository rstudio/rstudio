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
import com.google.gwt.dev.javac.UnitCacheSingleton;
import com.google.gwt.dev.jjs.JJSOptions;
import com.google.gwt.dev.jjs.PermutationResult;
import com.google.gwt.dev.shell.CheckForUpdates;
import com.google.gwt.dev.shell.CheckForUpdates.UpdateResult;
import com.google.gwt.dev.util.Memory;
import com.google.gwt.dev.util.PersistenceBackedObject;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.arg.ArgHandlerDeployDir;
import com.google.gwt.dev.util.arg.ArgHandlerExtraDir;
import com.google.gwt.dev.util.arg.ArgHandlerLocalWorkers;
import com.google.gwt.dev.util.arg.ArgHandlerSaveSourceOutput;
import com.google.gwt.dev.util.arg.ArgHandlerWarDir;
import com.google.gwt.dev.util.arg.ArgHandlerWorkDirOptional;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;
import com.google.gwt.thirdparty.guava.common.collect.Sets;
import com.google.gwt.util.tools.Utility;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.FutureTask;

/**
 * The main executable entry point for the GWT Java to JavaScript compiler.
 */
public class Compiler {

  static class ArgProcessor extends PrecompileTaskArgProcessor {
    public ArgProcessor(CompilerOptions options) {
      super(options);

      registerHandler(new ArgHandlerLocalWorkers(options));

      // Override the ArgHandlerWorkDirRequired in the super class.
      registerHandler(new ArgHandlerWorkDirOptional(options));

      registerHandler(new ArgHandlerWarDir(options));
      registerHandler(new ArgHandlerDeployDir(options));
      registerHandler(new ArgHandlerExtraDir(options));
      registerHandler(new ArgHandlerSaveSourceOutput(options));
    }

    @Override
    protected String getName() {
      return Compiler.class.getName();
    }
  }

  public static void main(String[] args) {
    Memory.initialize();
    if (System.getProperty("gwt.jjs.dumpAst") != null) {
      System.out.println("Will dump AST to: "
          + System.getProperty("gwt.jjs.dumpAst"));
    }

    SpeedTracerLogger.init();

    /*
     * NOTE: main always exits with a call to System.exit to terminate any
     * non-daemon threads that were started in Generators. Typically, this is to
     * shutdown AWT related threads, since the contract for their termination is
     * still implementation-dependent.
     */
    final CompilerOptions options = new CompilerOptionsImpl();
    if (new ArgProcessor(options).processArgs(args)) {
      CompileTask task = new CompileTask() {
        @Override
        public boolean run(TreeLogger logger) throws UnableToCompleteException {
          FutureTask<UpdateResult> updater = null;
          if (!options.isUpdateCheckDisabled()) {
            updater = CheckForUpdates.checkForUpdatesInBackgroundThread(logger,
                CheckForUpdates.ONE_DAY);
          }
          boolean success = new Compiler(options).run(logger);
          if (success) {
            CheckForUpdates.logUpdateAvailable(logger, updater);
          }
          return success;
        }
      };
      if (CompileTaskRunner.runWithAppropriateLogger(options, task)) {
        // Exit w/ success code.
        System.exit(0);
      }
    }
    // Exit w/ non-success code.
    System.exit(1);
  }

  private final CompilerContext.Builder compilerContextBuilder;
  private CompilerContext compilerContext;
  private final CompilerOptionsImpl options;

  public Compiler(CompilerOptions compilerOptions) {
    this.options = new CompilerOptionsImpl(compilerOptions);
    this.compilerContextBuilder = new CompilerContext.Builder();
    this.compilerContext = compilerContextBuilder.options(options).build();
  }

  public boolean run(TreeLogger logger) throws UnableToCompleteException {
    ModuleDef[] modules = new ModuleDef[options.getModuleNames().size()];
    int i = 0;
    for (String moduleName : options.getModuleNames()) {
      modules[i++] = ModuleDefLoader.loadFromClassPath(logger, compilerContext, moduleName, true);
    }
    return run(logger, modules);
  }

  public boolean run(TreeLogger logger, ModuleDef... modules)
      throws UnableToCompleteException {
    boolean tempWorkDir = false;
    try {
      if (options.getWorkDir() == null) {
        options.setWorkDir(Utility.makeTemporaryDirectory(null, "gwtc"));
        tempWorkDir = true;
      }
      if ((options.isSoycEnabled() || options.isJsonSoycEnabled()) &&
           options.getExtraDir() == null) {
        options.setExtraDir(new File("extras"));
      }

      File persistentUnitCacheDir = null;
      if (options.getWarDir() != null && !options.getWarDir().getName().endsWith(".jar")) {
        persistentUnitCacheDir = new File(options.getWarDir(), "../");
      }
      compilerContext = compilerContextBuilder.unitCache(
          UnitCacheSingleton.get(logger, persistentUnitCacheDir)).build();

      for (ModuleDef module : modules) {
        compilerContext = compilerContextBuilder.module(module).build();
        String moduleName = module.getCanonicalName();
        if (options.isValidateOnly()) {
          if (!Precompile.validate(logger, compilerContext)) {
            return false;
          }
        } else {
          long compileStart = System.currentTimeMillis();
          TreeLogger branch = logger.branch(TreeLogger.INFO,
              "Compiling module " + moduleName);

          // Optimize early since permutation compiles will run in process.
          options.setOptimizePrecompile(true);
          Precompilation precompilation = Precompile.precompile(branch, compilerContext);
          if (precompilation == null) {
            return false;
          }
          // TODO: move to precompile() after params are refactored
          if (!options.shouldSaveSource()) {
            precompilation.removeSourceArtifacts(branch);
          }

          Event compilePermutationsEvent = SpeedTracerLogger.start(CompilerEventType.COMPILE_PERMUTATIONS);
          Permutation[] allPerms = precompilation.getPermutations();
          List<PersistenceBackedObject<PermutationResult>> resultFiles =
              CompilePerms.makeResultFiles(options.getCompilerWorkDir(moduleName), allPerms);
          CompilePerms.compile(branch, compilerContext, precompilation, allPerms,
              options.getLocalWorkers(), resultFiles);
          compilePermutationsEvent.end();

          ArtifactSet generatedArtifacts = precompilation.getGeneratedArtifacts();
          JJSOptions precompileOptions = precompilation.getUnifiedAst().getOptions();

          precompilation = null; // No longer needed, so save the memory

          Event linkEvent = SpeedTracerLogger.start(CompilerEventType.LINK);
          File absPath = new File(options.getWarDir(), module.getName());
          absPath = absPath.getAbsoluteFile();

          String logMessage = "Linking into " + absPath;
          if (options.getExtraDir() != null) {
            File absExtrasPath = new File(options.getExtraDir(),
                module.getName());
            absExtrasPath = absExtrasPath.getAbsoluteFile();
            logMessage += "; Writing extras to " + absExtrasPath;
          }
          Link.link(logger.branch(TreeLogger.TRACE, logMessage), module,
              module.getPublicResourceOracle(), generatedArtifacts, allPerms, resultFiles,
              Sets.<PermutationResult>newHashSet(), precompileOptions, options);

          linkEvent.end();
          long compileDone = System.currentTimeMillis();
          long delta = compileDone - compileStart;
          if (branch.isLoggable(TreeLogger.INFO)) {
            branch.log(TreeLogger.INFO, "Compilation succeeded -- "
                + String.format("%.3f", delta / 1000d) + "s");
          }
        }
      }

    } catch (IOException e) {
      logger.log(TreeLogger.ERROR, "Unable to create compiler work directory",
          e);
      return false;
    } finally {
      if (tempWorkDir) {
        Util.recursiveDelete(options.getWorkDir(), false);
      }
    }
    return true;
  }
}
