/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.dev.shell;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.ModuleDefLoader;
import com.google.gwt.dev.cfg.Rules;
import com.google.gwt.dev.jdt.ByteCodeCompiler;
import com.google.gwt.dev.jdt.RebindOracle;
import com.google.gwt.dev.jdt.SourceOracle;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides an environment for a {@link com.google.gwt.dev.shell.ModuleSpace}
 * that works appropriately for the development shell.
 */
public class ShellModuleSpaceHost implements ModuleSpaceHost {

  private static Map byteCodeCompilersByModule = new HashMap();

  protected final File genDir;

  protected final TypeOracle typeOracle;

  private CompilingClassLoader classLoader;

  private final TreeLogger logger;

  private final ModuleDef module;

  private final File outDir;

  private RebindOracle rebindOracle;

  private ModuleSpace space;

  /**
   * @param module the module associated with the hosted module space
   */
  public ShellModuleSpaceHost(TreeLogger logger, TypeOracle typeOracle,
      ModuleDef module, File genDir, File outDir) {
    this.logger = logger;
    this.typeOracle = typeOracle;
    this.module = module;
    this.genDir = genDir;

    // Combine the user's output dir with the module name to get the
    // module-specific output dir.
    this.outDir = new File(outDir, module.getName());
  }

  public CompilingClassLoader getClassLoader() {
    checkForModuleSpace();
    return classLoader;
  }

  public String[] getEntryPointTypeNames() {
    checkForModuleSpace();
    return module.getEntryPointTypeNames();
  }

  public TreeLogger getLogger() {
    return logger;
  }

  public void onModuleReady(ModuleSpace readySpace)
      throws UnableToCompleteException {
    this.space = readySpace;

    // Create a host for the hosted mode compiler.
    // We add compilation units to it as deferred binding generators write them.
    //
    SourceOracle srcOracle = new HostedModeSourceOracle(typeOracle);

    // Create or find the compiler to be used by the compiling class loader.
    //
    ByteCodeCompiler compiler = getOrCreateByteCodeCompiler(srcOracle);

    // Establish an environment for JavaScript property providers to run.
    //
    ModuleSpacePropertyOracle propOracle = new ModuleSpacePropertyOracle(
        module.getProperties(), readySpace);

    // Set up the rebind oracle for the module.
    // It has to wait until now because we need to inject javascript.
    //
    Rules rules = module.getRules();
    rebindOracle = new StandardRebindOracle(typeOracle, propOracle, rules,
        genDir, outDir, module.getCacheManager());

    // Create a completely isolated class loader which owns all classes
    // associated with a particular module. This effectively builds a
    // separate 'domain' for each running module, so that they all behave as
    // though they are running separately. This allows the shell to run
    // multiple modules, both in succession and simultaneously, without getting
    // confused.
    //
    // Note that the compiling class loader has no parent. This keeps it from
    // accidentally 'escaping' its domain and loading classes from the system
    // class loader (the one that loaded the shell itself).
    //
    classLoader = new CompilingClassLoader(logger, compiler, typeOracle);
  }

  public String rebind(TreeLogger rebindLogger, String sourceTypeName)
      throws UnableToCompleteException {
    checkForModuleSpace();
    return rebindOracle.rebind(rebindLogger, sourceTypeName);
  }

  ByteCodeCompiler getOrCreateByteCodeCompiler(SourceOracle srcOracle) {
    ByteCodeCompiler compiler;
    synchronized (byteCodeCompilersByModule) {
      compiler = (ByteCodeCompiler) byteCodeCompilersByModule.get(module);
      if (compiler == null) {
        compiler = new ByteCodeCompiler(srcOracle, module.getCacheManager());
        if (ModuleDefLoader.getEnableCachingModules()) {
          byteCodeCompilersByModule.put(module, compiler);
        }
      }
    }
    return compiler;
  }

  private void checkForModuleSpace() {
    if (space == null) {
      throw new IllegalStateException("Module initialization error");
    }
  }
}
