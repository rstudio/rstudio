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
package com.google.gwt.dev.jjs;

import com.google.gwt.dev.js.JsNamespaceOption;
import com.google.gwt.dev.util.arg.OptionMethodNameDisplayMode;
import com.google.gwt.dev.util.arg.OptionOptimize;
import com.google.gwt.dev.util.arg.SourceLevel;

import java.io.Serializable;

/**
 * Concrete class to implement all JJS options.
 */
public class JJSOptionsImpl implements JJSOptions, Serializable {

  private boolean addRuntimeChecks = false;
  private boolean clusterSimilarFunctions = true;
  private boolean incrementalCompile = false;
  private boolean compilerMetricsEnabled = false;
  private boolean disableClassMetadata = false;
  private boolean enableAssertions;
  private int fragmentCount = -1;
  private int fragmentsMerge = -1;
  private boolean inlineLiteralParameters = true;
  private boolean jsonSoycEnabled = false;
  private JsNamespaceOption namespace = JsNamespaceOption.NONE;
  private int optimizationLevel = OptionOptimize.OPTIMIZE_LEVEL_DEFAULT;
  private boolean optimizeDataflow = true;
  private boolean ordinalizeEnums = true;
  private JsOutputOption output = JsOutputOption.OBFUSCATED;
  private boolean removeDuplicateFunctions = true;
  private boolean runAsyncEnabled = true;
  private SourceLevel sourceLevel = SourceLevel.DEFAULT_SOURCE_LEVEL;
  private boolean soycEnabled = false;
  private boolean soycExtra = false;
  private boolean soycHtmlDisabled = false;
  private boolean strict = false;
  private boolean generateJsInteropExports = false;
  private boolean useDetailedTypeIds = false;
  private OptionMethodNameDisplayMode.Mode methodNameDisplayMode =
      OptionMethodNameDisplayMode.Mode.NONE;
  private boolean closureFormatEnabled = false;

  public JJSOptionsImpl() {
  }

  public void copyFrom(JJSOptions other) {
    setAddRuntimeChecks(other.shouldAddRuntimeChecks());
    setClassMetadataDisabled(other.isClassMetadataDisabled());
    setClusterSimilarFunctions(other.shouldClusterSimilarFunctions());
    setIncrementalCompileEnabled(other.isIncrementalCompileEnabled());
    setCompilerMetricsEnabled(other.isCompilerMetricsEnabled());
    setEnableAssertions(other.isEnableAssertions());
    setFragmentCount(other.getFragmentCount());
    setFragmentsMerge(other.getFragmentsMerge());
    setInlineLiteralParameters(other.shouldInlineLiteralParameters());
    setOptimizationLevel(other.getOptimizationLevel());
    setOptimizeDataflow(other.shouldOptimizeDataflow());
    setOrdinalizeEnums(other.shouldOrdinalizeEnums());
    setOutput(other.getOutput());
    setRemoveDuplicateFunctions(other.shouldRemoveDuplicateFunctions());
    setRunAsyncEnabled(other.isRunAsyncEnabled());
    setSoycEnabled(other.isSoycEnabled());
    setSoycExtra(other.isSoycExtra());
    setJsonSoycEnabled(other.isJsonSoycEnabled());
    setSoycHtmlDisabled(other.isSoycHtmlDisabled());
    setStrict(other.isStrict());
    setSourceLevel(other.getSourceLevel());
    setNamespace(other.getNamespace());
    setGenerateJsInteropExports(other.shouldGenerateJsInteropExports());
    setUseDetailedTypeIds(other.useDetailedTypeIds());
    setMethodNameDisplayMode(other.getMethodNameDisplayMode());
    setClosureCompilerFormatEnabled(other.isClosureCompilerFormatEnabled());
  }

  @Override
  public int getFragmentCount() {
    return fragmentCount;
  }

  @Override
  public int getFragmentsMerge() {
    return fragmentsMerge;
  }

  @Override
  public com.google.gwt.dev.util.arg.OptionMethodNameDisplayMode.Mode getMethodNameDisplayMode() {
    return methodNameDisplayMode;
  }
  @Override
  public JsNamespaceOption getNamespace() {
    return namespace;
  }

  @Override
  public int getOptimizationLevel() {
    return optimizationLevel;
  }

  @Override
  public JsOutputOption getOutput() {
    return output;
  }

  @Override
  public SourceLevel getSourceLevel() {
    return sourceLevel;
  }

  @Override
  public boolean isClassMetadataDisabled() {
    return disableClassMetadata;
  }

  @Override
  public boolean isCompilerMetricsEnabled() {
    return compilerMetricsEnabled;
  }

  public boolean isDraftCompile() {
    return optimizationLevel == OptionOptimize.OPTIMIZE_LEVEL_DRAFT;
  }

  @Override
  public boolean isEnableAssertions() {
    return enableAssertions;
  }

