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
import com.google.gwt.dev.CompileTaskRunner.CompileTask;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.ModuleDefLoader;
import com.google.gwt.dev.cfg.PropertyPermutations;
import com.google.gwt.dev.jjs.PermutationResult;
import com.google.gwt.dev.jjs.UnifiedAst;
import com.google.gwt.dev.util.FileBackedObject;
import com.google.gwt.dev.util.PerfCounter;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.arg.ArgHandlerLocalWorkers;
import com.google.gwt.dev.util.arg.OptionLocalWorkers;
import com.google.gwt.util.tools.ArgHandlerString;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Performs the second phase of compilation, converting the Precompile's AST
 * into JavaScript outputs.
 */
public class CompilePerms {

  /**
   * Options for CompilePerms.
   */
  public interface CompilePermsOptions extends PrecompileTaskOptions,
      OptionLocalWorkers, OptionPerms {
  }

  /**
   * Handles options for which permutations to compile.
   */
  public interface OptionPerms {
    /**
     * Gets the ordered set of permutations to compile. Returns a zero-length
     * array if all permutations should be compiled.
     */
    int[] getPermsToCompile();

    /**
     * Adds another permutation to compile.
     */
    void setPermsToCompile(int[] permsToCompile);
  }

  /**
   * Argument handler for specifying the which perms to run.
   */
  protected static final class ArgHandlerPerms extends ArgHandlerString {
    private final OptionPerms option;

    public ArgHandlerPerms(OptionPerms option) {
      this.option = option;
    }

    @Override
    public String getPurpose() {
      return "Comma-delimited list of 0-based permutations to compile";
    }

    @Override
    public String getTag() {
      return "-perms";
    }

    @Override
    public String[] getTagArgs() {
      return new String[]{"permlist"};
    }

    @Override
    public boolean setString(String str) {
      String[] split = str.split(",");
      if (split.length < 1) {
        System.err.println(getTag()
            + " requires a comma-delimited list of integers");
        return false;
      }

      SortedSet<Integer> permSet = new TreeSet<Integer>();
      for (String item : split) {
        try {
          int value = Integer.parseInt(item);
          if (value < 0) {
            System.err.println(getTag() + " error: negative value '" + value
                + "' is not allowed");
            return false;
          }
          permSet.add(value);
        } catch (NumberFormatException e) {
          System.err.println(getTag()
              + " requires a comma-delimited list of integers; '" + item
              + "' is not an integer");
          return false;
        }
      }
      int[] permsToCompile = new int[permSet.size()];
      int i = 0;
      for (int perm : permSet) {
        permsToCompile[i++] = perm;
      }
      option.setPermsToCompile(permsToCompile);
      return true;
    }
  }

  static class ArgProcessor extends PrecompileTaskArgProcessor {
    public ArgProcessor(CompilePermsOptions options) {
      super(options);
      registerHandler(new ArgHandlerPerms(options));
      registerHandler(new ArgHandlerLocalWorkers(options));
    }

    @Override
    protected String getName() {
      return CompilePerms.class.getName();
    }
  }

  /**
   * Concrete class to implement compiler perm options.
   */
  static class CompilePermsOptionsImpl extends PrecompileTaskOptionsImpl implements
      CompilePermsOptions {

    private int localWorkers;
    private int[] permsToCompile;

    public CompilePermsOptionsImpl() {
    }

    public CompilePermsOptionsImpl(CompilePermsOptions other) {
      copyFrom(other);
    }

    public void copyFrom(CompilePermsOptions other) {
      super.copyFrom(other);
      setPermsToCompile(other.getPermsToCompile());
      setLocalWorkers(other.getLocalWorkers());
    }

    @Override
    public int getLocalWorkers() {
      return localWorkers;
    }

    @Override
    public int[] getPermsToCompile() {
      return (permsToCompile == null) ? null : permsToCompile.clone();
    }

    @Override
    public void setLocalWorkers(int localWorkers) {
      this.localWorkers = localWorkers;
    }

    @Override
    public void setPermsToCompile(int[] permsToCompile) {
      this.permsToCompile = (permsToCompile == null) ? null
          : permsToCompile.clone();
    }
  }

  /**
   * Compile a single permutation.
   * 
   * @throws UnableToCompleteException if the permutation compile fails
   */
  public static PermutationResult compile(TreeLogger logger,
      Permutation permutation, UnifiedAst unifiedAst)
      throws UnableToCompleteException {
    return unifiedAst.compilePermutation(logger, permutation);
  }

  /**
   * Compile multiple permutations.
   */
  public static void compile(TreeLogger logger, Precompilation precompilation,
      Permutation[] perms, int localWorkers,
      List<FileBackedObject<PermutationResult>> resultFiles)
      throws UnableToCompleteException {
    final TreeLogger branch = logger.branch(TreeLogger.INFO, "Compiling "
        + perms.length + " permutation" + (perms.length > 1 ? "s" : ""));
    PermutationWorkerFactory.compilePermutations(branch, precompilation, perms,
        localWorkers, resultFiles);
    logger.log(TreeLogger.INFO, "Compile of permutations succeeded");
  }

