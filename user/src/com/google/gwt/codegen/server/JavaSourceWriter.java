/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.codegen.server;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A mechanism to write Java source files.
 * 
 * @see JavaSourceWriterBuilder
 * <p>
 * Experimental API - subject to change.
 */
public class JavaSourceWriter extends SourceWriterBase {

  private static final Pattern PKG_REGEX_BOTH = Pattern.compile("(com\\.google|javax?)\\..*");
  private static final Pattern PKG_REGEX_GOOGLE = Pattern.compile("com\\.google\\..*");
  private static final Pattern PKG_REGEX_JAVA = Pattern.compile("javax?\\..*");

  private final AbortablePrintWriter printWriter;

  /**
   * @param printWriter
   * @param targetPackageName
   * @param imports
   * @param isClass
   * @param classJavaDocComment
   * @param annotationDeclarations
   * @param targetClassShortName
   * @param superClassName
   * @param interfaceNames
   */
  public JavaSourceWriter(AbortablePrintWriter printWriter, String targetPackageName,
      Iterable<String> imports, boolean isClass, String classJavaDocComment,
      Iterable<String> annotationDeclarations, String targetClassShortName, String superClassName,
      Iterable<String> interfaceNames) {
    this.printWriter = printWriter;
    if (targetPackageName == null) {
      throw new IllegalArgumentException("Cannot supply a null package name to"
          + targetClassShortName);
    }
    // TODO: support a user-specified file header
    if (targetPackageName.length() > 0) {
      println("package " + targetPackageName + ";");
    }

    // Write imports, splitting into com.google, other, and java/javax groups
    writeImportGroup(imports, PKG_REGEX_GOOGLE, true);
    writeImportGroup(imports, PKG_REGEX_BOTH, false);
    writeImportGroup(imports, PKG_REGEX_JAVA, true);

    // Write class header
    if (classJavaDocComment != null) {
      beginJavaDocComment();
      print(classJavaDocComment);
      endJavaDocComment();
    } else {
      // beginJavaDocComment adds its own leading newline, make up for it here.
      println();
    }
    for (String annotation : annotationDeclarations) {
      println('@' + annotation);
    }
    if (isClass) {
      emitClassDecl(targetClassShortName, superClassName, interfaceNames);
    } else {
      emitInterfaceDecl(targetClassShortName, superClassName, interfaceNames);
    }
    println(" {");
    indent();
  }

  @Override
  public void abort() {
    printWriter.abort();
  }

  @Override
  public void close() {
    super.close();
    printWriter.close();
  }

  @Override
  protected void writeString(String s) {
    printWriter.print(s);
  }

  private void emitClassDecl(String targetClassShortName,
      String superClassName, Iterable<String> interfaceNames) {
    print("public class " + targetClassShortName);
    if (superClassName != null) {
      print(" extends " + superClassName);
    }
    boolean first = true;
    for (String interfaceName : interfaceNames) {
      if (first) {
        print(" implements ");
        first = false;
      } else {
        print(", ");
      }
      print(interfaceName);
    }
  }

  private void emitInterfaceDecl(String targetClassShortName,
      String superClassName, Iterable<String> interfaceNames) {
    if (superClassName != null) {
      throw new IllegalArgumentException("Cannot set superclass name "
          + superClassName + " on a interface.");
    }
    print("public interface " + targetClassShortName);
    boolean first = true;
    for (String interfaceName : interfaceNames) {
      if (first) {
        print(" extends ");
        first = false;
      } else {
        print(", ");
      }
      print(interfaceName);
    }
  }

  /**
   * Write a group of imports matching or not matching a regex.
   * 
   * @param imports
   * @param regex
   * @param includeMatches true to include imports matching the regex, false to
   *     include only those that don't match
   */
  private void writeImportGroup(Iterable<String> imports, Pattern regex, boolean includeMatches) {
    boolean firstOfGroup = true;
    for (String importEntry : imports) {
      Matcher matcher = regex.matcher(importEntry);
      if (matcher.matches() == includeMatches) {
        if (firstOfGroup) {
          println();
          firstOfGroup = false;
        }
        println("import " + importEntry + ";");
      }
    }
  }
}
