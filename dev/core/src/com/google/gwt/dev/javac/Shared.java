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

import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.util.Util;

import java.io.IOException;
import java.io.InputStream;

/**
 * A grab bag of utility functions useful for dealing with java files.
 */
public class Shared {

  public static final int MOD_ABSTRACT = 0x00000001;
  public static final int MOD_FINAL = 0x00000002;
  public static final int MOD_NATIVE = 0x00000004;
  public static final int MOD_PRIVATE = 0x00000008;
  public static final int MOD_PROTECTED = 0x00000010;
  public static final int MOD_PUBLIC = 0x00000020;
  public static final int MOD_STATIC = 0x00000040;
  public static final int MOD_TRANSIENT = 0x00000080;
  public static final int MOD_VOLATILE = 0x00000100;

  public static String getPackageName(String qualifiedTypeName) {
    int pos = qualifiedTypeName.lastIndexOf('.');
    return (pos < 0) ? "" : qualifiedTypeName.substring(0, pos);
  }

  public static String getPackageNameFromBinary(String binaryName) {
    int pos = binaryName.lastIndexOf('/');
    return (pos < 0) ? "" : binaryName.substring(0, pos).replace('/', '.');
  }

  public static String getShortName(String qualifiedTypeName) {
    int pos = qualifiedTypeName.lastIndexOf('.');
    return (pos < 0) ? qualifiedTypeName : qualifiedTypeName.substring(pos + 1);
  }

  public static String getSlashedPackageFrom(String internalName) {
    int pos = internalName.lastIndexOf('/');
    return (pos < 0) ? "" : internalName.substring(0, pos);
  }

  /**
   * Returns the source name of the Java type defined within the given source Resource.
   */
  public static String getTypeName(Resource sourceFile) {
    String path = sourceFile.getPath();
    assert (path.endsWith(".java"));
    path = path.substring(0, path.lastIndexOf('.'));
    return path.replace('/', '.');
  }

  public static String makeTypeName(String packageName, String shortName) {
    if (packageName.length() == 0) {
      return shortName;
    } else {
      return packageName + '.' + shortName;
    }
  }

  public static String readSource(Resource sourceFile) throws IOException {
    InputStream contents = sourceFile.openContents();
    return Util.readStreamAsString(contents);
  }

  public static String toPath(String qualifiedTypeName) {
    return qualifiedTypeName.replace('.', '/') + ".java";
  }

  /**
   * Returns the source name of the type defined within the given path.
   */
  public static String toTypeName(String path) {
    assert (path.endsWith(".java"));
    path = path.substring(0, path.lastIndexOf('.'));
    return path.replace('/', '.');
  }
}
