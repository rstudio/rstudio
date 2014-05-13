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
  public static final MockJavaResource AUTOCLOSEABLE =
      createMockJavaResource("java.lang.AutoCloseable",
          "package java.lang;",
          "import java.lang.Exception;",
          "public interface AutoCloseable {",
          "  void close() throws Exception; ",
          "}");

  public static final MockJavaResource ANNOTATION =
      createMockJavaResource("java.lang.annotation.Annotation",
          "package java.lang.annotation;",
          "public interface Annotation {",
          "}");

  public static final MockJavaResource BAR =
      createMockJavaResource("test.Bar",
          "package test;",
          "public class Bar extends Foo {",
          "  public String value() { return \"Bar\"; }",
          "}");

  public static final MockJavaResource BOOLEAN =
      createMockJavaResource("java.lang.Boolean",
          "package java.lang;",
          "public class Boolean {",
          "  private boolean value;",
          "  public Boolean(boolean value) {",
          "    this.value = value;",
          "  }",
          "  public static Boolean valueOf(boolean b) { return new Boolean(b); }",
          "  public boolean booleanValue() { return value; }",
          "}");

  public static final MockJavaResource BYTE =
      createMockJavaResource("java.lang.Byte",
          "package java.lang;",
          "public class Byte extends Number {",
          "  private byte value;",
          "  public Byte(byte value) {",
          "    this.value = value;",
          "  }",
          "  public static Byte valueOf(byte b) { return new Byte(b); }",
          "  public byte byteValue() { return value; }",
          "}\n");

  public static final MockJavaResource CHARACTER =
      createMockJavaResource("java.lang.Character",
          "package java.lang;",
          "public class Character {",
          "  private char value;",
          "  public Character(char value) {",
          "    this.value = value;",
          "  }",
          "  public static Character valueOf(char c) { return new Character(c); }",
          "  public char charValue() { return value; }",
          "}");

  public static final MockJavaResource CHAR_SEQUENCE =
      createMockJavaResource("java.lang.CharSequence",
          "package java.lang;",
          "public interface CharSequence {",
          "  char charAt(int index);",
          "  int length();",
          "  String toString();",
          "}");

  public static final MockJavaResource CLASS =
      createMockJavaResource("java.lang.Class",
          "package java.lang;",
          "public class Class<T> {",
          "  public String getName() { return null; }",
          "  public String getSimpleName() { return null; }",
          "}");

  public static final MockJavaResource CLASS_NOT_FOUND_EXCEPTION =
      createMockJavaResource("java.lang.ClassNotFoundException",
          "package java.lang;",
          "public class ClassNotFoundException extends Exception {",
          "  public ClassNotFoundException() {}",
          "  public ClassNotFoundException(String msg) {}",
          "  public ClassNotFoundException(String msg, Throwable t) {}",
          "  public Throwable getCause() { return null; }",
          "  public Throwable getException() { return null; }",
          "}");

  public static final MockJavaResource COLLECTION =
      createMockJavaResource("java.util.Collection",
          "package java.util;",
          "public interface Collection<E> {",
          "}");

  public static final MockJavaResource COMPARABLE =
      createMockJavaResource("java.lang.Comparable",
          "package java.lang;",
          "public interface Comparable<T> {",
          "  public int compareTo(T other);",
          "}");

  public static final MockJavaResource DOUBLE =
      createMockJavaResource("java.lang.Double",
          "package java.lang;",
          "public class Double extends Number {",
          "  private double value;",
          "  public Double(double value) {",
          "    this.value = value;",
          "  }",
          "  public static boolean isNaN(double d) { return false; }",
          "  public static Double valueOf(double d) { return new Double(d); }",
          "  public double doubleValue() { return value; }",
          "}");

  public static final MockJavaResource ENUM =
      createMockJavaResource("java.lang.Enum",
          "package java.lang;",
          "import java.io.Serializable;",
          "public abstract class Enum<E extends Enum<E>> implements Serializable {",
          "  protected Enum(String name, int ordinal) {}",
          "  protected static Object createValueOfMap(Enum[] constants) { return null; }",
          "  protected static Enum valueOf(Object map, String name) { return null; }",
          "}");

  public static final MockJavaResource ERROR =
      createMockJavaResource("java.lang.Error",
          "package java.lang;",
          "public class Error extends Throwable {",
          "}");

  public static final MockJavaResource EXCEPTION =
      createMockJavaResource("java.lang.Exception",
          "package java.lang;",
          "public class Exception extends Throwable {",
          "}");

  public static final MockJavaResource FLOAT =
      createMockJavaResource("java.lang.Float",
          "package java.lang;",
          "public class Float extends Number {",
          "  private float value;",
          "  public Float(float value) {",
          "    this.value = value;",
          "  }",
          "  public static Float valueOf(float f) { return new Float(f); }",
          "  public float floatValue() { return value; }",
          "}");

  public static final MockJavaResource FOO =
      createMockJavaResource("test.Foo",
          "package test;",
          "public class Foo {",
          "  public String value() { return \"Foo\"; }",
          "}");

  public static final MockJavaResource INTEGER =
      createMockJavaResource("java.lang.Integer",
          "package java.lang;",
          "public class Integer extends Number {",
          "  private int value;",
          "  public Integer(int value) {",
          "    this.value = value;",
          "  }",
          "  public static Integer valueOf(int i) { return new Integer(i); }",
          "  public int intValue() { return value; }",
          "}");

  public static final MockJavaResource IS_SERIALIZABLE =
      createMockJavaResource("com.google.gwt.user.client.rpc.IsSerializable",
          "package com.google.gwt.user.client.rpc;",
          "public interface IsSerializable {",
          "}");

  public static final MockJavaResource JAVASCRIPTOBJECT =
      createMockJavaResource("com.google.gwt.core.client.JavaScriptObject",
          "package com.google.gwt.core.client;",
          "public class JavaScriptObject {",
          "  public static Object createArray() {return null;}",
          "  public static native JavaScriptObject createObject() /*-{ return {}; }-*/;",
          "  protected JavaScriptObject() { }",
          "  public final String toString() { return \"JavaScriptObject\"; }",
          "}");

  public static final MockJavaResource LIST =
      createMockJavaResource("java.util.List",
          "package java.util;",
          "public interface List<T> extends Collection<T> {",
          "  public T get(int i);",
          "}");

  public static final MockJavaResource ARRAY_LIST =
      createMockJavaResource("java.util.ArrayList",
          "package java.util;",
          "public class ArrayList<T> implements List<T> {",
          "  public T get(int i) { return null; }",
          "}");

  public static final MockJavaResource LONG =
      createMockJavaResource("java.lang.Long",
          "package java.lang;",
          "public class Long extends Number {",
          "  private long value;",
          "  public Long(long value) {",
          "    this.value = value;",
          "  }",
          "  public static Long valueOf(long l) { return new Long(l); }",
          "  public long longValue() { return value; }",
          "}");

  public static final MockJavaResource MAP =
      createMockJavaResource("java.util.Map",
          "package java.util;",
          "public interface Map<K,V> { }");

  public static final MockJavaResource NO_CLASS_DEF_FOUND_ERROR =
      createMockJavaResource("java.lang.NoClassDefFoundError",
          "package java.lang;",
          "public class NoClassDefFoundError extends Error {",
          "  public NoClassDefFoundError() {}",
          "  public NoClassDefFoundError(String msg) {}",
          "}");

  public static final MockJavaResource NUMBER =
      createMockJavaResource("java.lang.Number",
          "package java.lang;",
          "public class Number implements java.io.Serializable {",
          "}");

  public static final MockJavaResource OBJECT =
      createMockJavaResource("java.lang.Object",
          "package java.lang;",
          "public class Object {",
          "  private Class<?> ___clazz;",
          "  public Object castableTypeMap = null;",
          "  public Object typeMarker = null;",
          "  public boolean equals(Object that){return this == that;}",
          "  public int hashCode() { return 0; }",
          "  public String toString() { return \"Object\"; }",
          "  public Class<?> getClass() { return ___clazz; }",
          "}");

  public static final MockJavaResource RUNTIME_EXCEPTION =
      createMockJavaResource("java.lang.RuntimeException",
          "package java.lang;",
          "public class RuntimeException extends Exception {",
          "  public RuntimeException() {}",
          "  public RuntimeException(String message) {}",
          "}");

  public static final MockJavaResource SERIALIZABLE =
      createMockJavaResource("java.io.Serializable",
          "package java.io;",
          "public interface Serializable { }");

  public static final MockJavaResource SHORT =
      createMockJavaResource("java.lang.Short",
          "package java.lang;",
          "public class Short extends Number {",
          "  private short value;",
          "  public Short(short value) {",
          "    this.value = value;",
          "  }",
          "  public static Short valueOf(short s) { return new Short(s); }",
          "  public short shortValue() { return value; }",
          "}");

  public static final MockJavaResource STRING =
      createMockJavaResource("java.lang.String",
          "package java.lang;",
          "import java.io.Serializable;",
          "public final class String implements Comparable<String>, CharSequence, Serializable {",
          "  public String() { }",
          "  public String(char c) { }",
          "  public String(String s) { }",
          "  public static String _String() { return \"\"; }",
          "  public static String _String(char c) { return \"\" + c; }",
          "  public static String _String(String s) { return s; }",
          "  public char charAt(int index) { return 'a'; }",
          "  public int compareTo(String other) { return -1; }",
          "  public boolean equals(Object obj) { return false; }",
          "  public boolean equalsIgnoreCase(String str) { return false; }",
          "  public int length() { return 0; }",
          "  public static String valueOf(int i) { return \"\" + i; }",
          "  public static String valueOf(char c) { return \"\" + c; }",
          "  public int hashCode() { return 0; }",
          "  public String replace(char c1, char c2) { return null; }",
          "  public boolean startsWith(String str) { return false; }",
          "  public String toLowerCase() { return null; }",
          "  public String toString() { return this; }",
          "  public static String valueOf(boolean b) { return null; }",
          "}");

  public static final MockJavaResource STRING_BUILDER =
      createMockJavaResource("java.lang.StringBuilder",
          "package java.lang;",
          "public final class StringBuilder {",
          "}");

  public static final MockJavaResource SUPPRESS_WARNINGS =
      createMockJavaResource("java.lang.SuppressWarnings",
          "package java.lang;",
          "public @interface SuppressWarnings {",
          "  String[] value();",
          "}");

  public static final MockJavaResource THROWABLE =
      createMockJavaResource("java.lang.Throwable",
          "package java.lang;",
          "public class Throwable {",
          "  public String getMessage() { return \"\"; }",
          "  public Throwable getCause() { return null; }",
          "  public void addSuppressed(Throwable ex) { }",
          "}");

  // TODO: move JS* annotations to intrinsic mock resource base
  public static final MockJavaResource JSTYPE =
      createMockJavaResource("com.google.gwt.core.client.js.JsType",
          "package com.google.gwt.core.client.js;",
          "public @interface JsType {\n",
          "  String prototype() default \"\";\n" +
              "  boolean isNative() default false;\n",
          "}"
      );
  public static final MockJavaResource JSTYPEPROTOTYPE =
      createMockJavaResource("com.google.gwt.core.client.js.impl.PrototypeOfJsType",
          "package com.google.gwt.core.client.js.impl;",
          "public @interface PrototypeOfJsType {\n",
          "}");
  public static final MockJavaResource JSEXPORT =
      createMockJavaResource("com.google.gwt.core.client.js.JsExport",
          "package com.google.gwt.core.client.js;",
          "public @interface JsExport {\n",
          "  String value() default \"\";\n",
          "}");
  public static final MockJavaResource JSPROPERTY =
      createMockJavaResource("com.google.gwt.core.client.js.JsProperty",
          "package com.google.gwt.core.client.js;",
          "public @interface JsProperty {\n",
          "  String value() default \"\";\n",
          "}");

  public static MockJavaResource[] getStandardResources() {
    return new MockJavaResource[]{
        AUTOCLOSEABLE, ANNOTATION, ARRAY_LIST, BYTE, BOOLEAN, CHARACTER, CHAR_SEQUENCE, CLASS,
        CLASS_NOT_FOUND_EXCEPTION, COLLECTION, COMPARABLE, DOUBLE, ENUM, EXCEPTION, ERROR, FLOAT,
        INTEGER, IS_SERIALIZABLE, JAVASCRIPTOBJECT, LIST, LONG, MAP, NO_CLASS_DEF_FOUND_ERROR, NUMBER,
        OBJECT, RUNTIME_EXCEPTION, SERIALIZABLE, SHORT, STRING, STRING_BUILDER, SUPPRESS_WARNINGS, THROWABLE,
        JSTYPE, JSTYPEPROTOTYPE, JSEXPORT, JSPROPERTY};
  }

  /**
   * Creates a new MockJavaResource.
   */
  public static MockJavaResource createMockJavaResource(String resourceName,
      final String... lines) {
    return new MockJavaResource(resourceName) {
      @Override
      public CharSequence getContent() {
        StringBuilder code = new StringBuilder();
        for (String line : lines) {
          code.append(line + "\n");
        }
        return code;
      }
    };
  }
}