  public static void main(String[] args) {
    int exitCode = -1;
    /*
     * NOTE: main always exits with a call to System.exit to terminate any
     * non-daemon threads that were started in Generators. Typically, this is to
     * shutdown AWT related threads, since the contract for their termination is
     * still implementation-dependent.
     */
    final CompilePermsOptions options = new CompilePermsOptionsImpl();
    if (new ArgProcessor(options).processArgs(args)) {
      CompileTask task = new CompileTask() {
        @Override
        public boolean run(TreeLogger logger) throws UnableToCompleteException {
          return new CompilePerms(options).run(logger);
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

  public static List<FileBackedObject<PermutationResult>> makeResultFiles(
      File compilerWorkDir, Permutation[] perms) {
    List<FileBackedObject<PermutationResult>> toReturn = new ArrayList<FileBackedObject<PermutationResult>>(
        perms.length);
    for (int i = 0; i < perms.length; ++i) {
      File f = makePermFilename(compilerWorkDir, perms[i].getId());
      toReturn.add(new FileBackedObject<PermutationResult>(
          PermutationResult.class, f));
    }
    return toReturn;
  }

  /**
   * Return the filename corresponding to the given permutation number,
   * one-based.
   */
  static File makePermFilename(File compilerWorkDir, int permNumber) {
    return new File(compilerWorkDir, "permutation-" + permNumber + ".js");
  }

  static PrecompilationResult readPrecompilationFile(TreeLogger logger,
      File precompilationFile) {
    PrecompilationResult precompileResults = null;
    try {
      precompileResults = Util.readFileAsObject(precompilationFile,
          PrecompilationResult.class);
    } catch (IOException e) {
      logger.log(TreeLogger.ERROR, "Failed to read "
          + precompilationFile + "\nHas Precompile been run?");
    } catch (ClassNotFoundException e) {
      logger.log(TreeLogger.ERROR, "Failed to read "
          + precompilationFile, e);
    }
    return precompileResults;
  }

  /**
   * Choose the subset of requested permutations that correspond to the
   * indicated precompilation.
   */
  static Permutation[] selectPermutationsForPrecompilation(
      int[] permsToRun, Precompilation precompilation) {
    if (permsToRun == null) {
      // Special case: compile everything.
      return precompilation.getPermutations();
    }
    ArrayList<Permutation> subPermsList = new ArrayList<Permutation>();
    for (int id : permsToRun) {
      for (Permutation perm : precompilation.getPermutations()) {
        if (perm.getId() == id) {
          subPermsList.add(perm);
        }
      }
    }
    return subPermsList.toArray(new Permutation[subPermsList.size()]);
  }

  private final CompilePermsOptionsImpl options;

  public CompilePerms(CompilePermsOptions options) {
    this.options = new CompilePermsOptionsImpl(options);
  }

  public boolean run(TreeLogger logger) throws UnableToCompleteException {
    for (String moduleName : options.getModuleNames()) {
      /*
       * NOTE: as a special case, null means "compile everything".
       */
      int[] permsToRun = options.getPermsToCompile();

      File compilerWorkDir = options.getCompilerWorkDir(moduleName);
      File precompilationFile = new File(compilerWorkDir,
          Precompile.PRECOMPILE_FILENAME);

      PrecompilationResult precompileResults = readPrecompilationFile(logger, 
          precompilationFile);

      if (precompileResults instanceof PrecompileTaskOptions) {
        PrecompileTaskOptions precompilationOptions = (PrecompileTaskOptions) precompileResults;
        if (!precompileAndCompile(logger, moduleName, compilerWorkDir,
            precompilationOptions)) {
          return false;
        }
      } else {
        Precompilation precompilation = (Precompilation) precompileResults;
        // Choose which permutations go with this permutation
        Permutation[] subPerms = selectPermutationsForPrecompilation(
            permsToRun, precompilation);

        List<FileBackedObject<PermutationResult>> resultFiles = makeResultFiles(
            compilerWorkDir, subPerms);
        compile(logger, precompilation, subPerms, options.getLocalWorkers(),
            resultFiles);
      }
    }

    return true;
  }

  /**
   * Run both a precompile and a compile with the given precompilation options.
   */
  private boolean precompileAndCompile(TreeLogger logger, String moduleName,
      File compilerWorkDir, PrecompileTaskOptions precompilationOptions)
      throws UnableToCompleteException {
    precompilationOptions.setOptimizePrecompile(false);
    precompilationOptions.setGenDir(null);

    ModuleDef module = ModuleDefLoader.loadFromClassPath(logger, moduleName);
    PropertyPermutations allPermutations = new PropertyPermutations(
        module.getProperties(), module.getActiveLinkerNames());
    List<PropertyPermutations> collapsedPermutations = allPermutations.collapseProperties();
    int[] perms = options.getPermsToCompile();
    if (perms == null) {
      perms = new int[collapsedPermutations.size()];
      for (int i = 0; i < perms.length; ++i) {
        perms[i] = i;
      }
    }

    logger = logger.branch(TreeLogger.INFO, "Compiling " + perms.length
        + " permutation" + (perms.length > 1 ? "s" : ""));
    for (int permId : perms) {
      /*
       * TODO(spoon,scottb): move Precompile out of the loop to run only once
       * per shard. Then figure out a way to avoid copying the generated
       * artifacts into every perm result on a shard.
       */
      PropertyPermutations onePerm = collapsedPermutations.get(permId);

      Precompilation precompilation = Precompile.precompile(logger,
          precompilationOptions, module, permId, onePerm,
          precompilationOptions.getGenDir());
      if (precompilation == null) {
        return false;
      }

      // Choose which permutations go with this precompilation
      Permutation[] subPerms = selectPermutationsForPrecompilation(
          new int[]{permId}, precompilation);
      assert subPerms.length == 1;

      PermutationResult permResult = compile(logger, subPerms[0],
          precompilation.getUnifiedAst());
      Link.linkOnePermutationToJar(logger, module,
          precompilation.getGeneratedArtifacts(), permResult, makePermFilename(
              compilerWorkDir, permId), precompilationOptions);
    }

    logger.log(TreeLogger.INFO, "Compile of permutations succeeded");
    return true;
  }
}
