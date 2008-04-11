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
package com.google.gwt.dev.jdt;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.CompilationUnitProvider;

/**
 * Utilities for tests that build a type oracle and watch for errors.
 * 
 */
public class TypeOracleTestingUtils {
  public static void addCup(TypeOracleBuilder builder, String typeName,
      CharSequence code) throws UnableToCompleteException {
    CompilationUnitProvider cup = createCup(typeName, code);
    builder.addCompilationUnit(cup);
  }

  public static CompilationUnitProvider createCup(String typeName,
      CharSequence code) {
    String packageName;
    String className;
    int pos = typeName.lastIndexOf('.');
    if (pos >= 0) {
      packageName = typeName.substring(0, pos);
      className = typeName.substring(pos + 1);
    } else {
      packageName = "";
      className = typeName;
    }
    StaticCompilationUnitProvider cup = new StaticCompilationUnitProvider(
        packageName, className, code.toString().toCharArray());
    return cup;
  }

  /**
   * Add compilation units for basic classes like Object and String.
   */
  public static void addStandardCups(TypeOracleBuilder builder)
      throws UnableToCompleteException {
    {
      StringBuffer code = new StringBuffer();
      code.append("package java.lang;\n");
      code.append("public class Object {\n");
      code.append("  public String toString() { return \"Object\"; }\n");
      code.append("  public Object clone() { return this; } ");
      code.append("}\n");
      addCup(builder, "java.lang.Object", code);
    }
    {
      StringBuffer code = new StringBuffer();
      code.append("package java.lang;\n");
      code.append("public class Class<T> {\n");
      code.append("}\n");
      addCup(builder, "java.lang.Class", code);
    }
    {
      StringBuffer code = new StringBuffer();
      code.append("package java.lang;\n");
      code.append("public final class String {\n");
      code.append("  public int length() { return 0; }\n");
      code.append("}\n");
      addCup(builder, "java.lang.String", code);
    }
    {
      StringBuffer code = new StringBuffer();
      code.append("package java.lang;\n");
      code.append("public interface Serializable { }\n");
      addCup(builder, "java.lang.Serializable", code);
    }
    {
      StringBuffer code = new StringBuffer();
      code.append("package java.util;\n");
      code.append("public interface Map<K,V> { }\n");
      addCup(builder, "java.util.Map", code);
    }
    {
      StringBuffer code = new StringBuffer();
      code.append("package java.lang;\n");
      code.append("public @interface SuppressWarnings {\n");
      code.append("  String[] value();\n");
      code.append("}\n");
      addCup(builder, "java.lang.SuppressWarnings", code);
    }
    {
      StringBuffer code = new StringBuffer();
      code.append("package java.lang.annotation;\n");
      code.append("public interface Annotation {\n");
      code.append("}\n");
      addCup(builder, "java.lang.annotation.Annotation", code);
    }
    {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.core.client;\n");
      code.append("public class JavaScriptObject {\n");
      code.append("  protected JavaScriptObject() { }\n");
      code.append("}\n");
      addCup(builder, "com.google.gwt.core.client.JavaScriptObject", code);
    }
  }

  public static void buildTypeOracleForCode(String typeName, CharSequence code,
      TreeLogger testLogger) throws UnableToCompleteException {
    TypeOracleBuilder builder = new TypeOracleBuilder();
    addStandardCups(builder);
    addCup(builder, typeName, code);
    builder.build(testLogger);
  }
}
