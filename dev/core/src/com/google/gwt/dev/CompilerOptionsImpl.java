/*
 * Copyright 2013 Google Inc.
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

import com.google.gwt.dev.Link.LinkOptionsImpl;

import java.io.File;

/**
 * Concrete class to implement compiler process, precompile task and compile task options.
 */
public class CompilerOptionsImpl extends PrecompileTaskOptionsImpl implements CompilerOptions {

  private LinkOptionsImpl linkOptions = new LinkOptionsImpl();
  private int localWorkers;

  public CompilerOptionsImpl() {
  }

  public CompilerOptionsImpl(CompilerOptions other) {
    copyFrom(other);
  }

  public void copyFrom(CompilerOptions other) {
    super.copyFrom(other);
    linkOptions.copyFrom(other);
    localWorkers = other.getLocalWorkers();
  }

  @Override
  public File getDeployDir() {
    return linkOptions.getDeployDir();
  }

  @Override
  public File getExtraDir() {
    return linkOptions.getExtraDir();
  }

  @Override
  public int getLocalWorkers() {
    return localWorkers;
  }

  @Override
  public File getSaveSourceOutput() {
    return linkOptions.getSaveSourceOutput();
  }

  @Override
  public File getWarDir() {
    return linkOptions.getWarDir();
  }

  @Override
  public void setDeployDir(File extraDir) {
    linkOptions.setDeployDir(extraDir);
  }

  @Override
  public void setExtraDir(File extraDir) {
    linkOptions.setExtraDir(extraDir);
  }

  @Override
  public void setLocalWorkers(int localWorkers) {
    this.localWorkers = localWorkers;
  }

  @Override
  public void setSaveSourceOutput(File dest) {
    linkOptions.setSaveSourceOutput(dest);
  }

  @Override
  public void setWarDir(File outDir) {
    linkOptions.setWarDir(outDir);
  }
}
