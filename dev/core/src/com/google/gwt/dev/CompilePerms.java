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
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.arg.ArgHandlerLocalWorkers;
import com.google.gwt.util.tools.ArgHandlerString;

import java.io.File;
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
    private int[] permsToCompile = new int[0];

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
      return permsToCompile.clone();
    }

    public void setLocalWorkers(int localWorkers) {
      this.localWorkers = localWorkers;
    }

    public void setPermsToCompile(int[] permsToCompile) {
      this.permsToCompile = permsToCompile.clone();
    }
  }

  /**
   * Compile a single permutation.
   */
  public static PermutationResult compile(TreeLogger logger,
      Permutation permutation, UnifiedAst unifiedAst) {
    try {
      return JavaToJavaScriptCompiler.compilePermutation(logger, unifiedAst,
          permutation.getRebindAnswers());
    } catch (UnableToCompleteException e) {
      // We intentionally don't pass in the exception here since the real
      // cause has been logged.
      return null;
    }
  }

  /**
   * Compile multiple permutations.
   */
  public static void compile(TreeLogger logger,
      Precompilation precompilation, Permutation[] perms, int localWorkers,
      List<FileBackedObject<PermutationResult>> resultFiles)
      throws UnableToCompleteException {
    final TreeLogger branch = logger.branch(TreeLogger.INFO, "Compiling "
        + perms.length + " permutations");
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

  private final CompilePermsOptionsImpl options;

  public CompilePerms(CompilePermsOptions options) {
    this.options = new CompilePermsOptionsImpl(options);
  }

  public boolean run(TreeLogger logger) throws UnableToCompleteException {
    for (String moduleName : options.getModuleNames()) {
      File compilerWorkDir = options.getCompilerWorkDir(moduleName);
      File precompilationFile = new File(compilerWorkDir,
          Precompile.PRECOMPILATION_FILENAME);
      if (!precompilationFile.exists()) {
        logger.log(TreeLogger.ERROR, "File not found '"
            + precompilationFile.getAbsolutePath()
            + "'; please run Precompile first");
        return false;
      }
      Precompilation precompilation;
      try {
        /*
         * TODO: don't bother deserializing the generated artifacts.
         */
        precompilation = Util.readFileAsObject(precompilationFile,
            Precompilation.class);
      } catch (ClassNotFoundException e) {
        logger.log(TreeLogger.ERROR, "Unable to deserialize '"
            + precompilationFile.getAbsolutePath() + "'", e);
        return false;
      }

      Permutation[] perms = precompilation.getPermutations();
      Permutation[] subPerms;
      int[] permsToRun = options.getPermsToCompile();
      if (permsToRun.length == 0) {
        subPerms = perms;
      } else {
        int i = 0;
        subPerms = new Permutation[permsToRun.length];
        // Range check the supplied perms.
        for (int permToRun : permsToRun) {
          if (permToRun >= perms.length) {
            logger.log(TreeLogger.ERROR, "The specified perm number '"
                + permToRun + "' is too big; the maximum value is "
                + (perms.length - 1) + "'");
            return false;
          }
          subPerms[i++] = perms[permToRun];
        }
      }

      List<FileBackedObject<PermutationResult>> resultFiles = makeResultFiles(
          compilerWorkDir, subPerms);
      compile(logger, precompilation, subPerms, options.getLocalWorkers(),
          resultFiles);
    }
    return true;
  }
}
