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
package com.google.gwt.dev.javac.testing.impl;

/**
 * Contains standard Java source files for testing.
 */
public class JavaResourceBase {

  public static final MockJavaResource ANNOTATION = new MockJavaResource(
      "java.lang.annotation.Annotation") {
    @Override
    public CharSequence getContent() {
      StringBuilder code = new StringBuilder();
      code.append("package java.lang.annotation;\n");
      code.append("public interface Annotation {\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource BAR = new MockJavaResource("test.Bar") {
    @Override
    public CharSequence getContent() {
      StringBuilder code = new StringBuilder();
      code.append("package test;\n");
      code.append("public class Bar extends Foo {\n");
      code.append("  public String value() { return \"Bar\"; }\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource BOOLEAN = new MockJavaResource("java.lang.Boolean") {
    @Override
    public CharSequence getContent() {
      StringBuilder code = new StringBuilder();
      code.append("package java.lang;\n");
      code.append("public class Boolean {\n");
      code.append("  private boolean value;\n");
      code.append("  public Boolean(boolean value) {\n");
      code.append("    this.value = value;\n");
      code.append("  }\n");
      code.append("  public static Boolean valueOf(boolean b) { return new Boolean(b); }\n");
      code.append("  public boolean booleanValue() { return value; }\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource BYTE = new MockJavaResource("java.lang.Byte") {
    @Override
    public CharSequence getContent() {
      StringBuilder code = new StringBuilder();
      code.append("package java.lang;\n");
      code.append("public class Byte extends Number {\n");
      code.append("  private byte value;\n");
      code.append("  public Byte(byte value) {\n");
      code.append("    this.value = value;\n");
      code.append("  }\n");
      code.append("  public static Byte valueOf(byte b) { return new Byte(b); }\n");
      code.append("  public byte byteValue() { return value; }\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource CHARACTER = new MockJavaResource("java.lang.Character") {
    @Override
    public CharSequence getContent() {
      StringBuilder code = new StringBuilder();
      code.append("package java.lang;\n");
      code.append("public class Character {\n");
      code.append("  private char value;\n");
      code.append("  public Character(char value) {\n");
      code.append("    this.value = value;\n");
      code.append("  }\n");
      code.append("  public static Character valueOf(char c) { return new Character(c); }\n");
      code.append("  public char charValue() { return value; }\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource CLASS = new MockJavaResource("java.lang.Class") {
    @Override
    public CharSequence getContent() {
      StringBuilder code = new StringBuilder();
      code.append("package java.lang;\n");
      code.append("public class Class<T> {\n");
      code.append("  public String getName() { return null; }\n");
      code.append("  public String getSimpleName() { return null; }\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource CLASS_NOT_FOUND_EXCEPTION = new MockJavaResource(
      "java.lang.ClassNotFoundException") {
    @Override
    public CharSequence getContent() {
      StringBuilder code = new StringBuilder();
      code.append("package java.lang;\n");
      code.append("public class ClassNotFoundException extends Exception {\n");
      code.append("  public ClassNotFoundException() {}\n");
      code.append("  public ClassNotFoundException(String msg) {}\n");
      code.append("  public ClassNotFoundException(String msg, Throwable t) {}\n");
      code.append("  public Throwable getCause() { return null; }\n");
      code.append("  public Throwable getException() { return null; }\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource COLLECTION = new MockJavaResource("java.util.Collection") {
    @Override
    public CharSequence getContent() {
      StringBuilder code = new StringBuilder();
      code.append("package java.util;\n");
      code.append("public interface Collection<E> {\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource DOUBLE = new MockJavaResource("java.lang.Double") {
    @Override
    public CharSequence getContent() {
      StringBuilder code = new StringBuilder();
      code.append("package java.lang;\n");
      code.append("public class Double extends Number {\n");
      code.append("  private double value;\n");
      code.append("  public Double(double value) {\n");
      code.append("    this.value = value;\n");
      code.append("  }\n");
      code.append("  public static boolean isNaN(double d) { return false; }\n");
      code.append("  public static Double valueOf(double d) { return new Double(d); }\n");
      code.append("  public double doubleValue() { return value; }\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource ENUM = new MockJavaResource("java.lang.Enum") {
    @Override
    public CharSequence getContent() {
      StringBuilder code = new StringBuilder();
      code.append("package java.lang;\n");
      code.append("import java.io.Serializable;\n");
      code.append("public abstract class Enum<E extends Enum<E>> implements Serializable {\n");
      code.append("  protected Enum(String name, int ordinal) {}\n");
      code.append("  protected static Object createValueOfMap(Enum[] constants) { return null; }\n");
      code.append("  protected static Enum valueOf(Object map, String name) { return null; }\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource ERROR = new MockJavaResource("java.lang.Error") {
    @Override
    public CharSequence getContent() {
      StringBuilder code = new StringBuilder();
      code.append("package java.lang;\n");
      code.append("public class Error extends Throwable {\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource EXCEPTION = new MockJavaResource("java.lang.Exception") {
    @Override
    public CharSequence getContent() {
      StringBuilder code = new StringBuilder();
      code.append("package java.lang;\n");
      code.append("public class Exception extends Throwable {\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource FLOAT = new MockJavaResource("java.lang.Float") {
    @Override
    public CharSequence getContent() {
      StringBuilder code = new StringBuilder();
      code.append("package java.lang;\n");
      code.append("public class Float extends Number {\n");
      code.append("  private float value;\n");
      code.append("  public Float(float value) {\n");
      code.append("    this.value = value;\n");
      code.append("  }\n");
      code.append("  public static Float valueOf(float f) { return new Float(f); }\n");
      code.append("  public float floatValue() { return value; }\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource FOO = new MockJavaResource("test.Foo") {
    @Override
    public CharSequence getContent() {
      StringBuilder code = new StringBuilder();
      code.append("package test;\n");
      code.append("public class Foo {\n");
      code.append("  public String value() { return \"Foo\"; }\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource INTEGER = new MockJavaResource("java.lang.Integer") {
    @Override
    public CharSequence getContent() {
      StringBuilder code = new StringBuilder();
      code.append("package java.lang;\n");
      code.append("public class Integer extends Number {\n");
      code.append("  private int value;\n");
      code.append("  public Integer(int value) {\n");
      code.append("    this.value = value;\n");
      code.append("  }\n");
      code.append("  public static Integer valueOf(int i) { return new Integer(i); }\n");
      code.append("  public int intValue() { return value; }\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource IS_SERIALIZABLE = new MockJavaResource(
      "com.google.gwt.user.client.rpc.IsSerializable") {
    @Override
    public CharSequence getContent() {
      StringBuilder code = new StringBuilder();
      code.append("package com.google.gwt.user.client.rpc;\n");
      code.append("public interface IsSerializable {\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource JAVASCRIPTOBJECT = new MockJavaResource(
      "com.google.gwt.core.client.JavaScriptObject") {
    @Override
    public CharSequence getContent() {
      StringBuilder code = new StringBuilder();
      code.append("package com.google.gwt.core.client;\n");
      code.append("public class JavaScriptObject {\n");
      code.append("  public static native JavaScriptObject createObject() /*-{ return {}; }-*/;\n");
      code.append("  protected JavaScriptObject() { }\n");
      code.append("  public final String toString() { return \"JavaScriptObject\"; }\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource LONG = new MockJavaResource("java.lang.Long") {
    @Override
    public CharSequence getContent() {
      StringBuilder code = new StringBuilder();
      code.append("package java.lang;\n");
      code.append("public class Long extends Number {\n");
      code.append("  private long value;\n");
      code.append("  public Long(long value) {\n");
      code.append("    this.value = value;\n");
      code.append("  }\n");
      code.append("  public static Long valueOf(long l) { return new Long(l); }\n");
      code.append("  public long longValue() { return value; }\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource MAP = new MockJavaResource("java.util.Map") {
    @Override
    public CharSequence getContent() {
      StringBuilder code = new StringBuilder();
      code.append("package java.util;\n");
      code.append("public interface Map<K,V> { }\n");
      return code;
    }
  };
  public static final MockJavaResource NO_CLASS_DEF_FOUND_ERROR = new MockJavaResource(
      "java.lang.NoClassDefFoundError") {
    @Override
    public CharSequence getContent() {
      StringBuilder code = new StringBuilder();
      code.append("package java.lang;\n");
      code.append("public class NoClassDefFoundError extends Error {\n");
      code.append("  public NoClassDefFoundError() {}\n");
      code.append("  public NoClassDefFoundError(String msg) {}\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource NUMBER = new MockJavaResource("java.lang.Number") {
    @Override
    public CharSequence getContent() {
      StringBuilder code = new StringBuilder();
      code.append("package java.lang;\n");
      code.append("public class Number implements java.io.Serializable {\n");
      code.append("}\n");
      return code;
    }
  };

  public static final MockJavaResource OBJECT = new MockJavaResource("java.lang.Object") {
    @Override
    public CharSequence getContent() {
      StringBuilder code = new StringBuilder();
      code.append("package java.lang;\n");
      code.append("public class Object {\n");
      code.append("  private Class<?> ___clazz;");
      code.append("  public boolean equals(Object that){return this == that;}");
      code.append("  public int hashCode() { return 0; }\n");
      code.append("  public String toString() { return \"Object\"; }\n");
      code.append("  public Object clone() { return this; } ");
      code.append("  public Class<?> getClass() { return ___clazz; } ");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource SERIALIZABLE = new MockJavaResource("java.io.Serializable") {
    @Override
    public CharSequence getContent() {
      StringBuilder code = new StringBuilder();
      code.append("package java.io;\n");
      code.append("public interface Serializable { }\n");
      return code;
    }
  };
  public static final MockJavaResource SHORT = new MockJavaResource("java.lang.Short") {
    @Override
    public CharSequence getContent() {
      StringBuilder code = new StringBuilder();
      code.append("package java.lang;\n");
      code.append("public class Short extends Number {\n");
      code.append("  private short value;\n");
      code.append("  public Short(short value) {\n");
      code.append("    this.value = value;\n");
      code.append("  }\n");
      code.append("  public static Short valueOf(short s) { return new Short(s); }\n");
      code.append("  public short shortValue() { return value; }\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource STRING = new MockJavaResource("java.lang.String") {
    @Override
    public CharSequence getContent() {
      StringBuilder code = new StringBuilder();
      code.append("package java.lang;\n");
      code.append("import java.io.Serializable;\n");
      code.append("public final class String implements Serializable {\n");
      code.append("  public String() { }\n");
      code.append("  public String(char c) { }\n");
      code.append("  public String(String s) { }\n");
      code.append("  public static String _String() { return \"\"; }\n");
      code.append("  public static String _String(char c) { return \"\" + c; }\n");
      code.append("  public static String _String(String s) { return s; }\n");
      code.append("  private static final long serialVersionUID = 0L;\n");
      code.append("  public char charAt(int index) { return 'a'; }\n");
      code.append("  public boolean equals(Object obj) { return false; }\n");
      code.append("  public boolean equalsIgnoreCase(String str) { return false; }\n");
      code.append("  public int length() { return 0; }\n");
      code.append("  public static String valueOf(int i) { return \"\" + i; }\n");
      code.append("  public static String valueOf(char c) { return \"\" + c; }\n");
      code.append("  public static String valueOf(long l) { return \"\" + l; }\n");
      code.append("  public int hashCode() { return 0; }\n");
      code.append("  public String replace(char c1, char c2) { return null; }\n");
      code.append("  public boolean startsWith(String str) { return false; }\n");
      code.append("  public String toLowerCase() { return null; }\n");
      code.append("  public static String valueOf(boolean b) { return null; }\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource STRING_BUILDER = new MockJavaResource(
      "java.lang.StringBuilder") {
    @Override
    public CharSequence getContent() {
      StringBuilder code = new StringBuilder();
      code.append("package java.lang;\n");
      code.append("public final class StringBuilder {\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource SUPPRESS_WARNINGS = new MockJavaResource(
      "java.lang.SuppressWarnings") {
    @Override
    public CharSequence getContent() {
      StringBuilder code = new StringBuilder();
      code.append("package java.lang;\n");
      code.append("public @interface SuppressWarnings {\n");
      code.append("  String[] value();\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource THROWABLE = new MockJavaResource("java.lang.Throwable") {
    @Override
    public CharSequence getContent() {
      StringBuilder code = new StringBuilder();
      code.append("package java.lang;\n");
      code.append("public class Throwable {\n");
      code.append("  public String getMessage() { return \"\"; }\n");
      code.append("  public Throwable getCause() { return null; }\n");
      code.append("}\n");
      return code;
    }
  };

  public static MockJavaResource[] getStandardResources() {
    return new MockJavaResource[]{
        ANNOTATION, BYTE, BOOLEAN, CHARACTER, CLASS, CLASS_NOT_FOUND_EXCEPTION, COLLECTION, DOUBLE,
        ENUM, EXCEPTION, ERROR, FLOAT, INTEGER, IS_SERIALIZABLE, JAVASCRIPTOBJECT, LONG, MAP,
        NO_CLASS_DEF_FOUND_ERROR, NUMBER, OBJECT, SERIALIZABLE, SHORT, STRING, STRING_BUILDER,
        SUPPRESS_WARNINGS, THROWABLE};
  }
}
