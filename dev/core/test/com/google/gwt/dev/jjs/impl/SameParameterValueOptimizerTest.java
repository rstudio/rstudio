/*
 * Copyright 2009 Google Inc.
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
// Copyright 2009 Google Inc. All Rights Reserved.

package com.google.gwt.dev.jjs.impl;

import com.google.gwt.dev.jjs.JJSOptionsImpl;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JProgram;

/**
 * Test for SameParameterValueOptimizer.
 */
public class SameParameterValueOptimizerTest extends OptimizerTestBase {
  public void testDifferentParameter() throws Exception {
    addSnippetClassDecl("static void foo(int i) { int j = i; }");
    optimizeMethod("foo", "void",
        "foo(1); foo(2);").intoString(
            "int j = i;");
  }

  public void testDifferentParameter_Null() throws Exception {
    addSnippetClassDecl("static void foo(String s) { String j = s; }");
    optimizeMethod("foo", "void",
        "foo(null); foo(\"\");").intoString(
        "String j = s;");
  }

  public void testNonConstParameter() throws Exception {
    addSnippetClassDecl("static int foo(int i) { return i; }");

    optimizeMethod("foo", "void", "foo(foo(1));").intoString(
        "return i;");
  }

  public void testNumericCast() throws Exception {
    addSnippetClassDecl("static void foo(long i) { long j = i; }");
    optimizeMethod("foo", "void",
        "foo(1); foo(1);").intoString(
        "long j = 1L;");
  }

  public void testOneParameterValue() throws Exception {
    addSnippetClassDecl("static void foo(int i) { int j = i; }");
    optimizeMethod("foo", "void",
        "foo(1);").intoString(
        "int j = 1;");
  }

  public void testSameParameter() throws Exception {
    addSnippetClassDecl("static void foo(int i) { int j = i; }");
    optimizeMethod("foo", "void",
        "foo(1); foo(1);").intoString(
        "int j = 1;");
  }

  public void testSameParameter_Null() throws Exception {
    addSnippetClassDecl("static void foo(String s) { String j = s; }");
    optimizeMethod("foo", "void",
        "foo(null); foo(null);").intoString(
        "String j = (String) null;");
  }

  public void testDontKillParameterValue_Binop() throws Exception {
    addSnippetClassDecl("static void foo(int i) { if (i == 2) {} int j = i; }");
    optimizeMethod("foo", "void",
        "foo(1); ").intoString(
            "if (1 == 2) {",
            "}",
            "int j = 1;");
  }

  public void testDontKillParameterValue_LocalPrefix() throws Exception {
    addSnippetClassDecl("static void foo(int i) { int j = i; ++j; }");
    optimizeMethod("foo", "void",
        "foo(1); ").intoString(
            "int j = 1;",
            "++j;");
  }

  public void testDontKillParameterValue_LocalPostfix() throws Exception {
    addSnippetClassDecl("static void foo(int i) { int j = i; j++; }");
    optimizeMethod("foo", "void",
        "foo(1); ").intoString(
            "int j = 1;",
            "j++;");
  }

  public void testKillParameterValue_Prefix() throws Exception {
    addSnippetClassDecl("static void foo(int i) { ++i; int j = i; }");
    optimizeMethod("foo", "void",
        "foo(1); ").intoString(
            "++i;",
            "int j = i;");
  }

  public void testKillParameterValue_Assign() throws Exception {
    addSnippetClassDecl("static void foo(int i) { i = 2; int j = i; }");
    optimizeMethod("foo", "void",
        "foo(1); ").intoString(
            "i = 2;",
            "int j = i;");
  }

  public void testKillParameterValue_Postfix() throws Exception {
    addSnippetClassDecl("static void foo(int i) { i++; int j = i; }");
    optimizeMethod("foo", "void",
    "foo(1); ").intoString(
        "i++;",
        "int j = i;");
  }

  public void testJsniReferenceSaveMethod() throws Exception {
    addSnippetClassDecl(
        "public static native void someStaticMethod() /*-{" +
        "  var foo = @test.EntryPoint::foo(Ljava/lang/String;)" +
        "}-*/");
    addSnippetClassDecl("static void foo(String s) { String p = s; }");

    optimizeMethod(
        "foo",
        "void",
        "foo(\"\"); foo(\"\");").intoString(
        "String p = s;");
  }

  public void testInstanceMethod_Poly() throws Exception {
    addSnippetClassDecl("void foo(int i) { int j = i; }");
    optimizeMethod("foo", "void", "new EntryPoint().foo(1);").intoString(
      "int j = i;");
  }

  public void testInstanceMethod_Final() throws Exception {
    addSnippetClassDecl("final void foo(int i) { int j = i; }");
    optimizeMethod("$foo", "void", "new EntryPoint().foo(1);").intoString(
      "int j = i;");
  }

  public void testOverrides() throws Exception {
    addSnippetClassDecl("void foo(int i) { int j = i; }");
    addSnippetClassDecl(
        "static class Override extends EntryPoint {",
        "  void foo(int i) { int j = i; }", // overrides
        "}");
    optimizeMethod("foo", "void", "new EntryPoint().foo(1);").intoString(
      "int j = i;");
  }

  @Override
  protected boolean optimizeMethod(JProgram program, JMethod method) {
    MakeCallsStatic.exec(new JJSOptionsImpl(), program);
    return SameParameterValueOptimizer.exec(program).didChange();
  }
}
