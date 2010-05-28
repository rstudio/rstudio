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
import com.google.gwt.dev.jjs.ast.JDeclarationStatement;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JLocal;
import com.google.gwt.dev.jjs.ast.JLocalRef;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JPostfixOperation;
import com.google.gwt.dev.jjs.ast.JPrefixOperation;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JType;

/**
 * Test for {@link TempLocalVisitor}.
 */
public class TempLocalVisitorTest extends OptimizerTestBase {
  /**
   * For testing purposes; replaces all non-lvalue expressions with a temp.
   */
  private static final class AlwaysReplacer extends TempLocalVisitor {
    @Override
    public void endVisit(JExpression x, Context ctx) {
      SourceInfo info = x.getSourceInfo();
      JType type = x.getType();
      JLocal local = createTempLocal(info, type);
      local.getDeclarationStatement().initializer = x;
      ctx.replaceMe(new JLocalRef(info, local));
    }

    @Override
    public boolean visit(JBinaryOperation x, Context ctx) {
      super.visit(x, ctx);
      if (x.getRhs() != null) {
        JExpression newRhs = accept(x.getRhs());
        if (newRhs != x.getRhs()) {
          ctx.replaceMe(new JBinaryOperation(x.getSourceInfo(), x.getType(),
              x.getOp(), x.getLhs(), newRhs));
        }
      }
      return false;
    }

    @Override
    public boolean visit(JDeclarationStatement x, Context ctx) {
      super.visit(x, ctx);
      if (x.initializer != null) {
        x.initializer = accept(x.initializer);
      }
      return false;
    }

    @Override
    public boolean visit(JPostfixOperation x, Context ctx) {
      if (x.getOp().isModifying()) {
        return false;
      }
      return true;
    }

    @Override
    public boolean visit(JPrefixOperation x, Context ctx) {
      if (x.getOp().isModifying()) {
        return false;
      }
      return true;
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
    assertTransform("int i = 3;").into("int $t0 = 3; int i = $t0;");
  }

  public void testExisting() throws Exception {
    assertTransform("int $t0 = 3; int i = $t0;").into(
        "int $t1 = 3; int $t0 = $t1; int $t2 = $t0; int i = $t2;");
  }

  public void testForStatement() throws Exception {
    StringBuilder original = new StringBuilder();
    original.append("for (int i = 3; true; );");

    StringBuilder expected = new StringBuilder();
    /*
     * TODO(scottb): technically $t1 could be part of the for statement's
     * initializer list.
     */
    expected.append("boolean $t1 = true;");
    expected.append("for (int $t0 = 3, i = $t0; $t1; );");

    assertTransform(original.toString()).into(expected.toString());
  }

  public void testForStatementScoping() throws Exception {
    StringBuilder original = new StringBuilder();
    original.append("boolean f = false;");
    original.append("for (int i = 3; f; );");
    original.append("int j = 4;");

    StringBuilder expected = new StringBuilder();
    expected.append("boolean $t0 = false;");
    expected.append("boolean f = $t0;");
    expected.append("boolean $t2 = f;");
    expected.append("for (int $t1 = 3, i = $t1; $t2; );");
    /*
     * TODO(scottb): technically we could reuse $t1 here as a minor improvement.
     */
    expected.append("int $t3 = 4; int j = $t3;");

    assertTransform(original.toString()).into(expected.toString());
  }

  public void testNested() throws Exception {
    assertTransform("{ int i = 3; } ").into("{ int $t0 = 3; int i = $t0; }");
  }

  public void testNestedPost() throws Exception {
    StringBuilder original = new StringBuilder();
    original.append("int i = 3;");
    original.append("{ int j = 4; }");

    StringBuilder expected = new StringBuilder();
    expected.append("int $t0 = 3; int i = $t0;");
    expected.append("{ int $t1 = 4; int j = $t1; }");

    assertTransform(original.toString()).into(expected.toString());
  }

  public void testNestedPre() throws Exception {
    StringBuilder original = new StringBuilder();
    original.append("{ int i = 3; }");
    original.append("int j = 4;");

    StringBuilder expected = new StringBuilder();
    expected.append("{ int $t0 = 3; int i = $t0; }");
    expected.append("int $t1 = 4; int j = $t1;");

    assertTransform(original.toString()).into(expected.toString());
  }

  public void testReuse() throws Exception {
    StringBuilder original = new StringBuilder();
    original.append("{ int i = 3; }");
    original.append("{ int j = 4; }");

    StringBuilder expected = new StringBuilder();
    expected.append("{ int $t0 = 3; int i = $t0; }");
    expected.append("{ int $t0 = 4; int j = $t0; }");

    assertTransform(original.toString()).into(expected.toString());
  }

  public void testReuseTwice() throws Exception {
    StringBuilder original = new StringBuilder();
    original.append("{ int i = 3; int j = 4; }");
    original.append("{ int i = 3; int j = 4; }");

    StringBuilder expected = new StringBuilder();
    expected.append("{ int $t0 = 3; int i = $t0; int $t1 = 4; int j = $t1; }");
    expected.append("{ int $t0 = 3; int i = $t0; int $t1 = 4; int j = $t1; }");

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
    expected.append("int $t0 = 0; int a = $t0;");
    expected.append("{ int $t1 = 3; int i = $t1; int $t2 = 4; int j = $t2; }");
    expected.append("int $t3 = 1; int b = $t3;");
    expected.append("{ int $t1 = 3; int i = $t1; int $t2 = 4; int j = $t2; }");
    expected.append("int $t4 = 2; int c = $t4;");

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
