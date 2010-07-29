/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.junit;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.junit.JUnitShell.Strategy;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.junit.client.GWTTestCase.TestModuleInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Strategy that compiles modules as tests run. Optimizes total test time.
 */
class ParallelCompileStrategy extends PreCompileStrategy {

  /**
   * The {@link BatchingStrategy} used to compile, which is set on the first
   * compilation and is the same across all compilations.
   */
  private BatchingStrategy batchingStrategy;

  /**
   * The list of all synthetic module names to be compiled.
   */
  private List<String> modulesToCompile = new ArrayList<String>();

  /**
   * The {@link TreeLogger} used to compile, which is set on the first
   * compilation and is the same across all compilations.
   */
  private TreeLogger treeLogger;

  public ParallelCompileStrategy(JUnitShell junitShell) {
    super(junitShell);
  }

  @Override
  public void maybeCompileAhead() throws UnableToCompleteException {
    if (modulesToCompile.size() > 0) {
      String nextModule = modulesToCompile.remove(0);
      TestModuleInfo moduleInfo = GWTTestCase.getTestsForModule(nextModule);
      String syntheticModuleName = moduleInfo.getSyntheticModuleName();
      maybeCompileModuleImpl(moduleInfo.getModuleName(), syntheticModuleName,
          moduleInfo.getStrategy(), batchingStrategy, treeLogger);
    }
  }

  @Override
  public ModuleDef maybeCompileModule(String moduleName,
      String syntheticModuleName, Strategy strategy,
      BatchingStrategy batchingStrategy, TreeLogger treeLogger)
      throws UnableToCompleteException {

    // Initialize the map of modules.
    if (preCompiledModuleDefs == null) {
      this.batchingStrategy = batchingStrategy;
      this.treeLogger = treeLogger;
      preCompiledModuleDefs = new HashMap<String, ModuleDef>();
      String[] allModuleNames = GWTTestCase.getAllTestModuleNames();
      for (String curModuleName : allModuleNames) {
        modulesToCompile.add(curModuleName);
      }
    }

    // Compile the requested module if needed.
    ModuleDef moduleDef = preCompiledModuleDefs.get(syntheticModuleName);
    if (moduleDef == null) {
      moduleDef = maybeCompileModuleImpl(moduleName, syntheticModuleName,
          strategy, batchingStrategy, treeLogger);
    }
    return moduleDef;
  }

  @Override
  protected ModuleDef maybeCompileModuleImpl(String moduleName,
      String syntheticModuleName, Strategy strategy,
      BatchingStrategy batchingStrategy, TreeLogger treeLogger)
      throws UnableToCompleteException {
    modulesToCompile.remove(syntheticModuleName);
    ModuleDef moduleDef = super.maybeCompileModuleImpl(moduleName,
        syntheticModuleName, strategy, batchingStrategy, treeLogger);
    preCompiledModuleDefs.put(syntheticModuleName, moduleDef);
    return moduleDef;
  }
}
