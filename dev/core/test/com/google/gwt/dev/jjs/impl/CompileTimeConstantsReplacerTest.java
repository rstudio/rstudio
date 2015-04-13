/*
 * Copyright 2014 Google Inc.
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
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JFieldRef;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JVisitor;

/**
 * Tests for class {@link CompileTimeConstantsReplacer}.
 */
public class CompileTimeConstantsReplacerTest extends OptimizerTestBase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    runDeadCodeElimination = false;
  }

  public void testReplacement_basic() throws Exception {
    addSnippetClassDecl(
        "static class A  { ",
        "  final static int f1 = 1;",
        "  static int f2 = 2;",
        "  final static int f3;",
        "  static { f3 = 3; }",
        "}");
    optimizeInto("int i = A.f1;", "int i = 1;");
    optimizeInto("int i = A.f2;", "int i = A.f2;");
    optimizeInto("int i = A.f3;", "int i = A.f3;");
  }

  public void testReplacement_inLValue() throws Exception {
    addSnippetClassDecl("static class B {"
        + " final static int f1 = 1;"
        + " int m() { "
        + "   if (f1 == 1) "
        + "     return 1; "
        + "   else "
        + "     return 2;"
        + " } "
        + "}");
    Result result = optimize("void", "int i = B.f1; int[] a = new int[] {1, 2, 3}; a[B.f1] = 1;");
    result.into("int i = 1; int[] a = new int[] {1, 2, 3}; a[1] = 1;");
    JDeclaredType bType = result.findClass("test.EntryPoint.B");
    assertTrue("f1 not found as l-value of declaration statement",
        findMethod(bType, "$clinit").getBody().toString().contains("final static int f1 = 1"));
    assertTrue("f1 in condition was not replaced",
        findMethod(bType, "m").getBody().toString().contains("1 == 1"));
    assertEquals("incorrect modifiers for f1",
        "final static int f1", findField(bType, "f1").toString());
    assertNoCompileTimeConstants(result.getOptimizedProgram());
  }

  public void testReplacement_withAddition() throws Exception {
    addSnippetClassDecl(
        "static class A  { ",
        "  final static int f1 = 1 + 1;",
        "  static int f2 = 2;",
        "  final static int f3;",
        "  static { f3 = 3; }",
        "}",
        "static class B  { ",
        "  final static int f1 = 1 + A.f1;",
        "  static int f2 = 2;",
        "  final static int f3;",
        "  static { f3 = 3; }",
        "}");
    optimizeInto("int i = A.f1;", "int i = 1 + 1;");
    optimizeInto("int i = B.f1;", "int i = 1 + 1 + 1;");
  }

  public void testReplacement_withCoercion() throws Exception {
    addSnippetClassDecl(
        "static class A  { ",
        "  final static double f1 = 1 + 1;",
        "  static int f2 = 2;",
        "  final static int f3;",
        "  static { f3 = 3; }",
        "}",
        "static class B  { ",
        "  final static double f1 = 1 + A.f1;",
        "  static int f2 = 2;",
        "  final static int f3;",
        "  static { f3 = 3; }",
        "}");
    optimizeInto("double i = A.f1;", "double i = (double) (1 + 1);");
    optimizeInto("double i = B.f1;", "double i = 1 + (double) (1 + 1);");
  }

  @Override
  protected boolean doOptimizeMethod(TreeLogger logger, JProgram program, JMethod method) {
    CompileTimeConstantsReplacer.exec(program);
    return true;
  }

  private void assertNoCompileTimeConstants(JProgram program) {
    new JVisitor() {
      @Override
      public void endVisit(JFieldRef x, Context ctx) {
        assertTrue(x.getField() + " was not replaced everywhere",
            ctx.isLvalue() || !x.getField().isCompileTimeConstant());
      }
    }.accept(program);
  }

  private void optimizeInto(String original, String into) throws UnableToCompleteException {
    Result result = optimize("void", original);
    assertNoCompileTimeConstants(result.getOptimizedProgram());
    result.into(into);
  }
}
