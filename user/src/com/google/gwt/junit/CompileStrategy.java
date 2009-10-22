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
import com.google.gwt.dev.cfg.ConfigurationProperty;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.ModuleDefLoader;
import com.google.gwt.dev.util.collect.HashSet;
import com.google.gwt.junit.JUnitShell.Strategy;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.junit.client.GWTTestCase.TestModuleInfo;
import com.google.gwt.junit.client.impl.GWTRunner;
import com.google.gwt.junit.client.impl.JUnitHost.TestInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An interface that specifies how modules should be compiled.
 */
public abstract class CompileStrategy {

  /**
   * The list of modules that have already been compiled. We use this to avoid
   * adding test batches that have already been added.
   */
  private Set<String> compiledModuleNames = new HashSet<String>();

  private final JUnitShell junitShell;

  /**
   * Construct a CompileStrategy.
   * 
   * @param junitShell
   */
  public CompileStrategy(JUnitShell junitShell) {
    this.junitShell = junitShell;
  }

  /**
   * Maybe add a test block for the currently executed test case.
   * 
   * @param testCase the test case being run
   * @param batchingStrategy the batching strategy
   */
  public void maybeAddTestBlockForCurrentTest(GWTTestCase testCase,
      BatchingStrategy batchingStrategy) {
    if (batchingStrategy.isSingleTestOnly()) {
      TestInfo testInfo = new TestInfo(testCase.getSyntheticModuleName(),
          testCase.getClass().getName(), testCase.getName());
      List<TestInfo[]> testBlocks = new ArrayList<TestInfo[]>(1);
      testBlocks.add(new TestInfo[] {testInfo});
      getMessageQueue().addTestBlocks(testBlocks, false);
    }
  }

  /**
   * Let the compile strategy compile another module. This is called while
   * {@link JUnitShell} is waiting for the current test to complete.
   * 
   * @throws UnableToCompleteException if the compilation fails
   */
  public void maybeCompileAhead() throws UnableToCompleteException {
  }

  /**
   * Compile a single module using a synthetic module that adds JUnit support.
   * 
   * @param moduleName the module name
   * @param syntheticModuleName the synthetic module name
   * @param strategy the strategy
   * @param runStyle the run style
   * @param batchingStrategy the batching strategy
   * @param treeLogger the logger
   * @return the {@link ModuleDef} describing the synthetic module
   * @throws UnableToCompleteException
   */
  public abstract ModuleDef maybeCompileModule(String moduleName,
      String syntheticModuleName, Strategy strategy, RunStyle runStyle,
      BatchingStrategy batchingStrategy, TreeLogger treeLogger)
      throws UnableToCompleteException;

  /**
   * Compile a single module using a synthetic module that adds JUnit support.
   * 
   * @param moduleName the module name
   * @param syntheticModuleName the synthetic module name
   * @param strategy the strategy
   * @param runStyle the run style
   * @param batchingStrategy the batching strategy
   * @param treeLogger the logger
   * @return the {@link ModuleDef} describing the synthetic module
   */
  protected ModuleDef maybeCompileModuleImpl(String moduleName,
      String syntheticModuleName, Strategy strategy, RunStyle runStyle,
      BatchingStrategy batchingStrategy, TreeLogger treeLogger)
      throws UnableToCompleteException {

    // Let the runstyle compile the module.
    ModuleDef moduleDef = maybeCompileModuleImpl2(moduleName,
        syntheticModuleName, strategy, runStyle, treeLogger);

    // Add all test blocks for the module if we haven't seen this module before.
    if (!compiledModuleNames.contains(syntheticModuleName)) {
      compiledModuleNames.add(syntheticModuleName);
      if (!batchingStrategy.isSingleTestOnly()) {
        // Use >= so we can mock getModuleCount and force isFinalModule to true
        boolean isFinalModule = compiledModuleNames.size() >= getModuleCount();
        List<TestInfo[]> testBlocks = batchingStrategy.getTestBlocks(syntheticModuleName);
        getMessageQueue().addTestBlocks(testBlocks, isFinalModule);
      }
    }

    return moduleDef;
  }

  /**
   * Visible for testing and mocking.
   * 
   * @return the {@link JUnitMessageQueue}
   */
  JUnitMessageQueue getMessageQueue() {
    return JUnitShell.getMessageQueue();
  }

  /**
   * Visible for testing and mocking.
   * 
   * @return the number of modules to test
   */
  int getModuleCount() {
    return GWTTestCase.getModuleCount();
  }

