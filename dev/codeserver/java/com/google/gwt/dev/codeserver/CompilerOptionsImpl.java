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
import com.google.gwt.dev.cfg.Properties;
import com.google.gwt.dev.jjs.JsOutputOption;
import com.google.gwt.dev.js.JsNamespaceOption;
import com.google.gwt.dev.util.arg.OptionMethodNameDisplayMode;
import com.google.gwt.dev.util.arg.OptionOptimize;
import com.google.gwt.dev.util.arg.SourceLevel;
import com.google.gwt.thirdparty.guava.common.collect.LinkedListMultimap;
import com.google.gwt.thirdparty.guava.common.collect.ListMultimap;
import com.google.gwt.thirdparty.guava.common.collect.Lists;

import java.io.File;
import java.util.List;

/**
 * Defines the compiler settings that Super Dev Mode uses for compiling
 * GWT apps. For now, these settings are hard-coded to reasonable defaults.
 */
class CompilerOptionsImpl extends UnmodifiableCompilerOptions {
  private final CompileDir compileDir;
  private final boolean incremental;
  private final boolean failOnError;
  private final TreeLogger.Type logLevel;
  private final List<String> moduleNames;
  private final SourceLevel sourceLevel;
  private final boolean generateJsInteropExports;
  private final OptionMethodNameDisplayMode.Mode methodNameDisplayMode;
  private final ListMultimap<String, String> properties;
  private final boolean closureFormattedOutput;
  private final JsOutputOption output;

  CompilerOptionsImpl(CompileDir compileDir, String moduleName, Options options) {
    this.compileDir = compileDir;
    this.incremental = options.isIncrementalCompileEnabled();
    this.moduleNames = Lists.newArrayList(moduleName);
    this.sourceLevel = options.getSourceLevel();
    this.failOnError = options.isFailOnError();
    this.logLevel = options.getLogLevel();
    this.generateJsInteropExports = options.shouldGenerateJsInteropExports();
    this.methodNameDisplayMode = options.getMethodNameDisplayMode();
    this.properties = LinkedListMultimap.create(options.getProperties());
    this.closureFormattedOutput = options.isClosureFormattedOutput();
    this.output = options.getOutput();
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
  public Properties getFinalProperties() {
    return null; // handling this in a different way
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

  @Override
  public boolean shouldGenerateJsInteropExports() {
    return generateJsInteropExports;
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
    return logLevel;
  }

  @Override
  public OptionMethodNameDisplayMode.Mode getMethodNameDisplayMode() {
    return methodNameDisplayMode;
  }

  @Override
  public List<String> getModuleNames() {
    return moduleNames;
  }

  @Override
  public JsNamespaceOption getNamespace() {
    return JsNamespaceOption.PACKAGE;
  }

  @Override
  public int getOptimizationLevel() {
    return OptionOptimize.OPTIMIZE_LEVEL_DRAFT;
  }

  @Override
  public JsOutputOption getOutput() {
    return output;
  }

  @Override
  public ListMultimap<String, String> getProperties() {
    return properties;
  }

  @Override
  public File getSaveSourceOutput() {
    return null;
  }

  @Override
  public SourceLevel getSourceLevel() {
    return sourceLevel;
  }

  @Override
  public String getSourceMapFilePrefix() {
    return SourceHandler.SOURCEROOT_TEMPLATE_VARIABLE;
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
  public boolean isClassMetadataDisabled() {
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
  public boolean isIncrementalCompileEnabled() {
    return incremental;
  }

  @Override
  public  boolean isJsonSoycEnabled() {
    return false;
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
    return failOnError;
  }

  @Override
  public boolean isUpdateCheckDisabled() {
    return true;
  }

  @Override
  public boolean isValidateOnly() {
    return false;
  }

  @Override
  public boolean shouldAddRuntimeChecks() {
    // Not needed since no optimizations are on.
    return false;
  }

  @Override
  public boolean shouldClusterSimilarFunctions() {
    return false;
  }

  @Override
  public boolean shouldInlineLiteralParameters() {
    return false;
  }

  @Override
  public boolean shouldJDTInlineCompileTimeConstants() {
    return !isIncrementalCompileEnabled();
  }

  @Override
  public boolean shouldOptimizeDataflow() {
    return false;
  }

  @Override
  public boolean shouldOrdinalizeEnums() {
    return false;
  }

  @Override
  public boolean shouldRemoveDuplicateFunctions() {
    return false;
  }

  @Override
  public boolean shouldSaveSource() {
    return false; // handling this a different way
  }

  @Override
  public boolean useDetailedTypeIds() {
    return false;
  }

  @Override
  public boolean isClosureCompilerFormatEnabled() {
    return closureFormattedOutput;
  }
}
