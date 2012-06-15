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

package com.google.gwt.dev;

import com.google.gwt.dev.jjs.JJSOptionsImpl;
import com.google.gwt.dev.jjs.JsOutputOption;

import java.io.File;

class PrecompileTaskOptionsImpl extends CompileTaskOptionsImpl implements PrecompileTaskOptions {
  private boolean disableUpdateCheck;
  private boolean enableGeneratingOnShards = true;
  private File genDir;
  private final JJSOptionsImpl jjsOptions = new JJSOptionsImpl();
  private int maxPermsPerPrecompile;
  private boolean validateOnly;

  public PrecompileTaskOptionsImpl() {
  }

  public PrecompileTaskOptionsImpl(PrecompileTaskOptions other) {
    copyFrom(other);
  }

  public void copyFrom(PrecompileTaskOptions other) {
    super.copyFrom(other);

    jjsOptions.copyFrom(other);

    setDisableUpdateCheck(other.isUpdateCheckDisabled());
    setGenDir(other.getGenDir());
    setMaxPermsPerPrecompile(other.getMaxPermsPerPrecompile());
    setValidateOnly(other.isValidateOnly());
    setEnabledGeneratingOnShards(other.isEnabledGeneratingOnShards());
  }

  @Override
  public int getFragmentCount() {
    return jjsOptions.getFragmentCount();
  }
  
  @Override
  public int getFragmentsMerge() {
    return jjsOptions.getFragmentsMerge();
  }

  @Override
  public File getGenDir() {
    return genDir;
  }

  @Override
  public int getMaxPermsPerPrecompile() {
    return maxPermsPerPrecompile;
  }

  @Override
  public int getOptimizationLevel() {
    return jjsOptions.getOptimizationLevel();
  }

  @Override
  public JsOutputOption getOutput() {
    return jjsOptions.getOutput();
  }

  @Override
  public boolean isAggressivelyOptimize() {
    return jjsOptions.isAggressivelyOptimize();
  }

  @Override
  public boolean isCastCheckingDisabled() {
    return jjsOptions.isCastCheckingDisabled();
  }

  @Override
  public boolean isClassMetadataDisabled() {
    return jjsOptions.isClassMetadataDisabled();
  }

  @Override
  public boolean isClosureCompilerEnabled() {
    return jjsOptions.isClosureCompilerEnabled();
  }

  @Override
  public boolean isCompilerMetricsEnabled() {
    return jjsOptions.isCompilerMetricsEnabled();
  }

  public boolean isDraftCompile() {
    return jjsOptions.isDraftCompile();
  }

  @Override
  public boolean isEnableAssertions() {
    return jjsOptions.isEnableAssertions();
  }

  @Override
  public boolean isEnabledGeneratingOnShards() {
    return enableGeneratingOnShards;
  }

  @Override
  public boolean isOptimizePrecompile() {
    return jjsOptions.isOptimizePrecompile();
  }

  @Override
  public boolean isRunAsyncEnabled() {
    return jjsOptions.isRunAsyncEnabled();
  }

  @Override
  public boolean isSoycEnabled() {
    return jjsOptions.isSoycEnabled();
  }

  @Override
  public boolean isSoycExtra() {
    return jjsOptions.isSoycExtra();
  }

  @Override
  public boolean isSoycHtmlDisabled() {
    return jjsOptions.isSoycHtmlDisabled();
  }

  @Override
  public boolean isStrict() {
    return jjsOptions.isStrict();
  }

  @Override
  public boolean isUpdateCheckDisabled() {
    return disableUpdateCheck;
  }

  @Override
  public boolean isValidateOnly() {
    return validateOnly;
  }

  @Override
  public void setAggressivelyOptimize(boolean aggressivelyOptimize) {
    jjsOptions.setAggressivelyOptimize(aggressivelyOptimize);
  }

  @Override
  public void setCastCheckingDisabled(boolean disabled) {
    jjsOptions.setCastCheckingDisabled(disabled);
  }

  @Override
  public void setClassMetadataDisabled(boolean disabled) {
    jjsOptions.setClassMetadataDisabled(disabled);
  }

  @Override
  public void setClosureCompilerEnabled(boolean enabled) {
    jjsOptions.setClosureCompilerEnabled(enabled);
  }

  @Override
  public void setCompilerMetricsEnabled(boolean enabled) {
    jjsOptions.setCompilerMetricsEnabled(enabled);
  }

  @Override
  public void setDisableUpdateCheck(boolean disabled) {
    disableUpdateCheck = disabled;
  }

  @Override
  public void setEnableAssertions(boolean enableAssertions) {
    jjsOptions.setEnableAssertions(enableAssertions);
  }

  @Override
  public void setEnabledGeneratingOnShards(boolean enabled) {
    enableGeneratingOnShards = enabled;
  }

  @Override
  public void setFragmentCount(int numFragments) {
    jjsOptions.setFragmentCount(numFragments);
  }
  
  @Override
  public void setFragmentsMerge(int numFragments) {
    jjsOptions.setFragmentsMerge(numFragments);
  }

  @Override
  public void setGenDir(File genDir) {
    this.genDir = genDir;
  }

  @Override
  public void setMaxPermsPerPrecompile(int maxPermsPerPrecompile) {
    this.maxPermsPerPrecompile = maxPermsPerPrecompile;
  }

  @Override
  public void setOptimizationLevel(int level) {
    jjsOptions.setOptimizationLevel(level);
  }

  @Override
  public void setOptimizePrecompile(boolean optimize) {
    jjsOptions.setOptimizePrecompile(optimize);
  }

  @Override
  public void setOutput(JsOutputOption output) {
    jjsOptions.setOutput(output);
  }

  @Override
  public void setRunAsyncEnabled(boolean enabled) {
    jjsOptions.setRunAsyncEnabled(enabled);
  }

  @Override
  public void setSoycEnabled(boolean enabled) {
    jjsOptions.setSoycEnabled(enabled);
  }

  @Override
  public void setSoycExtra(boolean soycExtra) {
    jjsOptions.setSoycExtra(soycExtra);
  }

  @Override
  public void setSoycHtmlDisabled(boolean disabled) {
    jjsOptions.setSoycHtmlDisabled(disabled);
  }

  @Override
  public void setStrict(boolean strict) {
    jjsOptions.setStrict(strict);
  }

  @Override
  public void setValidateOnly(boolean validateOnly) {
    this.validateOnly = validateOnly;
  }
}