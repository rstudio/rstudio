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

import com.google.gwt.dev.javac.impl.JavaResourceBase;
import com.google.gwt.dev.javac.impl.MockJavaSourceFile;

/**
 * Contains standard Java source files for testing.
 */
public class JavaSourceCodeBase {

  public static final MockJavaSourceFile ANNOTATION = new MockJavaSourceFile(
      JavaResourceBase.ANNOTATION);
  public static final MockJavaSourceFile BAR = new MockJavaSourceFile(
      JavaResourceBase.BAR);
  public static final MockJavaSourceFile CLASS = new MockJavaSourceFile(
      JavaResourceBase.CLASS);
  public static final MockJavaSourceFile FOO = new MockJavaSourceFile(
      JavaResourceBase.FOO);
  public static final MockJavaSourceFile JAVASCRIPTOBJECT = new MockJavaSourceFile(
      JavaResourceBase.JAVASCRIPTOBJECT);
  public static final MockJavaSourceFile MAP = new MockJavaSourceFile(
      JavaResourceBase.MAP);
  public static final MockJavaSourceFile OBJECT = new MockJavaSourceFile(
      JavaResourceBase.OBJECT);
  public static final MockJavaSourceFile SERIALIZABLE = new MockJavaSourceFile(
      JavaResourceBase.SERIALIZABLE);
  public static final MockJavaSourceFile STRING = new MockJavaSourceFile(
      JavaResourceBase.STRING);
  public static final MockJavaSourceFile SUPPRESS_WARNINGS = new MockJavaSourceFile(
      JavaResourceBase.SUPPRESS_WARNINGS);

  public static MockJavaSourceFile[] getStandardResources() {
    return new MockJavaSourceFile[] {
        ANNOTATION, CLASS, JAVASCRIPTOBJECT, MAP, OBJECT, SERIALIZABLE, STRING,
        SUPPRESS_WARNINGS};
  }
}
