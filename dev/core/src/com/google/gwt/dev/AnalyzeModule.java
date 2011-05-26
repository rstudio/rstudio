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
import com.google.gwt.dev.cfg.PropertyPermutations;
import com.google.gwt.dev.util.Memory;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;

import java.io.File;
import java.io.IOException;

/**
 * Performs the first phase of compilation, generating the set of permutations
 * to compile and writing it out to a file.
 */
public class AnalyzeModule {

  /**
   * Mirror all the compiler options, in case this makes some difference in
   * module analysis.
   */
  @SuppressWarnings("serial")
  static class AnalyzeModuleOptionsImpl extends PrecompileTaskOptionsImpl implements
      AnalyzeModuleOptions {

    public AnalyzeModuleOptionsImpl() {
    }

    public AnalyzeModuleOptionsImpl(AnalyzeModuleOptions other) {
      copyFrom(other);
    }

    public void copyFrom(AnalyzeModuleOptions other) {
      super.copyFrom(other);
    }
  }

  static class ArgProcessor extends PrecompileTaskArgProcessor {
    public ArgProcessor(AnalyzeModuleOptions options) {
      super(options);
    }

    @Override
    protected String getName() {
      return AnalyzeModule.class.getName();
    }
  }

  private interface AnalyzeModuleOptions extends PrecompileTaskOptions {
    // This interface is here to support future options.
  }

  /**
   * The options passed to the AnalyzeModule step are saved here and passed
   * through to future steps.
   */
  static final String OPTIONS_FILENAME = "compilerOptions.ser";

  /**
   * Count of the maximum number of permutations in the module configuration.
   * Used to communicate the number of permutations defined in a module to an
   * external build tool.
   */
  static final String PERM_COUNT_FILENAME = "permCount.txt";

  /**
   * Performs a command-line analysis of the module with output to files for use
   * in further sharded build steps.
   */
  public static void main(String[] args) {
    Memory.initialize();
    SpeedTracerLogger.init();
    Event analyzeModuleEvent = SpeedTracerLogger.start(CompilerEventType.ANALYZE_MODULE);
    final AnalyzeModuleOptions options = new AnalyzeModuleOptionsImpl();
    if (new ArgProcessor(options).processArgs(args)) {
      CompileTask task = new CompileTask() {
        @Override
        public boolean run(TreeLogger logger) throws UnableToCompleteException {
          return new AnalyzeModule(options).run(logger);
        }
      };
      CompileTaskRunner.runWithAppropriateLogger(options, task);
    }
    analyzeModuleEvent.end();
  }

  /**
   * Loads the AnalyzeModule.OPTIONS_FILENAME data.
   *
   * Silently returns <code>null</code> if the file is not found or another problem is
   * encountered reading the file.
   */
  public static PrecompileTaskOptions readAnalyzeModuleOptionsFile(
      TreeLogger logger, File compilerWorkDir) {
    File optionsFile = new File(compilerWorkDir, AnalyzeModule.OPTIONS_FILENAME);
    PrecompileTaskOptions precompilationOptions = null;
    try {
      precompilationOptions = Util.readFileAsObject(optionsFile,
          PrecompileTaskOptions.class);
    } catch (IOException e) {
      if (logger.isLoggable(TreeLogger.DEBUG)) {
        logger.log(TreeLogger.DEBUG, "Failed to read " + optionsFile
            + "\nHas AnalyzeModule been run?  Falling back.", e);
      }
      return null;
    } catch (ClassNotFoundException e) {
      logger.log(TreeLogger.ERROR, "Failed to read " + optionsFile, e);
      return null;
    }
    return precompilationOptions;
  }

  private final AnalyzeModuleOptionsImpl options;

  public AnalyzeModule(AnalyzeModuleOptions options) {
    this.options = new AnalyzeModuleOptionsImpl(options);
  }

  public boolean run(TreeLogger logger) throws UnableToCompleteException {
    for (String moduleName : options.getModuleNames()) {
      File compilerWorkDir = options.getCompilerWorkDir(moduleName);
      Util.recursiveDelete(compilerWorkDir, true);
      // No need to check mkdirs result because an IOException will occur anyway
      compilerWorkDir.mkdirs();

      ModuleDef module = ModuleDefLoader.loadFromClassPath(logger, moduleName);
      if (logger.isLoggable(TreeLogger.INFO)) {
        logger.log(TreeLogger.INFO, "Analyzing module " + module.getName());
      }

      /*
       * Count the permutations to expose to external build tools performing a
       * sharded compile.
       */
      int numPermutations = new PropertyPermutations(module.getProperties(),
          module.getActiveLinkerNames()).collapseProperties().size();
      Util.writeStringAsFile(logger, new File(compilerWorkDir,
          AnalyzeModule.PERM_COUNT_FILENAME), String.valueOf(numPermutations));
      Util.writeObjectAsFile(logger, new File(compilerWorkDir,
          AnalyzeModule.OPTIONS_FILENAME), options);

      // TODO(zundel): Serializing the ModuleDef structure would save time in
      // subsequent steps.

      // TODO(zundel): Building the initial type oracle in this step would save
      // cputime when the precompile step is sharded.
    }

    return true;
  }
}
