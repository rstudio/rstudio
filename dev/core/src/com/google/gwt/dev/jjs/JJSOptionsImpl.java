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
import com.google.gwt.dev.util.arg.OptionOptimize;
import com.google.gwt.dev.util.arg.SourceLevel;

import java.io.Serializable;

/**
 * Concrete class to implement all JJS options.
 */
public class JJSOptionsImpl implements JJSOptions, Serializable {

  private boolean addRuntimeChecks = false;
  private boolean aggressivelyOptimize = true;
  private boolean closureCompilerEnabled;
  private boolean clusterSimilarFunctions = true;
  private boolean compilerMetricsEnabled = false;
  private boolean disableCastChecking = false;
  private boolean disableClassMetadata = false;
  private boolean enableAssertions;
  private int fragmentCount = -1;
  private int fragmentsMerge = -1;
  private boolean inlineLiteralParameters = true;
  private boolean jsonSoycEnabled = false;
  private JsNamespaceOption namespace = JsNamespaceOption.NONE;
  private int optimizationLevel = OptionOptimize.OPTIMIZE_LEVEL_DEFAULT;
  private boolean optimizeDataflow = true;
  private boolean optimizePrecompile = false;
  private boolean ordinalizeEnums = true;
  private JsOutputOption output = JsOutputOption.OBFUSCATED;
  private boolean removeDuplicateFunctions = true;
  private boolean runAsyncEnabled = true;
  private SourceLevel sourceLevel = SourceLevel.DEFAULT_SOURCE_LEVEL;
  private boolean soycEnabled = false;
  private boolean soycExtra = false;
  private boolean soycHtmlDisabled = false;
  private boolean strict = false;
  private boolean strictSourceResources = false;
  private boolean strictPublicResources = false;

  public JJSOptionsImpl() {
  }

  public JJSOptionsImpl(JJSOptions other) {
    copyFrom(other);
  }

  public void copyFrom(JJSOptions other) {
    setAddRuntimeChecks(other.shouldAddRuntimeChecks());
    setAggressivelyOptimize(other.isAggressivelyOptimize());
    setCastCheckingDisabled(other.isCastCheckingDisabled());
    setClassMetadataDisabled(other.isClassMetadataDisabled());
    setClosureCompilerEnabled(other.isClosureCompilerEnabled());
    setClusterSimilarFunctions(other.shouldClusterSimilarFunctions());
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
    setEnforceStrictSourceResources(other.enforceStrictSourceResources());
    setEnforceStrictPublicResources(other.enforceStrictPublicResources());
    setSourceLevel(other.getSourceLevel());
    setNamespace(other.getNamespace());
  }

  @Override
  public boolean enforceStrictSourceResources() {
    return strictSourceResources;
  }

  @Override
  public boolean enforceStrictPublicResources() {
    return strictPublicResources;
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
  @Deprecated
  public boolean isAggressivelyOptimize() {
    return aggressivelyOptimize;
  }

  @Override
  public boolean isCastCheckingDisabled() {
    return disableCastChecking;
  }

  @Override
  public boolean isClassMetadataDisabled() {
    return disableClassMetadata;
  }

  @Override
  public boolean isClosureCompilerEnabled() {
    return closureCompilerEnabled;
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
  public boolean isOptimizePrecompile() {
    return optimizePrecompile;
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
  @Deprecated
  public void setAggressivelyOptimize(boolean enabled) {
    aggressivelyOptimize = enabled;
  }

  @Override
  public void setCastCheckingDisabled(boolean disabled) {
    disableCastChecking = disabled;
  }

  @Override
  public void setClassMetadataDisabled(boolean disabled) {
    disableClassMetadata = disabled;
  }

  @Override
  public void setClosureCompilerEnabled(boolean enabled) {
    closureCompilerEnabled = enabled;
  }

  @Override
  public void setClusterSimilarFunctions(boolean enabled) {
    clusterSimilarFunctions = enabled;
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
  public void setEnforceStrictSourceResources(boolean strictSourceResources) {
    this.strictSourceResources = strictSourceResources;
  }

  @Override
  public void setEnforceStrictPublicResources(boolean strictPublicResources) {
    this.strictPublicResources = strictPublicResources;
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
  public void setOptimizePrecompile(boolean optimize) {
    optimizePrecompile = optimize;
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
  public boolean shouldAddRuntimeChecks() {
    return addRuntimeChecks;
  }

  @Override
  public boolean shouldClusterSimilarFunctions() {
    return clusterSimilarFunctions;
  }

  @Override
  public boolean shouldInlineLiteralParameters() {
    return inlineLiteralParameters;
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
}
