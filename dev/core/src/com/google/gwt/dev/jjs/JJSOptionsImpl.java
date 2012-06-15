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

import com.google.gwt.dev.util.arg.OptionOptimize;

import java.io.Serializable;

/**
 * Concrete class to implement all JJS options.
 */
public class JJSOptionsImpl implements JJSOptions, Serializable {

  private boolean aggressivelyOptimize = true;
  private boolean compilerMetricsEnabled = false;
  private boolean disableCastChecking = false;
  private boolean disableClassMetadata = false;
  private boolean enableAssertions;
  private int optimizationLevel = OptionOptimize.OPTIMIZE_LEVEL_MAX;
  private boolean optimizePrecompile = false;
  private JsOutputOption output = JsOutputOption.OBFUSCATED;
  private boolean runAsyncEnabled = true;
  private boolean soycEnabled = false;
  private boolean soycExtra = false;
  private boolean soycHtmlDisabled = false;
  private boolean strict = false;
  private boolean closureCompilerEnabled;
  private int fragmentCount = -1;
  private int fragmentsMerge = -1;

  public JJSOptionsImpl() {
  }

  public JJSOptionsImpl(JJSOptions other) {
    copyFrom(other);
  }

  public void copyFrom(JJSOptions other) {
    setAggressivelyOptimize(other.isAggressivelyOptimize());
    setCastCheckingDisabled(other.isCastCheckingDisabled());
    setClassMetadataDisabled(other.isClassMetadataDisabled());
    setCompilerMetricsEnabled(other.isCompilerMetricsEnabled());
    setEnableAssertions(other.isEnableAssertions());
    setOptimizationLevel(other.getOptimizationLevel());
    setOutput(other.getOutput());
    setRunAsyncEnabled(other.isRunAsyncEnabled());
    setSoycEnabled(other.isSoycEnabled());
    setSoycExtra(other.isSoycExtra());
    setSoycHtmlDisabled(other.isSoycHtmlDisabled());
    setStrict(other.isStrict());
    setClosureCompilerEnabled(other.isClosureCompilerEnabled());
    setFragmentsMerge(other.getFragmentsMerge());
    setFragmentCount(other.getFragmentCount());
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
  public int getOptimizationLevel() {
    return optimizationLevel;
  }

  @Override
  public JsOutputOption getOutput() {
    return output;
  }

  @Override
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
    return this.closureCompilerEnabled;
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
  public void setAggressivelyOptimize(boolean aggressivelyOptimize) {
    this.aggressivelyOptimize = aggressivelyOptimize;
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
    this.closureCompilerEnabled = enabled;
  }

  @Override
  public void setCompilerMetricsEnabled(boolean enabled) {
    this.compilerMetricsEnabled = enabled;
  }

  @Override
  public void setEnableAssertions(boolean enableAssertions) {
    this.enableAssertions = enableAssertions;
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
  public void setOptimizationLevel(int level) {
    optimizationLevel = level;
  }

  @Override
  public void setOptimizePrecompile(boolean optimize) {
    optimizePrecompile = optimize;
  }

  @Override
  public void setOutput(JsOutputOption output) {
    this.output = output;
  }

  @Override
  public void setRunAsyncEnabled(boolean enabled) {
    runAsyncEnabled = enabled;
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
  public void setStrict(boolean strict) {
    this.strict = strict;
  }

}
