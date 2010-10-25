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
import com.google.gwt.dev.Precompile.PrecompileOptionsImpl;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.ModuleDefLoader;
import com.google.gwt.dev.cfg.PropertyPermutations;
import com.google.gwt.dev.jjs.JJSOptions;
import com.google.gwt.dev.util.Memory;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;

import java.io.File;

/**
 * Performs the first phase of compilation, generating the set of permutations
 * to compile, and a ready-to-compile AST.
 */
public class AnalyzeModule {

  private interface AnalyzeModuleOptions extends JJSOptions, CompileTaskOptions {
  }

  static class ArgProcessor extends CompileArgProcessor {
    public ArgProcessor(AnalyzeModuleOptions options) {
      super(options);
    }

    @Override
    protected String getName() {
      return AnalyzeModule.class.getName();
    }
  }

  /**
   * Mirror all the compiler options, in case this makes some
   * difference in module analysis.
   */
  static class AnalyzeModuleOptionsImpl extends PrecompileOptionsImpl
      implements AnalyzeModuleOptions {
    private final PrecompileOptionsImpl precompileOptions = new PrecompileOptionsImpl();

    public AnalyzeModuleOptionsImpl() {
    }

    public AnalyzeModuleOptionsImpl(AnalyzeModuleOptions other) {
      copyFrom(other);
    }

    public void copyFrom(AnalyzeModuleOptions other) {
      super.copyFrom(other);
      precompileOptions.copyFrom(other);
    }
  }

  /**
   * Performs a command-line analysis of the module with output to files
   * for use in further sharded build steps.
   */
  public static void main(String[] args) {
    Memory.initialize();
    SpeedTracerLogger.init();
    Event analyzeModuleEvent = SpeedTracerLogger.start(CompilerEventType.ANALYZE_MODULE);
    final AnalyzeModuleOptions options = new AnalyzeModuleOptionsImpl();
    if (new ArgProcessor(options).processArgs(args)) {
      CompileTask task = new CompileTask() {
        public boolean run(TreeLogger logger) throws UnableToCompleteException {
          return new AnalyzeModule(options).run(logger);
        }
      };
      CompileTaskRunner.runWithAppropriateLogger(options, task);
    }
    analyzeModuleEvent.end();
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
      
      logger.log(TreeLogger.INFO, "Analyzing module " + module.getName());

      /*
       * Count the permutations to expose to external build tools performing
       * a sharded compile.
       */
      int numPermutations =
          new PropertyPermutations(module.getProperties(), module.getActiveLinkerNames())
              .collapseProperties().size();
      Util.writeStringAsFile(logger, new File(compilerWorkDir, Precompile.PERM_COUNT_FILENAME),
          String.valueOf(numPermutations));
      
      // TODO(zundel): Serializing the ModuleDef structure would save time in subsequent steps.
      
      // TODO(zundel): Building the initial type oracle in this step would save cputime when
      //               the precompile step is sharded.
    }

    return true;
  }
}
