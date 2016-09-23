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

package com.google.gwt.dev.jjs.impl;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.thirdparty.guava.common.base.Joiner;
import com.google.gwt.thirdparty.guava.common.collect.Lists;

/**
 * Tests for the {@link Devirtualizer} visitor.
 */
public class DevirtualizerTest extends OptimizerTestBase {

  /**
   * Devirtualizer should allow dual Java/JSO implementations of the same
   * interface, so long as there is only one of each. If there are multiple
   * methods with the same method name, it should distinguish between them.
   */
  public void testDualJsoImpl() throws UnableToCompleteException {

    addSnippetImport("com.google.gwt.lang.Cast");
    addSnippetImport("com.google.gwt.core.client.JavaScriptObject");

    // Defines a bunch of JSO interfaces and classes with functions a() and b().
    addSnippetClassDecl(
        "interface Iface1 { int a(); int b(); }",
        "static class J1 implements Iface1 {",
        "  public int a() { return 1; }",
        "  public int b() { return 1; }",
        "}",
        "static class Jso1 extends JavaScriptObject implements Iface1 {",
        "  protected Jso1() { }",
        "  public final int a() { return 2; }",
        "  public final int b() { return 2; }",
        "  public static native Jso1 create() /*-{ return {} }-*/;",
        "}",
        "static interface Iface2 { int a(); int b(); }",
        "static class J2 implements Iface2 {",
        "  public int a() { return 3; }",
        "  public int b() { return 3; }",
        "}",
        "static class Jso2 extends JavaScriptObject implements Iface2 {",
        "  protected Jso2() { }",
        "  public final int a() { return 4; }",
        "  public final int b() { return 4; }",
        "  public static native Jso2 create() /*-{ return {} }-*/;",
        "}",
        "static Iface1 val1 = new J1();",
        "static Iface1 val2 = Jso1.create();",
        "static Iface2 val3 = new J2();",
        "static Iface2 val4 = Jso2.create();");

    // Constructs a code snippet that calls a() but NOT b().
    String code = "int result = val1.a() + val2.a() + val3.a() + val4.a();";

    // Constructs an expectation about the resulting devirtualized method calls of a(). The salient
    // point in the results below is that the JSO method used for val1 and val1 has a different name
    // the method used for val2 and val3.
    String expected = Joiner.on("").join(
        "int result = ",
        "EntryPoint$Jso1.a__I__devirtual$(EntryPoint.val1) + ",
        "EntryPoint$Jso1.a__I__devirtual$(EntryPoint.val2) + ",
        "EntryPoint$Jso2.a__I__devirtual$(EntryPoint.val3) + ",
        "EntryPoint$Jso2.a__I__devirtual$(EntryPoint.val4);");

    Result result = optimize("void", code);
    // Asserts that a() method calls were redirected to the devirtualized version.
    result.intoString(expected);
    // Asserts that a() AND b() method definitions were both duplicated as devirtualized versions
    // even though b() was never called.
    result.classHasMethods("EntryPoint$Jso1", Lists.newArrayList(
        "a()I", "b()I", "$a(Ltest/EntryPoint$Jso1;)I", "$b(Ltest/EntryPoint$Jso1;)I"));
  }

  public void testDevirtualizeString() throws UnableToCompleteException {

    addSnippetImport("com.google.gwt.lang.Cast");
    addSnippetImport("com.google.gwt.core.client.JavaScriptObject");

    // Defines a JSO and a Java object that implements Comparable.
    addSnippetClassDecl(
        "static class J1 implements Comparable<J1> {",
        "  public int compareTo(J1 other) { return 1; }",
        "}",
        "static class Jso1 extends JavaScriptObject implements Comparable<Jso1> {",
        "  protected Jso1() { }",
        "  final public int compareTo(Jso1 other) { return 2; }",
        "  public static native Jso1 create() /*-{ return {} }-*/;",
        "}",
        "static Comparable javaVal = new J1();",
        "static Comparable jsoVal = Jso1.create();",
        "static Comparable stringVal = \"string\";",
        "static String aString = \"string\";",
        "static CharSequence stringCharSeq = \"string\";");

    // Constructs a code snippet that calls a() but NOT b().
    String code = Joiner.on("").join(
        "int result = javaVal.compareTo(javaVal) + jsoVal.compareTo(jsoVal) +",
        " stringVal.compareTo(stringVal) + stringCharSeq.length() + aString.length();");

    // Constructs an expectation about the resulting devirtualized method calls for
    // Comparable.compareTo() and CharSequence.length(). Note that calls to CharSequence.length and
    // String.length are devirtualized separately.
    String expected = String.format(Joiner.on("").join(
        "int result = ",
        // Methods in Comparable and CharSequence end up in String even if used by a JSO.
        "String.compareTo_%s__I__devirtual$(EntryPoint.javaVal, EntryPoint.javaVal) + ",
        "String.compareTo_%s__I__devirtual$(EntryPoint.jsoVal, EntryPoint.jsoVal) + ",
        "String.compareTo_%s__I__devirtual$(EntryPoint.stringVal, EntryPoint.stringVal) + ",
        "String.length__I__devirtual$(EntryPoint.stringCharSeq) + ",
        "String.length__I__devirtual$(EntryPoint.aString);"), "Ljava_lang_Object",
        "Ljava_lang_Object", "Ljava_lang_Object");

    Result result = optimize("void", code.toString());
    result.intoString(expected.toString());
  }

