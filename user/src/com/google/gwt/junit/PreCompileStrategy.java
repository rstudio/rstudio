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

import java.util.Map;
import java.util.HashMap;

/**
 * Strategy that compiles all modules before returning results. Optimizes test
 * system usage.
 */
class PreCompileStrategy extends CompileStrategy {
  /**
   * A mapping of synthetic module names to their precompiled synthetic module
   * defs.
   */
  Map<String, ModuleDef> preCompiledModuleDefs;

  public PreCompileStrategy(JUnitShell junitShell) {
    super(junitShell);
  }

  @Override
  public ModuleDef maybeCompileModule(String moduleName,
      String syntheticModuleName, Strategy strategy,
      BatchingStrategy batchingStrategy, TreeLogger treeLogger)
      throws UnableToCompleteException {
    maybePrecompileModules(batchingStrategy, treeLogger);

    // Since all test blocks from a module are added to the queue at the
    // same time, we can safely take the module out of the hash map at
    // this point.
    return preCompiledModuleDefs.get(syntheticModuleName);
  }

  /**
   * Precompile all modules if needed.
   */
  private void maybePrecompileModules(BatchingStrategy batchingStrategy,
      TreeLogger treeLogger) throws UnableToCompleteException {
    if (preCompiledModuleDefs == null) {
      preCompiledModuleDefs = new HashMap<String, ModuleDef>();
      for (String moduleName : GWTTestCase.getAllTestModuleNames()) {
        TestModuleInfo moduleInfo = GWTTestCase.getTestsForModule(moduleName);
        String syntheticModuleName = moduleInfo.getSyntheticModuleName();
        if (syntheticModuleName != null) {
          ModuleDef moduleDef = maybeCompileModuleImpl(
              moduleInfo.getModuleName(), syntheticModuleName,
              moduleInfo.getStrategy(), batchingStrategy, treeLogger);
          preCompiledModuleDefs.put(syntheticModuleName, moduleDef);
        }
      }
    }
  }
}