  /**
   * Let the {@link RunStyle} compile the module if needed
   * 
   * Visible for testing and mocking.
   * 
   * @param moduleName the module name
   * @param syntheticModuleName the synthetic module name
   * @param strategy the strategy
   * @param runStyle the run style
   * @param treeLogger the logger
   * @return the {@link ModuleDef} describing the synthetic module
   */
  ModuleDef maybeCompileModuleImpl2(String moduleName,
      String syntheticModuleName, Strategy strategy, RunStyle runStyle,
      TreeLogger treeLogger) throws UnableToCompleteException {
    /*
     * Synthesize a synthetic module that derives from the user-specified module
     * but also includes JUnit support.
     */
    ModuleDef moduleDef = ModuleDefLoader.createSyntheticModule(treeLogger,
        syntheticModuleName, new String[] {
            moduleName, strategy.getModuleInherit()}, true);

    // Replace any user entry points with our test runner.
    moduleDef.clearEntryPoints();
    moduleDef.addEntryPointTypeName(GWTRunner.class.getName());

    // Squirrel away the name of the active module for GWTRunnerGenerator
    ConfigurationProperty moduleNameProp = moduleDef.getProperties().createConfiguration(
        "junit.moduleName", false);
    moduleNameProp.setValue(syntheticModuleName);

    strategy.processModule(moduleDef);

    junitShell.maybeCompileForWebMode(syntheticModuleName,
        JUnitShell.getRemoteUserAgents());

    return moduleDef;
  }
}

/**
 * Strategy that compiles modules as tests run. Optimizes total test time.
 */
class ParallelCompileStrategy extends PreCompileStrategy {

  /**
   * The list of all synthetic module names to be compiled.
   */
  private List<String> modulesToCompile = new ArrayList<String>();

  /**
   * The {@link RunStyle} used to compile, which is set on the first compilation
   * and is the same across all compilations.
   */
  private RunStyle runStyle;

  /**
   * The {@link BatchingStrategy} used to compile, which is set on the first
   * compilation and is the same across all compilations.
   */
  private BatchingStrategy batchingStrategy;

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
          moduleInfo.getStrategy(), runStyle, batchingStrategy, treeLogger);
    }
  }

  @Override
  public ModuleDef maybeCompileModule(String moduleName,
      String syntheticModuleName, Strategy strategy, RunStyle runStyle,
      BatchingStrategy batchingStrategy, TreeLogger treeLogger)
      throws UnableToCompleteException {

    // Initialize the map of modules.
    if (preCompiledModuleDefs == null) {
      this.runStyle = runStyle;
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
          strategy, runStyle, batchingStrategy, treeLogger);
    }
    return moduleDef;
  }

  @Override
  protected ModuleDef maybeCompileModuleImpl(String moduleName,
      String syntheticModuleName, Strategy strategy, RunStyle runStyle,
      BatchingStrategy batchingStrategy, TreeLogger treeLogger)
      throws UnableToCompleteException {
    modulesToCompile.remove(syntheticModuleName);
    ModuleDef moduleDef = super.maybeCompileModuleImpl(moduleName,
        syntheticModuleName, strategy, runStyle, batchingStrategy, treeLogger);
    preCompiledModuleDefs.put(syntheticModuleName, moduleDef);
    return moduleDef;
  }
}

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
      String syntheticModuleName, Strategy strategy, RunStyle runStyle,
      BatchingStrategy batchingStrategy, TreeLogger treeLogger)
      throws UnableToCompleteException {
    maybePrecompileModules(runStyle, batchingStrategy, treeLogger);

    // Since all test blocks from a module are added to the queue at the
    // same time, we can safely take the module out of the hash map at
    // this point.
    return preCompiledModuleDefs.get(syntheticModuleName);
  }

  /**
   * Precompile all modules if needed.
   */
  private void maybePrecompileModules(RunStyle runStyle,
      BatchingStrategy batchingStrategy, TreeLogger treeLogger)
      throws UnableToCompleteException {
    if (preCompiledModuleDefs == null) {
      preCompiledModuleDefs = new HashMap<String, ModuleDef>();
      for (String moduleName : GWTTestCase.getAllTestModuleNames()) {
        TestModuleInfo moduleInfo = GWTTestCase.getTestsForModule(moduleName);
        String syntheticModuleName = moduleInfo.getSyntheticModuleName();
        ModuleDef moduleDef = maybeCompileModuleImpl(
            moduleInfo.getModuleName(), syntheticModuleName,
            moduleInfo.getStrategy(), runStyle, batchingStrategy, treeLogger);
        preCompiledModuleDefs.put(syntheticModuleName, moduleDef);
      }
    }
  }
}

/**
 * 
 * Strategy that compiles only one module at a time. Optimizes memory usage.
 */
class SimpleCompileStrategy extends CompileStrategy {
  public SimpleCompileStrategy(JUnitShell junitShell) {
    super(junitShell);
  }

  @Override
  public ModuleDef maybeCompileModule(String moduleName,
      String syntheticModuleName, Strategy strategy, RunStyle runStyle,
      BatchingStrategy batchingStrategy, TreeLogger treeLogger)
      throws UnableToCompleteException {
    return maybeCompileModuleImpl(moduleName, syntheticModuleName, strategy,
        runStyle, batchingStrategy, treeLogger);
  }
}
