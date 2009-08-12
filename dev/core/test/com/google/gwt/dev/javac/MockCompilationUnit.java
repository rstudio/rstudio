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

import com.google.gwt.dev.util.Name.BinaryName;

public class MockCompilationUnit extends CompilationUnit {

  private final String typeName;
  private final String source;

  public MockCompilationUnit(String typeName) {
    this.typeName = typeName;
    this.source = null;
  }

  public MockCompilationUnit(String typeName, String source) {
    this.typeName = typeName;
    this.source = source;
  }

  public String getDisplayLocation() {
    return "/mock/" + BinaryName.toInternalName(typeName) + ".java";
  }

  @Override
  public long getLastModified() {
    return 0;
  }

  @Override
  public String getSource() {
    assert source != null;
    return source;
  }

  public String getTypeName() {
    return typeName;
  }

  public boolean isGenerated() {
    return true;
  }

  @Override
  public boolean isSuperSource() {
    return false;
  }
}
