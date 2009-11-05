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

/**
 * Contains standard Java source files for testing.
 */
public class JavaResourceBase {

  public static final MockJavaResource ANNOTATION = new MockJavaResource(
      "java.lang.annotation.Annotation") {
    @Override
    protected CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package java.lang.annotation;\n");
      code.append("public interface Annotation {\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource BAR = new MockJavaResource("test.Bar") {
    @Override
    protected CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package test;\n");
      code.append("public class Bar extends Foo {\n");
      code.append("  public String value() { return \"Bar\"; }\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource CLASS = new MockJavaResource(
      "java.lang.Class") {
    @Override
    protected CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package java.lang;\n");
      code.append("public class Class<T> {\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource COLLECTION = new MockJavaResource(
      "java.util.Collection") {
    @Override
    protected CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package java.util;\n");
      code.append("public interface Collection<E> {\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource ENUM = new MockJavaResource(
      "java.lang.Enum") {
    @Override
    protected CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package java.lang;\n");
      code.append("import java.io.Serializable;\n");
      code.append("public abstract class Enum<E extends Enum<E>> implements Serializable {\n");
      code.append("  protected Enum(String name, int ordinal) {}\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource EXCEPTION = new MockJavaResource(
      "java.lang.Exception") {
    @Override
    protected CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package java.lang;\n");
      code.append("public class Exception extends Throwable {\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource FOO = new MockJavaResource("test.Foo") {
    @Override
    protected CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package test;\n");
      code.append("public class Foo {\n");
      code.append("  public String value() { return \"Foo\"; }\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource IS_SERIALIZABLE = new MockJavaResource(
      "com.google.gwt.user.client.rpc.IsSerializable") {
    @Override
    protected CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.user.client.rpc;\n");
      code.append("public interface IsSerializable {\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource JAVASCRIPTOBJECT = new MockJavaResource(
      "com.google.gwt.core.client.JavaScriptObject") {
    @Override
    protected CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.core.client;\n");
      code.append("public class JavaScriptObject {\n");
      code.append("  protected JavaScriptObject() { }\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource MAP = new MockJavaResource(
      "java.util.Map") {
    @Override
    protected CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package java.util;\n");
      code.append("public interface Map<K,V> { }\n");
      return code;
    }
  };
  public static final MockJavaResource OBJECT = new MockJavaResource(
      "java.lang.Object") {
    @Override
    protected CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package java.lang;\n");
      code.append("public class Object {\n");
      code.append("  public String toString() { return \"Object\"; }\n");
      code.append("  public Object clone() { return this; } ");
      code.append("  public Class<?> getClass() { return Object.class; } ");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource SERIALIZABLE = new MockJavaResource(
      "java.io.Serializable") {
    @Override
    protected CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package java.io;\n");
      code.append("public interface Serializable { }\n");
      return code;
    }
  };
  public static final MockJavaResource STRING = new MockJavaResource(
      "java.lang.String") {
    @Override
    protected CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package java.lang;\n");
      code.append("import java.io.Serializable;\n");
      code.append("public final class String implements Serializable {\n");
      code.append("  private static final long serialVersionUID = 0L;\n");
      code.append("  public int length() { return 0; }\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource SUPPRESS_WARNINGS = new MockJavaResource(
      "java.lang.SuppressWarnings") {
    @Override
    protected CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package java.lang;\n");
      code.append("public @interface SuppressWarnings {\n");
      code.append("  String[] value();\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource THROWABLE = new MockJavaResource(
      "java.lang.Throwable") {
    @Override
    protected CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package java.lang;\n");
      code.append("public class Throwable {\n");
      code.append("}\n");
      return code;
    }
  };

  public static MockJavaResource[] getStandardResources() {
    return new MockJavaResource[] {
        ANNOTATION, CLASS, COLLECTION, ENUM, EXCEPTION, IS_SERIALIZABLE,
        JAVASCRIPTOBJECT, MAP, OBJECT, SERIALIZABLE, STRING, SUPPRESS_WARNINGS,
        THROWABLE};
  }
}
