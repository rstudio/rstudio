/*
 * Copyright 2010 Google Inc.
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
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JBinaryOperator;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JExpressionStatement;
import com.google.gwt.dev.jjs.ast.JLocal;
import com.google.gwt.dev.jjs.ast.JLocalRef;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JType;

/**
 * Test for {@link TempLocalVisitor}.
 */
public class TempLocalVisitorTest extends JJSTestBase {
  /**
   * Replaces expressions with assignment-to-temp. Does not replace lvalues, or
   * the top level expression in an expression statement.
   */
  private static final class AlwaysReplacer extends TempLocalVisitor {
    /**
     * Don't bother replacing entire JExpressionStatements with a temp.
     */
    private JExpression dontBother;

    @Override
    public boolean visit(JExpressionStatement x, Context ctx) {
      dontBother = x.getExpr();
      return super.visit(x, ctx);
    }

    @Override
    public void endVisit(JExpression x, Context ctx) {
      if (x != dontBother && !ctx.isLvalue()) {
        SourceInfo info = x.getSourceInfo();
        JType type = x.getType();
        JLocal local = createTempLocal(info, type);
        ctx.replaceMe(new JBinaryOperation(info, type, JBinaryOperator.ASG,
            new JLocalRef(info, local), x));
      }
    }
  }

  private final class Result {
    private final String optimized;
    private final String returnType;
    private final String userCode;

    public Result(String returnType, String userCode, String optimized) {
      this.returnType = returnType;
      this.userCode = userCode;
      this.optimized = optimized;
    }

    public void into(String expected) throws UnableToCompleteException {
      JProgram program = compileSnippet(returnType, expected);
      expected = getMainMethodSource(program);
      assertEquals(userCode, expected, optimized);
    }
  }

  public void testBasic() throws Exception {
    assertTransform("int i = 3;").into("int $t0; int i = $t0 = 3;");
  }

  public void testExisting() throws Exception {
    assertTransform("int $t0; int i = $t0 = 3;").into(
        "int $t0; int $t1; int $t2; int i = $t2 = $t0 = $t1 = 3;");
  }

  public void testForStatement() throws Exception {
    StringBuilder original = new StringBuilder();
    original.append("for (int i = 0; true; i += 1);");

    StringBuilder expected = new StringBuilder();
    /*
     * TODO(scottb): technically $t1 and $t2 could be part of the for
     * statement's initializer list.
     */
    expected.append("boolean $t1;");
    expected.append("int $t2;");
    expected.append("for (int $t0, i = $t0 = 0; $t1 = true; i += $t2 = 1);");

    assertTransform(original.toString()).into(expected.toString());
  }

  public void testForStatementScoping() throws Exception {
    StringBuilder original = new StringBuilder();
    original.append("boolean f = false;");
    original.append("for (int i = 3; f; );");
    original.append("int j = 4;");

    StringBuilder expected = new StringBuilder();
    expected.append("boolean $t0;");
    expected.append("boolean f = $t0 = false;");
    expected.append("boolean $t2;");
    expected.append("for (int $t1, i = $t1 = 3; $t2 = f; );");
    /*
     * TODO(scottb): technically we could reuse $t1 here as a minor improvement.
     */
    expected.append("int $t3; int j = $t3 = 4;");

    assertTransform(original.toString()).into(expected.toString());
  }

  public void testNested() throws Exception {
    assertTransform("{ int i = 3; } ").into("{ int $t0; int i = $t0 = 3; }");
  }

  public void testNestedPost() throws Exception {
    StringBuilder original = new StringBuilder();
    original.append("int i = 3;");
    original.append("{ int j = 4; }");

    StringBuilder expected = new StringBuilder();
    expected.append("int $t0; int i = $t0 = 3;");
    expected.append("{ int $t1; int j = $t1 = 4; }");

    assertTransform(original.toString()).into(expected.toString());
  }

  public void testNestedPre() throws Exception {
    StringBuilder original = new StringBuilder();
    original.append("{ int i = 3; }");
    original.append("int j = 4;");

    StringBuilder expected = new StringBuilder();
    expected.append("{ int $t0; int i = $t0 = 3; }");
    expected.append("int $t1; int j = $t1 = 4;");

    assertTransform(original.toString()).into(expected.toString());
  }

  public void testReuse() throws Exception {
    StringBuilder original = new StringBuilder();
    original.append("{ int i = 3; }");
    original.append("{ int j = 4; }");

    StringBuilder expected = new StringBuilder();
    expected.append("{ int $t0; int i = $t0 = 3; }");
    expected.append("{ int $t0; int j = $t0 = 4; }");

    assertTransform(original.toString()).into(expected.toString());
  }

  public void testReuseTwice() throws Exception {
    StringBuilder original = new StringBuilder();
    original.append("{ int i = 3; int j = 4; }");
    original.append("{ int i = 3; int j = 4; }");

    StringBuilder expected = new StringBuilder();
    expected.append("{ int $t0; int i = $t0 = 3; int $t1; int j = $t1 = 4; }");
    expected.append("{ int $t0; int i = $t0 = 3; int $t1; int j = $t1 = 4; }");

    assertTransform(original.toString()).into(expected.toString());
  }

  public void testVeryComplex() throws Exception {
    StringBuilder original = new StringBuilder();
    original.append("int a = 0;");
    original.append("{ int i = 3; int j = 4; }");
    original.append("int b = 1;");
    original.append("{ int i = 3; int j = 4; }");
    original.append("int c = 2;");

    StringBuilder expected = new StringBuilder();
    expected.append("int $t0; int a = $t0 = 0;");
    expected.append("{ int $t1; int i = $t1 = 3; int $t2; int j = $t2 = 4; }");
    expected.append("int $t3; int b = $t3 = 1;");
    expected.append("{ int $t1; int i = $t1 = 3; int $t2; int j = $t2 = 4; }");
    expected.append("int $t4; int c = $t4 = 2;");

    assertTransform(original.toString()).into(expected.toString());
  }

  private Result assertTransform(String codeSnippet)
      throws UnableToCompleteException {
    JProgram program = compileSnippet("void", codeSnippet);
    JMethod mainMethod = findMainMethod(program);
    new AlwaysReplacer().accept(mainMethod);
    return new Result("void", codeSnippet, mainMethod.getBody().toSource());
  }
}
