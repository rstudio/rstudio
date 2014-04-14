/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.dev.jjs.impl.gflow.constants;

import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JBinaryOperator;
import com.google.gwt.dev.jjs.ast.JBooleanLiteral;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JIntLiteral;
import com.google.gwt.dev.jjs.ast.JNullLiteral;
import com.google.gwt.dev.jjs.ast.JPrimitiveType;
import com.google.gwt.dev.jjs.ast.JValueLiteral;
import com.google.gwt.dev.jjs.ast.JVariableRef;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.js.JMultiExpression;
import com.google.gwt.thirdparty.guava.common.base.Preconditions;

/**
 * Evaluate expression based on current assumptions.
 */
public class ExpressionEvaluator {
  /**
   * Main evaluation visitor.
   */
  private static class EvaluatorVisitor extends JVisitor {
    private final ConstantsAssumption assumptions;
    /**
     * Contains evaluation result for visited node. Is <code>null</code>
     * if we can't evaluate.
     */
    private JValueLiteral result = null;

    public EvaluatorVisitor(ConstantsAssumption assumptions) {
      this.assumptions = assumptions;
    }

    public JValueLiteral evaluate(JExpression expression) {
      Preconditions.checkNotNull(expression);
      accept(expression);
      return result;
    }

    @Override
    public boolean visit(JBinaryOperation x, Context ctx) {
      accept(x.getRhs());
      if (result == null) {
        return false;
      }
      JValueLiteral rhs = result;
      accept(x.getLhs());
      if (result == null) {
        return false;
      }
      JValueLiteral lhs = result;
      result = evalBinOp(x, lhs, rhs);
      return false;
    }

    @Override
    public boolean visit(JExpression x, Context ctx) {
      // We don't know what's this expression about. Can't evaluate it.
      result = null;
      return false;
    }

    @Override
    public boolean visit(JMultiExpression x, Context ctx) {
      accept(x.getExpression(x.getNumberOfExpressions() - 1));
      return false;
    }

    @Override
    public boolean visit(JValueLiteral x, Context ctx) {
      result = x;
      return false;
    }

    @Override
    public boolean visit(JVariableRef x, Context ctx) {
      result = assumptions != null ? assumptions.get(x.getTarget()) : null;
      return false;
    }
  }

  public static JValueLiteral evalBinOp(JBinaryOperation x, JValueLiteral lhs,
      JValueLiteral rhs) {
    if (lhs instanceof JNullLiteral ||
        rhs instanceof JNullLiteral) {
      if (x.getOp() == JBinaryOperator.EQ) {
        return JBooleanLiteral.get(
            (lhs instanceof JNullLiteral) && (rhs instanceof JNullLiteral));
      } else if (x.getOp() == JBinaryOperator.NEQ) {
        return JBooleanLiteral.get(
            !(lhs instanceof JNullLiteral) || !(rhs instanceof JNullLiteral));
      }
    }

    if (!lhs.getType().equals(rhs.getType())) {
      // do not even try to get type conversions right :)
      return null;
    }

    // TODO: support other types.

    if (lhs.getType().equals(JPrimitiveType.INT)) {
      if (!(lhs instanceof JIntLiteral) ||
          !(rhs instanceof JIntLiteral)) {
        return null;
      }

      int a = ((JIntLiteral) lhs).getValue();
      int b = ((JIntLiteral) rhs).getValue();

      switch (x.getOp()) {
        case ADD:
          return new JIntLiteral(x.getSourceInfo(), a + b);
        case MUL:
          return new JIntLiteral(x.getSourceInfo(), a * b);
        case SUB:
          return new JIntLiteral(x.getSourceInfo(), a - b);
        case DIV:
          if (b != 0) {
            return new JIntLiteral(x.getSourceInfo(), a / b);
          } else {
            return null;
          }
        case EQ:
          return JBooleanLiteral.get(a == b);
        case NEQ:
          return JBooleanLiteral.get(a != b);
        case GT:
          return JBooleanLiteral.get(a > b);
        case GTE:
          return JBooleanLiteral.get(a >= b);
        case LT:
          return JBooleanLiteral.get(a < b);
        case LTE:
          return JBooleanLiteral.get(a <= b);

        default:
          return null;
      }
    } else if (lhs.getType().equals(JPrimitiveType.BOOLEAN)) {
      if (!(lhs instanceof JBooleanLiteral) ||
          !(rhs instanceof JBooleanLiteral)) {
        return null;
      }

      boolean a = ((JBooleanLiteral) lhs).getValue();
      boolean b = ((JBooleanLiteral) rhs).getValue();

      switch (x.getOp()) {
        case EQ:
          return JBooleanLiteral.get(a == b);
        case NEQ:
          return JBooleanLiteral.get(a != b);

        default:
          return null;
      }
    }

    return null;
  }

  public static JValueLiteral evaluate(JExpression expression,
      ConstantsAssumption assumptions) {
    return new EvaluatorVisitor(assumptions).evaluate(expression);
  }
}
