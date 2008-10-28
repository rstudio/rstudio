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

import java.io.File;

/**
 * Concrete class to implement compiler task options.
 */
class CompileTaskOptionsImpl implements CompileTaskOptions {

  public static final String GWT_COMPILER_DIR = ".gwt-tmp" + File.separatorChar
      + "compiler";

  private File compilerWorkDir;
  private Type logLevel;
  private String moduleName;
  private File outDir;
  private boolean useGuiLogger;

  public CompileTaskOptionsImpl() {
  }

  public CompileTaskOptionsImpl(CompileTaskOptions other) {
    copyFrom(other);
  }

  public void copyFrom(CompileTaskOptions other) {
    setLogLevel(other.getLogLevel());
    setModuleName(other.getModuleName());
    setOutDir(other.getOutDir());
    setUseGuiLogger(other.isUseGuiLogger());
  }

  public File getCompilerWorkDir() {
    if (compilerWorkDir == null) {
      compilerWorkDir = new File(getOutDir(), GWT_COMPILER_DIR + File.separator
          + moduleName);
    }
    return compilerWorkDir;
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

}