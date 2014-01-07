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

import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JExpressionStatement;
import com.google.gwt.dev.jjs.ast.JModVisitor;

/**
 * Test for {@link JModVisitor}.
 */
public class JModVisitorTest extends JJSTestBase {
  // TODO(rluble): add more comprehensive tests.

  /**
   * Removes all expression statements.
   */
  private static final class RemoveAllExpressionStatementsVisitor extends JModVisitor {

    @Override
    public void endVisit(JExpressionStatement exprStatement, Context ctx) {
      ctx.removeMe();
    }
  }

  public void testRemoveForBody() throws Exception {
    assertTransform("for(int i = 3; i < 4; i++) i = 8;",
        new RemoveAllExpressionStatementsVisitor()).into("for(int i = 3; i < 4; i++) ;");

    assertTransform("for(int i = 3; i < 4; i++) { i = 8; }",
        new RemoveAllExpressionStatementsVisitor()).into("for(int i = 3; i < 4; i++) {}");
  }

  public void testRemoveIfThenBody() throws Exception {
    assertTransform("int i = 3; if (i == 5) i = 3;",
        new RemoveAllExpressionStatementsVisitor()).into("int i = 3; if (i == 5) ;");
  }

  public void testRemoveIfElseBody() throws Exception {
    assertTransform("int i = 3; if (i == 5) {} else i = 4;",
        new RemoveAllExpressionStatementsVisitor()).into("int i = 3; if (i == 5) {}");
  }

  public void testRemoveDoBody() throws Exception {
    assertTransform("int i = 3; do i = 3; while (false);",
        new RemoveAllExpressionStatementsVisitor()).into("int i = 3; do ; while (false);");
  }

  public void testRemoveWhileBody() throws Exception {
    assertTransform("int i = 3;  while (i < 3) i = 4;",
        new RemoveAllExpressionStatementsVisitor()).into("int i = 3; while (i < 3);");
  }
}
