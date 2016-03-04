/*
 * Copyright 2014 Google Inc.
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

/**
 * Test for {@link MethodInliner}.
 */
public class MethodInlinerTest extends OptimizerTestBase {
  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  public void testNoMethodCall() throws Exception {
    addSnippetClassDecl("static int fun1(int a) { return a; }");
    addSnippetClassDecl("static int fun2(int a, int b) { return a + b; }");
    Result result = optimize("void", "");
    assertEquals("static int fun1(int a){ return a; }",
        getCanonicalSource(result.findMethod("fun1")));
    assertEquals("static int fun2(int a, int b){ return a + b; }",
        getCanonicalSource(result.findMethod("fun2")));
  }

  public void testLocalVariables() throws Exception {
    addSnippetImport("javaemul.internal.annotations.DoNotInline");
    addSnippetClassDecl("static int fun() { int a = 1; return a; }");
    addSnippetClassDecl("@DoNotInline static int caller() { return fun(); }");
    Result result = optimize("int", "return caller();");
    assertEquals("static int caller(){ return (a = 1, a); }",
        getCanonicalSource(result.findMethod("caller")));
  }

  public void testLocalVariables_unassignedAtDefinition() throws Exception {
    addSnippetImport("javaemul.internal.annotations.DoNotInline");
    addSnippetClassDecl("static int fun() { int a; return a = 1; }");
    addSnippetClassDecl("@DoNotInline static int caller() { return fun(); }");
    Result result = optimize("int", "return caller();");
    assertEquals("static int caller(){ return a = 1; }",
        getCanonicalSource(result.findMethod("caller")));
  }

  public void testLocalVariablesUnusedReturn() throws Exception {
    addSnippetImport("javaemul.internal.annotations.DoNotInline");
    addSnippetClassDecl("static int fun() { int a = 1; return a; }");
    addSnippetClassDecl("@DoNotInline static int caller() { fun(); return 1; }");
    Result result = optimize("int", "return caller();");
    assertEquals("static int caller(){ a = 1; return 1; }",
        getCanonicalSource(result.findMethod("caller")));
  }

  private static String getCanonicalSource(JMethod method) {
    return method.toSource().replaceAll("\\s+", " ").trim();
  }

  public void testSimple() throws Exception {
    addSnippetClassDecl("static int fun1(int a) { return a; }");
    addSnippetClassDecl("static int fun2(int a, int b) { return a + b; }");
    addSnippetClassDecl("static int fun3(int a) { return 1 + fun1(a); }");
    addSnippetClassDecl("static int fun4() {", "  int a = 1;", "  int b = 2;",
        "  return fun1(a) + fun2(a, b); }");
    Result result = optimize("int", "return fun3(1) + fun4();");

    // one method call in caller
    assertEquals("static int fun3(int a){ return 1 + a; }",
        getCanonicalSource(result.findMethod("fun3")));

    // two method calls in caller
    assertEquals(
        "static int fun4(){ int a = 1; int b = 2; return a + a + b; }",
        getCanonicalSource(result.findMethod("fun4")));
  }

  public void testLargerMethodBody() throws Exception {
    addSnippetClassDecl("static void fun0(int a) { int b = a; b++; assert(b>0); }");
    addSnippetClassDecl("static int fun1(int a) { fun0(a); return 2 + a; }");
    addSnippetClassDecl("static int fun2(int a) { return 1 + fun1(a); }");
    addSnippetClassDecl("static int fun3() {", "  int a = 1;", "  int b = 2;", "  a = a + fun1(a);",
        "  return a + fun1(b); }");
    Result result = optimize("int", "return fun2(1);");
    assertEquals(
        "static int fun1(int a){ EntryPoint.fun0(a); return 2 + a; }",
        getCanonicalSource(result.findMethod("fun1")));
    // one method call in caller
    assertEquals("static int fun2(int a){ return (EntryPoint.fun0(a), 1 + 2 + a); }",
        getCanonicalSource(result.findMethod("fun2")));

    // two method calls in caller
    assertEquals("static int fun3(){ int a = 1; int b = 2;"
        + " a = a + ((EntryPoint.fun0(a), 2 + a));"
        + " return a + ((EntryPoint.fun0(b), 2 + b)); }",
        getCanonicalSource(result.findMethod("fun3")));
  }

