/*
 * Copyright 2010 Google Inc.
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
import com.google.gwt.core.ext.linker.impl.StandardLinkerContext;
import com.google.gwt.dev.CompileTaskRunner.CompileTask;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.ModuleDefLoader;
import com.google.gwt.dev.cfg.PropertyPermutations;
import com.google.gwt.dev.shell.CheckForUpdates;
import com.google.gwt.dev.shell.CheckForUpdates.UpdateResult;
import com.google.gwt.dev.util.Memory;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.arg.ArgHandlerPerm;
import com.google.gwt.dev.util.arg.OptionPerm;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;

import java.io.File;
import java.util.List;
import java.util.concurrent.FutureTask;

/**
 * Creates a ready-to-compile AST for a single permutation.
 *
 * Also collapses similar permutations and builds them together.
 */
public class PrecompileOnePerm {

  /**
   * The set of options for the precompiler.
   */
  public interface PrecompileOnePermOptions extends
      PrecompileTaskOptions, OptionPerm {
  }

  static class ArgProcessor extends PrecompileTaskArgProcessor {
    public ArgProcessor(PrecompileOnePermOptions options) {
      super(options);
      registerHandler(new ArgHandlerPerm(options));
    }

    @Override
    protected String getName() {
      return PrecompileOnePerm.class.getName();
    }
  }

  @SuppressWarnings("serial")
  static class PrecompileOnePermOptionsImpl extends
      PrecompileTaskOptionsImpl implements PrecompileOnePermOptions {

    int permToCompile = -1;

    public PrecompileOnePermOptionsImpl() {
    }

    public PrecompileOnePermOptionsImpl(PrecompileOnePermOptions other) {
      copyFrom(other);
    }

    public void copyFrom(PrecompileOnePermOptions other) {
      super.copyFrom(other);
      setPermToCompile(other.getPermToCompile());
    }

    @Override
    public int getPermToCompile() {
      return permToCompile;
    }

    @Override
    public void setPermToCompile(int permToCompile) {
      this.permToCompile = permToCompile;
    }
  }

  /**
   * Performs a command-line precompile.
   */
  public static void main(String[] args) {
    Memory.initialize();
    SpeedTracerLogger.init();
    Event precompileEvent = SpeedTracerLogger.start(CompilerEventType.PRECOMPILE);
    if (System.getProperty("gwt.jjs.dumpAst") != null) {
      System.out.println("Will dump AST to: "
          + System.getProperty("gwt.jjs.dumpAst"));
    }

    /*
     * NOTE: main always exits with a call to System.exit to terminate any
     * non-daemon threads that were started in Generators. Typically, this is to
     * shutdown AWT related threads, since the contract for their termination is
     * still implementation-dependent.
     */
    final PrecompileOnePermOptions options = new PrecompileOnePermOptionsImpl();
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
          boolean success = new PrecompileOnePerm(options).run(logger);
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
    precompileEvent.end();
    System.exit(success ? 0 : 1);
  }

  /**
   * Returns a filename for the serialized AST when precompile is performed
   * separately on a per-permutation basis.
   */
  static String getPrecompileFilename(int permutationBase) {
    return Precompile.PRECOMPILE_FILENAME_PREFIX + "-" + permutationBase
        + Precompile.PRECOMPILE_FILENAME_SUFFIX;
  }

  private static boolean validateOptions(TreeLogger logger, PrecompileOnePermOptions options) {
    // Fatal Errors
    if (options.getModuleNames().size() != 1) {
      logger.log(TreeLogger.ERROR, "Expected a single module name.");
      return false;
    }
    if (options.getPermToCompile() < 0) {
      logger.log(TreeLogger.ERROR,
          "Expected argument -perm to specify the permutation to compile.");
      return false;
    }

    // Warnings
    if (!options.isEnabledGeneratingOnShards()) {
      logger.log(TreeLogger.WARN,
      "-XdisableGeneratingOnShards has no effect in PrecompileOnePerm");
    }
    if (options.getMaxPermsPerPrecompile() != -1) {
      logger.log(TreeLogger.WARN,
      "-XmaxPermsPerPrecompile has no effect in PrecompileOnePerm");
    }

    return true;
  }

  private final PrecompileOnePermOptionsImpl options;

  public PrecompileOnePerm(PrecompileOnePermOptions options) {
    this.options = new PrecompileOnePermOptionsImpl(options);
  }

  public boolean run(TreeLogger logger) throws UnableToCompleteException {

    if (!validateOptions(logger, options)) {
      return false;
    }

    // Avoid early optimizations since permutation compiles will run separately.
    options.setOptimizePrecompile(false);

    List<String> moduleNames = options.getModuleNames();

    int permToRun = options.getPermToCompile();

    String moduleName = moduleNames.get(0);
    File compilerWorkDir = options.getCompilerWorkDir(moduleName);

    ModuleDef module = ModuleDefLoader.loadFromClassPath(logger, moduleName);
    StandardLinkerContext linkerContext = new StandardLinkerContext(
        TreeLogger.NULL, module, options);

    if (!linkerContext.allLinkersAreShardable()) {
      logger.log(TreeLogger.ERROR,
          "This compilation mode requires all linkers to be shardable.");
      return false;
    }

    PrecompileTaskOptions optionsFileData = AnalyzeModule.readAnalyzeModuleOptionsFile(
        logger, compilerWorkDir);
    if (optionsFileData == null) {
      logger.log(TreeLogger.ERROR, "Couldn't find "
          + AnalyzeModule.OPTIONS_FILENAME + " in " + compilerWorkDir);
      return false;
    }
    logger.log(TreeLogger.INFO, "Precompiling only specified permutations");

    if (options.isValidateOnly()) {
      TreeLogger branch = logger.branch(TreeLogger.INFO,
          "Validating compilation " + module.getName());
      if (!Precompile.validate(branch, options, module, options.getGenDir())) {
        branch.log(TreeLogger.ERROR, "Validation failed");
        return false;
      }
      branch.log(TreeLogger.INFO, "Validation succeeded");
    } else {
      TreeLogger branch = logger.branch(TreeLogger.INFO, "Precompiling module "
          + module.getName());
      if (!precompilePermutation(logger, compilerWorkDir, module, branch,
          permToRun)) {
        branch.log(TreeLogger.ERROR, "Precompile failed");
        return false;
      }
    }
    return true;
  }

  private boolean precompilePermutation(TreeLogger logger,
      File compilerWorkDir, ModuleDef module, TreeLogger branch, int permId)
      throws UnableToCompleteException {

    // Only precompile specified permutations
    List<PropertyPermutations> collapsedPermutations =
      Precompile.getCollapsedPermutations(module);

    PropertyPermutations onePerm = collapsedPermutations.get(permId);
    Precompilation precompilation = Precompile.precompile(branch, options,
        module, permId, onePerm, options.getGenDir());
    if (precompilation == null) {
      branch.log(TreeLogger.ERROR, "Precompilation failed");
      return false;
    }
    File precompilationFile = new File(compilerWorkDir,
        getPrecompileFilename(permId));
    Util.writeObjectAsFile(logger, precompilationFile, precompilation);

    if (branch.isLoggable(TreeLogger.INFO)) {
      branch.log(TreeLogger.INFO, "Precompilation succeeded for permutation "
          + permId);
    }

    return true;
  }
}
