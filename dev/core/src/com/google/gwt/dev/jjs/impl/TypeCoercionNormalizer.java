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
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JBinaryOperator;
import com.google.gwt.dev.jjs.ast.JCharLiteral;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JLongLiteral;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JPrimitiveType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JType;

/**
 * Type coercion (and operator overloading) semantics differ widely in Java and JavaScript.
 * This pass fixes the following mismatches:
 * <ul>
 *   <li>the binary concat operator (+) due to very loose semantics in JavaScript</li>
 *   <li>integer division, due to integers beign represented as floats in JavaScript</li>
 * </ul>
 */
public class TypeCoercionNormalizer {

  /**
   * Explicitly convert any char or long type expressions within a concat
   * operation into strings because normal JavaScript conversion does not work
   * correctly.
   */
  private class ConcatRewriteVisitor extends JModVisitor {

    @Override
    public void endVisit(JBinaryOperation x, Context ctx) {
      if (!isConcatOperation(x.getOp())) {
        return;
      }

      JExpression newLhs = coerceToString(x.getLhs());
      JExpression newRhs = coerceToString(x.getRhs());

      assert !x.getOp().isAssignment() || newLhs == x.getLhs() :
          "L-values can never be rewritten (CONCAT_ASG can not have an lhs of type char or long).";

      if (newLhs != x.getLhs() || newRhs != x.getRhs()) {
        JBinaryOperation newExpr =
            new JBinaryOperation(x.getSourceInfo(), program.getTypeJavaLangString(),
                x.getOp(), newLhs, newRhs);
        ctx.replaceMe(newExpr);
      }
    }

    private JExpression coerceToString(JExpression expr) {
      final JPrimitiveType typePrimitiveChar = program.getTypePrimitiveChar();
      final JPrimitiveType typePrimitiveLong = program.getTypePrimitiveLong();

      if (expr instanceof JLongLiteral) {
        // Replace the literal by a string containing the literal.
        long longValue = ((JLongLiteral) expr).getValue();
        return program.getStringLiteral(expr.getSourceInfo(), String.valueOf(longValue));
      } else if (expr.getType() == typePrimitiveLong) {
        JMethodCall call = new JMethodCall(expr.getSourceInfo(), null,
            program.getIndexedMethod("LongLib.toString"), expr);
        return call;
      } else if (expr instanceof JCharLiteral) {
        // Replace the literal by a string containing the literal.
        char charValue = ((JCharLiteral) expr).getValue();
        return program.getStringLiteral(expr.getSourceInfo(), Character.toString(charValue));
      } else if (expr.getType() == typePrimitiveChar) {
        // A non literal expression of type Char.
        // Replace with Cast.charToString(c)
        JMethodCall call = new JMethodCall(expr.getSourceInfo(), null,
            program.getIndexedMethod("Cast.charToString"), expr);
        return call;
      }

      return expr;
    }

    private boolean isConcatOperation(JBinaryOperator operator) {
      switch (operator) {
        case CONCAT:
        case ASG_CONCAT:
          return true;
        default:
          return false;
      }
    }
  }

  /**
   * Handle integral divide operations which may have floating point results.
   */
  private class DivRewriteVisitor extends JModVisitor {

    @Override
    public void endVisit(JBinaryOperation x, Context ctx) {
      JType type = x.getType();
      if (x.getOp() != JBinaryOperator.DIV || type == program.getTypePrimitiveFloat()
          || type == program.getTypePrimitiveDouble()) {
        return;
      }

      /*
       * If the numerator was already in range, we can assume the output is
       * also in range. Therefore, we don't need to do the full conversion,
       * but rather a narrowing int conversion instead.
       */
      String methodName = "Cast.narrow_" + type.getName();
      JMethod castMethod = program.getIndexedMethod(methodName);
      JMethodCall call = new JMethodCall(x.getSourceInfo(), null, castMethod, type, x);
      x.setType(program.getTypePrimitiveDouble());
      ctx.replaceMe(call);
    }
  }

  public static void exec(JProgram program) {
    new TypeCoercionNormalizer(program).execImpl();
  }

  private final JProgram program;

  private TypeCoercionNormalizer(JProgram program) {
    this.program = program;
  }

  private void execImpl() {
    new ConcatRewriteVisitor().accept(program);
    new DivRewriteVisitor().accept(program);
  }
}