  @Override
  public boolean isJsonSoycEnabled() {
    return jsonSoycEnabled;
  }

  @Override
  public boolean isRunAsyncEnabled() {
    return runAsyncEnabled;
  }

  @Override
  public boolean isSoycEnabled() {
    return soycEnabled;
  }

  @Override
  public boolean isSoycExtra() {
    return soycExtra;
  }

  @Override
  public boolean isSoycHtmlDisabled() {
    return soycHtmlDisabled;
  }

  @Override
  public boolean isStrict() {
    return strict;
  }

  @Override
  public void setAddRuntimeChecks(boolean enabled) {
    addRuntimeChecks = enabled;
  }

  @Override
  public void setClassMetadataDisabled(boolean disabled) {
    disableClassMetadata = disabled;
  }

  @Override
  public void setClusterSimilarFunctions(boolean enabled) {
    clusterSimilarFunctions = enabled;
  }

  @Override
  public void setIncrementalCompileEnabled(boolean enabled) {
    incrementalCompile = enabled;
  }

  @Override
  public void setCompilerMetricsEnabled(boolean enabled) {
    compilerMetricsEnabled = enabled;
  }

  @Override
  public void setEnableAssertions(boolean enabled) {
    enableAssertions = enabled;
  }

  @Override
  public void setFragmentCount(int numFragments) {
    this.fragmentCount = numFragments;
  }

  @Override
  public void setFragmentsMerge(int numFragments) {
    this.fragmentsMerge = numFragments;
  }

  @Override
  public void setInlineLiteralParameters(boolean enabled) {
    inlineLiteralParameters = enabled;
  }

  @Override
  public void setJsonSoycEnabled(boolean enabled) {
    jsonSoycEnabled = enabled;
  }

  @Override
  public void setMethodNameDisplayMode(
      com.google.gwt.dev.util.arg.OptionMethodNameDisplayMode.Mode methodNameDisplayMode) {
    this.methodNameDisplayMode = methodNameDisplayMode;
  }

  @Override
  public void setNamespace(JsNamespaceOption newValue) {
    namespace = newValue;
  }

  @Override
  public void setOptimizationLevel(int level) {
    optimizationLevel = level;
  }

  @Override
  public void setOptimizeDataflow(boolean enabled) {
    optimizeDataflow = enabled;
  }

  @Override
  public void setOrdinalizeEnums(boolean enabled) {
    ordinalizeEnums = enabled;
  }

  @Override
  public void setOutput(JsOutputOption output) {
    this.output = output;
  }

  @Override
  public void setRemoveDuplicateFunctions(boolean enabled) {
    removeDuplicateFunctions = enabled;
  }

  @Override
  public void setRunAsyncEnabled(boolean enabled) {
    runAsyncEnabled = enabled;
  }

  @Override
  public void setSourceLevel(SourceLevel sourceLevel) {
    this.sourceLevel = sourceLevel;
  }

  @Override
  public void setSoycEnabled(boolean enabled) {
    soycEnabled = enabled;
  }

  @Override
  public void setSoycExtra(boolean enabled) {
    soycExtra = enabled;
  }

  @Override
  public void setSoycHtmlDisabled(boolean disabled) {
    soycHtmlDisabled = disabled;
  }

  @Override
  public void setStrict(boolean enabled) {
    strict = enabled;
  }

  @Override
  public void setUseDetailedTypeIds(boolean enabled) {
    useDetailedTypeIds = enabled;
  }

  @Override
  public boolean shouldAddRuntimeChecks() {
    return addRuntimeChecks;
  }

  @Override
  public boolean shouldClusterSimilarFunctions() {
    return clusterSimilarFunctions;
  }

  @Override
  public boolean isIncrementalCompileEnabled() {
    return incrementalCompile;
  }

  @Override
  public boolean shouldInlineLiteralParameters() {
    return inlineLiteralParameters;
  }

  @Override
  public boolean shouldJDTInlineCompileTimeConstants() {
    return !isIncrementalCompileEnabled();
  }

  @Override
  public boolean shouldOptimizeDataflow() {
    return optimizeDataflow;
  }

  @Override
  public boolean shouldOrdinalizeEnums() {
    return ordinalizeEnums;
  }

  @Override
  public boolean shouldRemoveDuplicateFunctions() {
    return removeDuplicateFunctions;
  }

  @Override
  public boolean shouldGenerateJsInteropExports() {
    return generateJsInteropExports;
  }

  @Override
  public void setGenerateJsInteropExports(boolean generateExports) {
    generateJsInteropExports = generateExports;
  }

  @Override
  public boolean useDetailedTypeIds() {
    return useDetailedTypeIds;
  }

  @Override
  public boolean isClosureCompilerFormatEnabled() {
    return closureFormatEnabled;
  }

  @Override
  public void setClosureCompilerFormatEnabled(boolean enabled) {
    closureFormatEnabled = enabled;
  }
}
