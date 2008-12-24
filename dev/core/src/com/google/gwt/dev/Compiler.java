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
import com.google.gwt.dev.Link.LinkOptionsImpl;
import com.google.gwt.dev.Precompile.PrecompileOptionsImpl;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.ModuleDefLoader;
import com.google.gwt.dev.util.FileBackedObject;
import com.google.gwt.dev.util.PerfLogger;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.arg.ArgHandlerExtraDir;
import com.google.gwt.dev.util.arg.ArgHandlerLocalWorkers;
import com.google.gwt.dev.util.arg.ArgHandlerWarDir;
import com.google.gwt.dev.util.arg.ArgHandlerWorkDirOptional;
import com.google.gwt.util.tools.Utility;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * The main executable entry point for the GWT Java to JavaScript compiler.
 */
public class Compiler {

  static class ArgProcessor extends Precompile.ArgProcessor {
    public ArgProcessor(CompilerOptions options) {
      super(options);

      registerHandler(new ArgHandlerLocalWorkers(options));

      // Override the ArgHandlerWorkDirRequired in the super class.
      registerHandler(new ArgHandlerWorkDirOptional(options));

      registerHandler(new ArgHandlerWarDir(options));
      registerHandler(new ArgHandlerExtraDir(options));
    }

    @Override
    protected String getName() {
      return Compiler.class.getName();
    }
  }

  static class CompilerOptionsImpl extends PrecompileOptionsImpl implements
      CompilerOptions {

    private LinkOptionsImpl linkOptions = new LinkOptionsImpl();
    private int localWorkers;

    public CompilerOptionsImpl() {
    }

    public CompilerOptionsImpl(CompilerOptions other) {
      copyFrom(other);
    }

    public void copyFrom(CompilerOptions other) {
      super.copyFrom(other);
      linkOptions.copyFrom(other);
      localWorkers = other.getLocalWorkers();
    }

    public File getExtraDir() {
      return linkOptions.getExtraDir();
    }

    public int getLocalWorkers() {
      return localWorkers;
    }

    public File getWarDir() {
      return linkOptions.getWarDir();
    }

    public void setExtraDir(File extraDir) {
      linkOptions.setExtraDir(extraDir);
    }

    public void setLocalWorkers(int localWorkers) {
      this.localWorkers = localWorkers;
    }

    public void setWarDir(File outDir) {
      linkOptions.setWarDir(outDir);
    }
  }

  public static void main(String[] args) {
    /*
     * NOTE: main always exits with a call to System.exit to terminate any
     * non-daemon threads that were started in Generators. Typically, this is to
     * shutdown AWT related threads, since the contract for their termination is
     * still implementation-dependent.
     */
    final CompilerOptions options = new CompilerOptionsImpl();
    if (new ArgProcessor(options).processArgs(args)) {
      CompileTask task = new CompileTask() {
        public boolean run(TreeLogger logger) throws UnableToCompleteException {
          return new Compiler(options).run(logger);
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

  private final CompilerOptionsImpl options;

  public Compiler(CompilerOptions options) {
    this.options = new CompilerOptionsImpl(options);
  }

  public boolean run(TreeLogger logger) throws UnableToCompleteException {
    PerfLogger.start("compile");
    boolean tempWorkDir = false;
    try {
      if (options.getWorkDir() == null) {
        options.setWorkDir(Utility.makeTemporaryDirectory(null, "gwtc"));
        tempWorkDir = true;
      }

      for (String moduleName : options.getModuleNames()) {
        ModuleDef module = ModuleDefLoader.loadFromClassPath(logger, moduleName);
        File compilerWorkDir = options.getCompilerWorkDir(moduleName);

        if (options.isValidateOnly()) {
          if (!Precompile.validate(logger, options, module,
              options.getGenDir(), compilerWorkDir)) {
            return false;
          }
        } else {
          long compileStart = System.currentTimeMillis();
          logger = logger.branch(TreeLogger.INFO, "Compiling module "
              + moduleName);

          Precompilation precompilation = Precompile.precompile(logger,
              options, module, options.getGenDir(), compilerWorkDir);

          if (precompilation == null) {
            return false;
          }

          Permutation[] allPerms = precompilation.getPermutations();
          List<FileBackedObject<PermutationResult>> resultFiles
              = CompilePerms.makeResultFiles(
              options.getCompilerWorkDir(moduleName), allPerms);
          CompilePerms.compile(logger, precompilation, allPerms, options.getLocalWorkers(),
              resultFiles);

          Link.link(logger.branch(TreeLogger.INFO, "Linking into "
              + options.getWarDir().getPath()), module, precompilation,
              resultFiles, options.getWarDir(), options.getExtraDir());

          long compileDone = System.currentTimeMillis();
          long delta = compileDone - compileStart;
          logger.log(TreeLogger.INFO, "Compilation succeeded -- "
              + String.format("%.3f", delta / 1000d) + "s");
        }
      }

    } catch (IOException e) {
      logger.log(TreeLogger.ERROR, "Unable to create compiler work directory",
          e);
      return false;
    } finally {
      PerfLogger.end();
      if (tempWorkDir) {
        Util.recursiveDelete(options.getWorkDir(), false);
      }
    }
    return true;
  }
}
