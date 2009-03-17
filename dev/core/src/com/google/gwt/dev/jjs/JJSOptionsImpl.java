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

import java.io.File;
import java.io.Serializable;

/**
 * Concrete class to implement all JJS options.
 */
public class JJSOptionsImpl implements JJSOptions, Serializable {

  private boolean aggressivelyOptimize = true;
  private boolean disableClassMetadata = false;
  private boolean draftCompile = false;
  private boolean enableAssertions;
  private JsOutputOption output = JsOutputOption.OBFUSCATED;
  private boolean runAsyncEnabled = true;
  private boolean soycEnabled = false;
  private File workDir;

  public JJSOptionsImpl() {
  }

  public JJSOptionsImpl(JJSOptions other) {
    copyFrom(other);
  }

  public void copyFrom(JJSOptions other) {
    setAggressivelyOptimize(other.isAggressivelyOptimize());
    setClassMetadataDisabled(other.isClassMetadataDisabled());
    setDraftCompile(other.isDraftCompile());
    setEnableAssertions(other.isEnableAssertions());
    setOutput(other.getOutput());
    setRunAsyncEnabled(other.isRunAsyncEnabled());
    setSoycEnabled(other.isSoycEnabled());
    setWorkDir(other.getWorkDir());
  }

  public JsOutputOption getOutput() {
    return output;
  }

  public File getWorkDir() {
    return workDir;
  }

  public boolean isAggressivelyOptimize() {
    return aggressivelyOptimize;
  }

  public boolean isClassMetadataDisabled() {
    return disableClassMetadata;
  }

  public boolean isDraftCompile() {
    return draftCompile;
  }

  public boolean isEnableAssertions() {
    return enableAssertions;
  }

  public boolean isRunAsyncEnabled() {
    return runAsyncEnabled;
  }

  public boolean isSoycEnabled() {
    return soycEnabled;
  }

  public void setAggressivelyOptimize(boolean aggressivelyOptimize) {
    this.aggressivelyOptimize = aggressivelyOptimize;
  }

  public void setClassMetadataDisabled(boolean disabled) {
    disableClassMetadata = disabled;
  }

  public void setDraftCompile(boolean draft) {
    this.draftCompile = draft;
  }

  public void setEnableAssertions(boolean enableAssertions) {
    this.enableAssertions = enableAssertions;
  }

  public void setOutput(JsOutputOption output) {
    this.output = output;
  }

  public void setRunAsyncEnabled(boolean enabled) {
    runAsyncEnabled = enabled;
  }

  public void setSoycEnabled(boolean enabled) {
    soycEnabled = enabled;
  }
  
  public void setWorkDir(File workDir) {
    this.workDir = workDir;
  }
}
