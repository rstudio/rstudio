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
package com.google.gwt.dev.javac.impl;

import com.google.gwt.dev.javac.CompilationUnit;
import com.google.gwt.dev.javac.JavaSourceFile;

/**
 * A compilation unit that was generated.
 */
public class SourceFileCompilationUnit extends CompilationUnit {

  private String sourceCode;
  private JavaSourceFile sourceFile;

  public SourceFileCompilationUnit(JavaSourceFile sourceFile) {
    this.sourceFile = sourceFile;
  }

  @Override
  public String getDisplayLocation() {
    return sourceFile.getLocation();
  }

  @Override
  public long getLastModified() {
    return sourceFile.getLastModified();
  }

  @Override
  public String getSource() {
    if (sourceCode == null) {
      sourceCode = sourceFile.readSource();
    }
    return sourceCode;
  }

  public JavaSourceFile getSourceFile() {
    return sourceFile;
  }

  @Override
  public String getTypeName() {
    return sourceFile.getTypeName();
  }

  @Override
  public boolean isGenerated() {
    return false;
  }

  @Override
  protected void dumpSource() {
    sourceCode = null;
  }

}
