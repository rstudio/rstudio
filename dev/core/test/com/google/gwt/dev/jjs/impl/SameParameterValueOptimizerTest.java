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
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JProgram;

/**
 * Test for SameParameterValueOptimizer.
 */
public class SameParameterValueOptimizerTest extends OptimizerTestBase {
  private static class OptimizationResult {
    private final JMethod method;

    public OptimizationResult(JMethod method) {
      this.method = method;
    }

    public void into(String... expectedStrings) {
      StringBuffer expected = new StringBuffer();
      for (String s : expectedStrings) {
        if (expected.length() != 0) {
          expected.append("\n");
        }
        expected.append(s);
      }
      assertEquals(expected.toString(), method.toSource());
    }
  }

  public void testDifferentParameter() throws Exception {
    assertOptimize("foo", "static void foo(int i) { int j = i; }",
        "foo(1); foo(2);").into("public static void foo(int i){",
        "  int j = i;", "}");
  }

  public void testNonConstParameter() throws Exception {
    assertOptimize("foo", "static int foo(int i) { return i; }", "foo(foo(1));").into(
        "public static int foo(int i){", "  return i;", "}");
  }

  public void testNumericCast() throws Exception {
    assertOptimize("foo", "static void foo(long i) { long j = i; }",
        "foo(1); foo(1);").into("public static void foo(long i){",
        "  long j = 1L;", "}");
  }

  public void testSameParameter() throws Exception {
    assertOptimize("foo", "static void foo(int i) { int j = i; }",
        "foo(1); foo(1);").into("public static void foo(int i){",
        "  int j = 1;", "}");
  }

  public void testJsniReferenceSaveMethod() throws Exception {
    addSnippetClassDecl(
        "public static native void someStaticMethod() /*-{" +
        "  var foo = @test.EntryPoint::foo(Ljava/lang/String;)" +
        "}-*/");
    
    assertOptimize(
        "foo", 
        "static void foo(String s) { String p = s; }",
        "foo(\"\"); foo(\"\");").into(
        "public static void foo(String s){",
        "  String p = s;", 
        "}");
  }

  private OptimizationResult assertOptimize(String methodName,
      String methodDecl, String codeSnippet) throws UnableToCompleteException {
    addSnippetClassDecl(methodDecl);
    JProgram program = compileSnippet("void", codeSnippet);
    SameParameterValueOptimizer.exec(program);
    JMethod method = findMethod(program, methodName);
    return new OptimizationResult(method);
  }
}
