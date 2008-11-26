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

import com.google.gwt.dev.javac.JavaSourceFile;

public class MockJavaSourceFile extends JavaSourceFile {

  private final String location;
  private final String qualifiedTypeName;
  private final String source;

  public MockJavaSourceFile(JavaSourceFile sourceFile) {
    this(sourceFile.getTypeName(), sourceFile.readSource(),
        sourceFile.getLocation());
  }

  public MockJavaSourceFile(MockResource resource) {
    this(Shared.toTypeName(resource.getPath()),
        resource.getContent().toString(), resource.getLocation());
  }

  public MockJavaSourceFile(String qualifiedTypeName, String source) {
    this(qualifiedTypeName, source, "/mock/" + Shared.toPath(qualifiedTypeName));
  }

  public MockJavaSourceFile(String qualifiedTypeName, String source,
      String location) {
    this.qualifiedTypeName = qualifiedTypeName;
    this.source = source;
    this.location = location;
  }

  @Override
  public long getLastModified() {
    return 0;
  }

  @Override
  public String getLocation() {
    return location;
  }

  @Override
  public String getPackageName() {
    return Shared.getPackageName(qualifiedTypeName);
  }

  @Override
  public String getShortName() {
    return Shared.getShortName(qualifiedTypeName);
  }

  @Override
  public String getTypeName() {
    return qualifiedTypeName;
  }

  @Override
  public boolean isSuperSource() {
    return false;
  }

  @Override
  public String readSource() {
    return source;
  }

}
