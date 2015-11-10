/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.thirdparty.guava.common.base.Joiner;

/**
 * Test for {@link ImplementJsVarargs}.
 */
public class ImplementJsVarargsTest extends OptimizerTestBase {
  // TODO(rluble): add unit test for the rest of the functionality.

  public void testOptimizedArguments_justPassThru() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType static class A {",
        "  public static void m(Object... obj) { n(obj); }",
        "  public static void n(Object... obj) { }",
        "}");

    Result result = optimize("void", "A.m();");

    assertEquals(
        Joiner.on('\n').join(
            "public static void m(Object[] _arguments_){",
            "  EntryPoint$A.n(_arguments_);",
            "}"), result.findMethod("test.EntryPoint$A.m([Ljava/lang/Object;)V").toSource());
  }

  public void testOptimizedArguments_onlyAccess() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType static class A {",
        "  public static void m(Object... obj) { n(obj[5], obj.length); }",
        "  public static void n(Object arg1, Object arg2) { }",
        "}");

    Result result = optimize("void", "A.m();");

    assertEquals(
        Joiner.on('\n').join(
            "public static void m(Object[] _arguments_){",
            "  EntryPoint$A.n(_arguments_[5], Integer.valueOf(_arguments_.length));",
            "}"), result.findMethod("test.EntryPoint$A.m([Ljava/lang/Object;)V").toSource());
  }

  public void testOptimizedArguments_offsetAccess() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType static class A {",
        "  public static void m(int i, Object... obj) { n(obj[5], obj.length); }",
        "  public static void n(Object arg1, Object arg2) { }",
        "}");

    Result result = optimize("void", "A.m(2);");

    assertEquals(
        Joiner.on('\n').join(
            "public static void m(int i, Object[] _arguments_){",
            "  EntryPoint$A.n(_arguments_[5 + 1], Integer.valueOf(_arguments_.length - 1));",
            "}"), result.findMethod("test.EntryPoint$A.m(I[Ljava/lang/Object;)V").toSource());
  }

  public void testOptimizedArguments_writeToArguments() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType static class A {",
        "  public static void m(Object... obj) { obj[5] = 1; }",
        "}");

    Result result = optimize("void", "A.m(2);");

    assertEquals(
        Joiner.on('\n').join(
            "public static void m(Object[] _arguments_){",
            "  {",
            "    Object[] obj = new Object[][_arguments_.length];",
            "    for (int $i = 0; $i < _arguments_.length; $i++) {",
            "      obj[$i] = _arguments_[$i];",
            "    }",
            "  }",
            "  obj[5] = Integer.valueOf(1);",
            "}"), result.findMethod("test.EntryPoint$A.m([Ljava/lang/Object;)V").toSource());
  }

  public void testOptimizedArguments_postIncrement() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType static class A {",
        "  public static void m(int... obj) { obj[5]++; }",
        "}");

    Result result = optimize("void", "A.m(2);");

    assertEquals(
        Joiner.on('\n').join(
            "public static void m(int[] _arguments_){",
            "  {",
            "    int[] obj = new int[][_arguments_.length];",
            "    for (int $i = 0; $i < _arguments_.length; $i++) {",
            "      obj[$i] = _arguments_[$i];",
            "    }",
            "  }",
            "  obj[5]++;",
            "}"), result.findMethod("test.EntryPoint$A.m([I)V").toSource());
  }

  public void testOptimizedArguments_preDecrement() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType static class A {",
        "  public static void m(int... obj) { --obj[5]; }",
        "}");

    Result result = optimize("void", "A.m(2);");

    assertEquals(
        Joiner.on('\n').join(
            "public static void m(int[] _arguments_){",
            "  {",
            "    int[] obj = new int[][_arguments_.length];",
            "    for (int $i = 0; $i < _arguments_.length; $i++) {",
            "      obj[$i] = _arguments_[$i];",
            "    }",
            "  }",
            "  --obj[5];",
            "}"), result.findMethod("test.EntryPoint$A.m([I)V").toSource());
  }

  public void testOptimizedArguments_call() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType static class A {",
        "  public static void m(int... obj) { n(obj); }",
        "  public static void n(int[] obj) { }",
        "}");

    Result result = optimize("void", "A.m(2);");

    assertEquals(
        Joiner.on('\n').join(
            "public static void m(int[] _arguments_){",
            "  {",
            "    int[] obj = new int[][_arguments_.length];",
            "    for (int $i = 0; $i < _arguments_.length; $i++) {",
            "      obj[$i] = _arguments_[$i];",
            "    }",
            "  }",
            "  EntryPoint$A.n(obj);",
            "}"), result.findMethod("test.EntryPoint$A.m([I)V").toSource());
  }

  @Override
  protected boolean doOptimizeMethod(TreeLogger logger, JProgram program, JMethod method) {
    program.addEntryMethod(findMainMethod(program));
    boolean didChange = true;
    do {
      didChange &= TypeTightener.exec(program).didChange();
      didChange &= MethodCallTightener.exec(program).didChange();
    } while (didChange);
    ImplementJsVarargs.exec(program);
    return true;
  }
}
