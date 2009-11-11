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
package com.google.gwt.dev.javac;

import com.google.gwt.dev.util.Util;

import java.io.File;

/**
 * A compilation unit based on a file.
 */
public class FileCompilationUnit extends CompilationUnit {
  private final File file;
  private final String typeName;

  public FileCompilationUnit(File file, String packageName) {
    this.file = file;
    String fileName = file.getName();
    assert fileName.endsWith(".java");
    fileName = fileName.substring(0, fileName.length() - 5);
    if (packageName.length() == 0) {
      this.typeName = fileName;
    } else {
      this.typeName = packageName + '.' + fileName;
    }
  }

  @Override
  public String getDisplayLocation() {
    return file.getAbsolutePath();
  }

  @Override
  public long getLastModified() {
    return file.lastModified();
  }

  @Override
  public String getSource() {
    return Util.readFileAsString(file);
  }

  @Override
  public String getTypeName() {
    return typeName;
  }

  @Override
  public boolean isGenerated() {
    return false;
  }

  @Override
  public boolean isSuperSource() {
    return false;
  }
}