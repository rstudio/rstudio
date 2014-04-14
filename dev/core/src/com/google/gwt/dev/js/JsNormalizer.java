/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.dev.js;

import com.google.gwt.dev.js.ast.JsBinaryOperation;
import com.google.gwt.dev.js.ast.JsBinaryOperator;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsExpression;
import com.google.gwt.dev.js.ast.JsModVisitor;
import com.google.gwt.dev.js.ast.JsPostfixOperation;
import com.google.gwt.dev.js.ast.JsPrefixOperation;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsUnaryOperation;
import com.google.gwt.dev.js.ast.JsUnaryOperator;

/**
 * Fixes any semantic errors introduced by JS AST gen.
 *
 * <ul>
 * <li>Creating clinit calls can put comma expressions as lvalues; the modifying
 * operation must be moved inside the comma expression to the last argument.</li>
 * </ul>
 */
public class JsNormalizer {

  /**
   * Resolves any unresolved JsNameRefs.
   */
  private static class JsNormalizing extends JsModVisitor {

    @Override
    public void endVisit(JsBinaryOperation x, JsContext ctx) {
      maybeShuffleModifyingBinary(x, ctx);
    }

    @Override
    public void endVisit(JsPostfixOperation x, JsContext ctx) {
      maybeShuffleModifyingUnary(x, ctx);
    }

    @Override
    public void endVisit(JsPrefixOperation x, JsContext ctx) {
      maybeShuffleModifyingUnary(x, ctx);
    }

    /**
     * Due to the way clinits are constructed, you can end up with a comma
     * operation as the argument to a modifying operation, which is illegal.
     * Juggle things to put the operator inside of the comma expression.
     */
    private void maybeShuffleModifyingBinary(JsBinaryOperation x, JsContext ctx) {
      JsBinaryOperator myOp = x.getOperator();
      JsExpression lhs = x.getArg1();

      if (myOp.isAssignment() && (lhs instanceof JsBinaryOperation)) {
        // Find the rightmost comma operation
        JsBinaryOperation curLhs = (JsBinaryOperation) lhs;
        assert (curLhs.getOperator() == JsBinaryOperator.COMMA);
        while (curLhs.getArg2() instanceof JsBinaryOperation) {
          curLhs = (JsBinaryOperation) curLhs.getArg2();
          assert (curLhs.getOperator() == JsBinaryOperator.COMMA);
        }
        // curLhs is now the rightmost comma operation; slide our operation in
        x.setArg1(curLhs.getArg2());
        curLhs.setArg2(x);
        // replace myself with the comma expression
        ctx.replaceMe(lhs);
      }
    }

    /**
     * Due to the way clinits are constructed, you can end up with a comma
     * operation as the argument to a modifying operation, which is illegal.
     * Juggle things to put the operator inside of the comma expression.
     */
    private void maybeShuffleModifyingUnary(JsUnaryOperation x, JsContext ctx) {
      JsUnaryOperator myOp = x.getOperator();
      JsExpression arg = x.getArg();
      if (myOp.isModifying() && (arg instanceof JsBinaryOperation)) {
        // Find the rightmost comma operation
        JsBinaryOperation curArg = (JsBinaryOperation) arg;
        assert (curArg.getOperator() == JsBinaryOperator.COMMA);
        while (curArg.getArg2() instanceof JsBinaryOperation) {
          curArg = (JsBinaryOperation) curArg.getArg2();
          assert (curArg.getOperator() == JsBinaryOperator.COMMA);
        }
        // curArg is now the rightmost comma operation; slide our operation in
        x.setArg(curArg.getArg2());
        curArg.setArg2(x);
        // replace myself with the comma expression
        ctx.replaceMe(arg);
      }
    }
  }

  public static void exec(JsProgram program) {
    new JsNormalizer(program).execImpl();
  }

  private final JsProgram program;

  private JsNormalizer(JsProgram program) {
    this.program = program;
  }

  private void execImpl() {
    JsNormalizing normalizer = new JsNormalizing();
    normalizer.accept(program);
  }
}
