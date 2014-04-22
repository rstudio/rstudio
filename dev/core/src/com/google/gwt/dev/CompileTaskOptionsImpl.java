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
import com.google.gwt.dev.cfg.Properties;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Concrete class to implement compile task options.
 */
class CompileTaskOptionsImpl implements CompileTaskOptions {

  private Properties finalProperties;
  private List<String> libraryPaths = new ArrayList<String>();
  private boolean link;
  private Type logLevel;
  private final List<String> moduleNames = new ArrayList<String>();
  private String outputLibraryPath;
  private File workDir;

  public CompileTaskOptionsImpl() {
  }

  public CompileTaskOptionsImpl(CompileTaskOptions other) {
    copyFrom(other);
  }

  @Override
  public void addModuleName(String moduleName) {
    moduleNames.add(moduleName);
  }

  public void copyFrom(CompileTaskOptions other) {
    setLogLevel(other.getLogLevel());
    setModuleNames(other.getModuleNames());
    setWorkDir(other.getWorkDir());
    setLibraryPaths(other.getLibraryPaths());
    setOutputLibraryPath(other.getOutputLibraryPath());
    setFinalProperties(other.getFinalProperties());
    setLink(other.shouldLink());
  }

  public File getCompilerWorkDir(String moduleName) {
    return new File(new File(getWorkDir(), moduleName), "compiler");
  }

  @Override
  public Properties getFinalProperties() {
    return finalProperties;
  }

  @Override
  public List<String> getLibraryPaths() {
    return libraryPaths;
  }

  @Override
  public Type getLogLevel() {
    return logLevel;
  }

  @Override
  public List<String> getModuleNames() {
    return new ArrayList<String>(moduleNames);
  }

  @Override
  public String getOutputLibraryPath() {
    return outputLibraryPath;
  }

  @Override
  public File getWorkDir() {
    return workDir;
  }

  @Override
  public void setFinalProperties(Properties finalProperties) {
    this.finalProperties = finalProperties;
  }

  @Override
  public void setLibraryPaths(List<String> libraryPaths) {
    this.libraryPaths.clear();
    this.libraryPaths.addAll(libraryPaths);
  }

  @Override
  public void setLink(boolean link) {
    this.link = link;
  }

  @Override
  public void setLogLevel(Type logLevel) {
    this.logLevel = logLevel;
  }

  @Override
  public void setModuleNames(List<String> moduleNames) {
    this.moduleNames.clear();
    this.moduleNames.addAll(moduleNames);
  }

  @Override
  public void setOutputLibraryPath(String outputLibraryPath) {
    this.outputLibraryPath = outputLibraryPath;
  }

  @Override
  public void setWorkDir(File workDir) {
    this.workDir = workDir;
  }

  @Override
  public boolean shouldLink() {
    return link;
  }
}
