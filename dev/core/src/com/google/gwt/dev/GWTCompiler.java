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
import com.google.gwt.dev.CompilePerms.CompilePermsOptionsImpl;
import com.google.gwt.dev.CompileTaskRunner.CompileTask;
import com.google.gwt.dev.Precompile.PrecompileOptionsImpl;
import com.google.gwt.dev.util.PerfLogger;
import com.google.gwt.dev.util.arg.ArgHandlerExtraDir;

import java.io.File;

/**
 * The main executable entry point for the GWT Java to JavaScript compiler.
 */
public class GWTCompiler {

  static final class ArgProcessor extends Precompile.ArgProcessor {
    public ArgProcessor(CompilerOptions options) {
      super(options);
      registerHandler(new ArgHandlerExtraDir(options));
    }

    @Override
    protected String getName() {
      return GWTCompiler.class.getName();
    }
  }

  static class GWTCompilerOptionsImpl extends PrecompileOptionsImpl implements
      CompilerOptions {

    private File extraDir;

    public GWTCompilerOptionsImpl() {
    }

    public GWTCompilerOptionsImpl(CompilerOptions other) {
      copyFrom(other);
    }

    public void copyFrom(CompilerOptions other) {
      super.copyFrom(other);
      setExtraDir(other.getExtraDir());
    }

    public File getExtraDir() {
      return extraDir;
    }

    public void setExtraDir(File extraDir) {
      this.extraDir = extraDir;
    }
  }

  public static void main(String[] args) {
    /*
     * NOTE: main always exits with a call to System.exit to terminate any
     * non-daemon threads that were started in Generators. Typically, this is to
     * shutdown AWT related threads, since the contract for their termination is
     * still implementation-dependent.
     */
    final CompilerOptions options = new GWTCompilerOptionsImpl();
    if (new ArgProcessor(options).processArgs(args)) {
      CompileTask task = new CompileTask() {
        public boolean run(TreeLogger logger) throws UnableToCompleteException {
          return new GWTCompiler(options).run(logger);
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

  private final GWTCompilerOptionsImpl options;

  public GWTCompiler(CompilerOptions options) {
    this.options = new GWTCompilerOptionsImpl(options);
  }

  public boolean run(TreeLogger logger) throws UnableToCompleteException {
    if (options.isValidateOnly()) {
      return new Precompile(options).run(logger);
    } else {
      PerfLogger.start("compile");
      logger = logger.branch(TreeLogger.INFO, "Compiling module "
          + options.getModuleName());
      if (new Precompile(options).run(logger)) {
        /*
         * TODO: use the in-memory result of Precompile to run CompilePerms
         * instead of serializing through the file system.
         */
        CompilePermsOptionsImpl permsOptions = new CompilePermsOptionsImpl();
        permsOptions.copyFrom(options);
        if (new CompilePerms(permsOptions).run(logger)) {
          if (new Link(options).run(logger)) {
            logger.log(TreeLogger.INFO, "Compilation succeeded");
            PerfLogger.end();
            return true;
          }
        }
      }
      logger.log(TreeLogger.ERROR, "Compilation failed");
      PerfLogger.end();
      return false;
    }
  }
}
