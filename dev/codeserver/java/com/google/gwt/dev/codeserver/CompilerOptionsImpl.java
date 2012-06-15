/*
 * Copyright 2011 Google Inc.
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

package com.google.gwt.dev.codeserver;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.jjs.JsOutputOption;
import com.google.gwt.dev.util.arg.OptionOptimize;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Defines the compiler settings that Super Dev Mode uses for compiling
 * GWT apps. For now, these settings are hard-coded to reasonable defaults.
 */
class CompilerOptionsImpl extends UnmodifiableCompilerOptions {
  private final CompileDir compileDir;
  private final String moduleName;

  CompilerOptionsImpl(CompileDir compileDir, String moduleName) {
    this.compileDir = compileDir;
    this.moduleName = moduleName;
  }

  @Override
  public File getDeployDir() {
    return compileDir.getDeployDir();
  }

  @Override
  public File getExtraDir() {
    return compileDir.getExtraDir();
  }

  @Override
  public int getFragmentCount() {
    return -1;
  }
  
  @Override
  public int getFragmentsMerge() {
    return -1;
  }

  @Override
  public File getGenDir() {
    return compileDir.getGenDir();
  }

  /**
   * Number of threads to use to compile permutations.
   */
  @Override
  public int getLocalWorkers() {
    return 1;
  }

  @Override
  public TreeLogger.Type getLogLevel() {
    return TreeLogger.Type.WARN;
  }

  @Override
  public int getMaxPermsPerPrecompile() {
    return -1;
  }

  @Override
  public List<String> getModuleNames() {
    return Arrays.asList(moduleName);
  }

  @Override
  public int getOptimizationLevel() {
    return OptionOptimize.OPTIMIZE_LEVEL_DRAFT;
  }

  @Override
  public File getOutDir() {
    return null; // unused?
  }

  @Override
  public JsOutputOption getOutput() {
    return JsOutputOption.PRETTY;
  }

  @Override
  public File getWarDir() {
    return compileDir.getWarDir();
  }

  @Override
  public File getWorkDir() {
    return compileDir.getWorkDir();
  }

  @Override
  public boolean isAggressivelyOptimize() {
    return false;
  }

  @Override
  public boolean isCastCheckingDisabled() {
    return false;
  }

  @Override
  public boolean isClassMetadataDisabled() {
    return false;
  }

  @Override
  public boolean isClosureCompilerEnabled() {
    return false;
  }

  @Override
  public boolean isCompilerMetricsEnabled() {
    return false;
  }

  @Override
  public boolean isEnableAssertions() {
    return true;
  }

  @Override
  public boolean isEnabledGeneratingOnShards() {
    return true;
  }

  @Override
  public boolean isOptimizePrecompile() {
    return true;
  }

  @Override
  public boolean isRunAsyncEnabled() {
    return false;
  }

  @Override
  public boolean isSoycEnabled() {
    return false;
  }

  @Override
  public boolean isSoycExtra() {
    return false;
  }

  @Override
  public boolean isSoycHtmlDisabled() {
    return true;
  }

  @Override
  public boolean isStrict() {
    return false;
  }

  @Override
  public boolean isUpdateCheckDisabled() {
    return true;
  }

  @Override
  public boolean isUseGuiLogger() {
    return false;
  }

  @Override
  public boolean isValidateOnly() {
    return false;
  }

}
