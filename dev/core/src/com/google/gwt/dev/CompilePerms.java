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
import com.google.gwt.dev.jjs.JavaToJavaScriptCompiler;
import com.google.gwt.dev.jjs.UnifiedAst;
import com.google.gwt.dev.util.FileBackedObject;
import com.google.gwt.dev.util.arg.ArgHandlerLocalWorkers;
import com.google.gwt.dev.util.arg.OptionLocalWorkers;
import com.google.gwt.util.tools.ArgHandlerString;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
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
  public interface CompilePermsOptions extends CompileTaskOptions,
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
      return new String[] {"permlist"};
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

  static class ArgProcessor extends CompileArgProcessor {
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
  static class CompilePermsOptionsImpl extends CompileTaskOptionsImpl implements
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

    public int getLocalWorkers() {
      return localWorkers;
    }

    public int[] getPermsToCompile() {
      return (permsToCompile == null) ? null : permsToCompile.clone();
    }

    public void setLocalWorkers(int localWorkers) {
      this.localWorkers = localWorkers;
    }

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
    return JavaToJavaScriptCompiler.compilePermutation(logger, unifiedAst,
        permutation.getRebindAnswers(), permutation.getPropertyOracles(),
        permutation.getId());
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
    PermutationWorkerFactory.compilePermutations(logger, precompilation, perms,
        localWorkers, resultFiles);
    branch.log(TreeLogger.INFO, "Permutation compile succeeded");
  }

  public static void main(String[] args) {
    /*
     * NOTE: main always exits with a call to System.exit to terminate any
     * non-daemon threads that were started in Generators. Typically, this is to
     * shutdown AWT related threads, since the contract for their termination is
     * still implementation-dependent.
     */
    final CompilePermsOptions options = new CompilePermsOptionsImpl();
    if (new ArgProcessor(options).processArgs(args)) {
      CompileTask task = new CompileTask() {
        public boolean run(TreeLogger logger) throws UnableToCompleteException {
          return new CompilePerms(options).run(logger);
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

  /**
   * Check whether any of the listed permutations have their precompilation in
   * the supplied precompilation file.
   */
  private static boolean isPrecompileForAnyOf(
      PrecompilationFile precompilationFile, int[] permutations) {
    if (permutations == null) {
      // Special case: compile everything.
      return true;
    }
    for (int perm : permutations) {
      if (precompilationFile.isForPermutation(perm)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Choose the subset of requested permutations that correspond to the
   * indicated precompilation.
   */
  private static Permutation[] selectPermutationsForPrecompilation(
      int[] permsToRun, PrecompilationFile precompilationFile,
      Precompilation precompilation) {
    if (permsToRun == null) {
      // Special case: compile everything.
      return precompilation.getPermutations();
    }
    ArrayList<Permutation> subPermsList = new ArrayList<Permutation>();
    for (int perm : permsToRun) {
      if (precompilationFile.isForPermutation(perm)) {
        subPermsList.add(precompilation.getPermutation(perm));
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
      Collection<PrecompilationFile> precomps;
      try {
        precomps = PrecompilationFile.scanJarFile(new File(compilerWorkDir,
            Precompile.PRECOMPILE_FILENAME));
      } catch (IOException e) {
        logger.log(TreeLogger.ERROR, "Failed to scan "
            + Precompile.PRECOMPILE_FILENAME + "; has Precompile been run?", e);
        return false;
      }

      /*
       * Check that all requested permutations actually have a Precompile
       * available
       */
      if (permsToRun != null) {
        checking_perms : for (int perm : permsToRun) {
          for (PrecompilationFile precomp : precomps) {
            if (precomp.isForPermutation(perm)) {
              continue checking_perms;
            }
          }
          logger.log(TreeLogger.ERROR,
              "No precompilation file found for permutation " + perm);
          return false;
        }
      } else {
        // TODO: validate that a contiguous set of all perms exists.
      }

      /*
       * Perform the compiles one file at a time, to minimize the number of
       * times a Precompilation needs to be deserialized.
       */
      for (PrecompilationFile precompilationFile : precomps) {
        if (!isPrecompileForAnyOf(precompilationFile, permsToRun)) {
          continue;
        }
        Precompilation precompilation;
        try {
          /*
           * TODO: don't bother deserializing the generated artifacts.
           */
          precompilation = precompilationFile.newInstance(logger);
        } catch (UnableToCompleteException e) {
          return false;
        }

        // Choose which permutations go with this permutation
        Permutation[] subPerms = selectPermutationsForPrecompilation(
            permsToRun, precompilationFile, precompilation);

        List<FileBackedObject<PermutationResult>> resultFiles = makeResultFiles(
            compilerWorkDir, subPerms);
        compile(logger, precompilation, subPerms, options.getLocalWorkers(),
            resultFiles);
      }
    }

    return true;
  }
}