  public void testMoreCallSequences() throws Exception {
    addSnippetClassDecl("static int fun1(int a) { return a; }");
    addSnippetClassDecl("static int fun2(int a) { return fun1(a)+1; }");
    addSnippetClassDecl("static int fun3(int a) { return fun2(a) + 2; }");
    addSnippetClassDecl("static int fun4(int a) { return fun3(a) + 3; }");
    Result result = optimize("int", "return fun4(1);");
    assertEquals("static int fun2(int a){ return a + 1; }",
        getCanonicalSource(result.findMethod("fun2")));
    assertEquals("static int fun3(int a){ return a + 1 + 2; }",
        getCanonicalSource(result.findMethod("fun3")));
    assertEquals("static int fun4(int a){ return a + 1 + 2 + 3; }",
        getCanonicalSource(result.findMethod("fun4")));
  }

  public void testDeadCodeElimination_notInlinable() throws Exception {
    // fun1 cannot be inlined
    addSnippetClassDecl("static boolean test1(int a)" + "{ return a>1; }");
    addSnippetClassDecl("static int fun1(int a)" + "{ if (test1(a)) { return a; }"
        + "else {switch(a) { case 1: a++; break; default: a=a+2; break; }; return a; }" + "}");
    addSnippetClassDecl("static int fun2(int a)" + "{return fun1(a);}");
    Result result = optimize("int", "return fun2(0);");
    assertEquals("static int fun1(int a){ if (a > 1) { return a;"
        + " } else { switch (a) { case 1: ++a;"
        + " break; default: a = a + 2; }"
        + " return a; } }", getCanonicalSource(result.findMethod("fun1")));
    assertEquals("static int fun2(int a){ return EntryPoint.fun1(a); }",
        getCanonicalSource(result.findMethod("fun2")));
  }

  public void testDeadCodeElimination_delayedInline() throws Exception {
    // same fun1() and fun2() as the previous test, but different test1();
    // fun1 can be inlined after inling test1 and DeadCodeElimination
    addSnippetClassDecl("static boolean test1(int a)" + "{ return true; }");
    addSnippetClassDecl("static int fun1(int a)" + "{ if (test1(a)) { return a; }"
        + "else {switch(a) { case 1: a++; break; default: a=a+2; break; }; return a; }" + "}");
    addSnippetClassDecl("static int fun2(int a)" + "{return fun1(a);}");
    Result result = optimize("int", "return fun2(0);");
    assertEquals("static int fun1(int a){ return a; }",
        getCanonicalSource(result.findMethod("fun1")));
    assertEquals("static int fun2(int a){ return a; }",
        getCanonicalSource(result.findMethod("fun2")));
  }

  public void testRecursion1() throws Exception {
    // one recursive function, and one call to the function
    addSnippetClassDecl("static int fun1(int a) { return a<=0 ? a : fun1(a-1)+a; }");
    addSnippetClassDecl("static int fun2(int b) { return b + fun1(b); }");
    Result result = optimize("int", "return fun2(5);");
    assertEquals(
        "static int fun1(int a){ return a <= 0 ? a : EntryPoint.fun1(a - 1) + a; }",
        getCanonicalSource(result.findMethod("fun1")));
    // never inline a recursive function
    assertEquals("static int fun2(int b){ return b + EntryPoint.fun1(b); }",
        getCanonicalSource(result.findMethod("fun2")));
  }

  public void testRecursion2() throws Exception {
    // one call inside a recursive function
    addSnippetClassDecl("static int fun1(int a) { return a + 1; }");
    addSnippetClassDecl("static int fun2(int b) { return b<=0 ? fun1(b) : fun2(b-1)+b; }");
    Result result = optimize("int", "return fun2(5);");
    // recursive function can inline other functions
    assertEquals("static int fun2(int b){"
        + " return b <= 0 ? b + 1 : EntryPoint.fun2(b - 1) + b; }",
        getCanonicalSource(result.findMethod("fun2")));
  }

  public void testRecursion3() throws Exception {
    // nested recursive call
    addSnippetClassDecl("static int fun1(int a) { return a<=0 ? a : fun2(a-1)+a; }");
    addSnippetClassDecl("static int fun2(int a) { return a<=0 ? a : fun1(a-1)+a; }");
    Result result = optimize("int", "return fun1(0);");
    assertEquals("static int fun1(int a){"
        + " return a <= 0 ? a : (a - 1 <= 0 ? a - 1 : EntryPoint.fun1(a - 1 - 1) + a - 1) + a; }",
        getCanonicalSource(result.findMethod("fun1")));
    assertEquals(
        "static int fun2(int a){ return a <= 0 ? a : EntryPoint.fun1(a - 1) + a; }",
        getCanonicalSource(result.findMethod("fun2")));
  }

  @Override
  protected boolean doOptimizeMethod(TreeLogger logger, JProgram program, JMethod method) {
    program.addEntryMethod(findMainMethod(program));
    boolean didChange = false;
    while (MethodInliner.exec(program).didChange()) {
      didChange = true;
    }
    return didChange;
  }
}