  public void testDevirtualizeJsOverlay() throws UnableToCompleteException {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsOverlay");
    addSnippetClassDecl(
        "@JsType(isNative=true) public static class NativeClass {",
        "  @JsOverlay public final void m() { };",
        "}");

    String code = Joiner.on('\n').join(
        "NativeClass object = null;",
        "object.m();");

    String expected = Joiner.on('\n').join(
        "EntryPoint$NativeClass object = null;",
        "EntryPoint$NativeClass.$m(object);");
    Result result = optimize("void", code);
    result.intoString(expected);
  }

  public void testDevirtualizeObjectMethods() throws UnableToCompleteException {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsOverlay");
    addSnippetClassDecl(
        "@JsType(isNative=true) public static class NativeClass {",
        "}",
        "public static class NativeClassSubclass extends NativeClass {",
        "}");

    String code = Joiner.on('\n').join(
        "NativeClass nativeClass = null;",
        "nativeClass.toString();",
        "nativeClass.equals(nativeClass);",
        "nativeClass.hashCode();",
        "NativeClassSubclass subclass = null;",
        "subclass.toString();",
        "subclass.equals(subclass);",
        "subclass.hashCode();"
        );

    String expected = Joiner.on('\n').join(
        "EntryPoint$NativeClass nativeClass = null;",
        "Runtime.toString(nativeClass);",
        "Object.equals_Ljava_lang_Object__Z__devirtual$(nativeClass, nativeClass);",
        "Object.hashCode__I__devirtual$(nativeClass);",
        "EntryPoint$NativeClassSubclass subclass = null;",
        "Runtime.toString(subclass);",
        "Object.equals_Ljava_lang_Object__Z__devirtual$(subclass, subclass);",
        "Object.hashCode__I__devirtual$(subclass);");
    Result result = optimize("void", code);
    result.intoString(expected);
  }

  public void testDevirtualizeObjectMethodsExplicitelyDefined() throws UnableToCompleteException {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsOverlay");
    addSnippetClassDecl(
        "@JsType(isNative=true) public static class NativeClass {",
        "  public native String toString();",
        "  public native int hashCode();",
        "  public native boolean equals(Object o);",
        "}",
        "public static class NativeClassSubclass extends NativeClass {",
        "}");

    String code = Joiner.on('\n').join(
        "NativeClass nativeClass = null;",
        "nativeClass.toString();",
        "nativeClass.equals(nativeClass);",
        "nativeClass.hashCode();",
        "NativeClassSubclass subclass = null;",
        "subclass.toString();",
        "subclass.equals(subclass);",
        "subclass.hashCode();"
    );

    String expected = Joiner.on('\n').join(
        "EntryPoint$NativeClass nativeClass = null;",
        "Runtime.toString(nativeClass);",
        "Object.equals_Ljava_lang_Object__Z__devirtual$(nativeClass, nativeClass);",
        "Object.hashCode__I__devirtual$(nativeClass);",
        "EntryPoint$NativeClassSubclass subclass = null;",
        "Runtime.toString(subclass);",
        "Object.equals_Ljava_lang_Object__Z__devirtual$(subclass, subclass);",
        "Object.hashCode__I__devirtual$(subclass);");
    Result result = optimize("void", code);
    result.intoString(expected);
  }

  @Override
  protected boolean doOptimizeMethod(TreeLogger logger, JProgram program, JMethod method) {
    ReplaceCallsToNativeJavaLangObjectOverrides.exec(program);
    Devirtualizer.exec(program);
    return true;
  }
}
