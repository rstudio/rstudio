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
import com.google.gwt.dev.CompileTaskRunner.CompileTask;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.ModuleDefLoader;
import com.google.gwt.dev.jjs.PermutationResult;
import com.google.gwt.dev.util.PerfCounter;
import com.google.gwt.dev.util.arg.ArgHandlerPerm;
import com.google.gwt.dev.util.arg.OptionPerm;

import java.io.File;
import java.util.List;

/**
 * Performs the second phase of compilation, converting one of the Precompile's
 * serialized AST files into JavaScript outputs.
 */
public class CompileOnePerm {

  /**
   * Options for CompilePerm.
   */
  public interface CompileOnePermOptions extends PrecompileTaskOptions, OptionPerm {
  }

  static class ArgProcessor extends PrecompileTaskArgProcessor {
    public ArgProcessor(CompileOnePermOptions options) {
      super(options);
      registerHandler(new ArgHandlerPerm(options));
    }

    @Override
    protected String getName() {
      return CompileOnePerm.class.getName();
    }
  }

  /**
   * Concrete class to implement compiler perm options.
   */
  static class CompileOnePermOptionsImpl extends PrecompileTaskOptionsImpl
      implements CompileOnePermOptions {

    private int permToCompile = -1;

    public CompileOnePermOptionsImpl() {
    }

    public CompileOnePermOptionsImpl(CompileOnePermOptions other) {
      copyFrom(other);
    }

    public void copyFrom(CompileOnePermOptions other) {
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

  public static void main(String[] args) {
    int exitCode = -1;
    /*
     * NOTE: main always exits with a call to System.exit to terminate any
     * non-daemon threads that were started in Generators. Typically, this is to
     * shutdown AWT related threads, since the contract for their termination is
     * still implementation-dependent.
     */
    final CompileOnePermOptions options = new CompileOnePermOptionsImpl();
    if (new ArgProcessor(options).processArgs(args)) {
      CompileTask task = new CompileTask() {
        @Override
        public boolean run(TreeLogger logger) throws UnableToCompleteException {
          return new CompileOnePerm(options).run(logger);
        }
      };
      if (CompileTaskRunner.runWithAppropriateLogger(options, task)) {
        // Exit w/ success code.
        exitCode = 0;
      }
    }
    PerfCounter.print();
    System.exit(exitCode);
  }

  /**
   * Return the filename corresponding to the given permutation number,
   * one-based.
   */
  static File makePermFilename(File compilerWorkDir, int permNumber) {
    return new File(compilerWorkDir, "permutation-" + permNumber + ".js");
  }

  /**
   * Run Compile where precompilation occurred previously on sharded steps of
   * Precompile. There will be a different serialized AST file named
   * permutation-*.ser, one per permutation.
   *
   * @param logger tree logger
   * @param moduleName Name of the GWT module with the entry point
   * @param permId permutations to compile
   * @param compilerWorkDir directory where work files are stored
   *
   * @return <code>true</code> if compilation succeeds
   */
  private static boolean compileSpecificPermutation(TreeLogger logger,
      String moduleName, PrecompileTaskOptions precompilationOptions, int permId,
      File compilerWorkDir) throws UnableToCompleteException {

    ModuleDef module = ModuleDefLoader.loadFromClassPath(logger, moduleName);

    logger = logger.branch(TreeLogger.INFO, "Compiling permutation " + permId);

    File precompilationFile = new File(compilerWorkDir,
        PrecompileOnePerm.getPrecompileFilename(permId));
    Precompilation precompilation = (Precompilation) CompilePerms.readPrecompilationFile(
        logger, precompilationFile);

    // Choose the permutation that goes with this precompilation
    Permutation[] subPerms = CompilePerms.selectPermutationsForPrecompilation(
        new int[]{permId}, precompilation);
    assert subPerms.length == 1;

    PermutationResult permResult = precompilation.getUnifiedAst().compilePermutation(
        logger, subPerms[0]);
    Link.linkOnePermutationToJar(logger, module,
        precompilation.getGeneratedArtifacts(), permResult, makePermFilename(
            compilerWorkDir, permId), precompilationOptions);
    return true;
  }

  private final CompileOnePermOptionsImpl options;

  public CompileOnePerm(CompileOnePermOptions options) {
    this.options = new CompileOnePermOptionsImpl(options);
  }

  public boolean run(TreeLogger logger) throws UnableToCompleteException {
    List<String> moduleNames = options.getModuleNames();
    if (moduleNames.size() != 1) {
      logger.log(TreeLogger.ERROR, "Expected a single module name.");
      return false;
    }
    int permToRun = options.getPermToCompile();
    if (permToRun < 0) {
      logger.log(TreeLogger.ERROR,
          "Expected argument -perm to specify the permutation to compile.");
      return false;
    }

    String moduleName = moduleNames.get(0);
    File compilerWorkDir = options.getCompilerWorkDir(moduleName);

    // Look for the sentinel file that indicates that this compilation already
    // has a precompilation result from a previous Precompile step.
    PrecompileTaskOptions precompilationOptions = AnalyzeModule.readAnalyzeModuleOptionsFile(
        logger, compilerWorkDir);
    if (precompilationOptions == null) {
      logger.log(TreeLogger.ERROR, "Could not read file "
          + AnalyzeModule.OPTIONS_FILENAME + " output from AnalyzeModule step.");
      return false;
    }

    return compileSpecificPermutation(logger, moduleName,
        precompilationOptions, permToRun, compilerWorkDir);
  }
}
