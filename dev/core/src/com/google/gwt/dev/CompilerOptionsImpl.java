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
package com.google.gwt.dev;

import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.dev.jjs.JJSOptionsImpl;

import java.io.File;

/**
 * Concrete class to implement all compiler options.
 */
public class CompilerOptionsImpl extends JJSOptionsImpl implements
    CompilerOptions {
  private File genDir;
  private Type logLevel;
  private String moduleName;
  private File outDir;
  private boolean useGuiLogger;
  private boolean validateOnly;

  public CompilerOptionsImpl() {
  }

  public CompilerOptionsImpl(CompilerOptions other) {
    copyFrom(other);
  }

  public void copyFrom(CompilerOptions other) {
    super.copyFrom(other);
    setGenDir(other.getGenDir());
    setLogLevel(other.getLogLevel());
    setOutDir(other.getOutDir());
    setUseGuiLogger(other.isUseGuiLogger());
    setValidateOnly(false);
  }

  public File getGenDir() {
    return genDir;
  }

  public Type getLogLevel() {
    return logLevel;
  }

  public String getModuleName() {
    return moduleName;
  }

  public File getOutDir() {
    return outDir;
  }

  public boolean isUseGuiLogger() {
    return useGuiLogger;
  }

  public boolean isValidateOnly() {
    return validateOnly;
  }

  public void setGenDir(File genDir) {
    this.genDir = genDir;
  }

  public void setLogLevel(Type logLevel) {
    this.logLevel = logLevel;
  }

  public void setModuleName(String moduleName) {
    this.moduleName = moduleName;
  }

  public void setOutDir(File outDir) {
    this.outDir = outDir;
  }

  public void setUseGuiLogger(boolean useGuiLogger) {
    this.useGuiLogger = useGuiLogger;
  }

  public void setValidateOnly(boolean validateOnly) {
    this.validateOnly = validateOnly;
  }
}